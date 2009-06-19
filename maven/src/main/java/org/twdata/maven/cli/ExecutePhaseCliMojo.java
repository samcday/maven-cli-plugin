package org.twdata.maven.cli;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jline.ConsoleReader;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.PluginManager;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.StringUtils;

/**
 * Provides an interactive command line interface for Maven plugins, allowing
 * users to execute plugins directly.
 *
 * @requiresDependencyResolution execute
 * @aggregator true
 * @goal execute-phase
 */
public class ExecutePhaseCliMojo extends AbstractMojo {

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

    private final List<String> exitCommands = Collections
            .unmodifiableList(new ArrayList<String>() {
                {
                    add("quit");
                    add("exit");
                    add("bye");
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

    public void execute() throws MojoExecutionException {
        resolveModulesInProject();
        resolveUserAliases();
        List<String> availableCommands = buildAvailableCommands();
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
        availableCommands.addAll(exitCommands);
        availableCommands.addAll(listCommands);
        availableCommands.addAll(modules.keySet());
        availableCommands.addAll(defaultProperties);

        return availableCommands;
    }

    private void startListeningForCommands(List<String> availableCommands)
            throws MojoExecutionException {
        getLog().info("Waiting for commands");
        try {
            ConsoleReader reader = createConsoleReader(availableCommands);
            String line;
            CommandCallBuilder commandCallBuilder =
                    new CommandCallBuilder(project, modules, userAliases);
            CommandCallRunner runner = new CommandCallRunner(session, project, getLog());

            while ((line = reader.readLine()) != null) {
                if (StringUtils.isEmpty(line)) {
                    continue;
                } else if (exitCommands.contains(line)) {
                    break;
                } else if (listCommands.contains(line)) {
                    listReactorProjects();
                } else {
                    try {
                        executeLifeCyclePhases(commandCallBuilder, runner, line);
                    } catch (IllegalArgumentException ex) {
                        continue;
                    }
                }
            }
        } catch (IOException e) {
            throw new MojoExecutionException("Unable to execute cli commands",
                    e);
        }
    }

    private ConsoleReader createConsoleReader(List<String> availableCommands) throws IOException {
        ConsoleReader reader = new ConsoleReader(System.in,
                    new OutputStreamWriter(System.out));
        reader.addCompletor(new CommandsCompletor(availableCommands));
        reader.setBellEnabled(false);
        reader.setDefaultPrompt((prompt != null ? prompt : "maven2") + "> ");
        return reader;
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
            commandCallBuilder.parseCommand(line, calls);
        } catch (IllegalArgumentException ex) {
            getLog().error("Invalid command: " + line);
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
