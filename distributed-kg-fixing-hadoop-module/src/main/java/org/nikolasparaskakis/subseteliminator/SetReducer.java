package org.nikolasparaskakis.subseteliminator;

//import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Reducer;

import java.io.IOException;
import java.util.*;

public class SetReducer extends Reducer<Text, Text, Text, Text> {
    private List<String> normalizedSetStrings = new ArrayList<>();
    private Map<String, String> setToOriginalLine = new LinkedHashMap<>(); // now uses string keys

    public void reduce(Text key, Iterable<Text> values, Context context) {
        normalizedSetStrings.clear();
        setToOriginalLine.clear();

        for (Text val : values) {
            String[] parts = val.toString().split("###");
            String[] elements = parts[0].split(",(?![^<]*>)");

            TreeSet<String> normalized = new TreeSet<>(Arrays.asList(elements)); // TreeSet to ensure order
            String normalizedSetStr = String.join(",", normalized);
            String originalLine = parts[1];

            // Avoid duplicate keys (same set string)
            if (!setToOriginalLine.containsKey(normalizedSetStr)) {
                normalizedSetStrings.add(normalizedSetStr);
                setToOriginalLine.put(normalizedSetStr, originalLine);
            }
        }

        HashMap<String, String> outputLines = new HashMap<>();

        for (int i = 0; i < normalizedSetStrings.size(); i++) {
            Set<String> s1 = new HashSet<>(Arrays.asList(normalizedSetStrings.get(i).split(",(?![^<]*>)")));
            boolean isSubset = false;

            for (int j = 0; j < normalizedSetStrings.size(); j++) {
                if (i == j) continue;
                Set<String> s2 = new HashSet<>(Arrays.asList(normalizedSetStrings.get(j).split(",(?![^<]*>)")));
                if (s2.containsAll(s1) && s2.size() > s1.size()) {
                    isSubset = true;
                    break;
                }
            }

            if (!isSubset) {
                outputLines.put(normalizedSetStrings.get(i), setToOriginalLine.get(normalizedSetStrings.get(i)));
            }
        }

        // Sort the keys (normalized sets) for consistent output order
        List<String> sortedKeys = new ArrayList<>(outputLines.keySet());
        Collections.sort(sortedKeys);  // optional: remove if sorting not needed

        for (String normalizedSet : sortedKeys) {
            String originalLine = outputLines.get(normalizedSet);
            try {
                context.write(new Text(normalizedSet), new Text(originalLine));
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
