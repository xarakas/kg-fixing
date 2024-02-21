import static org.semanticweb.owlapi.apibinding.OWLManager.createOWLOntologyManager;
import static org.semanticweb.owlapi.search.Searcher.annotations;

import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;

public class MEtest {

    static Logger logger = Logger.getLogger(MEtest.class.getName());


    public static void main(String[] args){
        FileHandler fh;
        try {
            // This block configures the logger with handler and formatter
            fh = new FileHandler("./MEtest.log");
            logger.addHandler(fh);
            SimpleFormatter formatter = new SimpleFormatter();
            fh.setFormatter(formatter);
        } catch (Exception e) {
            e.printStackTrace();
        }

        /* MARITIME ontology */
//        OntBreakdown my_ont = new OntBreakdown("MSA-HSN.owl");
        /* PIZZA ontology */
        OntBreakdown my_ont = new OntBreakdown(IRI.create("http://protege.stanford.edu/ontologies/pizza/pizza.owl"), false);
        /* Comic book ontology */
//        OntBreakdown my_ont = new OntBreakdown("comicbook.owl");
        /* Wine ontology */
//        OntBreakdown my_ont = new OntBreakdown("wine.owl");
        /* SHACL ontology */
//        OntBreakdown my_ont = new OntBreakdown("SHACL.owl");
        /* charis-muslim */
//        OntBreakdown my_ont = new OntBreakdown("charis-inconsistent.ttl");
//        logger.info("Loaded ontology: " + my_ont.getOntology().getSignature());

        /* // This chunk copies the whole ontology to a new one
        OWLOntologyManager man = OWLManager.createOWLOntologyManager();
        OWLDataFactory df = man.getOWLDataFactory();
        IRI new_iri = IRI.create(String.valueOf(my_ont.getOntology().getOntologyID().getOntologyIRI().get()));
        OWLOntology newOntology = man.createOntology(new_iri);
        my_ont.getOntology().classesInSignature().forEach(p->{
                OWLClass c = df.getOWLClass(p.getIRI());
                OWLAxiom clas = df.getOWLDeclarationAxiom(c);
                man.addAxiom(newOntology,clas);
                });
        my_ont.getOntology().logicalAxioms().forEach(
                newOntology::addAxiom);
        System.out.println("ORIGINAL");
        my_ont.getOntology().classesInSignature().forEach(p -> {System.out.println(p.toString());});
        System.out.println("SURROGATE");
        newOntology.classesInSignature().forEach(p -> {System.out.println(p.toString());});
        System.out.println("ORIGINAL axioms");
        my_ont.getOntology().logicalAxioms().forEach(p -> {System.out.println(p.toString());});
        System.out.println("SURROGATE axioms");
        newOntology.logicalAxioms().forEach(p -> {System.out.println(p.toString());});
         */

//////////////////////////////////////////////////
//        my_ont.printOntologySummary();
//        my_ont.printOntologyClasses();
//        my_ont.printOntologyIndividuals();
//        Stream<OWLNamedIndividual> inds =  my_ont.getOntologyIndividuals();
//        logger.info("Individuals count: " + inds.count());
//        my_ont.printOntologyObjectProperties();
//        my_ont.printOntologyABox();
//        my_ont.printOntologyTBox();
//        my_ont.printOntologyRBox();
//        my_ont.printOntologyLogicalAxioms();
/////////////////////////////////////////////////
//        AtomicInteger i = new AtomicInteger();
        /* Decompose into modules for each ABox term in original ontology */
//        my_ont.printOntologyIndividuals();
        my_ont.getOntologyIndividuals().forEach(p -> {
//            i.getAndIncrement();
//                    logger.info("\n\nNew module \n\n");
                    OWLOntology a = null;
//                    String k = p.toString().split("#")[p.toString().split("#").length-1].replace(">","");
//                    System.out.println("Module of "+k);
//                    String k = "Bay";
                    IRI term = null;
                    try {
                        term = my_ont.findTermIRI(p);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    try {
                        a = my_ont.getABoxTermModule(term);
                        OntBreakdown moduleBr = new OntBreakdown(a, false);
                        logger.info(moduleBr.getExplanations(10000, 100).toString());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

//                        moduleBr.getOntology().classesInSignature().forEach(System.out::println);
//                        moduleBr.getOntology().logicalAxioms().forEach(System.out::println);
//                    System.out.println("Inconsistency explanations");
//                    moduleBr.explainInconsistency();

                });
//            k = k.replace(">","");

//            my_ont.printCustomModuleOfOntology(term,0);
//            System.out.println("T-BOX");
//            my_ont.printOntologyTBox();
//            System.out.println("R-BOX");
//            my_ont.printOntologyRBox();
//            System.out.println("A-BOX");
//            my_ont.printOntologyABox();
//            try {
//                a = my_ont.getSyntLocModuleOfOntSignature(k);
//            } catch (OWLOntologyCreationException e) {
//                e.printStackTrace();
//            }
//            /* Log module summary and String.hashcode() */
//            OntBreakdown my_onta = new OntBreakdown(a);
////            logger.info("module signature: "+a.getSignature());
//            Stream<OWLNamedIndividual> indsa =  my_onta.getOntologyIndividuals();
////            logger.info("MODULE for "+k+": Individuals count: " + indsa.count());
//            logger.info("MODULE for "+k+": Individuals: ");
//            indsa.forEach(p->logger.info(p.toString()));
////            logger.info("MODULE for "+k+": A-Box: ");
////            my_onta.getOntologyABox().forEach(p->logger.info(p.toString()));
//////            logger.info("MODULE for "+k+": " + String.valueOf(i.get()));
////            logger.info("MODULE for "+k+": " + String.valueOf(my_onta.logOntologySummary()));
//
//
//            /* Reason on individual module */
//            try {
//                OWLOntologyManager man = OWLManager.createOWLOntologyManager();
//                OWLDataFactory df = man.getOWLDataFactory();
//                OWLReasonerFactory rf = new ReasonerFactory();
//                OWLReasoner r = rf.createReasoner(a);
//                /* ******************** */
//                /* This changes for each different ontology */
//                r.precomputeInferences(InferenceType.CLASS_HIERARCHY);
////                r.getSubClasses(df.getOWLClass("https://www.maritimelinkeddata.org/c-msa-hsn#MSAObservation"), false).forEach(System.out::println);
//                r.getSubClasses(df.getOWLClass("http://www.semanticweb.org/xarakas/ontologies/2023/2/charis-muslim#City"), false).forEach(System.out::println);
//                /* ******************** */
//            } catch (InconsistentOntologyException e) {
////                logger.info("Inconsistency explanation: " + my_onta.explainInconsistency());
//            }
//            /*-----------------------------*/


        }

    }



