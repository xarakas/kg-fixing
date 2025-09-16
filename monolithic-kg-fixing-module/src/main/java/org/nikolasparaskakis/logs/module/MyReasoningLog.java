package org.nikolasparaskakis.logs.module;



import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;



/**
 * Class representing a log entry for module reasoning.
 * Each log entry contains information about the base individual,
 * outer and inner round numbers, timing metrics, and explanations count.
 */
@SuppressWarnings({"unused", "DuplicatedCode"})
public class MyReasoningLog {

    /**
     * The outer round number.
     */
    private final int outerRound;

    /**
     * The inner round number.
     */
    private final int innerRound;

    /**
     * Time taken to check consistency in milliseconds.
     */
    private final long checkConsistencyTimeMillis;

    /**
     * Time taken to check repairability in milliseconds.
     */
    private final long checkRepairabilityTimeMillis;

    /**
     * Time taken to get explanations in milliseconds.
     */
    private final long getExplanationsTimeMillis;

    /**
     * Time taken to compute fixes in milliseconds.
     */
    private final long computeFixesTimeMillis;

    /**
     * The number of explanations found.
     */
    private final int explanationsCount;

    /** Constructor for ModuleReasoningLog.
     * @param outerRound The outer round number.
     * @param innerRound The inner round number.
     * @param checkConsistencyTimeMillis Time taken to check consistency in milliseconds.
     * @param checkRepairabilityTimeMillis Time taken to check repairability in milliseconds.
     * @param getExplanationsTimeMillis Time taken to get explanations in milliseconds.
     * @param computeFixesTimeMillis Time taken to compute fixes in milliseconds.
     * @param explanationsCount The number of explanations found.
     */
    public MyReasoningLog(int outerRound, int innerRound, long checkConsistencyTimeMillis, long checkRepairabilityTimeMillis, long getExplanationsTimeMillis, long computeFixesTimeMillis, int explanationsCount) {
        this.outerRound = outerRound;
        this.innerRound = innerRound;
        this.checkConsistencyTimeMillis = checkConsistencyTimeMillis;
        this.checkRepairabilityTimeMillis = checkRepairabilityTimeMillis;
        this.getExplanationsTimeMillis = getExplanationsTimeMillis;
        this.computeFixesTimeMillis = computeFixesTimeMillis;
        this.explanationsCount = explanationsCount;
    }

    /** Converts this ModuleReasoningLog object to a ObjectNode.
     * @return A ObjectNode representing this ModuleReasoningLog object.
     */
    public ObjectNode toObjectNode() {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode json = mapper.createObjectNode();
        json.put("outerRound", this.outerRound);
        json.put("innerRound", this.innerRound);
        json.put("checkConsistencyTimeMillis", this.checkConsistencyTimeMillis);
        json.put("checkRepairabilityTimeMillis", this.checkRepairabilityTimeMillis);
        json.put("getExplanationsTimeMillis", this.getExplanationsTimeMillis);
        json.put("computeFixesTimeMillis", this.computeFixesTimeMillis); // fixed mapping
        json.put("explanationsCount", this.explanationsCount);
        return json;
    }

    /** Parses a ObjectNode and returns a ModuleReasoningLog object.
     * @param root The ObjectNode to parse.
     * @return A ModuleReasoningLog object.
     * @throws RuntimeException if there is an error parsing the ObjectNode.
     */
    public static MyReasoningLog fromObjectNode(ObjectNode root) {
        int outerRound = root.path("outerRound").asInt();
        int innerRound = root.path("innerRound").asInt();
        long checkConsistencyTimeMillis = root.path("checkConsistencyTimeMillis").asLong();
        long checkRepairabilityTimeMillis = root.path("checkRepairabilityTimeMillis").asLong();
        long getExplanationsTimeMillis = root.path("getExplanationsTimeMillis").asLong();
        long computeFixesTimeMillis = root.path("computeFixesTimeMillis").asLong();
        int explanationsCount = root.path("explanationsCount").asInt();

        return new MyReasoningLog(
                outerRound,
                innerRound,
                checkConsistencyTimeMillis,
                checkRepairabilityTimeMillis,
                getExplanationsTimeMillis,
                computeFixesTimeMillis,
                explanationsCount
        );
    }

