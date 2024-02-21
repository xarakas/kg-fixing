import static org.semanticweb.owlapi.apibinding.OWLManager.createOWLOntologyManager;
import static org.semanticweb.owlapi.search.Searcher.annotations;
import static org.semanticweb.owlapi.vocab.OWLRDFVocabulary.RDFS_LABEL;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

import com.clarkparsia.owlapi.explanation.BlackBoxExplanation;
import com.clarkparsia.owlapi.explanation.HSTExplanationGenerator;
import org.semanticweb.HermiT.Configuration;
import org.semanticweb.HermiT.ReasonerFactory;
import org.semanticweb.owl.explanation.api.Explanation;
import org.semanticweb.owl.explanation.api.ExplanationGenerator;
import org.semanticweb.owl.explanation.api.ExplanationGeneratorFactory;
import org.semanticweb.owl.explanation.api.ExplanationManager;
import org.semanticweb.owl.explanation.impl.blackbox.checker.InconsistentOntologyExplanationGeneratorFactory;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.formats.FunctionalSyntaxDocumentFormat;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.reasoner.InconsistentOntologyException;
import org.semanticweb.owlapi.reasoner.InferenceType;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;

public class OwlParser {

    public static void main(String[] args) throws OWLException,
            InstantiationException, IllegalAccessException,
            ClassNotFoundException, FileNotFoundException {

        OWLOntologyManager man = OWLManager.createOWLOntologyManager();
        OWLDataFactory df = man.getOWLDataFactory();
        /*  Load the ontology from URL */
//        IRI pizzaontology = IRI.create("http://protege.stanford.edu/ontologies/pizza/pizza.owl");
//        OWLOntology o = man.loadOntology(pizzaontology);

        /*  Load the ontology from file */
        File file = new File("example-KG/abox_inconsistency_disjoint_classes.ttl");
//        File file = new File("MSA-HSN.owl");
//         File file = new File("comicbook.owl");
// //       File file = new File("wine.owl");
//        File file = new File("SHACL.owl");
        OWLOntology o = man.loadOntologyFromOntologyDocument(file);
        System.out.println(o);
        System.out.println("Axioms: "+o.getAxiomCount()+", Format: "+man.getOntologyFormat(o));

        o.logicalAxioms().forEach(System.out::println);
        OntBreakdown ont = new OntBreakdown(o, false);
        ont.printOntologyTBox();
        System.out.println("T-Box ends here");

        ont.printOntologyABox();
        System.out.println("A-Box ends here");

        ont.printOntologyRBox();
        System.out.println("R-Box ends here");

        OWLReasonerFactory rf = new ReasonerFactory();
        OWLReasoner r = rf.createReasoner(o);
        System.out.println("Reasoning Result:");
        try {
            /* Maritime */
//            r.precomputeInferences(InferenceType.CLASS_HIERARCHY);
//            r.getSubClasses(df.getOWLClass("https://www.maritimelinkeddata.org/c-msa-hsn#MSAObservation"), false).forEach(System.out::println);

           /* Comicbook */
//            r.precomputeInferences(InferenceType.CLASS_HIERARCHY);
//            r.getSubClasses(df.getOWLClass("http://comicmeta.org/cbo/Planet"), false).forEach(System.out::println);

            /* Wine */
//            r.precomputeInferences(InferenceType.CLASS_HIERARCHY);
//            r.getSubClasses(df.getOWLClass("http://www.w3.org/TR/2003/CR-owl-guide-20030818/wine#Wine"), false).forEach(System.out::println);
//            IRI wineIRI=IRI.create("hhttp://www.w3.org/TR/2003/CR-owl-guide-20030818/wine#Wine");
//            OWLClass wine=df.getOWLClass(wineIRI);
//            Configuration configuration=new Configuration();
//            configuration.throwInconsistentOntologyException=true;
//            System.out.println("Is wine satisfiable? "+r.isSatisfiable(wine));

//            /* SHACL ontology */
//            r.precomputeInferences(InferenceType.CLASS_HIERARCHY);
//            r.getSubClasses(df.getOWLClass("http://www.w3.org/ns/shacl#Parameter"), false).forEach(System.out::println);

            /* Ice Cream */
            IRI icecreamIRI=IRI.create("http://www.co-ode.org/ontologies/pizza/pizza.owl#IceCream");
            OWLClass icecream=df.getOWLClass(icecreamIRI);
            Configuration configuration=new Configuration();
            configuration.throwInconsistentOntologyException=true;
            System.out.println("Is icecream satisfiable? "+r.isSatisfiable(icecream));
            System.out.println("Computing explanations...");
            BlackBoxExplanation exp=new BlackBoxExplanation(o, rf, r);
            HSTExplanationGenerator multExplanator=new HSTExplanationGenerator(exp);
            // Now we can get explanations for the unsatisfiability.
            Set<Set<OWLAxiom>> explanations=multExplanator.getExplanations(icecream);
            // Let us print them. Each explanation is one possible set of axioms that cause the
            // unsatisfiability.
            for (Set<OWLAxiom> explanation : explanations) {
                System.out.println("------------------");
                System.out.println("Axioms causing the unsatisfiability: ");
                for (OWLAxiom causingAxiom : explanation) {
                    System.out.println(causingAxiom);
                }
                System.out.println("------------------");
            }
//            r.getSubClasses(df.getOWLClass("http://protege.stanford.edu/ontologies/pizza/pizza.owl#PizzaTopping"), false).forEach(System.out::println);

        } catch (InconsistentOntologyException e) {
            e.printStackTrace();
            System.out.println("\n\tFound Inconsistency!\n");
            Supplier<OWLOntologyManager> m = () -> man;
            ExplanationGenerator<OWLAxiom> explainInconsistency = new InconsistentOntologyExplanationGeneratorFactory(rf,df,m,1000L).createExplanationGenerator(o);
            // Ask for an explanation of `Thing subclass of Nothing` - this axiom is entailed in any inconsistent ontology
            Set<Explanation<OWLAxiom>> explanations = explainInconsistency.getExplanations(df.getOWLSubClassOfAxiom(df
                    .getOWLThing(), df.getOWLNothing()),100);
            for (Explanation exp : explanations){
                System.out.println(exp.getAxioms());
            }

//            Iterator iter = explanations.iterator();
//            while(iter.hasNext()){
//                System.out.print(iter.next()+",");
//            }
        }

        /*  Save the ontology */
//        File fileout = new File("MSA-HSN_updt.owl");
//        man.saveOntology(o, new FunctionalSyntaxDocumentFormat(),new FileOutputStream(fileout));
    }
}