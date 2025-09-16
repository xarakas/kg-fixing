package org.nikolasparaskakis.io;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.*;

import java.io.*;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for interacting with Hadoop Distributed File System (HDFS).
 *
 * <p><b>Spark-safe note:</b> All self-contained methods use
 * FileSystem.newInstance(URI, Configuration) so closing them will NOT
 * close Hadoop’s cached FileSystem used by Spark’s input readers.</p>
 *
 * <p><b>Important:</b> If you obtain BufferedReaders from this class, make sure
 * you close the readers you get back. Do NOT call FileSystem.closeAll() in executors.</p>
 */
@SuppressWarnings({"unused", "DuplicatedCode"})
public class HDFSIO {

    private final String hdfsLocation;          // as given
    private final String baseWithSlash;         // normalized

    public HDFSIO(String hdfsLocation) {
        this.hdfsLocation = hdfsLocation;
        this.baseWithSlash = hdfsLocation.endsWith("/") ? hdfsLocation : hdfsLocation + "/";
    }

    private Configuration baseConf() {
        Configuration conf = new Configuration();
        conf.set("fs.defaultFS", this.hdfsLocation);
        return conf;
    }

    // Make a Path absolute against the normalized base URI.
    private Path abs(String pathStr) {
        Path p = new Path(pathStr);
        // If caller already passed an absolute path ("/...") or fully-qualified URI ("hdfs://..."),
        // just use it as-is.
        if (p.isAbsolute() || pathStr.startsWith("hdfs://") || pathStr.startsWith("s3a://") || pathStr.startsWith("file:")) {
            return p;
        }
        // Ensure no double slashes
        String rel = pathStr.startsWith("/") ? pathStr.substring(1) : pathStr;
        return new Path(baseWithSlash + rel);
    }

