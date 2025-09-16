package org.nikolasparaskakis.io;

import java.io.Closeable;
import java.io.IOException;

public class PartitionLogger implements Closeable, AutoCloseable {

    private final HDFSIO io;

    private final String moduleEventLogsFilePath;
    private final String moduleExplanationsLogsFilePath;
    private final String moduleFixesLogsFilePath;
    private final String moduleInnerRoundNumLogsFilePath;
    private final String moduleReasoningLogsFilePath;
    private final String moduleSizeLogsFilePath;

    private final String binEventLogsFilePath;
    private final String binExplanationsLogsFilePath;
    private final String binFixesLogsFilePath;
    private final String binInnerRoundNumLogsFilePath;
    private final String binReasoningLogsFilePath;
    private final String binSizeLogsFilePath;

    private final int flushEvery;

    private final StringBuilder moduleEventLogsBuffer = new StringBuilder();
    private final StringBuilder moduleExplanationsLogsBuffer  = new StringBuilder();
    private final StringBuilder moduleFixesLogsBuffer = new StringBuilder();
    private final StringBuilder moduleInnerRoundNumLogsBuffer = new StringBuilder();
    private final StringBuilder moduleReasoningLogsBuffer = new StringBuilder();
    private final StringBuilder moduleSizeLogsBuffer = new StringBuilder();

    private final StringBuilder binEventLogsBuffer = new StringBuilder();
    private final StringBuilder binExplanationsLogsBuffer  = new StringBuilder();
    private final StringBuilder binFixesLogsBuffer = new StringBuilder();
    private final StringBuilder binInnerRoundNumLogsBuffer = new StringBuilder();
    private final StringBuilder binReasoningLogsBuffer = new StringBuilder();
    private final StringBuilder binSizeLogsBuffer = new StringBuilder();

    private int counter = 0;

    public PartitionLogger(HDFSIO io,
                           String moduleEventLogsDir,
                           String moduleExplanationsLogsDir,
                           String moduleFixesLogsDir,
                           String moduleInnerRoundNumLogsDir,
                           String moduleReasoningLogsDir,
                           String moduleSizeLogsDir,
                           String binEventLogsDir,
                           String binExplanationsLogsDir,
                           String binFixesLogsDir,
                           String binInnerRoundNumLogsDir,
                           String binReasoningLogsDir,
                           String binSizeLogsDir,
                           String partitionFileName,
                           int flushEvery) {
        this.io = io;
        this.moduleEventLogsFilePath = moduleEventLogsDir + "/" + partitionFileName;
        this.moduleExplanationsLogsFilePath = moduleExplanationsLogsDir + "/" + partitionFileName;
        this.moduleFixesLogsFilePath = moduleFixesLogsDir + "/" + partitionFileName;
        this.moduleInnerRoundNumLogsFilePath = moduleInnerRoundNumLogsDir + "/" + partitionFileName;
        this.moduleReasoningLogsFilePath = moduleReasoningLogsDir + "/" + partitionFileName;
        this.moduleSizeLogsFilePath = moduleSizeLogsDir + "/" + partitionFileName;
        this.binEventLogsFilePath = binEventLogsDir + "/" + partitionFileName;
        this.binExplanationsLogsFilePath = binExplanationsLogsDir + "/" + partitionFileName;
        this.binFixesLogsFilePath = binFixesLogsDir + "/" + partitionFileName;
        this.binInnerRoundNumLogsFilePath = binInnerRoundNumLogsDir + "/" + partitionFileName;
        this.binReasoningLogsFilePath = binReasoningLogsDir + "/" + partitionFileName;
        this.binSizeLogsFilePath = binSizeLogsDir + "/" + partitionFileName;
        this.flushEvery = Math.max(1, flushEvery);
    }

    public void addModuleEventLog(String jsonLine) {
        moduleEventLogsBuffer.append(jsonLine).append('\n');
        tick();
    }

    public void addModuleExplanationsLog(String jsonLine) {
        moduleExplanationsLogsBuffer.append(jsonLine).append('\n');
        tick();
    }

    public void addModuleFixesLog(String jsonLine) {
        moduleFixesLogsBuffer.append(jsonLine).append('\n');
        tick();
    }

    public void addModuleInnerRoundNumLog(String jsonLine) {
        moduleInnerRoundNumLogsBuffer.append(jsonLine).append('\n');
        tick();
    }

