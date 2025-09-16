package org.nikolasparaskakis.logs.module;



import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.time.LocalDateTime;



/**
 * Class representing a log entry for module events.
 */
@SuppressWarnings({"unused", "DuplicatedCode"})
public class MyEventLog {

    /**
     * The type of event.
     */
    private final MyEvent eventType;

    /**
     * The timestamp of the event.
     */
    private final LocalDateTime localDateTime;

    /** Constructor for ModuleEventLog.
     * @param baseIndividual The base individual associated with this log entry.
     * @param eventType The type of event.
     * @param localDateTime The timestamp of the event.
     */
    public MyEventLog(MyEvent eventType, LocalDateTime localDateTime) {
        this.eventType = eventType;
        this.localDateTime = localDateTime;
    }

    /** Converts this ModuleEventLog object to a ObjectNode.
     * @return A ObjectNode representing this ModuleEventLog object.
     */
    public ObjectNode toObjectNode() {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode objectNode = mapper.createObjectNode();
        objectNode.put("eventType", this.eventType.toString());
        objectNode.put("localDateTime", this.localDateTime != null ? this.localDateTime.toString() : null);
        return objectNode;
    }

    /** Parses an ObjectNode and returns a ModuleEventLog object.
     * @param node The ObjectNode to parse.
     * @return A ModuleEventLog object.
     */
    public static MyEventLog fromObjectNode(ObjectNode node) {
        String eventType = node.get("eventType").asText();
        String localDateTimeStr = node.get("localDateTime").asText(null);
        LocalDateTime localDateTime = localDateTimeStr != null ? LocalDateTime.parse(localDateTimeStr) : null;
        return new MyEventLog(Enum.valueOf(MyEvent.class, eventType), localDateTime);
    }

    /** Converts this ModuleEventLog object to a JSON string.
     * @return A JSON string representing this ModuleEventLog object.
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

    /** Parses a JSON string to create a ModuleEventLog instance.
     * @param json  String containing the JSON representation of the log entry.
     * @return      ModuleEventLog instance.
     * @throws RuntimeException if there is an error parsing the JSON string.
     */
    public static MyEventLog fromJsonString(String json) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            ObjectNode node = (ObjectNode) mapper.readTree(json);
            return fromObjectNode(node);
        } catch (Exception e) {
            throw new RuntimeException("Error deserializing from JSON", e);
        }
    }


    /**
     * Gets the type of event.
     * @return The type of event.
     */
    public MyEvent getEventType() {
        return this.eventType;
    }

    /**
     * Gets the variable name for the event type.
     * @return The variable name for the event type.
     */
    public String getEventTypeVarName() { return "eventType"; }

    /**
     * Gets the timestamp of the event.
     * @return The timestamp of the event.
     */
    public LocalDateTime getLocalDateTime() {
        return this.localDateTime;
    }

    /**
     * Gets the variable name for the timestamp.
     * @return The variable name for the timestamp.
     */
    public String getLocalDateTimeVarName() { return "localDateTime"; }
}