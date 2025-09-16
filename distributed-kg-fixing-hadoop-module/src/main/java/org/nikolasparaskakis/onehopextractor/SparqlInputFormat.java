package org.nikolasparaskakis.onehopextractor;

import java.io.BufferedWriter;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.*;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.Writable;
import org.apache.hadoop.mapreduce.InputFormat;
import org.apache.hadoop.mapreduce.InputSplit;
import org.apache.hadoop.mapreduce.JobContext;
import org.apache.hadoop.mapreduce.RecordReader;
import org.apache.hadoop.mapreduce.TaskAttemptContext;

import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.repository.sparql.SPARQLRepository;

public class SparqlInputFormat extends InputFormat<LongWritable, Text> {

    // InputSplit that holds LIMIT/OFFSET info for one batch.
    public static class SparqlInputSplit extends InputSplit implements Writable {

        private long offset;
        private int batchSize;

        public SparqlInputSplit() { }

        public SparqlInputSplit(long offset, int batchSize) {
            this.offset = offset;
            this.batchSize = batchSize;
        }

        @Override
        public long getLength() {
            return batchSize;
        }

        @Override
        public String[] getLocations() {
            return new String[0];
        }

        @Override
        public void write(DataOutput out) throws IOException {
            out.writeLong(offset);
            out.writeInt(batchSize);
        }

        @Override
        public void readFields(DataInput in) throws IOException {
            offset = in.readLong();
            batchSize = in.readInt();
        }

        public long getOffset() {
            return offset;
        }

        public int getBatchSize() {
            return batchSize;
        }
    }

    // RecordReader that executes the SPARQL query for the given split.
    public static class SparqlRecordReader extends RecordReader<LongWritable, Text> {

        private final LongWritable key = new LongWritable();
        private final Text value = new Text();
        private List<String> triples;
        private int currentIndex = 0;

        private static final String FAILURE_LOG_PATH = "/tmp/failed-queries/failed-queries.log";

        @Override
        public void initialize(InputSplit genericSplit, TaskAttemptContext context) throws IOException {

            SparqlInputSplit split = (SparqlInputSplit) genericSplit;
            Configuration conf = context.getConfiguration();
            String endpoint = conf.get("sparqlEndpoint");
            String graphDomain = conf.get("graphDomain");
            int batchSize = split.getBatchSize();
            long offset = split.getOffset();

            List<String> leadsStr = new ArrayList<>(Arrays.asList(conf.getStrings("leads", new String[0])));
            List<String> classesInTBoxStr = new ArrayList<>(Arrays.asList(conf.getStrings("classesInTBox", new String[0])));
            List<String> predicatesInTBoxStr = new ArrayList<>(Arrays.asList(conf.getStrings("predicatesInTBox", new String[0])));

            String sparqlQuery = "SELECT ?s ?p ?o " +
                    "FROM <" + graphDomain + "> " +
                    "WHERE { ?s ?p ?o . } " +
                    "LIMIT " + batchSize +
                    " OFFSET " + offset;

            final int MAX_ATTEMPTS = 5;
            final int RETRY_DELAY_MS = 3000;
            int attempt = 0;

            while (attempt < MAX_ATTEMPTS) {
                SPARQLRepository repo = new SPARQLRepository(endpoint + "/sparql", endpoint + "/update");
                try {
                    repo.init();
                    try (RepositoryConnection conn = repo.getConnection()) {
                        TupleQuery query = conn.prepareTupleQuery(QueryLanguage.SPARQL, sparqlQuery);
                        triples = new ArrayList<>();
                        try (TupleQueryResult result = query.evaluate()) {
                            String RDF_TYPE = "http://www.w3.org/1999/02/22-rdf-syntax-ns#type";

                            while (result.hasNext()) {
                                try {

                                    BindingSet bindingSet = result.next();

                                    Value sVal = bindingSet.getValue("s");
                                    Value pVal = bindingSet.getValue("p");
                                    Value oVal = bindingSet.getValue("o");

                                    int consider;
                                    if (pVal.stringValue().equals(RDF_TYPE)) {
                                        if (classesInTBoxStr.contains(oVal.stringValue())) {
                                            consider = 0;
                                        } else {
                                            continue;
                                        }
                                    } else if (predicatesInTBoxStr.contains(pVal.stringValue())) {
                                        if (leadsStr.contains(pVal.stringValue())) {
                                            consider = oVal.isLiteral() ? 0 : 1;
                                        }
                                        else {
                                            consider = oVal.isLiteral() ? 0 : 2;
                                        }
                                    } else {
                                        continue;
                                    }

                                    String tripleStr = consider + " " + sVal + " " + pVal + " " + oVal;
                                    triples.add(tripleStr);
                                }
                                catch (Exception e) {
                                    // Skip malformed triple and optionally log
                                    System.err.println("Skipping malformed triple due to parsing error: " + e.getMessage());
                                }
                            }
                        } catch (Exception e) {
                            // If the whole result is broken, skip this split entirely
                            System.err.println("Skipping batch due to bad XML or RDF response: " + e.getMessage());
                            logFailedQuery(sparqlQuery, e);
                            triples.clear(); // This makes hasNext() return false
                        }

                    }

                    // If we reach here, it means success
                    break;

                } catch (Exception e) {
                    attempt++;
                    System.err.println("SPARQL query failed on attempt " + attempt + ": " + e.getMessage());

                    if (attempt >= MAX_ATTEMPTS) {
                        throw new IOException("SPARQL query failed after " + MAX_ATTEMPTS + " attempts", e);
                    }

                    try {
                        Thread.sleep(RETRY_DELAY_MS);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new IOException("Retry interrupted", ie);
                    }

                } finally {
                    repo.shutDown();
                }
            }
        }


