package org.twdata.maven.cli;

import org.apache.maven.plugin.MojoExecutionException;

public interface CliConsole {
    void startConsole(CommandInterpreter interpreter) throws MojoExecutionException;
}
