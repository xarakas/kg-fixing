package org.nikolasparaskakis.pipeline;

import com.beust.jcommander.JCommander;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Counter;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.sparql.SPARQLRepository;
import org.nikolasparaskakis.deduplicator.DedupMapper;
import org.nikolasparaskakis.deduplicator.DedupReducer;
import org.nikolasparaskakis.equalseteliminator.EqualSetMapper;
import org.nikolasparaskakis.equalseteliminator.EqualSetReducer;
import org.nikolasparaskakis.khopextractor.KHopMapper;
import org.nikolasparaskakis.khopextractor.KHopReducer;
import org.nikolasparaskakis.onehopextractor.OneHopCombiner;
import org.nikolasparaskakis.onehopextractor.SparqlInputFormat;
import org.nikolasparaskakis.onehopextractor.OneHopMapper;
import org.nikolasparaskakis.onehopextractor.OneHopReducer;
import org.nikolasparaskakis.subseteliminator.SetMapper;
import org.nikolasparaskakis.subseteliminator.SetReducer;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.model.parameters.Imports;
import org.semanticweb.owlapi.vocab.OWLRDFVocabulary;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static org.semanticweb.owlapi.apibinding.OWLManager.createOWLOntologyManager;

public class PipelineKHopExtractor {

    static Logger logger = Logger.getLogger(PipelineKHopExtractor.class.getName());

