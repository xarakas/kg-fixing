package org.nikolasparaskakis.khopextractor;

import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Reducer;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * The Reducer receives all values for a node.
 * It first reads the node's original record (if available) to recover the neighbor list
 * and existing reachable set, then processes all candidate updates.
 * For each source, it keeps the minimum hop count.
 * If the reachable set changes, it increments a counter.
 */
public class KHopReducer extends Reducer<Text, Text, Text, Text> {

    @Override
    public void reduce(Text key, Iterable<Text> values, Context context) throws IOException, InterruptedException {

        String neighborsStr = "";
        Map<String, Integer> oldReachableMap = new HashMap<>();
        Map<String, Integer> newReachableMap = new HashMap<>();

        // Process all incoming values for this node.
        for (Text val : values) {
            String s = val.toString();
            if (s.startsWith("NODE\t")) {
                // This is the original node record.
                String record = s.substring("NODE\t".length());
                String[] parts = record.split("\\|");
                for (String part : parts) {
                    if (part.startsWith("NEIGHBORS#")) {
                        neighborsStr = part.substring("NEIGHBORS#".length());
                    } else if (part.startsWith("REACHABLE#")) {
                        String rStr = part.substring("REACHABLE#".length());
                        if (!rStr.isEmpty()) {
                            String[] entries = rStr.split(",(?![^<]*>)");
                            for (String entry : entries) {
                                String[] pair = entry.split(":(?![^<>]*>)");
                                if (pair.length == 2) {
                                    try {
                                        int hop = Integer.parseInt(pair[1]);
                                        oldReachableMap.put(pair[0], hop);
//                                        newReachableMap.put(pair[0], hop); // initialize new map with old values
                                    } catch(NumberFormatException e) {
                                        // skip malformed entry
                                    }
                                }
                            }
                        }
                    }
                }
            }
            else if (s.startsWith("UPDATE\t")) {
                // This is a candidate update in the form "source:hop".
                String update = s.substring("UPDATE\t".length());
                String[] pair = update.split(":(?![^<>]*>)");
                if (pair.length == 2) {
                    try {
                        int hop = Integer.parseInt(pair[1]);
                        // update newReachableMap if necessary
                        if (!newReachableMap.containsKey(pair[0]) || hop < newReachableMap.get(pair[0])) {
                            newReachableMap.put(pair[0], hop);
                        }
                    }
                    catch(NumberFormatException e) {
                        // skip malformed entry
                    }
                }
            }
        }

        Map<String, Integer> updatedReachableMap = new HashMap<>(oldReachableMap);

        newReachableMap.forEach((source, hop) -> {
            if (updatedReachableMap.containsKey(source)) {
                if (hop < updatedReachableMap.get(source))
                    updatedReachableMap.replace(source, hop);
            }
            else {
                updatedReachableMap.put(source, hop);
            }
        });

        // Compare newReachableMap with oldReachableMap.
        if (!updatedReachableMap.equals(oldReachableMap)) {
            context.getCounter("KHop", "NUM_UPDATES").increment(1);
        }

        // Reconstruct the reachable set as a string.
        StringBuilder reachableStrBuilder = new StringBuilder();
        boolean first = true;
        for (Map.Entry<String, Integer> entry : updatedReachableMap.entrySet()) {
            if (!first) {
                reachableStrBuilder.append(",");
            }
            reachableStrBuilder.append(entry.getKey()).append(":").append(entry.getValue());
            first = false;
        }
        // Build the updated record in the same format.
        String updatedRecord = "NEIGHBORS#" + neighborsStr + "|" + "REACHABLE#" + reachableStrBuilder;
        context.write(key, new Text(updatedRecord));
    }
}