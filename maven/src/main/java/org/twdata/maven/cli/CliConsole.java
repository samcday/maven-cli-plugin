package org.twdata.maven.cli;

public interface CliConsole {
    String readLine();
    void writeInfo(String info);
    void writeError(String error);
    void writeDebug(String debug);
}
