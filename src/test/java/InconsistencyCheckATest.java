import org.junit.Assert;
import org.junit.Test;
import org.semanticweb.owlapi.model.*;
import uk.ac.manchester.cs.owl.owlapi.OWLDataFactoryImpl;

import java.io.File;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static org.semanticweb.owlapi.apibinding.OWLManager.createOWLOntologyManager;

public class InconsistencyCheckATest {
    @Test
    public void givenKG_whenExtractingModules_thenReturnEveryModule() throws OWLOntologyCreationException {
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
        AtomicInteger i = new AtomicInteger(0);
        ConcurrentHashMap<Integer, OWLOntology> matrix = new ConcurrentHashMap<>();
        ConcurrentHashMap<OWLEntity, ConcurrentLinkedQueue<OWLAxiom>> matrix2 = my_ont.getABoxModulesConcurrentHashMap();
        my_ont.getOntologyIndividuals().collect(Collectors.toList()).parallelStream().forEach(p -> {
            IRI term = p.getIRI();
//            try {
//                term = my_ont.findTermIRI(p);
//            } catch (Exception e) { // catch "term not found"
//                e.printStackTrace();
//                Assert.assertEquals(1,0);// Fail if error
//            }
            try {
                matrix.put(i.getAndIncrement(), my_ont.getABoxTermModule(term));
            } catch (OWLOntologyCreationException e) {
                e.printStackTrace();
            }
        });
        // Then
        Assert.assertEquals(o.getIndividualsInSignature().toArray().length, matrix.size() );
//        Assert.assertEquals(o.getIndividualsInSignature().toArray().length, matrix2.size() );
    }

    @Test
    public void givenKG_whenExtractingModules2_thenReturnEveryModule() throws OWLOntologyCreationException {
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
        AtomicInteger i = new AtomicInteger(0);
        ConcurrentHashMap<Integer, OWLOntology> matrix = new ConcurrentHashMap<>();
        ConcurrentHashMap<OWLEntity, ConcurrentLinkedQueue<OWLAxiom>> matrix2 = my_ont.getABoxModulesConcurrentHashMap();

        my_ont.getOntologyIndividuals().collect(Collectors.toList()).parallelStream().forEach(p -> {
            IRI term = p.getIRI();
//            try {
//                term = my_ont.findTermIRI(p);
//            } catch (Exception e) { // catch "term not found"
//                e.printStackTrace();
//                Assert.assertEquals(1,0);// Fail if error
//            }
            try {
                matrix.put(i.getAndIncrement(), my_ont.getABoxTermModule2(term));
            } catch (OWLOntologyCreationException e) {
                e.printStackTrace();
            }
        });
        // Then
        Assert.assertEquals(o.getIndividualsInSignature().toArray().length, matrix.size() );
//        Assert.assertEquals(o.getIndividualsInSignature().toArray().length, matrix2.size() );
    }


    @Test
    public void givenRangeInconsistentKG_whenReasoning_thenReturnInconsistencyExplanations() throws OWLOntologyCreationException {
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
        OntBreakdown ont = new OntBreakdown(o, false);
        OWLOntology test0 = ont.getABoxTermModule(IRI.create("https://www.maritimelinkeddata.org/c-msa-hsn#AISVesselPosition/228827000-1443769141"));
        OntBreakdown module = new OntBreakdown(test0, false);
        OWLOntology test1 = ont.getABoxTermModule(IRI.create("https://www.maritimelinkeddata.org/c-msa-hsn#tanker"));
        OntBreakdown module1 = new OntBreakdown(test1, false);

        // When
        String actual_expl = module.explainInconsistency();
        String expected_expl = "[DataPropertyAssertion(<http://www.w3.org/ns/sosa/resultTime> <https://www.maritimelinkeddata.org/c-msa-hsn#AISVesselPosition/228827000-1443769141> \"1443769141\"^^xsd:int), DataPropertyRange(<http://www.w3.org/ns/sosa/resultTime> xsd:dateTime)]";
        String actual_expl1 = module1.explainInconsistency();
        String expected_expl1 = "";

        // Then
        /* Found inconsistency */
        Assert.assertEquals(expected_expl,actual_expl);
        /* Found no inconsistency */
        Assert.assertEquals(expected_expl1,actual_expl1);
    }


    @Test
    public void givenRangeInconsistentKG_whenReasoning2_thenReturnInconsistencyExplanations() throws OWLOntologyCreationException {
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
        OntBreakdown ont = new OntBreakdown(o, false);
        OWLOntology test0 = ont.getABoxTermModule2(IRI.create("https://www.maritimelinkeddata.org/c-msa-hsn#AISVesselPosition/228827000-1443769141"));
        OntBreakdown module = new OntBreakdown(test0, false);
        OWLOntology test1 = ont.getABoxTermModule2(IRI.create("https://www.maritimelinkeddata.org/c-msa-hsn#tanker"));
        OntBreakdown module1 = new OntBreakdown(test1, false);

        // When
        String actual_expl = module.explainInconsistency();
        String expected_expl = "[DataPropertyAssertion(<http://www.w3.org/ns/sosa/resultTime> <https://www.maritimelinkeddata.org/c-msa-hsn#AISVesselPosition/228827000-1443769141> \"1443769141\"^^xsd:int), DataPropertyRange(<http://www.w3.org/ns/sosa/resultTime> xsd:dateTime)]";
        String actual_expl1 = module1.explainInconsistency();
        String expected_expl1 = "";

        // Then
        /* Found inconsistency */
        Assert.assertEquals(expected_expl,actual_expl);
        /* Found no inconsistency */
        Assert.assertEquals(expected_expl1,actual_expl1);
    }

