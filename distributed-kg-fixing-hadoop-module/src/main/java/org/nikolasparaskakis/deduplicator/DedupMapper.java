package org.nikolasparaskakis.deduplicator;

import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;

import java.io.IOException;
import java.util.Set;
import java.util.TreeSet;

public class DedupMapper extends Mapper<LongWritable, Text, Text, Text> {
    private Text outKey = new Text();
    private Text outValue = new Text();

    public void map(LongWritable key, Text value, Context context) throws IOException, InterruptedException {
        String line = value.toString().trim();
//        if (!line.contains("REACHABLE#")) return;

        String[] parts = line.split("\t",2);
        if (parts.length < 2) return;

//        String[] sections = parts[1].split("\\|");
//        if (sections.length < 2 || !sections[1].startsWith("REACHABLE#")) return;

//        String reachablePart = sections[1].replace("REACHABLE#", "").trim();
//
//        Set<String> reachableSet = new TreeSet<>();
//        if (!reachablePart.isEmpty()) {
//            String[] entries = reachablePart.split(",(?![^<]*>)");
//            for (String entry : entries) {
//                String[] pair = entry.split(":(?![^<]*>)");
//                if (pair.length > 0) {
//                    reachableSet.add(pair[0].trim());
//                }
//            }
//        }
//
//        if (reachableSet.isEmpty()) return;

//        String normalizedSet = String.join(",", reachableSet);
        outKey.set(parts[0]);     // key: normalized reachable set
        outValue.set(parts[1]);            // value: full original line
        context.write(outKey, outValue);
    }
}
