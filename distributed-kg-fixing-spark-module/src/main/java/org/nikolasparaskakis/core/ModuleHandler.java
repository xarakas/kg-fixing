package org.nikolasparaskakis.core;



import openllet.owlapi.OpenlletReasonerFactory;
import org.nikolasparaskakis.utils.CustomExplanationProgressMonitor;
import org.nikolasparaskakis.utils.CustomOwlReasonerFactory;
import org.nikolasparaskakis.utils.DataAxiomSanitizer;
import org.semanticweb.HermiT.ReasonerFactory;
import org.semanticweb.elk.owlapi.ElkReasonerFactory;
import org.semanticweb.owl.explanation.api.*;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.reasoner.*;
import uk.ac.manchester.cs.factplusplus.owlapi.FaCTPlusPlusReasonerFactory;
import uk.ac.manchester.cs.jfact.JFactFactory;
import org.semanticweb.owl.explanation.impl.blackbox.checker.InconsistentOntologyExplanationGeneratorFactory;
import java.time.Duration;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import static org.nikolasparaskakis.utils.DataAxiomSanitizer.sanitize;
import static org.semanticweb.owlapi.apibinding.OWLManager.createOWLOntologyManager;
import org.semanticweb.owl.explanation.api.Explanation;
import org.semanticweb.owl.explanation.api.ExplanationGenerator;



/**
 * Class for handling ontology modules, checking consistency, and generating explanations for inconsistencies.
 */
@SuppressWarnings({"unused", "DuplicatedCode"})
public class ModuleHandler implements AutoCloseable {

    /**
     * The base individual used in explanations.
     */
    private String baseIndividual;

    /**
     * The ontology containing the module axioms.
     */
    private OWLOntology ontology;

    /**
     * The data factory for creating OWL API objects.
     */
    private OWLDataFactory dataFactory;

    /**
     * The reasoner selection:
     * 1 - HermiT
     * 2 - Openllet
     * 3 - JFact
     * 4 - FaCT++
     * 5 - ELK
     */
    private final int reasonerSelection;

    /**
     * Constructor for ModuleHandler.
     * @param ontologyAxioms The set of OWLAxioms representing the module.
     * @param iri The IRI for the ontology.
     * @param baseIndividual The base individual used in explanations.
     * @param reasonerSelection The reasoner selection (1-5).
     */
    public ModuleHandler(Set<OWLAxiom> ontologyAxioms, IRI iri, String baseIndividual, int reasonerSelection) {
        // Store the base individual
        this.baseIndividual = baseIndividual;
        // Create ontology from the provided axioms
        OWLOntologyManager manager;
        try {
            manager = createOWLOntologyManager();
            this.ontology = manager.createOntology(ontologyAxioms, iri);
        } catch (OWLOntologyCreationException e) {
            throw new RuntimeException(e);
        }
        sanitize(this.ontology, DataAxiomSanitizer.Strategy.TO_STRING);
        // Get the data factory
        this.dataFactory = manager.getOWLDataFactory();
        // Store the reasoner selection
        this.reasonerSelection = reasonerSelection;
    }


    public ModuleHandler(Set<OWLAxiom> ontologyAxioms, int reasonerSelection) {
        this.baseIndividual = "undefined";
        // Create ontology from the provided axioms
        OWLOntologyManager manager;
        try {
            manager = createOWLOntologyManager();
            this.ontology = manager.createOntology(ontologyAxioms);
        } catch (OWLOntologyCreationException e) {
            throw new RuntimeException(e);
        }
        sanitize(this.ontology, DataAxiomSanitizer.Strategy.TO_STRING);
        // Get the data factory
        this.dataFactory = manager.getOWLDataFactory();
        // Store the reasoner selection
        this.reasonerSelection = reasonerSelection;
    }

    /**
     * Get the OWLReasonerFactory based on the reasoner selection.
     * @return The selected OWLReasonerFactory.
     */
    private OWLReasonerFactory getOwlReasonerFactory() {
        // Select the reasoner factory based on the reasonerSelection value
        OWLReasonerFactory reasonerFactory;
        if(this.reasonerSelection == 1) {
            reasonerFactory = new ReasonerFactory();
        } else if (this.reasonerSelection == 2) {
            reasonerFactory = new OpenlletReasonerFactory();
        } else if (this.reasonerSelection == 3) {
            reasonerFactory = new JFactFactory();
        } else if (this.reasonerSelection == 4) {
            reasonerFactory = new FaCTPlusPlusReasonerFactory();
        } else if (this.reasonerSelection == 5) {
            reasonerFactory = new ElkReasonerFactory();
        } else {
            throw new RuntimeException("Unrecognized reasoner selection: " + this.reasonerSelection);
        }
        return reasonerFactory;
    }

