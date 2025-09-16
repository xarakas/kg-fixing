package org.nikolasparaskakis.core;



import org.semanticweb.owlapi.model.OWLAxiom;
import java.util.Set;



public final class ExplanationOutcome {
    public enum Status {
        COMPLETE,            // all requested explanations produced
        PARTIAL_TIMEOUT,     // partial due to timeout
        PARTIAL_INTERRUPTED, // partial due to thread interruption/cancel
        FAILED               // no result due to unexpected failure
    }

    private final Set<Set<OWLAxiom>> explanations;
    private final Status status;
    private final Throwable error; // nullable

    public ExplanationOutcome(Set<Set<OWLAxiom>> explanations, Status status, Throwable error) {
        this.explanations = explanations == null ? java.util.Collections.emptySet() : explanations;
        this.status = status;
        this.error = error;
    }

    public Set<Set<OWLAxiom>> getExplanations() { return explanations; }
    public Status getStatus() { return status; }
    public Throwable getError() { return error; }
}