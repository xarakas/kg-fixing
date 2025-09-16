package org.nikolasparaskakis.core;



import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.query.*;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.sparql.SPARQLRepository;
import org.semanticweb.owlapi.formats.TurtleDocumentFormat;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.rio.RioOWLRDFConsumerAdapter;
import org.semanticweb.owlapi.util.AnonymousNodeChecker;
import org.semanticweb.owlapi.util.AnonymousNodeCheckerImpl;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;
import static org.semanticweb.owlapi.apibinding.OWLManager.createOWLOntologyManager;



/**
 * Class for handling ABox operations in an ontology using a SPARQL endpoint.
 */
@SuppressWarnings({"unused", "DuplicatedCode", "ExtractMethodRecommender", "FieldCanBeLocal"})
public class ABoxHandler implements AutoCloseable {

    /**
     * SPARQL repository for connecting to the SPARQL endpoint.
     */
    private SPARQLRepository repo;

    /**
     * Connection to the SPARQL endpoint.
     */
//    private RepositoryConnection conn;

    /**
     * The graph domain (named graph) where the ABox data is stored.
     */
    private String graphDomain;

    /**
     * Maximum number of retries for SPARQL queries.
     */
    private final int MAX_RETRIES;

    /**
     * Reference to the TBoxHandler for accessing TBox axioms.
     */
    private TBoxHandler tBoxHandler;

    /**
     * Position tracker to manage immutable axioms and positions.
     */
    private PositionTracker pt;

    /**
     * Constructor for ABoxHandler.
     * @param sparqlEndpoint The SPARQL endpoint URL.
     * @param graphDomain The graph domain (named graph) where the ABox data is stored.
     * @param tBoxHandler Reference to the TBoxHandler for accessing TBox axioms.
     * @param pt Position tracker to manage immutable axioms and positions.
     */
    public ABoxHandler(String sparqlEndpoint, String graphDomain, TBoxHandler tBoxHandler, PositionTracker pt) {
        this.graphDomain = graphDomain;
        this.tBoxHandler = tBoxHandler;
        this.MAX_RETRIES = 5;

        this.repo = new SPARQLRepository(sparqlEndpoint + "/sparql", sparqlEndpoint + "/update");

        this.pt = pt;
    }

