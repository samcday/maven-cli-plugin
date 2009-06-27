package org.twdata.maven.cli;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class ExitCommand implements Command {
    private final Set<String> exitCommands;

    public ExitCommand() {
        Set<String> commands = new HashSet<String>();
        commands.add("quit");
        commands.add("exit");
        commands.add("bye");
        exitCommands = Collections.unmodifiableSet(commands);
    }

    public boolean run(String request) {
        return false;
    }

    public Set<String> getCommandNames() {
        return exitCommands;
    }

    public boolean matchesRequest(String request) {
        return exitCommands.contains(request);
    }
}
