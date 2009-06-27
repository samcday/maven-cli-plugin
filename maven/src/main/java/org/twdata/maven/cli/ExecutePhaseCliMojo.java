package org.twdata.maven.cli;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.PluginManager;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.StringUtils;
import org.twdata.maven.cli.externalapi.JLineCliConsole;

/**
 * Provides an interactive command line interface for Maven plugins, allowing
 * users to execute plugins directly.
 *
 * @requiresDependencyResolution execute
 * @aggregator true
 * @goal execute-phase
 */
public class ExecutePhaseCliMojo extends AbstractMojo implements CommandInterpreter {
    /**
     * Command aliases. Commands should be in the form GROUP_ID:ARTIFACT_ID:GOAL
     *
     * @parameter
     */
    private Map<String, String> userAliases;

    /**
     * Command prompt text.
     *
     * @parameter
     */
    private String prompt;

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
     * The Maven PluginManager Object
     *
     * @component
     * @required
     */
    protected PluginManager pluginManager;

    /**
     * The reactor projects.
     *
     * @parameter expression="${reactorProjects}"
     * @readonly
     */
    protected List reactorProjects;

    protected Map<String, MavenProject> modules;

    private List<Command> commands = new ArrayList<Command>();
    private CliConsole console;

    public void execute() throws MojoExecutionException {
        resolveModulesInProject();
        resolveUserAliases();
        console = new JLineCliConsole(System.in, System.out, getLog(), prompt);

        buildCommands();
        List<String> validCommandTokens = buildValidCommandTokens();

        ((JLineCliConsole) console).setCompletor(new CommandsCompletor(validCommandTokens));
        startListeningForCommands();
    }

    private void resolveModulesInProject() {
        modules = new HashMap<String, MavenProject>();
        for (Object reactorProject : reactorProjects) {
            modules.put(((MavenProject) reactorProject).getArtifactId(),
                    (MavenProject) reactorProject);
        }
    }

    private void resolveUserAliases() {
        if (userAliases == null) {
            userAliases = new HashMap<String, String>();
        }

        compactWhiteSpacesInUserAliases();
    }

    private void buildCommands() throws MojoExecutionException {
        commands.add(new ExitCommand());
        commands.add(new ListProjectsCommand(modules.keySet(), console));

        PhaseCallBuilder commandCallBuilder = new PhaseCallBuilder(project, modules, userAliases);
        CommandCallRunner runner = new CommandCallRunner(session, project, getLog());

        commands.add(new ExecutePhaseCommand(modules.keySet(), commandCallBuilder, runner, console));
    }

    private List<String> buildValidCommandTokens() {
        List<String> availableCommands = new ArrayList<String>();
        availableCommands.addAll(userAliases.keySet());
        availableCommands.addAll(modules.keySet());

        for (Command command : commands) {
            availableCommands.addAll(command.getCommandNames());
        }

        return availableCommands;
    }

    private void startListeningForCommands() throws MojoExecutionException {
        try {
            String line;

            getLog().info("Waiting for commands");
            while ((line = console.readLine()) != null) {
                if (StringUtils.isEmpty(line)) {
                    continue;
                } else if (interpretCommand(line.replaceAll(" {2,}", " ")) == false) {
                    break;
                }
            }
        } catch (Exception e) {
            throw new MojoExecutionException("Unable to execute cli commands", e);
        }
    }

    private void compactWhiteSpacesInUserAliases() {
        for (String key : userAliases.keySet()) {
            String value = userAliases.get(key).replaceAll("\\s{2,}", " ");
            userAliases.put(key, value);
        }
    }

    public boolean interpretCommand(String request) throws MojoExecutionException {
        for (Command cmd : commands) {
            if (cmd.matchesRequest(request)) {
                return cmd.run(request);
            }
        }

        return true;
    }
}
