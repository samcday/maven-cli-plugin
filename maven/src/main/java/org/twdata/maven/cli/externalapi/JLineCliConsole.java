package org.twdata.maven.cli.externalapi;

import org.twdata.maven.cli.CliConsole;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import jline.Completor;
import jline.ConsoleReader;
import org.apache.maven.plugin.MojoExecutionException;
import org.codehaus.plexus.util.StringUtils;
import org.twdata.maven.cli.CommandInterpreter;

public class JLineCliConsole implements CliConsole {
    private final List<String> exitCommands = Collections
            .unmodifiableList(new ArrayList<String>() {
                {
                    add("quit");
                    add("exit");
                    add("bye");
                }
            });
    private final ConsoleReader reader;

    public JLineCliConsole(InputStream in, OutputStream out, Completor completor,
            String prompt) throws MojoExecutionException {
        try {
            reader = new ConsoleReader(in, new OutputStreamWriter(out));
            reader.addCompletor(completor);
            reader.setBellEnabled(false);
            reader.setDefaultPrompt((prompt != null ? prompt : "maven2") + "> ");
        } catch (IOException ex) {
            throw new MojoExecutionException("Unable to create reader to read commands.", ex);
        }
    }

    public void startConsole(CommandInterpreter interpreter)
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