    /** Writes one line to HDFS (append if exists, else create). */
    public void writeStringToHDFS(String line, String filePathStr) {
        Configuration conf = baseConf();
        try (FileSystem fs = FileSystem.newInstance(new URI(this.hdfsLocation), conf)) {
            Path filePath = abs(filePathStr);
            Path parent = filePath.getParent();
            if (parent != null && !fs.exists(parent)) {
                fs.mkdirs(parent);
            }

            final boolean exists = fs.exists(filePath);
            try (FSDataOutputStream out = exists ? fs.append(filePath) : fs.create(filePath, true);
                 BufferedWriter writer = new BufferedWriter(
                         new OutputStreamWriter(out, StandardCharsets.UTF_8))) {

                writer.write(line);
                if (line.isEmpty() || line.charAt(line.length() - 1) != '\n') {
                    writer.write('\n');
                }
                writer.flush();
                out.hflush();
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to write to HDFS: " + filePathStr, e);
        }
    }

    /** Writes a block of text to HDFS (append if exists, else create). No extra '\n' is added. */
    public void writeBlockToHDFS(String content, String filePathStr) {
        Configuration conf = baseConf();
        try (FileSystem fs = FileSystem.newInstance(new URI(this.hdfsLocation), conf)) {
            Path filePath = abs(filePathStr);
            Path parent = filePath.getParent();
            if (parent != null && !fs.exists(parent)) fs.mkdirs(parent);

            final boolean exists = fs.exists(filePath);
            try (FSDataOutputStream out = exists ? fs.append(filePath) : fs.create(filePath, true);
                 BufferedWriter writer = new BufferedWriter(
                         new OutputStreamWriter(out, StandardCharsets.UTF_8))) {

                writer.write(content);   // no extra '\n'
                writer.flush();
                out.hflush();
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to write to HDFS: " + filePathStr, e);
        }
    }

    /** Reads the entire file content as a single String. */
    public String readStringFromHDFS(String filePathStr) {
        Configuration conf = baseConf();
        StringBuilder content = new StringBuilder();
        try (FileSystem fs = FileSystem.newInstance(new URI(this.hdfsLocation), conf)) {
            Path filePath = abs(filePathStr);
            if (!fs.exists(filePath)) {
                throw new FileNotFoundException("File does not exist: " + filePath);
            }
            try (FSDataInputStream in = fs.open(filePath);
                 BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    content.append(line).append('\n');
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to read file: " + filePathStr, e);
        }
        return content.toString();
    }

    /** Reads the entire file into a list of lines. */
    public ArrayList<String> readStringsFromHDFS(String filePathStr) {
        Configuration conf = baseConf();
        ArrayList<String> lines = new ArrayList<>();
        try (FileSystem fs = FileSystem.newInstance(new URI(this.hdfsLocation), conf)) {
            Path filePath = abs(filePathStr);
            if (!fs.exists(filePath)) {
                throw new FileNotFoundException("File does not exist: " + filePath);
            }
            try (FSDataInputStream in = fs.open(filePath);
                 BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    lines.add(line);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to read lines: " + filePathStr, e);
        }
        return lines;
    }

    /**
     * Returns a BufferedReader for reading a single HDFS file.
     * <p><b>Caller must close the returned reader.</b></p>
     * <p><b>Warning:</b> This method uses the cached FileSystem via {@code get()} because we
     * return an open stream. Do NOT close the FileSystem in executor code; just close the reader.</p>
     */
    public BufferedReader getBufferedReaderForHDFS(String filePathStr) {
        try {
            Configuration conf = baseConf();
            FileSystem fs = FileSystem.get(new URI(this.hdfsLocation), conf); // cached
            Path filePath = abs(filePathStr);
            if (!fs.exists(filePath)) {
                throw new FileNotFoundException("File does not exist: " + filePath);
            }
            FSDataInputStream in = fs.open(filePath);
            return new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new RuntimeException("Failed to open reader for: " + filePathStr, e);
        }
    }

    /**
     * Returns readers for all files in a directory (non-recursive).
     * <p><b>Caller must close every returned reader.</b></p>
     * <p><b>Warning:</b> Same note as above regarding not closing the cached FileSystem.</p>
     */
    public List<BufferedReader> getBufferedReadersForHDFSDirectory(String dirPathStr) {
        List<BufferedReader> readers = new ArrayList<>();
        try {
            Configuration conf = baseConf();
            FileSystem fs = FileSystem.get(new URI(this.hdfsLocation), conf); // cached
            Path dirPath = abs(dirPathStr);
            if (!fs.exists(dirPath) || !fs.getFileStatus(dirPath).isDirectory()) {
                throw new FileNotFoundException("Directory does not exist: " + dirPath);
            }
            for (FileStatus status : fs.listStatus(dirPath)) {
                if (status.isFile()) {
                    FSDataInputStream in = fs.open(status.getPath());
                    readers.add(new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8)));
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to open readers for dir: " + dirPathStr, e);
        }
        return readers;
    }

    /** Deletes a single file. */
    public boolean deleteFile(String filePathStr) {
        Configuration conf = baseConf();
        try (FileSystem fs = FileSystem.newInstance(new URI(this.hdfsLocation), conf)) {
            return fs.delete(abs(filePathStr), false);
        } catch (Exception e) {
            throw new RuntimeException("Failed to delete file: " + filePathStr, e);
        }
    }

    /** Deletes a directory recursively. */
    public boolean deleteDirectory(String dirPathStr) {
        Configuration conf = baseConf();
        try (FileSystem fs = FileSystem.newInstance(new URI(this.hdfsLocation), conf)) {
            return fs.delete(abs(dirPathStr), true);
        } catch (Exception e) {
            throw new RuntimeException("Failed to delete directory: " + dirPathStr, e);
        }
    }

    /** Creates a directory (and parents). */
    public boolean createDirectory(String dirPathStr) {
        Configuration conf = baseConf();
        try (FileSystem fs = FileSystem.newInstance(new URI(this.hdfsLocation), conf)) {
            return fs.mkdirs(abs(dirPathStr));
        } catch (Exception e) {
            throw new RuntimeException("Failed to create directory: " + dirPathStr, e);
        }
    }

    /** Returns true if the directory exists and contains at least one file. */
    public boolean hasFiles(String dirPathStr) {
        Configuration conf = baseConf();
        try (FileSystem fs = FileSystem.newInstance(new URI(this.hdfsLocation), conf)) {
            Path dirPath = abs(dirPathStr);
            if (!fs.exists(dirPath)) {
                return false;
            }
            for (FileStatus status : fs.listStatus(dirPath)) {
                if (status.isFile()) return true;
            }
            return false;
        } catch (Exception e) {
            System.err.println("hasFiles error: " + e.getMessage());
            return false;
        }
    }

    public String getHdfsLocation() {
        return hdfsLocation;
    }

    /**
     * Moves the single part file from a Spark saveAsTextFile() output directory
     * to a final file path, and cleans up the temporary directory.
     *
     * @param tmpSingleDir Temporary directory created by saveAsTextFile
     * @param finalFilePath Destination file (e.g. ".../merged-explanations.ndjson")
     * @throws IOException if anything goes wrong
     */
    public void moveSinglePartFile(String tmpSingleDir, String finalFilePath) throws IOException {
        Configuration conf = baseConf();
        try (FileSystem fs = FileSystem.newInstance(new java.net.URI(this.hdfsLocation), conf)) {
            // normalize both
            Path srcDir  = abs(tmpSingleDir);
            Path dstFile = abs(finalFilePath);

            // find part-*
            org.apache.hadoop.fs.FileStatus[] files = fs.listStatus(srcDir);
            Path partFile = null;
            for (org.apache.hadoop.fs.FileStatus f : files) {
                String name = f.getPath().getName();
                if (name.startsWith("part-")) { partFile = f.getPath(); break; }
            }
            if (partFile == null) {
                throw new IOException("No part file found in " + srcDir);
            }

            // ensure destination parent exists
            Path dstParent = dstFile.getParent();
            if (dstParent != null && !fs.exists(dstParent)) {
                fs.mkdirs(dstParent);
            }

            // overwrite if exists
            if (fs.exists(dstFile)) {
                fs.delete(dstFile, false);
            }

            // move (fast metadata op); fallback to copy if needed
            if (!fs.rename(partFile, dstFile)) {
                boolean copied = org.apache.hadoop.fs.FileUtil.copy(fs, partFile, fs, dstFile, true, conf);
                if (!copied) {
                    throw new IOException("Failed to move " + partFile + " to " + dstFile);
                }
            }

            // cleanup temp dir
            fs.delete(srcDir, true);
        } catch (Exception e) {
            throw new IOException("Error finalizing Spark output from " + tmpSingleDir +
                    " to " + finalFilePath, e);
        }
    }
}