    @Test
    public void givenRangeInconsistentKG_whenReasoning3_thenReturnInconsistencyExplanations() throws OWLOntologyCreationException {
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
        OWLDataFactory df = new OWLDataFactoryImpl();
        OntBreakdown ont = new OntBreakdown(o, false);
        ConcurrentHashMap<OWLEntity, ConcurrentLinkedQueue<OWLAxiom>> matrix2 = ont.getABoxModulesConcurrentHashMap();
        ConcurrentLinkedQueue<OWLAxiom> testBefore = null;
        for (OWLEntity i : matrix2.keySet()){
            if (i.toString().equals("<https://www.maritimelinkeddata.org/c-msa-hsn#AISVesselPosition/228827000-1443769141>")){
                testBefore = matrix2.get(i);
            }
        }
        //<https://www.maritimelinkeddata.org/c-msa-hsn#AISVesselPosition/228827000-1443769141>
//         = df.getOWLEntity(EntityType.NAMED_INDIVIDUAL, IRI.create("https://www.maritimelinkeddata.org/c-msa-hsn#AISVesselPosition/228827000-1443769141"));
        OWLOntologyManager man2;
        man2 = createOWLOntologyManager();
        OWLOntology test0 = man2.createOntology(testBefore.stream());
        ont.getTBoxOnly().getAxioms().forEach(test0::addAxiom);
//        OWLOntology test0 = ont.getABoxTermModule2(IRI.create("https://www.maritimelinkeddata.org/c-msa-hsn#AISVesselPosition/228827000-1443769141"));
        OntBreakdown module = new OntBreakdown(test0, false);
//        OWLOntology test1 = ont.getABoxTermModule2(IRI.create("https://www.maritimelinkeddata.org/c-msa-hsn#tanker"));
//        ConcurrentLinkedQueue<OWLAxiom> testBefore1 = matrix2.get("tanker");
        ConcurrentLinkedQueue<OWLAxiom> testBefore1 = null;
        for (OWLEntity i : matrix2.keySet()){
            if (i.toString().equals("<https://www.maritimelinkeddata.org/c-msa-hsn#tanker>")){
                testBefore1 = matrix2.get(i);
            }
        }
        OWLOntology test1 = man.createOntology(testBefore1.stream());
        OntBreakdown module1 = new OntBreakdown(test1, false);

        // When
        String actual_expl = module.explainInconsistency();
        String expected_expl = "[DataPropertyAssertion(<http://www.w3.org/ns/sosa/resultTime> <https://www.maritimelinkeddata.org/c-msa-hsn#AISVesselPosition/228827000-1443769141> \"1443769141\"^^xsd:int), DataPropertyRange(<http://www.w3.org/ns/sosa/resultTime> xsd:dateTime)]";
        String actual_expl1 = module1.explainInconsistency();
        String expected_expl1 = "";

        // Then
        /* Found inconsistency */
        Assert.assertEquals(expected_expl,actual_expl);
        /* Found no inconsistency */
        Assert.assertEquals(expected_expl1,actual_expl1);
    }

    @Test
    public void givenDisjointClassesInconsistentKG_whenReasoning_thenReturnInconsistencyExplanations() throws OWLOntologyCreationException {
        // Given
        OWLOntologyManager man;
        man = createOWLOntologyManager();
        OWLOntology o = null;
        try {
            o = man.loadOntologyFromOntologyDocument(new File("example-KG/charis-inconsistent.ttl"));
        } catch (OWLOntologyCreationException e) {
            e.printStackTrace();
            Assert.assertEquals(1,0);// Fail if error
        }
        OntBreakdown ont = new OntBreakdown(o, false);
        OWLOntology test0 = ont.getABoxTermModule(IRI.create("http://www.semanticweb.org/xarakas/ontologies/2023/2/charis-muslim#NewWorldOrder"));
        OntBreakdown module = new OntBreakdown(test0, false);
        OWLOntology test1 = ont.getABoxTermModule(IRI.create("http://www.semanticweb.org/xarakas/ontologies/2023/2/charis-muslim#Charis"));
        OntBreakdown module1 = new OntBreakdown(test1, false);

        // When
        String actual_expl = module.explainInconsistency();
        String expected_expl = "[ClassAssertion(<http://www.semanticweb.org/xarakas/ontologies/2023/2/charis-muslim#Christianity> <http://www.semanticweb.org/xarakas/ontologies/2023/2/charis-muslim#NewWorldOrder>), ClassAssertion(<http://www.semanticweb.org/xarakas/ontologies/2023/2/charis-muslim#Hinduism> <http://www.semanticweb.org/xarakas/ontologies/2023/2/charis-muslim#NewWorldOrder>), DisjointClasses(<http://www.semanticweb.org/xarakas/ontologies/2023/2/charis-muslim#Christianity> <http://www.semanticweb.org/xarakas/ontologies/2023/2/charis-muslim#Hinduism> <http://www.semanticweb.org/xarakas/ontologies/2023/2/charis-muslim#Islam>)]";
        String actual_expl1 = module1.explainInconsistency();
        String expected_expl1 = "";

        // Then
        /* Found inconsistency */
        Assert.assertEquals(expected_expl,actual_expl);
        /* Found no inconsistency */
        Assert.assertEquals(expected_expl1,actual_expl1);
    }

    @Test
    public void givenDisjointClassesInconsistentKG_whenReasoning2_thenReturnInconsistencyExplanations() throws OWLOntologyCreationException {
        // Given
        OWLOntologyManager man;
        man = createOWLOntologyManager();
        OWLOntology o = null;
        try {
            o = man.loadOntologyFromOntologyDocument(new File("example-KG/charis-inconsistent.ttl"));
        } catch (OWLOntologyCreationException e) {
            e.printStackTrace();
            Assert.assertEquals(1,0);// Fail if error
        }
        OntBreakdown ont = new OntBreakdown(o, false);
        OWLOntology test0 = ont.getABoxTermModule2(IRI.create("http://www.semanticweb.org/xarakas/ontologies/2023/2/charis-muslim#NewWorldOrder"));
//        test0.getAxioms().forEach(System.out::println);
        OntBreakdown module = new OntBreakdown(test0, false);
        OWLOntology test1 = ont.getABoxTermModule2(IRI.create("http://www.semanticweb.org/xarakas/ontologies/2023/2/charis-muslim#Charis"));
        OntBreakdown module1 = new OntBreakdown(test1, false);

        // When
        String actual_expl = module.explainInconsistency();
        String expected_expl = "[ClassAssertion(<http://www.semanticweb.org/xarakas/ontologies/2023/2/charis-muslim#Christianity> <http://www.semanticweb.org/xarakas/ontologies/2023/2/charis-muslim#NewWorldOrder>), ClassAssertion(<http://www.semanticweb.org/xarakas/ontologies/2023/2/charis-muslim#Hinduism> <http://www.semanticweb.org/xarakas/ontologies/2023/2/charis-muslim#NewWorldOrder>), DisjointClasses(<http://www.semanticweb.org/xarakas/ontologies/2023/2/charis-muslim#Christianity> <http://www.semanticweb.org/xarakas/ontologies/2023/2/charis-muslim#Hinduism> <http://www.semanticweb.org/xarakas/ontologies/2023/2/charis-muslim#Islam>)]";
        String actual_expl1 = module1.explainInconsistency();
        String expected_expl1 = "";

        // Then
        /* Found inconsistency */
        Assert.assertEquals(expected_expl,actual_expl);
        /* Found no inconsistency */
        Assert.assertEquals(expected_expl1,actual_expl1);
    }

