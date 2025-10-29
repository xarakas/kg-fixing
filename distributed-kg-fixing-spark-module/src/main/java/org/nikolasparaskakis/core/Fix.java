package org.nikolasparaskakis.core;



import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.nikolasparaskakis.utils.OWLFunctionalSyntaxParser;
import org.semanticweb.owlapi.model.OWLAxiom;

import java.io.Serializable;
import java.util.Objects;



/**
 * Class representing an (Update) Fix of an old OWLAxiom with a new OWLAxiom.
 *      The oldAxiom and newAxiom are expected to have the same AxiomType and the mutedPosition should be compatible with it (e.g. Position.SINGLE for class assertions). However, this is not enforced by this class, allowing for potential future extensions to arbitrary fixes.
 *      An Add/Delete Fix can also be expressed as instance of this class by setting oldAxiom/newAxiom as null respectively. However, these types are not used by the current fixing implementation.
 */
@SuppressWarnings({"unused", "DuplicatedCode"})
public class Fix {

    /**
     * The old axiom
     */
    private OWLAxiom oldAxiom;

    /**
     * The new axiom
     */
    private OWLAxiom newAxiom;

    /**
     * Whether this fix is a trivial one or not i.e. used a named null instance
     */
    private Boolean trivial = false; // Whether this fix is a trivial one or not i.e. used a named null instance

    /**
     * The Position that is muted by this fix (i.e. subject, object, or single)
     */
    private Position mutedPosition;

    /**
     * Constructor of Class Fix
     * @param oldAxiom          The old OWLAxiom to be updated (i.e. removed)
     * @param newAxiom          The new OWLAxiom to be adopted (i.e. inserted)
     * @param mutedPosition     The Position edited in the axiom.
     */
    public Fix(OWLAxiom oldAxiom, OWLAxiom newAxiom, Position mutedPosition) {
        this.oldAxiom = oldAxiom;
        this.newAxiom = newAxiom;
        this.mutedPosition = mutedPosition;
    }

    /**
     * Copy constructor for Fix objects
     * @param f     The Fix object to be copied
     */
    public Fix(Fix f) {
        this.oldAxiom = f.oldAxiom;
        this.newAxiom = f.newAxiom;
        this.trivial = f.trivial;
        this.mutedPosition = f.mutedPosition;
    }

    /**
     * Equals method for Fix objects
     * @param o     The object to be compared with this Fix
     * @return      True if the two Fix objects are equal, False otherwise
     */
    @Override
    public boolean equals(Object o) {
        // self check
        if (this == o)
            return true;
        // null check
        if (o == null)
            return false;
        // type check and cast
        if (getClass() != o.getClass())
            return false;
        Fix f = (Fix) o;
        // field comparison
        return Objects.equals(this.oldAxiom, f.getOldAxiom())
                && Objects.equals(this.newAxiom, f.getNewAxiom())
                && Objects.equals(this.mutedPosition, f.getMutedPosition());
    }

    /**
     * HashCode method for Fix objects
     * @return      The hash code of this Fix object
     */
    @Override
    public int hashCode() {
        return Objects.hash(oldAxiom, newAxiom, mutedPosition);
    }

    /**
     * Get the Position that is muted by this fix
     * @return  the Position that is muted by this fix (i.e. subject, object, or single)
     */
    public Position getMutedPosition() {
        return this.mutedPosition;
    }

    /**
     * Set the Position that is muted by this fix
     * @param pt the Position to be muted by this fix (i.e. subject, object, or single)
     */
    public void setMutedPosition(Position pt) {
        this.mutedPosition = pt;
    }

    /**
     * Get the old OWLAxiom to be updated (i.e. removed)
     * @return  the old OWLAxiom to be updated
     */
    public OWLAxiom getOldAxiom() {
        return oldAxiom;
    }

    /**
     * Get the new OWLAxiom to be adopted (i.e. inserted)
     * @return  the new OWLAxiom to be adopted
     */
    public OWLAxiom getNewAxiom() {
        return newAxiom;
    }

    /**
     * Set the old OWLAxiom to be updated (i.e. removed)
     * @param ax    the old OWLAxiom to be updated
     */
    public void setOldAxiom(OWLAxiom ax) {
        oldAxiom = ax;
    }

    /**
     * Set the new OWLAxiom to be adopted (i.e. inserted)
     * @param ax    the new OWLAxiom to be adopted
     */
    public void setNewAxiom(OWLAxiom ax) {
        newAxiom = ax;
    }

    /**
     * Check whether this fix is a "Delete fix": i.e. whether the new axiom replacing the old one is null.
     * This function is not used currently, as only update fixes are supported.
     * @return True if the fix is a "Delete fix"
     */
    public boolean isDelete() {
        return (newAxiom == null);
    }

