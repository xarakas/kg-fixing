package org.nikolasparaskakis.logs.bin;



import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.ArrayList;
import java.util.Arrays;



/**
 * Class representing a log entry for bin reasoning.
 */
@SuppressWarnings({"unused", "DuplicatedCode"})
public class BinReasoningLog {

    /**
     * The base individuals associated with this log entry.
     */
    private final ArrayList<String> baseIndividuals;

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

    /** Constructor for BinReasoningLog.
     * @param baseIndividuals The base individuals associated with this log entry.
     * @param outerRound The outer round number.
     * @param innerRound The inner round number.
     * @param checkConsistencyTimeMillis Time taken to check consistency in milliseconds.
     * @param checkRepairabilityTimeMillis Time taken to check repairability in milliseconds.
     * @param getExplanationsTimeMillis Time taken to get explanations in milliseconds.
     * @param computeFixesTimeMillis Time taken to compute fixes in milliseconds.
     * @param explanationsCount The number of explanations found.
     */
    public BinReasoningLog(ArrayList<String> baseIndividuals, int outerRound, int innerRound, long checkConsistencyTimeMillis, long checkRepairabilityTimeMillis, long getExplanationsTimeMillis, long computeFixesTimeMillis, int explanationsCount) {
        this.baseIndividuals = baseIndividuals;
        this.outerRound = outerRound;
        this.innerRound = innerRound;
        this.checkConsistencyTimeMillis = checkConsistencyTimeMillis;
        this.checkRepairabilityTimeMillis = checkRepairabilityTimeMillis;
        this.getExplanationsTimeMillis = getExplanationsTimeMillis;
        this.computeFixesTimeMillis = computeFixesTimeMillis;
        this.explanationsCount = explanationsCount;
    }

    /**
     * Converts this BinReasoningLog object to a ObjectNode.
     * @return A ObjectNode representing this BinReasoningLog object.
     */
    public ObjectNode toObjectNode() {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode jsonObject = mapper.createObjectNode();
        jsonObject.put("baseIndividuals", String.join(",", this.baseIndividuals));
        jsonObject.put("outerRound", this.outerRound);
        jsonObject.put("innerRound", this.innerRound);
        jsonObject.put("checkConsistencyTimeMillis", this.checkConsistencyTimeMillis);
        jsonObject.put("checkRepairabilityTimeMillis", this.checkRepairabilityTimeMillis);
        jsonObject.put("getExplanationsTimeMillis", this.getExplanationsTimeMillis);
        jsonObject.put("computeFixesTimeMillis", this.computeFixesTimeMillis);
        jsonObject.put("explanationsCount", this.explanationsCount);
        return jsonObject;
    }

    /**
     * Parses a ObjectNode and returns a BinReasoningLog object.
     * @param node The ObjectNode to parse.
     * @return A BinReasoningLog object.
     */
    public static BinReasoningLog fromObjectNode(ObjectNode node) {
        ArrayList<String> baseIndividuals = new ArrayList<>(Arrays.asList(node.get("baseIndividuals").asText().split(",(?![^<>]*>)")));
        int outerRound = node.get("outerRound").asInt();
        int innerRound = node.get("innerRound").asInt();
        long checkConsistencyTimeMillis = node.get("checkConsistencyTimeMillis").asLong();
        long checkRepairabilityTimeMillis = node.get("checkRepairabilityTimeMillis").asLong();
        long getExplanationsTimeMillis = node.get("getExplanationsTimeMillis").asLong();
        long computeFixesTimeMillis = node.get("computeFixesTimeMillis").asLong();
        int explanationsCount = node.get("explanationsCount").asInt();
        return new BinReasoningLog(
                baseIndividuals,
                outerRound,
                innerRound,
                checkConsistencyTimeMillis,
                checkRepairabilityTimeMillis,
                getExplanationsTimeMillis,
                computeFixesTimeMillis,
                explanationsCount
        );
    }

    /**
     * Converts this BinReasoningLog object to a JSON string.
     * @return A JSON string representing this BinReasoningLog object.
     * @throws RuntimeException if there is an error during serialization.
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
     * Parses a JSON string and returns a BinReasoningLog object.
     * @param json The JSON string to parse.
     * @return A BinReasoningLog object.
     * @throws RuntimeException if there is an error parsing the JSON.
     */
    public static BinReasoningLog fromJsonString(String json) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            ObjectNode node = (ObjectNode) mapper.readTree(json);
            return fromObjectNode(node);
        } catch (Exception e) {
            throw new RuntimeException("Error deserializing from JSON", e);
        }
    }

    /**
     * Gets the base individuals associated with this log entry.
     * @return The base individuals associated with this log entry.
     */
    public ArrayList<String> getBaseIndividuals() {
        return this.baseIndividuals;
    }

    /**
     * Gets the variable name for baseIndividuals.
     * @return The variable name for baseIndividuals.
     */
    public static String getBaseIndividualsVarName() { return "baseIndividuals"; }

    /**
     * Gets the outer round number.
     * @return The outer round number.
     */
    public int getOuterRound() { return this.outerRound; }

    /**
     * Gets the variable name for outerRound.
     * @return The variable name for outerRound.
     */
    public static String getOuterRoundVarName() { return "outerRound"; }

    /**
     * Gets the inner round number.
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