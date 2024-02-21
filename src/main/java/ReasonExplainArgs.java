import com.beust.jcommander.Parameter;
import java.util.ArrayList;
import java.util.List;

public class ReasonExplainArgs {
    @Parameter
    public List<String> parameters = new ArrayList<>();

    @Parameter(names = {"-h","--help"}, help = true, order = 0)
    public boolean help;

    @Parameter(names = {"-c", "--check-t-box"}, description = "Check also for T-Box inconsistencies", order = 1)
    public boolean checkT = false;

    @Parameter(names = {"-d", "--debug-logs"}, description = "Log debug messages", order = 2)
    public boolean debugLog = false;

    @Parameter(names = {"-t","--tbox"}, description = "File with the T-Box of the KG", required = true, order = 3)
    public String tBox = "";

    @Parameter(names = {"-a","--abox"}, description = "Comma-separated list of A-Box files of the KG" , order = 4)
    public String aBox = "";

    @Parameter(names = {"-ce","--class-expression"}, description = "File with class expressions in Manchester format", order = 5)
    public String ce = "";

    @Parameter(names = {"-n","--no-extraction"}, description = "Do not extract modules, explain the whole KG", order = 6)
    public boolean no = false;

    @Parameter(names = {"-s","--save-ontology"}, description = "Save merged ontology to file", order = 7)
    public boolean save = false;

    @Parameter(names = {"-l","--logfilename"}, description = "Name of the log file", order = 8)
    public String logFile = "ExtractModules.log";

    @Parameter(names = {"-o","--output-path"}, description = "Path to save the fixed KG file" , order = 9)
    public String output = "";

    @Parameter(names = {"-e","--extend-module"}, description = "Extend modules to account for more inconsistency types", order = 10)
    public boolean extend = false;

    @Parameter(names = {"-x","--fix-inconsistency"}, description = "Apply A-Box fixing method", order = 11)
    public boolean fixinc = false;

    /* Independent from the others */
    /* requires time to calculate maximally contained
    *  We suggest to enable mcd always - if not, opti-joint is employed i.e. considering all joint positions. */
    @Parameter(names = {"-mcd","--mcd-position"}, description =
            "Apply A-Box fixing based on Maximally ContaineD positions only (mcd)." +
            "Default: All joint positions are considered (opti-joint).", order = 12)
    public boolean mcd = false;

    /* Disjoint options for selection among fixes */
    @Parameter(names = {"-fixS","--fix-selection"}, description = "Strategy for selecting fixes." +
            /* faster */
            "\n-1: trivial-fix. Apply (trivial) A-Box fixing using only new Individuals." +
            /* second faster */
            "\n-2: greedy-fix.  Apply A-Box fixing calculating only the first (non-trivial) sound fix." +
            /* consistency in fewer steps (more meaningful perhaps) */
            "\n-3: rank-fix. Apply A-Box fix ranking based on number of explanations" +
            /* too slow for big graphs */
            "\n-other: random-fix. Calculate all sound fixes and select a random one.", order = 13)
    public int fixSelection = 0;

//    /* Disjoint from rank-fixes and greedy-fix */
//    /* faster */
//    @Parameter(names = {"-tFix","--trivial-fix"}, description = "Apply (trivial) A-Box fixing using only new Individuals", order = 13)
//    public boolean newIndividuals = false;
//
//    /* second faster */
//    @Parameter(names = {"-gFix","--greedy-fix"}, description = "Apply A-Box fixing calculating only the first (non-trivial) sound fix", order = 14)
//    public boolean firstFix = false;
//
//    /* consistency in fewer steps (more meaningful perhaps) */
//    @Parameter(names = {"-rFix","--rank-fix"}, description = "Apply A-Box fix ranking based on number of explanations", order = 15)
//    public boolean rankFixes = false;

    @Parameter(names = {"-to","--reasoner-timeout"}, description = "Timeout for the explanations retrieval task in milliseconds (default: 24hours)", order = 16)
    public int timeout = 86400000;

    @Parameter(names = {"-ft","--fixing-timeout"}, description = "Timeout for the fixing task in milliseconds (default: 24hours)", order = 17)
    public int fixingTimeout = 86400000;

    @Parameter(names = {"-explim", "--explanations-limit"}, description = "Maximum number of explanations to retrieve (default: Integer.MAX_VALUE)", order = 18)
    public int explanationsLimit = Integer.MAX_VALUE;

    public String getAllParams(){
        return "\nDebug logs: " + debugLog +
                "\nCheck T-Box inconsistency: " + checkT +
                "\nT-Box file: " + tBox +
                "\nA-Box file(s): " + aBox +
                "\nClass Expression input: " + ce +
                "\nLogfile name: " + logFile +
                "\nDo not split into modules: " + no +
                "\nSave the merged KG to file: " + save +
                "\nFilename to save the fixed KG:  " + output +
                "\nExtend modules: " + extend +
                "\nFix A-Box inconsistency: " + fixinc +
                "\nFixing with MCDs only: " + mcd +
                "\nFix selection strategy (1:trivial-fix, 2:greedy-fix, 3:rank-fix, default:random-fix) : " + fixSelection +
                "\nTimeout when retrieving explanations: " + timeout +
                "\nTimeout when fixing: " + fixingTimeout +
                "\nExplanations limit: " + explanationsLimit +
                "\n";
    }
//    @Parameter(names = "-debug", description = "Debug mode")
//    private boolean debug = false;
}
