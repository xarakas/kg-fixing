package org.nikolasparaskakis.onehopextractor;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.sparql.SPARQLRepository;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.model.parameters.Imports;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.semanticweb.owlapi.apibinding.OWLManager.createOWLOntologyManager;

public class OneHopExtractor {

    public static void main(String[] args) throws Exception {

        // Check if the right number of arguments was given
        if (args.length != 6) {
            System.err.println("Usage: OneHopExtractor " +
                    "<tBoxFilePath> " +
                    "<sparqlEndpoint> " +
                    "<graphDomain> " +
                    "<batchSize> " +
                    "<hdfsLocation> " +
                    "<outputDirectoryPath>");
            System.exit(-1);
        }

        // Parse the command line arguments
        String tBoxFilePath = args[0];
        String sparqlEndpoint = args[1];
        String graphDomain = args[2];
        int batchSize = Integer.parseInt(args[3]);
        String hdfsLocation = args[4];
        String outputDirectoryPath = args[5];

        // The output path of the one-hop links file
        String oneHopLinksFilepath = hdfsLocation + "/" + outputDirectoryPath + "/oneHopLinks";

        // Count the total number of triples in the triple store (ABox)
        long totalCount = countABoxAxioms(sparqlEndpoint, graphDomain);
        // Log the total number of triples in the triple store (ABox)
        System.out.println("Total count of ABox triples: " + totalCount);



        //////////////////////////////////
        /// Job 1: Extract 1-Hop Links ///
        //////////////////////////////////

        // Set the configuration for the MapReduce Job

        Configuration conf = new Configuration();

        conf.set("sparqlEndpoint", sparqlEndpoint);
        conf.setInt("batchSize", batchSize);
        conf.setLong("totalCount", totalCount);
        conf.set("graphDomain", graphDomain);

        // Get a set of owl axioms parsed from the TBox file
        Set<OWLAxiom> tBox = getOntologyTBox(tBoxFilePath);

        // Extract the leads and put them in the job configuration
        List<OWLEntity> leads = discoverLeadsForOPExt(tBox);
        conf.setStrings("leads", leads.stream()
                .map(entity -> entity.getIRI().toString()).toArray(String[]::new));

//        Set<OWLClass> classesInTBox = extractConceptsFromAxioms(tBox);
//        List<String> classesInTBoxStr = classesInTBox.stream()
//                .map(entity -> entity.getIRI().toString())
//                .collect(Collectors.toList());
//        conf1.setStrings("classesInTBox", classesInTBoxStr.toArray(new String[0]));

        // Extract the classes in TBox and put them in the job configuration
        Set<OWLClass> classesInTBox = extractConceptsFromAxioms(tBox);
        conf.setStrings("classesInTBox", classesInTBox.stream()
                .map(entity -> entity.getIRI().toString()).toArray(String[]::new));

        // Extract the predicates in TBox and put them in the job configuration
        Set<OWLEntity> predicatesInTBox = extractPropertiesFromAxioms(tBox);
        conf.setStrings("predicatesInTBox", predicatesInTBox.stream()
                .map(entity -> entity.getIRI().toString()).toArray(String[]::new));

        // Create Job and submit it for execution
        Job job = Job.getInstance(conf, "Job - Extract 1-Hop Links");
        job.setJarByClass(OneHopExtractor.class);
        job.setInputFormatClass(SparqlInputFormat.class);
        job.setMapperClass(OneHopMapper.class);
        job.setCombinerClass(OneHopCombiner.class);
        job.setReducerClass(OneHopReducer.class);
        job.setMapOutputKeyClass(Text.class);
        job.setMapOutputValueClass(Text.class);
        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(Text.class);
        FileOutputFormat.setOutputPath(job, new Path(oneHopLinksFilepath));

        // Wait for Job to complete, and check if it fails
        if (!job.waitForCompletion(true)) {
            System.err.println("Job (Extract 1-Hop Links) failed.");
            System.exit(1);
        }

        System.exit(0);
    }


