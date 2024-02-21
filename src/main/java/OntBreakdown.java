import com.clarkparsia.owlapi.explanation.BlackBoxExplanation;
import com.clarkparsia.owlapi.explanation.HSTExplanationGenerator;
import org.semanticweb.HermiT.Configuration;
import org.semanticweb.HermiT.ReasonerFactory;
import org.semanticweb.owl.explanation.api.Explanation;
import org.semanticweb.owl.explanation.api.ExplanationGenerator;
import org.semanticweb.owl.explanation.impl.blackbox.checker.InconsistentOntologyExplanationGeneratorFactory;
import org.semanticweb.owlapi.formats.TurtleDocumentFormat;
import org.semanticweb.owlapi.manchestersyntax.renderer.ManchesterOWLSyntaxObjectRenderer;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.parameters.Imports;
//import org.semanticweb.owlapi.modularity.locality.SyntacticLocalityModuleExtractor;
import org.semanticweb.owlapi.reasoner.*;
import org.semanticweb.owlapi.util.SimpleShortFormProvider;
import uk.ac.manchester.cs.owl.owlapi.OWLAxiomImpl;
import uk.ac.manchester.cs.owl.owlapi.OWLIndividualImpl;
import uk.ac.manchester.cs.owlapi.modularity.SyntacticLocalityModuleExtractor;
import org.semanticweb.owlapi.reasoner.structural.StructuralReasoner;
import uk.ac.manchester.cs.owlapi.modularity.ModuleType;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.StringWriter;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.semanticweb.owlapi.apibinding.OWLManager.createOWLOntologyManager;

public class OntBreakdown {


    private OWLOntologyManager man;
    private OWLDataFactory df;
    private IRI iri;
    private OWLOntology o;
    private SimpleShortFormProvider sf = new SimpleShortFormProvider();
    private boolean debug = false;
    private OWLOntology t;

    static Logger logger = Logger.getLogger(ExtractModules.class.getName());

    /*  Load the ontology from file */
    public OntBreakdown(String pathname, boolean debug){
        this.debug = debug;
        man = createOWLOntologyManager();
        df = man.getOWLDataFactory();
        File file = new File(pathname);
        try {
            o = man.loadOntologyFromOntologyDocument(file);
            t = man.createOntology(o.tboxAxioms(Imports.INCLUDED));
            o.rboxAxioms(Imports.INCLUDED).forEach(
                    t::addAxiom);
        } catch (OWLOntologyCreationException e) {
            e.printStackTrace();
        }
    }

    /*  Load the ontology from IRI */
    public OntBreakdown(IRI ontologyIri, boolean debug){
        this.debug = debug;
        man = createOWLOntologyManager();
        df = man.getOWLDataFactory();
        iri = ontologyIri;
        try {
            o = man.loadOntology(ontologyIri);
            t = man.createOntology(o.tboxAxioms(Imports.INCLUDED));
            o.rboxAxioms(Imports.INCLUDED).forEach(
                    t::addAxiom);
        } catch (OWLOntologyCreationException e) {
            e.printStackTrace();
        }
    }

    /*  Load the ontology from OWLOntology */
    public OntBreakdown(OWLOntology ontology, boolean debug) throws OWLOntologyCreationException {
        this.debug = debug;
        o = ontology;
        man = createOWLOntologyManager();
        df = man.getOWLDataFactory();
        t = man.createOntology(o.tboxAxioms(Imports.INCLUDED));
        o.rboxAxioms(Imports.INCLUDED).forEach(
                t::addAxiom);
    }


    public void printOntologySummary(){
        System.out.println(o);
        System.out.println("Axioms: "+o.getAxiomCount(Imports.INCLUDED)+", Format: "+man.getOntologyFormat(o));
        System.out.println("A-Box size: "+o.aboxAxioms(Imports.INCLUDED).toArray().length);
        System.out.println("T-Box size: "+o.tboxAxioms(Imports.INCLUDED).toArray().length);
        System.out.println("R-Box size: "+o.rboxAxioms(Imports.INCLUDED).toArray().length);
        System.out.println("No. of classes: " + o.classesInSignature(Imports.INCLUDED).toArray().length);
        System.out.println("No. of individuals: " + o.individualsInSignature(Imports.INCLUDED).toArray().length);
        System.out.println("No. of object properties: " + o.objectPropertiesInSignature(Imports.INCLUDED).toArray().length);
        System.out.println("No. of general class axioms: " + o.generalClassAxioms().toArray().length);
        System.out.println("No. of logical axioms: " + o.logicalAxioms(Imports.INCLUDED).toArray().length);
    }

