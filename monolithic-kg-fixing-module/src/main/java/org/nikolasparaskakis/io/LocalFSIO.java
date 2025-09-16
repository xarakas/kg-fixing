package org.nikolasparaskakis.io;

import java.io.*;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for interacting with the local filesystem.
 *
 * <p><b>Note:</b> This is a drop-in replacement for HDFSIO — method names and signatures
 * are intentionally identical so you can switch implementations without changing callers.</p>
 */
@SuppressWarnings({"unused", "DuplicatedCode"})
public class LocalFSIO {

    private final String baseLocation;     // as given (can be ".", "/data", "file:/data", etc.)
    private final Path basePath;           // normalized base path

    public LocalFSIO(String baseLocation) {
        this.baseLocation = baseLocation;
        // Support "file:/..." URIs, absolute paths, or relative paths
        if (baseLocation.startsWith("file:")) {
            this.basePath = Paths.get(URI.create(baseLocation)).normalize();
        } else {
            this.basePath = Paths.get(baseLocation).normalize();
        }
    }

    // Make a Path absolute against the normalized base path.
    private Path abs(String pathStr) {
        Path p;
        if (pathStr.startsWith("file:")) {
            p = Paths.get(URI.create(pathStr));
        } else {
            p = Paths.get(pathStr);
        }
        if (p.isAbsolute()) {
            return p.normalize();
        }
        return basePath.resolve(p).normalize();
    }

