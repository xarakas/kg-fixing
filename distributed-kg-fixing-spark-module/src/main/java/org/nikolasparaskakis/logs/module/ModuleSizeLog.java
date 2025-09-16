package org.nikolasparaskakis.logs.module;



import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;



/**
 * Class representing a log entry for module size.
 * Each log entry contains information about the base individual,
 * outer round number, and module size.
 */
@SuppressWarnings({"unused", "DuplicatedCode"})
public class ModuleSizeLog {

    /**
     * The base individual associated with this log entry.
     */
    private final String baseIndividual;

    /**
     * The outer round number.
     */
    private final int outerRound;

    /**
     * The size of the module.
     */
    private final int moduleSize;

    /** Constructor for ModuleSizeLog.
     * @param baseIndividual The base individual associated with this log entry.
     * @param outerRound The outer round number.
     * @param moduleSize The size of the module.
     */
    public ModuleSizeLog(String baseIndividual, int outerRound, int moduleSize) {
        this.baseIndividual = baseIndividual;
        this.outerRound = outerRound;
        this.moduleSize = moduleSize;
    }

    /** Converts this ModuleInnerRoundNumLog object to a ObjectNode.
     * @return A ObjectNode representing this ModuleInnerRoundNumLog object.
     */
    public ObjectNode toObjectNode() {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode json = mapper.createObjectNode();
        json.put("baseIndividual", this.baseIndividual);
        json.put("outerRound", this.outerRound);
        json.put("moduleSize", this.moduleSize);
        return json;
    }

    /** Parses a ObjectNode and returns a ModuleInnerRoundNumLog object.
     * @param root The ObjectNode to parse.
     * @return A ModuleInnerRoundNumLog object.
     * @throws RuntimeException if there is an error parsing the ObjectNode.
     */
    public static ModuleSizeLog fromObjectNode(ObjectNode root) {
        String baseIndividual = root.path("baseIndividual").asText(null);
        int outerRound = root.path("outerRound").asInt();
        int moduleSize = root.path("moduleSize").asInt();
        return new ModuleSizeLog(baseIndividual, outerRound, moduleSize);
    }

    /** Parses a JSON string and returns a ModuleSizeLog object.
     * @param json The JSON string to parse.
     * @return A ModuleSizeLog object.
     * @throws RuntimeException if there is an error parsing the JSON.
     */
    public static ModuleSizeLog fromJsonString(String json) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            ObjectNode root = (ObjectNode) mapper.readTree(json);
            return fromObjectNode(root);
        } catch (Exception e) {
            throw new RuntimeException("Error parsing JSON", e);
        }
    }

    /** Converts this ModuleInnerRoundNumLog object to a JSON string.
     * @return A JSON string representing this ModuleInnerRoundNumLog object.
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
     * Gets the base individual associated with this log entry.
     * @return The base individual.
     */
    public String getBaseIndividual() { return this.baseIndividual; }

    /**
     * Gets the variable name for baseIndividual.
     * @return The variable name for baseIndividual.
     */
    public static String getBaseIndividualVarName() { return "baseIndividual"; }

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
     * Gets the size of the module.
     * @return The size of the module.
     */
    public int getModuleSize() { return this.moduleSize; }

    /**
     * Gets the variable name for baseIndividual.
     * @return The variable name for baseIndividual.
     */
    public static String getModuleSizeVarName() { return "moduleSize"; }
}