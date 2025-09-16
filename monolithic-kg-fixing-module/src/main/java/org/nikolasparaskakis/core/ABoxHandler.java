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



    public ConcurrentLinkedQueue<OWLAxiom> getABox() {
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

        // Stable ordering is important when paginating with OFFSET
        // Many stores can handle this; if it's too slow, consider batching by subjects via VALUES instead.
        String batchTemplate = "CONSTRUCT { ?s ?p ?o } " +
            "FROM <%s> " +
            "WHERE { ?s ?p ?o } " +
            "ORDER BY ?s ?p ?o " +
            "LIMIT %d " +
            "OFFSET %d";

        TurtleDocumentFormat format = new TurtleDocumentFormat();
        OWLOntologyLoaderConfiguration configuration = new OWLOntologyLoaderConfiguration();
        AnonymousNodeChecker checker = new AnonymousNodeCheckerImpl();
        RioOWLRDFConsumerAdapter consumer = new RioOWLRDFConsumerAdapter(ontology, checker, configuration);
        consumer.setOntologyFormat(format);

        int offset = 0;
        boolean more = true;

        try (RepositoryConnection conn = repo.getConnection()) {
            consumer.startRDF();
            while (more) {
                String query = String.format(batchTemplate, this.graphDomain, BATCH_SIZE, offset);

                int attempts = 0;
                boolean batchDone = false;
                int statementsInBatch = 0;

                while (attempts < this.MAX_RETRIES && !batchDone) {
                    try {
                        GraphQuery gq = conn.prepareGraphQuery(QueryLanguage.SPARQL, query);
                        try (GraphQueryResult res = gq.evaluate()) {
                            while (res.hasNext()) {
                                Statement st = res.next();
                                statementsInBatch++;
                                try {
                                    consumer.handleStatement(st);
                                } catch (Exception perStmt) {
                                    System.err.println("Skipping bad statement: " + st + " — " + perStmt.getMessage());
                                }
                            }
                        }
                        batchDone = true; // success
                    } catch (Exception e) {
                        attempts++;
                        System.err.println("Error executing SPARQL batch at offset " + offset + ": " + e.getMessage());
                        if (attempts < this.MAX_RETRIES) {
                            try {
                                Thread.sleep(RETRY_DELAY_MS);
                            } catch (InterruptedException ie) {
                                Thread.currentThread().interrupt();
                                more = false; // bail out entirely if interrupted
                                break;
                            }
                        } else {
                            System.err.println("Max retries reached for offset " + offset + ". Stopping.");
                            more = false; // give up paging
                        }
                    }
                }

                if (!batchDone) {
                    // We failed this batch after retries; stop paging
                    break;
                }

                // If we got fewer than BATCH_SIZE triples, we've reached the end.
                if (statementsInBatch < BATCH_SIZE) {
                    more = false;
                } else {
                    offset += BATCH_SIZE;
                }
            }
            consumer.endRDF();
        }

        // Build the module: all gathered axioms minus the TBox axioms
        ConcurrentLinkedQueue<OWLAxiom> module =
                ontology.axioms().distinct().collect(Collectors.toCollection(ConcurrentLinkedQueue::new));
        module.removeAll(tBoxHandler.getOntologyTBox());

        man.clearOntologies();
        return module;
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