    @Test
    public void givenDisjointClassesInconsistentKG_whenReasoning3_thenReturnInconsistencyExplanations() throws OWLOntologyCreationException {
        // Given
        OWLOntologyManager man;
        man = createOWLOntologyManager();
        OWLOntology o = null;
        try {
            o = man.loadOntologyFromOntologyDocument(new File("example-KG/charis-inconsistent.ttl"));
        } catch (OWLOntologyCreationException e) {
            e.printStackTrace();
            Assert.assertEquals(1,0);// Fail if error
        }
        OntBreakdown ont = new OntBreakdown(o, false);
//        OWLOntology test0 = ont.getABoxTermModule2(IRI.create("http://www.semanticweb.org/xarakas/ontologies/2023/2/charis-muslim#NewWorldOrder"));
//        test0.getAxioms().forEach(System.out::println);
        ConcurrentHashMap<OWLEntity, ConcurrentLinkedQueue<OWLAxiom>> matrix2 = ont.getABoxModulesConcurrentHashMap();
//        ConcurrentLinkedQueue<OWLAxiom> testBefore = matrix2.get("NewWorldOrder");
        ConcurrentLinkedQueue<OWLAxiom> testBefore = null;
        for (OWLEntity i : matrix2.keySet()){
            if (i.toString().equals("<http://www.semanticweb.org/xarakas/ontologies/2023/2/charis-muslim#NewWorldOrder>")){
                testBefore = matrix2.get(i);
            }
        }

        OWLOntologyManager man2;
        man2 = createOWLOntologyManager();
        OWLOntology test0 = man2.createOntology(testBefore.stream());
        ont.getTBoxOnly().getAxioms().forEach(test0::addAxiom);
        OntBreakdown module = new OntBreakdown(test0, false);
        OWLOntology test1 = ont.getABoxTermModule2(IRI.create("http://www.semanticweb.org/xarakas/ontologies/2023/2/charis-muslim#Charis"));
        OntBreakdown module1 = new OntBreakdown(test1, false);

        // When
        String actual_expl = module.explainInconsistency();
        String expected_expl = "[ClassAssertion(<http://www.semanticweb.org/xarakas/ontologies/2023/2/charis-muslim#Christianity> <http://www.semanticweb.org/xarakas/ontologies/2023/2/charis-muslim#NewWorldOrder>), ClassAssertion(<http://www.semanticweb.org/xarakas/ontologies/2023/2/charis-muslim#Hinduism> <http://www.semanticweb.org/xarakas/ontologies/2023/2/charis-muslim#NewWorldOrder>), DisjointClasses(<http://www.semanticweb.org/xarakas/ontologies/2023/2/charis-muslim#Christianity> <http://www.semanticweb.org/xarakas/ontologies/2023/2/charis-muslim#Hinduism> <http://www.semanticweb.org/xarakas/ontologies/2023/2/charis-muslim#Islam>)]";
        String actual_expl1 = module1.explainInconsistency();
        String expected_expl1 = "";

        // Then
        /* Found inconsistency */
        Assert.assertEquals(expected_expl,actual_expl);
        /* Found no inconsistency */
        Assert.assertEquals(expected_expl1,actual_expl1);
    }

    @Test
    public void givenTBoxInconsistentKG_whenReasoning_thenReturnInconsistencyExplanations() throws OWLOntologyCreationException {
        // Given
        OWLOntologyManager man;
        man = createOWLOntologyManager();
        OWLOntology o = null;
        try {
            o = man.loadOntologyFromOntologyDocument(new File("example-KG/pizza.owl"));
        } catch (OWLOntologyCreationException e) {
            e.printStackTrace();
            Assert.assertEquals(1,0);// Fail if error
        }
        // When
        OntBreakdown my_ont = new OntBreakdown(o, false);
        String actual_expl = "";
        List<OWLNamedIndividual> individuals = my_ont.getOntologyIndividuals().collect(Collectors.toList());
        IRI term = individuals.get(0).getIRI();
        try {
            OWLOntology a = my_ont.getABoxTermModule(term);
            OntBreakdown moduleBr = new OntBreakdown(a, false);
            actual_expl = moduleBr.explainInconsistencySatisfiable();
        } catch (OWLOntologyCreationException e) {
            e.printStackTrace();
        }
        String expected_expl = "DisjointClasses(<http://www.co-ode.org/ontologies/pizza/pizza.owl#IceCream> <http://www.co-ode.org/ontologies/pizza/pizza.owl#Pizza> <http://www.co-ode.org/ontologies/pizza/pizza.owl#PizzaBase> <http://www.co-ode.org/ontologies/pizza/pizza.owl#PizzaTopping>)ObjectPropertyDomain(<http://www.co-ode.org/ontologies/pizza/pizza.owl#hasTopping> <http://www.co-ode.org/ontologies/pizza/pizza.owl#Pizza>)SubClassOf(<http://www.co-ode.org/ontologies/pizza/pizza.owl#IceCream> ObjectSomeValuesFrom(<http://www.co-ode.org/ontologies/pizza/pizza.owl#hasTopping> <http://www.co-ode.org/ontologies/pizza/pizza.owl#FruitTopping>))DisjointClasses(<http://www.co-ode.org/ontologies/pizza/pizza.owl#CheeseTopping> <http://www.co-ode.org/ontologies/pizza/pizza.owl#FishTopping> <http://www.co-ode.org/ontologies/pizza/pizza.owl#FruitTopping> <http://www.co-ode.org/ontologies/pizza/pizza.owl#HerbSpiceTopping> <http://www.co-ode.org/ontologies/pizza/pizza.owl#MeatTopping> <http://www.co-ode.org/ontologies/pizza/pizza.owl#NutTopping> <http://www.co-ode.org/ontologies/pizza/pizza.owl#SauceTopping> <http://www.co-ode.org/ontologies/pizza/pizza.owl#VegetableTopping>)SubClassOf(<http://www.co-ode.org/ontologies/pizza/pizza.owl#CheeseyVegetableTopping> <http://www.co-ode.org/ontologies/pizza/pizza.owl#VegetableTopping>)SubClassOf(<http://www.co-ode.org/ontologies/pizza/pizza.owl#CheeseyVegetableTopping> <http://www.co-ode.org/ontologies/pizza/pizza.owl#CheeseTopping>)";
        // Then
        /* Found T-Box inconsistency */
        Assert.assertEquals(expected_expl.length(), actual_expl.length());
    }