    public String logOntologySummary(){
        String log = o.toString();
        log = log + "\nAxioms: " + o.getAxiomCount() + ", Format: " + man.getOntologyFormat(o) + "\n"
            + "A-Box size: " + o.aboxAxioms(Imports.INCLUDED).toArray().length + "\n"
            + "T-Box size: " + o.tboxAxioms(Imports.INCLUDED).toArray().length + "\n"
            + "R-Box size: " + o.rboxAxioms(Imports.INCLUDED).toArray().length + "\n"
            + "No. of classes: " + o.classesInSignature(Imports.INCLUDED).toArray().length + "\n"
            + "No. of individuals: " + o.individualsInSignature(Imports.INCLUDED).toArray().length + "\n"
            + "No. of object properties: " + o.objectPropertiesInSignature(Imports.INCLUDED).toArray().length + "\n"
            + "No. of general class axioms: " + o.generalClassAxioms().toArray().length + "\n"
            + "No. of logical axioms: " + o.logicalAxioms(Imports.INCLUDED).toArray().length;
        return log;
    }

    public void printOntologyClasses(){
        System.out.println("All Classes:");
        o.classesInSignature().forEach(p->{System.out.println("Class: " + p); });
    }

    public void printOntologyAxioms(){
        System.out.println("All Axioms:");
        o.axioms(Imports.INCLUDED).forEach(p->{System.out.println("Axiom: " + p); });
    }

    public String logOntologyAxioms(){
        final String[] log = new String[1];
        log[0] = "";
        o.axioms(Imports.INCLUDED).forEach(p->{
            log[0] = log[0] + p + "\n"; });
        return log[0];
    }

    public String logOntologyTBox(String seperator){
        final String[] log = new String[1];
        log[0] = "";
        o.tboxAxioms(Imports.INCLUDED).forEach(p->{
            log[0] = log[0] + p + seperator; });
        return log[0];
    }

    public void printOntologyIndividuals(){
        System.out.println("All Individuals:");
        o.individualsInSignature().forEach(p->{System.out.println("Individual: " + p); });
    }

    public void printOntologyObjectProperties(){
        System.out.println("All Object Properties:"); // TODO: RELATIONS?
        o.objectPropertiesInSignature().forEach(p->{System.out.println("Object Property: " + p); });
    }

    public void printOntologyLogicalAxioms(){
        System.out.println("All Logical Axioms:");
        o.logicalAxioms().forEach(System.out::println);
    }

    public void printOntologyABox(){
        System.out.println("All A-Box Axioms:");
        o.aboxAxioms(Imports.INCLUDED).forEach(System.out::println);
    }

    public void printOntologyTBox(){
        System.out.println("All T-Box Axioms:");
        o.tboxAxioms(Imports.INCLUDED).forEach(System.out::println);
    }

    public void printOntologyRBox(){
        System.out.println("All R-Box Axioms:");
        o.rboxAxioms(Imports.INCLUDED).forEach(System.out::println);
    }

    public OWLOntology getOntology(){ return o; }

    public Stream<OWLClass> getOntologyClasses(){ return o.classesInSignature(Imports.INCLUDED); }

    public Stream<OWLAxiom> getOntologyAxioms(){
        return o.axioms(Imports.INCLUDED);
    }

    public Stream<OWLNamedIndividual> getOntologyIndividuals(){
        return o.individualsInSignature(Imports.INCLUDED);
    }

    public Stream<OWLLogicalAxiom> getOntologyLogicalAxioms(){
        return o.logicalAxioms(Imports.INCLUDED);
    }

    public Stream <OWLAxiom> getOntologyABox(){ return o.aboxAxioms(Imports.INCLUDED); }

    public Stream <OWLAxiom> getOntologyTBox(){ return o.tboxAxioms(Imports.INCLUDED); }

    public Stream <OWLAxiom> getOntologyRBox(){ return o.rboxAxioms(Imports.INCLUDED); }

