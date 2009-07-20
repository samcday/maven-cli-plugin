package org.twdata.maven.cli.commands;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.twdata.maven.cli.console.CliConsole;

public class ListProjectsCommand implements Command {
    private final Set<String> listCommands;

    private final Set<String> projectNames;
    private final CliConsole console;

    public ListProjectsCommand(Set<String> projectNames, CliConsole console) {
        this.console = console;
        this.projectNames = projectNames;

        Set<String> commands = new HashSet<String>();
        commands.add("list");
        commands.add("ls");
        listCommands = Collections.unmodifiableSet(commands);
    }

    public void describe(CommandDescription description) {
        description.describeCommandName("List module commands")
                .describeCommandToken("list, ls", null);
    }

    public boolean matchesRequest(String request) {
        return listCommands.contains(request);
    }

    public Set<String> getCommandNames() {
        return listCommands;
    }

    public boolean run(String request) {
        console.writeInfo("Listing available projects: ");

        for (String projectName : projectNames) {
            console.writeInfo("* " + projectName);
        }

        return true;
    }
}