    public void addModuleReasoningLog(String jsonLine) {
        moduleReasoningLogsBuffer.append(jsonLine).append('\n');
        tick();
    }

    public void addModuleSizeLog(String jsonLine) {
        moduleSizeLogsBuffer.append(jsonLine).append('\n');
        tick();
    }

    public void addBinEventLog(String jsonLine) {
        binEventLogsBuffer.append(jsonLine).append('\n');
        tick();
    }

    public void addBinExplanationsLog(String jsonLine) {
        binExplanationsLogsBuffer.append(jsonLine).append('\n');
        tick();
    }

    public void addBinFixesLog(String jsonLine) {
        binFixesLogsBuffer.append(jsonLine).append('\n');
        tick();
    }

    public void addBinInnerRoundNumLog(String jsonLine) {
        binInnerRoundNumLogsBuffer.append(jsonLine).append('\n');
        tick();
    }

    public void addBinReasoningLog(String jsonLine) {
        binReasoningLogsBuffer.append(jsonLine).append('\n');
        tick();
    }

    public void addBinSizeLog(String jsonLine) {
        binSizeLogsBuffer.append(jsonLine).append('\n');
        tick();
    }

    private void tick() {
        counter++;
        if (counter % flushEvery == 0) {
            flush();
        }
    }

    public void flush() {
        if (moduleEventLogsBuffer.length() > 0) {
            io.writeStringToHDFS(moduleEventLogsBuffer.toString(), moduleEventLogsFilePath);
            moduleEventLogsBuffer.setLength(0);
        }
        if (moduleExplanationsLogsBuffer.length() > 0) {
            io.writeStringToHDFS(moduleExplanationsLogsBuffer.toString(), moduleExplanationsLogsFilePath);
            moduleExplanationsLogsBuffer.setLength(0);
        }
        if (moduleFixesLogsBuffer.length() > 0) {
            io.writeStringToHDFS(moduleFixesLogsBuffer.toString(), moduleFixesLogsFilePath);
            moduleFixesLogsBuffer.setLength(0);
        }
        if (moduleInnerRoundNumLogsBuffer.length() > 0) {
            io.writeStringToHDFS(moduleInnerRoundNumLogsBuffer.toString(), moduleInnerRoundNumLogsFilePath);
            moduleInnerRoundNumLogsBuffer.setLength(0);
        }
        if (moduleReasoningLogsBuffer.length() > 0) {
            io.writeStringToHDFS(moduleReasoningLogsBuffer.toString(), moduleReasoningLogsFilePath);
            moduleReasoningLogsBuffer.setLength(0);
        }
        if (moduleSizeLogsBuffer.length() > 0) {
            io.writeStringToHDFS(moduleSizeLogsBuffer.toString(), moduleSizeLogsFilePath);
            moduleSizeLogsBuffer.setLength(0);
        }
        if (binEventLogsBuffer.length() > 0) {
            io.writeStringToHDFS(binEventLogsBuffer.toString(), binEventLogsFilePath);
            binEventLogsBuffer.setLength(0);
        }
        if (binExplanationsLogsBuffer.length() > 0) {
            io.writeStringToHDFS(binExplanationsLogsBuffer.toString(), binExplanationsLogsFilePath);
            binExplanationsLogsBuffer.setLength(0);
        }
        if (binFixesLogsBuffer.length() > 0) {
            io.writeStringToHDFS(binFixesLogsBuffer.toString(), binFixesLogsFilePath);
            binFixesLogsBuffer.setLength(0);
        }
        if (binInnerRoundNumLogsBuffer.length() > 0) {
            io.writeStringToHDFS(binInnerRoundNumLogsBuffer.toString(), binInnerRoundNumLogsFilePath);
            binInnerRoundNumLogsBuffer.setLength(0);
        }
        if (binReasoningLogsBuffer.length() > 0) {
            io.writeStringToHDFS(binReasoningLogsBuffer.toString(), binReasoningLogsFilePath);
            binReasoningLogsBuffer.setLength(0);
        }
        if (binSizeLogsBuffer.length() > 0) {
            io.writeStringToHDFS(binSizeLogsBuffer.toString(), binSizeLogsFilePath);
            binSizeLogsBuffer.setLength(0);
        }
    }

    @Override public void close() throws IOException {
        flush(); // final flush on scope exit
    }
}
