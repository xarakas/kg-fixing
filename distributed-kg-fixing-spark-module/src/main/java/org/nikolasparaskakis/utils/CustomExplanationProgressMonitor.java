package org.nikolasparaskakis.utils;



import org.semanticweb.owl.explanation.api.Explanation;
import org.semanticweb.owl.explanation.api.ExplanationGenerator;
import org.semanticweb.owl.explanation.api.ExplanationProgressMonitor;
import org.semanticweb.owlapi.model.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;



/**
 * Custom implementation of ExplanationProgressMonitor to track progress of explanation generation.
 * This monitor supports timeouts, maximum explanation limits, and optional axiom printing.
 * It ensures thread safety for concurrent explanation generation scenarios.
 * @param <E> The type of entailment being explained (e.g., OWLAxiom).
 */
@SuppressWarnings({"unused", "DuplicatedCode"})
public class CustomExplanationProgressMonitor<E> implements ExplanationProgressMonitor<E> {

    /**
     * The individual (as string) for which we are finding explanations.
     */
    private final String baseIndividual;

    /**
     * Deadline in nanoseconds (System.nanoTime) for cancelling the search.
     * Use Long.MAX_VALUE for no timeout.
     */
    private final long deadlineNanos;

    /**
     * Maximum number of explanations to find before cancelling the search.
     * Use Integer.MAX_VALUE for no limit.
     */
    private final int maxExplanations;

    /**
     * Whether to print found axioms to stdout as they are found.
     * Can be useful for long-running tasks to see progress.
     */
    private final boolean printAxioms;

    /**
     * How many callbacks we've seen (not necessarily unique explanations).
     */
    private final AtomicInteger callbacksSeen = new AtomicInteger(0);

    /**
     * Whether the search has been cancelled (by timeout or max explanations).
     */
    private volatile boolean cancelled = false;

    /**
     * Fingerprints of explanations we've seen so far (to avoid duplicates).
     * Uses a concurrent set for thread safety.
     */
    private final Set<String> seenKeys = ConcurrentHashMap.newKeySet();

    /**
     * Explanations we've found so far (in order of discovery).
     * Uses a concurrent queue for thread safety.
     */
    private final ConcurrentLinkedQueue<Explanation<E>> foundExplanations = new ConcurrentLinkedQueue<>();

    /**
     * Creates a monitor with no timeout and no explanation limit.
     * Axioms will be printed to stdout as they are found.
     * @param baseIndividual The individual (as string) for which we are finding explanations.
     */
    public CustomExplanationProgressMonitor(String baseIndividual) {
        this(baseIndividual, Duration.ZERO, Integer.MAX_VALUE, true);
    }

    /**
     * Creates a monitor with the given timeout and explanation limit.
     * Axioms will be printed to stdout as they are found.
     * @param baseIndividual The individual (as string) for which we are finding explanations.
     * @param timeout Maximum duration to search for explanations. Use null or zero/negative for no timeout.
     * @param maxExplanations Maximum number of explanations to find. Use zero or negative for no limit.
     */
    public CustomExplanationProgressMonitor(String baseIndividual, Duration timeout, int maxExplanations) {
        this(baseIndividual, timeout, maxExplanations, true);
    }

    /**
     * Creates a monitor with the given timeout, explanation limit, and axiom printing option.
     * @param baseIndividual The individual (as string) for which we are finding explanations.
     * @param timeout Maximum duration to search for explanations. Use null or zero/negative for no timeout.
     * @param maxExplanations Maximum number of explanations to find. Use zero or negative for no limit.
     * @param printAxioms Whether to print found axioms to stdout as they are found.
     */
    public CustomExplanationProgressMonitor(String baseIndividual, Duration timeout, int maxExplanations, boolean printAxioms) {
        this.baseIndividual = baseIndividual;
        this.deadlineNanos = (timeout == null || timeout.isZero() || timeout.isNegative())
                ? Long.MAX_VALUE
                : System.nanoTime() + timeout.toNanos();
        this.maxExplanations = maxExplanations <= 0 ? Integer.MAX_VALUE : maxExplanations;
        this.printAxioms = printAxioms;
    }

