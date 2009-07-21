package org.twdata.maven.cli;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.project.MavenProject;
import org.twdata.maven.cli.commands.Command;
import org.twdata.maven.cli.commands.ExitCommand;
import org.twdata.maven.cli.commands.HelpCommand;
import org.twdata.maven.cli.commands.ListProjectsCommand;
import org.twdata.maven.cli.console.CliConsole;

public abstract class AbstractCliMojo extends AbstractMojo {
    /**
     * The Maven Project Object
     *
     * @parameter expression="${project}"
     * @required
     * @readonly
     */
    protected MavenProject project;

    /**
     * The Maven Session Object
     *
     * @parameter expression="${session}"
     * @required
     * @readonly
     */
    protected MavenSession session;

    /**
     * The reactor projects.
     *
     * @parameter expression="${reactorProjects}"
     * @readonly
     */
    protected List reactorProjects;

    /**
     * Command prompt text.
     *
     * @parameter
     */
    protected String prompt;

    protected Map<String, MavenProject> modules = new HashMap<String, MavenProject>();
    protected List<Command> cliCommands = new ArrayList<Command>();

    protected void resolveModulesInProject() {
        for (Object reactorProject : reactorProjects) {
            modules.put(((MavenProject) reactorProject).getArtifactId(),
                    (MavenProject) reactorProject);
        }
    }

    protected void buildDefaultCommands() {
        cliCommands.add(new ExitCommand());
        cliCommands.add(new ListProjectsCommand(modules.keySet()));
        cliCommands.add(new HelpCommand(cliCommands));
    }

    protected boolean interpretCommand(String request, CliConsole console) {
        for (Command command : cliCommands) {
            if (command.matchesRequest(request)) {
                return command.run(request, console);
            }
        }

        console.writeError("Invalid command: " + request);
        return true;
    }
}
