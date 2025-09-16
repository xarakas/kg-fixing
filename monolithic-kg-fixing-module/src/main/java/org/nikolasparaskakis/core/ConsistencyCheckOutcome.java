package org.nikolasparaskakis.core;



public final class ConsistencyCheckOutcome {
    public enum Status {
        CONSISTENT,     // computed and consistent
        INCONSISTENT,   // computed and inconsistent
        TIMEOUT,        // hard wall-clock timeout hit
        INTERRUPTED,    // caller/thread interrupted
        FAILED;          // unexpected failure

        public RepairabilityCheckOutcome.Status toRepairabilityCheckOutcomeStatus() {
            switch (this) {
                case CONSISTENT:
                    return RepairabilityCheckOutcome.Status.REPAIRABLE;
                case INCONSISTENT:
                    return RepairabilityCheckOutcome.Status.UNREPAIRABLE;
                case TIMEOUT:
                    return RepairabilityCheckOutcome.Status.TIMEOUT;
                case INTERRUPTED:
                    return RepairabilityCheckOutcome.Status.INTERRUPTED;
                case FAILED:
                default:
                    return RepairabilityCheckOutcome.Status.FAILED;
            }
        }
    }

    private final Boolean consistent; // null if not computed
    private final Status status;
    private final Throwable error;    // nullable

    public ConsistencyCheckOutcome(Boolean consistent, Status status, Throwable error) {
        this.consistent = consistent;
        this.status = status;
        this.error = error;
    }

    public Boolean getConsistent() { return consistent; }
    public Status getStatus() { return status; }
    public Throwable getError() { return error; }
}