    public ConcurrentHashMap<OWLEntity, ConcurrentLinkedQueue<OWLAxiom>> getABoxModulesConcurrentHashMap(){
        ConcurrentHashMap<OWLEntity, ConcurrentLinkedQueue<OWLAxiom>> modules = new ConcurrentHashMap<>();
//        ConcurrentLinkedQueue<OWLIndividual> inds = new ConcurrentLinkedQueue(o.getIndividualsInSignature(Imports.INCLUDED));
//        o.getIndividualsInSignature(Imports.INCLUDED).forEach(p->{ System.out.println(p.toString() + p.isIndividual());});
        o.getAxioms(Imports.INCLUDED).parallelStream().forEach(
                i -> {
                    i.signature().forEach(
                            en -> {
//                                System.out.println(en.toString() + en.isIndividual() + inds.contains(en));
                                if (en.isIndividual()){ //(inds.contains(en)){
//                                    System.out.println(en.toString());
//                                    String temp_term = en.getIRI().getFragment();
                                    String temp_term = "Uninitialized";
                                    if (en.toString().contains("#")) {temp_term = en.toString().split("#")[en.toString().split("#").length-1].replace("/","-").replace(">","");}
                                    else {temp_term = en.toString().split("/")[en.toString().split("/").length-1].replace(">","");}
//                                    System.out.println("Would be: "+en.getIRI().getFragment()+", but is "+temp_term);
                                    ConcurrentLinkedQueue<OWLAxiom> temp = modules.computeIfPresent(en, (k,v) -> v);
//                                    System.out.println(temp_term);
                                    if(temp != null) {
                                        temp.add(i);
                                        modules.putIfAbsent(en, temp);
//                                        System.out.println("Added " + i + " " + temp_term);
                                    }
                                    else {
                                        ConcurrentLinkedQueue<OWLAxiom> temp1 = new ConcurrentLinkedQueue<>();
                                        temp1.add(i);
                                        modules.putIfAbsent(en, temp1);
//                                        System.out.println("Added " + i + " " + temp_term);
                                    }
                                }
                            }
                    );
                }
        );
        logger.info("Module count: "+modules.size());
        return modules;
    }

    public boolean isConsistent(){
        OWLReasonerFactory rf = new ReasonerFactory();
        Configuration conf = new Configuration();
        conf.individualTaskTimeout = Long.MAX_VALUE;
        conf.ignoreUnsupportedDatatypes = true;
        OWLReasoner reasoner = rf.createReasoner(o,conf);
        return reasoner.isConsistent();
    }

    // TODO: Deprecate this method
    public String explainInconsistency() {
        long startTime = System.nanoTime();
        String expl = "";
        OWLReasonerFactory rf = new ReasonerFactory();
        Configuration conf = new Configuration();
        conf.individualTaskTimeout = Long.MAX_VALUE;
        conf.ignoreUnsupportedDatatypes = true;
//        Reasoner r = (Reasoner) rf.createReasoner(o,conf);
        Supplier<OWLOntologyManager> m = () -> man;
        if (debug){
            logger.info("Checking for inconsistency");
            o.getAxioms().forEach(a -> {logger.info(a.toString());});
        }
        ExplanationGenerator<OWLAxiom> explainInconsistency = new CustomConfInconsistentOntologyExplanationGeneratorFactory(rf, df, m, conf).createExplanationGenerator(o);
        // Ask for an explanation of `Thing subclass of Nothing` - this axiom is entailed in any inconsistent ontology
        Set<Explanation<OWLAxiom>> explanations = explainInconsistency.getExplanations(df.getOWLSubClassOfAxiom(df
                .getOWLThing(), df.getOWLNothing()), 100);
        for (Explanation exp : explanations) expl = expl + exp.getAxioms().toString();
        long endTime = System.nanoTime();
        if (debug){
            logger.info("Checking for inconsistency of " + o.getOntologyID().getOntologyIRI().get().toString() + " required " + (endTime - startTime)/1000000 + " milliseconds.");
        }
        return expl;
    }

    public Set<Set<OWLAxiom>> getExplanations(long timeoutMillis, int limit) {
        long startTime = System.nanoTime();
        Set<Set<OWLAxiom>> explanationsAll = new HashSet<> ();
        OWLReasonerFactory rf = new ReasonerFactory();
        Configuration conf = new Configuration();
        conf.individualTaskTimeout = timeoutMillis;
        conf.ignoreUnsupportedDatatypes = true;
        Supplier<OWLOntologyManager> m = () -> man;
        OWLClass errorTimeoutEnt = m.get().getOWLDataFactory().getOWLEntity(EntityType.CLASS,IRI.create("ReasoningTimedOut!"));
//        OWLIndividual errorTimeoutInd = m.get().getOWLDataFactory().getOWLNamedIndividual("ReasoningTimedOut");
        OWLAxiom errorTimeout = m.get().getOWLDataFactory().getOWLDeclarationAxiom(errorTimeoutEnt);
        if (debug){
            logger.info("Checking for inconsistency");
            o.getAxioms().forEach(a -> {logger.info(a.toString());});
        }
        ExplanationGenerator<OWLAxiom> explainInconsistency = new CustomConfInconsistentOntologyExplanationGeneratorFactory(rf, df, m, conf).createExplanationGenerator(o);
        //Callable<Set<Explanation<OWLAxiom>>> explainTask = () -> {
            // Ask for an explanation of `Thing subclass of Nothing` - this axiom is entailed in any inconsistent ontology
            Set<Explanation<OWLAxiom>> explanations = explainInconsistency.getExplanations(df.getOWLSubClassOfAxiom(df
//                    .getOWLThing(), df.getOWLNothing()), 10000);
                    .getOWLThing(), df.getOWLNothing()), limit);
          //  return explanations;
        //};
//        ExecutorService executor = Executors.newFixedThreadPool(1);
//        Future<Set<Explanation<OWLAxiom>>> futureExplanations =  executor.submit(explainTask);
//        Set<Explanation<OWLAxiom>> explanations = new HashSet<>();
//        try {
//            explanations = futureExplanations.get(timeoutMillis,TimeUnit.MILLISECONDS);
//        } catch (TimeoutException | InterruptedException | ExecutionException e){
//            futureExplanations.cancel(true);
//            Set<OWLAxiom> err = new HashSet<>();
//            err.add(errorTimeout);
//            explanations.add(new Explanation<>(errorTimeout,err));
//            logger.severe("Ontology: " + t.getOntologyID().getOntologyIRI().get().toString() + " - Reasoning timed out after " + timeoutMillis);
//        }

        long endTime = System.nanoTime();
        if (debug){
            logger.info("Checking for inconsistency of " + o.getOntologyID().getOntologyIRI().get().toString() + " required " + (endTime - startTime)/1000000 + " milliseconds.");
        }
        for (Explanation exp : explanations) {
            explanationsAll.add(exp.getAxioms());
        }
//        executor.shutdownNow();
        return explanationsAll;
    }


