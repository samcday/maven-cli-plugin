package org.twdata.maven.cli;

import java.io.IOException;

public interface CliConsole {
    String readLine() throws IOException;
}
