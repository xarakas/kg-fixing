package org.nikolasparaskakis.io;

import java.io.Closeable;
import java.io.IOException;

public class MyLogger implements Closeable, AutoCloseable {

    private final LocalFSIO io;

    private final String myEventLogsFilePath;
    private final String myExplanationsLogsFilePath;
    private final String myFixesLogsFilePath;
    private final String myInnerRoundNumLogsFilePath;
    private final String myReasoningLogsFilePath;
    private final String mySizeLogsFilePath;

    private final int flushEvery;

    private final StringBuilder myEventLogsBuffer = new StringBuilder();
    private final StringBuilder myExplanationsLogsBuffer  = new StringBuilder();
    private final StringBuilder myFixesLogsBuffer = new StringBuilder();
    private final StringBuilder myInnerRoundNumLogsBuffer = new StringBuilder();
    private final StringBuilder myReasoningLogsBuffer = new StringBuilder();
    private final StringBuilder mySizeLogsBuffer = new StringBuilder();

    private int counter = 0;

    public MyLogger(LocalFSIO io,
                           String myEventLogsDir,
                           String myExplanationsLogsDir,
                           String myFixesLogsDir,
                           String myInnerRoundNumLogsDir,
                           String myReasoningLogsDir,
                           String mySizeLogsDir,
                           String fileName,
                           int flushEvery) {
        this.io = io;
        this.myEventLogsFilePath = myEventLogsDir + "/" + fileName;
        this.myExplanationsLogsFilePath = myExplanationsLogsDir + "/" + fileName;
        this.myFixesLogsFilePath = myFixesLogsDir + "/" + fileName;
        this.myInnerRoundNumLogsFilePath = myInnerRoundNumLogsDir + "/" + fileName;
        this.myReasoningLogsFilePath = myReasoningLogsDir + "/" + fileName;
        this.mySizeLogsFilePath = mySizeLogsDir + "/" + fileName;
        this.flushEvery = Math.max(1, flushEvery);
    }

    public void addMyEventLog(String jsonLine) {
        myEventLogsBuffer.append(jsonLine).append('\n');
        tick();
    }

    public void addMyExplanationsLog(String jsonLine) {
        myExplanationsLogsBuffer.append(jsonLine).append('\n');
        tick();
    }

    public void addMyFixesLog(String jsonLine) {
        myFixesLogsBuffer.append(jsonLine).append('\n');
        tick();
    }

    public void addMyInnerRoundNumLog(String jsonLine) {
        myInnerRoundNumLogsBuffer.append(jsonLine).append('\n');
        tick();
    }

    public void addMyReasoningLog(String jsonLine) {
        myReasoningLogsBuffer.append(jsonLine).append('\n');
        tick();
    }

    public void addMySizeLog(String jsonLine) {
        mySizeLogsBuffer.append(jsonLine).append('\n');
        tick();
    }

    private void tick() {
        counter++;
        if (counter % flushEvery == 0) {
            flush();
        }
    }

    public void flush() {
        if (myEventLogsBuffer.length() > 0) {
            io.writeStringToHDFS(myEventLogsBuffer.toString(), myEventLogsFilePath);
            myEventLogsBuffer.setLength(0);
        }
        if (myExplanationsLogsBuffer.length() > 0) {
            io.writeStringToHDFS(myExplanationsLogsBuffer.toString(), myExplanationsLogsFilePath);
            myExplanationsLogsBuffer.setLength(0);
        }
        if (myFixesLogsBuffer.length() > 0) {
            io.writeStringToHDFS(myFixesLogsBuffer.toString(), myFixesLogsFilePath);
            myFixesLogsBuffer.setLength(0);
        }
        if (myInnerRoundNumLogsBuffer.length() > 0) {
            io.writeStringToHDFS(myInnerRoundNumLogsBuffer.toString(), myInnerRoundNumLogsFilePath);
            myInnerRoundNumLogsBuffer.setLength(0);
        }
        if (myReasoningLogsBuffer.length() > 0) {
            io.writeStringToHDFS(myReasoningLogsBuffer.toString(), myReasoningLogsFilePath);
            myReasoningLogsBuffer.setLength(0);
        }
        if (mySizeLogsBuffer.length() > 0) {
            io.writeStringToHDFS(mySizeLogsBuffer.toString(), mySizeLogsFilePath);
            mySizeLogsBuffer.setLength(0);
        }
    }

    @Override public void close() throws IOException {
        flush(); // final flush on scope exit
    }
}
