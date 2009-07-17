package org.twdata.maven.cli.console;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import jline.Completor;
import jline.ConsoleReader;
import org.apache.maven.plugin.logging.Log;

public class JLineCliConsole implements CliConsole {
    private final ConsoleReader consoleReader;
    private final Log logger;

    public JLineCliConsole(InputStream in, OutputStream out, Log logger,
            String prompt) {
        try {
            consoleReader = new ConsoleReader(in, new OutputStreamWriter(out));
            consoleReader.setBellEnabled(false);
            consoleReader.setDefaultPrompt((prompt != null ? prompt : "maven2") + "> ");
            this.logger = logger;
        } catch (IOException ex) {
            throw new RuntimeException("Unable to create reader to read commands.", ex);
        }
    }

    public void setCompletor(Completor completor) {
        consoleReader.addCompletor(completor);
    }

    @Override
    public String readLine() {
        try {
            return consoleReader.readLine();
        } catch (IOException ex) {
            throw new RuntimeException("Unable to read command.", ex);
        }
    }

    public void writeInfo(String info) {
        logger.info(info);
    }

    public void writeError(String error) {
        logger.error(error);
    }

    public void writeDebug(String debug) {
        logger.debug(debug);
    }
}
