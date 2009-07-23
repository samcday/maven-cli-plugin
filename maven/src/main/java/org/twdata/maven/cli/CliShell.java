package org.twdata.maven.cli;

import java.util.List;
import org.codehaus.plexus.util.StringUtils;
import org.twdata.maven.cli.commands.Command;
import org.twdata.maven.cli.console.CliConsole;

public class CliShell {
    private final List<Command> commands;
    private final CliConsole console;

    public CliShell(List<Command> commands, CliConsole console) {
        this.commands = commands;
        this.console = console;
    }

    public void run() {
        console.writeInfo("Waiting for commands...");

        String line;
        while ((line = console.readLine()) != null) {
            try {
                if (StringUtils.isEmpty(line)) {
                    continue;
                } else if (interpretCommand(line.trim().replaceAll(" {2,}", " ")) == false) {
                    break;
                }
            } catch (Exception ex) {
                console.writeError("Unable to complete running command: " + line + "\n"
                        + ex.toString());
            }
        }
    }

    private boolean interpretCommand(String request) {
        for (Command command : commands) {
            if (command.matchesRequest(request)) {
                return command.run(request, console);
            }
        }

        console.writeError("Invalid command: " + request);
        return true;
    }
}
