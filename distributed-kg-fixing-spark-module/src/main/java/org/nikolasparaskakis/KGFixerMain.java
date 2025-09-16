package org.nikolasparaskakis;



import com.beust.jcommander.JCommander;
import org.apache.spark.TaskContext;
import org.apache.spark.sql.*;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import java.time.LocalDateTime;
import org.nikolasparaskakis.analytics.Queries;
import org.nikolasparaskakis.core.*;
import org.nikolasparaskakis.io.HDFSIO;
import org.nikolasparaskakis.io.PartitionLogger;
import org.nikolasparaskakis.logs.bin.*;
import org.nikolasparaskakis.logs.global.FixesLog;
import org.nikolasparaskakis.logs.module.*;
import org.nikolasparaskakis.core.ModuleData;
import org.semanticweb.owlapi.model.*;
import java.util.*;
import static org.nikolasparaskakis.core.Bin.assignModulesToBins;
import static org.nikolasparaskakis.utils.Helper.*;



/**
 * Main class for the KGFixer application.
 * This class sets up the Spark environment, parses command line arguments,
 * and executes the main logic of the application based on the specified mode.
 */
@SuppressWarnings({"unused", "DuplicatedCode", "UnusedAssignment"})
public class KGFixerMain {

    public static void main(String[] args) {

        // Parse command line arguments
        KGFixerArgs c_args = new KGFixerArgs();
        JCommander jc = new JCommander(c_args);
        try {
            jc.parse(args);
        } catch (Exception e) {
            jc.usage();
            System.exit(0);
        }
        if (c_args.help) {
            jc.usage();
            System.exit(0);
        }

        // Print all parameters
        System.out.println(c_args.getAllParams());

        // Read parameters
        boolean debugLogs = c_args.debugLogs;
        String tBoxFilePath = c_args.tBoxFilePath;
        String sparqlEndpoint = c_args.sparqlEndpoint;
        String graphDomain = c_args.graphDomain;
        String hdfsLocation = c_args.hdfsLocation;
        String inputDirectoryPath = c_args.inputDirectoryPath;
        String outputDirectoryPath = c_args.outputDirectoryPath;
        int partitionsNumber = c_args.partitionsNumber;
        boolean saveInitialOntology = c_args.saveInitialOntology;
        boolean saveFixedOntology = c_args.saveFixedOntology;
        boolean saveModule = c_args.saveModules;
        String logFilename = c_args.logFilename;
        int mode = c_args.mode;
        int reasonerSelection = c_args.reasonerSelection;
        int fixSelection = c_args.fixSelection;
        boolean mcd = c_args.mcd;
        boolean extend = c_args.extend;
        boolean printInconsistencies = c_args.printInconsistencies;
        long perOpTimeoutMillis = c_args.perOpTimeoutMillis;
        long reasonerTimeoutMillis = c_args.reasonerTimeoutMillis;
        long fixingTimeoutMillis = c_args.fixingTimeoutMillis;
        int explanationsLimit = c_args.explanationsLimit;
        int outerRound = c_args.outerRound;
        int limitInTop = c_args.limitInTop;
        int binCapacity = c_args.binCapacity;

        // Create an HDFSIO object for setup tasks
        HDFSIO hdfsIOForSetup = new HDFSIO(hdfsLocation);

        // Define input  file path
        String kHopLinksFilePath = hdfsLocation + "/" + inputDirectoryPath + "/kHopLinks";
        String inputPositionTrackerFilePath = inputDirectoryPath + "/position-tracker.json";

        // Define output file paths
        String mergedFixesFilePath = outputDirectoryPath + "/merged-fixes.ndjson";
        String mergedExplanationsFilePath = outputDirectoryPath + "/merged-explanations.ndjson";
        String sparkTotalTimeFilePath = outputDirectoryPath + "/" + "spark-total-time.txt";
        String stopLoopFlagFilePath = outputDirectoryPath + "/" + "stop-loop-flag.txt";
        String explanationsNumberFilePath = outputDirectoryPath + "/" + "explanations-number.txt";
        String outputPositionTrackerFilePath = outputDirectoryPath + "/position-tracker.json";

        // Define output directories

        boolean successfullyCreated;
        boolean successfullyDeleted;

        // Create the directory for ModuleEventLogs
        String moduleEventLogsDir = outputDirectoryPath + "/moduleEventLogs";
        successfullyDeleted = hdfsIOForSetup.deleteDirectory(moduleEventLogsDir);
        successfullyCreated = hdfsIOForSetup.createDirectory(moduleEventLogsDir);

        if (!successfullyCreated) {
            System.err.println("Failed to create directory: " + moduleEventLogsDir);
            System.exit(1);
        }

        // Create the directory for BinEventLogs
        String binEventLogsDir = outputDirectoryPath + "/binEventLogs";
        successfullyDeleted = hdfsIOForSetup.deleteDirectory(binEventLogsDir);
        successfullyCreated = hdfsIOForSetup.createDirectory(binEventLogsDir);

        if (!successfullyCreated) {
            System.err.println("Failed to create directory: " + binEventLogsDir);
            System.exit(1);
        }

        // Create the directory for ModuleExplanationsLogs
        String moduleExplanationsLogsDir = outputDirectoryPath + "/moduleExplanationsLogs";
        successfullyDeleted = hdfsIOForSetup.deleteDirectory(moduleExplanationsLogsDir);
        successfullyCreated = hdfsIOForSetup.createDirectory(moduleExplanationsLogsDir);

        if (!successfullyCreated) {
            System.err.println("Failed to create directory: " + moduleExplanationsLogsDir);
            System.exit(1);
        }

        // Create the directory for BinExplanationsLogs
        String binExplanationsLogsDir = outputDirectoryPath + "/binExplanationsLogs";
        successfullyDeleted = hdfsIOForSetup.deleteDirectory(binExplanationsLogsDir);
        successfullyCreated = hdfsIOForSetup.createDirectory(binExplanationsLogsDir);

        if (!successfullyCreated) {
            System.err.println("Failed to create directory: " + binExplanationsLogsDir);
            System.exit(1);
        }

        // Create the directory for ModuleFixesLogs
        String moduleFixesLogsDir = outputDirectoryPath + "/moduleFixesLogs";
        successfullyDeleted = hdfsIOForSetup.deleteDirectory(moduleFixesLogsDir);
        successfullyCreated = hdfsIOForSetup.createDirectory(moduleFixesLogsDir);

        if (!successfullyCreated) {
            System.err.println("Failed to create directory: " + moduleFixesLogsDir);
            System.exit(1);
        }

        // Create the directory for BinFixesLogs
        String binFixesLogsDir = outputDirectoryPath + "/binFixesLogs";
        successfullyDeleted = hdfsIOForSetup.deleteDirectory(binFixesLogsDir);
        successfullyCreated = hdfsIOForSetup.createDirectory(binFixesLogsDir);

        if (!successfullyCreated) {
            System.err.println("Failed to create directory: " + binFixesLogsDir);
            System.exit(1);
        }

        // Create the directory for ModuleInnerRoundNumLogs
        String moduleInnerRoundNumLogsDir = outputDirectoryPath + "/moduleInnerRoundNumLogs";
        successfullyDeleted = hdfsIOForSetup.deleteDirectory(moduleInnerRoundNumLogsDir);
        successfullyCreated = hdfsIOForSetup.createDirectory(moduleInnerRoundNumLogsDir);

        if (!successfullyCreated) {
            System.err.println("Failed to create directory: " + moduleInnerRoundNumLogsDir);
            System.exit(1);
        }

        // Create the directory for BinInnerRoundNumLogs
        String binInnerRoundNumLogsDir = outputDirectoryPath + "/binInnerRoundNumLogs";
        successfullyDeleted = hdfsIOForSetup.deleteDirectory(binInnerRoundNumLogsDir);
        successfullyCreated = hdfsIOForSetup.createDirectory(binInnerRoundNumLogsDir);

        if (!successfullyCreated) {
            System.err.println("Failed to create directory: " + binInnerRoundNumLogsDir);
            System.exit(1);
        }

        // Create the directory for ModuleReasoningLogs
        String moduleReasoningLogsDir = outputDirectoryPath + "/moduleReasoningLogs";
        successfullyDeleted = hdfsIOForSetup.deleteDirectory(moduleReasoningLogsDir);
        successfullyCreated = hdfsIOForSetup.createDirectory(moduleReasoningLogsDir);

        if (!successfullyCreated) {
            System.err.println("Failed to create directory: " + moduleReasoningLogsDir);
            System.exit(1);
        }

        // Create the directory for BinReasoningLogs
        String binReasoningLogsDir = outputDirectoryPath + "/binReasoningLogs";
        successfullyDeleted = hdfsIOForSetup.deleteDirectory(binReasoningLogsDir);
        successfullyCreated = hdfsIOForSetup.createDirectory(binReasoningLogsDir);

        if (!successfullyCreated) {
            System.err.println("Failed to create directory: " + binReasoningLogsDir);
            System.exit(1);
        }

        // Create the directory for ModuleSizeLogs
        String moduleSizeLogsDir = outputDirectoryPath + "/moduleSizeLogs";
        successfullyDeleted = hdfsIOForSetup.deleteDirectory(moduleSizeLogsDir);
        successfullyCreated = hdfsIOForSetup.createDirectory(moduleSizeLogsDir);

        if (!successfullyCreated) {
            System.err.println("Failed to create directory: " + moduleSizeLogsDir);
            System.exit(1);
        }

        // Create the directory for BinSizeLogs
        String binSizeLogsDir = outputDirectoryPath + "/binSizeLogs";
        successfullyDeleted = hdfsIOForSetup.deleteDirectory(binSizeLogsDir);
        successfullyCreated = hdfsIOForSetup.createDirectory(binSizeLogsDir);

        if (!successfullyCreated) {
            System.err.println("Failed to create directory: " + binSizeLogsDir);
            System.exit(1);
        }

        // Create the directory for analytics
        String analyticsDir = outputDirectoryPath + "/analytics";
        successfullyDeleted = hdfsIOForSetup.deleteDirectory(analyticsDir);
        successfullyCreated = hdfsIOForSetup.createDirectory(analyticsDir);

        if (!successfullyCreated) {
            System.err.println("Failed to create directory: " + analyticsDir);
            System.exit(1);
        }

        // Configure Spark
        SparkConf conf = new SparkConf()
            .setAppName("KGFixer")
            .set("spark.task.maxFailures", "5");

        // Create Spark context
        try (JavaSparkContext sc = new JavaSparkContext(conf)) {

            if (mode == 1) {
                // --------------------------------
                // Mode 1 (Log module sizes)
                // --------------------------------

                // Create an HDFSIO object
                HDFSIO hdfsIO = new HDFSIO(hdfsLocation);

                // Create an RDD
                JavaRDD<String> rdd = sc.textFile(kHopLinksFilePath, partitionsNumber);

                //  Get start time in millis
                long startTimeMillis = System.currentTimeMillis();
                // Get start date time
                LocalDateTime startDateTime = LocalDateTime.now();

                // Write the start date time to hdfs
                hdfsIO.writeStringToHDFS("Start date time by Spark: " + startDateTime, sparkTotalTimeFilePath);
                System.out.println("Start date time by Spark: " + startDateTime);

                // For each partition in the RDD
                rdd.foreachPartition((Iterator<String> partitionIterator) -> {
                    // Define the partition file name
                    String partitionFileName = "partition_" + TaskContext.getPartitionId() + ".ndjson";

                    // Create TBoxHandler and ABoxHandler objects
                    TBoxHandler tBoxHandler = new TBoxHandler(tBoxFilePath);
                    try (ABoxHandler aBoxHandler = new ABoxHandler(sparqlEndpoint, graphDomain, tBoxHandler, new PositionTracker())) {

                        // Create an HDFSIO object
                        HDFSIO partitionHdfsIO = new HDFSIO(hdfsLocation);

                        try (PartitionLogger logger = new PartitionLogger(
                                partitionHdfsIO,
                                moduleEventLogsDir,
                                moduleExplanationsLogsDir,
                                moduleFixesLogsDir,
                                moduleInnerRoundNumLogsDir,
                                moduleReasoningLogsDir,
                                moduleSizeLogsDir,
                                binEventLogsDir,
                                binExplanationsLogsDir,
                                binFixesLogsDir,
                                binInnerRoundNumLogsDir,
                                binReasoningLogsDir,
                                binSizeLogsDir,
                                partitionFileName,
                                1000
                        )) {
                            // For each line in the partition we process
                            while (partitionIterator.hasNext()) {
                                // Read the next line from the partition
                                String line = partitionIterator.next();

                                // Extract the base individual from the input line
                                String baseIndividualStr = extractBaseIndividualStr(line);

                                // Get the neighbors of the base individual and their distance
                                HashMap<String, Integer> neighborsMap = extractNeighborsMap(line);

                                // Construct a list with the base individual and its neighbors if extend is enabled
                                ArrayList<String> allIndividuals = new ArrayList<>();
                                allIndividuals.add(baseIndividualStr);
                                if (extend) {
                                    allIndividuals.addAll(neighborsMap.keySet());
                                }

                                // Write event log STARTED_COUNTING_TRIPLES_OF_MODULE for the current module
                                ModuleEventLog startedCountingTriplesOfModuleEventLog = new ModuleEventLog(baseIndividualStr, ModuleEvent.STARTED_COUNTING_TRIPLES_OF_MODULE, LocalDateTime.now());
                                logger.addModuleEventLog(startedCountingTriplesOfModuleEventLog.toJsonString());
                                // Print in stdout the STARTED_COUNTING_TRIPLES_OF_MODULE event
                                System.out.println(startedCountingTriplesOfModuleEventLog.toJsonString());

                                // Get module size
                                int moduleSize = aBoxHandler.countIndividualTriples(allIndividuals);

                                // Write event log ENDED_COUNTING_TRIPLES_OF_MODULE for the current module
                                ModuleEventLog endedCountingTriplesOfModuleEventLog = new ModuleEventLog(baseIndividualStr, ModuleEvent.ENDED_COUNTING_TRIPLES_OF_MODULE, LocalDateTime.now());
                                logger.addModuleEventLog(endedCountingTriplesOfModuleEventLog.toJsonString());
                                // Print in stdout the ENDED_COUNTING_TRIPLES_OF_MODULE event
                                System.out.println(endedCountingTriplesOfModuleEventLog.toJsonString());

                                // Write module size log for the current module
                                ModuleSizeLog moduleSizeLog = new ModuleSizeLog(baseIndividualStr, outerRound, moduleSize);
                                logger.addModuleSizeLog(moduleSizeLog.toJsonString());
                            }
                        }
                    }
                });

                //  Get end time in millis
                long endTimeMillis = System.currentTimeMillis();
                // Get end date time
                LocalDateTime endDateTime = LocalDateTime.now();

                // Calculate total time in millis
                long totalTimeMillis = endTimeMillis - startTimeMillis;

                // Write the end date time to hdfs
                hdfsIO.writeStringToHDFS("End date time by Spark: " + endDateTime, sparkTotalTimeFilePath);
                System.out.println("End date time by Spark: " + endDateTime);

                // Write the total time in millis to hdfs
                hdfsIO.writeStringToHDFS("Total time elapsed by Spark: " + totalTimeMillis + " ms", sparkTotalTimeFilePath);
                System.out.println("Total time elapsed by Spark: " + totalTimeMillis + " ms");

                // Write stop flag to hdfs
                hdfsIO.writeStringToHDFS("stop", stopLoopFlagFilePath);
                System.out.println("Writing stop flag to hdfs.");
            } else if (mode == 2) {
                // -----------------------------------------
                // Mode 2 (Log module sizes and bin sizes)
                // -----------------------------------------

                // Create an HDFSIO object
                HDFSIO hdfsIO = new HDFSIO(hdfsLocation);

                // Create an RDD
                JavaRDD<String> rdd = sc.textFile(kHopLinksFilePath, partitionsNumber);

                // Get start time in millis
                long startTimeMillis = System.currentTimeMillis();
                // Get start date time
                LocalDateTime startDateTime = LocalDateTime.now();

                // Write the start time in millis to hdfs
                hdfsIO.writeStringToHDFS("Start date time by Spark: " + startDateTime, sparkTotalTimeFilePath);
                System.out.println("Start date time by Spark: " + startDateTime);

                // For each partition in the RDD
                rdd.foreachPartition((Iterator<String> partitionIterator) -> {
                    // Define the partition file name
                    String partitionFileName = "partition_" + TaskContext.getPartitionId() + ".ndjson";

                    // Create TBoxHandler and ABoxHandler objects
                    TBoxHandler tBoxHandler = new TBoxHandler(tBoxFilePath);
                    try (ABoxHandler aBoxHandler = new ABoxHandler(sparqlEndpoint, graphDomain, tBoxHandler, new PositionTracker())) {

                        // Create an HDFSIO object
                        HDFSIO partitionHdfsIO = new HDFSIO(hdfsLocation);

                        // Initialize a list of ModuleData objects to host the info of the partition's modules
                        List<ModuleData> modules = new ArrayList<>();

                        try (PartitionLogger logger = new PartitionLogger(
                                partitionHdfsIO,
                                moduleEventLogsDir,
                                moduleExplanationsLogsDir,
                                moduleFixesLogsDir,
                                moduleInnerRoundNumLogsDir,
                                moduleReasoningLogsDir,
                                moduleSizeLogsDir,
                                binEventLogsDir,
                                binExplanationsLogsDir,
                                binFixesLogsDir,
                                binInnerRoundNumLogsDir,
                                binReasoningLogsDir,
                                binSizeLogsDir,
                                partitionFileName,
                                1000
                        )) {
                            // For each line in the partition we process
                            while (partitionIterator.hasNext()) {
                                // Read the next line from the partition
                                String line = partitionIterator.next();

                                // Extract the base individual from the input line
                                String baseIndividualStr = extractBaseIndividualStr(line);

                                // Get the neighbors of the base individual and their distance
                                HashMap<String, Integer> neighborsMap = extractNeighborsMap(line);

                                // Construct a list with the base individual and its neighbors if extend is enabled
                                ArrayList<String> allIndividuals = new ArrayList<>();
                                allIndividuals.add(baseIndividualStr);
                                if (extend) {
                                    allIndividuals.addAll(neighborsMap.keySet());
                                }

                                // Write event log STARTED_COUNTING_TRIPLES_OF_MODULE for the current module
                                ModuleEventLog startedCountingTriplesOfModuleEventLog = new ModuleEventLog(baseIndividualStr, ModuleEvent.STARTED_COUNTING_TRIPLES_OF_MODULE, LocalDateTime.now());
                                logger.addModuleEventLog(startedCountingTriplesOfModuleEventLog.toJsonString());
                                // Print in stdout the STARTED_COUNTING_TRIPLES_OF_MODULE event
                                System.out.println(startedCountingTriplesOfModuleEventLog.toJsonString());

                                // Get module size
                                int moduleSize = aBoxHandler.countIndividualTriples(allIndividuals);

                                // Write event log ENDED_COUNTING_TRIPLES_OF_MODULE for the current module
                                ModuleEventLog endedCountingTriplesOfModuleEventLog = new ModuleEventLog(baseIndividualStr, ModuleEvent.ENDED_COUNTING_TRIPLES_OF_MODULE, LocalDateTime.now());
                                logger.addModuleEventLog(endedCountingTriplesOfModuleEventLog.toJsonString());
                                // Print in stdout the ENDED_COUNTING_TRIPLES_OF_MODULE event
                                System.out.println(endedCountingTriplesOfModuleEventLog.toJsonString());

                                // Write module size log for the current module
                                ModuleSizeLog moduleSizeLog = new ModuleSizeLog(baseIndividualStr, outerRound, moduleSize);
                                logger.addModuleSizeLog(moduleSizeLog.toJsonString());

                                // Create ModuleData object hosting the info of the current module
                                ModuleData moduleData = new ModuleData(baseIndividualStr, new ArrayList<>(neighborsMap.keySet()), moduleSize);
                                modules.add(moduleData);
                            }

                            // Initialize a list of bins
                            ArrayList<Bin> bins = new ArrayList<>();

                            // Best-fit bin packing
                            assignModulesToBins(modules, bins, binCapacity);

                            // For each bin
                            for (Bin b : bins) {
                                // Merge all base individuals and all their neighbors
                                ArrayList<String> allIndividuals = new ArrayList<>();
                                allIndividuals.addAll(b.baseIndividuals());
                                allIndividuals.addAll(b.mergedNeighbors());

                                // Write event log STARTED_COUNTING_TRIPLES_OF_BIN for the current module
                                BinEventLog startedCountingTriplesOfBinEventLog = new BinEventLog(b.baseIndividuals(), BinEvent.STARTED_COUNTING_TRIPLES_OF_BIN, LocalDateTime.now());
                                logger.addBinEventLog(startedCountingTriplesOfBinEventLog.toJsonString());
                                // Print in stdout the STARTED_COUNTING_TRIPLES_OF_BIN event
                                System.out.println(startedCountingTriplesOfBinEventLog.toJsonString());

                                // Get bin size
                                int binSize = aBoxHandler.countIndividualTriples(allIndividuals);

                                // Write event log ENDED_COUNTING_TRIPLES_OF_BIN for the current module
                                BinEventLog endedCountingTriplesOfBinEventLog = new BinEventLog(b.baseIndividuals(), BinEvent.ENDED_COUNTING_TRIPLES_OF_BIN, LocalDateTime.now());
                                logger.addBinEventLog(endedCountingTriplesOfBinEventLog.toJsonString());
                                // Print in stdout the ENDED_COUNTING_TRIPLES_OF_BIN event
                                System.out.println(endedCountingTriplesOfBinEventLog.toJsonString());

                                // Write bin size log for the current module
                                BinSizeLog binSizeLog = new BinSizeLog(b.baseIndividuals(), outerRound, binSize);
                                logger.addBinSizeLog(binSizeLog.toJsonString());
                            }
                        }
                    }
                });

                //  Get end time in millis
                long endTimeMillis = System.currentTimeMillis();
                // Get end date time
                LocalDateTime endDateTime = LocalDateTime.now();

                //  Calculate total time in millis
                long totalTimeMillis = endTimeMillis - startTimeMillis;

                // Write the start date time to hdfs
                hdfsIO.writeStringToHDFS("End date time by Spark: " + endDateTime, sparkTotalTimeFilePath);
                System.out.println("End date time by Spark: " + endDateTime);

                // Write the total time in millis to hdfs
                hdfsIO.writeStringToHDFS("Total time elapsed by Spark: " + totalTimeMillis + " ms", sparkTotalTimeFilePath);
                System.out.println("Total time elapsed by Spark: " + totalTimeMillis + " ms");

                // Write stop flag to hdfs
                hdfsIO.writeStringToHDFS("stop", stopLoopFlagFilePath);
                System.out.println("Writing stop flag to hdfs.");
            } else if (mode == 3) {
                // --------------------------------
                // Mode 3 (Get explanations only)
                // --------------------------------

                // Create an HDFSIO object
                HDFSIO hdfsIO = new HDFSIO(hdfsLocation);

                // Create an RDD
                JavaRDD<String> rdd = sc.textFile(kHopLinksFilePath, partitionsNumber);

                //  Get start time in millis
                long startTimeMillis = System.currentTimeMillis();
                // Get start date time
                LocalDateTime startDateTime = LocalDateTime.now();

                // Write the start time in millis to hdfs
                hdfsIO.writeStringToHDFS("Start date time by Spark: " + startDateTime, sparkTotalTimeFilePath);
                System.out.println("Start date time by Spark: " + startDateTime);

                // For each partition in the RDD
                rdd.foreachPartition((Iterator<String> partitionIterator) -> {
                    // Define the partition file name
                    String partitionFileName = "partition_" + TaskContext.getPartitionId() + ".ndjson";

                    // Create TBoxHandler and ABoxHandler objects
                    TBoxHandler tBoxHandler = new TBoxHandler(tBoxFilePath);
                    try (ABoxHandler aBoxHandler = new ABoxHandler(sparqlEndpoint, graphDomain, tBoxHandler, new PositionTracker())) {

                        // Create an HDFSIO object
                        HDFSIO partitionHdfsIO = new HDFSIO(hdfsLocation);

                        try (PartitionLogger logger = new PartitionLogger(
                                partitionHdfsIO,
                                moduleEventLogsDir,
                                moduleExplanationsLogsDir,
                                moduleFixesLogsDir,
                                moduleInnerRoundNumLogsDir,
                                moduleReasoningLogsDir,
                                moduleSizeLogsDir,
                                binEventLogsDir,
                                binExplanationsLogsDir,
                                binFixesLogsDir,
                                binInnerRoundNumLogsDir,
                                binReasoningLogsDir,
                                binSizeLogsDir,
                                partitionFileName,
                                1000
                        )) {
                            // For each line in the partition we process
                            while (partitionIterator.hasNext()) {

                                // Read the next line from the partition
                                String line = partitionIterator.next();

                                // Extract the base individual from the input line
                                String baseIndividualStr = extractBaseIndividualStr(line);

                                // Get the neighbors of the base individual and their distance
                                HashMap<String, Integer> neighborsMap = extractNeighborsMap(line);

                                // Construct a list with the base individual and its neighbors if extend is enabled
                                ArrayList<String> allIndividuals = new ArrayList<>();
                                allIndividuals.add(baseIndividualStr);
                                if (extend) {
                                    allIndividuals.addAll(neighborsMap.keySet());
                                }
                                System.out.println("Number of individuals: " + allIndividuals.size());

                                // Write event log STARTED_FETCHING_TRIPLES_OF_MODULE for the current module
                                ModuleEventLog startedFetchingTriplesOfModuleEventLog = new ModuleEventLog(baseIndividualStr, ModuleEvent.STARTED_FETCHING_TRIPLES_OF_MODULE, LocalDateTime.now());
                                logger.addModuleEventLog(startedFetchingTriplesOfModuleEventLog.toJsonString());
                                // Print in stdout the STARTED_FETCHING_TRIPLES_OF_MODULE event
                                System.out.println(startedFetchingTriplesOfModuleEventLog.toJsonString());

                                // Create a set of the ABox axioms
                                Set<OWLAxiom> abox = new HashSet<>(aBoxHandler.getABoxModules(allIndividuals));

                                // Write event log ENDED_FETCHING_TRIPLES_OF_MODULE for the current module
                                ModuleEventLog endedFetchingTriplesOfModuleEventLog = new ModuleEventLog(baseIndividualStr, ModuleEvent.ENDED_FETCHING_TRIPLES_OF_MODULE, LocalDateTime.now());
                                logger.addModuleEventLog(endedFetchingTriplesOfModuleEventLog.toJsonString());
                                // Print in stdout the ENDED_FETCHING_TRIPLES_OF_MODULE event
                                System.out.println(endedFetchingTriplesOfModuleEventLog.toJsonString());

                                // Create a set of the TBox axioms
                                Set<OWLAxiom> tBox = tBoxHandler.getOntologyTBox();

                                // Create a set of all axioms (ABox and TBox)
                                Set<OWLAxiom> all_boxes = new HashSet<>();
                                all_boxes.addAll(abox);
                                all_boxes.addAll(tBox);

                                // Write event log STARTED_COUNTING_TRIPLES_OF_MODULE for the current module
                                ModuleEventLog startedCountingTriplesOfModuleEventLog = new ModuleEventLog(baseIndividualStr, ModuleEvent.STARTED_COUNTING_TRIPLES_OF_MODULE, LocalDateTime.now());
                                logger.addModuleEventLog(startedCountingTriplesOfModuleEventLog.toJsonString());
                                // Print in stdout the STARTED_COUNTING_TRIPLES_OF_MODULE event
                                System.out.println(startedCountingTriplesOfModuleEventLog.toJsonString());

                                // Get module size
                                int moduleSize = abox.size();

                                // Write event log ENDED_COUNTING_TRIPLES_OF_MODULE for the current module
                                ModuleEventLog endedCountingTriplesOfModuleEventLog = new ModuleEventLog(baseIndividualStr, ModuleEvent.ENDED_COUNTING_TRIPLES_OF_MODULE, LocalDateTime.now());
                                logger.addModuleEventLog(endedCountingTriplesOfModuleEventLog.toJsonString());
                                // Print in stdout the ENDED_COUNTING_TRIPLES_OF_MODULE event
                                System.out.println(endedCountingTriplesOfModuleEventLog.toJsonString());

                                // Write module size log for the current module
                                ModuleSizeLog moduleSizeLog = new ModuleSizeLog(baseIndividualStr, outerRound, moduleSize);
                                logger.addModuleSizeLog(moduleSizeLog.toJsonString());

                                // Create a ModuleHandler object that will host the whole ontology
                                try (ModuleHandler moduleHandler = new ModuleHandler(all_boxes, IRI.create(removeQuotes(baseIndividualStr)), baseIndividualStr, reasonerSelection)) {

                                    // Initialize variables for metrics logging
                                    long checkConsistencyTimeMillis;
                                    long getExplanationsTimeMillis = -1;
                                    int explanationsCount = -1;

                                    // Write event log STARTED_CHECKING_CONSISTENCY_OF_MODULE for the current module
                                    ModuleEventLog startedCheckingConsistencyOfModuleEventLog = new ModuleEventLog(baseIndividualStr, ModuleEvent.STARTED_CHECKING_CONSISTENCY_OF_MODULE, LocalDateTime.now());
                                    logger.addModuleEventLog(startedCheckingConsistencyOfModuleEventLog.toJsonString());
                                    // Print in stdout the STARTED_CHECKING_CONSISTENCY_OF_MODULE event
                                    System.out.println(startedCheckingConsistencyOfModuleEventLog.toJsonString());

                                    // Check consistency of the ontology and log the time needed for that
                                    long checkConsistencyStartTimeMillis = System.currentTimeMillis();
                                    ConsistencyCheckOutcome consistencyCheckOutcome = moduleHandler.isConsistent(reasonerTimeoutMillis);
                                    long checkConsistencyEndTimeMillis = System.currentTimeMillis();
                                    checkConsistencyTimeMillis = checkConsistencyEndTimeMillis - checkConsistencyStartTimeMillis;

                                    // Write event log ENDED_CHECKING_CONSISTENCY_OF_MODULE for the current module
                                    ModuleEventLog endedCheckingConsistencyOfModuleEventLog = new ModuleEventLog(baseIndividualStr, ModuleEvent.ENDED_CHECKING_CONSISTENCY_OF_MODULE, LocalDateTime.now());
                                    logger.addModuleEventLog(endedCheckingConsistencyOfModuleEventLog.toJsonString());
                                    // Print in stdout the ENDED_CHECKING_CONSISTENCY_OF_MODULE event
                                    System.out.println(endedCheckingConsistencyOfModuleEventLog.toJsonString());

                                    if ((consistencyCheckOutcome.getStatus() != ConsistencyCheckOutcome.Status.CONSISTENT) && (consistencyCheckOutcome.getStatus() != ConsistencyCheckOutcome.Status.INCONSISTENT))
                                    {
                                        // Write event log TIMED_OUT_CHECKING_CONSISTENCY_OF_MODULE for the current module
                                        ModuleEventLog timedOutCheckingConsistencyOfModuleEventLog = new ModuleEventLog(baseIndividualStr, ModuleEvent.TIMED_OUT_CHECKING_CONSISTENCY_OF_MODULE, LocalDateTime.now());
                                        logger.addModuleEventLog(timedOutCheckingConsistencyOfModuleEventLog.toJsonString());
                                        // Print in stdout the TIMED_OUT_CHECKING_CONSISTENCY_OF_MODULE event
                                        System.out.println(timedOutCheckingConsistencyOfModuleEventLog.toJsonString());
                                        continue;
                                    }

                                    boolean aBoxConsistent = consistencyCheckOutcome.getConsistent();

                                    // If the ontology is inconsistent
                                    if (!aBoxConsistent) {

                                        // Write event log STARTED_GETTING_EXPLANATIONS_OF_MODULE for the current module
                                        ModuleEventLog startedGettingExplanationsOfModuleEventLog = new ModuleEventLog(baseIndividualStr, ModuleEvent.STARTED_GETTING_EXPLANATIONS_OF_MODULE, LocalDateTime.now());
                                        logger.addModuleEventLog(startedGettingExplanationsOfModuleEventLog.toJsonString());
                                        // Print in stdout the STARTED_GETTING_EXPLANATIONS_OF_MODULE event
                                        System.out.println(startedGettingExplanationsOfModuleEventLog.toJsonString());

                                        // Get inconsistency explanations and log the time needed for that
                                        long getExplanationsStartTimeMillis = System.currentTimeMillis();
                                        ExplanationOutcome explanationOutcome = moduleHandler.getExplanations(perOpTimeoutMillis, reasonerTimeoutMillis, explanationsLimit);
                                        long getExplanationsEndTimeMillis = System.currentTimeMillis();
                                        getExplanationsTimeMillis = getExplanationsEndTimeMillis - getExplanationsStartTimeMillis;

                                        // Write event log ENDED_GETTING_EXPLANATIONS_OF_MODULE for the current module
                                        ModuleEventLog endedGettingExplanationsOfModuleEventLog = new ModuleEventLog(baseIndividualStr, ModuleEvent.ENDED_GETTING_EXPLANATIONS_OF_MODULE, LocalDateTime.now());
                                        logger.addModuleEventLog(endedGettingExplanationsOfModuleEventLog.toJsonString());
                                        // Print in stdout the ENDED_GETTING_EXPLANATIONS_OF_MODULE event
                                        System.out.println(endedGettingExplanationsOfModuleEventLog.toJsonString());

                                        if (explanationOutcome.getStatus() == ExplanationOutcome.Status.FAILED) {
                                            // Write event log TIMED_OUT_GETTING_EXPLANATIONS_OF_MODULE for the current module
                                            ModuleEventLog timedOutGettingExplanationsOfModuleEventLog = new ModuleEventLog(baseIndividualStr, ModuleEvent.TIMED_OUT_GETTING_EXPLANATIONS_OF_MODULE, LocalDateTime.now());
                                            logger.addModuleEventLog(timedOutGettingExplanationsOfModuleEventLog.toJsonString());
                                            // Print in stdout the TIMED_OUT_GETTING_EXPLANATIONS_OF_MODULE event
                                            System.out.println(timedOutGettingExplanationsOfModuleEventLog.toJsonString());
                                            continue;
                                        }

                                        // Sort the inconsistency explanations alphabetically
                                        List<List<OWLAxiom>> sortedExplanations = sortExplanations(explanationOutcome.getExplanations());

                                        explanationsCount = sortedExplanations.size();
                                        // Make a string representation of the inconsistency explanations found corresponding to the modules merged (base individuals merged)
                                        System.out.println(moduleExplanationsString(sortedExplanations, baseIndividualStr));

                                        // Write the inconsistency explanations found to hdfs
                                        ModuleExplanationsLog moduleExplanationsLog = new ModuleExplanationsLog(baseIndividualStr, outerRound, 1, sortedExplanations);
                                        logger.addModuleExplanationsLog(moduleExplanationsLog.toJsonString());
                                    }

                                    ModuleReasoningLog moduleReasoningLog = new ModuleReasoningLog(baseIndividualStr, outerRound, 1, checkConsistencyTimeMillis, -1, getExplanationsTimeMillis, -1, explanationsCount);
                                    logger.addModuleReasoningLog(moduleReasoningLog.toJsonString());
                                }
                            }
                        }
                    }
                });

                long explanationsNumber;
                // After processing all partitions, read all explanation logs, merge and deduplicate them, and write the unique explanations to hdfs
                if (hdfsIO.hasFiles(moduleExplanationsLogsDir)) {
                    // 1) Read raw NDJSON lines
                    JavaRDD<String> raw = sc.textFile(hdfsLocation + "/" + moduleExplanationsLogsDir);

                    // 2) Fast pre-filter: drop empties/whitespace and obvious non-JSON lines
                    JavaRDD<String> candidates = raw.filter(s -> {
                        if (s == null) return false;
                        String t = s.trim();
                        return !t.isEmpty() && t.charAt(0) == '{';
                    });

                    // 3) Merge/deduplicate on the filtered lines (ensure your mergeModuleExplanations
                    //    does safe parsing internally, or make a tryParseOrNull and filter nulls)
                    JavaRDD<String> mergedExplanationLines = mergeModuleExplanations(candidates)
                            .persist(org.apache.spark.storage.StorageLevel.MEMORY_AND_DISK()); // we'll use it twice

                    // 4) Count uniques
                    explanationsNumber = mergedExplanationLines.count();

                    // 5) Write a single NDJSON file
                    String tmpSingleDir = hdfsLocation + "/" + outputDirectoryPath + "/merged_explanations_" + java.util.UUID.randomUUID();
                    mergedExplanationLines.coalesce(1, true).saveAsTextFile(tmpSingleDir);
                    hdfsIO.moveSinglePartFile(tmpSingleDir, mergedExplanationsFilePath);
                } else {
                    explanationsNumber = 0;
                }

                // Write the total number of unique explanations found to hdfs
                hdfsIO.writeStringToHDFS("Number of explanations: " + explanationsNumber, explanationsNumberFilePath);
                System.out.println("Number of explanations: " + explanationsNumber);

                //  Get end time in millis
                long endTimeMillis = System.currentTimeMillis();
                // Get end date time
                LocalDateTime endDateTime = LocalDateTime.now();

                //  Calculate total time in millis
                long totalTimeMillis = endTimeMillis - startTimeMillis;

                // Write the start date time to hdfs
                hdfsIO.writeStringToHDFS("End date time by Spark: " + endDateTime, sparkTotalTimeFilePath);
                System.out.println("End date time by Spark: " + endDateTime);

                // Write the total time in millis to hdfs
                hdfsIO.writeStringToHDFS("Total time elapsed by Spark: " + totalTimeMillis + " ms", sparkTotalTimeFilePath);
                System.out.println("Total time elapsed by Spark: " + totalTimeMillis + " ms");

                // Write stop flag to hdfs
                hdfsIO.writeStringToHDFS("stop", stopLoopFlagFilePath);
                System.out.println("Writing stop flag to hdfs.");
            } else if (mode == 4) {
                // ------------------------------------------------
                // Mode 4 (Get explanations only with bin-packing)
                // ------------------------------------------------

                // Create an HDFSIO object
                HDFSIO hdfsIO = new HDFSIO(hdfsLocation);

                // Create an RDD
                JavaRDD<String> rdd = sc.textFile(kHopLinksFilePath, partitionsNumber);

                //  Get start time in millis
                long startTimeMillis = System.currentTimeMillis();
                // Get start date time
                LocalDateTime startDateTime = LocalDateTime.now();

                // Write the start time in millis to hdfs
                hdfsIO.writeStringToHDFS("Start date time by Spark: " + startDateTime, sparkTotalTimeFilePath);
                System.out.println("Start date time by Spark: " + startDateTime);

                // For each partition in the RDD
                rdd.foreachPartition((Iterator<String> partitionIterator) -> {
                    // Define the partition file name
                    String partitionFileName = "partition_" + TaskContext.getPartitionId() + ".ndjson";

                    // Create TBoxHandler and ABoxHandler objects
                    TBoxHandler tBoxHandler = new TBoxHandler(tBoxFilePath);
                    try (ABoxHandler aBoxHandler = new ABoxHandler(sparqlEndpoint, graphDomain, tBoxHandler, new PositionTracker())) {

                        // Create an HDFSIO object
                        HDFSIO partitionHdfsIO = new HDFSIO(hdfsLocation);

                        // Initialize a list of ModuleData objects to host the info of the partition's modules
                        List<ModuleData> modules = new ArrayList<>();

                        try (PartitionLogger logger = new PartitionLogger(
                                partitionHdfsIO,
                                moduleEventLogsDir,
                                moduleExplanationsLogsDir,
                                moduleFixesLogsDir,
                                moduleInnerRoundNumLogsDir,
                                moduleReasoningLogsDir,
                                moduleSizeLogsDir,
                                binEventLogsDir,
                                binExplanationsLogsDir,
                                binFixesLogsDir,
                                binInnerRoundNumLogsDir,
                                binReasoningLogsDir,
                                binSizeLogsDir,
                                partitionFileName,
                                1000
                        )) {
                            // For each line in the partition we process
                            while (partitionIterator.hasNext()) {
                                // Read the next line
                                String line = partitionIterator.next();

                                // Extract the base individual from the input line
                                String baseIndividualStr = extractBaseIndividualStr(line);

                                // Get the neighbors of the base individual and their distance
                                HashMap<String, Integer> neighborsMap = extractNeighborsMap(line);

                                // Construct a list with the base individual and its neighbors if extend is enabled
                                ArrayList<String> allIndividuals = new ArrayList<>();
                                allIndividuals.add(baseIndividualStr);
                                if (extend) {
                                    allIndividuals.addAll(neighborsMap.keySet());
                                }

                                // Write event log STARTED_COUNTING_TRIPLES_OF_MODULE for the current module
                                ModuleEventLog startedCountingTriplesOfModuleEventLog = new ModuleEventLog(baseIndividualStr, ModuleEvent.STARTED_COUNTING_TRIPLES_OF_MODULE, LocalDateTime.now());
                                logger.addModuleEventLog(startedCountingTriplesOfModuleEventLog.toJsonString());
                                // Print in stdout the STARTED_COUNTING_TRIPLES_OF_MODULE event
                                System.out.println(startedCountingTriplesOfModuleEventLog.toJsonString());

                                // Get module size
                                int moduleSize = aBoxHandler.countIndividualTriples(allIndividuals);

                                // Write event log ENDED_COUNTING_TRIPLES_OF_MODULE for the current module
                                ModuleEventLog endedCountingTriplesOfModuleEventLog = new ModuleEventLog(baseIndividualStr, ModuleEvent.ENDED_COUNTING_TRIPLES_OF_MODULE, LocalDateTime.now());
                                logger.addModuleEventLog(endedCountingTriplesOfModuleEventLog.toJsonString());
                                // Print in stdout the ENDED_COUNTING_TRIPLES_OF_MODULE event
                                System.out.println(endedCountingTriplesOfModuleEventLog.toJsonString());

                                // Write module size log for the current module
                                ModuleSizeLog moduleSizeLog = new ModuleSizeLog(baseIndividualStr, outerRound, moduleSize);
                                logger.addModuleSizeLog(moduleSizeLog.toJsonString());

                                // Create ModuleData object hosting the info of the current module
                                ModuleData moduleData = new ModuleData(baseIndividualStr, new ArrayList<>(neighborsMap.keySet()), moduleSize);
                                modules.add(moduleData);
                            }

                            // Initialize the list of bins for the bin-packing
                            List<Bin> bins = new ArrayList<>();

                            // Best-fit bin packing
                            assignModulesToBins(modules, bins, binCapacity);

                            // For each bin
                            for (Bin b : bins) {

                                // Merge all base individuals and all their neighbors
                                ArrayList<String> allIndividuals = new ArrayList<>();
                                allIndividuals.addAll(b.baseIndividuals());
                                allIndividuals.addAll(b.mergedNeighbors());

                                // Write event log STARTED_FETCHING_TRIPLES_OF_BIN for the current module
                                BinEventLog startedFetchingTriplesOfBinEventLog = new BinEventLog(b.baseIndividuals(), BinEvent.STARTED_FETCHING_TRIPLES_OF_BIN, LocalDateTime.now());
                                logger.addBinEventLog(startedFetchingTriplesOfBinEventLog.toJsonString());
                                // Print in stdout the STARTED_FETCHING_TRIPLES_OF_BIN event
                                System.out.println(startedFetchingTriplesOfBinEventLog.toJsonString());

                                // Create a set of the ABox axioms
                                Set<OWLAxiom> abox = new HashSet<>(aBoxHandler.getABoxModules(allIndividuals));

                                // Write event log ENDED_FETCHING_TRIPLES_OF_BIN for the current module
                                BinEventLog endedFetchingTriplesOfBinEventLog = new BinEventLog(b.baseIndividuals(), BinEvent.ENDED_FETCHING_TRIPLES_OF_BIN, LocalDateTime.now());
                                logger.addBinEventLog(endedFetchingTriplesOfBinEventLog.toJsonString());
                                // Print in stdout the ENDED_FETCHING_TRIPLES_OF_BIN event
                                System.out.println(endedFetchingTriplesOfBinEventLog.toJsonString());

                                // Create a set of the TBox axioms
                                Set<OWLAxiom> tBox = tBoxHandler.getOntologyTBox();

                                // Create a set of all axioms (ABox and TBox)
                                Set<OWLAxiom> all_boxes = new HashSet<>();
                                all_boxes.addAll(abox);
                                all_boxes.addAll(tBox);

                                // Write event log STARTED_COUNTING_TRIPLES_OF_BIN for the current module
                                BinEventLog startedCountingTriplesOfBinEventLog = new BinEventLog(b.baseIndividuals(), BinEvent.STARTED_COUNTING_TRIPLES_OF_BIN, LocalDateTime.now());
                                logger.addBinEventLog(startedCountingTriplesOfBinEventLog.toJsonString());
                                // Print in stdout the STARTED_COUNTING_TRIPLES_OF_BIN event
                                System.out.println(startedCountingTriplesOfBinEventLog.toJsonString());

                                // Get module size
                                int binSize = abox.size();

                                // Write event log ENDED_COUNTING_TRIPLES_OF_BIN for the current module
                                BinEventLog endedCountingTriplesOfBinEventLog = new BinEventLog(b.baseIndividuals(), BinEvent.ENDED_COUNTING_TRIPLES_OF_BIN, LocalDateTime.now());
                                logger.addBinEventLog(endedCountingTriplesOfBinEventLog.toJsonString());
                                // Print in stdout the ENDED_COUNTING_TRIPLES_OF_BIN event
                                System.out.println(endedCountingTriplesOfBinEventLog.toJsonString());

                                // Write bin size log for the current module
                                BinSizeLog binSizeLog = new BinSizeLog(b.baseIndividuals(), outerRound, binSize);
                                logger.addBinSizeLog(binSizeLog.toJsonString());

                                // Create a ModuleHandler object that will host the whole ontology
                                try (ModuleHandler moduleHandler = new ModuleHandler(all_boxes, IRI.generateDocumentIRI(), String.join(",", b.baseIndividuals()), reasonerSelection)) {

                                    // Initialize variables for metrics logging
                                    long checkConsistencyTimeMillis;
                                    long getExplanationsTimeMillis = -1;
                                    int explanationsCount = -1;

                                    // Write event log STARTED_CHECKING_CONSISTENCY_OF_BIN for the current module
                                    BinEventLog startedCheckingConsistencyOfBinEventLog = new BinEventLog(b.baseIndividuals(), BinEvent.STARTED_CHECKING_CONSISTENCY_OF_BIN, LocalDateTime.now());
                                    logger.addBinEventLog(startedCheckingConsistencyOfBinEventLog.toJsonString());
                                    // Print in stdout the STARTED_CHECKING_CONSISTENCY_OF_BIN event
                                    System.out.println(startedCheckingConsistencyOfBinEventLog.toJsonString());

                                    // Check consistency of the ontology and log the time needed for that
                                    long checkConsistencyStartTimeMillis = System.currentTimeMillis();
                                    ConsistencyCheckOutcome consistencyCheckOutcome = moduleHandler.isConsistent(reasonerTimeoutMillis);
                                    long checkConsistencyEndTimeMillis = System.currentTimeMillis();
                                    checkConsistencyTimeMillis = checkConsistencyEndTimeMillis - checkConsistencyStartTimeMillis;

                                    // Write event log ENDED_CHECKING_CONSISTENCY_OF_BIN for the current module
                                    BinEventLog endedCheckingConsistencyOfBinEventLog = new BinEventLog(b.baseIndividuals(), BinEvent.ENDED_CHECKING_CONSISTENCY_OF_BIN, LocalDateTime.now());
                                    logger.addBinEventLog(endedCheckingConsistencyOfBinEventLog.toJsonString());
                                    // Print in stdout the ENDED_CHECKING_CONSISTENCY_OF_BIN event
                                    System.out.println(endedCheckingConsistencyOfBinEventLog.toJsonString());

                                    if ((consistencyCheckOutcome.getStatus() != ConsistencyCheckOutcome.Status.CONSISTENT) && (consistencyCheckOutcome.getStatus() != ConsistencyCheckOutcome.Status.INCONSISTENT))
                                    {
                                        // Write event log TIMED_OUT_CHECKING_CONSISTENCY_OF_BIN for the current module
                                        BinEventLog timedOutCheckingConsistencyOfBinEventLog = new BinEventLog(b.baseIndividuals(), BinEvent.TIMED_OUT_CHECKING_CONSISTENCY_OF_BIN, LocalDateTime.now());
                                        logger.addModuleEventLog(timedOutCheckingConsistencyOfBinEventLog.toJsonString());
                                        // Print in stdout the TIMED_OUT_CHECKING_CONSISTENCY_OF_BIN event
                                        System.out.println(timedOutCheckingConsistencyOfBinEventLog.toJsonString());
                                        continue;
                                    }

                                    boolean aBoxConsistent = consistencyCheckOutcome.getConsistent();

                                    // If the ontology is inconsistent
                                    if (!aBoxConsistent) {

                                        // Write event log STARTED_GETTING_EXPLANATIONS_OF_BIN for the current module
                                        BinEventLog startedGettingExplanationsOfBinEventLog = new BinEventLog(b.baseIndividuals(), BinEvent.STARTED_GETTING_EXPLANATIONS_OF_BIN, LocalDateTime.now());
                                        logger.addBinEventLog(startedGettingExplanationsOfBinEventLog.toJsonString());
                                        // Print in stdout the STARTED_GETTING_EXPLANATIONS_OF_BIN event
                                        System.out.println(startedGettingExplanationsOfBinEventLog.toJsonString());

                                        // Get inconsistency explanations and log the time needed for that
                                        long getExplanationsTimeMillisStart = System.currentTimeMillis();
                                        ExplanationOutcome explanationOutcome = moduleHandler.getExplanations(perOpTimeoutMillis, reasonerTimeoutMillis, explanationsLimit);
                                        long getExplanationsTimeMillisEnd = System.currentTimeMillis();
                                        getExplanationsTimeMillis = getExplanationsTimeMillisEnd - getExplanationsTimeMillisStart;

                                        // Write event log ENDED_GETTING_EXPLANATIONS_OF_BIN for the current module
                                        BinEventLog endedGettingExplanationsOfBinEventLog = new BinEventLog(b.baseIndividuals(), BinEvent.ENDED_GETTING_EXPLANATIONS_OF_BIN, LocalDateTime.now());
                                        logger.addBinEventLog(endedGettingExplanationsOfBinEventLog.toJsonString());
                                        // Print in stdout the ENDED_GETTING_EXPLANATIONS_OF_BIN event
                                        System.out.println(endedGettingExplanationsOfBinEventLog.toJsonString());

                                        if (explanationOutcome.getStatus() == ExplanationOutcome.Status.FAILED) {
                                            // Write event log TIMED_OUT_GETTING_EXPLANATIONS_OF_BIN for the current module
                                            BinEventLog timedOutGettingExplanationsOfBinEventLog = new BinEventLog(b.baseIndividuals(), BinEvent.TIMED_OUT_GETTING_EXPLANATIONS_OF_BIN, LocalDateTime.now());
                                            logger.addModuleEventLog(timedOutGettingExplanationsOfBinEventLog.toJsonString());
                                            // Print in stdout the TIMED_OUT_GETTING_EXPLANATIONS_OF_BIN event
                                            System.out.println(timedOutGettingExplanationsOfBinEventLog.toJsonString());
                                            continue;
                                        }

                                        // Sort the inconsistency explanations alphabetically
                                        List<List<OWLAxiom>> sortedExplanations = sortExplanations(explanationOutcome.getExplanations());

                                        explanationsCount = sortedExplanations.size();
                                        // Make a string representation of the inconsistency explanations found corresponding to the modules merged (base individuals merged)
                                        System.out.println(moduleExplanationsString(sortedExplanations, String.join(",", b.baseIndividuals())));

                                        // Write the inconsistency explanations found to hdfs
                                        BinExplanationsLog binExplanationsLog = new BinExplanationsLog(b.baseIndividuals(), outerRound, 1, sortedExplanations);
                                        logger.addBinExplanationsLog(binExplanationsLog.toJsonString());
                                    }

                                    BinReasoningLog binReasoningLog = new BinReasoningLog(b.baseIndividuals(), outerRound, 1, checkConsistencyTimeMillis, -1, getExplanationsTimeMillis, -1, explanationsCount);
                                    logger.addBinReasoningLog(binReasoningLog.toJsonString());
                                }
                            }
                        }
                    }
                });

                // After processing all partitions, read all explanation logs, merge and deduplicate them, and write the unique explanations to hdfs
                long explanationsNumber;
                // After processing all partitions, read all explanation logs, merge and deduplicate them, and write the unique explanations to hdfs
                if (hdfsIO.hasFiles(binExplanationsLogsDir)) {
                    // 1) Read raw NDJSON
                    JavaRDD<String> rawBin = sc.textFile(hdfsLocation + "/" + binExplanationsLogsDir);

                    // 2) Fast pre-filter to skip blanks / non-JSON
                    JavaRDD<String> candidatesBin = rawBin.filter(s -> {
                        if (s == null) return false;
                        String t = s.trim();
                        return !t.isEmpty() && t.charAt(0) == '{';
                    });

                    // 3) Merge/deduplicate (ensure mergeBinExplanations is safe on parsing, or it internally filters)
                    JavaRDD<String> mergedBinExplanationLines = mergeBinExplanations(candidatesBin)
                            .persist(org.apache.spark.storage.StorageLevel.MEMORY_AND_DISK());

                    // 4) Count uniques
                    explanationsNumber = mergedBinExplanationLines.count();

                    // 5) Write a single NDJSON file
                    String tmpSingleDirBin = hdfsLocation + "/" + outputDirectoryPath + "/merged_explanations_" + java.util.UUID.randomUUID();
                    mergedBinExplanationLines.coalesce(1, true).saveAsTextFile(tmpSingleDirBin);
                    hdfsIO.moveSinglePartFile(tmpSingleDirBin, mergedExplanationsFilePath);
                } else {
                    explanationsNumber = 0;
                }

                // Write the total number of unique explanations found to hdfs
                hdfsIO.writeStringToHDFS("Number of explanations: " + explanationsNumber, explanationsNumberFilePath);
                System.out.println("Number of explanations: " + explanationsNumber);

                //  Get end time in millis
                long endTimeMillis = System.currentTimeMillis();
                // Get end date time
                LocalDateTime endDateTime = LocalDateTime.now();

                //  Calculate total time in millis
                long totalTimeMillis = endTimeMillis - startTimeMillis;

                // Write the start date time to hdfs
                hdfsIO.writeStringToHDFS("End date time by Spark: " + endDateTime, sparkTotalTimeFilePath);
                System.out.println("End date time by Spark: " + endDateTime);

                // Write the total time in millis to hdfs
                hdfsIO.writeStringToHDFS("Total time elapsed by Spark: " + totalTimeMillis + " ms", sparkTotalTimeFilePath);
                System.out.println("Total time elapsed by Spark: " + totalTimeMillis + " ms");

                // Write stop flag to hdfs
                hdfsIO.writeStringToHDFS("stop", stopLoopFlagFilePath);
                System.out.println("Writing stop flag to hdfs.");
            } else if (mode == 5) {
                // ----------------------------------
                // Mode 5 (Get explanations and fix)
                // ----------------------------------

                // Create an HDFSIO object
                HDFSIO hdfsIO = new HDFSIO(hdfsLocation);

                // Create an RDD
                JavaRDD<String> rdd = sc.textFile(kHopLinksFilePath, partitionsNumber);

                //  Get start time in millis
                long startTimeMillis = System.currentTimeMillis();
                // Get start date time
                LocalDateTime startDateTime = LocalDateTime.now();

                // Write the start time in millis to hdfs
                hdfsIO.writeStringToHDFS("Start date time by Spark: " + startDateTime, sparkTotalTimeFilePath);
                System.out.println("Start date time by Spark: " + startDateTime);

                // For each partition in the RDD
                rdd.foreachPartition((Iterator<String> partitionIterator) -> {
                    // Define the partition file name
                    String partitionFileName = "partition_" + TaskContext.getPartitionId() + ".ndjson";

                    // Create a Random object
                    Random random = new Random();

                    // Read the global position tracker from hdfs if outerRound > 1 else create a new one
                    PositionTracker globalPositionTracker;
                    if (outerRound > 1) {
                        globalPositionTracker = PositionTracker.fromJsonString(hdfsIO.readStringFromHDFS(inputPositionTrackerFilePath));
                    } else {
                        globalPositionTracker = new PositionTracker();
                    }

                    // Create TBoxHandler and ABoxHandler objects
                    TBoxHandler tBoxHandler = new TBoxHandler(tBoxFilePath);
                    try (ABoxHandler aBoxHandler = new ABoxHandler(sparqlEndpoint, graphDomain, tBoxHandler, new PositionTracker(globalPositionTracker))) {

                        // Create an HDFSIO object
                        HDFSIO partitionHdfsIO = new HDFSIO(hdfsLocation);

                        try (PartitionLogger logger = new PartitionLogger(
                                partitionHdfsIO,
                                moduleEventLogsDir,
                                moduleExplanationsLogsDir,
                                moduleFixesLogsDir,
                                moduleInnerRoundNumLogsDir,
                                moduleReasoningLogsDir,
                                moduleSizeLogsDir,
                                binEventLogsDir,
                                binExplanationsLogsDir,
                                binFixesLogsDir,
                                binInnerRoundNumLogsDir,
                                binReasoningLogsDir,
                                binSizeLogsDir,
                                partitionFileName,
                                1000
                        )) {
                            // For each line in the partition we process
                            while (partitionIterator.hasNext()) {

                                // Read the next line
                                String line = partitionIterator.next();

                                // Extract the base individual from the input line
                                String baseIndividualStr = extractBaseIndividualStr(line);

                                // Get the neighbors of the base individual and their distance
                                HashMap<String, Integer> neighborsMap = extractNeighborsMap(line);

                                // Construct a list with the base individual and its neighbors if extend is enabled
                                ArrayList<String> allIndividuals = new ArrayList<>();
                                allIndividuals.add(baseIndividualStr);
                                if (extend) {
                                    allIndividuals.addAll(neighborsMap.keySet());
                                }

                                // Write event log STARTED_FETCHING_TRIPLES_OF_MODULE for the current module
                                ModuleEventLog startedFetchingTriplesOfModuleEventLog = new ModuleEventLog(baseIndividualStr, ModuleEvent.STARTED_FETCHING_TRIPLES_OF_MODULE, LocalDateTime.now());
                                logger.addModuleEventLog(startedFetchingTriplesOfModuleEventLog.toJsonString());
                                // Print in stdout the STARTED_FETCHING_TRIPLES_OF_MODULE event
                                System.out.println(startedFetchingTriplesOfModuleEventLog.toJsonString());

                                // Create a set of the ABox axioms
                                Set<OWLAxiom> abox = new HashSet<>(aBoxHandler.getABoxModules(allIndividuals));

                                // Write event log ENDED_FETCHING_TRIPLES_OF_MODULE for the current module
                                ModuleEventLog endedFetchingTriplesOfModuleEventLog = new ModuleEventLog(baseIndividualStr, ModuleEvent.ENDED_FETCHING_TRIPLES_OF_MODULE, LocalDateTime.now());
                                logger.addModuleEventLog(endedFetchingTriplesOfModuleEventLog.toJsonString());
                                // Print in stdout the ENDED_FETCHING_TRIPLES_OF_MODULE event
                                System.out.println(endedFetchingTriplesOfModuleEventLog.toJsonString());

                                // Create a set of the TBox axioms
                                Set<OWLAxiom> tBox = tBoxHandler.getOntologyTBox();

                                // Create a set of all axioms (ABox and TBox)
                                Set<OWLAxiom> all_boxes = new HashSet<>();
                                all_boxes.addAll(abox);
                                all_boxes.addAll(tBox);

                                // Write event log STARTED_COUNTING_TRIPLES_OF_MODULE for the current module
                                ModuleEventLog startedCountingTriplesOfModuleEventLog = new ModuleEventLog(baseIndividualStr, ModuleEvent.STARTED_COUNTING_TRIPLES_OF_MODULE, LocalDateTime.now());
                                logger.addModuleEventLog(startedCountingTriplesOfModuleEventLog.toJsonString());
                                // Print in stdout the STARTED_COUNTING_TRIPLES_OF_MODULE event
                                System.out.println(startedCountingTriplesOfModuleEventLog.toJsonString());

                                // Get module size
                                int moduleSize = abox.size();

                                // Write event log ENDED_COUNTING_TRIPLES_OF_MODULE for the current module
                                ModuleEventLog endedCountingTriplesOfModuleEventLog = new ModuleEventLog(baseIndividualStr, ModuleEvent.ENDED_COUNTING_TRIPLES_OF_MODULE, LocalDateTime.now());
                                logger.addModuleEventLog(endedCountingTriplesOfModuleEventLog.toJsonString());
                                // Print in stdout the ENDED_COUNTING_TRIPLES_OF_MODULE event
                                System.out.println(endedCountingTriplesOfModuleEventLog.toJsonString());

                                // Write module size log for the current module
                                ModuleSizeLog moduleSizeLog = new ModuleSizeLog(baseIndividualStr, outerRound, moduleSize);
                                logger.addModuleSizeLog(moduleSizeLog.toJsonString());

                                // Create a ModuleHandler object that will host the whole ontology
                                try (ModuleHandler moduleHandler = new ModuleHandler(all_boxes, IRI.create(removeQuotes(baseIndividualStr)), baseIndividualStr, reasonerSelection)) {

                                    // Initialize metrics
                                    long checkConsistencyTimeMillis;
                                    long checkRepairabilityTimeMillis = -1;
                                    long getExplanationsTimeMillis = -1;
                                    long computeFixesTimeMillis = -1;
                                    int explanationsCount = -1;

                                    // Initialize the module's fixer
                                    Fixer fixer = new Fixer(moduleHandler.getOntology(), new PositionTracker(globalPositionTracker), fixSelection, fixingTimeoutMillis, reasonerSelection, reasonerTimeoutMillis, perOpTimeoutMillis, explanationsLimit, mcd);

                                    // Write event log STARTED_CHECKING_CONSISTENCY_OF_MODULE for the current module
                                    ModuleEventLog startedInitialCheckingConsistencyOfModuleEventLog = new ModuleEventLog(baseIndividualStr, ModuleEvent.STARTED_CHECKING_CONSISTENCY_OF_MODULE, LocalDateTime.now());
                                    logger.addModuleEventLog(startedInitialCheckingConsistencyOfModuleEventLog.toJsonString());
                                    // Print in stdout the STARTED_CHECKING_CONSISTENCY_OF_MODULE event
                                    System.out.println(startedInitialCheckingConsistencyOfModuleEventLog.toJsonString());

                                    // Check if module is consistent
                                    long checkConsistencyStartTimeMillis = System.currentTimeMillis();
                                    ConsistencyCheckOutcome consistencyCheckOutcome = fixer.CheckConsistency(reasonerTimeoutMillis);
                                    long checkConsistencyEndTimeMillis = System.currentTimeMillis();
                                    checkConsistencyTimeMillis = checkConsistencyEndTimeMillis - checkConsistencyStartTimeMillis;

                                    // Write event log ENDED_CHECKING_CONSISTENCY_OF_MODULE for the current module
                                    ModuleEventLog endedInitialCheckingConsistencyOfModuleEventLog = new ModuleEventLog(baseIndividualStr, ModuleEvent.ENDED_CHECKING_CONSISTENCY_OF_MODULE, LocalDateTime.now());
                                    logger.addModuleEventLog(endedInitialCheckingConsistencyOfModuleEventLog.toJsonString());
                                    // Print in stdout the ENDED_CHECKING_CONSISTENCY_OF_MODULE event
                                    System.out.println(endedInitialCheckingConsistencyOfModuleEventLog.toJsonString());

                                    if ((consistencyCheckOutcome.getStatus() != ConsistencyCheckOutcome.Status.CONSISTENT) && (consistencyCheckOutcome.getStatus() != ConsistencyCheckOutcome.Status.INCONSISTENT))
                                    {
                                        // Write event log TIMED_OUT_CHECKING_CONSISTENCY_OF_MODULE for the current module
                                        ModuleEventLog timedOutCheckingConsistencyOfModuleEventLog = new ModuleEventLog(baseIndividualStr, ModuleEvent.TIMED_OUT_CHECKING_CONSISTENCY_OF_MODULE, LocalDateTime.now());
                                        logger.addModuleEventLog(timedOutCheckingConsistencyOfModuleEventLog.toJsonString());
                                        // Print in stdout the TIMED_OUT_CHECKING_CONSISTENCY_OF_MODULE event
                                        System.out.println(timedOutCheckingConsistencyOfModuleEventLog.toJsonString());
                                        continue;
                                    }

                                    boolean aBoxConsistent = consistencyCheckOutcome.getConsistent();

                                    // Check if module is p-Repairable
                                    boolean pRepairable = true;
                                    if (!aBoxConsistent) {
                                        // Write event log STARTED_CHECKING_REPAIRABILITY_OF_MODULE for the current module
                                        ModuleEventLog startedInitialCheckingRepairabilityOfModuleEventLog = new ModuleEventLog(baseIndividualStr, ModuleEvent.STARTED_CHECKING_REPAIRABILITY_OF_MODULE, LocalDateTime.now());
                                        logger.addModuleEventLog(startedInitialCheckingRepairabilityOfModuleEventLog.toJsonString());
                                        // Print in stdout the STARTED_CHECKING_REPAIRABILITY_OF_MODULE event
                                        System.out.println(startedInitialCheckingRepairabilityOfModuleEventLog.toJsonString());

                                        long checkRepairabilityStartTimeMillis = System.currentTimeMillis();
                                        pRepairable = fixer.CheckRepairability().getRepairable();
                                        long checkRepairabilityEndTimeMillis = System.currentTimeMillis();
                                        checkRepairabilityTimeMillis = checkRepairabilityEndTimeMillis - checkRepairabilityStartTimeMillis;

                                        // Write event log ENDED_CHECKING_REPAIRABILITY_OF_MODULE for the current module
                                        ModuleEventLog endedInitialCheckingRepairabilityOfModuleEventLog = new ModuleEventLog(baseIndividualStr, ModuleEvent.ENDED_CHECKING_REPAIRABILITY_OF_MODULE, LocalDateTime.now());
                                        logger.addModuleEventLog(endedInitialCheckingRepairabilityOfModuleEventLog.toJsonString());
                                        // Print in stdout the ENDED_CHECKING_REPAIRABILITY_OF_MODULE event
                                        System.out.println(endedInitialCheckingRepairabilityOfModuleEventLog.toJsonString());
                                    }

                                    // Initialize a structure to keep the module's fixes
                                    // (in case it is initially inconsistent)
                                    Set<ArrayList<Fix>> fixesOfModule = new HashSet<>();

                                    int innerRound = 0;

                                    ModuleReasoningLog initialModuleReasoningLog = new ModuleReasoningLog(baseIndividualStr, outerRound, 0, checkConsistencyTimeMillis, checkRepairabilityTimeMillis, getExplanationsTimeMillis, computeFixesTimeMillis, explanationsCount);
                                    logger.addModuleReasoningLog(initialModuleReasoningLog.toJsonString());

                                    // While the module is inconsistent and p-repairable, keep trying to fix it
                                    while (!aBoxConsistent && pRepairable) {

                                        checkRepairabilityTimeMillis = -1;

                                        innerRound++;

                                        // Write event log STARTED_GETTING_EXPLANATIONS_OF_MODULE for the current module
                                        ModuleEventLog startedGettingExplanationsOfModuleEventLog = new ModuleEventLog(baseIndividualStr, ModuleEvent.STARTED_GETTING_EXPLANATIONS_OF_MODULE, LocalDateTime.now());
                                        logger.addModuleEventLog(startedGettingExplanationsOfModuleEventLog.toJsonString());
                                        // Print in stdout the STARTED_GETTING_EXPLANATIONS_OF_MODULE event
                                        System.out.println(startedGettingExplanationsOfModuleEventLog.toJsonString());

                                        long getExplanationsStartTimeMillis = System.currentTimeMillis();
                                        ExplanationOutcome explanationOutcome = moduleHandler.getExplanations(perOpTimeoutMillis, reasonerTimeoutMillis, explanationsLimit);
                                        long getExplanationsEndTimeMillis = System.currentTimeMillis();
                                        getExplanationsTimeMillis = getExplanationsEndTimeMillis - getExplanationsStartTimeMillis;

                                        // Write event log ENDED_GETTING_EXPLANATIONS_OF_MODULE for the current module
                                        ModuleEventLog endedGettingExplanationsOfModuleEventLog = new ModuleEventLog(baseIndividualStr, ModuleEvent.ENDED_GETTING_EXPLANATIONS_OF_MODULE, LocalDateTime.now());
                                        logger.addModuleEventLog(endedGettingExplanationsOfModuleEventLog.toJsonString());
                                        // Print in stdout the ENDED_GETTING_EXPLANATIONS_OF_MODULE event
                                        System.out.println(endedGettingExplanationsOfModuleEventLog.toJsonString());

                                        if (explanationOutcome.getStatus() == ExplanationOutcome.Status.FAILED) {
                                            // Write event log TIMED_OUT_GETTING_EXPLANATIONS_OF_MODULE for the current module
                                            ModuleEventLog timedOutGettingExplanationsOfModuleEventLog = new ModuleEventLog(baseIndividualStr, ModuleEvent.TIMED_OUT_GETTING_EXPLANATIONS_OF_MODULE, LocalDateTime.now());
                                            logger.addModuleEventLog(timedOutGettingExplanationsOfModuleEventLog.toJsonString());
                                            // Print in stdout the TIMED_OUT_GETTING_EXPLANATIONS_OF_MODULE event
                                            System.out.println(timedOutGettingExplanationsOfModuleEventLog.toJsonString());
                                            break;
                                        }

                                        List<List<OWLAxiom>> sortedExplanations = sortExplanations(explanationOutcome.getExplanations());

                                        explanationsCount = sortedExplanations.size();

                                        System.out.println(moduleExplanationsString(sortedExplanations, baseIndividualStr));

                                        // Write the inconsistency explanations found to hdfs
                                        ModuleExplanationsLog moduleExplanationsLog = new ModuleExplanationsLog(baseIndividualStr, outerRound, innerRound, sortedExplanations);
                                        logger.addModuleExplanationsLog(moduleExplanationsLog.toJsonString());

                                        // Write event log STARTED_COMPUTING_FIXES_OF_MODULE for the current module
                                        ModuleEventLog startedComputingFixesOfModuleEventLog = new ModuleEventLog(baseIndividualStr, ModuleEvent.STARTED_COMPUTING_FIXES_OF_MODULE, LocalDateTime.now());
                                        logger.addModuleEventLog(startedComputingFixesOfModuleEventLog.toJsonString());
                                        // Print in stdout the STARTED_COMPUTING_FIXES_OF_MODULE event
                                        System.out.println(startedComputingFixesOfModuleEventLog.toJsonString());

                                        // Variable to keep sound fixes
                                        long computeFixesStartTimeMillis = System.currentTimeMillis();
                                        HashSet<Fix> sFixes;
                                        if (fixSelection == 2 || fixSelection == 1) {
                                            sFixes = fixer.ComputeSomeSoundFixes(explanationOutcome.getExplanations());
                                        } else if (fixSelection == 4) {
                                            sFixes = fixer.ComputeIARFixes(explanationOutcome.getExplanations());
                                        } else {
                                            HashSet<Fix> fixes = fixer.ComputeFixes(explanationOutcome.getExplanations());
                                            sFixes = fixer.KeepSoundFixes(fixes);
                                        }
                                        long computeFixesEndTimeMillis = System.currentTimeMillis();
                                        computeFixesTimeMillis = computeFixesEndTimeMillis - computeFixesStartTimeMillis;

                                        // Write event log ENDED_COMPUTING_FIXES_OF_MODULE for the current module
                                        ModuleEventLog endedComputingFixesOfModuleEventLog = new ModuleEventLog(baseIndividualStr, ModuleEvent.ENDED_COMPUTING_FIXES_OF_MODULE, LocalDateTime.now());
                                        logger.addModuleEventLog(endedComputingFixesOfModuleEventLog.toJsonString());
                                        // Print in stdout the ENDED_COMPUTING_FIXES_OF_MODULE event
                                        System.out.println(endedComputingFixesOfModuleEventLog.toJsonString());

                                        // Choose and apply a fix
                                        if (!sFixes.isEmpty()) {
                                            if (fixSelection != 4) {
                                                int randomNumber = random.nextInt(sFixes.size());
                                                Iterator<Fix> it = sFixes.iterator();
                                                int currentIndex = 0;
                                                Fix chosenFix = null;
                                                while (it.hasNext()) {
                                                    Fix tmpFix = it.next();
                                                    if (currentIndex == randomNumber)
                                                        chosenFix = tmpFix;
                                                    currentIndex++;
                                                }
                                                // Apply the chosen fix
                                                if (chosenFix != null) {
                                                    fixer.applyFix(chosenFix);
                                                    System.out.println("Fix " + chosenFix + " applied in module.");
                                                    updateFixesOfModule(chosenFix, fixesOfModule);
                                                }
                                            } else {
                                                for (Fix fix : sFixes) {
                                                    fixer.applyFix(fix);
                                                    System.out.println("Fix " + fix + " applied in module.");
                                                    updateFixesOfModule(fix, fixesOfModule);
                                                }
                                            }
                                        } else {
                                            System.out.println("\n******* No Sound fixes available!");
                                        }

                                        // Write event log STARTED_CHECKING_CONSISTENCY_OF_MODULE for the current module
                                        ModuleEventLog startedCheckingConsistencyOfModuleEventLog = new ModuleEventLog(baseIndividualStr, ModuleEvent.STARTED_CHECKING_CONSISTENCY_OF_MODULE, LocalDateTime.now());
                                        logger.addModuleEventLog(startedCheckingConsistencyOfModuleEventLog.toJsonString());
                                        // Print in stdout the STARTED_CHECKING_CONSISTENCY_OF_MODULE event
                                        System.out.println(startedCheckingConsistencyOfModuleEventLog.toJsonString());

                                        checkConsistencyStartTimeMillis = System.currentTimeMillis();
                                        consistencyCheckOutcome = fixer.CheckConsistency(reasonerTimeoutMillis);
                                        checkConsistencyEndTimeMillis = System.currentTimeMillis();
                                        checkConsistencyTimeMillis = checkConsistencyEndTimeMillis - checkConsistencyStartTimeMillis;

                                        // Write event log ENDED_CHECKING_CONSISTENCY_OF_MODULE for the current module
                                        ModuleEventLog endedCheckingConsistencyOfModuleEventLog = new ModuleEventLog(baseIndividualStr, ModuleEvent.ENDED_CHECKING_CONSISTENCY_OF_MODULE, LocalDateTime.now());
                                        logger.addModuleEventLog(endedCheckingConsistencyOfModuleEventLog.toJsonString());
                                        // Print in stdout the ENDED_CHECKING_CONSISTENCY_OF_MODULE event
                                        System.out.println(endedCheckingConsistencyOfModuleEventLog.toJsonString());

                                        if ((consistencyCheckOutcome.getStatus() != ConsistencyCheckOutcome.Status.CONSISTENT) && (consistencyCheckOutcome.getStatus() != ConsistencyCheckOutcome.Status.INCONSISTENT))
                                        {
                                            // Write event log TIMED_OUT_CHECKING_CONSISTENCY_OF_MODULE for the current module
                                            ModuleEventLog timedOutCheckingConsistencyOfModuleEventLog = new ModuleEventLog(baseIndividualStr, ModuleEvent.TIMED_OUT_CHECKING_CONSISTENCY_OF_MODULE, LocalDateTime.now());
                                            logger.addModuleEventLog(timedOutCheckingConsistencyOfModuleEventLog.toJsonString());
                                            // Print in stdout the TIMED_OUT_CHECKING_CONSISTENCY_OF_MODULE event
                                            System.out.println(timedOutCheckingConsistencyOfModuleEventLog.toJsonString());
                                            break;
                                        }

                                        aBoxConsistent = consistencyCheckOutcome.getConsistent();

                                        if (aBoxConsistent) {
                                            System.out.println("\n++++++++++++++++ The module is Consistent! [ " + baseIndividualStr + " ]");
                                        } else {
                                            // Write event log STARTED_CHECKING_REPAIRABILITY_OF_MODULE for the current module
                                            ModuleEventLog startedCheckingRepairabilityOfModuleEventLog = new ModuleEventLog(baseIndividualStr, ModuleEvent.STARTED_CHECKING_REPAIRABILITY_OF_MODULE, LocalDateTime.now());
                                            logger.addModuleEventLog(startedCheckingRepairabilityOfModuleEventLog.toJsonString());
                                            // Print in stdout the STARTED_CHECKING_REPAIRABILITY_OF_MODULE event
                                            System.out.println(startedCheckingRepairabilityOfModuleEventLog.toJsonString());

                                            long checkRepairabilityStartTimeMillis = System.currentTimeMillis();
                                            pRepairable = fixer.CheckRepairability().getRepairable();
                                            long checkRepairabilityEndTimeMillis = System.currentTimeMillis();
                                            checkRepairabilityTimeMillis = checkRepairabilityEndTimeMillis - checkRepairabilityStartTimeMillis;

                                            // Write event log ENDED_CHECKING_REPAIRABILITY_OF_MODULE for the current module
                                            ModuleEventLog endedCheckingRepairabilityOfModuleEventLog = new ModuleEventLog(baseIndividualStr, ModuleEvent.ENDED_CHECKING_REPAIRABILITY_OF_MODULE, LocalDateTime.now());
                                            logger.addModuleEventLog(endedCheckingRepairabilityOfModuleEventLog.toJsonString());
                                            // Print in stdout the ENDED_CHECKING_REPAIRABILITY_OF_MODULE event
                                            System.out.println(endedCheckingRepairabilityOfModuleEventLog.toJsonString());

                                            if (pRepairable) {
                                                System.out.println("\n++++++++++++++++ The module is Inconsistent but P-repairable! [ " + baseIndividualStr + " ]");
                                            } else {
                                                System.out.println("\n++++++++++++++++ The module is Inconsistent and Not P-repairable! [ " + baseIndividualStr + " ]");
                                                throw new RuntimeException("P-repairability check failed!");
                                            }
                                        }

                                        ModuleReasoningLog moduleReasoningLog = new ModuleReasoningLog(baseIndividualStr, outerRound, innerRound, checkConsistencyTimeMillis, checkRepairabilityTimeMillis, getExplanationsTimeMillis, computeFixesTimeMillis, explanationsCount);
                                        logger.addModuleReasoningLog(moduleReasoningLog.toJsonString());
                                    }

                                    ModuleInnerRoundNumLog moduleInnerRoundNumLog = new ModuleInnerRoundNumLog(baseIndividualStr, outerRound, innerRound);
                                    logger.addModuleInnerRoundNumLog(moduleInnerRoundNumLog.toJsonString());

                                    ModuleFixesLog moduleFixesLog = new ModuleFixesLog(baseIndividualStr, outerRound, innerRound, fixesOfModule);
                                    logger.addModuleFixesLog(moduleFixesLog.toJsonString());
                                }
                            }
                        }
                    }
                });

                long explanationsNumber;
                // After processing all partitions, read all explanation logs, merge and deduplicate them, and write the unique explanations to hdfs
                if (hdfsIO.hasFiles(moduleExplanationsLogsDir)) {
                    // 1) Read raw NDJSON lines
                    JavaRDD<String> raw = sc.textFile(hdfsLocation + "/" + moduleExplanationsLogsDir);

                    // 2) Fast pre-filter: drop empties/whitespace and obvious non-JSON lines
                    JavaRDD<String> candidates = raw.filter(s -> {
                        if (s == null) return false;
                        String t = s.trim();
                        return !t.isEmpty() && t.charAt(0) == '{';
                    });

                    // 3) Merge/deduplicate on the filtered lines (ensure your mergeModuleExplanations
                    //    does safe parsing internally, or make a tryParseOrNull and filter nulls)
                    JavaRDD<String> mergedExplanationLines = mergeModuleExplanations(candidates)
                            .persist(org.apache.spark.storage.StorageLevel.MEMORY_AND_DISK()); // we'll use it twice

                    // 4) Count uniques
                    explanationsNumber = mergedExplanationLines.count();

                    // 5) Write a single NDJSON file
                    String tmpSingleDir = hdfsLocation + "/" + outputDirectoryPath + "/merged_explanations_" + java.util.UUID.randomUUID();
                    mergedExplanationLines.coalesce(1, true).saveAsTextFile(tmpSingleDir);
                    hdfsIO.moveSinglePartFile(tmpSingleDir, mergedExplanationsFilePath);
                } else {
                    explanationsNumber = 0;
                }
                // Write the total number of unique explanations found to hdfs
                hdfsIO.writeStringToHDFS("Number of explanations: " + explanationsNumber, explanationsNumberFilePath);
                System.out.println("Number of explanations: " + explanationsNumber);

                // If there are fixes
                if (hdfsIO.hasFiles(moduleFixesLogsDir)) {
                    // After processing all partitions, read all explanation logs, merge and deduplicate them, and write the unique explanations to hdfs
                    // 1) Read raw NDJSON lines
                    JavaRDD<String> rawFixes = sc.textFile(hdfsLocation + "/" + moduleFixesLogsDir);

                    // 2) Fast pre-filter: drop blanks / whitespace-only / non-JSON
                    JavaRDD<String> candidatesFixes = rawFixes.filter(s -> {
                        if (s == null) return false;
                        String t = s.trim();
                        return !t.isEmpty() && t.charAt(0) == '{';
                    });

                    // 3) Merge & deduplicate (ensure mergeModuleFixes handles safe parsing inside)
                    JavaRDD<String> mergedFixesLines = mergeModuleFixes(candidatesFixes);
//                            .persist(org.apache.spark.storage.StorageLevel.MEMORY_AND_DISK()); // <- only if you also do .count()

                    // (optional) Count uniques:
                    // long fixesNumber = mergedFixesLines.count();
                    // hdfsIO.writeStringToHDFS("Number of fixes: " + fixesNumber, fixesNumberFilePath);

                    // 4) Write a single NDJSON file
                    String tmpSingleDirFixes = hdfsLocation + "/" + outputDirectoryPath + "/merged_fixes_" + java.util.UUID.randomUUID();
                    mergedFixesLines.coalesce(1, true).saveAsTextFile(tmpSingleDirFixes);
                    hdfsIO.moveSinglePartFile(tmpSingleDirFixes, mergedFixesFilePath);

                    PositionTracker globalPositionTracker;
                    if (outerRound > 1) {
                        globalPositionTracker = PositionTracker.fromJsonString(hdfsIO.readStringFromHDFS(inputPositionTrackerFilePath));
                    } else {
                        globalPositionTracker = new PositionTracker();
                    }
                    TBoxHandler tBoxHandler = new TBoxHandler(tBoxFilePath);
                    try (ABoxHandler aBoxHandler = new ABoxHandler(sparqlEndpoint, graphDomain, tBoxHandler, globalPositionTracker)) {
                        mergedFixesLines.foreach(line -> {
                            try {
                                FixesLog fixesLog = FixesLog.fromJsonString(line);
                                boolean applied = aBoxHandler.applyFixToTripleStore(fixesLog.getFixes().stream().findFirst().orElseThrow(() -> new Exception("No fixes found in FixesLog!")));
                                if (!applied) {
                                    throw new Exception("Fixes could not be applied to triple store! " + line);
                                }
                            } catch (Exception e) {
                                System.err.println("Error processing line: " + e.getMessage());
                            }
                        });
                    }

                    hdfsIO.writeStringToHDFS(globalPositionTracker.toJsonString(), outputPositionTrackerFilePath);
                }

                //  Get end time in millis
                long endTimeMillis = System.currentTimeMillis();
                // Get end date time
                LocalDateTime endDateTime = LocalDateTime.now();

                //  Calculate total time in millis
                long totalTimeMillis = endTimeMillis - startTimeMillis;

                // Write the start date time to hdfs
                hdfsIO.writeStringToHDFS("End date time by Spark: " + endDateTime, sparkTotalTimeFilePath);
                System.out.println("End date time by Spark: " + endDateTime);

                // Write the total time in millis to hdfs
                hdfsIO.writeStringToHDFS("Total time elapsed by Spark: " + totalTimeMillis + " ms", sparkTotalTimeFilePath);
                System.out.println("Total time elapsed by Spark: " + totalTimeMillis + " ms");

                if ((!hdfsIO.hasFiles(moduleFixesLogsDir)) || (fixSelection == 1) || (fixSelection == 4)) {
                    // Write stop flag to hdfs
                    hdfsIO.writeStringToHDFS("stop", stopLoopFlagFilePath);
                    System.out.println("Writing stop flag to hdfs.");
                }
            }
            else if (mode == 6) {
                // -----------------------------------------------------
                // Mode 1 (Get explanations and fix) with bin-packing
                // -----------------------------------------------------

                // Create an HDFSIO object
                HDFSIO hdfsIO = new HDFSIO(hdfsLocation);

                // Create an RDD
                JavaRDD<String> rdd = sc.textFile(kHopLinksFilePath, partitionsNumber);

                //  Get start time in millis
                long startTimeMillis = System.currentTimeMillis();
                // Get start date time
                LocalDateTime startDateTime = LocalDateTime.now();

                // Write the start time in millis to hdfs
                hdfsIO.writeStringToHDFS("Start date time by Spark: " + startDateTime, sparkTotalTimeFilePath);
                System.out.println("Start date time by Spark: " + startDateTime);

                rdd.foreachPartition((Iterator<String> partitionIterator) -> {
                    // Define the partition file name
                    String partitionFileName = "partition_" + TaskContext.getPartitionId() + ".ndjson";

                    // Create a Random object
                    Random random = new Random();

                    // Read the global position tracker from hdfs if outerRound > 1 else create a new one
                    PositionTracker globalPositionTracker;
                    if (outerRound > 1) {
                        globalPositionTracker = PositionTracker.fromJsonString(hdfsIO.readStringFromHDFS(inputPositionTrackerFilePath));
                    } else {
                        globalPositionTracker = new PositionTracker();
                    }

                    // Create TBoxHandler and ABoxHandler objects
                    TBoxHandler tBoxHandler = new TBoxHandler(tBoxFilePath);
                    try (ABoxHandler aBoxHandler = new ABoxHandler(sparqlEndpoint, graphDomain, tBoxHandler, new PositionTracker(globalPositionTracker))) {

                        // Create an HDFSIO object
                        HDFSIO partitionHdfsIO = new HDFSIO(hdfsLocation);

                        // Initialize list of modules structure that will be used in bin-packing algorithm
                        List<ModuleData> modules = new ArrayList<>();

                        try (PartitionLogger logger = new PartitionLogger(
                                partitionHdfsIO,
                                moduleEventLogsDir,
                                moduleExplanationsLogsDir,
                                moduleFixesLogsDir,
                                moduleInnerRoundNumLogsDir,
                                moduleReasoningLogsDir,
                                moduleSizeLogsDir,
                                binEventLogsDir,
                                binExplanationsLogsDir,
                                binFixesLogsDir,
                                binInnerRoundNumLogsDir,
                                binReasoningLogsDir,
                                binSizeLogsDir,
                                partitionFileName,
                                1000
                        )) {
                            // For each line in the partition we process
                            while (partitionIterator.hasNext()) {

                                // Read the next line
                                String line = partitionIterator.next();

                                // Extract the base individual from the input line
                                String baseIndividualStr = extractBaseIndividualStr(line);

                                // Get the neighbors of the base individual and their distance
                                HashMap<String, Integer> neighborsMap = extractNeighborsMap(line);

                                // Construct a list with the base individual and its neighbors if extend is enabled
                                ArrayList<String> allIndividuals = new ArrayList<>();
                                allIndividuals.add(baseIndividualStr);
                                if (extend) {
                                    allIndividuals.addAll(neighborsMap.keySet());
                                }

                                // Write event log STARTED_COUNTING_TRIPLES_OF_MODULE for the current module
                                ModuleEventLog startedCountingTriplesOfModuleEventLog = new ModuleEventLog(baseIndividualStr, ModuleEvent.STARTED_COUNTING_TRIPLES_OF_MODULE, LocalDateTime.now());
                                logger.addModuleEventLog(startedCountingTriplesOfModuleEventLog.toJsonString());
                                // Print in stdout the STARTED_COUNTING_TRIPLES_OF_MODULE event
                                System.out.println(startedCountingTriplesOfModuleEventLog.toJsonString());

                                // Get module size
                                int moduleSize = aBoxHandler.countIndividualTriples(allIndividuals);

                                // Write event log ENDED_COUNTING_TRIPLES_OF_MODULE for the current module
                                ModuleEventLog endedCountingTriplesOfModuleEventLog = new ModuleEventLog(baseIndividualStr, ModuleEvent.ENDED_COUNTING_TRIPLES_OF_MODULE, LocalDateTime.now());
                                logger.addModuleEventLog(endedCountingTriplesOfModuleEventLog.toJsonString());
                                // Print in stdout the ENDED_COUNTING_TRIPLES_OF_MODULE event
                                System.out.println(endedCountingTriplesOfModuleEventLog.toJsonString());

                                // Write module size log for the current module
                                ModuleSizeLog moduleSizeLog = new ModuleSizeLog(baseIndividualStr, outerRound, moduleSize);
                                logger.addModuleSizeLog(moduleSizeLog.toJsonString());

                                // Create ModuleData object hosting the info of the current module
                                ModuleData moduleData = new ModuleData(baseIndividualStr, new ArrayList<>(neighborsMap.keySet()), moduleSize);
                                modules.add(moduleData);
                            }

                            // Initialize the list of bins for the bin-packing
                            List<Bin> bins = new ArrayList<>();

                            // Best-fit bin packing
                            assignModulesToBins(modules, bins, binCapacity);

                            // For each bin
                            for (Bin b : bins) {

                                // Merge all base individuals and all their neighbors
                                ArrayList<String> allIndividuals = new ArrayList<>();
                                allIndividuals.addAll(b.baseIndividuals());
                                allIndividuals.addAll(b.mergedNeighbors());

                                // Write event log STARTED_FETCHING_TRIPLES_OF_BIN for the current module
                                BinEventLog startedFetchingTriplesOfBinEventLog = new BinEventLog(b.baseIndividuals(), BinEvent.STARTED_FETCHING_TRIPLES_OF_BIN, LocalDateTime.now());
                                logger.addBinEventLog(startedFetchingTriplesOfBinEventLog.toJsonString());
                                // Print in stdout the STARTED_FETCHING_TRIPLES_OF_BIN event
                                System.out.println(startedFetchingTriplesOfBinEventLog.toJsonString());

                                // Create a set of the ABox axioms
                                Set<OWLAxiom> abox = new HashSet<>(aBoxHandler.getABoxModules(allIndividuals));

                                // Write event log ENDED_FETCHING_TRIPLES_OF_BIN for the current module
                                BinEventLog endedFetchingTriplesOfBinEventLog = new BinEventLog(b.baseIndividuals(), BinEvent.ENDED_FETCHING_TRIPLES_OF_BIN, LocalDateTime.now());
                                logger.addBinEventLog(endedFetchingTriplesOfBinEventLog.toJsonString());
                                // Print in stdout the ENDED_FETCHING_TRIPLES_OF_BIN event
                                System.out.println(endedFetchingTriplesOfBinEventLog.toJsonString());

                                // Create a set of the TBox axioms
                                Set<OWLAxiom> tBox = tBoxHandler.getOntologyTBox();

                                // Create a set of all axioms (ABox and TBox)
                                Set<OWLAxiom> all_boxes = new HashSet<>();
                                all_boxes.addAll(abox);
                                all_boxes.addAll(tBox);

                                // Write event log STARTED_COUNTING_TRIPLES_OF_BIN for the current module
                                BinEventLog startedCountingTriplesOfBinEventLog = new BinEventLog(b.baseIndividuals(), BinEvent.STARTED_COUNTING_TRIPLES_OF_BIN, LocalDateTime.now());
                                logger.addBinEventLog(startedCountingTriplesOfBinEventLog.toJsonString());
                                // Print in stdout the STARTED_COUNTING_TRIPLES_OF_BIN event
                                System.out.println(startedCountingTriplesOfBinEventLog.toJsonString());

                                // Get module size
                                int binSize = abox.size();

                                // Write event log ENDED_COUNTING_TRIPLES_OF_BIN for the current module
                                BinEventLog endedCountingTriplesOfBinEventLog = new BinEventLog(b.baseIndividuals(), BinEvent.ENDED_COUNTING_TRIPLES_OF_BIN, LocalDateTime.now());
                                logger.addBinEventLog(endedCountingTriplesOfBinEventLog.toJsonString());
                                // Print in stdout the ENDED_COUNTING_TRIPLES_OF_BIN event
                                System.out.println(endedCountingTriplesOfBinEventLog.toJsonString());

                                // Write bin size log for the current module
                                BinSizeLog binSizeLog = new BinSizeLog(b.baseIndividuals(), outerRound, binSize);
                                logger.addBinSizeLog(binSizeLog.toJsonString());

                                // Create a ModuleHandler object that will host the whole ontology
                                try (ModuleHandler moduleHandler = new ModuleHandler(all_boxes, IRI.generateDocumentIRI(), String.join(",", b.baseIndividuals()), reasonerSelection)) {

                                    // Initialize metrics
                                    long checkConsistencyTimeMillis;
                                    long checkRepairabilityTimeMillis = -1;
                                    long getExplanationsTimeMillis = -1;
                                    long computeFixesTimeMillis = -1;
                                    int explanationsCount = -1;

                                    // Initialize the module's fixer
                                    Fixer fixer = new Fixer(moduleHandler.getOntology(), new PositionTracker(globalPositionTracker), fixSelection, fixingTimeoutMillis, reasonerSelection, reasonerTimeoutMillis, perOpTimeoutMillis, explanationsLimit, mcd);

                                    // Write event log STARTED_CHECKING_CONSISTENCY_OF_BIN for the current module
                                    BinEventLog startedInitialCheckingConsistencyOfBinEventLog = new BinEventLog(b.baseIndividuals(), BinEvent.STARTED_CHECKING_CONSISTENCY_OF_BIN, LocalDateTime.now());
                                    logger.addBinEventLog(startedInitialCheckingConsistencyOfBinEventLog.toJsonString());
                                    // Print in stdout the STARTED_CHECKING_CONSISTENCY_OF_BIN event
                                    System.out.println(startedInitialCheckingConsistencyOfBinEventLog.toJsonString());

                                    // Check if module is consistent
                                    long checkConsistencyStartTimeMillis = System.currentTimeMillis();
                                    ConsistencyCheckOutcome consistencyCheckOutcome = fixer.CheckConsistency(reasonerTimeoutMillis);
                                    long checkConsistencyEndTimeMillis = System.currentTimeMillis();
                                    checkConsistencyTimeMillis = checkConsistencyEndTimeMillis - checkConsistencyStartTimeMillis;

                                    // Write event log ENDED_CHECKING_CONSISTENCY_OF_BIN for the current module
                                    BinEventLog endedInitialCheckingConsistencyOfBinEventLog = new BinEventLog(b.baseIndividuals(), BinEvent.ENDED_CHECKING_CONSISTENCY_OF_BIN, LocalDateTime.now());
                                    logger.addBinEventLog(endedInitialCheckingConsistencyOfBinEventLog.toJsonString());
                                    // Print in stdout the ENDED_CHECKING_CONSISTENCY_OF_BIN event
                                    System.out.println(endedInitialCheckingConsistencyOfBinEventLog.toJsonString());

                                    if ((consistencyCheckOutcome.getStatus() != ConsistencyCheckOutcome.Status.CONSISTENT) && (consistencyCheckOutcome.getStatus() != ConsistencyCheckOutcome.Status.INCONSISTENT))
                                    {
                                        // Write event log TIMED_OUT_CHECKING_CONSISTENCY_OF_BIN for the current module
                                        BinEventLog timedOutInitialCheckingConsistencyOfBinEventLog = new BinEventLog(b.baseIndividuals(), BinEvent.TIMED_OUT_CHECKING_CONSISTENCY_OF_BIN, LocalDateTime.now());
                                        logger.addBinEventLog(timedOutInitialCheckingConsistencyOfBinEventLog.toJsonString());
                                        // Print in stdout the TIMED_OUT_CHECKING_CONSISTENCY_OF_BIN event
                                        System.out.println(timedOutInitialCheckingConsistencyOfBinEventLog.toJsonString());
                                        continue;
                                    }

                                    boolean aBoxConsistent = consistencyCheckOutcome.getConsistent();

                                    // Check if module is p-Repairable
                                    boolean pRepairable = true;
                                    if (!aBoxConsistent) {
                                        // Write event log STARTED_CHECKING_REPAIRABILITY_OF_BIN for the current module
                                        BinEventLog startedInitialCheckingRepairabilityOfBinEventLog = new BinEventLog(b.baseIndividuals(), BinEvent.STARTED_CHECKING_REPAIRABILITY_OF_BIN, LocalDateTime.now());
                                        logger.addBinEventLog(startedInitialCheckingRepairabilityOfBinEventLog.toJsonString());
                                        // Print in stdout the STARTED_CHECKING_REPAIRABILITY_OF_BIN event
                                        System.out.println(startedInitialCheckingRepairabilityOfBinEventLog.toJsonString());

                                        long checkRepairabilityStartTimeMillis = System.currentTimeMillis();
                                        pRepairable = fixer.CheckRepairability().getRepairable();
                                        long checkRepairabilityEndTimeMillis = System.currentTimeMillis();
                                        checkRepairabilityTimeMillis = checkRepairabilityEndTimeMillis - checkRepairabilityStartTimeMillis;

                                        // Write event log ENDED_CHECKING_REPAIRABILITY_OF_BIN for the current module
                                        BinEventLog endedInitialCheckingRepairabilityOfBinEventLog = new BinEventLog(b.baseIndividuals(), BinEvent.ENDED_CHECKING_REPAIRABILITY_OF_BIN, LocalDateTime.now());
                                        logger.addBinEventLog(endedInitialCheckingRepairabilityOfBinEventLog.toJsonString());
                                        // Print in stdout the ENDED_CHECKING_REPAIRABILITY_OF_BIN event
                                        System.out.println(endedInitialCheckingRepairabilityOfBinEventLog.toJsonString());
                                    }

                                    // Initialize a structure to keep the module's fixes
                                    // (in case it is initially inconsistent)
                                    Set<ArrayList<Fix>> fixesOfBin = new HashSet<>();

                                    int innerRound = 0;

                                    BinReasoningLog initialBinReasoningLog = new BinReasoningLog(b.baseIndividuals(), outerRound, 0, checkConsistencyTimeMillis, checkRepairabilityTimeMillis, getExplanationsTimeMillis, computeFixesTimeMillis, explanationsCount);
                                    logger.addBinReasoningLog(initialBinReasoningLog.toJsonString());

                                    // While the module is inconsistent and p-repairable, keep trying to fix it
                                    while (!aBoxConsistent && pRepairable) {

                                        checkRepairabilityTimeMillis = -1;

                                        innerRound++;

                                        // Write event log STARTED_GETTING_EXPLANATIONS_OF_BIN for the current module
                                        BinEventLog startedGettingExplanationsOfBinEventLog = new BinEventLog(b.baseIndividuals(), BinEvent.STARTED_GETTING_EXPLANATIONS_OF_BIN, LocalDateTime.now());
                                        logger.addBinEventLog(startedGettingExplanationsOfBinEventLog.toJsonString());
                                        // Print in stdout the STARTED_GETTING_EXPLANATIONS_OF_BIN event
                                        System.out.println(startedGettingExplanationsOfBinEventLog.toJsonString());

                                        long getExplanationsStartTimeMillis = System.currentTimeMillis();
                                        ExplanationOutcome explanationOutcome = moduleHandler.getExplanations(perOpTimeoutMillis, reasonerTimeoutMillis, explanationsLimit);
                                        long getExplanationsEndTimeMillis = System.currentTimeMillis();
                                        getExplanationsTimeMillis = getExplanationsEndTimeMillis - getExplanationsStartTimeMillis;

                                        // Write event log ENDED_GETTING_EXPLANATIONS_OF_BIN for the current module
                                        BinEventLog endedGettingExplanationsOfBinEventLog = new BinEventLog(b.baseIndividuals(), BinEvent.ENDED_GETTING_EXPLANATIONS_OF_BIN, LocalDateTime.now());
                                        logger.addBinEventLog(endedGettingExplanationsOfBinEventLog.toJsonString());
                                        // Print in stdout the ENDED_GETTING_EXPLANATIONS_OF_BIN event
                                        System.out.println(endedGettingExplanationsOfBinEventLog.toJsonString());

                                        if (explanationOutcome.getStatus() != ExplanationOutcome.Status.FAILED)
                                        {
                                            // Write event log TIMED_OUT_GETTING_EXPLANATIONS_OF_BIN for the current module
                                            BinEventLog timedOutGettingExplanationsOfBinEventLog = new BinEventLog(b.baseIndividuals(), BinEvent.TIMED_OUT_GETTING_EXPLANATIONS_OF_BIN, LocalDateTime.now());
                                            logger.addBinEventLog(timedOutGettingExplanationsOfBinEventLog.toJsonString());
                                            // Print in stdout the TIMED_OUT_GETTING_EXPLANATIONS_OF_BIN event
                                            System.out.println(timedOutGettingExplanationsOfBinEventLog.toJsonString());
                                            break;
                                        }

                                        List<List<OWLAxiom>> sortedExplanations = sortExplanations(explanationOutcome.getExplanations());

                                        explanationsCount = sortedExplanations.size();

                                        System.out.println(moduleExplanationsString(sortedExplanations, String.join(",", b.baseIndividuals())));

                                        // Write the inconsistency explanations found to hdfs
                                        BinExplanationsLog binExplanationsLog = new BinExplanationsLog(b.baseIndividuals(), outerRound, innerRound, sortedExplanations);
                                        logger.addBinExplanationsLog(binExplanationsLog.toJsonString());

                                        // Write event log STARTED_COMPUTING_FIXES_OF_BIN for the current module
                                        BinEventLog startedComputingFixesOfBinEventLog = new BinEventLog(b.baseIndividuals(), BinEvent.STARTED_COMPUTING_FIXES_OF_BIN, LocalDateTime.now());
                                        logger.addBinEventLog(startedComputingFixesOfBinEventLog.toJsonString());
                                        // Print in stdout the STARTED_COMPUTING_FIXES_OF_BIN event
                                        System.out.println(startedComputingFixesOfBinEventLog.toJsonString());

                                        // Variable to keep sound fixes
                                        long computeFixesStartTimeMillis = System.currentTimeMillis();
                                        HashSet<Fix> sFixes;
                                        if (fixSelection == 2 || fixSelection == 1) {
                                            sFixes = fixer.ComputeSomeSoundFixes(explanationOutcome.getExplanations());
                                        } else if (fixSelection == 4) {
                                            sFixes = fixer.ComputeIARFixes(explanationOutcome.getExplanations());
                                        } else {
                                            HashSet<Fix> fixes = fixer.ComputeFixes(explanationOutcome.getExplanations());
                                            sFixes = fixer.KeepSoundFixes(fixes);
                                        }
                                        long computeFixesEndTimeMillis = System.currentTimeMillis();
                                        computeFixesTimeMillis = computeFixesEndTimeMillis - computeFixesStartTimeMillis;

                                        // Write event log ENDED_COMPUTING_FIXES_OF_BIN for the current module
                                        BinEventLog endedComputingFixesOfBinEventLog = new BinEventLog(b.baseIndividuals(), BinEvent.ENDED_COMPUTING_FIXES_OF_BIN, LocalDateTime.now());
                                        logger.addBinEventLog(endedComputingFixesOfBinEventLog.toJsonString());
                                        // Print in stdout the ENDED_COMPUTING_FIXES_OF_BIN event
                                        System.out.println(endedComputingFixesOfBinEventLog.toJsonString());

                                        // Choose and apply a fix
                                        if (!sFixes.isEmpty()) {
                                            if (fixSelection != 4) {
                                                int randomNumber = random.nextInt(sFixes.size());
                                                Iterator<Fix> it = sFixes.iterator();
                                                int currentIndex = 0;
                                                Fix chosenFix = null;
                                                while (it.hasNext()) {
                                                    Fix tmpFix = it.next();
                                                    if (currentIndex == randomNumber)
                                                        chosenFix = tmpFix;
                                                    currentIndex++;
                                                }
                                                // Apply the chosen fix
                                                if (chosenFix != null) {
                                                    fixer.applyFix(chosenFix);
                                                    System.out.println("Fix " + chosenFix + " applied in module.");
                                                    updateFixesOfModule(chosenFix, fixesOfBin);
                                                }
                                            } else {
                                                for (Fix fix : sFixes) {
                                                    fixer.applyFix(fix);
                                                    System.out.println("Fix " + fix + " applied in module.");
                                                    updateFixesOfModule(fix, fixesOfBin);
                                                }
                                            }
                                        } else {
                                            System.out.println("\n******* No Sound fixes available!");
                                        }

                                        // Write event log STARTED_CHECKING_CONSISTENCY_OF_BIN for the current module
                                        BinEventLog startedCheckingConsistencyOfBinEventLog = new BinEventLog(b.baseIndividuals(), BinEvent.STARTED_CHECKING_CONSISTENCY_OF_BIN, LocalDateTime.now());
                                        logger.addBinEventLog(startedCheckingConsistencyOfBinEventLog.toJsonString());
                                        // Print in stdout the STARTED_CHECKING_CONSISTENCY_OF_BIN event
                                        System.out.println(startedCheckingConsistencyOfBinEventLog.toJsonString());

                                        checkConsistencyStartTimeMillis = System.currentTimeMillis();
                                        consistencyCheckOutcome = fixer.CheckConsistency(reasonerTimeoutMillis);
                                        checkConsistencyEndTimeMillis = System.currentTimeMillis();
                                        checkConsistencyTimeMillis = checkConsistencyEndTimeMillis - checkConsistencyStartTimeMillis;

                                        // Write event log ENDED_CHECKING_CONSISTENCY_OF_BIN for the current module
                                        BinEventLog endedCheckingConsistencyOfBinEventLog = new BinEventLog(b.baseIndividuals(), BinEvent.ENDED_CHECKING_CONSISTENCY_OF_BIN, LocalDateTime.now());
                                        logger.addBinEventLog(endedCheckingConsistencyOfBinEventLog.toJsonString());
                                        // Print in stdout the ENDED_CHECKING_CONSISTENCY_OF_BIN event
                                        System.out.println(endedCheckingConsistencyOfBinEventLog.toJsonString());

                                        if ((consistencyCheckOutcome.getStatus() != ConsistencyCheckOutcome.Status.CONSISTENT) && (consistencyCheckOutcome.getStatus() != ConsistencyCheckOutcome.Status.INCONSISTENT))
                                        {
                                            // Write event log TIMED_OUT_CHECKING_CONSISTENCY_OF_BIN for the current module
                                            BinEventLog timedOutCheckingConsistencyOfBinEventLog = new BinEventLog(b.baseIndividuals(), BinEvent.TIMED_OUT_CHECKING_CONSISTENCY_OF_BIN, LocalDateTime.now());
                                            logger.addBinEventLog(timedOutCheckingConsistencyOfBinEventLog.toJsonString());
                                            // Print in stdout the TIMED_OUT_CHECKING_CONSISTENCY_OF_BIN event
                                            System.out.println(timedOutCheckingConsistencyOfBinEventLog.toJsonString());
                                            break;
                                        }

                                        aBoxConsistent = consistencyCheckOutcome.getConsistent();

                                        if (aBoxConsistent) {
                                            System.out.println("\n++++++++++++++++ The module is Consistent! [ " + String.join(",", b.baseIndividuals()) + " ]");
                                        } else {
                                            // Write event log STARTED_CHECKING_REPAIRABILITY_OF_BIN for the current module
                                            BinEventLog startedCheckingRepairabilityOfBinEventLog = new BinEventLog(b.baseIndividuals(), BinEvent.STARTED_CHECKING_REPAIRABILITY_OF_BIN, LocalDateTime.now());
                                            logger.addBinEventLog(startedCheckingRepairabilityOfBinEventLog.toJsonString());
                                            // Print in stdout the STARTED_CHECKING_REPAIRABILITY_OF_BIN event
                                            System.out.println(startedCheckingRepairabilityOfBinEventLog.toJsonString());

                                            long checkRepairabilityStartTimeMillis = System.currentTimeMillis();
                                            pRepairable = fixer.CheckRepairability().getRepairable();
                                            long checkRepairabilityEndTimeMillis = System.currentTimeMillis();
                                            checkRepairabilityTimeMillis = checkRepairabilityEndTimeMillis - checkRepairabilityStartTimeMillis;

                                            // Write event log ENDED_CHECKING_REPAIRABILITY_OF_BIN for the current module
                                            BinEventLog endedCheckingRepairabilityOfBinEventLog = new BinEventLog(b.baseIndividuals(), BinEvent.ENDED_CHECKING_REPAIRABILITY_OF_BIN, LocalDateTime.now());
                                            logger.addBinEventLog(endedCheckingRepairabilityOfBinEventLog.toJsonString());
                                            // Print in stdout the ENDED_CHECKING_REPAIRABILITY_OF_BIN event
                                            System.out.println(endedCheckingRepairabilityOfBinEventLog.toJsonString());

                                            if (pRepairable) {
                                                System.out.println("\n++++++++++++++++ The module is Inconsistent but P-repairable! [ " + String.join(",", b.baseIndividuals()) + " ]");
                                            } else {
                                                System.out.println("\n++++++++++++++++ The module is Inconsistent and Not P-repairable! [ " + String.join(",", b.baseIndividuals()) + " ]");
                                                throw new RuntimeException("P-repairability check failed!");
                                            }
                                        }

                                        BinReasoningLog binReasoningLog = new BinReasoningLog(b.baseIndividuals(), outerRound, innerRound, checkConsistencyTimeMillis, checkRepairabilityTimeMillis, getExplanationsTimeMillis, computeFixesTimeMillis, explanationsCount);
                                        logger.addBinReasoningLog(binReasoningLog.toJsonString());
                                    }


                                    BinInnerRoundNumLog binInnerRoundNumLog = new BinInnerRoundNumLog(b.baseIndividuals(), outerRound, innerRound);
                                    logger.addBinInnerRoundNumLog(binInnerRoundNumLog.toJsonString());

                                    BinFixesLog binFixesLog = new BinFixesLog(b.baseIndividuals(), outerRound, innerRound, fixesOfBin);
                                    logger.addBinFixesLog(binFixesLog.toJsonString());
                                }
                            }
                        }
                    }
                });

                long explanationsNumber;
                // After processing all partitions, read all explanation logs, merge and deduplicate them, and write the unique explanations to hdfs
                if (hdfsIO.hasFiles(binExplanationsLogsDir)) {
                    // 1) Read raw NDJSON
                    JavaRDD<String> rawBin = sc.textFile(hdfsLocation + "/" + binExplanationsLogsDir);

                    // 2) Fast pre-filter to skip blanks / non-JSON
                    JavaRDD<String> candidatesBin = rawBin.filter(s -> {
                        if (s == null) return false;
                        String t = s.trim();
                        return !t.isEmpty() && t.charAt(0) == '{';
                    });

                    // 3) Merge/deduplicate (ensure mergeBinExplanations is safe on parsing, or it internally filters)
                    JavaRDD<String> mergedBinExplanationLines = mergeBinExplanations(candidatesBin)
                            .persist(org.apache.spark.storage.StorageLevel.MEMORY_AND_DISK());

                    // 4) Count uniques
                    explanationsNumber = mergedBinExplanationLines.count();

                    // 5) Write a single NDJSON file
                    String tmpSingleDirBin = hdfsLocation + "/" + outputDirectoryPath + "/merged_explanations_" + java.util.UUID.randomUUID();
                    mergedBinExplanationLines.coalesce(1, true).saveAsTextFile(tmpSingleDirBin);
                    hdfsIO.moveSinglePartFile(tmpSingleDirBin, mergedExplanationsFilePath);
                } else {
                    explanationsNumber = 0;
                }

                // Write the total number of unique explanations found to hdfs
                hdfsIO.writeStringToHDFS("Number of explanations: " + explanationsNumber, explanationsNumberFilePath);
                System.out.println("Number of explanations: " + explanationsNumber);

                // If there are fixes
                if (hdfsIO.hasFiles(binFixesLogsDir)) {
                    // After processing all partitions, read all explanation logs, merge and deduplicate them, and write the unique explanations to hdfs
                    // 1) Read raw NDJSON
                    JavaRDD<String> rawBinFixes = sc.textFile(hdfsLocation + "/" + binFixesLogsDir);

                    // 2) Fast pre-filter: skip blanks / non-JSON
                    JavaRDD<String> candidatesBinFixes = rawBinFixes.filter(s -> {
                        if (s == null) return false;
                        String t = s.trim();
                        return !t.isEmpty() && t.charAt(0) == '{';
                    });

                    // 3) Merge & deduplicate (ensure mergeBinFixes safely parses or pre-filters inside)
                    JavaRDD<String> mergedFixesLines = mergeBinFixes(candidatesBinFixes);
                    // If you also need a count, persist before counting & saving:
                    // .persist(org.apache.spark.storage.StorageLevel.MEMORY_AND_DISK);

                    // Optional:
                    // long binFixesNumber = mergedBinFixesLines.count();
                    // hdfsIO.writeStringToHDFS("Number of bin fixes: " + binFixesNumber, binFixesNumberFilePath);

                    // 4) Single-file output
                    String tmpSingleDirBinFixes = hdfsLocation + "/" + outputDirectoryPath + "/merged_fixes_" + java.util.UUID.randomUUID();
                    mergedFixesLines.coalesce(1, true).saveAsTextFile(tmpSingleDirBinFixes);
                    hdfsIO.moveSinglePartFile(tmpSingleDirBinFixes, mergedFixesFilePath);

                    PositionTracker globalPositionTracker;
                    if (outerRound > 1) {
                        globalPositionTracker = PositionTracker.fromJsonString(hdfsIO.readStringFromHDFS(inputPositionTrackerFilePath));
                    } else {
                        globalPositionTracker = new PositionTracker();
                    }
                    TBoxHandler tBoxHandler = new TBoxHandler(tBoxFilePath);
                    try (ABoxHandler aBoxHandler = new ABoxHandler(sparqlEndpoint, graphDomain, tBoxHandler, globalPositionTracker)) {
                        mergedFixesLines.foreach(line -> {
                            try {
                                FixesLog fixesLog = FixesLog.fromJsonString(line);
                                boolean applied = aBoxHandler.applyFixToTripleStore(fixesLog.getFixes().stream().findFirst().orElseThrow(() -> new Exception("No fixes found in FixesLog!")));
                                if (!applied) {
                                    throw new Exception("Fixes could not be applied to triple store! " + line);
                                }
                            } catch (Exception e) {
                                System.err.println("Error processing line: " + e.getMessage());
                            }
                        });
                    }

                    hdfsIO.writeStringToHDFS(globalPositionTracker.toJsonString(), outputPositionTrackerFilePath);
                }

                //  Get end time in millis
                long endTimeMillis = System.currentTimeMillis();
                // Get end date time
                LocalDateTime endDateTime = LocalDateTime.now();

                //  Calculate total time in millis
                long totalTimeMillis = endTimeMillis - startTimeMillis;

                // Write the start date time to hdfs
                hdfsIO.writeStringToHDFS("End date time by Spark: " + endDateTime, sparkTotalTimeFilePath);
                System.out.println("End date time by Spark: " + endDateTime);

                // Write the total time in millis to hdfs
                hdfsIO.writeStringToHDFS("Total time elapsed by Spark: " + totalTimeMillis + " ms", sparkTotalTimeFilePath);
                System.out.println("Total time elapsed by Spark: " + totalTimeMillis + " ms");

                if ((!hdfsIO.hasFiles(moduleFixesLogsDir)) || (fixSelection == 1) || (fixSelection == 4)) {
                    // Write stop flag to hdfs
                    hdfsIO.writeStringToHDFS("stop", stopLoopFlagFilePath);
                    System.out.println("Writing stop flag to hdfs.");
                }
            } else {
                throw new RuntimeException("Unimplemented mode.");
            }

//            sc.hadoopConfiguration().set("fs.defaultFS", "hdfs://localhost:9000");

            // Create SparkSession from existing JavaSparkContext
            SparkSession spark = SparkSession.builder()
                    .appName("KGFixer Analysis")
                    .config(sc.getConf())
                    .getOrCreate();

            String moduleSizeLogsViewName = "ModuleSizeLogs";
            String binSizeLogsViewName = "BinSizeLogs";
            String moduleReasoningLogsViewName = "ModuleReasoningLogs";
            String binReasoningLogsViewName = "BinReasoningLogs";
            String moduleInnerRoundNumLogsViewName = "ModuleInnerRoundNumLogs";
            String binInnerRoundNumLogsViewName = "BinInnerRoundNumLogs";

            Queries queries = new Queries(limitInTop, moduleSizeLogsViewName, binSizeLogsViewName, moduleReasoningLogsViewName, binReasoningLogsViewName, moduleInnerRoundNumLogsViewName, binInnerRoundNumLogsViewName);

            if (hdfsIOForSetup.hasFiles(moduleSizeLogsDir)) {
                Encoder<ModuleSizeLog> moduleSizeLogEncoder = Encoders.bean(ModuleSizeLog.class);
                Dataset<ModuleSizeLog> moduleSizeLogDataset = spark.read().json(hdfsLocation + "/" + moduleSizeLogsDir).as(moduleSizeLogEncoder);
                moduleSizeLogDataset.createOrReplaceTempView(moduleSizeLogsViewName);

                Dataset<Row> getTotalIndividualsQueryResult = spark.sql(queries.getTotalIndividualsQuery());
                getTotalIndividualsQueryResult.show();
                getTotalIndividualsQueryResult
                        .coalesce(1)
                        .write()
                        .mode("overwrite")
                        .option("header", "true")
                        .csv(hdfsLocation + "/" + analyticsDir + "/" + "totalIndividuals");

                Dataset<Row> getModuleSizeQueryResult = spark.sql(queries.getModuleSizeQuery());
                getModuleSizeQueryResult.show();
                getModuleSizeQueryResult
                        .coalesce(1)
                        .write()
                        .mode("overwrite")
                        .option("header", "true")
                        .csv(hdfsLocation + "/" + analyticsDir + "/" + "moduleSizeStats");

                Dataset<Row> getTopModulesQueryResult = spark.sql(queries.getTopModules());
                getTopModulesQueryResult.show(10);
                getTopModulesQueryResult
                    .coalesce(1)
                    .write()
                    .mode("overwrite")
                    .option("header", "true")
                    .csv(hdfsLocation + "/" + analyticsDir + "/" + "topIndividuals");
            }

            if (hdfsIOForSetup.hasFiles(binSizeLogsDir)) {
                Encoder<BinSizeLog> binSizeLogEncoder = Encoders.bean(BinSizeLog.class);
                Dataset<BinSizeLog> binSizeLogDataset = spark.read().json(hdfsLocation + "/" + binSizeLogsDir).as(binSizeLogEncoder);
                binSizeLogDataset.createOrReplaceTempView(binSizeLogsViewName);

                Dataset<Row> getTotalBinsQueryResult = spark.sql(queries.getTotalBinsQuery());
                getTotalBinsQueryResult.show();
                getTotalBinsQueryResult
                        .coalesce(1)
                        .write()
                        .mode("overwrite")
                        .option("header", "true")
                        .csv(hdfsLocation + "/" + analyticsDir + "/" + "totalBins");

                Dataset<Row> getBinSizeQueryResult = spark.sql(queries.getBinSizeQuery());
                getBinSizeQueryResult.show();
                getBinSizeQueryResult
                        .coalesce(1)
                        .write()
                        .mode("overwrite")
                        .option("header", "true")
                        .csv(hdfsLocation + "/" + analyticsDir + "/" + "binSizeStats");

                Dataset<Row> getTopBinsQueryResult = spark.sql(queries.getTopBins());
                getTopBinsQueryResult.show(10);
                getTopBinsQueryResult
                    .coalesce(1)
                    .write()
                    .mode("overwrite")
                    .option("header", "true")
                    .csv(hdfsLocation + "/" + analyticsDir + "/" + "topBins");
            }

            if (hdfsIOForSetup.hasFiles(moduleReasoningLogsDir)) {
                Encoder<ModuleReasoningLog> moduleReasoningLogEncoder = Encoders.bean(ModuleReasoningLog.class);
                Dataset<ModuleReasoningLog> moduleReasoningLogDataset = spark.read().json(hdfsLocation + "/" + moduleReasoningLogsDir).as(moduleReasoningLogEncoder);
                moduleReasoningLogDataset.createOrReplaceTempView(moduleReasoningLogsViewName);

                Dataset<Row> getCheckConsistencyTimeQueryResult = spark.sql(queries.getCheckConsistencyTimeQuery(ModuleReasoningLog.class));
                getCheckConsistencyTimeQueryResult.show();
                getCheckConsistencyTimeQueryResult
                        .coalesce(1)
                        .write()
                        .mode("overwrite")
                        .option("header", "true")
                        .csv(hdfsLocation + "/" + analyticsDir + "/" + "moduleCheckConsistencyTimeStats");

                Dataset<Row> getCheckRepairabilityTimeQueryResult = spark.sql(queries.getCheckRepairabilityTimeQuery(ModuleReasoningLog.class));
                getCheckRepairabilityTimeQueryResult.show();
                getCheckRepairabilityTimeQueryResult
                        .coalesce(1)
                        .write()
                        .mode("overwrite")
                        .option("header", "true")
                        .csv(hdfsLocation + "/" + analyticsDir + "/" + "moduleCheckRepairabilityTimeStats");

                Dataset<Row> getGetExplanationsTimeQueryResult = spark.sql(queries.getGetExplanationsTimeQuery(ModuleReasoningLog.class));
                getGetExplanationsTimeQueryResult.show();
                getGetExplanationsTimeQueryResult
                        .coalesce(1)
                        .write()
                        .mode("overwrite")
                        .option("header", "true")
                        .csv(hdfsLocation + "/" + analyticsDir + "/" + "moduleGetExplanationsTimeStats");

                Dataset<Row> getComputeFixesTimeQueryResult = spark.sql(queries.getComputeFixesTimeQuery(ModuleReasoningLog.class));
                getComputeFixesTimeQueryResult.show();
                getComputeFixesTimeQueryResult
                        .coalesce(1)
                        .write()
                        .mode("overwrite")
                        .option("header", "true")
                        .csv(hdfsLocation + "/" + analyticsDir + "/" + "moduleComputeFixesTimeStats");

                Dataset<Row> getExplanationsCountQueryResult = spark.sql(queries.getExplanationsCountQuery(ModuleReasoningLog.class));
                getExplanationsCountQueryResult.show();
                getExplanationsCountQueryResult
                        .coalesce(1)
                        .write()
                        .mode("overwrite")
                        .option("header", "true")
                        .csv(hdfsLocation + "/" + analyticsDir + "/" + "moduleExplanationsCountStats");
            }

            if (hdfsIOForSetup.hasFiles(binReasoningLogsDir))
            {
                Encoder<BinReasoningLog> binReasoningLogEncoder = Encoders.bean(BinReasoningLog.class);
                Dataset<BinReasoningLog> binReasoningLogDataset = spark.read().json(hdfsLocation + "/" + binReasoningLogsDir).as(binReasoningLogEncoder);
                binReasoningLogDataset.createOrReplaceTempView(binReasoningLogsViewName);

                Dataset<Row> getCheckConsistencyTimeQueryResult = spark.sql(queries.getCheckConsistencyTimeQuery(BinReasoningLog.class));
                getCheckConsistencyTimeQueryResult.show();
                getCheckConsistencyTimeQueryResult
                        .coalesce(1)
                        .write()
                        .mode("overwrite")
                        .option("header", "true")
                        .csv(hdfsLocation + "/" + analyticsDir + "/" + "binCheckConsistencyTimeStats");

                Dataset<Row> getCheckRepairabilityTimeQueryResult = spark.sql(queries.getCheckRepairabilityTimeQuery(BinReasoningLog.class));
                getCheckRepairabilityTimeQueryResult.show();
                getCheckRepairabilityTimeQueryResult
                        .coalesce(1)
                        .write()
                        .mode("overwrite")
                        .option("header", "true")
                        .csv(hdfsLocation + "/" + analyticsDir + "/" + "binCheckRepairabilityTimeStats");

                Dataset<Row> getGetExplanationsTimeQueryResult = spark.sql(queries.getGetExplanationsTimeQuery(BinReasoningLog.class));
                getGetExplanationsTimeQueryResult.show();
                getGetExplanationsTimeQueryResult
                        .coalesce(1)
                        .write()
                        .mode("overwrite")
                        .option("header", "true")
                        .csv(hdfsLocation + "/" + analyticsDir + "/" + "binGetExplanationsTimeStats");

                Dataset<Row> getComputeFixesTimeQueryResult = spark.sql(queries.getComputeFixesTimeQuery(BinReasoningLog.class));
                getComputeFixesTimeQueryResult.show();
                getComputeFixesTimeQueryResult
                        .coalesce(1)
                        .write()
                        .mode("overwrite")
                        .option("header", "true")
                        .csv(hdfsLocation + "/" + analyticsDir + "/" + "binComputeFixesTimeStats");

                Dataset<Row> getExplanationsCountQueryResult = spark.sql(queries.getExplanationsCountQuery(BinReasoningLog.class));
                getExplanationsCountQueryResult.show();
                getExplanationsCountQueryResult
                        .coalesce(1)
                        .write()
                        .mode("overwrite")
                        .option("header", "true")
                        .csv(hdfsLocation + "/" + analyticsDir + "/" + "binExplanationsCountStats");
            }

            if (hdfsIOForSetup.hasFiles(moduleInnerRoundNumLogsDir)) {
                Encoder<ModuleInnerRoundNumLog> moduleInnerRoundNumLogEncoder = Encoders.bean(ModuleInnerRoundNumLog.class);
                Dataset<ModuleInnerRoundNumLog> moduleInnerRoundNumLogDataset = spark.read().json(hdfsLocation + "/" + moduleInnerRoundNumLogsDir).as(moduleInnerRoundNumLogEncoder);
                moduleInnerRoundNumLogDataset.createOrReplaceTempView(moduleInnerRoundNumLogsViewName);

                Dataset<Row> getInnerRoundNumQueryResult = spark.sql(queries.getInnerRoundNumQuery(ModuleInnerRoundNumLog.class));
                getInnerRoundNumQueryResult.show();
                getInnerRoundNumQueryResult
                        .coalesce(1)
                        .write()
                        .mode("overwrite")
                        .option("header", "true")
                        .csv(hdfsLocation + "/" + analyticsDir + "/" + "moduleInnerRoundNumStats");
            }

            if (hdfsIOForSetup.hasFiles(binInnerRoundNumLogsDir)) {
                Encoder<BinInnerRoundNumLog> binInnerRoundNumLogEncoder = Encoders.bean(BinInnerRoundNumLog.class);
                Dataset<BinInnerRoundNumLog> binInnerRoundNumLogDataset = spark.read().json(hdfsLocation + "/" + binInnerRoundNumLogsDir).as(binInnerRoundNumLogEncoder);
                binInnerRoundNumLogDataset.createOrReplaceTempView(binInnerRoundNumLogsViewName);

                Dataset<Row> getInnerRoundNumQueryResult = spark.sql(queries.getInnerRoundNumQuery(BinInnerRoundNumLog.class));
                getInnerRoundNumQueryResult.show();
                getInnerRoundNumQueryResult
                        .coalesce(1)
                        .write()
                        .mode("overwrite")
                        .option("header", "true")
                        .csv(hdfsLocation + "/" + analyticsDir + "/" + "binInnerRoundNumStats");
            }
        }
        catch (Exception e) {
            System.err.println(e.getMessage());
            throw new RuntimeException(e);
        }
    }
}