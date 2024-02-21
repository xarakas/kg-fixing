import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.model.parameters.Imports;
import uk.ac.manchester.cs.owl.owlapi.OWLClassAssertionAxiomImpl;
import uk.ac.manchester.cs.owl.owlapi.OWLNamedIndividualImpl;

import java.io.File;
import java.util.Optional;
import java.util.Set;

import static org.junit.Assert.*;
import static org.semanticweb.owlapi.apibinding.OWLManager.createOWLOntologyManager;

public class FixTest {
    OWLAxiom oldA = null;
    OWLAxiom newA = null;
    Fix f = null;
    Fix fEqual = null;
    Fix fNonEqual = null;

    @Before
    public void setUp() throws Exception {
        // Given
        OWLOntologyManager man;
        man = createOWLOntologyManager();
        OWLOntology o = null;
        try {
            o = man.loadOntologyFromOntologyDocument(new File("example-KG/MSA-HSN.owl"));
        } catch (OWLOntologyCreationException e) {
            e.printStackTrace();
            Assert.assertEquals(1,0);// Fail if error reading the file
        }
        // When
        Set<OWLAxiom> axioms  = o.getABoxAxioms(Imports.INCLUDED);
        Optional<OWLAxiom> oa = axioms.stream().sorted().findFirst();
        OWLAxiom initialAxiom = null;
        if(oa.isPresent()){
            initialAxiom = oa.get();
        } else {
            Assert.assertEquals(1,0);// No axioms found in the loaded ontology
        }
        if(!initialAxiom.getAxiomType().equals(AxiomType.CLASS_ASSERTION)){
            Assert.assertEquals(1,0);// Wrong axiom, thi should be the class assertion below:
//            ClassAssertion(<http://www.ontology-of-units-of-measure.org/resource/om-2/Quantity> <http://www.ontology-of-units-of-measure.org/resource/om-2/length>)
        } else {
            OWLClassAssertionAxiomImpl oldAxiom  = (OWLClassAssertionAxiomImpl) initialAxiom;
            oldA = oldAxiom;
            OWLNamedIndividualImpl newInd = new OWLNamedIndividualImpl(IRI.create(o.getOntologyID().getOntologyIRI().get().getNamespace(), "NewNamedIndividual"));
            OWLClassAssertionAxiomImpl newAxiom = new OWLClassAssertionAxiomImpl(newInd, oldAxiom.getClassExpression(), initialAxiom.getAnnotations());
            newA = newAxiom;
            f = new Fix(oldAxiom, newAxiom, Position.SINGLE);
            fEqual = new Fix(oldAxiom, newAxiom, Position.SINGLE);
            OWLNamedIndividualImpl newInd2 = new OWLNamedIndividualImpl(IRI.create(o.getOntologyID().getOntologyIRI().get().getNamespace(), "NewNamedIndividual2"));
            OWLClassAssertionAxiomImpl newAxiom2 = new OWLClassAssertionAxiomImpl(newInd2, oldAxiom.getClassExpression(), initialAxiom.getAnnotations());
            fNonEqual = new Fix(oldAxiom, newAxiom2, Position.SINGLE);
        }
//        System.out.println(f);
    }

    @Test
    public void testEquals() {
        Assert.assertTrue(f.equals(fEqual) && fEqual.equals(f));// f is equal to fEqual
        Assert.assertFalse(f.equals(fNonEqual));// f is not equal to fNonEqual
        Assert.assertFalse(fNonEqual.equals(f));// f is not equal to fNonEqual
    }

    @Test
    public void testHashCode() {
        Assert.assertEquals(f.hashCode(),fEqual.hashCode());
        Assert.assertTrue(f.hashCode() != fNonEqual.hashCode());
    }

    @Test
    public void getMutedPosition() {
        Assert.assertEquals(Position.SINGLE,f.getMutedPosition());// non implemented
    }

    @Test
    public void setMutedPosition() {
        fNonEqual.setMutedPosition(Position.OBJECT);
        Assert.assertEquals(Position.OBJECT,fNonEqual.getMutedPosition());
    }

    @Test
    public void getOldAxiom() {
        Assert.assertEquals(oldA,f.getOldAxiom());
    }

    @Test
    public void getNewAxiom() {
        Assert.assertEquals(newA,f.getNewAxiom());
    }

    @Test
    public void setOldAxiom() {
        fNonEqual.setOldAxiom(newA);
        Assert.assertEquals(newA,fNonEqual.getOldAxiom());
    }

    @Test
    public void setNewAxiom() {
        fNonEqual.setNewAxiom(oldA);
        Assert.assertEquals(oldA,fNonEqual.getNewAxiom());
    }

    @Test
    public void isDelete() {
        Assert.assertFalse(f.isDelete());// no Delete fixes supported yet
    }

    @Test
    public void isAdd() {
        Assert.assertFalse(f.isAdd());// no Add fixes supported yet
    }

    @Test
    public void isUpdate() {
        Assert.assertTrue(f.isUpdate());// Only update fixes supproted so far
        Assert.assertTrue(fEqual.isUpdate());// Only update fixes supproted so far
        Assert.assertTrue(fNonEqual.isUpdate());// Only update fixes supproted so far
    }

    @Test
    public void testToString() {
        String expected = "Update Fix: ClassAssertion(<http://www.ontology-of-units-of-measure.org/resource/om-2/Quantity> <https://www.maritimelinkeddata.org/NewNamedIndividual>) \n" +
                " \t in place of ClassAssertion(<http://www.ontology-of-units-of-measure.org/resource/om-2/Quantity> <http://www.ontology-of-units-of-measure.org/resource/om-2/length>)\n" +
                " \t (muted position SINGLE)";
        Assert.assertEquals(expected, f.toString());// Only update fixes supproted so far
    }
}