    /** Parses a JSON string to create a ModuleReasoningLog instance.
     * @param json  String containing the JSON representation of the log entry.
     * @return      ModuleReasoningLog instance.
     * @throws RuntimeException if there is an error parsing the JSON string.
     */
    public static MyReasoningLog fromJsonString(String json) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            ObjectNode root = (ObjectNode) mapper.readTree(json);
            return fromObjectNode(root);
        } catch (Exception e) {
            throw new RuntimeException("Error parsing JSON", e);
        }
    }

    /** Serialize this log entry to a JSON string.
     * @return String containing the JSON representation of this log entry.
     */
    public String toJsonString() {
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.writeValueAsString(this.toObjectNode());
        } catch (Exception e) {
            throw new RuntimeException("Error serializing to JSON", e);
        }
    }

    /**
     * Gets the outer round number associated with this log entry.
     * @return The outer round number.
     */
    public int getOuterRound() { return this.outerRound; }

    /**
     * Gets the variable name for outerRound.
     * @return The variable name for outerRound.
     */
    public static String getOuterRoundVarName() { return "outerRound"; }

    /**
     * Gets the inner round number associated with this log entry.
     * @return The inner round number.
     */
    public int getInnerRound() { return this.innerRound; }

    /**
     * Gets the variable name for innerRound.
     * @return The variable name for innerRound.
     */
    public static String getInnerRoundVarName() { return "innerRound"; }

    /**
     * Gets the time taken to check consistency in milliseconds.
     * @return The time taken to check consistency in milliseconds.
     */
    public long getCheckConsistencyTimeMillis() { return this.checkConsistencyTimeMillis; }

    /**
     * Gets the variable name for checkConsistencyTimeMillis.
     * @return The variable name for checkConsistencyTimeMillis.
     */
    public static String getCheckConsistencyTimeMillisVarName() { return "checkConsistencyTimeMillis"; }

    /**
     * Gets the time taken to check repairability in milliseconds.
     * @return The time taken to check repairability in milliseconds.
     */
    public long getCheckRepairabilityTimeMillis() { return this.checkRepairabilityTimeMillis; }

    /**
     * Gets the variable name for checkRepairabilityTimeMillis.
     * @return The variable name for checkRepairabilityTimeMillis.
     */
    public static String getCheckRepairabilityTimeMillisVarName() { return "checkRepairabilityTimeMillis"; }

    /**
     * Gets the time taken to get explanations in milliseconds.
     * @return The time taken to get explanations in milliseconds.
     */
    public long getGetExplanationsTimeMillis() { return this.getExplanationsTimeMillis; }

    /**
     * Gets the variable name for getExplanationsTimeMillis.
     * @return The variable name for getExplanationsTimeMillis.
     */
    public static String getGetExplanationsTimeMillisVarName() { return "getExplanationsTimeMillis"; }

    /**
     * Gets the time taken to compute fixes in milliseconds.
     * @return The time taken to compute fixes in milliseconds.
     */
    public long getComputeFixesTimeMillis() { return this.computeFixesTimeMillis; }

    /**
     * Gets the variable name for computeFixesTimeMillis.
     * @return The variable name for computeFixesTimeMillis.
     */
    public static String getComputeFixesTimeMillisVarName() { return "computeFixesTimeMillis"; }

    /**
     * Gets the number of explanations found.
     * @return The number of explanations found.
     */
    public int getExplanationsCount() { return this.explanationsCount; }

    /**
     * Gets the variable name for explanationsCount.
     * @return The variable name for explanationsCount.
     */
    public static String getExplanationsCountVarName() { return "explanationsCount"; }
}
