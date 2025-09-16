package org.nikolasparaskakis.core;



import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.nikolasparaskakis.utils.OWLFunctionalSyntaxParser;
import org.semanticweb.owlapi.model.OWLAxiom;
import java.util.HashMap;
import java.util.HashSet;



/**
 * Class representing a tracker of all the positions of the Knowledge Graph that are immutable. That is the accumulated ground-truth indicated by the fixing process so far.
 * This PositionTracker provides the functionality of storing immutable or partly immutable axioms (i.e. axioms having only a specific position as immutable).
 * The PositionTracker also provides the functionality checking the immutability of specific axioms or positions.
 */
@SuppressWarnings({"unused", "DuplicatedCode"})
public class PositionTracker {

    /**
     * All OWLAxioms with only immutable Positions (no mutable Positions at all).
     */
    private final HashSet<OWLAxiom> immutableAxioms;

    /**
     * All OWLAxioms with some immutable and some mutable Positions
     * The Boolean represents the immutable Position: true -> immutable SUBJECT, false -> immutable OBJECT (OWLAxioms with a SINGLE Position can't be partly immutable)
     */
    private final HashMap<OWLAxiom, Boolean> partlyImmutableAxioms;

    /**
     * Default constructor of an empty PositionTracker.
     */
    public PositionTracker() {
        this.immutableAxioms = new HashSet<>();
        this.partlyImmutableAxioms = new HashMap<>();
    }

    /**
     * Copy constructor of a new PositionTracker based on an existing one.
     * @param pt    the old PositionTracker to be copied
     */
    public PositionTracker(PositionTracker pt) {
        this.immutableAxioms = new HashSet<>(pt.immutableAxioms);
        this.partlyImmutableAxioms = new HashMap<>(pt.partlyImmutableAxioms);
    }

    public PositionTracker(HashSet<OWLAxiom> immutableAxioms, HashMap<OWLAxiom, Boolean> partlyImmutableAxioms) {
        this.immutableAxioms = new HashSet<>(immutableAxioms);
        this.partlyImmutableAxioms = new HashMap<>(partlyImmutableAxioms);
    }

    /**
     * Add an OWLAxiom as (fully) immutable (i.e. no Position in this axiom is allowed to be muted)
     * @param ax    the OWLAxiom to be stored as immutable
     */
    public void addImmutableAxiom(OWLAxiom ax) {
        this.immutableAxioms.add(ax);
    }

    /**
     * Add an OWLAxiom positions as immutable
     * @param ax                    the OWLAxiom of this position
     * @param immutablePosition     the Position to be stored as immutable in the OWLAxiom ax
     */
    public void addImmutablePosition(OWLAxiom ax, Position immutablePosition) {
        if(immutablePosition == Position.SINGLE) {
            addImmutableAxiom(ax);
        } else {
            // If it is not an immutable axiom
            if (!this.immutableAxioms.contains(ax)) {
                boolean subjectImmutable = immutablePosition == Position.SUBJECT; // True for immutable subject, False for immutable object.
                // If it is a new axiom for this tracker
                if (!this.partlyImmutableAxioms.containsKey(ax)) {
                    this.partlyImmutableAxioms.put(ax, subjectImmutable);
                } else { // One position of this axiom is already immutable
                    boolean knownSubjectImmutable = this.partlyImmutableAxioms.get(ax);
                    // If we are adding an immutable position other than the one we already know
                    if (knownSubjectImmutable ^ subjectImmutable) {
                        // Both positions are actually immutable, add as immutable axiom\
                        this.immutableAxioms.add(ax);
                        this.partlyImmutableAxioms.remove(ax);
                    } // Else, we already know that this position is immutable. No need for any action.
                }
            } // Else, this is already immutable axiom. No need to add an immutable position.
        }
    }

    /**
     * Check whether this OWLAxiom is fully immutable
     * @param ax    The OWLAxiom to be checked for immutability
     * @return      True if ax has no mutable position, False if at least one position is mutable
     */
    public boolean isImmutable(OWLAxiom ax) {
        return this.immutableAxioms.contains(ax);
    }

    /**
     * Check whether this AxiomPosition is immutable
     * @param ap    The AxiomPosition to be checked for immutability
     * @return      True if AxiomPosition ap is Immutable
     */
    public boolean isImmutable(AxiomPosition ap) {
        OWLAxiom ax = ap.getAxiom();
        Position p = ap.getPosition();
        boolean positionImmutable = isImmutable(ax);
        if(!positionImmutable) {// not fully immutable axiom
            // Check for specific position immutability
            if(!p.equals(Position.SINGLE) && isPartlyImmutable(ax)){
                if(p.equals(Position.SUBJECT)){
                    positionImmutable = hasImmutableSubject(ax);
                } else if (p.equals(Position.OBJECT)){
                    positionImmutable = hasImmutableObject(ax);
                }
            } // else leave positionImmutable = False
        } // else leave positionImmutable = False
        return positionImmutable;
    }

    /**
     * Check whether this OWLAxiom is partly immutable
     * @param ax    the OWLAxiom to be checked for partial immutability
     * @return      True if OWLAxiom ax has no mutable Position, False if at least one Position in OWLAxiom ax is mutable
     */
    public boolean isPartlyImmutable(OWLAxiom ax) {
        return this.partlyImmutableAxioms.containsKey(ax);
    }

