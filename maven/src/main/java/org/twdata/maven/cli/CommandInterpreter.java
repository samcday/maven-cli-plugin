package org.twdata.maven.cli;

import org.apache.maven.plugin.MojoExecutionException;

interface CommandInterpreter {
    void interpretCommand(String command) throws MojoExecutionException;
}
