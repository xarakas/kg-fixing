import org.semanticweb.owlapi.model.OWLAxiom;
import java.util.Objects;

/**
 * Class representing a Position of an Abox OWLAxiom
 */
public class AxiomPosition {

    private OWLAxiom axiom = null;
    private Position position = null;

    /**
     * Constructor of Class AxiomPosition
     * @param axiom             The OWLAxiom to be adopted (i.e. inserted)
     * @param position     The Position in the axiom
     */
    public AxiomPosition(OWLAxiom axiom, Position position){
        this.setAxiom(axiom);
        this.setPosition(position);
    }

    public OWLAxiom getAxiom() {
        return axiom;
    }

    public void setAxiom(OWLAxiom axiom) {
        this.axiom = axiom;
    }

    public Position getPosition() {
        return position;
    }

    public void setPosition(Position position) {
        this.position = position;
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
        AxiomPosition f = (AxiomPosition) o;
        // field comparison
        return Objects.equals(this.axiom, f.getAxiom())
                && Objects.equals(this.position, f.getPosition());
    }

    @Override
    public int hashCode() {
        return Objects.hash(axiom, position);
    }
    public String toString(){
        String s = "Position: " + position + " \t of axiom: " + axiom;
        return s;
    }
}