    public static int countABoxAxioms(String sparqlEndpoint, String graphDomain) {

        SPARQLRepository repo = new SPARQLRepository(
                sparqlEndpoint+"/sparql",
                sparqlEndpoint+"/update"
        );
        RepositoryConnection conn = repo.getConnection();
        repo.init();

        // SPARQL query to count all triples in the ABox
        String query = "SELECT (COUNT(*) AS ?count) " +
                "FROM " + "<" + graphDomain + "> " +
                "WHERE { " +
                "  ?s ?p ?o . " +
                "}";

        final int RETRY_DELAY_MS = 3000; // Delay in milliseconds before retrying
        int count = 0;
        int attempts = 0;

        while (attempts < 5) {
            try {
                // Execute the query
                try (TupleQueryResult result = conn.prepareTupleQuery(query).evaluate()) {
                    if (result.hasNext()) {
                        BindingSet bindingSet = result.next();
                        count = Integer.parseInt(bindingSet.getValue("count").stringValue());
                        return count; // Success, return result
                    }
                }
            }
            catch (Exception e) {
                attempts++;
                System.err.println("Error executing count query (attempt " + attempts + "): " + e.getMessage());

                if (attempts < 5) {
                    try {
                        Thread.sleep(RETRY_DELAY_MS); // Wait before retrying
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt(); // Restore interrupted state
                        break;
                    }
                }
                else {
                    System.err.println("All retry attempts failed. Returning count as 0.");
                }
            }
        }

        conn.close();

        return count; // Return 0 if all attempts fail
    }



    public static List<OWLEntity> discoverLeadsForOPExt(Set<OWLAxiom> tBoxOntology) {

        OWLOntologyManager man_t;

        // The OWLDataFactory instance used to create OWL API constructs, such as classes, properties, and individuals.

        OWLOntology tBoxOnt;
        man_t = createOWLOntologyManager();
        try {
            tBoxOnt = man_t.createOntology(tBoxOntology);
        } catch (OWLOntologyCreationException e) {
            throw new RuntimeException(e);
        }

        // Initialize a list to store discovered object properties
        List<OWLEntity> leads = new ArrayList<>();

        // Discover object properties in nested class expressions (e.g., ObjectAllValuesFrom, ObjectSomeValuesFrom)
        // that might induce inconsistencies
        tBoxOnt.getAxioms().forEach(a -> {
            a.getNestedClassExpressions().forEach( b -> {
                // Check for relevant class expression types
                if ((b.getClassExpressionType()== ClassExpressionType.OBJECT_ALL_VALUES_FROM) ||
                        (b.getClassExpressionType()==ClassExpressionType.OBJECT_SOME_VALUES_FROM)) {

                    // Add object properties in the signature of these expressions
                    a.getObjectPropertiesInSignature().forEach(c -> {
                        if (!leads.contains(c)){
                            leads.add(c);
                        }
                    });
                }
            });
        });

        // Discover additional object properties from RBox axioms that might induce inconsistencies
        tBoxOnt.getRBoxAxioms(Imports.INCLUDED).forEach(a -> {
            a.getObjectPropertiesInSignature().forEach(c -> {
                if (!leads.contains(c)) {
                    leads.add(c);
                }
            });
        });

        // Return the list of discovered object properties
        return leads;
    }



    public static Set<OWLAxiom> getOntologyTBox(String tBoxFilePath) {

        OWLOntology tBoxFromFile;
        Set<OWLAxiom> tBox;
        Set<OWLAxiom> rBox;
        Set<OWLAxiom> declarationsTBox;
        Set<OWLAxiom> otherTBox;

        OWLOntologyManager man_t;

        man_t = createOWLOntologyManager();

        File tBoxFile = new File(tBoxFilePath);

        try {
            tBoxFromFile = man_t.loadOntologyFromOntologyDocument(tBoxFile);
        } catch (OWLOntologyCreationException e) {
            throw new RuntimeException(e);
        }

        // Extract the TBox axioms and create a new ontology for them
        tBox = tBoxFromFile.tboxAxioms(Imports.INCLUDED).collect(Collectors.toSet());

        // Extract the RBox axioms and create a new ontology for them
        rBox = tBoxFromFile.rboxAxioms(Imports.INCLUDED).collect(Collectors.toSet());

        // Extract declaration axioms and create a new ontology for them
        declarationsTBox = getDeclarationAxioms(tBoxFromFile);

        otherTBox = getOtherAxioms(tBoxFromFile.axioms().collect(Collectors.toSet()), tBox, rBox, declarationsTBox);

        Set<OWLAxiom> ontology = new HashSet<>();

        ontology.addAll(declarationsTBox);

        ontology.addAll(tBox);

        ontology.addAll(rBox);

        ontology.addAll(otherTBox);

        ontology.removeIf(ax -> ax.getAxiomType().equals(AxiomType.ANNOTATION_ASSERTION));

        return ontology;
    }



