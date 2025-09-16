package org.nikolasparaskakis.logs.module;



import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.nikolasparaskakis.core.Fix;
import java.util.ArrayList;
import java.util.Set;



/**
 * Class representing a log entry for module fixes.
 * Each log entry contains information about the base individual,
 * outer and inner round numbers, and a set of lists of fixes applied during that round.
 * Each list represents a sequence of related fixes.
 */
@SuppressWarnings({"unused", "DuplicatedCode"})
public class MyFixesLog {

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

    /** Constructor for ModuleFixesLog.
     * @param outerRound The outer round number.
     * @param innerRound The inner round number.
     * @param fixes A set of lists of fixes applied during this round.
     */
    public MyFixesLog(int outerRound, int innerRound, Set<ArrayList<Fix>> fixes) {
        this.outerRound = outerRound;
        this.innerRound = innerRound;
        this.fixes = fixes;
    }

    /**
     * Converts this ModuleFixesLog object to an ObjectNode.
     * @return An ObjectNode representing this ModuleFixesLog object.
     */
    public ObjectNode toObjectNode() {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode root = mapper.createObjectNode();
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
     * Parses an ObjectNode and returns a ModuleFixesLog object.
     * @param node The ObjectNode to parse.
     * @return A ModuleFixesLog object.
     */
    public static MyFixesLog fromObjectNode(ObjectNode node) {
        int outerRound = node.get("outerRound").asInt();
        int innerRound = node.get("innerRound").asInt();

        Set<ArrayList<Fix>> fixesOfModule = new java.util.HashSet<>();
        ArrayNode fixesArray = (ArrayNode) node.get("fixes");
        for (JsonNode fixObjNode : fixesArray) {
            ArrayList<Fix> fixList = new ArrayList<>();
            ArrayNode sequenceArray = (ArrayNode) fixObjNode.get("sequenceOfFixes");
            for (JsonNode fixNode : sequenceArray) {
                // Assuming Fix has a static fromObjectNode(ObjectNode) method
                fixList.add(Fix.fromObjectNode((ObjectNode) fixNode));
            }
            fixesOfModule.add(fixList);
        }
        return new MyFixesLog(outerRound, innerRound, fixesOfModule);
    }

    /** Converts this ModuleFixesLog object to a JSON string.
     * @return A JSON string representing this ModuleFixesLog object.
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

    /** Parses a JSON string and returns a ModuleFixesLog object.
     * @param json The JSON string to parse.
     * @return A ModuleFixesLog object.
     * @throws RuntimeException if there is an error parsing the JSON.
     */
    public static MyFixesLog fromJsonString(String json) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            ObjectNode node = (ObjectNode) mapper.readTree(json);
            return fromObjectNode(node);
        } catch (Exception e) {
            throw new RuntimeException("Error deserializing from JSON", e);
        }
    }

    /**
     * Gets the outer round.
     * @return The outer round associated with this log entry.
     */
    public int getOuterRound() { return this.outerRound; }

    /**
     * Gets the variable name for outerRound.
     * @return The variable name for outerRound.
     */
    public String getOuterRoundVarName() { return "outerRound"; }

    /**
     * Gets the inner round.
     * @return The inner round associated with this log entry.
     */
    public int getInnerRound() { return this.innerRound; }

    /**
     * Gets the variable name for innerRound.
     * @return The variable name for innerRound.
     */
    public String getInnerRoundVarName() { return "innerRound"; }

    /**
     * Gets the list of fixes.
     * @return The list of fixes associated with this log entry.
     */
    public Set<ArrayList<Fix>> getFixes() { return this.fixes; }

    /**
     * Gets the variable name for fixes.
     * @return The variable name for fixes.
     */
    public String getFixesVarName() { return "fixes"; }
}