    /** Writes one line to local FS (append if exists, else create). Ensures a trailing '\n'. */
    public void writeStringToHDFS(String line, String filePathStr) {
        Path filePath = abs(filePathStr);
        try {
            Path parent = filePath.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            // Create if missing, append if exists
            try (BufferedWriter writer = Files.newBufferedWriter(
                    filePath,
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND)) {

                writer.write(line);
                if (line.isEmpty() || line.charAt(line.length() - 1) != '\n') {
                    writer.write('\n');
                }
                writer.flush();
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to write to file: " + filePath, e);
        }
    }

    /** Writes a block of text (append if exists, else create). No extra '\n' is added. */
    public void writeBlockToHDFS(String content, String filePathStr) {
        Path filePath = abs(filePathStr);
        try {
            Path parent = filePath.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            try (BufferedWriter writer = Files.newBufferedWriter(
                    filePath,
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND)) {

                writer.write(content); // no extra newline
                writer.flush();
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to write to file: " + filePath, e);
        }
    }

    /** Reads the entire file content as a single String. */
    public String readStringFromHDFS(String filePathStr) {
        Path filePath = abs(filePathStr);
        try {
            if (!Files.exists(filePath)) {
                throw new FileNotFoundException("File does not exist: " + filePath);
            }
            // Read all lines and join with '\n' to match HDFS version behavior
            List<String> lines = Files.readAllLines(filePath, StandardCharsets.UTF_8);
            if (lines.isEmpty()) return "";
            StringBuilder sb = new StringBuilder();
            for (String l : lines) sb.append(l).append('\n');
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("Failed to read file: " + filePath, e);
        }
    }

    /** Reads the entire file into a list of lines. */
    public ArrayList<String> readStringsFromHDFS(String filePathStr) {
        Path filePath = abs(filePathStr);
        try {
            if (!Files.exists(filePath)) {
                throw new FileNotFoundException("File does not exist: " + filePath);
            }
            return new ArrayList<>(Files.readAllLines(filePath, StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new RuntimeException("Failed to read lines: " + filePath, e);
        }
    }

    /**
     * Returns a BufferedReader for reading a single local file.
     * <p><b>Caller must close the returned reader.</b></p>
     */
    public BufferedReader getBufferedReaderForHDFS(String filePathStr) {
        Path filePath = abs(filePathStr);
        try {
            if (!Files.exists(filePath)) {
                throw new FileNotFoundException("File does not exist: " + filePath);
            }
            return Files.newBufferedReader(filePath, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Failed to open reader for: " + filePath, e);
        }
    }

    /**
     * Returns readers for all files in a directory (non-recursive).
     * <p><b>Caller must close every returned reader.</b></p>
     */
    public List<BufferedReader> getBufferedReadersForHDFSDirectory(String dirPathStr) {
        Path dirPath = abs(dirPathStr);
        List<BufferedReader> readers = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dirPath)) {
            if (!Files.exists(dirPath) || !Files.isDirectory(dirPath)) {
                throw new FileNotFoundException("Directory does not exist: " + dirPath);
            }
            for (Path p : stream) {
                if (Files.isRegularFile(p)) {
                    readers.add(Files.newBufferedReader(p, StandardCharsets.UTF_8));
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to open readers for dir: " + dirPath, e);
        }
        return readers;
    }

    /** Deletes a single file. */
    public boolean deleteFile(String filePathStr) {
        Path p = abs(filePathStr);
        try {
            return Files.deleteIfExists(p);
        } catch (Exception e) {
            throw new RuntimeException("Failed to delete file: " + p, e);
        }
    }

    /** Deletes a directory recursively. */
    public boolean deleteDirectory(String dirPathStr) {
        Path dir = abs(dirPathStr);
        if (!Files.exists(dir)) return false;
        try {
            Files.deleteIfExists(dir);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
        return true;
    }

    /** Creates a directory (and parents). */
    public boolean createDirectory(String dirPathStr) {
        Path dir = abs(dirPathStr);
        try {
            Files.createDirectories(dir);
            return true;
        } catch (Exception e) {
            throw new RuntimeException("Failed to create directory: " + dir, e);
        }
    }

    /** Returns true if the directory exists and contains at least one file. */
    public boolean hasFiles(String dirPathStr) {
        Path dir = abs(dirPathStr);
        try {
            if (!Files.exists(dir) || !Files.isDirectory(dir)) return false;
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
                for (Path p : stream) {
                    if (Files.isRegularFile(p)) return true;
                }
            }
            return false;
        } catch (Exception e) {
            System.err.println("hasFiles error: " + e.getMessage());
            return false;
        }
    }

    public String getHdfsLocation() {
        // kept for drop-in compatibility with HDFSIO
        return baseLocation;
    }

    /**
     * Moves the single part file from a Spark saveAsTextFile()-like output directory
     * to a final file path, and cleans up the temporary directory.
     *
     * @param tmpSingleDir   Temporary directory created by saveAsTextFile
     * @param finalFilePath  Destination file (e.g. ".../merged-explanations.ndjson")
     * @throws IOException if anything goes wrong
     */
    public void moveSinglePartFile(String tmpSingleDir, String finalFilePath) throws IOException {
        Path srcDir  = abs(tmpSingleDir);
        Path dstFile = abs(finalFilePath);

        if (!Files.exists(srcDir) || !Files.isDirectory(srcDir)) {
            throw new IOException("Source temp dir does not exist or is not a directory: " + srcDir);
        }

        // find part-*
        Path partFile = null;
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(srcDir, "part-*")) {
            for (Path p : stream) {
                if (Files.isRegularFile(p)) { partFile = p; break; }
            }
        }

        if (partFile == null) {
            // fallback: scan all entries if glob failed
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(srcDir)) {
                for (Path p : stream) {
                    if (Files.isRegularFile(p) && p.getFileName().toString().startsWith("part-")) {
                        partFile = p; break;
                    }
                }
            }
        }

        if (partFile == null) {
            throw new IOException("No part file found in " + srcDir);
        }

        // ensure destination parent exists
        Path dstParent = dstFile.getParent();
        if (dstParent != null) Files.createDirectories(dstParent);

        // overwrite if exists
        if (Files.exists(dstFile)) {
            Files.delete(dstFile);
        }

        // move atomically if possible; fall back to REPLACE_EXISTING
        try {
            Files.move(partFile, dstFile, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(partFile, dstFile, StandardCopyOption.REPLACE_EXISTING);
        }

        // cleanup temp dir
        deleteDirectory(srcDir.toString());
    }
}