    public String explainInconsistencySatisfiable() {
        ConcurrentLinkedQueue<OWLAxiom> expl = new ConcurrentLinkedQueue<>();
//        ConsoleProgressMonitor progressMonitor = new ConsoleProgressMonitor();
//        OWLReasonerConfiguration config = new SimpleConfiguration(progressMonitor, Long.MAX_VALUE);
        Configuration conf = new Configuration();
        conf.individualTaskTimeout = Long.MAX_VALUE;
        conf.ignoreUnsupportedDatatypes = true;
//        conf.throwInconsistentOntologyException=true;
        int classCount = (int) o.classesInSignature(Imports.INCLUDED).count();
        AtomicInteger progress = new AtomicInteger();
        if (debug){
            logger.info("Checking for inconsistency (satisfiable classes). Classes count: " + classCount);
        }
        long startTime = System.nanoTime();
        // TODO make sure no issues arise if we parallelize the following
        try {
            o.classesInSignature(Imports.INCLUDED).collect(Collectors.toList()).parallelStream().forEach(
                    p -> {
                        progress.set(progress.get() + 1);
                        if (debug){
                            logger.info(classCount + "/" + progress.get() + ": Checking for inconsistency (is class " +p.getIRI().toString() + " satisfiable?)");
                        }
                        long startTime1 = System.nanoTime();
                        OWLOntology ont = getOntology();
                        OWLReasonerFactory rf = new ReasonerFactory();
                        OWLReasoner r = rf.createReasoner(ont, conf);
                        if (!r.isSatisfiable(p)) {
                            BlackBoxExplanation exp = new BlackBoxExplanation(ont, rf, r);
                            HSTExplanationGenerator multExplanator = new HSTExplanationGenerator(exp);
                            // Now we can get explanations for the unsatisfiability.
                            Set<Set<OWLAxiom>> explanations = multExplanator.getExplanations(p);
                            // Let us print them. Each explanation is one possible set of axioms that cause the
                            // unsatisfiability.
                            for (Set<OWLAxiom> explanation : explanations) {
                                //                            System.out.println("------------------");
                                //                            System.out.println("Axioms causing the unsatisfiability: ");
                                expl.addAll(explanation);
                                //                            System.out.println("------------------");
                            }
                        }
                        long endTime1 = System.nanoTime();
                        if (debug){
                            logger.info("Checking for inconsistency (is class " +p.getIRI().toString() + " satisfiable?) required " + (endTime1 - startTime1)/1000000 + " milliseconds in total.");
                        }
                    });
        } catch (InconsistentOntologyException e){
            Supplier<OWLOntologyManager> m = () -> man;
            OWLReasonerFactory rf = new ReasonerFactory();
            ExplanationGenerator<OWLAxiom> explainInconsistency = new InconsistentOntologyExplanationGeneratorFactory(rf, df, m, 1000000L).createExplanationGenerator(o);
            // Ask for an explanation of `Thing subclass of Nothing` - this axiom is entailed in any inconsistent ontology
            Set<Explanation<OWLAxiom>> explanations = explainInconsistency.getExplanations(df.getOWLSubClassOfAxiom(df
                    .getOWLThing(), df.getOWLNothing()), 100);
            for (Explanation exp : explanations) expl.addAll(exp.getAxioms());
        }

        StringBuilder result_expl = new StringBuilder();
        for (OWLAxiom axiom : expl) {
            result_expl.append(axiom);
        }
        long endTime = System.nanoTime();
        if (debug){
            logger.info("Checking for inconsistency (satisfiable classes) required " + (endTime - startTime)/1000000 + " milliseconds in total.");
        }
        return result_expl.toString();
    }

