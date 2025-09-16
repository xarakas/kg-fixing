package org.nikolasparaskakis.logs.module;



import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;



/**
 * Class representing a log entry for module inner round numbers.
 * Each log entry contains information about the base individual,
 * outer round number, and inner round number.
 */
@SuppressWarnings({"unused", "DuplicatedCode"})
public class ModuleInnerRoundNumLog {

    /**
     * The base individual associated with this log entry.
     */
    private final String baseIndividual;

    /**
     * The outer round number.
     */
    private final int outerRound;

    /**
     * The inner round number.
     */
    private final int innerRoundNum;

    /** Constructor for ModuleInnerRoundNumLog.
     * @param baseIndividual The base individual associated with this log entry.
     * @param outerRound The outer round number.
     * @param innerRoundNum The inner round number.
     */
    public ModuleInnerRoundNumLog(String baseIndividual, int outerRound, int innerRoundNum) {
        this.baseIndividual = baseIndividual;
        this.outerRound = outerRound;
        this.innerRoundNum = innerRoundNum;
    }

    /** Converts this ModuleInnerRoundNumLog object to a ObjectNode.
     * @return A ObjectNode representing this ModuleInnerRoundNumLog object.
     */
    public ObjectNode toObjectNode() {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode jsonObject = mapper.createObjectNode();
        jsonObject.put("baseIndividual", this.baseIndividual);
        jsonObject.put("outerRound", this.outerRound);
        jsonObject.put("innerRoundNum", this.innerRoundNum);
        return jsonObject;
    }

    /** Parses a ObjectNode and returns a ModuleInnerRoundNumLog object.
     * @param root The ObjectNode to parse.
     * @return A ModuleInnerRoundNumLog object.
     * @throws RuntimeException if there is an error parsing the ObjectNode.
     */
    public static ModuleInnerRoundNumLog fromObjectNode(ObjectNode root) {
        String baseIndividual = root.get("baseIndividual").asText();
        int outerRound = root.get("outerRound").asInt();
        int innerRoundNum = root.get("innerRoundNum").asInt();
        return new ModuleInnerRoundNumLog(baseIndividual, outerRound, innerRoundNum);
    }

    /** Parses a JSON string and returns a ModuleInnerRoundNumLog object.
     * @param json The JSON string to parse.
     * @return A ModuleInnerRoundNumLog object.
     * @throws RuntimeException if there is an error parsing the JSON.
     */
    public static ModuleInnerRoundNumLog fromJsonString(String json) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            ObjectNode root = (ObjectNode) mapper.readTree(json);
            return fromObjectNode(root);
        } catch (Exception e) {
            throw new RuntimeException("Error parsing JSON", e);
        }
    }

    /** Converts this ModuleInnerRoundNumLog object to a JSON string.
     *
     * @return A JSON string representing this ModuleInnerRoundNumLog object.
     * @throws RuntimeException if there is an error converting to JSON.
     */
    public String toJsonString() {
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.writeValueAsString(this.toObjectNode());
        } catch (Exception e) {
            throw new RuntimeException("Error serializing to JSON", e);
        }
    }

    /** Getter for baseIndividual.
     * @return The base individual associated with this log entry.
     */
    public String getBaseIndividual() {
        return this.baseIndividual;
    }

    /** Getter for baseIndividual variable name.
     * @return The variable name for baseIndividual.
     */
    public static String getBaseIndividualVarName() { return "baseIndividual"; }

    /** Getter for outerRound.
     * @return The outer round number.
     */
    public int getOuterRound() {
        return this.outerRound;
    }

    /** Getter for outerRound variable name.
     * @return The variable name for outerRound.
     */
    public static String getOuterRoundVarName() { return "outerRound"; }

    /** Getter for innerRoundNum.
     * @return The inner round number.
     */
    public int getInnerRoundNum() { return this.innerRoundNum; }

    /** Getter for innerRoundNum variable name.
     * @return The variable name for innerRoundNum.
     */
    public static String getInnerRoundNumVarName() { return "innerRoundNum"; }
}