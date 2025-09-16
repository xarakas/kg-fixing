package org.nikolasparaskakis.logs.bin;



import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.nikolasparaskakis.core.Fix;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Set;



/**
 * Class representing a log entry for bin fixes.
 * Each log entry contains information about the base individuals,
 * outer and inner round numbers, and a set of lists of fixes applied during that round.
 * Each list represents a sequence of related fixes.
 */
@SuppressWarnings({"unused", "DuplicatedCode"})
public class BinFixesLog {

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
     * A set of lists of fixes applied during this round.
     * Each list represents a sequence of related fixes.
     */
    private final Set<ArrayList<Fix>> fixes;

    /**
     * Constructor for BinFixesLog.
     * @param baseIndividuals The base individuals associated with this log entry.
     * @param outerRound The outer round number.
     * @param innerRound The inner round number.
     * @param fixes A set of lists of fixes applied during this round.
     */
    public BinFixesLog(ArrayList<String> baseIndividuals, int outerRound, int innerRound, Set<ArrayList<Fix>> fixes) {
        this.baseIndividuals = baseIndividuals;
        this.outerRound = outerRound;
        this.innerRound = innerRound;
        this.fixes = fixes;
    }

    /**
     * Converts this BinFixesLog object to an ObjectNode.
     * @return An ObjectNode representing this BinFixesLog object.
     */
    public ObjectNode toObjectNode() {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode root = mapper.createObjectNode();
        root.put("baseIndividuals", String.join(",", this.baseIndividuals));
        root.put("outerRound", outerRound);
        root.put("innerRound", innerRound);

        ArrayNode fixesArray = root.putArray("fixes");
        for (ArrayList<Fix> fixList : fixes) {
            ObjectNode fixObj = mapper.createObjectNode();
            if (!fixList.isEmpty()) {
                Fix firstFix = fixList.get(0);
                fixObj.put("firstAxiom", firstFix.getOldAxiom().toString());
            } else {
                fixObj.put("firstAxiom", "");
            }
            ArrayNode sequenceArray = fixObj.putArray("sequenceOfFixes");
            for (Fix fix : fixList) {
                // Assuming Fix has a toObjectNode() method
                sequenceArray.add(fix.toObjectNode());
            }
            fixesArray.add(fixObj);
        }
        return root;
    }

    /**
     * Parses a ObjectNode and returns a BinFixesLog object.
     * @param node The ObjectNode to parse.
     * @return A BinFixesLog object.
     */
    public static BinFixesLog fromObjectNode(ObjectNode node) {
        ArrayList<String> baseIndividuals = new ArrayList<>(Arrays.asList(node.get("baseIndividuals").asText().split(",(?![^<>]*>)")));
        int outerRound = node.get("outerRound").asInt();
        int innerRound = node.get("innerRound").asInt();
        Set<ArrayList<Fix>> fixesOfBin = new java.util.HashSet<>();
        ArrayNode fixesArray = (ArrayNode) node.get("fixes");
        for (JsonNode fixObjNode : fixesArray) {
            ArrayList<Fix> fixList = new ArrayList<>();
            ArrayNode sequenceArray = (ArrayNode) fixObjNode.get("sequenceOfFixes");
            for (JsonNode fixNode : sequenceArray) {
                fixList.add(Fix.fromObjectNode((ObjectNode) fixNode));
            }
            fixesOfBin.add(fixList);
        }
        return new BinFixesLog(baseIndividuals, outerRound, innerRound, fixesOfBin);
    }

    /**
     * Converts this BinFixesLog object to its JSON string representation.
     * @return The JSON string representation of this BinFixesLog.
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
     * Creates a BinFixesLog object from its JSON string representation.
     * @param json The JSON string representation of the BinFixesLog.
     * @return The BinFixesLog object.
     * @throws RuntimeException if there is an error parsing the JSON string.
     */
    public static BinFixesLog fromJsonString(String json) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            ObjectNode node = (ObjectNode) mapper.readTree(json);
            return fromObjectNode(node);
        } catch (Exception e) {
            throw new RuntimeException("Error deserializing from JSON", e);
        }
    }

    /**
     * Gets the base individuals.
     * @return The base individuals associated with this log entry.
     */
    public ArrayList<String> getBaseIndividuals() { return this.baseIndividuals; }

    /**
     * Gets the variable name for baseIndividuals.
     * @return String representing the variable name for baseIndividuals.
     */
    public String getBaseIndividualsVarName() { return "baseIndividuals"; }

    /**
     * Gets the outer round.
     * @return The outer round associated with this log entry.
     */
    public int getOuterRound() { return this.outerRound; }

    /**
     * Gets the variable name for outerRound.
     * @return String representing the variable name for outerRound.
     */
    public String getOuterRoundVarName() { return "outerRound"; }

    /**
     * Gets the inner round.
     * @return The inner round associated with this log entry.
     */
    public int getInnerRound() { return this.innerRound; }

    /**
     * Gets the variable name for innerRound.
     * @return String representing the variable name for innerRound.
     */
    public String getInnerRoundVarName() { return "innerRound"; }

    /**
     * Gets the fixes of the bin.
     * @return The fixes of the bin associated with this log entry.
     */
    public Set<ArrayList<Fix>> getFixes() { return this.fixes; }

    /**
     * Gets the variable name for fixes.
     * @return String representing the variable name for fixes.
     */
    public String getFixesVarName() { return "fixes"; }
}