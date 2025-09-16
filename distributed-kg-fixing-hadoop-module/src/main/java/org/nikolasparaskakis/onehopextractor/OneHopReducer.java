package org.nikolasparaskakis.onehopextractor;

import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Reducer;

public class OneHopReducer extends Reducer<Text, Text, Text, Text> {
    private final Text result = new Text();

    @Override
    protected void reduce(Text key, Iterable<Text> values, Context context) throws IOException, InterruptedException {

        Set<String> uniqueEntries = new LinkedHashSet<>(); // preserves insertion order

        for (Text val : values) {
            String[] entries = val.toString().split(",(?![^<]*>)");
            for (String entry : entries) {
                if (!entry.isEmpty()) {
                    uniqueEntries.add(entry);
                }
            }
        }

        StringBuilder neighbors1 = new StringBuilder();
        StringBuilder neighbors2 = new StringBuilder();

        boolean first = true;
        for (String entry : uniqueEntries) {
            if (!first) {
                neighbors1.append(",");
                neighbors2.append(",");
            }

            neighbors1.append(entry);
            neighbors2.append(entry).append(":1");
            first = false;
        }

        String formattedValue;
        if (!uniqueEntries.isEmpty())
            formattedValue = "NEIGHBORS#" + neighbors1 + "|REACHABLE#" + key.toString() + ":0," + neighbors2;
        else
            formattedValue = "NEIGHBORS#" + "|REACHABLE#" + key.toString() + ":0";

        result.set(formattedValue);
        context.write(key, result);
    }
}