    public ConsistencyCheckOutcome isConsistentSafe(long timeoutMillis) {
        final OWLReasonerFactory rf = getOwlReasonerFactory();
        final OWLReasonerConfiguration cfg = new SimpleConfiguration(timeoutMillis); // advisory
        final AtomicReference<OWLReasoner> ref = new AtomicReference<>();
        final AtomicReference<Boolean> result = new AtomicReference<>(null);
        final AtomicReference<Throwable> failure = new AtomicReference<>(null);

        Thread t = new Thread(() -> {
            OWLReasoner r = null;
            try {
                r = rf.createReasoner(this.ontology, cfg); // creation can throw
                ref.set(r);
                result.set(r.isConsistent());
            } catch (Throwable ex) {
                failure.set(ex);
            } finally {
                if (r != null) {
                    try { r.dispose(); } catch (Exception ignored) {}
                }
            }
        }, "ConsistencyCheck");
        t.setDaemon(true);
        t.start();

        try {
            t.join(timeoutMillis);
            if (t.isAlive()) {
                // Hard timeout → request stop and clean up
                OWLReasoner r = ref.get();
                if (r != null) {
                    try { r.interrupt(); } catch (Exception ignored) {}
                    try { r.dispose();   } catch (Exception ignored) {}
                }
                t.interrupt();
                t.join(1500); // best-effort
                return new ConsistencyCheckOutcome(
                        null,
                        ConsistencyCheckOutcome.Status.TIMEOUT,
                        new TimeOutException("Consistency check exceeded " + timeoutMillis + " ms")
                );
            }
            if (failure.get() != null) {
                return new ConsistencyCheckOutcome(
                        null,
                        ConsistencyCheckOutcome.Status.FAILED,
                        failure.get()
                );
            }
            Boolean res = result.get();
            if (res == null) {
                return new ConsistencyCheckOutcome(
                        null,
                        ConsistencyCheckOutcome.Status.FAILED,
                        new RuntimeException("Consistency result not available")
                );
            }
            return new ConsistencyCheckOutcome(
                    res,
                    res ? ConsistencyCheckOutcome.Status.CONSISTENT : ConsistencyCheckOutcome.Status.INCONSISTENT,
                    null
            );
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            OWLReasoner r = ref.get();
            if (r != null) {
                try { r.interrupt(); } catch (Exception ignored) {}
                try { r.dispose();   } catch (Exception ignored) {}
            }
            return new ConsistencyCheckOutcome(
                    null,
                    ConsistencyCheckOutcome.Status.INTERRUPTED,
                    new ReasonerInterruptedException(ie)
            );
        }
    }



    public ConsistencyCheckOutcome isConsistent(long timeoutMillis) {

        return isConsistentSafe(timeoutMillis);
    }




    public ConsistencyCheckOutcome isConsistent() {

        // Return the consistency result
        return isConsistentSafe(Long.MAX_VALUE);
    }



