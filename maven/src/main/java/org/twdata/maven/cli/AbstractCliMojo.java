package org.twdata.maven.cli;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import jline.Completor;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.twdata.maven.cli.commands.Command;
import org.twdata.maven.cli.commands.ExitCommand;
import org.twdata.maven.cli.commands.HelpCommand;
import org.twdata.maven.cli.commands.ListProjectsCommand;

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

    protected abstract void beforeExecuteSetup();
    protected abstract void afterExecuteSetup();
    protected abstract Command getSpecializedCliMojoCommand();

    public final void execute() throws MojoExecutionException {
        beforeExecuteSetup();
        resolveModulesInProject();
        buildCommands();
        buildValidCommandTokens();
        afterExecuteSetup();
    }

    private void resolveModulesInProject() {
        for (Object reactorProject : reactorProjects) {
            modules.put(((MavenProject) reactorProject).getArtifactId(),
                    (MavenProject) reactorProject);
        }
    }

    private void buildCommands() {
        cliCommands.add(getSpecializedCliMojoCommand());
        cliCommands.add(new ExitCommand());
        cliCommands.add(new ListProjectsCommand(modules.keySet()));
        cliCommands.add(new HelpCommand(cliCommands));
    }

    private List<String> buildValidCommandTokens() {
        CommandTokenCollector collector = new CommandTokenCollector();
        for (Command command : cliCommands) {
            command.collectCommandTokens(collector);
        }

        List<String> availableCommands = new ArrayList<String>();
        availableCommands.addAll(collector.getCollectedTokens());
        return availableCommands;
    }

    protected Completor getCommandsCompletor() {
        return new CommandsCompletor(buildValidCommandTokens());
    }
}
