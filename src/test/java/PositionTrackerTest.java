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

import static org.semanticweb.owlapi.apibinding.OWLManager.createOWLOntologyManager;

public class PositionTrackerTest {
    OWLAxiom classAx = null;
    OWLAxiom classAxUpdatted = null;
    OWLAxiom propertyAx = null;
    PositionTracker pt = null;

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
            OWLNamedIndividualImpl newInd = new OWLNamedIndividualImpl(IRI.create(o.getOntologyID().getOntologyIRI().get().getNamespace(), "NewNamedIndividual"));
            OWLClassAssertionAxiomImpl newAxiom = new OWLClassAssertionAxiomImpl(newInd, oldAxiom.getClassExpression(), initialAxiom.getAnnotations());
            classAxUpdatted = newAxiom;
         }

        // Get an OBJECT_PROPERTY_ASSERTION axiom as well
        Optional<OWLAxiom> axiomPropertyAssert = axioms.stream().filter(x -> x.getAxiomType().equals(AxiomType.OBJECT_PROPERTY_ASSERTION)).sorted().findFirst();
        if(axiomPropertyAssert.isPresent()){
            propertyAx = axiomPropertyAssert.get();// This axiom should be the class assertion below:
//          ObjectPropertyAssertion(<http://www.w3.org/ns/sosa/madeObservation> <https://www.maritimelinkeddata.org/c-msa-hsn#NARIAISProcessingSystem> <https://www.maritimelinkeddata.org/c-msa-hsn#AISVesselPosition/228827000-1443769141>)
        } else {
            Assert.assertEquals(1,0);// No OBJECT_PROPERTY_ASSERTION axioms found in the loaded ontology
        }

        pt = new PositionTracker();

    }

    @Test
    public void addImmutableAxiom() {
        pt.addImmutableAxiom(classAx);
        Assert.assertFalse(pt.isImmutable(propertyAx));
        pt.addImmutableAxiom(propertyAx);
        Assert.assertTrue(pt.isImmutable(classAx));
        Assert.assertTrue(pt.isImmutable(propertyAx));
        Assert.assertFalse(pt.isImmutable(classAxUpdatted));
    }

    @Test
    public void addImmutablePosition() {
        Assert.assertFalse(pt.hasImmutableObject(propertyAx));
        pt.addImmutablePosition(propertyAx,Position.OBJECT);
        Assert.assertTrue(pt.hasImmutableObject(propertyAx));
        Assert.assertFalse(pt.hasImmutableSubject(propertyAx));
        pt.addImmutablePosition(propertyAx,Position.SUBJECT);
        Assert.assertTrue(pt.hasImmutableSubject(propertyAx));
    }

    @Test
    public void isImmutable() {
        Assert.assertFalse(pt.isImmutable(propertyAx));
        Assert.assertFalse(pt.isImmutable(classAx));
        pt.addImmutableAxiom(classAx);
        Assert.assertTrue(pt.isImmutable(classAx));
        pt.addImmutablePosition(propertyAx, Position.SUBJECT);
        Assert.assertFalse(pt.isImmutable(propertyAx));
        pt.addImmutablePosition(propertyAx, Position.OBJECT);
        Assert.assertTrue(pt.isImmutable(propertyAx));
    }

    @Test
    public void isPartlyImmutable() {
        //Class axioms can;t be partly immutables
        Assert.assertFalse(pt.isPartlyImmutable(classAx));
        pt.addImmutableAxiom(classAx);
        Assert.assertFalse(pt.isPartlyImmutable(classAx));
        //Property axioms should be partly immutable when only one position is mutable
        Assert.assertFalse(pt.isPartlyImmutable(propertyAx));
        pt.addImmutablePosition(propertyAx, Position.SUBJECT);
        Assert.assertTrue(pt.isPartlyImmutable(propertyAx));
        pt.addImmutablePosition(propertyAx, Position.OBJECT);
        Assert.assertFalse(pt.isPartlyImmutable(propertyAx));
    }

    @Test
    public void hasImmutableSubject() {
        Assert.assertFalse(pt.hasImmutableSubject(propertyAx));
        pt.addImmutablePosition(propertyAx, Position.OBJECT);
        Assert.assertFalse(pt.hasImmutableSubject(propertyAx));
        pt.addImmutablePosition(propertyAx, Position.SUBJECT);
        Assert.assertTrue(pt.hasImmutableSubject(propertyAx));
    }

    @Test
    public void hasImmutableObject() {
        Assert.assertFalse(pt.hasImmutableObject(propertyAx));
        pt.addImmutablePosition(propertyAx, Position.SUBJECT);
        Assert.assertFalse(pt.hasImmutableObject(propertyAx));
        pt.addImmutablePosition(propertyAx, Position.OBJECT);
        Assert.assertTrue(pt.hasImmutableObject(propertyAx));
    }

    @Test
    public void testToString() {
        pt.addImmutableAxiom(classAx);
        pt.addImmutablePosition(propertyAx, Position.OBJECT);
        String expected = "Immutable Position Tracker: \n" +
                "\t Immutable axioms: 1\n" +
                "\t\t [ClassAssertion(<http://www.ontology-of-units-of-measure.org/resource/om-2/Quantity> <http://www.ontology-of-units-of-measure.org/resource/om-2/length>)]\n" +
                "\t Partly Immutable axioms: 1\n" +
                "\t\t {ObjectPropertyAssertion(<http://www.w3.org/ns/sosa/madeObservation> <https://www.maritimelinkeddata.org/c-msa-hsn#NARIAISProcessingSystem> <https://www.maritimelinkeddata.org/c-msa-hsn#AISVesselPosition/228015700-1444072938>)=false}";
        Assert.assertEquals(expected,pt.toString()); //Not implemented yet
    }
}