    @Test
    public void givenTBoxInconsistentKG_whenReasoning2_thenReturnInconsistencyExplanations() throws OWLOntologyCreationException {
        // Given
        OWLOntologyManager man;
        man = createOWLOntologyManager();
        OWLOntology o = null;
        try {
            o = man.loadOntologyFromOntologyDocument(new File("example-KG/pizza.owl"));
        } catch (OWLOntologyCreationException e) {
            e.printStackTrace();
            Assert.assertEquals(1,0);// Fail if error
        }
        // When
        OntBreakdown my_ont = new OntBreakdown(o, false);
        String actual_expl = "";
        List<OWLNamedIndividual> individuals = my_ont.getOntologyIndividuals().collect(Collectors.toList());
        IRI term = individuals.get(0).getIRI();
        try {
            OWLOntology a = my_ont.getABoxTermModule2(term);
            OntBreakdown moduleBr = new OntBreakdown(a, false);
            actual_expl = moduleBr.explainInconsistencySatisfiable();
        } catch (OWLOntologyCreationException e) {
            e.printStackTrace();
        }
        String expected_expl = "DisjointClasses(<http://www.co-ode.org/ontologies/pizza/pizza.owl#IceCream> <http://www.co-ode.org/ontologies/pizza/pizza.owl#Pizza> <http://www.co-ode.org/ontologies/pizza/pizza.owl#PizzaBase> <http://www.co-ode.org/ontologies/pizza/pizza.owl#PizzaTopping>)ObjectPropertyDomain(<http://www.co-ode.org/ontologies/pizza/pizza.owl#hasTopping> <http://www.co-ode.org/ontologies/pizza/pizza.owl#Pizza>)SubClassOf(<http://www.co-ode.org/ontologies/pizza/pizza.owl#IceCream> ObjectSomeValuesFrom(<http://www.co-ode.org/ontologies/pizza/pizza.owl#hasTopping> <http://www.co-ode.org/ontologies/pizza/pizza.owl#FruitTopping>))DisjointClasses(<http://www.co-ode.org/ontologies/pizza/pizza.owl#CheeseTopping> <http://www.co-ode.org/ontologies/pizza/pizza.owl#FishTopping> <http://www.co-ode.org/ontologies/pizza/pizza.owl#FruitTopping> <http://www.co-ode.org/ontologies/pizza/pizza.owl#HerbSpiceTopping> <http://www.co-ode.org/ontologies/pizza/pizza.owl#MeatTopping> <http://www.co-ode.org/ontologies/pizza/pizza.owl#NutTopping> <http://www.co-ode.org/ontologies/pizza/pizza.owl#SauceTopping> <http://www.co-ode.org/ontologies/pizza/pizza.owl#VegetableTopping>)SubClassOf(<http://www.co-ode.org/ontologies/pizza/pizza.owl#CheeseyVegetableTopping> <http://www.co-ode.org/ontologies/pizza/pizza.owl#VegetableTopping>)SubClassOf(<http://www.co-ode.org/ontologies/pizza/pizza.owl#CheeseyVegetableTopping> <http://www.co-ode.org/ontologies/pizza/pizza.owl#CheeseTopping>)";
        // Then
        /* Found T-Box inconsistency */
        Assert.assertEquals(expected_expl.length(), actual_expl.length());
    }

    @Test
    public void givenTBoxInconsistentKG_whenReasoning3_thenReturnInconsistencyExplanations() throws OWLOntologyCreationException {
        // Given
        OWLOntologyManager man;
        man = createOWLOntologyManager();
        OWLOntology o = null;
        try {
            o = man.loadOntologyFromOntologyDocument(new File("example-KG/pizza.owl"));
        } catch (OWLOntologyCreationException e) {
            e.printStackTrace();
            Assert.assertEquals(1,0);// Fail if error
        }
        // When
        OntBreakdown my_ont = new OntBreakdown(o, false);
        String actual_expl = "";
        List<OWLNamedIndividual> individuals = my_ont.getOntologyIndividuals().collect(Collectors.toList());
        IRI term = individuals.get(0).getIRI();
        System.out.println(term);
        try {
            ConcurrentHashMap<OWLEntity, ConcurrentLinkedQueue<OWLAxiom>> matrix2 = my_ont.getABoxModulesConcurrentHashMap();
//            ConcurrentLinkedQueue<OWLAxiom> testBefore = matrix2.get(term.getFragment());
            ConcurrentLinkedQueue<OWLAxiom> testBefore = null;
            for (OWLEntity i : matrix2.keySet()){
                if (i.toString().equals("<"+term.toString()+">")){
                    testBefore = matrix2.get(i);
                }
            }
            OWLOntologyManager man2;
            man2 = createOWLOntologyManager();
            OWLOntology a = man2.createOntology(testBefore.stream());
            my_ont.getTBoxOnly().getAxioms().forEach(a::addAxiom);
//            OWLOntology a = my_ont.getABoxTermModule2(term);
            OntBreakdown moduleBr = new OntBreakdown(a, false);
            actual_expl = moduleBr.explainInconsistencySatisfiable();
        } catch (OWLOntologyCreationException e) {
            e.printStackTrace();
        }
        String expected_expl = "DisjointClasses(<http://www.co-ode.org/ontologies/pizza/pizza.owl#IceCream> <http://www.co-ode.org/ontologies/pizza/pizza.owl#Pizza> <http://www.co-ode.org/ontologies/pizza/pizza.owl#PizzaBase> <http://www.co-ode.org/ontologies/pizza/pizza.owl#PizzaTopping>)ObjectPropertyDomain(<http://www.co-ode.org/ontologies/pizza/pizza.owl#hasTopping> <http://www.co-ode.org/ontologies/pizza/pizza.owl#Pizza>)SubClassOf(<http://www.co-ode.org/ontologies/pizza/pizza.owl#IceCream> ObjectSomeValuesFrom(<http://www.co-ode.org/ontologies/pizza/pizza.owl#hasTopping> <http://www.co-ode.org/ontologies/pizza/pizza.owl#FruitTopping>))DisjointClasses(<http://www.co-ode.org/ontologies/pizza/pizza.owl#CheeseTopping> <http://www.co-ode.org/ontologies/pizza/pizza.owl#FishTopping> <http://www.co-ode.org/ontologies/pizza/pizza.owl#FruitTopping> <http://www.co-ode.org/ontologies/pizza/pizza.owl#HerbSpiceTopping> <http://www.co-ode.org/ontologies/pizza/pizza.owl#MeatTopping> <http://www.co-ode.org/ontologies/pizza/pizza.owl#NutTopping> <http://www.co-ode.org/ontologies/pizza/pizza.owl#SauceTopping> <http://www.co-ode.org/ontologies/pizza/pizza.owl#VegetableTopping>)SubClassOf(<http://www.co-ode.org/ontologies/pizza/pizza.owl#CheeseyVegetableTopping> <http://www.co-ode.org/ontologies/pizza/pizza.owl#VegetableTopping>)SubClassOf(<http://www.co-ode.org/ontologies/pizza/pizza.owl#CheeseyVegetableTopping> <http://www.co-ode.org/ontologies/pizza/pizza.owl#CheeseTopping>)";
        // Then
        /* Found T-Box inconsistency */
        Assert.assertEquals(expected_expl.length(), actual_expl.length());
    }

