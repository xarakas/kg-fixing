package org.nikolasparaskakis.logs.global;



import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.nikolasparaskakis.utils.OWLFunctionalSyntaxParser;
import org.semanticweb.owlapi.model.OWLAxiom;
import java.util.ArrayList;
import java.util.List;



/**
 * Class representing a log entry for explanations.
 * Each log entry contains information about the outer round number,
 * and a list of explanations.
 * Each explanation is represented as a list of OWLAxioms.
 */
@SuppressWarnings({"unused", "DuplicatedCode"})
public class ExplanationLog {

    /**
     * The outer round number.
     */
    private final int outerRound;

    /**
     * A list of OWLAxioms representing the explanation.
     */
    private final List<OWLAxiom> explanation;

    /**
     * Constructor for ExplanationLog.
     * @param outerRound The outer round number.
     * @param explanation A list of OWLAxioms representing the explanation.
     */
    public ExplanationLog(int outerRound, List<OWLAxiom> explanation) {
        this.outerRound = outerRound;
        this.explanation = explanation;
    }

    /**
     * Build a JSON ObjectNode representation of this log entry.
     * @return ObjectNode representing this log entry.
     */
    public ObjectNode toObjectNode() {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode jsonExplanations = mapper.createObjectNode();
        jsonExplanations.put("outerRound", this.outerRound);
        ArrayNode explanationArray = mapper.createArrayNode();
        for (OWLAxiom axiom : this.explanation) {
            explanationArray.add(axiom.toString());
        }
        jsonExplanations.set("explanation", explanationArray);

        return jsonExplanations;
    }

    /**
     * Parses a ObjectNode and returns an ExplanationLog object.
     * @param root The ObjectNode to parse.
     * @return An ExplanationLog object.
     */
    public static ExplanationLog fromObjectNode(ObjectNode root) {
        int outerRound = root.get("outerRound").asInt();
        List<OWLAxiom> explanationList = new ArrayList<>();
        JsonNode explanationArray = root.get("explanation");
        if (explanationArray != null && explanationArray.isArray()) {
            OWLFunctionalSyntaxParser parser = new OWLFunctionalSyntaxParser();
            for (JsonNode axiomNode : explanationArray) {
                String axiomStr = axiomNode.asText();
                OWLAxiom parsedAxiom = parser.parse(axiomStr);
                explanationList.add(parsedAxiom);
            }
        }
        return new ExplanationLog(outerRound, explanationList);
    }

    /**
     * Parses a JSON string and returns an ExplanationsLog object.
     * @param json The JSON string to parse.
     * @return An ExplanationsLog object.
     * @throws RuntimeException if there is an error parsing the JSON.
     */
    public static ExplanationLog fromJsonString(String json) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            ObjectNode root = (ObjectNode) mapper.readTree(json);
            return fromObjectNode(root);
        } catch (Exception e) {
            throw new RuntimeException("Error parsing JSON", e);
        }
    }

    /**
     * Converts this ExplanationLog object to a JSON string.
     * @return A JSON string representing this ExplanationLog object.
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
     * Get the outer round number.
     * @return int representing the outer round number.
     */
    public int getOuterRound() {
        return this.outerRound;
    }

    /**
     * Get the variable name used for outer round in JSON representation.
     * @return String representing the variable name for outer round.
     */
    public String getOuterRoundVarName() {
        return "outerRound";
    }

    /**
     * Get the explanation.
     * @return List of OWLAxioms representing the explanation.
     */
    public List<OWLAxiom> getExplanation() {
        return this.explanation;
    }

    /**
     * Get the variable name used for explanations in JSON representation.
     * @return String representing the variable name for explanations.
     */
    public String getExplanationVarName() {
        return "explanation";
    }
}