        @Override
        public boolean nextKeyValue() {
            if (triples != null && currentIndex < triples.size()) {
                key.set(currentIndex);
                value.set(triples.get(currentIndex));
                currentIndex++;
                return true;
            }
            return false;
        }

        @Override
        public LongWritable getCurrentKey() {
            return key;
        }

        @Override
        public Text getCurrentValue() {
            return value;
        }

        @Override
        public float getProgress() {
            if (triples == null || triples.isEmpty()) {
                return 1.0f;
            }
            return (float) currentIndex / triples.size();
        }

        @Override
        public void close() { }


        private void logFailedQuery(String query, Exception e) {
            // use a synchronized block so you don’t interleave writes if multiple threads share the class
            synchronized (SparqlRecordReader.class) {
                Path path = Paths.get(FAILURE_LOG_PATH);
                try {
                    Files.createDirectories(path.getParent()); // make sure parent dir exists
                } catch (IOException ignored) { }
                try (BufferedWriter w = Files.newBufferedWriter(path,
                        StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
                    w.write("---- Failed at " + Instant.now() + " ----");
                    w.newLine();
                    w.write(query);
                    w.newLine();
                    w.write("Error: " + e.getClass().getSimpleName() + ": " + e.getMessage());
                    w.newLine();
                    w.newLine();
                } catch (IOException io) {
                    System.err.println("Unable to write failed‑query log: " + io.getMessage());
                }
            }
        }
    }

    @Override
    public List<InputSplit> getSplits(JobContext job) {
        Configuration conf = job.getConfiguration();
        long totalCount = conf.getLong("totalCount", 1000000L);
        int batchSize = conf.getInt("batchSize", 10000);
        int numSplits = (int) Math.ceil((double) totalCount / batchSize);
        List<InputSplit> splits = new ArrayList<>();
        for (int i = 0; i < numSplits; i++) {
            long offset = (long) i * batchSize;
            splits.add(new SparqlInputSplit(offset, batchSize));
        }
        return splits;
    }

    @Override
    public RecordReader<LongWritable, Text> createRecordReader(InputSplit split, TaskAttemptContext context) throws IOException {
        return new SparqlRecordReader();
    }



}
