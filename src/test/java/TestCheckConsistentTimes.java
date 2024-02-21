import org.junit.Assert;
import org.junit.Test;
import org.semanticweb.owlapi.model.*;
import java.io.File;
import java.util.Set;
import static org.semanticweb.owlapi.apibinding.OWLManager.createOWLOntologyManager;

public class TestCheckConsistentTimes {
    @Test
    public void givenInconsistentKG_whenCheckingForConsistency_thenCompareTimes() throws OWLOntologyCreationException {
        // Given
        OWLOntologyManager man;
        man = createOWLOntologyManager();
        OWLOntology o = null;
        try {
            o = man.loadOntologyFromOntologyDocument(new File("example-KG/MSA-HSN.owl"));
        } catch (OWLOntologyCreationException e) {
            e.printStackTrace();
            Assert.assertEquals(1,0);// Fail if error
        }

        // When
        OntBreakdown my_ont = new OntBreakdown(o,false);
        long startA = System.nanoTime();
        Set<Set<OWLAxiom>> testExplain = my_ont.getExplanations(10000,100);
        long endA = System.nanoTime();
        long startB = System.nanoTime();
        boolean testCons = my_ont.isConsistent();
        long endB = System.nanoTime();
        long explainTime = endA - startA;
        long checkTime = endB - startB;
        System.out.println("Time to get inconsistency explanations: " + explainTime);
        System.out.println("Time to just test if ontology is consistent: " + checkTime);

        boolean testTime = explainTime < checkTime;
        //Then
        Assert.assertFalse(testExplain.isEmpty());
        Assert.assertFalse(testCons);
        Assert.assertFalse(testTime);
    }
}
