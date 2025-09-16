package org.nikolasparaskakis.logs.bin;



import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;



/**
 * Class representing a log entry for bin events.
 */
@SuppressWarnings({"unused", "DuplicatedCode"})
public class BinEventLog {

    /**
     * The base individual associated with this log entry.
     */
    private final ArrayList<String> baseIndividuals;

    /**
     * The type of event.
     */
    private final BinEvent eventType;

    /**
     * The timestamp of the event.
     */
    private final LocalDateTime localDateTime;

    /** Constructor for BinEventLog.
     * @param baseIndividuals The base individuals associated with this log entry.
     * @param eventType The type of event.
     * @param localDateTime The timestamp of the event.
     */
    public BinEventLog(ArrayList<String> baseIndividuals, BinEvent eventType, LocalDateTime localDateTime) {
        this.baseIndividuals = baseIndividuals;
        this.eventType = eventType;
        this.localDateTime = localDateTime;
    }

    /**
     * Converts this BinEventLog object to a ObjectNode.
     * @return A ObjectNode representing this BinEventLog object.
     */
    public ObjectNode toObjectNode() {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode objectNode = mapper.createObjectNode();
        objectNode.put("baseIndividuals", String.join(",", this.baseIndividuals));
        objectNode.put("eventType", this.eventType.toString());
        objectNode.put("localDateTime", this.localDateTime != null ? this.localDateTime.toString() : null);
        return objectNode;
    }

    /**
     * Parses a ObjectNode and returns a BinEventLog object.
     * @param node The ObjectNode to parse.
     * @return A BinEventLog object.
     */
    public static BinEventLog fromObjectNode(ObjectNode node) {
        ArrayList<String> baseIndividuals = new ArrayList<>(Arrays.asList(node.get("baseIndividuals").asText().split(",(?![^<>]*>)")));
        String eventType = node.get("eventType").asText();
        String localDateTimeStr = node.get("localDateTime").asText(null);
        LocalDateTime localDateTime = localDateTimeStr != null ? LocalDateTime.parse(localDateTimeStr) : null;
        return new BinEventLog(baseIndividuals, Enum.valueOf(BinEvent.class, eventType), localDateTime);
    }

    /**
     * Converts this BinEventLog object to a JSON string.
     * @return A JSON string representing this BinEventLog object.
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
     * Parses a JSON string to create a BinEventLog instance.
     * @param json  String containing the JSON representation of the log entry.
     * @return      BinEventLog instance.
     * @throws RuntimeException if there is an error parsing the JSON string.
     */
    public static BinEventLog fromJsonString(String json) {
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
     * @return ArrayList of Strings representing the base individuals.
     */
    public ArrayList<String> getBaseIndividuals() {
        return this.baseIndividuals;
    }

    /**
     * Gets the variable name for baseIndividuals.
     * @return String representing the variable name for baseIndividuals.
     */
    public static String getBaseIndividualsVarName() { return "baseIndividuals"; }

    /**
     * Gets the type of event.
     * @return String representing the type of event.
     */
    public BinEvent getEventType() { return this.eventType; }

    /**
     * Gets the variable name for eventType.
     * @return String representing the variable name for eventType.
     */
    public static String getEventTypeVarName() { return "eventType"; }

    /**
     * Gets the timestamp of the event.
     * @return LocalDateTime representing the timestamp of the event.
     */
    public LocalDateTime getLocalDateTime() { return this.localDateTime; }

    /**
     * Gets the variable name for localDateTime.
     * @return String representing the variable name for localDateTime.
     */
    public static String getLocalDateTimeVarName() { return "localDateTime"; }
}