    /**
     * Callback method invoked when an explanation is found.
     * @param generator The explanation generator that found the explanation.
     * @param explanation The explanation that was found (maybe null).
     * @param allFoundExplanations All explanations found so far (maybe null).
     */
    @Override
    public void foundExplanation(ExplanationGenerator<E> generator, Explanation<E> explanation, Set<Explanation<E>> allFoundExplanations) {

        Explanation<E> toRecord = explanation;

        // If null, try to find a new one we haven't recorded yet
        if (toRecord == null && allFoundExplanations != null) {
            toRecord = allFoundExplanations.stream()
                    .filter(ex -> seenKeys.add(fingerprint(ex)))
                    .findFirst().orElse(null);
        }

        // If we have a concrete explanation, store it once (by fingerprint)
        if (toRecord != null) {
            String key = fingerprint(toRecord);
            if (seenKeys.add(key)) {
                foundExplanations.add(toRecord);
            }
        }

        int seen = callbacksSeen.incrementAndGet();

        if (printAxioms && toRecord != null) {
            String axioms = toRecord.getAxioms().stream()
                    .map(String::valueOf)
                    .collect(Collectors.joining("\n  - "));
            System.out.println("##################################################");
            System.out.println("Found explanation (callback #" + seen + ") for " + baseIndividual);
            System.out.println("Axioms:\n  - " + axioms);
        }

        // Cancel if we reached either the explanation cap or the time cap
        if (foundExplanations.size() >= maxExplanations) {
            cancelled = true;
            System.out.println("[Monitor] Reached max explanations (" + maxExplanations + "). Cancelling search.");
            return;
        }
        if (System.nanoTime() >= deadlineNanos) {
            cancelled = true;
            System.out.println("[Monitor] Timeout reached. Cancelling search.");
        }
    }

    /**
     * Whether the explanation search should be cancelled.
     * This will return true if the search has been explicitly cancelled,
     * or if the timeout or max explanations limit has been reached.
     * @return true if the search should be cancelled, false otherwise.
     */
    @Override
    public boolean isCancelled() {
        if (!cancelled && System.nanoTime() >= deadlineNanos) {
            cancelled = true;
        }
        return cancelled;
    }

    public void cancel() {
        this.cancelled = true;
    }

    /**
     * How many callbacks we've seen (not necessarily unique explanations).
     * @return Number of callbacks seen so far.
     */
    public int getCallbackCount() {
        return callbacksSeen.get();
    }

    /**
     * Gets the set of unique explanations found so far.
     * The returned set is a snapshot and modifications to it will not affect the monitor's state.
     * @return Set of unique explanations found so far.
     */
    public Set<Explanation<E>> getFoundExplanations() {
        return new HashSet<>(foundExplanations);
    }

    /**
     * Gets the set of unique axiom sets found so far.
     * Each explanation is represented as a set of axioms.
     * The returned set is a snapshot and modifications to it will not affect the monitor's state.
     * @return Set of unique axiom sets found so far.
     */
    public Set<Set<OWLAxiom>> getFoundAxiomSets() {
        return foundExplanations.stream()
                .map(Explanation::getAxioms)
                .map(HashSet::new)
                .collect(Collectors.toSet());
    }

    /**
     * Generates a fingerprint string for an explanation based on its axioms.
     * The fingerprint is a sorted concatenation of the string representations of the axioms,
     * separated by "||". This allows for easy comparison of explanations.
     * @param ex The explanation to generate a fingerprint for.
     * @return Fingerprint string representing the explanation.
     */
    private String fingerprint(Explanation<E> ex) {
        return ex.getAxioms().stream()
                .map(String::valueOf)
                .sorted()
                .collect(Collectors.joining("||"));
    }

    /**
     * Gets the base individual (as string) for which explanations are being found.
     * @return The base individual string.
     */
    public String getBaseIndividual() {
        return baseIndividual;
    }

    /**
     * Gets the deadline in nanoseconds (System.nanoTime) for cancelling the search.
     * @return Deadline in nanoseconds.
     */
    public long getDeadlineNanos() {
        return deadlineNanos;
    }

    /**
     * Gets the maximum number of explanations to find before cancelling the search.
     * @return Maximum number of explanations.
     */
    public int getMaxExplanations() {
        return maxExplanations;
    }

    /**
     * Gets whether found axioms are printed to stdout as they are found.
     * @return true if axioms are printed, false otherwise.
     */
    public boolean isPrintAxioms() {
        return printAxioms;
    }

    /**
     * Gets how many callbacks we've seen (not necessarily unique explanations).
     * @return AtomicInteger representing the number of callbacks seen.
     */
    public AtomicInteger getCallbacksSeen() {
        return callbacksSeen;
    }

    /**
     * Gets the set of fingerprints of explanations we've seen so far (to avoid duplicates).
     * @return Set of explanation fingerprints.
     */
    public Set<String> getSeenKeys() {
        return seenKeys;
    }
}