    @Test
    public void givenUniversalQ_whenReasoning_thenReturnInconsistencyExplanations() throws OWLOntologyCreationException {
        // Given
        OWLOntologyManager man;
        man = createOWLOntologyManager();
        OWLOntology o = null;
        try {
            o = man.loadOntologyFromOntologyDocument(new File("example-KG/abox_inc_only_one.owl"));
        } catch (OWLOntologyCreationException e) {
            e.printStackTrace();
            Assert.assertEquals(1,0);// Fail if error
        }
        // When
        String actual_expl = "";
        String actual_expl2 = "";
        AtomicReference<String> actual_expl3 = new AtomicReference<>("");
        OntBreakdown moduleBr = new OntBreakdown(o, false);
        actual_expl = moduleBr.explainInconsistencySatisfiable();
//        System.out.println("ACTUAL= "+actual_expl);
        actual_expl2 = moduleBr.explainInconsistency();
        List<OWLNamedIndividual> individuals = moduleBr.getOntologyIndividuals().collect(Collectors.toList());
        individuals.forEach(p -> {
            OntBreakdown temp = moduleBr;
            IRI term = p.getIRI();
            try {
                OWLOntology a = temp.getABoxTermModule(term);
                OntBreakdown moduleBr2 = new OntBreakdown(a, false);
                actual_expl3.set(moduleBr2.explainInconsistency());

//                logger.info("explainInconsistency() required " + (endTime1 - startTime1)/1000000 + " milliseconds.");

            } catch (Exception e) {
                e.printStackTrace();
                Assert.assertEquals(1,0);// Fail if error
            }
        });
        String expected_expl = "EquivalentClasses(<http://www.semanticweb.org/xarakas/ontologies/2023/4/untitled-ontology-32#childoffamily> ObjectAllValuesFrom(<http://www.semanticweb.org/xarakas/ontologies/2023/4/untitled-ontology-32#hasParent> <http://www.semanticweb.org/xarakas/ontologies/2023/4/untitled-ontology-32#parentoffamily>))ClassAssertion(<http://www.semanticweb.org/xarakas/ontologies/2023/4/untitled-ontology-32#childoffamily> <http://www.semanticweb.org/xarakas/ontologies/2023/4/untitled-ontology-32#childB>)ObjectPropertyAssertion(<http://www.semanticweb.org/xarakas/ontologies/2023/4/untitled-ontology-32#hasParent> <http://www.semanticweb.org/xarakas/ontologies/2023/4/untitled-ontology-32#childB> <http://www.semanticweb.org/xarakas/ontologies/2023/4/untitled-ontology-32#dad>)DisjointClasses(<http://www.semanticweb.org/xarakas/ontologies/2023/4/untitled-ontology-32#minor> <http://www.semanticweb.org/xarakas/ontologies/2023/4/untitled-ontology-32#parentoffamily>)ClassAssertion(<http://www.semanticweb.org/xarakas/ontologies/2023/4/untitled-ontology-32#minor> <http://www.semanticweb.org/xarakas/ontologies/2023/4/untitled-ontology-32#dad>)EquivalentClasses(<http://www.semanticweb.org/xarakas/ontologies/2023/4/untitled-ontology-32#childoffamily> ObjectAllValuesFrom(<http://www.semanticweb.org/xarakas/ontologies/2023/4/untitled-ontology-32#hasParent> <http://www.semanticweb.org/xarakas/ontologies/2023/4/untitled-ontology-32#parentoffamily>))ClassAssertion(<http://www.semanticweb.org/xarakas/ontologies/2023/4/untitled-ontology-32#childoffamily> <http://www.semanticweb.org/xarakas/ontologies/2023/4/untitled-ontology-32#childA>)DisjointClasses(<http://www.semanticweb.org/xarakas/ontologies/2023/4/untitled-ontology-32#minor> <http://www.semanticweb.org/xarakas/ontologies/2023/4/untitled-ontology-32#parentoffamily>)ObjectPropertyAssertion(<http://www.semanticweb.org/xarakas/ontologies/2023/4/untitled-ontology-32#hasParent> <http://www.semanticweb.org/xarakas/ontologies/2023/4/untitled-ontology-32#childA> <http://www.semanticweb.org/xarakas/ontologies/2023/4/untitled-ontology-32#dad>)ClassAssertion(<http://www.semanticweb.org/xarakas/ontologies/2023/4/untitled-ontology-32#minor> <http://www.semanticweb.org/xarakas/ontologies/2023/4/untitled-ontology-32#dad>)";
        String expected_expl2 = "[EquivalentClasses(<http://www.semanticweb.org/xarakas/ontologies/2023/4/untitled-ontology-32#childoffamily> ObjectAllValuesFrom(<http://www.semanticweb.org/xarakas/ontologies/2023/4/untitled-ontology-32#hasParent> <http://www.semanticweb.org/xarakas/ontologies/2023/4/untitled-ontology-32#parentoffamily>)), ClassAssertion(<http://www.semanticweb.org/xarakas/ontologies/2023/4/untitled-ontology-32#childoffamily> <http://www.semanticweb.org/xarakas/ontologies/2023/4/untitled-ontology-32#childB>), ObjectPropertyAssertion(<http://www.semanticweb.org/xarakas/ontologies/2023/4/untitled-ontology-32#hasParent> <http://www.semanticweb.org/xarakas/ontologies/2023/4/untitled-ontology-32#childB> <http://www.semanticweb.org/xarakas/ontologies/2023/4/untitled-ontology-32#dad>), DisjointClasses(<http://www.semanticweb.org/xarakas/ontologies/2023/4/untitled-ontology-32#minor> <http://www.semanticweb.org/xarakas/ontologies/2023/4/untitled-ontology-32#parentoffamily>), ClassAssertion(<http://www.semanticweb.org/xarakas/ontologies/2023/4/untitled-ontology-32#minor> <http://www.semanticweb.org/xarakas/ontologies/2023/4/untitled-ontology-32#dad>)][EquivalentClasses(<http://www.semanticweb.org/xarakas/ontologies/2023/4/untitled-ontology-32#childoffamily> ObjectAllValuesFrom(<http://www.semanticweb.org/xarakas/ontologies/2023/4/untitled-ontology-32#hasParent> <http://www.semanticweb.org/xarakas/ontologies/2023/4/untitled-ontology-32#parentoffamily>)), ClassAssertion(<http://www.semanticweb.org/xarakas/ontologies/2023/4/untitled-ontology-32#childoffamily> <http://www.semanticweb.org/xarakas/ontologies/2023/4/untitled-ontology-32#childA>), DisjointClasses(<http://www.semanticweb.org/xarakas/ontologies/2023/4/untitled-ontology-32#minor> <http://www.semanticweb.org/xarakas/ontologies/2023/4/untitled-ontology-32#parentoffamily>), ObjectPropertyAssertion(<http://www.semanticweb.org/xarakas/ontologies/2023/4/untitled-ontology-32#hasParent> <http://www.semanticweb.org/xarakas/ontologies/2023/4/untitled-ontology-32#childA> <http://www.semanticweb.org/xarakas/ontologies/2023/4/untitled-ontology-32#dad>), ClassAssertion(<http://www.semanticweb.org/xarakas/ontologies/2023/4/untitled-ontology-32#minor> <http://www.semanticweb.org/xarakas/ontologies/2023/4/untitled-ontology-32#dad>)]";
        String expected_expl3 = "";
        // Then
        /* Inconsistency found in whole KG.... */
        Assert.assertEquals(expected_expl.length(), actual_expl.length());
        Assert.assertEquals(expected_expl2.length(), actual_expl2.length());
        /* ...but could not be found in the individual modules */
        Assert.assertEquals(expected_expl3.length(), actual_expl3.get().length());
    }