    /**
     * Retrieves the ABox module for a given individual.
     * @param individual The IRI of the individual.
     * @return A concurrent linked queue containing the ABox axioms related to the individual.
     */
    public ConcurrentLinkedQueue<OWLAxiom> getABoxModule(String individual) {
        OWLOntologyManager man = createOWLOntologyManager();
        OWLOntology ontology = null;

        // Construct a SPARQL query to fetch axioms where the individual's IRI is subject or object
        String queryAxioms = String.format(
                "CONSTRUCT { ?s ?p ?o } " +
                        "FROM <%s> " +
                        "WHERE {" +
                        " { VALUES ?s { %s } . ?s ?p ?o . } " +
                        " UNION " +
                        " { VALUES ?o { %s } . ?s ?p ?o . } " +
                        "}", this.graphDomain, individual, individual
        );

        // Constants for retry mechanism
        final int RETRY_DELAY_MS = 3000; // Wait 3 seconds before retrying
        int attempts = 0;
        boolean success = false;

        while (attempts < this.MAX_RETRIES && !success) {
            try {
                // Initialize an ontology to store the ABox axioms
                try {
                    // Create a new ontology from the declarations in the TBox
                    ontology = man.createOntology(tBoxHandler.getOntologyTBox());
                } catch (OWLOntologyCreationException e) {
                    throw new RuntimeException("Failed to create ontology: " + e.getMessage(), e);
                }

                // Set up the Turtle document format and ontology loader configuration
                TurtleDocumentFormat format = new TurtleDocumentFormat();
                OWLOntologyLoaderConfiguration configuration = new OWLOntologyLoaderConfiguration();
                AnonymousNodeChecker checker = new AnonymousNodeCheckerImpl();
                RioOWLRDFConsumerAdapter consumer = new RioOWLRDFConsumerAdapter(ontology, checker, configuration);
                consumer.setOntologyFormat(format);

                try (RepositoryConnection conn = repo.getConnection()) {
                    GraphQuery gq = conn.prepareGraphQuery(QueryLanguage.SPARQL, queryAxioms);

                    try (GraphQueryResult res = gq.evaluate()) {
                        consumer.startRDF();
                        while (res.hasNext()) {
                            Statement st = res.next();
                            try {
                                consumer.handleStatement(st);
                            } catch (Exception perStmt) {
                                System.err.println("Skipping bad statement: " + st + " — " + perStmt.getMessage());
                            }
                        }
                        consumer.endRDF();
                    }
                }

                success = true; // Mark success if no exception occurs
            } catch (Exception e) {
                attempts++;
                System.err.println("Error executing SPARQL query (attempt " + attempts + "): " + e.getMessage());
                man.clearOntologies();

                if (attempts < this.MAX_RETRIES) {
                    try {
                        Thread.sleep(RETRY_DELAY_MS);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                } else {
                    System.err.println("Max retries reached. Returning partial results.");
                    break;
                }
            }
        }

        if (ontology == null) {
            return null;
        }

        ConcurrentLinkedQueue<OWLAxiom> module = ontology.axioms()
                .distinct()
                .collect(Collectors.toCollection(ConcurrentLinkedQueue::new));

        module.removeAll(tBoxHandler.getOntologyTBox());

        man.clearOntologies();

        return module;
    }

    /**
     * Retrieves the ABox module for a list of individuals.
     * @param individuals List of IRIs of the individuals.
     * @return A concurrent linked queue containing the ABox axioms related to the individuals.
     */
    public ConcurrentLinkedQueue<OWLAxiom> getABoxModules(ArrayList<String> individuals) {

        List<String> tokens = individuals.stream()
                .distinct()
                .collect(Collectors.toList());

        if (tokens.isEmpty()) {
            return new ConcurrentLinkedQueue<>();
        }

        OWLOntologyManager man = createOWLOntologyManager();
        OWLOntology ontology;
        try {
            // one ontology for all batches, seeded with TBox
            ontology = man.createOntology(tBoxHandler.getOntologyTBox());
        } catch (OWLOntologyCreationException e) {
            throw new RuntimeException("Failed to create ontology: " + e.getMessage(), e);
        }

        final int BATCH_SIZE = 50_000;
        final int RETRY_DELAY_MS = 3_000;

        for (int start = 0; start < tokens.size(); start += BATCH_SIZE) {
            List<String> batch = tokens.subList(start, Math.min(start + BATCH_SIZE, tokens.size()));
            String joinedIRIs = String.join(" ", batch);

            String valuesSubject = "VALUES ?s { " + joinedIRIs + " }";
            String valuesObject = "VALUES ?o { " + joinedIRIs + " }";

            String queryAxioms = String.format(
                    "CONSTRUCT { ?s ?p ?o } " +
                            "FROM <%s> " +
                            "WHERE { " +
                            " { %s . ?s ?p ?o . } " +
                            " UNION " +
                            " { %s . ?s ?p ?o . } " +
                            "}",
                    this.graphDomain, valuesSubject, valuesObject
            );

            int attempts = 0;
            boolean success = false;

            while (attempts < this.MAX_RETRIES && !success) {
                try {
                    TurtleDocumentFormat format = new TurtleDocumentFormat();
                    OWLOntologyLoaderConfiguration configuration = new OWLOntologyLoaderConfiguration();
                    AnonymousNodeChecker checker = new AnonymousNodeCheckerImpl();
                    RioOWLRDFConsumerAdapter consumer = new RioOWLRDFConsumerAdapter(ontology, checker, configuration);
                    consumer.setOntologyFormat(format);

                    try (RepositoryConnection conn = repo.getConnection()) {
                        GraphQuery gq = conn.prepareGraphQuery(QueryLanguage.SPARQL, queryAxioms);

                        try (GraphQueryResult res = gq.evaluate()) {
                            consumer.startRDF();
                            while (res.hasNext()) {
                                Statement st = res.next();
                                try {
                                    consumer.handleStatement(st);
                                } catch (Exception perStmt) {
                                    System.err.println("Skipping bad statement: " + st + " — " + perStmt.getMessage());
                                }
                            }
                            consumer.endRDF();
                        }
                    }

                    success = true;
                } catch (Exception e) {
                    attempts++;
                    System.err.println(
                            "Error executing SPARQL batch " + (start / BATCH_SIZE + 1) +
                                    " (attempt " + attempts + "): " + e.getMessage()
                    );

                    if (attempts < this.MAX_RETRIES) {
                        try {
                            Thread.sleep(RETRY_DELAY_MS);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            break;
                        }
                    } else {
                        System.err.println(
                                "Max retries reached for batch " + (start / BATCH_SIZE + 1) +
                                        ". Continuing with next batch."
                        );
                    }
                }
            }
        }

        // Build the module: all gathered axioms minus the TBox axioms
        ConcurrentLinkedQueue<OWLAxiom> module = ontology.axioms().distinct().collect(Collectors.toCollection(ConcurrentLinkedQueue::new));

        module.removeAll(tBoxHandler.getOntologyTBox());

        man.clearOntologies();

        return module;
    }


//        ParserConfig pc = conn.getParserConfig();
//        pc.set(BasicParserSettings.VERIFY_URI_SYNTAX, false);
//        pc.set(BasicParserSettings.FAIL_ON_UNKNOWN_DATATYPES, false);
//        pc.set(BasicParserSettings.VERIFY_LANGUAGE_TAGS, false);
//        pc.set(BasicParserSettings.NORMALIZE_DATATYPE_VALUES, false);

    /**
     * Counts the number of distinct triples in the ABox where the given individuals appear as subject or object.
     * @param individuals List of IRIs of the individuals.
     * @return The count of distinct triples involving the given individuals.
     */
    public int countIndividualTriples(ArrayList<String> individuals) {
        String joinedIRIs = individuals.stream()
                .distinct()
                .collect(Collectors.joining(" "));

        String valuesSubject = String.format("VALUES ?s { %s }", joinedIRIs);
        String valuesObject = String.format("VALUES ?o { %s }", joinedIRIs);

        String query = String.format(
                "SELECT (COUNT(DISTINCT *) AS ?count) " +
                        "FROM <%s> " +
                        "WHERE { " +
                        " { %s . ?s ?p ?o . } " +
                        " UNION " +
                        " { %s . ?s ?p ?o . } " +
                        "}",
                graphDomain, valuesSubject, valuesObject
        );

        final int RETRY_DELAY_MS = 3000;
        int count = 0;
        int attempts = 0;

        while (attempts < 5) {
            try {
                try (RepositoryConnection conn = repo.getConnection()) {
                    TupleQuery tupleQuery = conn.prepareTupleQuery(query);
                    try (TupleQueryResult result = tupleQuery.evaluate()) {
                        if (result.hasNext()) {
                            BindingSet bindingSet = result.next();
                            count = Integer.parseInt(bindingSet.getValue("count").stringValue());
                            return count;
                        }
                    }
                }
            } catch (Exception e) {
                attempts++;
                System.err.println(
                        "Error executing count query (attempt " + attempts + "): " + e.getMessage()
                );

                if (attempts < 5) {
                    try {
                        Thread.sleep(RETRY_DELAY_MS);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                } else {
                    System.err.println("All retry attempts failed. Returning count as 0.");
                }
            }
        }

        return count;
    }

    /**
     * Applies a list of fixes to the triple store.
     * @param fixesList List of Fix objects representing the changes to be applied.
     * @return True if all fixes were applied successfully, false otherwise.
     */
    public boolean applyFixToTripleStore(ArrayList<Fix> fixesList) {
        try {
            for (Fix fix : fixesList) {
                System.out.println(fix.toString());
                if (fix.isUpdate()) {

                    if (fix.getMutedPosition() == Position.SINGLE) {
                        this.pt.addImmutableAxiom(fix.getNewAxiom());
                    } else { // some position in this axiom is mutable
                        this.pt.addImmutablePosition(fix.getNewAxiom(), fix.getMutedPosition());
                    }
                    try (RepositoryConnection conn = repo.getConnection()) {
                        // Handle the update operation: delete the old axiom and add the new one
                        String deleteQuery = constructDeleteQuery(fix.getOldAxiom());
                        Update deleteUpdate = conn.prepareUpdate(deleteQuery);
                        deleteUpdate.execute();
                        String insertQuery = constructInsertQuery(fix.getNewAxiom());
                        Update insertUpdate = conn.prepareUpdate(insertQuery);
                        insertUpdate.execute();
                    }
                } else if (fix.isDelete()) {
                    try (RepositoryConnection conn = repo.getConnection()) {
                        // Handle the delete operation: remove the old axiom
                        String deleteQuery = constructDeleteQuery(fix.getOldAxiom());
                        Update deleteUpdate = conn.prepareUpdate(deleteQuery);
                        deleteUpdate.execute();
                    }
                } else if (fix.isAdd()) {
                    this.pt.addImmutableAxiom(fix.getNewAxiom());
                    try (RepositoryConnection conn = repo.getConnection()) {
                        // Handle the add operation: insert the new axiom
                        String insertQuery = constructInsertQuery(fix.getNewAxiom());
                        Update insertUpdate = conn.prepareUpdate(insertQuery);
                        insertUpdate.execute();
                    }
                } else {
                    // Throw an exception for unsupported fix types
                    throw new IllegalArgumentException("Unsupported fix type: " + fix);
                }
            }
        }
        catch (Exception ex)  {
            // Indicate unsuccessful application of the fix
            return false;
        }

        // Indicate successful application of the fix
        return true;
    }

    /**
     * Constructs a SPARQL DELETE query for a given OWLAxiom.
     * @param oldAxiom The OWLAxiom to be deleted.
     * @return A SPARQL DELETE query string.
     */
    private String constructDeleteQuery(OWLAxiom oldAxiom) {
        if (oldAxiom instanceof OWLClassAssertionAxiom) {
            // Handle class assertion axioms
            OWLClassAssertionAxiom classAxiom = (OWLClassAssertionAxiom) oldAxiom;
            String subject = classAxiom.getIndividual().asOWLNamedIndividual().getIRI().toString();
            String object = classAxiom.getClassExpression().asOWLClass().getIRI().toString();
            return String.format("PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> " +
                    "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> " +
                    "DELETE WHERE { GRAPH <%s> { <%s> rdf:type <%s> . } }",
                    this.graphDomain, subject, object);
        } else if (oldAxiom instanceof OWLObjectPropertyAssertionAxiom) {
            // Handle object property assertion axioms
            OWLObjectPropertyAssertionAxiom propAxiom = (OWLObjectPropertyAssertionAxiom) oldAxiom;
            String subject = propAxiom.getSubject().asOWLNamedIndividual().getIRI().toString();
            String predicate = propAxiom.getProperty().asOWLObjectProperty().getIRI().toString();
            String object = propAxiom.getObject().asOWLNamedIndividual().getIRI().toString();
            return String.format("DELETE WHERE { GRAPH <%s> { <%s> <%s> <%s> . } }",
                    this.graphDomain, subject, predicate, object);
        } else if (oldAxiom instanceof OWLDataPropertyAssertionAxiom) {
            // Handle data property assertion axioms
            OWLDataPropertyAssertionAxiom dataAxiom = (OWLDataPropertyAssertionAxiom) oldAxiom;
            String subject = dataAxiom.getSubject().asOWLNamedIndividual().getIRI().toString();
            String predicate = dataAxiom.getProperty().asOWLDataProperty().getIRI().toString();
            OWLLiteral objectLiteral = dataAxiom.getObject();

            // Get lexical value
            String objectValue = objectLiteral.getLiteral();

            // Check if the literal has a language tag
            String languageTag = objectLiteral.getLang();

            // Get datatype
            OWLDatatype datatype = objectLiteral.getDatatype();
            String datatypeIRI = datatype.getIRI().toString();

            // Format literal correctly
            String formattedObject;
            if (!languageTag.isEmpty()) {
                formattedObject = String.format("\"%s\"@%s", objectValue, languageTag);
            } else if (!datatypeIRI.equals("http://www.w3.org/2001/XMLSchema#string")) {
                formattedObject = String.format("\"%s\"^^<%s>", objectValue, datatypeIRI);
            } else {
                formattedObject = String.format("\"%s\"", objectValue);
            }

            return String.format("DELETE WHERE { GRAPH <%s> { <%s> <%s> %s . } }",
                    this.graphDomain, subject, predicate, formattedObject);
        }

        // Throw an exception if the axiom type is not supported
        throw new IllegalArgumentException("Unsupported axiom type for deletion: " + oldAxiom);
    }

    /**
     * Constructs a SPARQL INSERT query for a given OWLAxiom.
     * @param newAxiom The OWLAxiom to be inserted.
     * @return A SPARQL INSERT query string.
     */
    private String constructInsertQuery(OWLAxiom newAxiom) {
        if (newAxiom instanceof OWLClassAssertionAxiom) {
            // Handle class assertion axioms
            OWLClassAssertionAxiom classAxiom = (OWLClassAssertionAxiom) newAxiom;
            String subject = classAxiom.getIndividual().asOWLNamedIndividual().getIRI().toString();
            String object = classAxiom.getClassExpression().asOWLClass().getIRI().toString();
            return String.format("PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> " +
                    "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> " +
                    "INSERT DATA { GRAPH <%s> { <%s> rdf:type <%s> . } }",
                    this.graphDomain, subject, object);
        } else if (newAxiom instanceof OWLObjectPropertyAssertionAxiom) {
            // Handle object property assertion axioms
            OWLObjectPropertyAssertionAxiom propAxiom = (OWLObjectPropertyAssertionAxiom) newAxiom;
            String subject = propAxiom.getSubject().asOWLNamedIndividual().getIRI().toString();
            String predicate = propAxiom.getProperty().asOWLObjectProperty().getIRI().toString();
            String object = propAxiom.getObject().asOWLNamedIndividual().getIRI().toString();
            return String.format("INSERT DATA { GRAPH <%s> { <%s> <%s> <%s> . } }",
                    this.graphDomain, subject, predicate, object);
        } else if (newAxiom instanceof OWLDataPropertyAssertionAxiom) {
            // Handle data property assertion axioms
            OWLDataPropertyAssertionAxiom dataAxiom = (OWLDataPropertyAssertionAxiom) newAxiom;
            String subject = dataAxiom.getSubject().asOWLNamedIndividual().getIRI().toString();
            String predicate = dataAxiom.getProperty().asOWLDataProperty().getIRI().toString();
            OWLLiteral objectLiteral = dataAxiom.getObject();

            // Get lexical value
            String objectValue = objectLiteral.getLiteral();

            // Check if the literal has a language tag
            String languageTag = objectLiteral.getLang();

            // Get datatype
            OWLDatatype datatype = objectLiteral.getDatatype();
            String datatypeIRI = datatype.getIRI().toString();

            // Format literal correctly
            String formattedObject;
            if (!languageTag.isEmpty()) {
                formattedObject = String.format("\"%s\"@%s", objectValue, languageTag);
            } else if (!datatypeIRI.equals("http://www.w3.org/2001/XMLSchema#string")) {
                formattedObject = String.format("\"%s\"^^<%s>", objectValue, datatypeIRI);
            } else {
                formattedObject = String.format("\"%s\"", objectValue);
            }

            return String.format("INSERT DATA { GRAPH <%s> { <%s> <%s> %s . } }",
                    this.graphDomain, subject, predicate, formattedObject);
        }

        // Throw an exception if the axiom type is not supported
        throw new IllegalArgumentException("Unsupported axiom type for insertion: " + newAxiom);
    }

    @Override
    public void close() {
        try { if (repo != null) repo.shutDown(); } catch (Exception ignored) {}
        repo = null;
        tBoxHandler = null;
        graphDomain = null;
        pt = null;
    }
}