    public Set<Set<OWLAxiom>> getExplanationsSatisfiable() {
        ConcurrentLinkedQueue<OWLAxiom> expl = new ConcurrentLinkedQueue<>();
//        ConsoleProgressMonitor progressMonitor = new ConsoleProgressMonitor();
//        OWLReasonerConfiguration config = new SimpleConfiguration(progressMonitor, Long.MAX_VALUE);
        Set<Set<OWLAxiom>> explanationsAll = new HashSet<> ();
        Configuration conf = new Configuration();
        conf.individualTaskTimeout = Long.MAX_VALUE;
        conf.ignoreUnsupportedDatatypes = true;
//        conf.throwInconsistentOntologyException=true;
        int classCount = (int) o.classesInSignature(Imports.INCLUDED).count();
        AtomicInteger progress = new AtomicInteger();
        if (debug){
            logger.info("Checking for inconsistency (satisfiable classes). Classes count: " + classCount);
        }
        long startTime = System.nanoTime();
        // TODO make sure no issues arise if we parallelize the following
        try {
            o.classesInSignature(Imports.INCLUDED).collect(Collectors.toList()).parallelStream().forEach(
                    p -> {
                        progress.set(progress.get() + 1);
                        if (debug){
                            logger.info(classCount + "/" + progress.get() + ": Checking for inconsistency (is class " +p.getIRI().toString() + " satisfiable?)");
                        }
                        long startTime1 = System.nanoTime();
                        OWLOntology ont = getOntology();
                        OWLReasonerFactory rf = new ReasonerFactory();
                        OWLReasoner r = rf.createReasoner(ont, conf);
                        if (!r.isSatisfiable(p)) {
                            BlackBoxExplanation exp = new BlackBoxExplanation(ont, rf, r);
                            HSTExplanationGenerator multExplanator = new HSTExplanationGenerator(exp);
                            // Now we can get explanations for the unsatisfiability.
                            Set<Set<OWLAxiom>> explanations = multExplanator.getExplanations(p);
                            explanationsAll.addAll(explanations);
                            // Let us print them. Each explanation is one possible set of axioms that cause the
                            // unsatisfiability.
//                            for (Set<OWLAxiom> explanation : explanations) {
//                                //                            System.out.println("------------------");
//                                //                            System.out.println("Axioms causing the unsatisfiability: ");
//                                expl.addAll(explanation);
//                                //                            System.out.println("------------------");
//                            }
                        }
                        long endTime1 = System.nanoTime();
                        if (debug){
                            logger.info("Checking for inconsistency (is class " +p.getIRI().toString() + " satisfiable?) required " + (endTime1 - startTime1)/1000000 + " milliseconds in total.");
                        }
                    });
        } catch (InconsistentOntologyException e){
            Supplier<OWLOntologyManager> m = () -> man;
            OWLReasonerFactory rf = new ReasonerFactory();
            ExplanationGenerator<OWLAxiom> explainInconsistency = new InconsistentOntologyExplanationGeneratorFactory(rf, df, m, 1000000L).createExplanationGenerator(o);
            // Ask for an explanation of `Thing subclass of Nothing` - this axiom is entailed in any inconsistent ontology
            Set<Explanation<OWLAxiom>> explanations = explainInconsistency.getExplanations(df.getOWLSubClassOfAxiom(df
                    .getOWLThing(), df.getOWLNothing()), 100);
            for (Explanation exp : explanations) {
                explanationsAll.add(exp.getAxioms());
//                expl.addAll(exp.getAxioms());
            }
        }
//        StringBuilder result_expl = new StringBuilder();
//        for (OWLAxiom axiom : expl) {
//            result_expl.append(axiom);
//        }
        long endTime = System.nanoTime();
        if (debug){
            logger.info("Checking for inconsistency (satisfiable classes) required " + (endTime - startTime)/1000000 + " milliseconds in total.");
        }
        return explanationsAll;
    }

