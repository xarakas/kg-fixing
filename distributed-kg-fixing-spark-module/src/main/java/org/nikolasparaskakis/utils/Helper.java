package org.nikolasparaskakis.utils;



import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaRDD;
import org.nikolasparaskakis.core.Fix;
import org.nikolasparaskakis.logs.bin.BinExplanationsLog;
import org.nikolasparaskakis.logs.bin.BinFixesLog;
import org.nikolasparaskakis.logs.global.ExplanationLog;
import org.nikolasparaskakis.logs.global.FixesLog;
import org.nikolasparaskakis.logs.module.ModuleExplanationsLog;
import org.nikolasparaskakis.logs.module.ModuleFixesLog;
import org.semanticweb.owlapi.model.OWLAxiom;
import scala.Tuple2;
import java.net.URI;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
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
     * @param baseIndividual The base individual associated with the module explanations.
     * @return A formatted string representing the module explanations.
     */
    public static String moduleExplanationsString(List<List<OWLAxiom>> sortedExplanations, String baseIndividual) {
        StringBuilder logBuilder = new StringBuilder();
        logBuilder.append("Inconsistency explanations of module ").append(baseIndividual).append(":\n");
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
     * Checks if a directory exists in HDFS.
     * @param dirPathStr The HDFS directory path (absolute or relative to the HDFS location).
     * @return True if the directory exists, false otherwise.
     */
    public static boolean hdfsDirectoryExists(String dirPathStr) {
        try {
            Configuration hadoopConf = new Configuration();
            hadoopConf.set("fs.defaultFS", "hdfs://localhost:9000"); // or your actual HDFS URI

            FileSystem fs = FileSystem.get(new URI("hdfs://localhost:9000"), hadoopConf);

            Path dirPath = new Path(dirPathStr);

            return fs.exists(dirPath) && fs.getFileStatus(dirPath).isDirectory();

        } catch (Exception e) {
            System.out.println(e.getMessage());
            return false;
        }
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
     * @param fixesOfModule The set of sequences of fixes for the module.
     */
    public static void updateFixesOfModule(
            Fix fix,
            Set<ArrayList<Fix>> fixesOfModule) {

        OWLAxiom old_axiom = fix.getOldAxiom();
        OWLAxiom new_axiom = fix.getNewAxiom();

        if (new_axiom != null) {
            boolean fix_added = false;

            if (!fixesOfModule.isEmpty()) {
                for (ArrayList<Fix> fixList : fixesOfModule) {
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
                fixesOfModule.add(tmp);
                fix_added = true;
            }
        }
        else {
            ArrayList<Fix> tmp = new ArrayList<>();
            tmp.add(fix);
            fixesOfModule.add(tmp);
        }
    }

    /**
     * Merges multiple JSON strings representing ModuleExplanationsLog objects into a single RDD of unique ExplanationLog JSON strings.
     * The merging process involves deduplicating inner lists of explanations and flattening them into a set of unique explanations.
     * Each unique explanation is then converted into an ExplanationLog object and serialized back to a JSON string.
     * @param lines A JavaRDD of JSON strings, each representing a ModuleExplanationsLog object.
     * @return A JavaRDD of JSON strings, each representing a unique ExplanationLog object.
     */
    public static JavaRDD<String> mergeModuleExplanations(JavaRDD<String> lines) {
        // FlatMap: parse each line, deduplicate inner lists, and flatten to sets
        AtomicInteger outerRound = new AtomicInteger();
        JavaRDD<Set<OWLAxiom>> uniqueExplanations = lines.flatMap(line -> {
            ModuleExplanationsLog log = ModuleExplanationsLog.fromJsonString(line);
            outerRound.set(log.getOuterRound());
            List<List<OWLAxiom>> explanations = log.getExplanations();
            // Deduplicate inner lists
            List<Set<OWLAxiom>> dedupedInner = explanations.stream()
                    .map(HashSet::new)
                    .collect(Collectors.toList());
            return dedupedInner.iterator();
        }).distinct(); // Deduplicate outer list

        // Map each unique set to ExplanationLog and serialize to JSON
        return uniqueExplanations.map(set -> {
            ExplanationLog explanationLog = new ExplanationLog(outerRound.get(), sortExplanation(set));
            return explanationLog.toJsonString();
        });
    }

    public static JavaRDD<String> mergeBinExplanations(JavaRDD<String> lines) {
        // FlatMap: parse each line, deduplicate inner lists, and flatten to sets
        AtomicInteger outerRound = new AtomicInteger();
        JavaRDD<Set<OWLAxiom>> uniqueExplanations = lines.flatMap(line -> {
            BinExplanationsLog log = BinExplanationsLog.fromJsonString(line);
            outerRound.set(log.getOuterRound());
            List<List<OWLAxiom>> explanations = log.getExplanations();
            // Deduplicate inner lists
            List<Set<OWLAxiom>> dedupedInner = explanations.stream()
                    .map(HashSet::new)
                    .collect(Collectors.toList());
            return dedupedInner.iterator();
        }).distinct(); // Deduplicate outer list

        // Map each unique set to ExplanationLog and serialize to JSON
        return uniqueExplanations.map(set -> {
            ExplanationLog explanationLog = new ExplanationLog(outerRound.get(), sortExplanation(set));
            return explanationLog.toJsonString();
        });
    }

    /**
     * Merges multiple JSON strings representing FixesLog objects into a single RDD of unique FixesLog JSON strings.
     * The merging process involves grouping fixes by their first axiom, collecting all sequences of fixes,
     * and creating a FixesLog object for each group. Each FixesLog object is then serialized back to a JSON string.
     * @param lines A JavaRDD of JSON strings, each representing a FixesLog object.
     * @return A JavaRDD of JSON strings, each representing a merged FixesLog object.
     */
    public static JavaRDD<String> mergeModuleFixes(JavaRDD<String> lines) {
        AtomicInteger outerRound = new AtomicInteger();
        // FlatMap each line to (firstAxiom, List<Fix>) pairs
        JavaPairRDD<String, ArrayList<ObjectNode>> axiomToFixes = lines.flatMapToPair(line -> {
            ModuleFixesLog log = ModuleFixesLog.fromJsonString(line);
            outerRound.set(log.getOuterRound());
            Set<ArrayList<Fix>> moduleFixes = log.getFixes();
            List<Tuple2<String, ArrayList<ObjectNode>>> out = new ArrayList<>();
            for (ArrayList<Fix> fixList : moduleFixes) {
                if (!fixList.isEmpty()) {
                    String firstAxiom = fixList.get(0).getOldAxiom().toString();
                    ArrayList<ObjectNode> jsonFixes = new ArrayList<>();
                    for (Fix f : fixList) {
                        jsonFixes.add(f.toObjectNode());
                    }
                    out.add(new Tuple2<>(firstAxiom, jsonFixes));
                }
            }
            return out.iterator();
        });

        // Group by firstAxiom, collect all lists of fixes
        JavaPairRDD<String, Iterable<ArrayList<ObjectNode>>> grouped = axiomToFixes.groupByKey();

        // For each group, create a FixesLog and serialize to JSON
        return grouped.map(pair -> {
            Set<ArrayList<Fix>> fixesSet = new HashSet<>();
            for (ArrayList<ObjectNode> fixList : pair._2) {
                ArrayList<Fix> tmp = new ArrayList<>();
                for (ObjectNode tmp1 : fixList) {
                    tmp.add(Fix.fromObjectNode(tmp1));
                }
                fixesSet.add(tmp);
            }
            // Use outerRound = 0 or any value, as outerRound is not tracked per group here
            FixesLog fixesLog = new FixesLog(outerRound.get(), fixesSet);
            return fixesLog.toJsonString();
        });
    }


    public static JavaRDD<String> mergeBinFixes(JavaRDD<String> lines) {
        AtomicInteger outerRound = new AtomicInteger();
        // FlatMap each line to (firstAxiom, List<Fix>) pairs
        JavaPairRDD<String, ArrayList<ObjectNode>> axiomToFixes = lines.flatMapToPair(line -> {
            BinFixesLog log = BinFixesLog.fromJsonString(line);
            outerRound.set(log.getOuterRound());
            Set<ArrayList<Fix>> moduleFixes = log.getFixes();
            List<Tuple2<String, ArrayList<ObjectNode>>> out = new ArrayList<>();
            for (ArrayList<Fix> fixList : moduleFixes) {
                if (!fixList.isEmpty()) {
                    String firstAxiom = fixList.get(0).getOldAxiom().toString();
                    ArrayList<ObjectNode> jsonFixes = new ArrayList<>();
                    for (Fix f : fixList) {
                        jsonFixes.add(f.toObjectNode());
                    }
                    out.add(new Tuple2<>(firstAxiom, jsonFixes));
                }
            }
            return out.iterator();
        });

        // Group by firstAxiom, collect all lists of fixes
        JavaPairRDD<String, Iterable<ArrayList<ObjectNode>>> grouped = axiomToFixes.groupByKey();

        // For each group, create a FixesLog and serialize to JSON
        return grouped.map(pair -> {
            Set<ArrayList<Fix>> fixesSet = new HashSet<>();
            for (ArrayList<ObjectNode> fixList : pair._2) {
                ArrayList<Fix> tmp = new ArrayList<>();
                for (ObjectNode tmp1 : fixList) {
                    tmp.add(Fix.fromObjectNode(tmp1));
                }
                fixesSet.add(tmp);
            }
            // Use outerRound = 0 or any value, as outerRound is not tracked per group here
            FixesLog fixesLog = new FixesLog(outerRound.get(), fixesSet);
            return fixesLog.toJsonString();
        });
    }
}
