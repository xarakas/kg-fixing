import org.semanticweb.owlapi.model.OWLAxiom;

import java.util.Objects;
import java.util.logging.Logger;

/**
 * Class representing an (Update) Fix of an old OWLAxiom with a new OWLAxiom.
 *      The oldAxiom and newAxiom are expected to have the same AxiomType and the mutedPosition should be compatible with it (e.g. Position.SINGLE for class assertions). However, this is not enforced by this class, allowing for potential future extensions to arbitrary fixes.
 *      An Add/Delete Fix can also be expressed as instance of this class by setting oldAxiom/newAxiom as null respectively. However, these types are not used by the current fixing implementation.
 */
public class Fix {

    static Logger logger = Logger.getLogger(ExtractModules.class.getName());
    private OWLAxiom oldAxiom = null;
    private OWLAxiom newAxiom = null;
    private Boolean trivial = false; // Whether this fix is a trivial one or not i.e. used a named null instance
    private Position mutedPosition = null;

    /**
     * Constructor of Class Fix
     * @param oldAxiom          The old OWLAxiom to be updated (i.e. removed)
     * @param newAxiom          The new OWLAxiom to be adopted (i.e. inserted)
     * @param mutedPosition     The Position edited in the axiom.
     */
    public Fix(OWLAxiom oldAxiom, OWLAxiom newAxiom, Position mutedPosition){
        this.oldAxiom = oldAxiom;
        this.newAxiom = newAxiom;
        this.mutedPosition = mutedPosition;
    }

    /**
     * Copy constructor for Fix objects
     * @param f     The Fix object to be coppied
     */
    public Fix(Fix f){
        this.oldAxiom = f.oldAxiom;
        this.newAxiom = f.newAxiom;
        this.trivial = f.trivial;
        this.mutedPosition = f.mutedPosition;
    }

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

    @Override
    public int hashCode() {
        return Objects.hash(oldAxiom, newAxiom, mutedPosition);
    }

    /**
     * Get the Position that is muted by this fix
     * @return  the Position that is muted by this fix (i.e. subject, object, or single)
     */
    public Position getMutedPosition(){
        return this.mutedPosition;
    }

    /**
     * Set the Position that is muted by this fix
     * @param pt the Position to be muted by this fix (i.e. subject, object, or single)
     */
    public void setMutedPosition(Position pt){
        this.mutedPosition = pt;
    }

    /**
     * Get the old OWLAxiom to be updated (i.e. removed)
     * @return  the old OWLAxiom to be updated
     */
    public OWLAxiom getOldAxiom(){
        return oldAxiom;
    }

    /**
     * Get the new OWLAxiom to be adopted (i.e. inserted)
     * @return  the new OWLAxiom to be adopted
     */
    public OWLAxiom getNewAxiom(){
        return newAxiom;
    }

    /**
     * Set the old OWLAxiom to be updated (i.e. removed)
     * @param ax    the old OWLAxiom to be updated
     */
    public void setOldAxiom(OWLAxiom ax){
        oldAxiom = ax;
    }

    /**
     * Set the new OWLAxiom to be adopted (i.e. inserted)
     * @param ax    the new OWLAxiom to be adopted
     */
    public void setNewAxiom(OWLAxiom ax){
        newAxiom = ax;
    }

    /**
     * Check whether this fix is a "Delete fix": i.e. whether the new axiom replacing the old one is null.
     * This function is not used currently, as only update fixes are supported.
     * @return True if the fix is a "Delete fix"
     */
    public boolean isDelete(){
        return (newAxiom == null);
    }

    /**
     * Check whether this fix is a "Add fix": i.e. whether the old axiom to be replaced is null.
     * This function is not used currently, as only update fixes are supported.
     * @return True if the fix is a "Add fix"
     */
    public boolean isAdd(){
        return (oldAxiom == null);
    }

    /**
     * Check whether this fix is a "Update fix": i.e. whether an existing old axiom is to be replaced by a new one.
     * @return True if the fix is a "Update fix"
     */
    public boolean isUpdate(){
        return (mutedPosition != null);
    }

    /**
     * Only Update fixes are supported.
     * @return
     */
    public String toString(){
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

    public Boolean getTrivial() {
        return trivial;
    }

    public void setTrivial(Boolean trivial) {
        this.trivial = trivial;
    }
}