    public String explainInconsistencySatisfiable2() {
        ConcurrentLinkedQueue<OWLAxiom> expl = new ConcurrentLinkedQueue<>();
        Configuration conf = new Configuration();
        conf.individualTaskTimeout = Long.MAX_VALUE;
        conf.ignoreUnsupportedDatatypes = true;
        OWLOntology ont = getOntology();
        OWLReasonerFactory rf = new ReasonerFactory();
        OWLReasoner r = rf.createReasoner(ont, conf);
        if (debug){
            logger.info("Checking for unsatisfiable classes.");
        }
        long startTime = System.nanoTime();
        Stream<OWLClass> l1 = r.unsatisfiableClasses();
        List<OWLClass> l = l1.collect(Collectors.toList());
//        Stream<OWLClass> l2 = l;
        if (debug){
            logger.info("Unsat. Classes found: " + l.size());
            l.forEach(p -> logger.info(p.getIRI().toString()));
        }
//        StringBuilder result_expl = new StringBuilder();
        try {
            l.parallelStream().forEach(p -> {
                OWLOntology ont2 = getOntology();
                OWLReasonerFactory rf2 = new ReasonerFactory();
                OWLReasoner r2 = rf2.createReasoner(ont2, conf);
                BlackBoxExplanation exp = new BlackBoxExplanation(ont2, rf2, r2);
                HSTExplanationGenerator multExplanator = new HSTExplanationGenerator(exp);
                // Now we can get explanations for the unsatisfiability.
                Set<Set<OWLAxiom>> explanations = multExplanator.getExplanations(p);
                // Let us print them. Each explanation is one possible set of axioms that cause the
                // unsatisfiability.
                for (Set<OWLAxiom> explanation : explanations) {
                    //                            System.out.println("------------------");
                    //                            System.out.println("Axioms causing the unsatisfiability: ");
                    expl.addAll(explanation);
                    //                            System.out.println("------------------");
                }
            });
        }
        catch (InconsistentOntologyException e){
            Supplier<OWLOntologyManager> m = () -> man;
            ExplanationGenerator<OWLAxiom> explainInconsistency = new InconsistentOntologyExplanationGeneratorFactory(rf, df, m, 1000000L).createExplanationGenerator(o);
            // Ask for an explanation of `Thing subclass of Nothing` - this axiom is entailed in any inconsistent ontology
            Set<Explanation<OWLAxiom>> explanations = explainInconsistency.getExplanations(df.getOWLSubClassOfAxiom(df
                    .getOWLThing(), df.getOWLNothing()), 100);
            for (Explanation exp : explanations) expl.addAll(exp.getAxioms());
        }
        StringBuilder result_expl = new StringBuilder();
        for (OWLAxiom axiom : expl) {
            result_expl.append(axiom);
        }
        long endTime = System.nanoTime();
        if (debug){
            logger.info("Checking explanations for unsatisfiable classes required " + (endTime - startTime)/1000000 + " milliseconds in total.");
        }
        return result_expl.toString();
    }

    public Set<Set<OWLAxiom>> getExplanationsSatisfiable2() {
        ConcurrentLinkedQueue<OWLAxiom> expl = new ConcurrentLinkedQueue<>();
        Configuration conf = new Configuration();
        conf.individualTaskTimeout = Long.MAX_VALUE;
        conf.ignoreUnsupportedDatatypes = true;
        Set<Set<OWLAxiom>> explanationsAll = new HashSet<> ();
        OWLOntology ont = getOntology();
        OWLReasonerFactory rf = new ReasonerFactory();
        OWLReasoner r = rf.createReasoner(ont, conf);
        if (debug){
            logger.info("Checking for unsatisfiable classes.");
        }
        long startTime = System.nanoTime();
        Stream<OWLClass> l1 = r.unsatisfiableClasses();
        List<OWLClass> l = l1.collect(Collectors.toList());
//        Stream<OWLClass> l2 = l;
        if (debug){
            logger.info("Unsat. Classes found: " + l.size());
            l.forEach(p -> logger.info(p.getIRI().toString()));
        }
//        StringBuilder result_expl = new StringBuilder();
        try {
            l.parallelStream().forEach(p -> {
                OWLOntology ont2 = getOntology();
                OWLReasonerFactory rf2 = new ReasonerFactory();
                OWLReasoner r2 = rf2.createReasoner(ont2, conf);
                BlackBoxExplanation exp = new BlackBoxExplanation(ont2, rf2, r2);
                HSTExplanationGenerator multExplanator = new HSTExplanationGenerator(exp);
                // Now we can get explanations for the unsatisfiability.
                Set<Set<OWLAxiom>> explanations = multExplanator.getExplanations(p);
                explanationsAll.addAll(explanations);

                // Let us print them. Each explanation is one possible set of axioms that cause the
                // unsatisfiability.
//                for (Set<OWLAxiom> explanation : explanations) {
//                    //                            System.out.println("------------------");
//                    //                            System.out.println("Axioms causing the unsatisfiability: ");
//                    expl.addAll(explanation);
//                    //                            System.out.println("------------------");
//                }
            });
        }
        catch (InconsistentOntologyException e){
            Supplier<OWLOntologyManager> m = () -> man;
            ExplanationGenerator<OWLAxiom> explainInconsistency = new InconsistentOntologyExplanationGeneratorFactory(rf, df, m, 1000000L).createExplanationGenerator(o);
            // Ask for an explanation of `Thing subclass of Nothing` - this axiom is entailed in any inconsistent ontology
            Set<Explanation<OWLAxiom>> explanations = explainInconsistency.getExplanations(df.getOWLSubClassOfAxiom(df
                    .getOWLThing(), df.getOWLNothing()), 100);
            for (Explanation exp : explanations) {
                explanationsAll.addAll(exp.getAxioms());
//                expl.addAll(exp.getAxioms());
            }
        }
//        StringBuilder result_expl = new StringBuilder();
//        for (OWLAxiom axiom : expl) {
//            result_expl.append(axiom);
//        }
        long endTime = System.nanoTime();
        if (debug){
            logger.info("Checking explanations for unsatisfiable classes required " + (endTime - startTime)/1000000 + " milliseconds in total.");
        }
        return explanationsAll;
    }

