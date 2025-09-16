package org.nikolasparaskakis.logs.bin;



import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.nikolasparaskakis.utils.OWLFunctionalSyntaxParser;
import org.semanticweb.owlapi.model.OWLAxiom;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;


/**
 * Class representing a log entry for bin explanations.
 * Each log entry contains information about the base individuals,
 * outer round number, and a list of explanations.
 * Each explanation is represented as a list of OWLAxioms.
 */
@SuppressWarnings({"unused", "DuplicatedCode"})
public class BinExplanationsLog {

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
     * A list of explanations, where each explanation is a list of OWLAxioms.
     */
    private final List<List<OWLAxiom>> explanations;

    /**
     * Constructor for BinExplanationsLog.
     * @param baseIndividuals The base individuals associated with this log entry.
     * @param outerRound The outer round number.
     * @param innerRound The inner round number.
     * @param explanations A list of explanations, where each explanation is a list of OWLAxioms.
     */
    public BinExplanationsLog(ArrayList<String> baseIndividuals, int outerRound, int innerRound, List<List<OWLAxiom>> explanations) {
        this.baseIndividuals = baseIndividuals;
        this.outerRound = outerRound;
        this.innerRound = innerRound;
        this.explanations = explanations;
    }

    /**
     * Build a JSON ObjectNode representation of this log entry.
     * @return ObjectNode representing this log entry.
     */
    public ObjectNode toObjectNode() {

        // Create ObjectMapper instance
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode jsonExplanations = mapper.createObjectNode();

        // Simple fields
        jsonExplanations.put("baseIndividuals", String.join(",", this.baseIndividuals));
        jsonExplanations.put("outerRound", this.outerRound);
        jsonExplanations.put("innerRound", this.innerRound);

        // Explanations: nested arrays
        ArrayNode explanationsArray = mapper.createArrayNode();
        for (List<OWLAxiom> explanation : this.explanations) {
            ArrayNode explanationArray = mapper.createArrayNode();
            for (OWLAxiom axiom : explanation) {
                explanationArray.add(axiom.toString());
            }
            explanationsArray.add(explanationArray);
        }

        // Add nested explanations array to main JSON object
        jsonExplanations.set("explanations", explanationsArray);

        // Return the constructed JSON object
        return jsonExplanations;
    }

    /**
     * Parses an ObjectNode and returns a BinExplanationsLog object.
     * @param root The ObjectNode to parse.
     * @return A BinExplanationsLog object.
     */
    public static BinExplanationsLog fromObjectNode(ObjectNode root) {
        List<List<OWLAxiom>> explanations = new ArrayList<>();
        ArrayList<String> baseIndividuals = new ArrayList<>(Arrays.asList(root.get("baseIndividuals").asText().split(",(?![^<>]*>)")));
        JsonNode outerRoundNode = root.get("outerRound");
        JsonNode innerRoundNode = root.get("innerRound");
        JsonNode explanationsArray = root.get("explanations");
        if (explanationsArray != null && explanationsArray.isArray()) {
            for (JsonNode explanationArray : explanationsArray) {
                List<OWLAxiom> explanation = new ArrayList<>();
                for (JsonNode axiomNode : explanationArray) {
                    String axiomStr = axiomNode.asText();
                    OWLFunctionalSyntaxParser parser = new OWLFunctionalSyntaxParser();
                    OWLAxiom parsedAxiom = parser.parse(axiomStr);
                    explanation.add(parsedAxiom);
                }
                explanations.add(explanation);
            }
        }
        return new BinExplanationsLog(baseIndividuals, outerRoundNode.asInt(), innerRoundNode.asInt(), explanations);
    }

    /**
     * Parses a JSON string and returns a BinExplanationsLog object.
     * @param json The JSON string to parse.
     * @return A BinExplanationsLog object.
     * @throws RuntimeException if there is an error parsing the JSON.
     */
    public static BinExplanationsLog fromJsonString(String json) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            ObjectNode node = (ObjectNode) mapper.readTree(json);
            return fromObjectNode(node);
        } catch (Exception e) {
            throw new RuntimeException("Error deserializing from JSON", e);
        }
    }

    /**
     * Converts this BinExplanationsLog object to a JSON string.
     *
     * @return A JSON string representing this BinExplanationsLog object.
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

    /**
     * Get the base individuals associated with this log entry.
     * @return ArrayList<String> representing the base individuals.
     */
    public ArrayList<String> getBaseIndividuals() {
        return this.baseIndividuals;
    }

    /**
     * Get the variable name for baseIndividuals.
     * @return String representing the variable name for baseIndividuals.
     */
    public static String getBaseIndividualsVarName() { return "baseIndividuals"; }

    /**
     * Get the outer round number.
     * @return int representing the outer round number.
     */
    public int getOuterRound() { return this.outerRound; }

    /**
     * Get the variable name for outerRound.
     * @return String representing the variable name for outerRound.
     */
    public static String getOuterRoundVarName() { return "outerRound"; }

    /**
     * Get the inner round number associated with this log entry.
     * @return int representing the inner round number.
     */
    public int getInnerRound() { return this.innerRound; }

    /**
     * Get the variable name for innerRound.
     * @return String representing the variable name for innerRound.
     */
    public static String getInnerRoundVarName() { return "innerRound"; }

    /**
     * Get the explanations associated with this log entry.
     * @return List of explanations, where each explanation is a list of OWLAxioms.
     */
    public List<List<OWLAxiom>> getExplanations() { return this.explanations; }

    /**
     * Get the variable name for explanations.
     * @return String representing the variable name for explanations.
     */
    public static String getExplanationsVarName() { return "explanations"; }
}