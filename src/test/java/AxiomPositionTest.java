import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.model.parameters.Imports;
import uk.ac.manchester.cs.owl.owlapi.OWLClassAssertionAxiomImpl;

import java.io.File;
import java.util.Optional;
import java.util.Set;

import static org.semanticweb.owlapi.apibinding.OWLManager.createOWLOntologyManager;

public class AxiomPositionTest {
    OWLAxiom classAx = null;
    OWLAxiom propertyAx = null;
    AxiomPosition classAP = null;
    AxiomPosition propertyAP = null;
    AxiomPosition propertyAPEqual = null;
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
        Optional<OWLAxiom> axiomClassAssert = axioms.stream().sorted().findFirst();
        OWLAxiom initialAxiom = null;
        if(axiomClassAssert.isPresent()){
            initialAxiom = axiomClassAssert.get();
        } else {
            Assert.assertEquals(1,0);// No axioms found in the loaded ontology
        }
        if(!initialAxiom.getAxiomType().equals(AxiomType.CLASS_ASSERTION)){
            Assert.assertEquals(1,0);// Wrong axiom, this axiom should be the class assertion below:
//            ClassAssertion(<http://www.ontology-of-units-of-measure.org/resource/om-2/Quantity> <http://www.ontology-of-units-of-measure.org/resource/om-2/length>)
        } else {
            OWLClassAssertionAxiomImpl oldAxiom  = (OWLClassAssertionAxiomImpl) initialAxiom;
            classAx = oldAxiom;
        }

        // Get an OBJECT_PROPERTY_ASSERTION axiom as well
        Optional<OWLAxiom> axiomPropertyAssert = axioms.stream().filter(x -> x.getAxiomType().equals(AxiomType.OBJECT_PROPERTY_ASSERTION)).sorted().findFirst();
        if(axiomPropertyAssert.isPresent()){
            propertyAx = axiomPropertyAssert.get();// This axiom should be the class assertion below:
//          ObjectPropertyAssertion(<http://www.w3.org/ns/sosa/madeObservation> <https://www.maritimelinkeddata.org/c-msa-hsn#NARIAISProcessingSystem> <https://www.maritimelinkeddata.org/c-msa-hsn#AISVesselPosition/228827000-1443769141>)
        } else {
            Assert.assertEquals(1,0);// No OBJECT_PROPERTY_ASSERTION axioms found in the loaded ontology
        }

        classAP = new AxiomPosition(classAx,Position.SINGLE);
        propertyAP = new AxiomPosition(propertyAx,Position.OBJECT);
        propertyAPEqual = new AxiomPosition(propertyAx,Position.OBJECT);
    }

    @Test
    public void getAxiom() {
        Assert.assertEquals(classAx,classAP.getAxiom());
        Assert.assertEquals(propertyAx,propertyAP.getAxiom());
    }

    @Test
    public void setAxiom() {
        classAP.setAxiom(propertyAx);
        Assert.assertEquals(propertyAx,classAP.getAxiom());
    }

    @Test
    public void getPosition() {
        Assert.assertEquals(Position.SINGLE,classAP.getPosition());
        Assert.assertEquals(Position.OBJECT,propertyAP.getPosition());
        Assert.assertFalse(Position.SUBJECT.equals(propertyAP.getPosition()));
        Assert.assertFalse(Position.SINGLE.equals(propertyAP.getPosition()));
        Assert.assertFalse(Position.SUBJECT.equals(classAP.getPosition()));
        Assert.assertFalse(Position.OBJECT.equals(classAP.getPosition()));
    }

    @Test
    public void setPosition() {
        propertyAP.setPosition(Position.SUBJECT);
        Assert.assertEquals(Position.SUBJECT,propertyAP.getPosition());
        Assert.assertFalse(Position.OBJECT.equals(propertyAP.getPosition()));
        Assert.assertFalse(Position.SINGLE.equals(propertyAP.getPosition()));
    }

    @Test
    public void testEquals() {
        Assert.assertTrue(propertyAP.equals(propertyAPEqual) && propertyAPEqual.equals(propertyAP));
        Assert.assertFalse(propertyAP.equals(classAP));
        propertyAPEqual.setPosition(Position.SUBJECT);
        Assert.assertFalse(propertyAP.equals(propertyAPEqual));
    }

    @Test
    public void testHashCode() {
        Assert.assertEquals(propertyAP.hashCode(),propertyAPEqual.hashCode());
        Assert.assertTrue(propertyAP.hashCode() != classAP.hashCode());
    }

    @Test
    public void testToString() {
        String expected = "Position: OBJECT \t of axiom: ObjectPropertyAssertion(<http://www.w3.org/ns/sosa/madeObservation> <https://www.maritimelinkeddata.org/c-msa-hsn#NARIAISProcessingSystem> <https://www.maritimelinkeddata.org/c-msa-hsn#AISVesselPosition/228015700-1444072938>)";
        Assert.assertEquals(expected, propertyAP.toString());
    }
}