    public IRI findTermIRI(OWLNamedIndividual name) throws Exception {
        IRI new_iri = null;
        IRI tempIRI = IRI.create(name.toString().replace("<","").replace(">",""));
        for (OWLEntity c : o.getSignature(Imports.INCLUDED)){
            if(c.getIRI().equals(tempIRI)) {
                new_iri = c.getIRI();
                break;
            }
            else if(getManchesterSyntax(c).equals(name.toString())) {
                new_iri = c.getIRI();
                break;
            }
        }
        if (new_iri==null){
            throw new Exception("No such term found in ontology: " + name);
        } else{
        return new_iri;}
    }

    // Imported from https://github.com/rsgoncalves/module-extractor/
    private String getManchesterSyntax(OWLObject obj) {
        StringWriter wr = new StringWriter();
        ManchesterOWLSyntaxObjectRenderer render = new ManchesterOWLSyntaxObjectRenderer(wr, sf);
        obj.accept(render);
        return wr.getBuffer().toString();
    }

    public OWLOntology getABoxTermModule(IRI term) throws OWLOntologyCreationException {
        // TODO Revise the following line, i.e. name the module by convention
        IRI new_iri = null;
        try {
            String temp_term = "Uninitialized";
            if (term.toString().contains("#")) {temp_term = term.toString().split("#")[term.toString().split("#").length-1].replace("/","-").split(">")[0];}
            else {temp_term = term.toString().split("/")[term.toString().split("/").length-1].split(">")[0];}
//            String temp_term = term.getFragment();
            new_iri = IRI.create(o.getOntologyID().getOntologyIRI().get() + "/module/" + temp_term.toString());
        } catch (Exception e){
            new_iri = IRI.create("http://moduleOf.ont/" + term.toString());
        }
        // ^^^^
        OWLOntologyManager man2 = createOWLOntologyManager();
        OWLOntology newOntology = man2.createOntology(new_iri);
        if (debug){
            logger.info("Extracting module of: " + term.toString());
        }
        long startTime = System.nanoTime();
        // TODO consider for parallelization
        o.tboxAxioms(Imports.INCLUDED).forEach(
            newOntology::addAxiom);
        o.rboxAxioms(Imports.INCLUDED).forEach(
                newOntology::addAxiom);
        o.getAxioms(Imports.INCLUDED).forEach(p -> p.signature().forEach( q -> {if(q.getIRI().equals(term)) newOntology.addAxiom(p);}));
        long endTime = System.nanoTime();
        if (debug){
            logger.info("Extracting module of " + term.toString() + " required " + (endTime - startTime)/1000000 + " milliseconds.");
        }
        return newOntology;
    }


    public OWLOntology getABoxTermModule2(IRI term) throws OWLOntologyCreationException {
        // TODO Revise the following line, i.e. name the module by convention
        IRI new_iri = null;
        try {
            String temp_term = "Uninitialized";
            if (term.toString().contains("#")) {temp_term = term.toString().split("#")[term.toString().split("#").length-1].replace("/","-").split(">")[0];}
            else {temp_term = term.toString().split("/")[term.toString().split("/").length-1].split(">")[0];}
//            String temp_term = term.getFragment();
            new_iri = IRI.create(o.getOntologyID().getOntologyIRI().get() + "/module/" + temp_term.toString());
        } catch (Exception e){
            new_iri = IRI.create("http://moduleOf.ont/" + term.toString());
        }
        // ^^^^
        OWLOntologyManager man2 = createOWLOntologyManager();
        OWLOntology newOntology = man2.createOntology(new_iri);
        if (debug){
            logger.info("Extracting module of: " + term.toString());
        }
        long startTime = System.nanoTime();
        // TODO consider for parallelization
        newOntology.addAxioms(t.getAxioms());
        o.getAxioms(Imports.INCLUDED).forEach(p -> { if(p.signature().filter(z -> z.getIRI().equals(term)).count()>0) { newOntology.addAxiom(p);}});
//        o.getAxioms(Imports.INCLUDED).forEach(p -> p.signature().forEach( q -> {if(q.getIRI().equals(term)) newOntology.addAxiom(p);}));
        long endTime = System.nanoTime();
        if (debug){
            logger.info("Extracting module of " + term.toString() + " required " + (endTime - startTime)/1000000 + " milliseconds.");
        }
        return newOntology;
    }

