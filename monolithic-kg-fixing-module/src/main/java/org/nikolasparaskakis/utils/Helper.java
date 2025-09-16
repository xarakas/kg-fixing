package org.nikolasparaskakis.utils;



import org.nikolasparaskakis.core.Fix;
import org.semanticweb.owlapi.model.OWLAxiom;
import java.util.*;
import java.util.stream.Collectors;



/**
 * A utility class providing helper methods for various operations such as
 * extracting neighbor maps, sorting explanations, handling HDFS directories,
 * and merging logs.
 */
@SuppressWarnings({"unused", "DuplicatedCode", "UnusedAssignment"})
public class Helper {

    /**
     * Extracts a map of neighbor individuals and their hop counts from a given line of text.
     * The line is expected to be formatted with tab and pipe delimiters, where the neighbors
     * are listed after the "REACHABLE#" prefix.
     * @param line The input line containing the base individual and its neighbors.
     * @return A HashMap where keys are neighbor individual strings and values are their corresponding hop counts as integers.
     */
    public static HashMap<String, Integer> extractNeighborsMap(String line) {
        HashMap<String, Integer> neighborsMap = new HashMap<>();
        String[] lineParts = line.split("\\t");
        String[] recordParts = lineParts[1].split("\\|");
        String[] neighbors = recordParts[1].substring("REACHABLE#".length()).split(",(?![^<>]*>)");

        // Add ABox assertions of the neighbours of the base individual in module's ontology
        for (String neighbor : neighbors) {
            String[] neighborHop = neighbor.split(":(?![^<>]*>)");
            String neighborToAdd = neighborHop[0];
            String hopToAdd = neighborHop[1];
            if (hopToAdd.equals("0")) {
                continue;
            }
            neighborsMap.put(neighborToAdd, Integer.parseInt(hopToAdd));
        }

        return neighborsMap;
    }

    /**
     * Sorts a set of explanations, where each explanation is a set of OWLAxioms.
     * The sorting is done first by converting each explanation set to a list,
     * then sorting the axioms within each explanation lexicographically by their string representation.
     * @param explanations A set of explanations, each represented as a set of OWLAxioms.
     * @return A list of explanations, where each explanation is a sorted list of OWLAxioms.
     */
    public static List<List<OWLAxiom>> sortExplanations(Set<Set<OWLAxiom>> explanations) {
        return explanations.stream()
                .map(explanation -> explanation.stream()
                        .sorted(Comparator.comparing(OWLAxiom::toString))
                        .collect(Collectors.toList()))
                .collect(Collectors.toList());
    }

    public static List<OWLAxiom> sortExplanation(Set<OWLAxiom> explanation) {
        return explanation.stream()
                .sorted(Comparator.comparing(OWLAxiom::toString))
                .collect(Collectors.toList());
    }

    /**
     * Generates a formatted string representation of module explanations.
     * Each explanation is listed with its axioms indented for clarity.
     * @param sortedExplanations A list of explanations, where each explanation is a list of OWLAxioms.
     * @return A formatted string representing the module explanations.
     */
    public static String moduleExplanationsString(List<List<OWLAxiom>> sortedExplanations) {
        StringBuilder logBuilder = new StringBuilder();
        logBuilder.append("Inconsistency explanations:\n");
        sortedExplanations.forEach(e -> {
            logBuilder.append("Explanation:\n");
            e.forEach(ax -> logBuilder.append("  ").append(ax).append("\n"));
        });
        return logBuilder.toString();
    }

    /**
     * Extracts the base individual string from a given line of text.
     * The line is expected to be tab-delimited, with the base individual as the first part.
     * @param line The input line containing the base individual and other data.
     * @return The base individual string extracted from the line.
     */
    public static String extractBaseIndividualStr(String line) {
        String[] lineParts = line.split("\\t");
        return lineParts[0];
    }

    /**
     * Removes the leading and trailing quotes from a given string.
     * @param str The input string with leading and trailing quotes.
     * @return The string without the leading and trailing quotes.
     */
    public static String removeQuotes(String str) {
        return str.substring(1, str.length()-1);
    }

    /**
     * Updates the set of fixes for a module by adding a new fix to the appropriate sequence.
     * If the new fix's old axiom matches the new axiom of the last fix in an existing sequence,
     * it appends the new fix to that sequence. Otherwise, it creates a new sequence for the new fix.
     * @param fix The new fix to be added.
     * @param fixesOfOntology The set of sequences of fixes for the module.
     */
    public static void updateFixesOfOntology(
            Fix fix,
            Set<ArrayList<Fix>> fixesOfOntology) {

        OWLAxiom old_axiom = fix.getOldAxiom();
        OWLAxiom new_axiom = fix.getNewAxiom();

        if (new_axiom != null) {
            boolean fix_added = false;

            if (!fixesOfOntology.isEmpty()) {
                for (ArrayList<Fix> fixList : fixesOfOntology) {
                    if (!fixList.isEmpty()) {
                        Fix lastFix = fixList.get(fixList.size() - 1);
                        if (lastFix.getNewAxiom().equals(old_axiom)) {
                            fixList.add(fix);
                            fix_added = true;
                        }
                    }
                }
            }

            if (!fix_added) {
                ArrayList<Fix> tmp = new ArrayList<>();
                tmp.add(fix);
                fixesOfOntology.add(tmp);
                fix_added = true;
            }
        }
        else {
            ArrayList<Fix> tmp = new ArrayList<>();
            tmp.add(fix);
            fixesOfOntology.add(tmp);
        }
    }
}
