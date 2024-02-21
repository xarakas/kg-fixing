import org.semanticweb.owlapi.model.OWLAxiom;

import java.util.HashMap;
import java.util.HashSet;
import java.util.logging.Logger;

/**
 * Class representing a tracker of all the positions of the Knowledge Graph that are immutable. That is the accumulated ground-truth indicated by the fixing process so far.
 *      This PositionTracker provides the functionality of storing immutable or partly immutable axioms (i.e. axioms having only a specific position as immutable).
 *      The PositionTracker also provides the functionality checking the immutability of specific axioms or positions.
 */
public class PositionTracker {

    static Logger logger = Logger.getLogger(ExtractModules.class.getName());
    // All OWLAxioms with only immutable Positions (no mutable Positions at all)
    private HashSet<OWLAxiom> immutableAxioms;
    // All OWLAxioms with some immutable and some mutable Positions
    // The Boolean represents the immutable Position: true -> immutable SUBJECT, false -> immutable OBJECT (OWLAxioms with a SINGLE Position can't be partly immutable)
    private HashMap<OWLAxiom, Boolean> partlyImmutableAxioms;

    /**
     * Default constructor of an empty PositionTracker.
     */
    public PositionTracker(){
        // Initialize empty sets of immutable positions
        immutableAxioms = new HashSet<OWLAxiom>();
        partlyImmutableAxioms = new HashMap<OWLAxiom, Boolean>();
    }

    /**
     * Copy constructor of a new PositionTracker based on an existing one.
     * @param pt    the old PositionTracker to be copied
     */
    public PositionTracker(PositionTracker pt){
        // Initialize empty sets of immutable positions
        this.immutableAxioms = new HashSet<OWLAxiom>(pt.immutableAxioms);
        this.partlyImmutableAxioms = new HashMap<OWLAxiom, Boolean>(pt.partlyImmutableAxioms);
    }

    /**
     * Add an OWLAxiom as (fully) immutable (i.e. no Position in this axiom is allowed to be muted)
     * @param ax    the OWLAxiom to be stored as immutable
     */
    public void addImmutableAxiom(OWLAxiom ax) {
        immutableAxioms.add(ax);
    }

    /**
     * Add an OWLAxiom positions as immutable
     * @param ax                    the OWLAxiom of this position
     * @param immutablePosition     the Position to be stored as immutable in the OWLAxiom ax
     */
    public void addImmutablePosition(OWLAxiom ax, Position immutablePosition) {
        if(immutablePosition == Position.SINGLE){
            addImmutableAxiom(ax);
        } else {
            // If it is not an immutable axiom
            if (!immutableAxioms.contains(ax)) {
                Boolean subjectImmutable = false; // True for immutable subject, False for immutable object.
                if(immutablePosition == Position.SUBJECT){
                    subjectImmutable = true;
                }
                // If it a new axiom for this tracker
                if (!partlyImmutableAxioms.containsKey(ax)) {
                    partlyImmutableAxioms.put(ax, subjectImmutable);
                } else { // One position of this axiom is already immutable
                    boolean knownSubjectImmutable = partlyImmutableAxioms.get(ax);
                    // If we are adding an immutable position other than the one we already know
                    if (knownSubjectImmutable ^ subjectImmutable) {
                        // Both positions are actually immutable, add as immutable axiom\
                        immutableAxioms.add(ax);
                        partlyImmutableAxioms.remove(ax);
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
    public boolean isImmutable(OWLAxiom ax){
        return immutableAxioms.contains(ax);
    }

    /**
     * Check whether this AxiomPosition is immutable
     * @param ap    The AxiomPosition to be checked for immutability
     * @return      True if AxiomPosition ap is Immutable
     */
    public boolean isImmutable(AxiomPosition ap){
        OWLAxiom ax = ap.getAxiom();
        Position p = ap.getPosition();
        boolean positionImmutable = isImmutable(ax);
        if(!positionImmutable) {// not fully immutable axiom
            // Check for specific position imutability
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
    public boolean isPartlyImmutable(OWLAxiom ax){
        return partlyImmutableAxioms.containsKey(ax);
    }

    /**
     * Check whether the subject of an axiom is immutable
     * @param ax    the OWLAxiom to be checked for immutable subject
     * @return      true if the OWLAxiom ax is fully immutable or its SUBJECT Position is immutable
     */
    public boolean hasImmutableSubject(OWLAxiom ax){
        boolean response = false;
        if(isImmutable(ax)){
            response = true;
        } else if(isPartlyImmutable(ax)){
            response = partlyImmutableAxioms.get(ax);
        }
        return response;
    }

    /**
     * Check whether the object of an axiom is immutable
     * @param ax    the OWLAxiom to be checked for immutable subject
     * @return      true if the OWLAxiom ax is fully immutable or its OBJECT Position is immutable
     */
    public boolean hasImmutableObject(OWLAxiom ax){
        boolean response = false;
        if(isImmutable(ax)){
            response = true;
        } else if(isPartlyImmutable(ax)){
            response = !partlyImmutableAxioms.get(ax);
        }
        return response;
    }

    /**
     * Print all the immutable Positions stored in the Position Tracker so far.
     */
    public void printImmutables(){
        logger.info("*******  Immutable Position Tracker: " +
                "   **** Immutable axioms: " + this.immutableAxioms.size() +
                        " \n "+ this.immutableAxioms +
                "   **** Partly Immutable axioms: " + this.partlyImmutableAxioms.size() +
                        " \n "+ this.partlyImmutableAxioms
        );
    }

    public String toString(){
        String s = "Immutable Position Tracker: " +
                "\n\t Immutable axioms: " + this.immutableAxioms.size() +
                "\n\t\t "+ this.immutableAxioms +
                "\n\t Partly Immutable axioms: " + this.partlyImmutableAxioms.size() +
                "\n\t\t "+ this.partlyImmutableAxioms;
        return s;
    }
}
