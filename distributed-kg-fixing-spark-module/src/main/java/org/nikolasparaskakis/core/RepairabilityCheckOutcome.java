package org.nikolasparaskakis.core;

public final class RepairabilityCheckOutcome {
    public enum Status {
        REPAIRABLE,
        UNREPAIRABLE,
        TIMEOUT,
        INTERRUPTED,
        FAILED
    }

    private final Boolean repairable; // null if not computed
    private final Status status;
    private final Throwable error;    // nullable

    public RepairabilityCheckOutcome(Boolean repairable, Status status, Throwable error) {
        this.repairable = repairable;
        this.status = status;
        this.error = error;
    }

    public Boolean getRepairable() { return repairable; }
    public Status getStatus() { return status; }
    public Throwable getError() { return error; }
}