    /**
     * Check whether the subject of an axiom is immutable
     * @param ax    the OWLAxiom to be checked for immutable subject
     * @return      true if the OWLAxiom ax is fully immutable or its SUBJECT Position is immutable
     */
    public boolean hasImmutableSubject(OWLAxiom ax) {
        boolean response = false;
        if(isImmutable(ax)){
            response = true;
        } else if(isPartlyImmutable(ax)){
            response = this.partlyImmutableAxioms.get(ax);
        }
        return response;
    }

    /**
     * Check whether the object of an axiom is immutable
     * @param ax    the OWLAxiom to be checked for immutable subject
     * @return      true if the OWLAxiom ax is fully immutable or its OBJECT Position is immutable
     */
    public boolean hasImmutableObject(OWLAxiom ax) {
        boolean response = false;
        if(isImmutable(ax)){
            response = true;
        } else if(isPartlyImmutable(ax)){
            response = !this.partlyImmutableAxioms.get(ax);
        }
        return response;
    }

    /**
     * String representation of this PositionTracker
     * @return  the String representation of this PositionTracker
     */
    public String toString() {
        return "Immutable Position Tracker: " +
                "\n\t Immutable axioms: " + this.immutableAxioms.size() +
                "\n\t\t "+ this.immutableAxioms +
                "\n\t Partly Immutable axioms: " + this.partlyImmutableAxioms.size() +
                "\n\t\t "+ this.partlyImmutableAxioms;
    }

    /**
     * Serialize to a JSON node
     * @return the JSON node representing this PositionTracker
     */
    public JsonNode toJsonNode() {
        // Create the main JSON object
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode json = mapper.createObjectNode();

        // Immutable axioms array
        ArrayNode immutableArray = mapper.createArrayNode();
        for (OWLAxiom ax : this.immutableAxioms) {
            immutableArray.add(ax.toString());
        }

        // Partly immutable axioms array
        ArrayNode partlyImmutableArray = mapper.createArrayNode();
        for (OWLAxiom ax : this.partlyImmutableAxioms.keySet()) {
            ObjectNode partlyImmutableObject = mapper.createObjectNode();
            partlyImmutableObject.put("axiom", ax.toString());
            partlyImmutableObject.put("boolean", this.partlyImmutableAxioms.get(ax));
            partlyImmutableArray.add(partlyImmutableObject);
        }

        // Set arrays in the main JSON object
        json.set("immutableAxioms", immutableArray);
        json.set("partlyImmutableAxioms", partlyImmutableArray);

        // Return the JSON representation
        return json;
    }

    /**
     * Serialize to a JSON string
     * @return  the JSON string representing this PositionTracker
     * @throws RuntimeException if there is an error during serialization
     */
    public String toJsonString() {
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(toJsonNode());
        } catch (Exception e) {
            throw new RuntimeException("Error serializing PositionTracker", e);
        }
    }

    /**
     * Parse from a JSON node
     * @param root  the JSON node representing a PositionTracker
     * @return      the PositionTracker object parsed from the JSON node
     */
    public static PositionTracker fromJsonNode(JsonNode root) {
        // Initialize storage for parsed axioms
        HashSet<OWLAxiom> immutableAxioms = new HashSet<>();
        HashMap<OWLAxiom, Boolean> partlyImmutableAxioms = new HashMap<>();

        // Parse immutable axioms
        JsonNode immutableArray = root.get("immutableAxioms");
        if (immutableArray != null && immutableArray.isArray()) {
            for (JsonNode axNode : immutableArray) {
                OWLFunctionalSyntaxParser parser = new OWLFunctionalSyntaxParser();
                OWLAxiom axiom = parser.parse(axNode.asText());
                immutableAxioms.add(axiom);
            }
        }

        // Parse partly immutable axioms
        JsonNode partlyArray = root.get("partlyImmutableAxioms");
        if (partlyArray != null && partlyArray.isArray()) {
            for (JsonNode objNode : partlyArray) {
                OWLFunctionalSyntaxParser parser = new OWLFunctionalSyntaxParser();
                OWLAxiom axiom = parser.parse(objNode.get("axiom").asText());
                boolean flag = objNode.get("boolean").asBoolean();
                partlyImmutableAxioms.put(axiom, flag);
            }
        }

        // Create and return the PositionTracker
        return new PositionTracker(immutableAxioms, partlyImmutableAxioms);
    }

    /**
     * Parse from a JSON string
     * @param json  the JSON string representing a PositionTracker
     * @return      the PositionTracker object parsed from the JSON string
     * @throws RuntimeException if there is an error during parsing
     */
    public static PositionTracker fromJsonString(String json) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(json);
            return fromJsonNode(root);
        } catch (Exception e) {
            throw new RuntimeException("Error parsing PositionTracker JSON", e);
        }
    }

    /**
     * Get the set of immutable axioms
     * @return  the HashSet of immutable OWLAxioms
     */
    public HashSet<OWLAxiom> getImmutableAxioms() {
        return this.immutableAxioms;
    }

    /**
     * Get the map of partly immutable axioms
     * @return  the HashMap of partly immutable OWLAxioms with their immutable Position (true for SUBJECT, false for OBJECT)
     */
    public HashMap<OWLAxiom, Boolean> getPartlyImmutableAxioms() {
        return this.partlyImmutableAxioms;
    }
}