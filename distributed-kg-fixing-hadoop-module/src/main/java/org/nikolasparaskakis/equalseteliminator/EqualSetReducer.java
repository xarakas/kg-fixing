package org.nikolasparaskakis.equalseteliminator;

import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Reducer;

import java.io.IOException;

public class EqualSetReducer extends Reducer<Text, Text, Text, NullWritable> {
    public void reduce(Text key, Iterable<Text> values, Context context) throws IOException, InterruptedException {
        // Emit only the first line seen for this normalized rechability set
        if (values.iterator().hasNext()) {
            context.write(values.iterator().next(), NullWritable.get());
        }
    }
}