    public static Set<OWLAxiom> getDeclarationAxioms(OWLOntology ontology) {
        // Stream through all axioms in the ontology
        return ontology.axioms()
                // Filter to include only declaration axioms
                .filter(axiom -> axiom instanceof OWLDeclarationAxiom)
                // Cast the filtered axioms to OWLDeclarationAxiom
                .map(axiom -> (OWLDeclarationAxiom) axiom)
                // Collect the results into a set
                .collect(Collectors.toSet());
    }



    public static Set<OWLAxiom> getOtherAxioms(Set<OWLAxiom> tBoxFromFile, Set<OWLAxiom> tBox, Set<OWLAxiom> rBox, Set<OWLAxiom> declarationsTBox) {

        Set<OWLAxiom> otherTBox = new HashSet<>();

        HashSet<AxiomType> mainAxiomTypes = new HashSet<>();

        mainAxiomTypes.addAll(extractAxiomTypes(tBox));
        mainAxiomTypes.addAll(extractAxiomTypes(rBox));
        mainAxiomTypes.addAll(extractAxiomTypes(declarationsTBox));

        HashSet<AxiomType> otherAxiomTypes = new HashSet<>(extractAxiomTypes(tBoxFromFile));
        otherAxiomTypes.removeAll(mainAxiomTypes);

        for (AxiomType<?> at : otherAxiomTypes) {
            Set<OWLAxiom> tmp = tBoxFromFile.stream().filter(ax -> ax.getAxiomType().equals(at)).collect(Collectors.toSet());
            if(!tmp.isEmpty())
                otherTBox.addAll(tmp);
        }

        return otherTBox;
    }



    public static Set<AxiomType> extractAxiomTypes(Set<OWLAxiom> axioms) {

        // Initialize a set to store the unique types of axioms
        Set<AxiomType> axiomTypes = new HashSet<>();

        // Iterate through each axiom in the input set
        for (OWLAxiom axiom : axioms) {
            // Add the name of the axiom type to the set
            axiomTypes.add(axiom.getAxiomType());
        }

        // Return the set of axiom types
        return axiomTypes;
    }



    public static Set<OWLClass> extractConceptsFromAxioms(Set<OWLAxiom> axioms) {

        // Create a set to store the extracted OWL classes (concepts)
        Set<OWLClass> concepts = new HashSet<>();

        // Iterate through each axiom in the input set
        for (OWLAxiom axiom : axioms) {

            // Retrieve all entities in the axiom's signature
            Set<OWLEntity> entities = axiom.getSignature();

            // Filter and process only OWL classes with absolute IRIs
            for (OWLEntity entity : entities) {
                if (entity.isOWLClass() && entity.getIRI().isAbsolute()) {

                    // Cast the entity to OWLClass
                    OWLClass owlClass = entity.asOWLClass();

                    if (!owlClass.isOWLThing()) {
                        concepts.add(owlClass);
                    }
                }
            }
        }

        // Return the set of extracted OWL classes (concepts)
        return concepts;
    }



    public static Set<OWLEntity> extractPropertiesFromAxioms(Set<OWLAxiom> axioms) {

        // Create a set to store the extracted properties
        Set<OWLEntity> properties = new HashSet<>();

        // Iterate through each axiom in the input set
        for (OWLAxiom axiom : axioms) {

            // Retrieve all entities in the axiom's signature
            Set<OWLEntity> entities = axiom.getSignature();

            // Filter and add only object or data properties to the result set
            for (OWLEntity entity : entities) {
                if (entity.isOWLObjectProperty() || entity.isOWLDataProperty()) {
                    properties.add(entity);
                }
            }
        }

        // Return the set of extracted properties
        return properties;
    }
}
