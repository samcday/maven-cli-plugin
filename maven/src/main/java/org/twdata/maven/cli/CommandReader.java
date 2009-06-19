package org.twdata.maven.cli;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import jline.ConsoleReader;
import org.apache.maven.plugin.MojoExecutionException;
import org.codehaus.plexus.util.StringUtils;

class CommandReader {
    private final List<String> exitCommands = Collections
            .unmodifiableList(new ArrayList<String>() {
                {
                    add("quit");
                    add("exit");
                    add("bye");
                }
            });
    private final ConsoleReader reader;

    public CommandReader(List<String> availableCommands, String prompt)
            throws MojoExecutionException {
        try {
            reader = new ConsoleReader(System.in, new OutputStreamWriter(System.out));
            availableCommands.addAll(exitCommands);
            reader.addCompletor(new CommandsCompletor(availableCommands));
            reader.setBellEnabled(false);
            reader.setDefaultPrompt((prompt != null ? prompt : "maven2") + "> ");
        } catch (IOException ex) {
            throw new MojoExecutionException("Unable to create reader to read commands.", ex);
        }
    }

    public void startListening(CommandInterpreter interpreter)
            throws MojoExecutionException {
        String line;

        try {
            while ((line = reader.readLine()) != null) {
                if (StringUtils.isEmpty(line)) {
                    continue;
                } else if (exitCommands.contains(line)) {
                    break;
                } else {
                    interpreter.interpretCommand(line);
                }
            }
        } catch (IOException e) {
            throw new MojoExecutionException("Unable to execute cli commands",
                    e);
        }
    }
}
