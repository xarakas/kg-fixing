package org.nikolasparaskakis.logs.bin;



import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.ArrayList;
import java.util.Arrays;



/**
 * Class representing a log entry for bin size.
 */
@SuppressWarnings({"unused", "DuplicatedCode"})
public class BinSizeLog {

    /**
     * The base individuals associated with this log entry.
     */
    private final ArrayList<String> baseIndividuals;

    /**
     * The outer round number.
     */
    private final int outerRound;

    /**
     * The size of the bin.
     */
    private final int binSize;

    /** Constructor for BinSizeLog.
     * @param baseIndividuals The base individuals associated with this log entry.
     * @param outerRound The outer round number.
     * @param binSize The size of the bin.
     */
    public BinSizeLog(ArrayList<String> baseIndividuals, int outerRound, int binSize) {
        this.baseIndividuals = baseIndividuals;
        this.outerRound = outerRound;
        this.binSize = binSize;
    }

    /** Converts this BinSizeLog object to a ObjectNode.
     * @return A ObjectNode representing this BinSizeLog object.
     */
    public ObjectNode toObjectNode() {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode jsonObject = mapper.createObjectNode();
        jsonObject.put("baseIndividuals", String.join(",", this.baseIndividuals));
        jsonObject.put("outerRound", this.outerRound);
        jsonObject.put("binSize", this.binSize);
        return jsonObject;
    }

    /** Parses a ObjectNode and returns a BinSizeLog object.
     * @param node The ObjectNode to parse.
     * @return A BinSizeLog object.
     */
    public static BinSizeLog fromObjectNode(ObjectNode node) {
        ArrayList<String> baseIndividuals = new ArrayList<>(Arrays.asList(node.get("baseIndividuals").asText().split(",(?![^<>]*>)")));
        int outerRound = node.get("outerRound").asInt();
        int binSize = node.get("binSize").asInt();
        return new BinSizeLog(baseIndividuals, outerRound, binSize);
    }

    /** Converts this BinSizeLog object to a JSON string.
     * @return A JSON string representing this BinSizeLog object.
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

    /** Parses a JSON string to create a BinSizeLog instance.
     * @param json  String containing the JSON representation of the log entry.
     * @return      BinSizeLog instance.
     * @throws RuntimeException if there is an error parsing the JSON string.
     */
    public static BinSizeLog fromJsonString(String json) {
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
    public int getOuterRound() {
        return this.outerRound;
    }

    /**
     * Gets the variable name for outerRound.
     * @return The variable name for outerRound.
     */
    public static String getOuterRoundVarName() { return "outerRound"; }

    /**
     * Gets the size of the bin.
     * @return The size of the bin.
     */
    public int getBinSize() { return this.binSize; }

    /**
     * Gets the variable name for binSize.
     * @return The variable name for binSize.
     */
    public static String getBinSizeVarName() { return "binSize"; }
}