    @Test
    public void givenUniversalQ_whenReasoning2_thenReturnInconsistencyExplanations() throws OWLOntologyCreationException {
        // Given
        OWLOntologyManager man;
        man = createOWLOntologyManager();
        OWLOntology o = null;
        try {
            o = man.loadOntologyFromOntologyDocument(new File("example-KG/abox_inc_only_one.owl"));
        } catch (OWLOntologyCreationException e) {
            e.printStackTrace();
            Assert.assertEquals(1,0);// Fail if error
        }
        // When
        String actual_expl = "";
        String actual_expl2 = "";
        AtomicReference<String> actual_expl3 = new AtomicReference<>("");
        OntBreakdown moduleBr = new OntBreakdown(o, false);
        actual_expl = moduleBr.explainInconsistencySatisfiable();
//        System.out.println("ACTUAL= "+actual_expl);
        actual_expl2 = moduleBr.explainInconsistency();
        List<OWLNamedIndividual> individuals = moduleBr.getOntologyIndividuals().collect(Collectors.toList());
        individuals.forEach(p -> {
            OntBreakdown temp = moduleBr;
            IRI term = p.getIRI();
            try {
                OWLOntology a = temp.getABoxTermModule2(term);
                OntBreakdown moduleBr2 = new OntBreakdown(a, false);
                actual_expl3.set(moduleBr2.explainInconsistency());

//                logger.info("explainInconsistency() required " + (endTime1 - startTime1)/1000000 + " milliseconds.");

            } catch (Exception e) {
                e.printStackTrace();
                Assert.assertEquals(1,0);// Fail if error
            }
        });
        String expected_expl = "EquivalentClasses(<http://www.semanticweb.org/xarakas/ontologies/2023/4/untitled-ontology-32#childoffamily> ObjectAllValuesFrom(<http://www.semanticweb.org/xarakas/ontologies/2023/4/untitled-ontology-32#hasParent> <http://www.semanticweb.org/xarakas/ontologies/2023/4/untitled-ontology-32#parentoffamily>))ClassAssertion(<http://www.semanticweb.org/xarakas/ontologies/2023/4/untitled-ontology-32#childoffamily> <http://www.semanticweb.org/xarakas/ontologies/2023/4/untitled-ontology-32#childB>)ObjectPropertyAssertion(<http://www.semanticweb.org/xarakas/ontologies/2023/4/untitled-ontology-32#hasParent> <http://www.semanticweb.org/xarakas/ontologies/2023/4/untitled-ontology-32#childB> <http://www.semanticweb.org/xarakas/ontologies/2023/4/untitled-ontology-32#dad>)DisjointClasses(<http://www.semanticweb.org/xarakas/ontologies/2023/4/untitled-ontology-32#minor> <http://www.semanticweb.org/xarakas/ontologies/2023/4/untitled-ontology-32#parentoffamily>)ClassAssertion(<http://www.semanticweb.org/xarakas/ontologies/2023/4/untitled-ontology-32#minor> <http://www.semanticweb.org/xarakas/ontologies/2023/4/untitled-ontology-32#dad>)EquivalentClasses(<http://www.semanticweb.org/xarakas/ontologies/2023/4/untitled-ontology-32#childoffamily> ObjectAllValuesFrom(<http://www.semanticweb.org/xarakas/ontologies/2023/4/untitled-ontology-32#hasParent> <http://www.semanticweb.org/xarakas/ontologies/2023/4/untitled-ontology-32#parentoffamily>))ClassAssertion(<http://www.semanticweb.org/xarakas/ontologies/2023/4/untitled-ontology-32#childoffamily> <http://www.semanticweb.org/xarakas/ontologies/2023/4/untitled-ontology-32#childA>)DisjointClasses(<http://www.semanticweb.org/xarakas/ontologies/2023/4/untitled-ontology-32#minor> <http://www.semanticweb.org/xarakas/ontologies/2023/4/untitled-ontology-32#parentoffamily>)ObjectPropertyAssertion(<http://www.semanticweb.org/xarakas/ontologies/2023/4/untitled-ontology-32#hasParent> <http://www.semanticweb.org/xarakas/ontologies/2023/4/untitled-ontology-32#childA> <http://www.semanticweb.org/xarakas/ontologies/2023/4/untitled-ontology-32#dad>)ClassAssertion(<http://www.semanticweb.org/xarakas/ontologies/2023/4/untitled-ontology-32#minor> <http://www.semanticweb.org/xarakas/ontologies/2023/4/untitled-ontology-32#dad>)";
        String expected_expl2 = "[EquivalentClasses(<http://www.semanticweb.org/xarakas/ontologies/2023/4/untitled-ontology-32#childoffamily> ObjectAllValuesFrom(<http://www.semanticweb.org/xarakas/ontologies/2023/4/untitled-ontology-32#hasParent> <http://www.semanticweb.org/xarakas/ontologies/2023/4/untitled-ontology-32#parentoffamily>)), ClassAssertion(<http://www.semanticweb.org/xarakas/ontologies/2023/4/untitled-ontology-32#childoffamily> <http://www.semanticweb.org/xarakas/ontologies/2023/4/untitled-ontology-32#childB>), ObjectPropertyAssertion(<http://www.semanticweb.org/xarakas/ontologies/2023/4/untitled-ontology-32#hasParent> <http://www.semanticweb.org/xarakas/ontologies/2023/4/untitled-ontology-32#childB> <http://www.semanticweb.org/xarakas/ontologies/2023/4/untitled-ontology-32#dad>), DisjointClasses(<http://www.semanticweb.org/xarakas/ontologies/2023/4/untitled-ontology-32#minor> <http://www.semanticweb.org/xarakas/ontologies/2023/4/untitled-ontology-32#parentoffamily>), ClassAssertion(<http://www.semanticweb.org/xarakas/ontologies/2023/4/untitled-ontology-32#minor> <http://www.semanticweb.org/xarakas/ontologies/2023/4/untitled-ontology-32#dad>)][EquivalentClasses(<http://www.semanticweb.org/xarakas/ontologies/2023/4/untitled-ontology-32#childoffamily> ObjectAllValuesFrom(<http://www.semanticweb.org/xarakas/ontologies/2023/4/untitled-ontology-32#hasParent> <http://www.semanticweb.org/xarakas/ontologies/2023/4/untitled-ontology-32#parentoffamily>)), ClassAssertion(<http://www.semanticweb.org/xarakas/ontologies/2023/4/untitled-ontology-32#childoffamily> <http://www.semanticweb.org/xarakas/ontologies/2023/4/untitled-ontology-32#childA>), DisjointClasses(<http://www.semanticweb.org/xarakas/ontologies/2023/4/untitled-ontology-32#minor> <http://www.semanticweb.org/xarakas/ontologies/2023/4/untitled-ontology-32#parentoffamily>), ObjectPropertyAssertion(<http://www.semanticweb.org/xarakas/ontologies/2023/4/untitled-ontology-32#hasParent> <http://www.semanticweb.org/xarakas/ontologies/2023/4/untitled-ontology-32#childA> <http://www.semanticweb.org/xarakas/ontologies/2023/4/untitled-ontology-32#dad>), ClassAssertion(<http://www.semanticweb.org/xarakas/ontologies/2023/4/untitled-ontology-32#minor> <http://www.semanticweb.org/xarakas/ontologies/2023/4/untitled-ontology-32#dad>)]";
        String expected_expl3 = "";
        // Then
        /* Inconsistency found in whole KG.... */
        Assert.assertEquals(expected_expl.length(), actual_expl.length());
        Assert.assertEquals(expected_expl2.length(), actual_expl2.length());
        /* ...but could not be found in the individual modules */
        Assert.assertEquals(expected_expl3.length(), actual_expl3.get().length());
    }