    /**
     * Check whether this fix is an "Add fix": i.e. whether the old axiom to be replaced is null.
     * This function is not used currently, as only update fixes are supported.
     * @return True if the fix is an "Add fix"
     */
    public boolean isAdd() {
        return (oldAxiom == null);
    }

    /**
     * Check whether this fix is an "Update fix": i.e. whether an existing old axiom is to be replaced by a new one.
     * @return True if the fix is an "Update fix"
     */
    public boolean isUpdate() {
        return (mutedPosition != null);
    }

    /**
     * String representation of this Fix
     * @return  the String representation of this Fix
     */
    public String toString() {

        String s = "Fix of unknown type.";
        if(isUpdate()){
            s = "Update Fix: " + newAxiom + " \n \t in place of " + oldAxiom + "\n \t (muted position "+ mutedPosition +")";
        } else if(isAdd()){
            s = "Add Fix: " + newAxiom;
        } else if(isDelete()) {
            s = "Delete Fix: " + oldAxiom;
        }
        return s;
    }

    /**
     * Convert this Fix object to a ObjectNode
     * @return The ObjectNode representing the Fix
     */
    public ObjectNode toObjectNode() {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode fixObject = mapper.createObjectNode();

        if (isUpdate()) {
            fixObject.put("typeOfFix", "UPDATE");
            fixObject.put("oldAxiom", oldAxiom.toString());
            fixObject.put("newAxiom", newAxiom.toString());
            fixObject.put("mutedPosition", mutedPosition.toString());
        } else if (isAdd()) {
            fixObject.put("typeOfFix", "ADD");
            fixObject.put("newAxiom", newAxiom.toString());
        } else if (isDelete()) {
            fixObject.put("typeOfFix", "DELETE");
            fixObject.put("oldAxiom", oldAxiom.toString());
        } else {
            fixObject.put("typeOfFix", "UNKNOWN");
        }

        return fixObject;
    }

    /**
     * Create a Fix object from a Jackson ObjectNode
     * @param json  The ObjectNode representing the Fix
     * @return      The Fix object
     * @throws RuntimeException if there is an error parsing the ObjectNode
     */
    public static Fix fromObjectNode(ObjectNode json) {
        OWLAxiom oldAxiom = null;
        OWLAxiom newAxiom = null;
        Position mutedPosition = null;

        OWLFunctionalSyntaxParser parser = new OWLFunctionalSyntaxParser();

        String type = json.get("typeOfFix").asText();

        switch (type) {
            case "UPDATE":
                if (json.has("mutedPosition")) {
                    mutedPosition = Position.valueOf(json.get("mutedPosition").asText());
                }
                if (json.has("oldAxiom")) {
                    oldAxiom = parser.parse(json.get("oldAxiom").asText());
                }
                if (json.has("newAxiom")) {
                    newAxiom = parser.parse(json.get("newAxiom").asText());
                }
                break;
            case "ADD":
                if (json.has("newAxiom")) {
                    newAxiom = parser.parse(json.get("newAxiom").asText());
                }
                break;
            case "DELETE":
                if (json.has("oldAxiom")) {
                    oldAxiom = parser.parse(json.get("oldAxiom").asText());
                }
                break;
            default:
                throw new RuntimeException("Unknown fix type: " + type);
        }

        return new Fix(oldAxiom, newAxiom, mutedPosition);
    }

    /**
     * Convert this Fix object to its JSON string representation
     * @return  The JSON string representation of the Fix
     * @throws RuntimeException if there is an error during serialization
     */
    public String toJsonString() {
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.writeValueAsString(this.toObjectNode());
        } catch (Exception e) {
            throw new RuntimeException("Error serializing Fix to JSON string", e);
        }
    }

    /**
     * Create a Fix object from its JSON string representation
     * @param jsonString    The JSON string representation of the Fix
     * @return              The Fix object
     * @throws RuntimeException if there is an error parsing the JSON string
     */
    public static Fix fromJsonString(String jsonString) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            ObjectNode node = (ObjectNode) mapper.readTree(jsonString);
            return fromObjectNode(node);
        } catch (Exception e) {
            throw new RuntimeException("Error parsing Fix from JSON string", e);
        }
    }

    /**
     * Check whether this fix is a trivial one or not i.e. used a named null instance
     * @return True if the fix is a trivial one
     */
    public Boolean getTrivial() {
        return trivial;
    }

    /**
     * Set whether this fix is a trivial one or not i.e. used a named null instance
     * @param trivial   True if the fix is a trivial one
     */
    public void setTrivial(Boolean trivial) {
        this.trivial = trivial;
    }
}
