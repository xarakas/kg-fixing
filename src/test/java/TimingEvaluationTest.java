import org.junit.Assert;
import org.junit.Test;
import org.semanticweb.owlapi.model.*;

import java.io.File;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.stream.Collectors;

import static org.semanticweb.owlapi.apibinding.OWLManager.createOWLOntologyManager;

public class TimingEvaluationTest {
    @Test
    public void given100classes_whenCheckingSatisfiability_thenMeasureTotalTime() throws OWLOntologyCreationException {
        // Given
        OWLOntologyManager man;
        man = createOWLOntologyManager();
        OWLOntology o = null;
        try {
            o = man.loadOntologyFromOntologyDocument(new File("example-KG/pizza.owl"));
//            o = man.loadOntologyFromOntologyDocument(new File("example-KG/ontology--DEV_type=parsed.nt"));
        } catch (OWLOntologyCreationException e) {
            e.printStackTrace();
            Assert.assertEquals(1,0);// Fail if error
        }
        // When
        OntBreakdown my_ont = new OntBreakdown(o, true);
        String actual_expl = "";
        long startTime = System.nanoTime();
        actual_expl = my_ont.explainInconsistencySatisfiable();
        long endTime = System.nanoTime();
//        System.out.println(actual_expl);
//        System.out.println("ExplainIncSatisf: " + (endTime-startTime)/1000000);
        String actual_expl2 = "";
        long startTime2 = System.nanoTime();
        actual_expl2 = my_ont.explainInconsistencySatisfiable2();
        long endTime2 = System.nanoTime();
//        System.out.println(actual_expl2);
//        System.out.println("Unsat Classes: " + (endTime2-startTime2)/1000000);


        // Then
        Assert.assertNotSame((endTime-startTime)/1000000, (endTime2-startTime2)/1000000);
        Assert.assertEquals(actual_expl.length(), actual_expl2.length());
    }



    @Test
    public void givenIndividuals_whenExtractingModules_thenMeasureTotalTime() throws OWLOntologyCreationException {
        // Given
        OWLOntologyManager man;
        man = createOWLOntologyManager();
        OWLOntology o = null;
        try {
            o = man.loadOntologyFromOntologyDocument(new File("example-KG/MSA-HSN.owl"));
//            o = man.loadOntologyFromOntologyDocument(new File("example-KG/ontology--DEV_type=parsed.nt"));
        } catch (OWLOntologyCreationException e) {
            e.printStackTrace();
            Assert.assertEquals(1,0);// Fail if error
        }
        // When
        OntBreakdown my_ont = new OntBreakdown(o, false);
        LinkedList<Integer> lengths = new LinkedList<>();
        List<OWLNamedIndividual> individuals = my_ont.getOntologyIndividuals().collect(Collectors.toList());
        long startTime = System.nanoTime();
        individuals.forEach(p -> {
            IRI term = p.getIRI();
            try {
                OWLOntology a = my_ont.getABoxTermModule(term);
                lengths.add(a.getAxiomCount());
            } catch (OWLOntologyCreationException e) {
                e.printStackTrace();
                Assert.assertEquals(1,0);// Fail if error
            }
        });
        long endTime = System.nanoTime();
        long timeA = (endTime-startTime)/1000000;
        System.out.println("GetModule: " + timeA);
        LinkedList<Integer> lengths2 = new LinkedList<>();
        individuals = my_ont.getOntologyIndividuals().collect(Collectors.toList());
        startTime = System.nanoTime();
        individuals.forEach(p -> {
            IRI term = p.getIRI();
            try {
                OWLOntology a = my_ont.getABoxTermModule2(term);
                lengths2.add(a.getAxiomCount());
            } catch (OWLOntologyCreationException e) {
                e.printStackTrace();
                Assert.assertEquals(1,0);// Fail if error
            }
        });
        endTime = System.nanoTime();
        long timeB = (endTime-startTime)/1000000;
        System.out.println("GetModule2: " + timeB);

        // Then
        Assert.assertNotSame(timeA, timeB);
        Assert.assertEquals(lengths, lengths2);
    }
}