    @Test
    public void givenUniversalQ_whenReasoning3_thenReturnInconsistencyExplanations() throws OWLOntologyCreationException {
        // Given
        OWLOntologyManager man;
        man = createOWLOntologyManager();
        OWLOntology o = null;
        try {
            o = man.loadOntologyFromOntologyDocument(new File("example-KG/abox_inc_only_one.owl"));
        } catch (OWLOntologyCreationException e) {
            e.printStackTrace();
            Assert.assertEquals(1,0);// Fail if error
        }
        // When
        String actual_expl = "";
        String actual_expl2 = "";
        AtomicReference<String> actual_expl3 = new AtomicReference<>("");
        OntBreakdown moduleBr = new OntBreakdown(o, false);
        actual_expl = moduleBr.explainInconsistencySatisfiable();
//        System.out.println("ACTUAL= "+actual_expl);
        actual_expl2 = moduleBr.explainInconsistency();
        ConcurrentHashMap<OWLEntity, ConcurrentLinkedQueue<OWLAxiom>> modules = moduleBr.getABoxModulesConcurrentHashMap();
        modules.forEach(1, (k,v) -> {
                    OWLOntologyManager man_t = createOWLOntologyManager();
                    IRI new_iri = null;
                    try {
//            String temp_term = "Uninitialized";
//            if (term.toString().contains("#")) {temp_term = term.toString().split("#")[term.toString().split("#").length-1].replace("/","-");}
//            else {temp_term = term.toString().split("/")[term.toString().split("/").length-1];}
//                String temp_term = k;
                        new_iri = IRI.create(moduleBr.getOntology().getOntologyID().getOntologyIRI().get() + "/module/" + k);
                    } catch (Exception e) {
                        new_iri = IRI.create("http://moduleOf.ont/" + k);
                    }

                    OWLOntology all_boxes = null;
                    try {
                        all_boxes = man_t.createOntology(v.stream(), new_iri);
                        moduleBr.getTBoxOnly().getAxioms().forEach(all_boxes::addAxiom);
                        OntBreakdown temp_br = new OntBreakdown(all_boxes, false);
                        actual_expl3.set(temp_br.explainInconsistency());

                    } catch (OWLOntologyCreationException e) {
                        e.printStackTrace();
                        Assert.assertEquals(1, 0);// Fail if error
                    }
                });
        String expected_expl = "EquivalentClasses(<http://www.semanticweb.org/xarakas/ontologies/2023/4/untitled-ontology-32#childoffamily> ObjectAllValuesFrom(<http://www.semanticweb.org/xarakas/ontologies/2023/4/untitled-ontology-32#hasParent> <http://www.semanticweb.org/xarakas/ontologies/2023/4/untitled-ontology-32#parentoffamily>))ClassAssertion(<http://www.semanticweb.org/xarakas/ontologies/2023/4/untitled-ontology-32#childoffamily> <http://www.semanticweb.org/xarakas/ontologies/2023/4/untitled-ontology-32#childB>)ObjectPropertyAssertion(<http://www.semanticweb.org/xarakas/ontologies/2023/4/untitled-ontology-32#hasParent> <http://www.semanticweb.org/xarakas/ontologies/2023/4/untitled-ontology-32#childB> <http://www.semanticweb.org/xarakas/ontologies/2023/4/untitled-ontology-32#dad>)DisjointClasses(<http://www.semanticweb.org/xarakas/ontologies/2023/4/untitled-ontology-32#minor> <http://www.semanticweb.org/xarakas/ontologies/2023/4/untitled-ontology-32#parentoffamily>)ClassAssertion(<http://www.semanticweb.org/xarakas/ontologies/2023/4/untitled-ontology-32#minor> <http://www.semanticweb.org/xarakas/ontologies/2023/4/untitled-ontology-32#dad>)EquivalentClasses(<http://www.semanticweb.org/xarakas/ontologies/2023/4/untitled-ontology-32#childoffamily> ObjectAllValuesFrom(<http://www.semanticweb.org/xarakas/ontologies/2023/4/untitled-ontology-32#hasParent> <http://www.semanticweb.org/xarakas/ontologies/2023/4/untitled-ontology-32#parentoffamily>))ClassAssertion(<http://www.semanticweb.org/xarakas/ontologies/2023/4/untitled-ontology-32#childoffamily> <http://www.semanticweb.org/xarakas/ontologies/2023/4/untitled-ontology-32#childA>)DisjointClasses(<http://www.semanticweb.org/xarakas/ontologies/2023/4/untitled-ontology-32#minor> <http://www.semanticweb.org/xarakas/ontologies/2023/4/untitled-ontology-32#parentoffamily>)ObjectPropertyAssertion(<http://www.semanticweb.org/xarakas/ontologies/2023/4/untitled-ontology-32#hasParent> <http://www.semanticweb.org/xarakas/ontologies/2023/4/untitled-ontology-32#childA> <http://www.semanticweb.org/xarakas/ontologies/2023/4/untitled-ontology-32#dad>)ClassAssertion(<http://www.semanticweb.org/xarakas/ontologies/2023/4/untitled-ontology-32#minor> <http://www.semanticweb.org/xarakas/ontologies/2023/4/untitled-ontology-32#dad>)";
        String expected_expl2 = "[EquivalentClasses(<http://www.semanticweb.org/xarakas/ontologies/2023/4/untitled-ontology-32#childoffamily> ObjectAllValuesFrom(<http://www.semanticweb.org/xarakas/ontologies/2023/4/untitled-ontology-32#hasParent> <http://www.semanticweb.org/xarakas/ontologies/2023/4/untitled-ontology-32#parentoffamily>)), ClassAssertion(<http://www.semanticweb.org/xarakas/ontologies/2023/4/untitled-ontology-32#childoffamily> <http://www.semanticweb.org/xarakas/ontologies/2023/4/untitled-ontology-32#childB>), ObjectPropertyAssertion(<http://www.semanticweb.org/xarakas/ontologies/2023/4/untitled-ontology-32#hasParent> <http://www.semanticweb.org/xarakas/ontologies/2023/4/untitled-ontology-32#childB> <http://www.semanticweb.org/xarakas/ontologies/2023/4/untitled-ontology-32#dad>), DisjointClasses(<http://www.semanticweb.org/xarakas/ontologies/2023/4/untitled-ontology-32#minor> <http://www.semanticweb.org/xarakas/ontologies/2023/4/untitled-ontology-32#parentoffamily>), ClassAssertion(<http://www.semanticweb.org/xarakas/ontologies/2023/4/untitled-ontology-32#minor> <http://www.semanticweb.org/xarakas/ontologies/2023/4/untitled-ontology-32#dad>)][EquivalentClasses(<http://www.semanticweb.org/xarakas/ontologies/2023/4/untitled-ontology-32#childoffamily> ObjectAllValuesFrom(<http://www.semanticweb.org/xarakas/ontologies/2023/4/untitled-ontology-32#hasParent> <http://www.semanticweb.org/xarakas/ontologies/2023/4/untitled-ontology-32#parentoffamily>)), ClassAssertion(<http://www.semanticweb.org/xarakas/ontologies/2023/4/untitled-ontology-32#childoffamily> <http://www.semanticweb.org/xarakas/ontologies/2023/4/untitled-ontology-32#childA>), DisjointClasses(<http://www.semanticweb.org/xarakas/ontologies/2023/4/untitled-ontology-32#minor> <http://www.semanticweb.org/xarakas/ontologies/2023/4/untitled-ontology-32#parentoffamily>), ObjectPropertyAssertion(<http://www.semanticweb.org/xarakas/ontologies/2023/4/untitled-ontology-32#hasParent> <http://www.semanticweb.org/xarakas/ontologies/2023/4/untitled-ontology-32#childA> <http://www.semanticweb.org/xarakas/ontologies/2023/4/untitled-ontology-32#dad>), ClassAssertion(<http://www.semanticweb.org/xarakas/ontologies/2023/4/untitled-ontology-32#minor> <http://www.semanticweb.org/xarakas/ontologies/2023/4/untitled-ontology-32#dad>)]";
        String expected_expl3 = "";
        // Then
        /* Inconsistency found in whole KG.... */
        Assert.assertEquals(expected_expl.length(), actual_expl.length());
        Assert.assertEquals(expected_expl2.length(), actual_expl2.length());
        /* ...but could not be found in the individual modules */
        Assert.assertEquals(expected_expl3.length(), actual_expl3.get().length());
    }

}
