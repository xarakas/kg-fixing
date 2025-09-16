package org.nikolasparaskakis.khopextractor;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;

import java.io.IOException;
import java.util.*;


public class KHopMapper extends Mapper<LongWritable, Text, Text, Text> {
    private int maxHops;
    private int currentIteration;

    @Override
    protected void setup(Context context) {
        Configuration conf = context.getConfiguration();
        maxHops = conf.getInt("maxHops", 2);
        currentIteration = conf.getInt("iterationNumber", 2);
    }

    @Override
    public void map(LongWritable key, Text value, Context context) throws IOException, InterruptedException {

        String line = value.toString().trim();
        if (line.isEmpty())
            return;

        String[] parts = line.split("\\t");
        if (parts.length != 2)
            return;

        String nodeId = parts[0];

        String recordStr = parts[1];

        String neighborsStr = "";
        String reachableStr = "";

        String[] tokens = recordStr.split("\\|");
        if (tokens.length != 2)
            return;

        for (String token : tokens) {
            if (token.startsWith("NEIGHBORS#")) {
                neighborsStr = token.substring("NEIGHBORS#".length());
            } else if (token.startsWith("REACHABLE#")) {
                reachableStr = token.substring("REACHABLE#".length());
            }
        }

        List<String> neighbors = new ArrayList<>();
        if (!neighborsStr.isEmpty()) {
            neighbors = Arrays.asList(neighborsStr.split(",(?![^<]*>)"));
        }

        neighbors.replaceAll(String::trim);

        Map<String, Integer> reachable = new HashMap<>();
        if (!reachableStr.isEmpty()) {
            String[] entries = reachableStr.split(",(?![^<]*>)");
            for (String entry : entries) {
                String[] pair = entry.split(":(?![^<>]*>)");
                if (pair.length == 2) {
                    try {
                        int hop = Integer.parseInt(pair[1]);
                        reachable.put(pair[0], hop);
                    } catch(NumberFormatException e) {
                        // skip malformed entry
                    }
                }
            }
        }

        String nodeRecord = "NEIGHBORS#" + neighborsStr + "|" +
                "REACHABLE#" + reachableStr;
        context.write(new Text(nodeId), new Text("NODE\t" + nodeRecord));


        for (Map.Entry<String, Integer> entry : reachable.entrySet()) {
            String source = entry.getKey();
            int hop = entry.getValue();
            int newHop = hop + 1;
            if (newHop <= maxHops) {
                for (String nbr : neighbors) {
                    if (newHop >= currentIteration+2)
                        context.write(new Text(nbr), new Text("UPDATE\t" + source + ":" + newHop));
                }
            }
        }
    }
}
