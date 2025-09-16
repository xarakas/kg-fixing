package org.nikolasparaskakis.logs.global;



import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.nikolasparaskakis.core.Fix;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;



/**
 * Class representing a log entry for fixes.
 * Each log entry contains information about the outer round number,
 * and a set of lists of fixes applied during that round.
 * Each list represents a sequence of related fixes.
 */
@SuppressWarnings({"unused", "DuplicatedCode"})
public class FixesLog {

    /**
     * The outer round number.
     */
    private final int outerRound;

    /**
     * A set of lists of fixes applied during this round.
     * Each list represents a sequence of related fixes.
     */
    private final Set<ArrayList<Fix>> fixes;

    /** Constructor for FixesLog.
     * @param outerRound The outer round number.
     * @param fixes A set of lists of fixes applied during this round.
     */
    public FixesLog(int outerRound, Set<ArrayList<Fix>> fixes) {
        this.outerRound = outerRound;
        this.fixes = fixes;
    }

    /**
     * Static method to create a FixesLog instance from a JSON ObjectNode.
     * @param root The ObjectNode containing the log data.
     * @return A FixesLog instance populated with data from the ObjectNode.
     */
    public static FixesLog fromObjectNode(ObjectNode root) {
        int outerRound = root.get("outerRound").asInt();
        Set<ArrayList<Fix>> fixes = new HashSet<>();
        ArrayNode sequences = (ArrayNode) root.get("candidateSequencesOfFixes");
        if (sequences != null) {
            for (JsonNode seqNode : sequences) {
                ArrayList<Fix> fixList = new ArrayList<>();
                for (JsonNode fixNode : seqNode) {
                    fixList.add(Fix.fromObjectNode((ObjectNode) fixNode));
                }
                fixes.add(fixList);
            }
        }
        return new FixesLog(outerRound, fixes);
    }

    /**
     * Converts this FixesLog instance to a JSON ObjectNode.
     * @return An ObjectNode representing this FixesLog instance.
     */
    public ObjectNode toObjectNode() {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode node = mapper.createObjectNode();
        node.put("outerRound", this.outerRound);

        // Assuming firstAxiom is the first Fix in the first sequence, or null if empty
        String firstAxiom = null;
        if (!fixes.isEmpty()) {
            ArrayList<Fix> firstSeq = fixes.iterator().next();
            if (!firstSeq.isEmpty()) {
                firstAxiom = firstSeq.get(0).toString(); // Adjust as needed
            }
        }
        node.put("firstAxiom", firstAxiom);

        ArrayNode sequencesNode = mapper.createArrayNode();
        for (ArrayList<Fix> fixList : fixes) {
            ArrayNode fixArray = mapper.createArrayNode();
            for (Fix fix : fixList) {
                fixArray.add(fix.toObjectNode());
            }
            sequencesNode.add(fixArray);
        }
        node.set("candidateSequencesOfFixes", sequencesNode);

        return node;
    }

    /**
     * Converts this FixesLog instance to a JSON string.
     * @return A JSON string representing this FixesLog instance.
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
     * Creates a FixesLog instance from a JSON string.
     * @param json The JSON string representing a FixesLog.
     * @return A FixesLog instance populated with data from the JSON string.
     */
    public static FixesLog fromJsonString(String json) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            ObjectNode node = (ObjectNode) mapper.readTree(json);
            return fromObjectNode(node);
        } catch (Exception e) {
            throw new RuntimeException("Error deserializing from JSON", e);
        }
    }

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
    public String getOuterRoundVarName() {
        return "outerRound";
    }

    /**
     * Gets the set of lists of fixes applied during this round.
     * @return A set of lists of fixes.
     */
    public Set<ArrayList<Fix>> getFixes() {
        return this.fixes;
    }

    /**
     * Gets the variable name for fixes.
     * @return The variable name for fixes.
     */
    public String getFixesVarName() {
        return "candidateSequencesOfFixes";
    }
}