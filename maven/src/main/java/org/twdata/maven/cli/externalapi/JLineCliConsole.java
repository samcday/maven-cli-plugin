package org.twdata.maven.cli.externalapi;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import jline.Completor;
import jline.ConsoleReader;
import org.apache.maven.plugin.MojoExecutionException;
import org.twdata.maven.cli.CliConsole;

public class JLineCliConsole implements CliConsole {
    private final ConsoleReader consoleReader;

    public JLineCliConsole(InputStream in, OutputStream out, Completor completor,
            String prompt) throws MojoExecutionException {
        try {
            consoleReader = new ConsoleReader(in, new OutputStreamWriter(out));
            consoleReader.addCompletor(completor);
            consoleReader.setBellEnabled(false);
            consoleReader.setDefaultPrompt((prompt != null ? prompt : "maven2") + "> ");
        } catch (IOException ex) {
            throw new MojoExecutionException("Unable to create reader to read commands.", ex);
        }
    }

    @Override
    public String readLine() throws IOException {
        return consoleReader.readLine();
    }

}
