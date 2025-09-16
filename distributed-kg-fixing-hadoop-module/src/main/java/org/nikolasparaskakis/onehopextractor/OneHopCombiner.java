package org.nikolasparaskakis.onehopextractor;

import java.io.IOException;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Reducer;

public class OneHopCombiner extends Reducer<Text, Text, Text, Text> {
    private final Text result = new Text();

    @Override
    protected void reduce(Text key, Iterable<Text> values, Context context) throws IOException, InterruptedException {
        StringBuilder neighbors = new StringBuilder();
        boolean first = true;

        for (Text val : values) {
            if (val.toString().isEmpty()) continue;
            if (!first) neighbors.append(",");
            neighbors.append(val);
            first = false;
        }

        result.set(neighbors.toString());
        context.write(key, result);
    }
}
