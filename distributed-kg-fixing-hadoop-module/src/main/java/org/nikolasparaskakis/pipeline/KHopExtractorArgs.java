package org.nikolasparaskakis.pipeline;

import com.beust.jcommander.Parameter;

import java.util.ArrayList;
import java.util.List;

public class KHopExtractorArgs {
    @Parameter
    public List<String> parameters = new ArrayList<>();

    @Parameter(
            names = {"-h", "--help"},
            help = true,
            order = 0
    )
    public boolean help;

    @Parameter(
            names = {"-d", "--debugLogs"},
            description = "Log debug messages",
            order = 1
    )
    public boolean debugLogs = false;

    @Parameter(
            names = {"-t", "--tBoxFilePath"},
            description = "File with the T-Box of the KG",
            required = true,
            order = 2
    )
    public String tBoxFilePath;

    @Parameter(
            names = {"-se", "--sparqlEndpoint"},
            description = "Triple store endpoint URL that stores the A-Box of the KG" ,
            required = true,
            order = 3
    )
    public String sparqlEndpoint;

    @Parameter(
            names = {"-l", "--log-filename"},
            description = "Name of the log file produced",
            order = 4
    )
    public String logFilename = "KHopExtractor.log";

    @Parameter(
            names = {"-graph", "--graph-domain"},
            description = "Graph domain in the triple store",
            required = true,
            order = 5
    )
    public String graphDomain;

    @Parameter(
            names = {"-hdfs", "--hdfs-location"},
            description = "HDFS Location",
            required = true,
            order = 6
    )
    public String hdfsLocation;


    @Parameter(
            names = {"-inputDir", "--input-directory-path"},
            description = "Directory with input to resume",
            order = 7
    )
    public String inputDirectoryPath;

    @Parameter(
            names = {"-outputDir", "--output-directory-path"},
            description = "Directory to store output files",
            order = 8
    )
    public String outputDirectoryPath = "KGLinks";

    @Parameter(
            names = {"-b", "--batch-size"},
            description = "Batch size used in sparql queries",
            order = 9
    )
    public int batchSize = 10000;


    @Parameter(
            names = {"-mh", "--max-hops"},
            description = "Max hops to reach during extending",
            order = 10
    )
    public int maxHops = 1;

    @Parameter(
            names = {"-mi", "--max-iterations"},
            description = "Max iteration to reach during extending",
            order = 11
    )
    public int maxIterations = 0;

    @Parameter(
            names = {"-redT", "--reducer-tasks"},
            description = "Number of reducer tasks.",
            order = 12
    )
    public int reducerTasks = 1;

    @Parameter(
            names = {"-elimEqSets", "--eliminate-equal-sets"},
            description = "Eliminate equivalent sets.",
            order = 13
    )
    public boolean eliminateEqualSets = false;

    @Parameter(
            names = {"-elimSub", "--eliminate-subsets"},
            description = "Eliminate subsets.",
            order = 14
    )
    public boolean eliminateSubsets = false;

    public String getAllParams(){
        return "\nDebug logs: " + debugLogs +
                "\nT-Box file: " + tBoxFilePath +
                "\nSparql Endpoint: " + sparqlEndpoint +
                "\nLogfile name: " + logFilename +
                "\nGraph domain: " + graphDomain +
                "\nHDFS Location: " + hdfsLocation +
                "\nInput directory path: " + inputDirectoryPath +
                "\nOutput directory path: " + outputDirectoryPath +
                "\nBatch size: " + batchSize +
                "\nMax hops: " + maxHops +
                "\nMax iterations: " + maxIterations +
                "\nReducer tasks: " + reducerTasks +
                "\nElimine equal sets: " + eliminateEqualSets +
                "\nEliminate subsets: " + eliminateSubsets +
                "\n";
    }
}
