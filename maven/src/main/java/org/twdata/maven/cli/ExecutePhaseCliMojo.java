package org.twdata.maven.cli;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
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
    private final List<String> exitCommands = Collections
            .unmodifiableList(new ArrayList<String>() {
                {
                    add("quit");
                    add("exit");
                    add("bye");
                }
            });

    private final List<String> defaultPhases = Collections
            .unmodifiableList(new ArrayList<String>() {
                {
                    add("clean");

                    add("validate");
                    add("generate-sources");
                    add("generate-resources");
                    add("test-compile");
                    add("test");
                    add("package");
                    add("integration-test");
                    add("install");
                    add("deploy");

                    add("site");
                    add("site-deploy");
                }
            });

    private final List<String> defaultProperties = Collections
            .unmodifiableList(new ArrayList<String>() {
                {
                    add("-o"); // offline mode
                    add("-N"); // don't recurse
                    add("-S"); // skip tests
                }
            });

    private final List<String> listCommands = Collections
            .unmodifiableList(new ArrayList<String>() {
                {
                    add("list");
                    add("ls");
                }
            });

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

    private CommandCallBuilder commandCallBuilder;
    private CommandCallRunner runner;

    public void execute() throws MojoExecutionException {
        resolveModulesInProject();
        resolveUserAliases();
        List<String> availableCommands = buildAvailableCommands();
        commandCallBuilder = new CommandCallBuilder(project, modules, userAliases);
        runner = new CommandCallRunner(session, project, getLog());

        startListeningForCommands(availableCommands);
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
    }

    private List<String> buildAvailableCommands() {
        List<String> availableCommands = new ArrayList<String>();
        availableCommands.addAll(defaultPhases);
        availableCommands.addAll(userAliases.keySet());
        availableCommands.addAll(listCommands);
        availableCommands.addAll(modules.keySet());
        availableCommands.addAll(defaultProperties);
        availableCommands.addAll(exitCommands);

        return availableCommands;
    }

    private void startListeningForCommands(List<String> availableCommands)
            throws MojoExecutionException {
        try {
            CommandsCompletor completor = new CommandsCompletor(availableCommands);
            CliConsole cliConsole = new JLineCliConsole(System.in, System.out,
                    completor, prompt);
            String line;

            getLog().info("Waiting for commands");
            while ((line = cliConsole.readLine()) != null) {
                if (StringUtils.isEmpty(line)) {
                    continue;
                } else if (interpretCommand(line) == false) {
                    break;
                }
            }
        } catch (IOException e) {
            throw new MojoExecutionException("Unable to execute cli commands",
                    e);
        }
    }

    public boolean interpretCommand(String command) throws MojoExecutionException {
        if (exitCommands.contains(command)) {
            return false;
        }

        if (listCommands.contains(command)) {
            listReactorProjects();
        } else {
            executeLifeCyclePhases(commandCallBuilder, runner, command);
        }

        return true;
    }

    private void listReactorProjects() {
        getLog().info("Listing available projects: ");
        for (Object reactorProject : reactorProjects) {
            getLog().info("* " + ((MavenProject) reactorProject).getArtifactId());
        }
    }

    private void executeLifeCyclePhases(CommandCallBuilder commandCallBuilder,
            CommandCallRunner runner, String line) {
        List<CommandCall> calls = new ArrayList<CommandCall>();
        try {
            calls = commandCallBuilder.parseCommand(line);
        } catch (IllegalArgumentException ex) {
            getLog().error("Invalid command: " + line);
            return;
        }

        for (CommandCall call : calls) {
            getLog().debug("Executing: " + call);
            long start = System.currentTimeMillis();
            runner.executeCommand(call);
            long now = System.currentTimeMillis();
            getLog().info("Execution time: " + (now - start) + " ms");
        }
    }
}