    /**
     * Get explanations for the inconsistency of the ontology with specified timeouts and explanations limit.
     * @param perOpTimeoutMillis The timeout in milliseconds for each reasoning operation.
     * @param timeoutMillis The overall timeout in milliseconds for the explanation generation process.
     * @param explanationsLimit The maximum number of explanations to retrieve.
     * @return A set of sets of OWLAxioms, where each inner set represents an explanation for the inconsistency.
     */
    public ExplanationOutcome getExplanationsSafe(long perOpTimeoutMillis, long timeoutMillis, int explanationsLimit) {
        // Per-op (advisory) config + your existing monitor
        OWLReasonerFactory baseReasonerFactory = getOwlReasonerFactory();
        OWLReasonerConfiguration reasonerConfiguration = new SimpleConfiguration(perOpTimeoutMillis);
        CustomOwlReasonerFactory reasonerFactory = new CustomOwlReasonerFactory(baseReasonerFactory, reasonerConfiguration);

        Duration durationTimeoutMillis = java.time.Duration.ofMillis(timeoutMillis);
        CustomExplanationProgressMonitor<OWLAxiom> monitor =
                new CustomExplanationProgressMonitor<>(this.baseIndividual, durationTimeoutMillis, explanationsLimit);

        Supplier<OWLOntologyManager> ontologyManagerSupplier = OWLManager::createOWLOntologyManager;
        ExplanationGeneratorFactory<OWLAxiom> explanationGeneratorFactory =
                new InconsistentOntologyExplanationGeneratorFactory(reasonerFactory, this.dataFactory, ontologyManagerSupplier, timeoutMillis);
        ExplanationGenerator<OWLAxiom> explanationGenerator =
                explanationGeneratorFactory.createExplanationGenerator(this.ontology, monitor);

        OWLAxiom targetAxiom = this.dataFactory.getOWLSubClassOfAxiom(this.dataFactory.getOWLThing(), this.dataFactory.getOWLNothing());

        final java.util.concurrent.atomic.AtomicReference<java.util.Set<Explanation<OWLAxiom>>> foundRef = new java.util.concurrent.atomic.AtomicReference<>();
        final java.util.concurrent.atomic.AtomicReference<Throwable> failureRef = new java.util.concurrent.atomic.AtomicReference<>();

        Thread t = new Thread(() -> {
            try {
                java.util.Set<Explanation<OWLAxiom>> found =
                        explanationGenerator.getExplanations(targetAxiom, explanationsLimit);
                foundRef.set(found);
            } catch (Throwable ex) {
                failureRef.set(ex);
            }
        }, "ExplanationGen-" + (baseIndividual == null ? "unknown" : baseIndividual));
        t.setDaemon(true);
        t.start();

        try {
            t.join(timeoutMillis);
            if (t.isAlive()) {
                // Hard timeout → stop thread and return partials from the monitor
                try { t.interrupt(); } catch (Exception ignored) {}
                t.join(1500); // best-effort

                java.util.Set<java.util.Set<OWLAxiom>> partial =
                        monitor.getFoundExplanations().stream()
                                .map(Explanation::getAxioms)
                                .collect(java.util.stream.Collectors.toSet());

                return new ExplanationOutcome(
                        partial,
                        ExplanationOutcome.Status.PARTIAL_TIMEOUT,
                        new TimeOutException("Explanations exceeded " + timeoutMillis + " ms")
                );
            }

            Throwable failure = failureRef.get();
            if (failure != null) {
                java.util.Set<java.util.Set<OWLAxiom>> partial =
                        monitor.getFoundExplanations().stream()
                                .map(Explanation::getAxioms)
                                .collect(java.util.stream.Collectors.toSet());

                if (failure instanceof ExplanationGeneratorInterruptedException ||
                        failure instanceof ReasonerInterruptedException) {
                    return new ExplanationOutcome(partial, ExplanationOutcome.Status.PARTIAL_INTERRUPTED, failure);
                }
                if (failure instanceof TimeOutException) {
                    return new ExplanationOutcome(partial, ExplanationOutcome.Status.PARTIAL_TIMEOUT, failure);
                }

                // Unexpected failure; keep your previous semantics
                return new ExplanationOutcome(
                        partial.isEmpty() ? java.util.Collections.emptySet() : partial,
                        partial.isEmpty() ? ExplanationOutcome.Status.FAILED : ExplanationOutcome.Status.PARTIAL_TIMEOUT,
                        failure
                );
            }

            // Success
            java.util.Set<Explanation<OWLAxiom>> found = foundRef.get();
            java.util.Set<java.util.Set<OWLAxiom>> out = (found == null)
                    ? java.util.Collections.emptySet()
                    : found.stream().map(Explanation::getAxioms).collect(java.util.stream.Collectors.toSet());
            return new ExplanationOutcome(out, ExplanationOutcome.Status.COMPLETE, null);

        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            java.util.Set<java.util.Set<OWLAxiom>> partial =
                    monitor.getFoundExplanations().stream()
                            .map(Explanation::getAxioms)
                            .collect(java.util.stream.Collectors.toSet());
            return new ExplanationOutcome(partial, ExplanationOutcome.Status.PARTIAL_INTERRUPTED,
                    new ReasonerInterruptedException(ie));
        }
    }


    public ExplanationOutcome getExplanations(long perOpTimeoutMillis, long timeoutMillis, int explanationsLimit) {
        return getExplanationsSafe(perOpTimeoutMillis, timeoutMillis, explanationsLimit);
    }

    public ExplanationOutcome getExplanations() {
        return getExplanationsSafe(Long.MAX_VALUE, Long.MAX_VALUE, Integer.MAX_VALUE);
    }

    public ExplanationOutcome getExplanations(int explanationsLimit) {
        return getExplanationsSafe(Long.MAX_VALUE, Long.MAX_VALUE, explanationsLimit);
    }

    @Override
    public void close() {
        try {
            if (ontology != null) {
                OWLOntologyManager m = ontology.getOWLOntologyManager();
                if (m != null) m.removeOntology(ontology);
            }
        } catch (Exception ignored) {}
        ontology = null;
        dataFactory = null;
        baseIndividual = null;
    }

    /**
     * Get the ontology containing the module axioms.
     * @return The OWLOntology object.
     */
    public OWLOntology getOntology() {
        return this.ontology;
    }

    /**
     * Get the data factory for creating OWL API objects.
     * @return The OWLDataFactory object.
     */
    public OWLDataFactory getDataFactory() {
        return this.dataFactory;
    }

    /**
     * Get the reasoner selection.
     * @return The reasoner selection integer (1-5).
     */
    public int getReasonerSelection() {
        return this.reasonerSelection;
    }

    /**
     * Get the base individual used in explanations.
     * @return The base individual string.
     */
    public String getBaseIndividual() {
        return this.baseIndividual;
    }
}