    public static void main(String[] args) throws Exception {

        KHopExtractorArgs c_args = new KHopExtractorArgs();
        JCommander jc = new JCommander(c_args);
        try {
            jc.parse(args);
        }
        catch (Exception e) {
            jc.usage();
            System.exit(0);
        }
        if (c_args.help) {
            jc.usage();
            System.exit(0);
        }

        String tBoxFilePath = c_args.tBoxFilePath;
        String sparqlEndpoint = c_args.sparqlEndpoint;
        String graphDomain = c_args.graphDomain;
        String hdfsLocation = c_args.hdfsLocation;
        String outputDirectoryPath = c_args.outputDirectoryPath;
        String inputDirectoryPath = c_args.inputDirectoryPath;
        boolean debugLogs = c_args.debugLogs;
        int batchSize = c_args.batchSize;
        int maxHops = c_args.maxHops;
        int maxIterations = c_args.maxIterations;
        int reducerTasks = c_args.reducerTasks;
        boolean eliminateEqualSets = c_args.eliminateEqualSets;
        boolean eliminateSubsets = c_args.eliminateSubsets;

        Configuration conf = new Configuration();
        FileSystem fs = FileSystem.get(new URI(hdfsLocation), conf);

        // The output path of the one-hop links file
        String oneHopLinksFilepath = hdfsLocation + "/" + outputDirectoryPath + "/oneHopLinks";

        // The output path of the k-hop links file
        String kHopLinksFilepath = hdfsLocation + "/" + outputDirectoryPath + "/kHopLinks";

        String tmpFilepath = hdfsLocation + "/" + outputDirectoryPath + "/tmp";

        // Count the total number of triples in the triple store (ABox)
        long totalCount = countABoxAxioms(sparqlEndpoint, graphDomain);
        // Log the total number of triples in the triple store (ABox)
        if (c_args.debugLogs) {
            logger.info("Total count of ABox triples: " + totalCount);
        }




        long startTime = System.currentTimeMillis();

        //////////////////////////////////
        /// Job 1: Extract 1-Hop Links ///
        //////////////////////////////////

        if (inputDirectoryPath == null) {

            Configuration conf1 = new Configuration();

            conf1.set("sparqlEndpoint", sparqlEndpoint);
            conf1.setInt("batchSize", batchSize);
            conf1.setLong("totalCount", totalCount);
            conf1.set("graphDomain", graphDomain);


            // Get a set of owl axioms parsed from the TBox file
            Set<OWLAxiom> tBox = getOntologyTBox(tBoxFilePath);

            // Extract the leads and put them in the job configuration
            List<OWLEntity> leads = discoverLeadsForOPExt(tBox);
            conf1.setStrings("leads", leads.stream()
                    .map(entity -> entity.getIRI().toString()).toArray(String[]::new));

            // Extract the classes in TBox and put them in the job configuration
            Set<OWLClass> classesInTBox = extractConceptsFromAxioms(tBox);
            conf1.setStrings("classesInTBox", classesInTBox.stream()
                    .map(entity -> entity.getIRI().toString()).toArray(String[]::new));

            // Extract the predicates in TBox and put them in the job configuration
            Set<OWLEntity> predicatesInTBox = extractPropertiesFromAxioms(tBox);
            conf1.setStrings("predicatesInTBox", predicatesInTBox.stream()
                    .map(entity -> entity.getIRI().toString()).toArray(String[]::new));

            // Create Job 1 and submit it for execution
            Job job1 = Job.getInstance(conf1, "Job 1 - Extract 1-Hop Links");
            job1.setJarByClass(PipelineKHopExtractor.class);
            job1.setInputFormatClass(SparqlInputFormat.class);
            job1.setMapperClass(OneHopMapper.class);
            job1.setCombinerClass(OneHopCombiner.class);
            job1.setReducerClass(OneHopReducer.class);
            job1.setNumReduceTasks(reducerTasks);
            job1.setMapOutputKeyClass(Text.class);
            job1.setMapOutputValueClass(Text.class);
            job1.setOutputKeyClass(Text.class);
            job1.setOutputValueClass(Text.class);
            FileOutputFormat.setOutputPath(job1, new Path(oneHopLinksFilepath));

            // Wait for Job 1 to complete, and check if it fails
            if (!job1.waitForCompletion(true)) {
                logger.severe("Job 1 (Extract 1-Hop Links) failed.");
                System.exit(1);
            }
        }



        //////////////////////////////////
        /// Job 2: Extract k-Hop Links ///
        //////////////////////////////////

        if (maxHops > 1) {
            int iteration = 0;

            String in;
            if (inputDirectoryPath != null) {
                in = hdfsLocation + "/" + inputDirectoryPath;
            }
            else {
                in = oneHopLinksFilepath;
            }

            String out;

            while (iteration < maxIterations) {
                out = hdfsLocation + "/" + outputDirectoryPath + "/kHopLinksIter" + iteration;
                boolean hasUpdates = runSingleIteration(in, out, maxHops, iteration, reducerTasks);
                iteration++;
                if (!hasUpdates) {
                    logger.info("Convergence reached at iteration " + (iteration - 1));
                    break;
                }
                in = out;
            }

            iteration = iteration - 1;

            String lastIterOutput = hdfsLocation + "/" + outputDirectoryPath + "/kHopLinksIter" + iteration;

            // Cleanup intermediate files
            for (int i = 0; i < iteration; i++) {
                Path intermediate = new Path(hdfsLocation + "/" + outputDirectoryPath + "/kHopLinksIter" + i);
                if (fs.exists(intermediate)) {
                    if (!fs.delete(intermediate, true))
                        logger.severe("Failed to delete intermediate output " + intermediate);
                }
            }

            // Rename final result
            Path finalPath = new Path(lastIterOutput);
            Path renamedPath = new Path(kHopLinksFilepath);
            if (!fs.rename(finalPath, renamedPath)) {
                logger.severe("Failed to rename final output to: " + renamedPath);
            }
        }
        else {
            Path srcPath = new Path(oneHopLinksFilepath);
            Path destPath = new Path(kHopLinksFilepath);

            // Perform a recursive copy of oneHopLinks to kHopLinks
            FileSystem srcFs = srcPath.getFileSystem(conf);
            FileSystem destFs = destPath.getFileSystem(conf);

            boolean success = org.apache.hadoop.fs.FileUtil.copy(
                    srcFs, srcPath,
                    destFs, destPath,
                    false,
                    conf
            );

            if (success) {
                logger.info("Successfully copied oneHopLinks to kHopLinks.");
            } else {
                logger.severe("Failed to copy oneHopLinks to kHopLinks.");
                System.exit(1);
            }
        }

        if (eliminateEqualSets)
        {
            Configuration conf5 = new Configuration();

            // ---------------------- Job 1 ----------------------
            Job job5 = Job.getInstance(conf5, "Equivalent Sets Elimination");
            job5.setJarByClass(PipelineKHopExtractor.class);
            job5.setMapperClass(EqualSetMapper.class);
            job5.setReducerClass(EqualSetReducer.class);
            job5.setNumReduceTasks(reducerTasks);
            job5.setMapOutputKeyClass(Text.class);
            job5.setMapOutputValueClass(Text.class);
            job5.setOutputKeyClass(Text.class);
            job5.setOutputValueClass(Text.class);

            FileInputFormat.addInputPath(job5, new Path(kHopLinksFilepath));
            FileOutputFormat.setOutputPath(job5, new Path(tmpFilepath));

            if (!job5.waitForCompletion(true)) {
                System.exit(1);
            }

            if (fs.exists(new Path(kHopLinksFilepath))) {
                fs.delete(new Path(kHopLinksFilepath), true);
            }

            Path finalPath = new Path(tmpFilepath);
            Path renamedPath = new Path(kHopLinksFilepath);
            if (!fs.rename(finalPath, renamedPath)) {
                logger.severe("Failed to rename final output to: " + renamedPath);
            }
        }

        if (eliminateSubsets) {
            Configuration conf3 = new Configuration();

            // ---------------------- Job 1 ----------------------
            Job job3 = Job.getInstance(conf3, "Subset Elimination");
            job3.setJarByClass(PipelineKHopExtractor.class);
            job3.setMapperClass(SetMapper.class);
            job3.setReducerClass(SetReducer.class);
            job3.setNumReduceTasks(reducerTasks);
            job3.setMapOutputKeyClass(Text.class);
            job3.setMapOutputValueClass(Text.class);
            job3.setOutputKeyClass(Text.class);
            job3.setOutputValueClass(Text.class);

            FileInputFormat.addInputPath(job3, new Path(kHopLinksFilepath));
            Path tmp1Path = new Path(tmpFilepath);
            FileOutputFormat.setOutputPath(job3, tmp1Path);

            if (!job3.waitForCompletion(true)) {
                System.exit(1);
            }

            if (fs.exists(new Path(kHopLinksFilepath))) {
                fs.delete(new Path(kHopLinksFilepath), true);
            }

            Path finalPath = new Path(tmpFilepath);
            Path renamedPath = new Path(kHopLinksFilepath);
            if (!fs.rename(finalPath, renamedPath)) {
                logger.severe("Failed to rename final output to: " + renamedPath);
            }

            // ---------------------- Job 2 ----------------------
            Job job4 = Job.getInstance(conf, "Final Deduplication");
            job4.setJarByClass(PipelineKHopExtractor.class);
            job4.setMapperClass(DedupMapper.class);
            job4.setReducerClass(DedupReducer.class);
            job4.setNumReduceTasks(reducerTasks);
            job4.setMapOutputKeyClass(Text.class);
            job4.setMapOutputValueClass(Text.class);
            job4.setOutputKeyClass(Text.class);
            job4.setOutputValueClass(NullWritable.class);

            FileInputFormat.addInputPath(job4, new Path(kHopLinksFilepath));
            FileOutputFormat.setOutputPath(job4, new Path(tmpFilepath));

            if (!job4.waitForCompletion(true)) {
                System.exit(1);
            }

            if (fs.exists(new Path(kHopLinksFilepath))) {
                fs.delete(new Path(kHopLinksFilepath), true);
            }

            finalPath = new Path(tmpFilepath);
            renamedPath = new Path(kHopLinksFilepath);
            if (!fs.rename(finalPath, renamedPath)) {
                logger.severe("Failed to rename final output to: " + renamedPath);
            }
        }

        long endTime = System.currentTimeMillis();
        long totalTime = endTime - startTime;
        System.out.println("(H) Total time elapsed: " + totalTime + "ms");

        System.exit(0);
    }



