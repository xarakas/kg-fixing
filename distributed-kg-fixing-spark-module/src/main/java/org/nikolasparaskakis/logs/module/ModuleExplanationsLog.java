package org.nikolasparaskakis.logs.module;



import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.nikolasparaskakis.utils.FunctionalAxiomRenderer;
import org.nikolasparaskakis.utils.OWLFunctionalSyntaxParser;
import org.semanticweb.owlapi.model.OWLAxiom;
import java.util.ArrayList;
import java.util.List;



/**
 * Class representing a log entry for module explanations.
 * Each log entry contains information about the base individual,
 * outer and inner round numbers, and a list of explanations.
 * Each explanation is represented as a list of OWLAxioms.
 */
@SuppressWarnings({"unused", "DuplicatedCode"})
public class ModuleExplanationsLog {

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
    private final int innerRound;

    /**
     * A list of explanations, where each explanation is a list of OWLAxioms.
     */
    private final List<List<OWLAxiom>> explanations;

    /**
     * Constructor for ModuleExplanationsLog.
     * @param baseIndividual    The base individual associated with this log entry.
     * @param outerRound       The outer round number.
     * @param innerRound       The inner round number.
     * @param explanations     A list of explanations, where each explanation is a list of OWLAxioms.
     */
    public ModuleExplanationsLog(String baseIndividual, int outerRound, int innerRound, List<List<OWLAxiom>> explanations) {
        this.baseIndividual = baseIndividual;
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
        jsonExplanations.put("baseIndividual", this.baseIndividual);
        jsonExplanations.put("outerRound", this.outerRound);
        jsonExplanations.put("innerRound", this.innerRound);

        // Explanations: nested arrays
        ArrayNode explanationsArray = mapper.createArrayNode();
        for (List<OWLAxiom> explanation : this.explanations) {
            ArrayNode explanationArray = mapper.createArrayNode();
            for (OWLAxiom axiom : explanation) {
//                explanationArray.add(axiom.toString());
                explanationArray.add(FunctionalAxiomRenderer.render(axiom));
            }
            explanationsArray.add(explanationArray);
        }

        // Add nested explanations array to main JSON object
        jsonExplanations.set("explanations", explanationsArray);

        // Return the constructed JSON object
        return jsonExplanations;
    }

    /**
     * Create a ModuleExplanationsLog instance from a Jackson ObjectNode.
     * @param root  ObjectNode containing the JSON representation of the log entry.
     * @return      ModuleExplanationsLog instance.
     */
    public static ModuleExplanationsLog fromObjectNode(ObjectNode root) {
        List<List<OWLAxiom>> explanations = new ArrayList<>();
        JsonNode baseIndividualNode = root.get("baseIndividual");
        JsonNode outerRoundNode = root.get("outerRound");
        JsonNode innerRoundNode = root.get("innerRound");
        JsonNode explanationsArray = root.get("explanations");
        if (explanationsArray != null && explanationsArray.isArray()) {
            for (JsonNode explanationArray : explanationsArray) {
                List<OWLAxiom> explanation = new ArrayList<>();
                for (JsonNode axiomNode : explanationArray) {
                    String axiomStr = axiomNode.asText();
//                    axiomStr = sanitizeFunctionalLiteral(axiomStr);
                    OWLFunctionalSyntaxParser parser = new OWLFunctionalSyntaxParser();
                    OWLAxiom parsedAxiom = parser.parse(axiomStr);
                    explanation.add(parsedAxiom);
                }
                explanations.add(explanation);
            }
        }
        return new ModuleExplanationsLog(baseIndividualNode.asText(), outerRoundNode.asInt(), innerRoundNode.asInt(), explanations);
    }

    /**
     * Parse a JSON string to create a ModuleExplanationsLog instance.
     * @param json  String containing the JSON representation of the log entry.
     * @return      ModuleExplanationsLog instance.
     * @throws RuntimeException if there is an error parsing the JSON string.
     */
    public static ModuleExplanationsLog fromJsonString(String json) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            if (json == null) throw new IllegalArgumentException("null json");
            String s = json.trim();
            if (!s.isEmpty() && s.charAt(0) == '\uFEFF') s = s.substring(1); // strip BOM
            if (s.isEmpty()) throw new IllegalArgumentException("empty line");

            JsonNode node = mapper.readTree(s);
            if (node == null || !node.isObject()) {
                throw new IllegalArgumentException("expected OBJECT, got " +
                        (node == null ? "null" : node.getNodeType()));
            }
            return fromObjectNode((ObjectNode) node);
        } catch (Exception e) {
            String prev = json == null ? "null" : (json.length() <= 200 ? json : json.substring(0, 200) + "…");
            throw new RuntimeException("Error parsing JSON line: " + prev, e);
        }
    }

    /**
     * Serialize this log entry to a JSON string.
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
     * Get the base individual associated with this log entry.
     * @return String representing the base individual.
     */
    public String getBaseIndividual() {
        return this.baseIndividual;
    }

    /**
     * Get the variable name used for the base individual in logs.
     * @return String representing the variable name for the base individual.
     */
    public String getBaseIndividualVarName() { return "baseIndividual"; }

    /**
     * Get the outer round number associated with this log entry.
     * @return int representing the outer round number.
     */
    public int getOuterRound() {
        return this.outerRound;
    }

    /**
     * Get the variable name used for the outer round in logs.
     * @return String representing the variable name for the outer round.
     */
    public String getOuterRoundVarName() { return "outerRound"; }

    /**
     * Get the inner round number associated with this log entry.
     * @return int representing the inner round number.
     */
    public int getInnerRound() { return this.innerRound; }

    /**
     * Get the variable name used for the inner round in logs.
     * @return String representing the variable name for the inner round.
     */
    public String getInnerRoundVarName() { return "innerRound"; }

    /**
     * Get the list of explanations associated with this log entry.
     * Each explanation is a list of OWLAxioms.
     * @return List of List of OWLAxioms representing the explanations.
     */
    public List<List<OWLAxiom>> getExplanations() { return this.explanations; }

    /**
     * Get the variable name used for explanations in logs.
     * @return String representing the variable name for explanations.
     */
    public String getExplanationsVarName() { return "explanations"; }
}