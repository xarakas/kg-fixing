package org.nikolasparaskakis;



import com.beust.jcommander.JCommander;
import java.io.IOException;
import java.time.LocalDateTime;
import org.nikolasparaskakis.core.*;
import org.nikolasparaskakis.io.LocalFSIO;
import org.nikolasparaskakis.io.MyLogger;
import org.nikolasparaskakis.logs.module.*;
import org.semanticweb.owlapi.model.*;
import java.util.*;
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
        String tBoxFilePath = c_args.tBoxFilePath;
        String sparqlEndpoint = c_args.sparqlEndpoint;
        String graphDomain = c_args.graphDomain;
        String hdfsLocation = c_args.hdfsLocation;
        String inputDirectoryPath = c_args.inputDirectoryPath;
        String outputDirectoryPath = c_args.outputDirectoryPath + "=timestamp=" + System.currentTimeMillis();
        boolean saveInitialOntology = c_args.saveInitialOntology;
        boolean saveFixedOntology = c_args.saveFixedOntology;
        String logFilename = c_args.logFilename;
        int mode = c_args.mode;
        int reasonerSelection = c_args.reasonerSelection;
        int fixSelection = c_args.fixSelection;
        boolean mcd = c_args.mcd;
        long perOpTimeoutMillis = c_args.perOpTimeoutMillis;
        long reasonerTimeoutMillis = c_args.reasonerTimeoutMillis;
        long fixingTimeoutMillis = c_args.fixingTimeoutMillis;
        int explanationsLimit = c_args.explanationsLimit;
        int outerRound = c_args.outerRound;

        // Create an HDFSIO object for setup tasks
        LocalFSIO hdfsIOForSetup = new LocalFSIO(hdfsLocation);

        // Define input  file path
        String inputPositionTrackerFilePath = inputDirectoryPath + "/position-tracker.json";

        // Define output file paths
        String sparkTotalTimeFilePath = outputDirectoryPath + "/" + "spark-total-time.txt";
        String stopLoopFlagFilePath = outputDirectoryPath + "/" + "stop-loop-flag.txt";
        String explanationsNumberFilePath = outputDirectoryPath + "/" + "explanations-number.txt";
        String outputPositionTrackerFilePath = outputDirectoryPath + "/position-tracker.json";

        // Define output directories

        boolean successfullyCreated;
        boolean successfullyDeleted;

        // Create the directory for ModuleEventLogs
        String myEventLogsDir = outputDirectoryPath + "/myEventLogs";
        successfullyDeleted = hdfsIOForSetup.deleteDirectory(myEventLogsDir);
        successfullyCreated = hdfsIOForSetup.createDirectory(myEventLogsDir);

        if (!successfullyCreated) {
            System.err.println("Failed to create directory: " + myEventLogsDir);
            System.exit(1);
        }

        // Create the directory for ModuleExplanationsLogs
        String myExplanationsLogsDir = outputDirectoryPath + "/myExplanationsLogs";
        successfullyDeleted = hdfsIOForSetup.deleteDirectory(myExplanationsLogsDir);
        successfullyCreated = hdfsIOForSetup.createDirectory(myExplanationsLogsDir);

        if (!successfullyCreated) {
            System.err.println("Failed to create directory: " + myExplanationsLogsDir);
            System.exit(1);
        }

        // Create the directory for ModuleFixesLogs
        String myFixesLogsDir = outputDirectoryPath + "/myFixesLogs";
        successfullyDeleted = hdfsIOForSetup.deleteDirectory(myFixesLogsDir);
        successfullyCreated = hdfsIOForSetup.createDirectory(myFixesLogsDir);

        if (!successfullyCreated) {
            System.err.println("Failed to create directory: " + myFixesLogsDir);
            System.exit(1);
        }

        // Create the directory for ModuleInnerRoundNumLogs
        String myInnerRoundNumLogsDir = outputDirectoryPath + "/myInnerRoundNumLogs";
        successfullyDeleted = hdfsIOForSetup.deleteDirectory(myInnerRoundNumLogsDir);
        successfullyCreated = hdfsIOForSetup.createDirectory(myInnerRoundNumLogsDir);

        if (!successfullyCreated) {
            System.err.println("Failed to create directory: " + myInnerRoundNumLogsDir);
            System.exit(1);
        }

        // Create the directory for ModuleReasoningLogs
        String myReasoningLogsDir = outputDirectoryPath + "/myReasoningLogs";
        successfullyDeleted = hdfsIOForSetup.deleteDirectory(myReasoningLogsDir);
        successfullyCreated = hdfsIOForSetup.createDirectory(myReasoningLogsDir);

        if (!successfullyCreated) {
            System.err.println("Failed to create directory: " + myReasoningLogsDir);
            System.exit(1);
        }

        // Create the directory for ModuleSizeLogs
        String mySizeLogsDir = outputDirectoryPath + "/mySizeLogs";
        successfullyDeleted = hdfsIOForSetup.deleteDirectory(mySizeLogsDir);
        successfullyCreated = hdfsIOForSetup.createDirectory(mySizeLogsDir);

        if (!successfullyCreated) {
            System.err.println("Failed to create directory: " + mySizeLogsDir);
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

        if (mode == 1) {
            // --------------------------------
            // Mode 1 (Get explanations only)
            // --------------------------------

            // Create an HDFSIO object
            LocalFSIO localFSIO = new LocalFSIO(hdfsLocation);

            //  Get start time in millis
            long startTimeMillis = System.currentTimeMillis();
            // Get start date time
            LocalDateTime startDateTime = LocalDateTime.now();

            // Write the start time in millis
            localFSIO.writeStringToHDFS("Start date time: " + startDateTime, sparkTotalTimeFilePath);
            System.out.println("Start date time: " + startDateTime);

            String fileName = "partition";

            long explanationsNumber = 0;

            // Create TBoxHandler and ABoxHandler objects
            TBoxHandler tBoxHandler = new TBoxHandler(tBoxFilePath);
            try (ABoxHandler aBoxHandler = new ABoxHandler(sparqlEndpoint, graphDomain, tBoxHandler, new PositionTracker())) {

                try (MyLogger logger = new MyLogger(
                        localFSIO,
                        myEventLogsDir,
                        myExplanationsLogsDir,
                        myFixesLogsDir,
                        myInnerRoundNumLogsDir,
                        myReasoningLogsDir,
                        mySizeLogsDir,
                        fileName,
                        1000
                )) {
                    // Write event log STARTED_FETCHING_TRIPLES
                    MyEventLog startedFetchingTriplesEventLog = new MyEventLog(MyEvent.STARTED_FETCHING_TRIPLES, LocalDateTime.now());
                    logger.addMyEventLog(startedFetchingTriplesEventLog.toJsonString());
                    // Print in stdout the STARTED_FETCHING_TRIPLES event
                    System.out.println(startedFetchingTriplesEventLog.toJsonString());

                    // Create a set of the ABox axioms
                    Set<OWLAxiom> abox = new HashSet<>(aBoxHandler.getABox());

                    // Write event log ENDED_FETCHING_TRIPLES
                    MyEventLog endedFetchingTriplesOfEventLog = new MyEventLog(MyEvent.ENDED_FETCHING_TRIPLES, LocalDateTime.now());
                    logger.addMyEventLog(endedFetchingTriplesOfEventLog.toJsonString());
                    // Print in stdout the ENDED_FETCHING_TRIPLES event
                    System.out.println(endedFetchingTriplesOfEventLog.toJsonString());

                    // Create a set of the TBox axioms
                    Set<OWLAxiom> tBox = tBoxHandler.getOntologyTBox();

                    // Create a set of all axioms (ABox and TBox)
                    Set<OWLAxiom> all_boxes = new HashSet<>();
                    all_boxes.addAll(abox);
                    all_boxes.addAll(tBox);

                    // Write event log STARTED_COUNTING_TRIPLES
                    MyEventLog startedCountingTriplesEventLog = new MyEventLog(MyEvent.STARTED_COUNTING_TRIPLES, LocalDateTime.now());
                    logger.addMyEventLog(startedCountingTriplesEventLog.toJsonString());
                    // Print in stdout the STARTED_COUNTING_TRIPLES event
                    System.out.println(startedCountingTriplesEventLog.toJsonString());

                    // Get module size
                    int moduleSize = abox.size();

                    // Write event log ENDED_COUNTING_TRIPLES
                    MyEventLog endedCountingTriplesEventLog = new MyEventLog(MyEvent.ENDED_COUNTING_TRIPLES, LocalDateTime.now());
                    logger.addMyEventLog(endedCountingTriplesEventLog.toJsonString());
                    // Print in stdout the ENDED_COUNTING_TRIPLES event
                    System.out.println(endedCountingTriplesEventLog.toJsonString());

                    // Write module size log
                    MySizeLog moduleSizeLog = new MySizeLog(outerRound, moduleSize);
                    logger.addMySizeLog(moduleSizeLog.toJsonString());

                    // Create a ModuleHandler object that will host the whole ontology
                    try (MyHandler moduleHandler = new MyHandler(all_boxes, IRI.create("http://ontology.com"), reasonerSelection)) {

                        // Initialize variables for metrics logging
                        long checkConsistencyTimeMillis;
                        long getExplanationsTimeMillis = -1;
                        int explanationsCount = -1;

                        // Write event log STARTED_CHECKING_CONSISTENCY
                        MyEventLog startedCheckingConsistencyEventLog = new MyEventLog(MyEvent.STARTED_CHECKING_CONSISTENCY, LocalDateTime.now());
                        logger.addMyEventLog(startedCheckingConsistencyEventLog.toJsonString());
                        // Print in stdout the STARTED_CHECKING_CONSISTENCY event
                        System.out.println(startedCheckingConsistencyEventLog.toJsonString());

                        // Check consistency of the ontology and log the time needed for that
                        long checkConsistencyStartTimeMillis = System.currentTimeMillis();
                        ConsistencyCheckOutcome consistencyCheckOutcome = moduleHandler.isConsistent(reasonerTimeoutMillis);
                        long checkConsistencyEndTimeMillis = System.currentTimeMillis();
                        checkConsistencyTimeMillis = checkConsistencyEndTimeMillis - checkConsistencyStartTimeMillis;

                        // Write event log ENDED_CHECKING_CONSISTENCY
                        MyEventLog endedCheckingConsistencyEventLog = new MyEventLog(MyEvent.ENDED_CHECKING_CONSISTENCY, LocalDateTime.now());
                        logger.addMyEventLog(endedCheckingConsistencyEventLog.toJsonString());
                        // Print in stdout the ENDED_CHECKING_CONSISTENCY event
                        System.out.println(endedCheckingConsistencyEventLog.toJsonString());

                        if ((consistencyCheckOutcome.getStatus() != ConsistencyCheckOutcome.Status.CONSISTENT) && (consistencyCheckOutcome.getStatus() != ConsistencyCheckOutcome.Status.INCONSISTENT))
                        {
                            // Write event log TIMED_OUT_CHECKING_CONSISTENCY
                            MyEventLog timedOutCheckingConsistencyEventLog = new MyEventLog(MyEvent.TIMED_OUT_CHECKING_CONSISTENCY, LocalDateTime.now());
                            logger.addMyEventLog(timedOutCheckingConsistencyEventLog.toJsonString());
                            // Print in stdout the TIMED_OUT_CHECKING_CONSISTENCY event
                            System.out.println(timedOutCheckingConsistencyEventLog.toJsonString());
                        }
                        else {

                            boolean aBoxConsistent = consistencyCheckOutcome.getConsistent();

                            // If the ontology is inconsistent
                            if (!aBoxConsistent) {

                                // Write event log STARTED_GETTING_EXPLANATIONS
                                MyEventLog startedGettingExplanationsEventLog = new MyEventLog(MyEvent.STARTED_GETTING_EXPLANATIONS, LocalDateTime.now());
                                logger.addMyEventLog(startedGettingExplanationsEventLog.toJsonString());
                                // Print in stdout the STARTED_GETTING_EXPLANATIONS event
                                System.out.println(startedGettingExplanationsEventLog.toJsonString());

                                // Get inconsistency explanations and log the time needed for that
                                long getExplanationsStartTimeMillis = System.currentTimeMillis();
                                ExplanationOutcome explanationOutcome = moduleHandler.getExplanations(perOpTimeoutMillis, reasonerTimeoutMillis, explanationsLimit);
                                long getExplanationsEndTimeMillis = System.currentTimeMillis();
                                getExplanationsTimeMillis = getExplanationsEndTimeMillis - getExplanationsStartTimeMillis;

                                // Write event log ENDED_GETTING_EXPLANATIONS
                                MyEventLog endedGettingExplanationsEventLog = new MyEventLog(MyEvent.ENDED_GETTING_EXPLANATIONS, LocalDateTime.now());
                                logger.addMyEventLog(endedGettingExplanationsEventLog.toJsonString());
                                // Print in stdout the ENDED_GETTING_EXPLANATIONS event
                                System.out.println(endedGettingExplanationsEventLog.toJsonString());

                                if (explanationOutcome.getStatus() == ExplanationOutcome.Status.FAILED) {
                                    // Write event log TIMED_OUT_GETTING_EXPLANATIONS
                                    MyEventLog timedOutGettingExplanationsEventLog = new MyEventLog(MyEvent.TIMED_OUT_GETTING_EXPLANATIONS, LocalDateTime.now());
                                    logger.addMyEventLog(timedOutGettingExplanationsEventLog.toJsonString());
                                    // Print in stdout the TIMED_OUT_GETTING_EXPLANATIONS event
                                    System.out.println(timedOutGettingExplanationsEventLog.toJsonString());
                                }
                                else {
                                    // Sort the inconsistency explanations alphabetically
                                    List<List<OWLAxiom>> sortedExplanations = sortExplanations(explanationOutcome.getExplanations());

                                    explanationsCount = sortedExplanations.size();
                                    explanationsNumber = explanationsCount;
                                    // Make a string representation of the inconsistency explanations found
                                    System.out.println(moduleExplanationsString(sortedExplanations));

                                    // Write the inconsistency explanations found to hdfs
                                    MyExplanationsLog moduleExplanationsLog = new MyExplanationsLog(outerRound, 1, sortedExplanations);
                                    logger.addMyExplanationsLog(moduleExplanationsLog.toJsonString());
                                }
                            }

                            MyReasoningLog moduleReasoningLog = new MyReasoningLog(outerRound, 1, checkConsistencyTimeMillis, -1, getExplanationsTimeMillis, -1, explanationsCount);
                            logger.addMyReasoningLog(moduleReasoningLog.toJsonString());
                        }
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

            // Write the total number of unique explanations found to hdfs
            localFSIO.writeStringToHDFS("Number of explanations: " + explanationsNumber, explanationsNumberFilePath);
            System.out.println("Number of explanations: " + explanationsNumber);

            //  Get end time in millis
            long endTimeMillis = System.currentTimeMillis();
            // Get end date time
            LocalDateTime endDateTime = LocalDateTime.now();

            //  Calculate total time in millis
            long totalTimeMillis = endTimeMillis - startTimeMillis;

            // Write the start date time to hdfs
            localFSIO.writeStringToHDFS("End date time: " + endDateTime, sparkTotalTimeFilePath);
            System.out.println("End date time: " + endDateTime);

            // Write the total time in millis to hdfs
            localFSIO.writeStringToHDFS("Total time elapsed: " + totalTimeMillis + " ms", sparkTotalTimeFilePath);
            System.out.println("Total time elapsed: " + totalTimeMillis + " ms");

            // Write stop flag to hdfs
            localFSIO.writeStringToHDFS("stop", stopLoopFlagFilePath);
            System.out.println("Writing stop flag to hdfs.");
        } else if (mode == 2) {
            // ----------------------------------
            // Mode 2 (Get explanations and fix)
            // ----------------------------------

            // Create an HDFSIO object
            LocalFSIO localFSIO = new LocalFSIO(hdfsLocation);

            //  Get start time in millis
            long startTimeMillis = System.currentTimeMillis();
            // Get start date time
            LocalDateTime startDateTime = LocalDateTime.now();

            // Write the start time in millis
            localFSIO.writeStringToHDFS("Start date time: " + startDateTime, sparkTotalTimeFilePath);
            System.out.println("Start date time: " + startDateTime);

            String fileName = "partition";

            long explanationsNumber = 0;

            // Create a Random object
            Random random = new Random();

            // Read the global position tracker from hdfs if outerRound > 1 else create a new one
            PositionTracker globalPositionTracker;
            if (outerRound > 1) {
                globalPositionTracker = PositionTracker.fromJsonString(localFSIO.readStringFromHDFS(inputPositionTrackerFilePath));
            } else {
                globalPositionTracker = new PositionTracker();
            }

            // Create TBoxHandler and ABoxHandler objects
            TBoxHandler tBoxHandler = new TBoxHandler(tBoxFilePath);
            try (ABoxHandler aBoxHandler = new ABoxHandler(sparqlEndpoint, graphDomain, tBoxHandler, new PositionTracker())) {

                try (MyLogger logger = new MyLogger(
                        localFSIO,
                        myEventLogsDir,
                        myExplanationsLogsDir,
                        myFixesLogsDir,
                        myInnerRoundNumLogsDir,
                        myReasoningLogsDir,
                        mySizeLogsDir,
                        fileName,
                        1000
                )) {
                    // Write event log STARTED_FETCHING_TRIPLES
                    MyEventLog startedFetchingTriplesEventLog = new MyEventLog(MyEvent.STARTED_FETCHING_TRIPLES, LocalDateTime.now());
                    logger.addMyEventLog(startedFetchingTriplesEventLog.toJsonString());
                    // Print in stdout the STARTED_FETCHING_TRIPLES event
                    System.out.println(startedFetchingTriplesEventLog.toJsonString());

                    // Create a set of the ABox axioms
                    Set<OWLAxiom> abox = new HashSet<>(aBoxHandler.getABox());

                    // Write event log ENDED_FETCHING_TRIPLES
                    MyEventLog endedFetchingTriplesOfEventLog = new MyEventLog(MyEvent.ENDED_FETCHING_TRIPLES, LocalDateTime.now());
                    logger.addMyEventLog(endedFetchingTriplesOfEventLog.toJsonString());
                    // Print in stdout the ENDED_FETCHING_TRIPLES event
                    System.out.println(endedFetchingTriplesOfEventLog.toJsonString());

                    // Create a set of the TBox axioms
                    Set<OWLAxiom> tBox = tBoxHandler.getOntologyTBox();

                    // Create a set of all axioms (ABox and TBox)
                    Set<OWLAxiom> all_boxes = new HashSet<>();
                    all_boxes.addAll(abox);
                    all_boxes.addAll(tBox);

                    // Write event log STARTED_COUNTING_TRIPLES
                    MyEventLog startedCountingTriplesEventLog = new MyEventLog(MyEvent.STARTED_COUNTING_TRIPLES, LocalDateTime.now());
                    logger.addMyEventLog(startedCountingTriplesEventLog.toJsonString());
                    // Print in stdout the STARTED_COUNTING_TRIPLES event
                    System.out.println(startedCountingTriplesEventLog.toJsonString());

                    // Get module size
                    int moduleSize = abox.size();

                    // Write event log ENDED_COUNTING_TRIPLES
                    MyEventLog endedCountingTriplesEventLog = new MyEventLog(MyEvent.ENDED_COUNTING_TRIPLES, LocalDateTime.now());
                    logger.addMyEventLog(endedCountingTriplesEventLog.toJsonString());
                    // Print in stdout the ENDED_COUNTING_TRIPLES event
                    System.out.println(endedCountingTriplesEventLog.toJsonString());

                    // Write module size log
                    MySizeLog moduleSizeLog = new MySizeLog(outerRound, moduleSize);
                    logger.addMySizeLog(moduleSizeLog.toJsonString());

                    // Create a ModuleHandler object that will host the whole ontology
                    try (MyHandler moduleHandler = new MyHandler(all_boxes, IRI.generateDocumentIRI(), reasonerSelection)) {

                        // Initialize variables for metrics logging
                        long checkConsistencyTimeMillis;
                        long checkRepairabilityTimeMillis = -1;
                        long getExplanationsTimeMillis = -1;
                        long computeFixesTimeMillis = -1;
                        int explanationsCount = -1;

                        Fixer fixer = new Fixer(moduleHandler.getOntology(), new PositionTracker(globalPositionTracker), fixSelection, fixingTimeoutMillis, reasonerSelection, reasonerTimeoutMillis, perOpTimeoutMillis, explanationsLimit, mcd);

                        // Write event log STARTED_CHECKING_CONSISTENCY
                        MyEventLog startedInitialCheckingConsistencyEventLog = new MyEventLog(MyEvent.STARTED_CHECKING_CONSISTENCY, LocalDateTime.now());
                        logger.addMyEventLog(startedInitialCheckingConsistencyEventLog.toJsonString());
                        // Print in stdout the STARTED_CHECKING_CONSISTENCY event
                        System.out.println(startedInitialCheckingConsistencyEventLog.toJsonString());

                        // Check consistency of the ontology and log the time needed for that
                        long checkConsistencyStartTimeMillis = System.currentTimeMillis();
                        ConsistencyCheckOutcome consistencyCheckOutcome = moduleHandler.isConsistent(reasonerTimeoutMillis);
                        long checkConsistencyEndTimeMillis = System.currentTimeMillis();
                        checkConsistencyTimeMillis = checkConsistencyEndTimeMillis - checkConsistencyStartTimeMillis;

                        // Write event log ENDED_CHECKING_CONSISTENCY
                        MyEventLog endedInitialCheckingConsistencyEventLog = new MyEventLog(MyEvent.ENDED_CHECKING_CONSISTENCY, LocalDateTime.now());
                        logger.addMyEventLog(endedInitialCheckingConsistencyEventLog.toJsonString());
                        // Print in stdout the ENDED_CHECKING_CONSISTENCY event
                        System.out.println(endedInitialCheckingConsistencyEventLog.toJsonString());

                        if ((consistencyCheckOutcome.getStatus() != ConsistencyCheckOutcome.Status.CONSISTENT) && (consistencyCheckOutcome.getStatus() != ConsistencyCheckOutcome.Status.INCONSISTENT))
                        {
                            // Write event log TIMED_OUT_CHECKING_CONSISTENCY
                            MyEventLog timedOutCheckingConsistencyEventLog = new MyEventLog(MyEvent.TIMED_OUT_CHECKING_CONSISTENCY, LocalDateTime.now());
                            logger.addMyEventLog(timedOutCheckingConsistencyEventLog.toJsonString());
                            // Print in stdout the TIMED_OUT_CHECKING_CONSISTENCY event
                            System.out.println(timedOutCheckingConsistencyEventLog.toJsonString());
                        }
                        else {

                            boolean aBoxConsistent = consistencyCheckOutcome.getConsistent();

                            boolean pRepairable = true;
                            if (!aBoxConsistent) {
                                // Write event log STARTED_CHECKING_REPAIRABILITY
                                MyEventLog startedInitialCheckingRepairabilityEventLog = new MyEventLog(MyEvent.STARTED_CHECKING_REPAIRABILITY, LocalDateTime.now());
                                logger.addMyEventLog(startedInitialCheckingRepairabilityEventLog.toJsonString());
                                // Print in stdout the STARTED_CHECKING_REPAIRABILITY event
                                System.out.println(startedInitialCheckingRepairabilityEventLog.toJsonString());

                                long checkRepairabilityStartTimeMillis = System.currentTimeMillis();
                                pRepairable = fixer.CheckRepairability().getRepairable();
                                long checkRepairabilityEndTimeMillis = System.currentTimeMillis();
                                checkRepairabilityTimeMillis = checkRepairabilityEndTimeMillis - checkRepairabilityStartTimeMillis;

                                // Write event log ENDED_CHECKING_REPAIRABILITY
                                MyEventLog endedInitialCheckingRepairabilityEventLog = new MyEventLog(MyEvent.ENDED_CHECKING_REPAIRABILITY, LocalDateTime.now());
                                logger.addMyEventLog(endedInitialCheckingRepairabilityEventLog.toJsonString());
                                // Print in stdout the ENDED_CHECKING_REPAIRABILITY event
                                System.out.println(endedInitialCheckingRepairabilityEventLog.toJsonString());
                            }

                            // Initialize a structure to keep the ontology's fixes
                            // (in case it is initially inconsistent)
                            Set<ArrayList<Fix>> fixesOfOntology = new HashSet<>();

                            int innerRound = 0;

                            MyReasoningLog initialModuleReasoningLog = new MyReasoningLog(outerRound, 0, checkConsistencyTimeMillis, checkRepairabilityTimeMillis, getExplanationsTimeMillis, computeFixesTimeMillis, explanationsCount);
                            logger.addMyReasoningLog(initialModuleReasoningLog.toJsonString());

                            // While the module is inconsistent and p-repairable, keep trying to fix it
                            while (!aBoxConsistent && pRepairable) {

                                checkRepairabilityTimeMillis = -1;

                                innerRound++;

                                // Write event log STARTED_GETTING_EXPLANATIONS
                                MyEventLog startedGettingExplanationsEventLog = new MyEventLog(MyEvent.STARTED_GETTING_EXPLANATIONS, LocalDateTime.now());
                                logger.addMyEventLog(startedGettingExplanationsEventLog.toJsonString());
                                // Print in stdout the STARTED_GETTING_EXPLANATIONS event
                                System.out.println(startedGettingExplanationsEventLog.toJsonString());

                                long getExplanationsStartTimeMillis = System.currentTimeMillis();
                                ExplanationOutcome explanationOutcome = moduleHandler.getExplanations(perOpTimeoutMillis, reasonerTimeoutMillis, explanationsLimit);
                                long getExplanationsEndTimeMillis = System.currentTimeMillis();
                                getExplanationsTimeMillis = getExplanationsEndTimeMillis - getExplanationsStartTimeMillis;

                                // Write event log ENDED_GETTING_EXPLANATIONS
                                MyEventLog endedGettingExplanationsEventLog = new MyEventLog(MyEvent.ENDED_GETTING_EXPLANATIONS, LocalDateTime.now());
                                logger.addMyEventLog(endedGettingExplanationsEventLog.toJsonString());
                                // Print in stdout the ENDED_GETTING_EXPLANATIONS event
                                System.out.println(endedGettingExplanationsEventLog.toJsonString());

                                if (explanationOutcome.getStatus() == ExplanationOutcome.Status.FAILED) {
                                    // Write event log TIMED_OUT_GETTING_EXPLANATIONS
                                    MyEventLog timedOutGettingExplanationsOfEventLog = new MyEventLog(MyEvent.TIMED_OUT_GETTING_EXPLANATIONS, LocalDateTime.now());
                                    logger.addMyEventLog(timedOutGettingExplanationsOfEventLog.toJsonString());
                                    // Print in stdout the TIMED_OUT_GETTING_EXPLANATIONS event
                                    System.out.println(timedOutGettingExplanationsOfEventLog.toJsonString());
                                    break;
                                }

                                List<List<OWLAxiom>> sortedExplanations = sortExplanations(explanationOutcome.getExplanations());

                                explanationsCount = sortedExplanations.size();

                                System.out.println(moduleExplanationsString(sortedExplanations));

                                // Write the inconsistency explanations found
                                MyExplanationsLog moduleExplanationsLog = new MyExplanationsLog(outerRound, innerRound, sortedExplanations);
                                logger.addMyExplanationsLog(moduleExplanationsLog.toJsonString());

                                // Write event log STARTED_COMPUTING_FIXES
                                MyEventLog startedComputingFixesEventLog = new MyEventLog(MyEvent.STARTED_COMPUTING_FIXES, LocalDateTime.now());
                                logger.addMyEventLog(startedComputingFixesEventLog.toJsonString());
                                // Print in stdout the STARTED_COMPUTING_FIXES event
                                System.out.println(startedComputingFixesEventLog.toJsonString());

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

                                // Write event log ENDED_COMPUTING_FIXES
                                MyEventLog endedComputingFixesEventLog = new MyEventLog(MyEvent.ENDED_COMPUTING_FIXES, LocalDateTime.now());
                                logger.addMyEventLog(endedComputingFixesEventLog.toJsonString());
                                // Print in stdout the ENDED_COMPUTING_FIXES event
                                System.out.println(endedComputingFixesEventLog.toJsonString());

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
                                            updateFixesOfOntology(chosenFix, fixesOfOntology);
                                        }
                                    } else {
                                        for (Fix fix : sFixes) {
                                            fixer.applyFix(fix);
                                            System.out.println("Fix " + fix + " applied in module.");
                                            updateFixesOfOntology(fix, fixesOfOntology);
                                        }
                                    }
                                } else {
                                    System.out.println("\n******* No Sound fixes available!");
                                }

                                // Write event log STARTED_CHECKING_CONSISTENCY
                                MyEventLog startedCheckingConsistencyEventLog = new MyEventLog(MyEvent.STARTED_CHECKING_CONSISTENCY, LocalDateTime.now());
                                logger.addMyEventLog(startedCheckingConsistencyEventLog.toJsonString());
                                // Print in stdout the STARTED_CHECKING_CONSISTENCY event
                                System.out.println(startedCheckingConsistencyEventLog.toJsonString());

                                checkConsistencyStartTimeMillis = System.currentTimeMillis();
                                consistencyCheckOutcome = fixer.CheckConsistency(reasonerTimeoutMillis);
                                checkConsistencyEndTimeMillis = System.currentTimeMillis();
                                checkConsistencyTimeMillis = checkConsistencyEndTimeMillis - checkConsistencyStartTimeMillis;

                                // Write event log ENDED_CHECKING_CONSISTENCY
                                MyEventLog endedCheckingConsistencyEventLog = new MyEventLog(MyEvent.ENDED_CHECKING_CONSISTENCY, LocalDateTime.now());
                                logger.addMyEventLog(endedCheckingConsistencyEventLog.toJsonString());
                                // Print in stdout the ENDED_CHECKING_CONSISTENCY event
                                System.out.println(endedCheckingConsistencyEventLog.toJsonString());

                                if ((consistencyCheckOutcome.getStatus() != ConsistencyCheckOutcome.Status.CONSISTENT) && (consistencyCheckOutcome.getStatus() != ConsistencyCheckOutcome.Status.INCONSISTENT))
                                {
                                    // Write event log TIMED_OUT_CHECKING_CONSISTENCY
                                    MyEventLog timedOutCheckingConsistencyEventLog = new MyEventLog(MyEvent.TIMED_OUT_CHECKING_CONSISTENCY, LocalDateTime.now());
                                    logger.addMyEventLog(timedOutCheckingConsistencyEventLog.toJsonString());
                                    // Print in stdout the TIMED_OUT_CHECKING_CONSISTENCY event
                                    System.out.println(timedOutCheckingConsistencyEventLog.toJsonString());
                                    break;
                                }

                                aBoxConsistent = consistencyCheckOutcome.getConsistent();

                                if (aBoxConsistent) {
                                    System.out.println("\n++++++++++++++++ The ontology is Consistent!");
                                } else {
                                    // Write event log STARTED_CHECKING_REPAIRABILITY
                                    MyEventLog startedCheckingRepairabilityEventLog = new MyEventLog(MyEvent.STARTED_CHECKING_REPAIRABILITY, LocalDateTime.now());
                                    logger.addMyEventLog(startedCheckingRepairabilityEventLog.toJsonString());
                                    // Print in stdout the STARTED_CHECKING_REPAIRABILITY event
                                    System.out.println(startedCheckingRepairabilityEventLog.toJsonString());

                                    long checkRepairabilityStartTimeMillis = System.currentTimeMillis();
                                    pRepairable = fixer.CheckRepairability().getRepairable();
                                    long checkRepairabilityEndTimeMillis = System.currentTimeMillis();
                                    checkRepairabilityTimeMillis = checkRepairabilityEndTimeMillis - checkRepairabilityStartTimeMillis;

                                    // Write event log ENDED_CHECKING_REPAIRABILITY
                                    MyEventLog endedCheckingRepairabilityEventLog = new MyEventLog(MyEvent.ENDED_CHECKING_REPAIRABILITY, LocalDateTime.now());
                                    logger.addMyEventLog(endedCheckingRepairabilityEventLog.toJsonString());
                                    // Print in stdout the ENDED_CHECKING_REPAIRABILITY event
                                    System.out.println(endedCheckingRepairabilityEventLog.toJsonString());

                                    if (pRepairable) {
                                        System.out.println("\n++++++++++++++++ The ontology is Inconsistent but P-repairable!");
                                    } else {
                                        System.out.println("\n++++++++++++++++ The ontology is Inconsistent and Not P-repairable!");
                                        throw new RuntimeException("P-repairability check failed!");
                                    }
                                }

                                MyReasoningLog myReasoningLog = new MyReasoningLog(outerRound, innerRound, checkConsistencyTimeMillis, checkRepairabilityTimeMillis, getExplanationsTimeMillis, computeFixesTimeMillis, explanationsCount);
                                logger.addMyReasoningLog(myReasoningLog.toJsonString());
                            }

                            MyInnerRoundNumLog myInnerRoundNumLog = new MyInnerRoundNumLog(outerRound, innerRound);
                            logger.addMyInnerRoundNumLog(myInnerRoundNumLog.toJsonString());

                            MyFixesLog myFixesLog = new MyFixesLog(outerRound, innerRound, fixesOfOntology);
                            logger.addMyFixesLog(myFixesLog.toJsonString());
                        }
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

            // Write the total number of unique explanations found to hdfs
            localFSIO.writeStringToHDFS("Number of explanations: " + explanationsNumber, explanationsNumberFilePath);
            System.out.println("Number of explanations: " + explanationsNumber);

//            PositionTracker newGlobalPositionTracker;
//            if (outerRound > 1) {
//                newGlobalPositionTracker = PositionTracker.fromJsonString(localFSIO.readStringFromHDFS(inputPositionTrackerFilePath));
//            } else {
//                newGlobalPositionTracker = new PositionTracker();
//            }
//            TBoxHandler newTBoxHandler = new TBoxHandler(tBoxFilePath);
//            try (ABoxHandler aBoxHandler = new ABoxHandler(sparqlEndpoint, graphDomain, tBoxHandler, globalPositionTracker)) {
//                mergedFixesLines.foreach(line -> {
//                    try {
//                        FixesLog fixesLog = FixesLog.fromJsonString(line);
//                        boolean applied = aBoxHandler.applyFixToTripleStore(fixesLog.getFixes().stream().findFirst().orElseThrow(() -> new Exception("No fixes found in FixesLog!")));
//                        if (!applied) {
//                            throw new Exception("Fixes could not be applied to triple store! " + line);
//                        }
//                    } catch (Exception e) {
//                        System.err.println("Error processing line: " + e.getMessage());
//                    }
//                });
//            }

            localFSIO.writeStringToHDFS(globalPositionTracker.toJsonString(), outputPositionTrackerFilePath);

            //  Get end time in millis
            long endTimeMillis = System.currentTimeMillis();
            // Get end date time
            LocalDateTime endDateTime = LocalDateTime.now();

            //  Calculate total time in millis
            long totalTimeMillis = endTimeMillis - startTimeMillis;

            // Write the start date time to hdfs
            localFSIO.writeStringToHDFS("End date time: " + endDateTime, sparkTotalTimeFilePath);
            System.out.println("End date time: " + endDateTime);

            // Write the total time in millis to hdfs
            localFSIO.writeStringToHDFS("Total time elapsed: " + totalTimeMillis + " ms", sparkTotalTimeFilePath);
            System.out.println("Total time elapsed: " + totalTimeMillis + " ms");

            // Write stop flag to hdfs
            localFSIO.writeStringToHDFS("stop", stopLoopFlagFilePath);
            System.out.println("Writing stop flag to hdfs.");
        } else {
            throw new RuntimeException("Unimplemented mode.");
        }

//            sc.hadoopConfiguration().set("fs.defaultFS", "hdfs://localhost:9000");

        // Create SparkSession from existing JavaSparkContext
//        SparkSession spark = SparkSession.builder()
//                .appName("KGFixer Analysis")
//                .config(sc.getConf())
//                .getOrCreate();
//
//        String moduleSizeLogsViewName = "ModuleSizeLogs";
//        String binSizeLogsViewName = "BinSizeLogs";
//        String moduleReasoningLogsViewName = "ModuleReasoningLogs";
//        String binReasoningLogsViewName = "BinReasoningLogs";
//        String moduleInnerRoundNumLogsViewName = "ModuleInnerRoundNumLogs";
//        String binInnerRoundNumLogsViewName = "BinInnerRoundNumLogs";
//
//        Queries queries = new Queries(limitInTop, moduleSizeLogsViewName, binSizeLogsViewName, moduleReasoningLogsViewName, binReasoningLogsViewName, moduleInnerRoundNumLogsViewName, binInnerRoundNumLogsViewName);
//
//        if (hdfsIOForSetup.hasFiles(moduleSizeLogsDir)) {
//            Encoder<MySizeLog> moduleSizeLogEncoder = Encoders.bean(MySizeLog.class);
//            Dataset<MySizeLog> moduleSizeLogDataset = spark.read().json(hdfsLocation + "/" + moduleSizeLogsDir).as(moduleSizeLogEncoder);
//            moduleSizeLogDataset.createOrReplaceTempView(moduleSizeLogsViewName);
//
//            Dataset<Row> getTotalIndividualsQueryResult = spark.sql(queries.getTotalIndividualsQuery());
//            getTotalIndividualsQueryResult.show();
//            getTotalIndividualsQueryResult
//                    .coalesce(1)
//                    .write()
//                    .mode("overwrite")
//                    .option("header", "true")
//                    .csv(hdfsLocation + "/" + analyticsDir + "/" + "totalIndividuals");
//
//            Dataset<Row> getModuleSizeQueryResult = spark.sql(queries.getModuleSizeQuery());
//            getModuleSizeQueryResult.show();
//            getModuleSizeQueryResult
//                    .coalesce(1)
//                    .write()
//                    .mode("overwrite")
//                    .option("header", "true")
//                    .csv(hdfsLocation + "/" + analyticsDir + "/" + "moduleSizeStats");
//
//            Dataset<Row> getTopModulesQueryResult = spark.sql(queries.getTopModules());
//            getTopModulesQueryResult.show(10);
//            getTopModulesQueryResult
//                .coalesce(1)
//                .write()
//                .mode("overwrite")
//                .option("header", "true")
//                .csv(hdfsLocation + "/" + analyticsDir + "/" + "topIndividuals");
//        }
//
//        if (hdfsIOForSetup.hasFiles(binSizeLogsDir)) {
//            Encoder<BinSizeLog> binSizeLogEncoder = Encoders.bean(BinSizeLog.class);
//            Dataset<BinSizeLog> binSizeLogDataset = spark.read().json(hdfsLocation + "/" + binSizeLogsDir).as(binSizeLogEncoder);
//            binSizeLogDataset.createOrReplaceTempView(binSizeLogsViewName);
//
//            Dataset<Row> getTotalBinsQueryResult = spark.sql(queries.getTotalBinsQuery());
//            getTotalBinsQueryResult.show();
//            getTotalBinsQueryResult
//                    .coalesce(1)
//                    .write()
//                    .mode("overwrite")
//                    .option("header", "true")
//                    .csv(hdfsLocation + "/" + analyticsDir + "/" + "totalBins");
//
//            Dataset<Row> getBinSizeQueryResult = spark.sql(queries.getBinSizeQuery());
//            getBinSizeQueryResult.show();
//            getBinSizeQueryResult
//                    .coalesce(1)
//                    .write()
//                    .mode("overwrite")
//                    .option("header", "true")
//                    .csv(hdfsLocation + "/" + analyticsDir + "/" + "binSizeStats");
//
//            Dataset<Row> getTopBinsQueryResult = spark.sql(queries.getTopBins());
//            getTopBinsQueryResult.show(10);
//            getTopBinsQueryResult
//                .coalesce(1)
//                .write()
//                .mode("overwrite")
//                .option("header", "true")
//                .csv(hdfsLocation + "/" + analyticsDir + "/" + "topBins");
//        }
//
//        if (hdfsIOForSetup.hasFiles(moduleReasoningLogsDir)) {
//            Encoder<ModuleReasoningLog> moduleReasoningLogEncoder = Encoders.bean(ModuleReasoningLog.class);
//            Dataset<ModuleReasoningLog> moduleReasoningLogDataset = spark.read().json(hdfsLocation + "/" + moduleReasoningLogsDir).as(moduleReasoningLogEncoder);
//            moduleReasoningLogDataset.createOrReplaceTempView(moduleReasoningLogsViewName);
//
//            Dataset<Row> getCheckConsistencyTimeQueryResult = spark.sql(queries.getCheckConsistencyTimeQuery(ModuleReasoningLog.class));
//            getCheckConsistencyTimeQueryResult.show();
//            getCheckConsistencyTimeQueryResult
//                    .coalesce(1)
//                    .write()
//                    .mode("overwrite")
//                    .option("header", "true")
//                    .csv(hdfsLocation + "/" + analyticsDir + "/" + "moduleCheckConsistencyTimeStats");
//
//            Dataset<Row> getCheckRepairabilityTimeQueryResult = spark.sql(queries.getCheckRepairabilityTimeQuery(ModuleReasoningLog.class));
//            getCheckRepairabilityTimeQueryResult.show();
//            getCheckRepairabilityTimeQueryResult
//                    .coalesce(1)
//                    .write()
//                    .mode("overwrite")
//                    .option("header", "true")
//                    .csv(hdfsLocation + "/" + analyticsDir + "/" + "moduleCheckRepairabilityTimeStats");
//
//            Dataset<Row> getGetExplanationsTimeQueryResult = spark.sql(queries.getGetExplanationsTimeQuery(ModuleReasoningLog.class));
//            getGetExplanationsTimeQueryResult.show();
//            getGetExplanationsTimeQueryResult
//                    .coalesce(1)
//                    .write()
//                    .mode("overwrite")
//                    .option("header", "true")
//                    .csv(hdfsLocation + "/" + analyticsDir + "/" + "moduleGetExplanationsTimeStats");
//
//            Dataset<Row> getComputeFixesTimeQueryResult = spark.sql(queries.getComputeFixesTimeQuery(ModuleReasoningLog.class));
//            getComputeFixesTimeQueryResult.show();
//            getComputeFixesTimeQueryResult
//                    .coalesce(1)
//                    .write()
//                    .mode("overwrite")
//                    .option("header", "true")
//                    .csv(hdfsLocation + "/" + analyticsDir + "/" + "moduleComputeFixesTimeStats");
//
//            Dataset<Row> getExplanationsCountQueryResult = spark.sql(queries.getExplanationsCountQuery(ModuleReasoningLog.class));
//            getExplanationsCountQueryResult.show();
//            getExplanationsCountQueryResult
//                    .coalesce(1)
//                    .write()
//                    .mode("overwrite")
//                    .option("header", "true")
//                    .csv(hdfsLocation + "/" + analyticsDir + "/" + "moduleExplanationsCountStats");
//        }
//
//        if (hdfsIOForSetup.hasFiles(binReasoningLogsDir))
//        {
//            Encoder<BinReasoningLog> binReasoningLogEncoder = Encoders.bean(BinReasoningLog.class);
//            Dataset<BinReasoningLog> binReasoningLogDataset = spark.read().json(hdfsLocation + "/" + binReasoningLogsDir).as(binReasoningLogEncoder);
//            binReasoningLogDataset.createOrReplaceTempView(binReasoningLogsViewName);
//
//            Dataset<Row> getCheckConsistencyTimeQueryResult = spark.sql(queries.getCheckConsistencyTimeQuery(BinReasoningLog.class));
//            getCheckConsistencyTimeQueryResult.show();
//            getCheckConsistencyTimeQueryResult
//                    .coalesce(1)
//                    .write()
//                    .mode("overwrite")
//                    .option("header", "true")
//                    .csv(hdfsLocation + "/" + analyticsDir + "/" + "binCheckConsistencyTimeStats");
//
//            Dataset<Row> getCheckRepairabilityTimeQueryResult = spark.sql(queries.getCheckRepairabilityTimeQuery(BinReasoningLog.class));
//            getCheckRepairabilityTimeQueryResult.show();
//            getCheckRepairabilityTimeQueryResult
//                    .coalesce(1)
//                    .write()
//                    .mode("overwrite")
//                    .option("header", "true")
//                    .csv(hdfsLocation + "/" + analyticsDir + "/" + "binCheckRepairabilityTimeStats");
//
//            Dataset<Row> getGetExplanationsTimeQueryResult = spark.sql(queries.getGetExplanationsTimeQuery(BinReasoningLog.class));
//            getGetExplanationsTimeQueryResult.show();
//            getGetExplanationsTimeQueryResult
//                    .coalesce(1)
//                    .write()
//                    .mode("overwrite")
//                    .option("header", "true")
//                    .csv(hdfsLocation + "/" + analyticsDir + "/" + "binGetExplanationsTimeStats");
//
//            Dataset<Row> getComputeFixesTimeQueryResult = spark.sql(queries.getComputeFixesTimeQuery(BinReasoningLog.class));
//            getComputeFixesTimeQueryResult.show();
//            getComputeFixesTimeQueryResult
//                    .coalesce(1)
//                    .write()
//                    .mode("overwrite")
//                    .option("header", "true")
//                    .csv(hdfsLocation + "/" + analyticsDir + "/" + "binComputeFixesTimeStats");
//
//            Dataset<Row> getExplanationsCountQueryResult = spark.sql(queries.getExplanationsCountQuery(BinReasoningLog.class));
//            getExplanationsCountQueryResult.show();
//            getExplanationsCountQueryResult
//                    .coalesce(1)
//                    .write()
//                    .mode("overwrite")
//                    .option("header", "true")
//                    .csv(hdfsLocation + "/" + analyticsDir + "/" + "binExplanationsCountStats");
//        }
//
//        if (hdfsIOForSetup.hasFiles(moduleInnerRoundNumLogsDir)) {
//            Encoder<MyInnerRoundNumLog> moduleInnerRoundNumLogEncoder = Encoders.bean(MyInnerRoundNumLog.class);
//            Dataset<MyInnerRoundNumLog> moduleInnerRoundNumLogDataset = spark.read().json(hdfsLocation + "/" + moduleInnerRoundNumLogsDir).as(moduleInnerRoundNumLogEncoder);
//            moduleInnerRoundNumLogDataset.createOrReplaceTempView(moduleInnerRoundNumLogsViewName);
//
//            Dataset<Row> getInnerRoundNumQueryResult = spark.sql(queries.getInnerRoundNumQuery(MyInnerRoundNumLog.class));
//            getInnerRoundNumQueryResult.show();
//            getInnerRoundNumQueryResult
//                    .coalesce(1)
//                    .write()
//                    .mode("overwrite")
//                    .option("header", "true")
//                    .csv(hdfsLocation + "/" + analyticsDir + "/" + "moduleInnerRoundNumStats");
//        }
//
//        if (hdfsIOForSetup.hasFiles(binInnerRoundNumLogsDir)) {
//            Encoder<BinInnerRoundNumLog> binInnerRoundNumLogEncoder = Encoders.bean(BinInnerRoundNumLog.class);
//            Dataset<BinInnerRoundNumLog> binInnerRoundNumLogDataset = spark.read().json(hdfsLocation + "/" + binInnerRoundNumLogsDir).as(binInnerRoundNumLogEncoder);
//            binInnerRoundNumLogDataset.createOrReplaceTempView(binInnerRoundNumLogsViewName);
//
//            Dataset<Row> getInnerRoundNumQueryResult = spark.sql(queries.getInnerRoundNumQuery(BinInnerRoundNumLog.class));
//            getInnerRoundNumQueryResult.show();
//            getInnerRoundNumQueryResult
//                    .coalesce(1)
//                    .write()
//                    .mode("overwrite")
//                    .option("header", "true")
//                    .csv(hdfsLocation + "/" + analyticsDir + "/" + "binInnerRoundNumStats");
//        }
    }
}