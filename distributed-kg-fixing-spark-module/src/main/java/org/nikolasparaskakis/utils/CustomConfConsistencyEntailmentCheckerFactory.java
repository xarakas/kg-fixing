//package org.nikolasparaskakis.utils;//import org.semanticweb.HermiT.Configuration;
//
////import org.semanticweb.owlapi.reasoner.SimpleConfiguration;
////import org.semanticweb.owlapi.reasoner.OWLReasonerConfiguration;
//
//import org.semanticweb.owl.explanation.impl.blackbox.EntailmentChecker;
//import org.semanticweb.owl.explanation.impl.blackbox.EntailmentCheckerFactory;
//import org.semanticweb.owlapi.model.OWLAxiom;
//import org.semanticweb.owlapi.model.OWLDataFactory;
//import org.semanticweb.owlapi.model.OWLOntologyManager;
//import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;
//
//import java.util.function.Supplier;
//
///**
// * Author: Matthew Horridge<br>
// * The University of Manchester<br>
// * Information Management Group<br>
// * Date: 01-May-2009
// */
//public class CustomConfConsistencyEntailmentCheckerFactory implements EntailmentCheckerFactory<OWLAxiom> {
//
//    private OWLReasonerFactory reasonerFactory;
//
//    private long timeout = Long.MAX_VALUE;
//
//    private OWLDataFactory df;
//
//    private Supplier<OWLOntologyManager> m;
//
////    private Configuration conf;
//
////    public ConsistencyEntailmentCheckerFactory(OWLReasonerFactory reasonerFactory) {
////        this(reasonerFactory, Long.MAX_VALUE);
////    }
//
//    public CustomConfConsistencyEntailmentCheckerFactory(OWLReasonerFactory reasonerFactory, Supplier<OWLOntologyManager> m, OWLDataFactory df) {
//        this.reasonerFactory = reasonerFactory;
//        this.df = df;
//        this.m = m;
////        this.conf = conf;
//    }
//
//    @Override
//    public EntailmentChecker<OWLAxiom> createEntailementChecker(OWLAxiom entailment) {
//        return new CustomConfConsistencyEntailmentChecker(reasonerFactory, m, df);
//    }
//}
