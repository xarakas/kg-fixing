package org.nikolasparaskakis.khopextractor;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Counter;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

public class KHopExtractor {

    public static void main(String[] args) throws Exception {

        if (args.length != 5) {
            System.err.println("Usage: KHopExtractor " +
                    "<hdfsLocation> " +
                    "<inputFilePath> " +
                    "<outputDirectoryPath> " +
                    "<maxHops> " +
                    "<maxIterations>");
            System.exit(1);
        }

        String hdfsLocation = args[0];
        String inputFilePath = args[1];
        String outputDirectoryPath = args[2];
        int maxHops = Integer.parseInt(args[3]);
        int maxIterations = Integer.parseInt(args[4]);

        int iteration = 0;
        String in = hdfsLocation + "/" + inputFilePath;
        String out;

        while (iteration < maxIterations) {
            out = hdfsLocation + outputDirectoryPath + "/kHopLinksIter" + iteration;

            boolean hasUpdates = runSingleIteration(in, out, maxHops, iteration);
            if (!hasUpdates) {
                System.out.println("Convergence reached at iteration " + iteration);
                break;
            }

            in = out;
            iteration++;
        }

        FileSystem fs = FileSystem.get(new Configuration());
        String lastIterOutput = hdfsLocation + outputDirectoryPath + "/kHopLinksIter" + iteration;
        String finalOutput = hdfsLocation + outputDirectoryPath + "/kHopLinks";

        // Cleanup intermediate files
        for (int i = 0; i < iteration; i++) {
            Path intermediate = new Path(hdfsLocation + outputDirectoryPath + "/kHopLinksIter" + i);
            if (fs.exists(intermediate)) {
                if(!fs.delete(intermediate, true))
                    System.err.println("Failed to delete intermediate output " + intermediate);
            }
        }

        // Rename final result
        Path finalPath = new Path(lastIterOutput);
        Path renamedPath = new Path(finalOutput);
        if (!fs.rename(finalPath, renamedPath)) {
            System.err.println("Failed to rename final output to: " + renamedPath);
        }
    }



    private static boolean runSingleIteration(String inputPath, String outputPath, int maxHops, int iteration) throws Exception {

        Configuration conf = new Configuration();

        if (maxHops == -1) {
            conf.setInt("maxHops", Integer.MAX_VALUE);
        } else {
            conf.setInt("maxHops", maxHops);
        }

        conf.setInt("iterationNumber", iteration);

        Job job = Job.getInstance(conf, "Job - Extract K-Hop Links Iteration " + iteration);
        job.setJarByClass(KHopExtractor.class);
        job.setMapperClass(KHopMapper.class);
        job.setReducerClass(KHopReducer.class);
        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(Text.class);

        FileInputFormat.addInputPath(job, new Path(inputPath));
        FileOutputFormat.setOutputPath(job, new Path(outputPath));

        boolean success = job.waitForCompletion(true);
        if (!success)
            return false;

        Counter counter = job.getCounters().findCounter("KHop", "NUM_UPDATES");
        long updates = counter.getValue();

        return updates > 0;
    }
}
