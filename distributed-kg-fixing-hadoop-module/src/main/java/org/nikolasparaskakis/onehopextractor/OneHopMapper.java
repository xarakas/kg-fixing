package org.nikolasparaskakis.onehopextractor;

import java.io.IOException;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;

public class OneHopMapper extends Mapper<LongWritable, Text, Text, Text> {

    private final Text outKey = new Text();
    private final Text outValue = new Text();

    @Override
    protected void map(LongWritable key, Text value, Context context) throws IOException, InterruptedException {
        // Assume each line is in the form: subject predicate object
        String line = value.toString().trim();
        if (line.isEmpty()) return;

        // Simple split; adjust parsing if using a full RDF parser.
        String[] parts = line.split(" ");
        if (parts.length != 4) return; // Skip malformed lines

        String consider = parts[0];
        String subject = parts[1];
//        String predicate = parts[2];
        String object = parts[3];

        // 0: emit (subject, )
        // 1: emit (subject, object) and (object, subject)
        // 2: emit (subject, ) and (object, )

        if (consider.trim().equals("1")) {
            if (subject.equals(object)) {
                outKey.set("<"+subject+">");
                outValue.set("<"+object+">");
                context.write(outKey, outValue);
            }
            else {
                outKey.set("<"+subject+">");
                outValue.set("<"+object+">");
                context.write(outKey, outValue);

                outKey.set("<"+object+">");
                outValue.set("<"+subject+">");
                context.write(outKey, outValue);
            }

        }
        else if (consider.trim().equals("2")) {
            outKey.set("<"+subject+">");
            outValue.set("");
            context.write(outKey, outValue);

            outKey.set("<"+object+">");
            outValue.set("");
            context.write(outKey, outValue);
        }
        else
        {
            outKey.set("<"+subject+">");
            outValue.set("");
            context.write(outKey, outValue);
        }
    }
}
