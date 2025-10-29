package org.nikolasparaskakis;



import com.beust.jcommander.Parameter;
import java.util.ArrayList;
import java.util.List;



@SuppressWarnings({"unused", "DuplicatedCode"})
public class KGFixerArgs {

    @Parameter
    public List<String> parameters = new ArrayList<>();

    @Parameter(
            names = {"-h", "--help"},
            help = true,
            order = 0
    )
    public boolean help;

    @Parameter(
            names = {"-t", "--tBox-file-path"},
            description = "File with the T-Box of the KG.",
            required = true,
            order = 1
    )
    public String tBoxFilePath;

    @Parameter(
            names = {"-se", "--sparql-endpoint"},
            description = "Triple store endpoint URL that stores the A-Box of the KG." ,
            required = true,
            order = 2
    )
    public String sparqlEndpoint;

    @Parameter(
            names = {"-graph", "--graph-domain"},
            description = "Graph domain inside the triple store.",
            required = true,
            order = 3
    )
    public String graphDomain;

    @Parameter(
            names = {"-hdfs", "--hdfs-location"},
            description = "HDFS location (URL).",
            required = true,
            order = 4
    )
    public String hdfsLocation;

    @Parameter(
            names = {"-inputDir", "--input-directory-path"},
            description = "Directory that contains the input file.",
            required = true,
            order = 5
    )
    public String inputDirectoryPath;

    @Parameter(
            names = {"-outputDir", "--output-directory-path"},
            description = "Directory in which the output file will be stored.",
            required = true,
            order = 6
    )
    public String outputDirectoryPath;

    @Parameter(
            names = {"-sio", "--save-initial-ontology"},
            description = "Save initial merged ontology to a file.",
            order = 7
    )
    public boolean saveInitialOntology = false;

    @Parameter(
            names = {"-sfo", "--save-fixed-ontology"},
            description = "Save fixed merged ontology to a file.",
            order = 8
    )
    public boolean saveFixedOntology = false;

    @Parameter(
            names = {"-l", "--log-file-name"},
            description = "Name of the log file produced by the run of the program.",
            order = 9
    )
    public String logFilename = "KGFixer.log";

    @Parameter(
            names = {"-mode", "--mode"},
            description = "Mode." +
                    "\n-1: Log module sizes" +
                    "\n-2: Only get explanations" +
                    "\n-3: Get explanations and apply A-Box fixing",
            required = true,
            order = 10
    )
    public int mode = 1;

    @Parameter(
            names = {"-r", "--reasoner-selection"},
            description = "OWL Reasoner." +
                    "\n-1: HermiT" +
                    "\n-2: Pellet" +
                    "\n-3: JFact" +
//                    "\n-4: Fact++" +
                    "\n-4: ELK",
            required = true,
            order = 11
    )
    public int reasonerSelection;

    /* Disjoint options for selection among fixes */
    @Parameter(
            names = {"-fixS", "--fix-selection"},
            description = "Strategy for selecting fixes." +
                    /* faster */
                    "\n-1: trivial-fix. Apply (trivial) A-Box fixing using only new Individuals." +
                    /* second faster */
                    "\n-2: greedy-fix.  Apply A-Box fixing calculating only the first (non-trivial) sound fix." +
                    /* consistency in fewer steps (more meaningful perhaps) */
                    "\n-3: rank-fix. Apply A-Box fix ranking based on number of explanations." +
                    "\n-4: iar-fix. Remove all ABox assertions present in inconsistency explanations." +
                    /* too slow for big graphs */
                    "\n-5: random-fix. Calculate all sound fixes and select a random one.",
            required = true,
            order = 12
    )
    public int fixSelection;

    /* Independent of the others */
    /* requires time to calculate maximally contained
     *  We suggest to enable mcd always - if not, opti-joint is employed i.e. considering all joint positions. */
    @Parameter(
            names = {"-mcd","--mcd"},
            description = "Apply A-Box fixing based on Maximally Contained positions only (mcd). " +
                    "If not enabled, all joint positions are considered (opti-joint).",
            order = 13
    )
    public boolean mcd = false;

    @Parameter(
            names = {"-per-op-timeout", "--per-op-timeout-millis"},
            description = "Timeout for the explanations retrieval task in milliseconds (default: 24hours)",
            order = 14
    )
    public int perOpTimeoutMillis = 120_000;

    @Parameter(
            names = {"-reasoner-timeout", "--reasoner-timeout-millis"},
            description = "Timeout for the explanations retrieval task in milliseconds (default: 24hours)",
            order = 15
    )
    public int reasonerTimeoutMillis = 120_000;

    @Parameter(
            names = {"-fixing-timeout", "--fixing-timeout-millis"},
            description = "Timeout for the fixing task in milliseconds (default: 24hours)",
            order = 16
    )
    public int fixingTimeoutMillis = 120_000;

    @Parameter(
            names = {"-expLim", "--explanations-limit"},
            description = "Maximum number of explanations to retrieve (default: Integer.MAX_VALUE)",
            order = 17
    )
    public int explanationsLimit = Integer.MAX_VALUE;

    @Parameter(
            names = {"-or", "--outer-round"},
            description = "Outer round",
            order = 18
    )
    public int outerRound = 1;

    public String getAllParams() {
        return  "\nT-Box file: " + tBoxFilePath +
                "\nSparql Endpoint: " + sparqlEndpoint +
                "\nGraph domain: " + graphDomain +
                "\nHDFS Location: " + hdfsLocation +
                "\nInput directory path: " + inputDirectoryPath +
                "\nOutput directory path: " + outputDirectoryPath +
                "\nSave initial ontology: " + saveInitialOntology +
                "\nSave fixed ontology: " + saveFixedOntology +
                "\nLog-file name: " + logFilename +
                "\nMode (1:log-module-sizes ; 2:only-explain ; 3:explain-and-fix): " + mode +
                "\nOWL Reasoner (1:HermiT ; 2:Pellet ; 3:JFact ; 4:Fact++ ; 5:ELK):" + reasonerSelection +
                "\nFix strategy (1:trivial-fix ; 2:greedy-fix ; 3:rank-fix ; 4:iar-fix ; 5:random-fix): " + fixSelection +
                "\nFixing with MCDs only: " + mcd +
                "\nTimeout per operation (explanation retrieval or fixing) in millis: " + perOpTimeoutMillis +
                "\nTimeout when retrieving explanations: " + reasonerTimeoutMillis +
                "\nTimeout when fixing: " + fixingTimeoutMillis +
                "\nExplanations limit: " + explanationsLimit +
                "\nOuter Round " + outerRound +
                "\n";
    }
}