    public OWLOntology getTBoxOnly() throws OWLOntologyCreationException {
        // TODO Revise the following line, i.e. name the module by convention
//        IRI term = IRI.create("onlyTBox");
//        IRI new_iri = null;
//        try {
//            new_iri = IRI.create(o.getOntologyID().getOntologyIRI().get() + "/module/" + term.toString());
//        } catch (Exception e){
//            new_iri = IRI.create("http://moduleOf.ont/" + term.toString());
//        }
//        // ^^^^
//        OWLOntology newOntology = man.createOntology(new_iri);
//        o.tboxAxioms(Imports.INCLUDED).forEach(
//                newOntology::addAxiom);
//        o.rboxAxioms(Imports.INCLUDED).forEach(
//                newOntology::addAxiom);
//        return newOntology;
        return t;
    }

    public OWLOntology getSyntLocModuleOfSignature(String remainder) throws OWLOntologyCreationException {
//        System.out.println("\n\nAll Modules:");
/*
        SyntacticLocalityModuleExtractor modex =
                new SyntacticLocalityModuleExtractor(LocalityClass.STAR,o.axioms());
        o.individualsInSignature().forEach(g -> modex.extract(o.));
        o.individualsInSignature().forEach(System.out::println);
        Stream<OWLAxiom> g = modex.extract(o.entitiesInSignature(iri));
        OWLOntology f = modex.extractAsOntology(o.entitiesInSignature(iri), man, IRI.create("http://ncsr.semantics.org/ontologies/SHACL/firstShacl.owl"));
        f.signature().forEach(System.out::println);
//        System.out.println(f.signature()..findFirst().toString());
        return g;*/
        
        // We want to extract a module for all toppings. We therefore have to
        // generate a seed signature that contains <remainder> and its
        // subclasses. We start by creating a signature that consists of
        // <remainder>.
        OWLClass toppingCls =
                df.getOWLClass(o.getOntologyID().getOntologyIRI().get() + "#", remainder);
        Set<OWLEntity> sig = new HashSet<OWLEntity>();
        sig.add(toppingCls);
        // We now add all subclasses (direct and indirect) of the chosen
        // classes. Ideally, it should be done using a DL reasoner, in order to
        // take inferred subclass relations into account. We are using the
        // structural reasoner of the OWL API for simplicity.
        Set<OWLEntity> seedSig = new HashSet<OWLEntity>();
        OWLReasoner reasoner =
                new StructuralReasoner(o, new SimpleConfiguration(), BufferingMode.NON_BUFFERING);
        for (OWLEntity ent : sig) {
            seedSig.add(ent);
            if (OWLClass.class.isAssignableFrom(ent.getClass())) {
                NodeSet<OWLClass> subClasses = reasoner.getSubClasses((OWLClass) ent, false);
                seedSig.addAll(asList(subClasses.entities()));
            }
        }
        // We now extract a locality-based module. For most reuse purposes, the
        // module type should be STAR -- this yields the smallest possible
        // locality-based module. These modules guarantee that all entailments
        // of the original ontology that can be formulated using only terms from
        // the seed signature or the module will also be entailments of the
        // module. In easier words, the module preserves all knowledge of the
        // ontology about the terms in the seed signature or the module.
        SyntacticLocalityModuleExtractor sme =
                new SyntacticLocalityModuleExtractor(createOWLOntologyManager(), o, ModuleType.STAR);
        IRI moduleIRI = IRI.create(remainder + ".owl", "");
        OWLOntology mod = sme.extractAsOntology(seedSig, moduleIRI);
        // The module can now be saved as usual
        return mod;
    }

    public static <T> List<T> asList(Stream<T> s) {
        return s.collect(Collectors.toList());
    }

    /*  Save the ontology into a file */
    public void save(String pathname) {
        TurtleDocumentFormat tdf = new TurtleDocumentFormat();
        try {
            FileOutputStream fos = new FileOutputStream(new File(pathname));
            man.saveOntology(this.o,tdf,fos);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (OWLOntologyStorageException e) {
            e.printStackTrace();
        }
    }
}
