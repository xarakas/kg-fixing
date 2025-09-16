package org.nikolasparaskakis.core;



import org.semanticweb.owlapi.model.OWLAxiom;
import java.util.Objects;



/**
 * Class representing an OWLAxiom and a Position in that axiom.
 * Used to represent where a new axiom should be inserted in an existing axiom.
 */
@SuppressWarnings({"unused", "DuplicatedCode"})
public class AxiomPosition {

    /**
     * The OWLAxiom
     */
    private OWLAxiom axiom = null;

    /**
     * The Position in the axiom
     */
    private Position position = null;

    /**
     * Constructor of Class AxiomPosition
     * @param axiom        The OWLAxiom to be adopted (i.e. inserted)
     * @param position     The Position in the axiom
     */
    public AxiomPosition(OWLAxiom axiom, Position position){
        this.setAxiom(axiom);
        this.setPosition(position);
    }

    /**
     * Gets the OWLAxiom
     * @return OWLAxiom
     */
    public OWLAxiom getAxiom() {
        return axiom;
    }

    /**
     * Sets the OWLAxiom
     * @param axiom the OWLAxiom to set for this AxiomPosition
     */
    public void setAxiom(OWLAxiom axiom) {
        this.axiom = axiom;
    }

    /**
     * Gets the Position within the axiom
     * @return Position within the axiom
     */
    public Position getPosition() {
        return position;
    }

    /**
     * Sets the Position in the axiom.
     * Updates the position field of this AxiomPosition instance with the provided value.
     * @param position the Position to set for this AxiomPosition
     */
    public void setPosition(Position position) {
        this.position = position;
    }

    /**
     * Indicates whether some other object is "equal to" this one.
     * Two AxiomPosition objects are considered equal if both their
     * axiom and position fields are equal.
     *
     * @param o The object to be compared with this AxiomPosition
     * @return  True if the two AxiomPosition objects are equal, false otherwise
     */
    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null)
            return false;
        if (getClass() != o.getClass())
            return false;
        AxiomPosition f = (AxiomPosition) o;
        return Objects.equals(this.axiom, f.getAxiom())
                && Objects.equals(this.position, f.getPosition());
    }

    /**
     * Returns a hash code value for this AxiomPosition.
     * The hash code is computed based on the axiom and position fields,
     * ensuring consistency with the "equals" method.
     * @return the hash code value for this AxiomPosition
     */
    @Override
    public int hashCode() {
        return Objects.hash(axiom, position);
    }

    /**
     * String representation of the AxiomPosition
     * @return String representation of the AxiomPosition
     */
    public String toString(){
        return "Position: " + position + " \t of axiom: " + axiom;
    }
}