    private static boolean runSingleIteration(String inputPath, String outputPath, int maxHops, int iteration, int reducerTasks) throws Exception {

        Configuration conf = new Configuration();

        if (maxHops == -1) {
            conf.setInt("maxHops", Integer.MAX_VALUE);
        } else {
            conf.setInt("maxHops", maxHops);
        }

        conf.setInt("iterationNumber", iteration);

        Job job = Job.getInstance(conf, "Job 2 - Extract K-Hop Links Iteration " + iteration);
        job.setJarByClass(PipelineKHopExtractor.class);
        job.setMapperClass(KHopMapper.class);
        job.setReducerClass(KHopReducer.class);
        job.setNumReduceTasks(reducerTasks);
        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(Text.class);

        FileInputFormat.addInputPath(job, new Path(inputPath));
        FileOutputFormat.setOutputPath(job, new Path(outputPath));

        boolean success = job.waitForCompletion(true);
        if (!success)
            return false;

        Counter counter = job.getCounters().findCounter("KHop", "NUM_UPDATES");
        long updates = counter.getValue();

        return updates > 0;
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

        OWLOntology tBoxOnt = null;
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
                if ((b.getClassExpressionType()==ClassExpressionType.OBJECT_ALL_VALUES_FROM) ||
                        (b.getClassExpressionType()==ClassExpressionType.OBJECT_SOME_VALUES_FROM)) {

                    // Add object properties in the signature of these expressions
                    a.getObjectPropertiesInSignature().forEach(c -> {
                        if (!leads.contains(c)) {
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

        OWLDataFactory df = OWLManager.getOWLDataFactory();
        OWLObjectProperty sameAs = df.getOWLObjectProperty(OWLRDFVocabulary.OWL_SAME_AS.getIRI());
        OWLObjectProperty differentFrom = df.getOWLObjectProperty(OWLRDFVocabulary.OWL_DIFFERENT_FROM.getIRI());

        leads.add(sameAs);
        leads.add(differentFrom);


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

        // The OWLDataFactory instance used to create OWL API constructs, such as classes, properties, and individuals.

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

        OWLDataFactory df = OWLManager.getOWLDataFactory();
        OWLObjectProperty sameAs = df.getOWLObjectProperty(OWLRDFVocabulary.OWL_SAME_AS.getIRI());
        OWLObjectProperty differentFrom = df.getOWLObjectProperty(OWLRDFVocabulary.OWL_DIFFERENT_FROM.getIRI());

        properties.add(sameAs);
        properties.add(differentFrom);

        // Return the set of extracted properties
        return properties;
    }
}