import org.junit.Assert;
import org.junit.Test;
import org.semanticweb.owlapi.model.*;

import java.io.File;
import java.util.Enumeration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static org.semanticweb.owlapi.apibinding.OWLManager.createOWLOntologyManager;

public class RefactoringTest {
    @Test
    public void givenKG_whenExtractingModules_thenReturnEveryModule() throws OWLOntologyCreationException {
        // Given
        OWLOntologyManager man;
        man = createOWLOntologyManager();
        OWLOntology o = null;
        try {
            o = man.loadOntologyFromOntologyDocument(new File("example-KG/MSA-HSN.owl"));
//            o = man.loadOntologyFromOntologyDocument(new File("example-KG/abox_inconsistency_all-5.ttl"));
        } catch (OWLOntologyCreationException e) {
            e.printStackTrace();
            Assert.assertEquals(1,0);// Fail if error
        }

        // When
        OntBreakdown my_ont = new OntBreakdown(o,false);
        AtomicInteger i = new AtomicInteger(0);
        ConcurrentHashMap<Integer, OWLOntology> matrix = new ConcurrentHashMap<>();
        ConcurrentHashMap<OWLEntity, ConcurrentLinkedQueue<OWLAxiom>> matrix2 = my_ont.getABoxModulesConcurrentHashMap();
        System.out.println("\nNew\n");
        Enumeration en = matrix2.keys();
        while (en.hasMoreElements()) {
            System.out.println(en.nextElement());
        }
        System.out.println("\nOld\n");
        my_ont.getOntologyIndividuals().collect(Collectors.toList()).parallelStream().forEach(p -> {
            IRI term = p.getIRI();
//            try {
//                term = my_ont.findTermIRI(p);
//            } catch (Exception e) { // catch "term not found"
//                e.printStackTrace();
//                Assert.assertEquals(1,0);// Fail if error
//            }
            try {
                String temp_term;
                if (term.toString().contains("#")) {temp_term = term.toString().split("#")[term.toString().split("#").length-1].replace("/","-");}
                else {temp_term = term.toString().split("/")[term.toString().split("/").length-1];}
                IRI test = IRI.create(temp_term);
                System.out.println(test.getFragment());
                matrix.put(i.getAndIncrement(), my_ont.getABoxTermModule(term));
            } catch (OWLOntologyCreationException e) {
                e.printStackTrace();
            }
        });
        // Then
        Assert.assertEquals(o.getIndividualsInSignature().toArray().length, matrix.size() );
        Assert.assertEquals(o.getIndividualsInSignature().toArray().length, matrix2.size() );
    }
}
