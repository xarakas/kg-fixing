package org.nikolasparaskakis.logs.bin;



import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.ArrayList;
import java.util.Arrays;



/**
 * Class representing a log entry for bin inner round number.
 */
@SuppressWarnings({"unused", "DuplicatedCode"})
public class BinInnerRoundNumLog {

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
    private final int innerRoundNum;

    /**
     * Constructor for BinInnerRoundNumLog.
     * @param baseIndividuals The base individuals associated with this log entry.
     * @param outerRound The outer round number.
     * @param innerRoundNum The inner round number.
     */
    public BinInnerRoundNumLog(ArrayList<String> baseIndividuals, int outerRound, int innerRoundNum) {
        this.baseIndividuals = baseIndividuals;
        this.outerRound = outerRound;
        this.innerRoundNum = innerRoundNum;
    }

    /**
     * Converts this BinInnerRoundNumLog object to a ObjectNode.
     * @return A ObjectNode representing this BinInnerRoundNumLog object.
     */
    public ObjectNode toObjectNode() {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode jsonObject = mapper.createObjectNode();
        jsonObject.put("baseIndividuals", String.join(",", this.baseIndividuals));
        jsonObject.put("outerRound", this.outerRound);
        jsonObject.put("innerRoundNum", this.innerRoundNum);
        return jsonObject;
    }

    /**
     * Parses a ObjectNode and returns a BinInnerRoundNumLog object.
     * @param node The ObjectNode to parse.
     * @return A BinInnerRoundNumLog object.
     */
    public static BinInnerRoundNumLog fromObjectNode(ObjectNode node) {
        ArrayList<String> baseIndividuals = new ArrayList<>(Arrays.asList(node.get("baseIndividuals").asText().split(",(?![^<>]*>)")));
        int outerRound = node.get("outerRound").asInt();
        int innerRoundNum = node.get("innerRoundNum").asInt();
        return new BinInnerRoundNumLog(baseIndividuals, outerRound, innerRoundNum);
    }

    /**
     * Converts this BinInnerRoundNumLog object to a JSON string.
     * @return A JSON string representing this BinInnerRoundNumLog object.
     * @throws RuntimeException if there is an error serializing to JSON.
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
     * Parses a JSON string and returns a BinInnerRoundNumLog object.
     * @param json The JSON string to parse.
     * @return A BinInnerRoundNumLog object.
     * @throws RuntimeException if there is an error parsing the JSON.
     */
    public static BinInnerRoundNumLog fromJsonString(String json) {
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
    public ArrayList<String> getBaseIndividuals() { return this.baseIndividuals; }

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
    public int getInnerRoundNum() { return this.innerRoundNum; }

    /**
     * Gets the variable name for innerRoundNum.
     * @return The variable name for innerRoundNum.
     */
    public static String getInnerRoundNumVarName() { return "innerRoundNum"; }
}