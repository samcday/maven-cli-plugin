package org.twdata.maven.cli;

import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jline.ConsoleReader;

import org.apache.maven.Maven;
import org.apache.maven.execution.DefaultMavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.PluginManager;
import org.apache.maven.profiles.ProfileManager;
import org.apache.maven.profiles.DefaultProfileManager;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.PlexusContainerException;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.embed.Embedder;
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

    protected Embedder embedder;
    protected Maven embeddedMaven;
    protected File userDir;

    private boolean pluginExecutionOfflineMode;

    public void execute() throws MojoExecutionException {
        resolveModulesInProject();
        resolveUserAliases();
        resolvePluginExecutionOfflineMode();
        initEmbeddedMaven();
        List<String> availableCommands = buildAvailableCommands();

        getLog().info("Waiting for commands");
        try {
            ConsoleReader reader = createConsoleReader(availableCommands);
            String line;

            while ((line = readCommand(reader)) != null) {
                if (StringUtils.isEmpty(line)) {
                    continue;
                } else if (exitCommands.contains(line)) {
                    break;
                } else if (listCommands.contains(line)) {
                    getLog().info("Listing available projects: ");
                    for (Object reactorProject : reactorProjects) {
                        getLog().info(
                                "* "
                                        + ((MavenProject) reactorProject)
                                        .getArtifactId());
                    }
                } else {
                    List<CommandCall> calls = new ArrayList<CommandCall>();
                    try {
                        parseCommand(line, calls);
                    } catch (IllegalArgumentException ex) {
                        getLog().error("Invalid command: " + line);
                        continue;
                    }

                    for (CommandCall call : calls) {
                        getLog().debug("Executing: " + call);
                        long start = System.currentTimeMillis();
                        executeCommand(call);
                        long now = System.currentTimeMillis();
                        getLog().info(
                                "Execution time: " + (now - start) + " ms");

                    }
                }
            }
        } catch (IOException e) {
            throw new MojoExecutionException("Unable to execute cli commands",
                    e);
        }
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

    private void resolvePluginExecutionOfflineMode() {
        pluginExecutionOfflineMode = session.getSettings().isOffline();
    }

    private void initEmbeddedMaven() throws MojoExecutionException {
        try {
            embedder = new Embedder();
            embedder.start();
            embeddedMaven = (Maven) embedder.lookup(Maven.ROLE);
            userDir = new File(System.getProperty("user.dir"));
        } catch (PlexusContainerException e) {
            throw new MojoExecutionException(e.getMessage());
        } catch (ComponentLookupException e) {
            throw new MojoExecutionException(e.getMessage());
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

    private ConsoleReader createConsoleReader(List<String> availableCommands) throws IOException {
        ConsoleReader reader = new ConsoleReader(System.in,
                    new OutputStreamWriter(System.out));
        reader.addCompletor(new CommandsCompletor(availableCommands));
        reader.setBellEnabled(false);
        reader.setDefaultPrompt((prompt != null ? prompt : "maven2") + "> ");
        return reader;
    }

    /**
     * Recursively parses commands to resolve all aliases
     *
     * @param text     The text to evaluate
     * @param aliases  The list of aliases available
     * @param commands The list of commands found so far
     */
    private void parseCommand(String text, List<CommandCall> commands) {
        new CommandCallBuilder(project, modules, userAliases).parseCommand(text, commands);
    }

    private String readCommand(ConsoleReader reader) throws IOException {
        return reader.readLine();
    }

    private void executeCommand(CommandCall commandCall) {
        for (MavenProject currentProject : commandCall.getProjects()) {
            try {
                // QUESTION: which should it be?
                session.getExecutionProperties().putAll(commandCall.getProperties());
                //project.getProperties().putAll(commandCall.getProperties());

                session.setCurrentProject(currentProject);
                session.getSettings().setOffline(commandCall.isOffline() ? true : pluginExecutionOfflineMode);
                ProfileManager profileManager = new DefaultProfileManager(embedder.getContainer(),
                        commandCall.getProperties());
                profileManager.explicitlyActivate(commandCall.getProfiles());
                MavenExecutionRequest request = new DefaultMavenExecutionRequest(
                        session.getLocalRepository(), session.getSettings(),
                        session.getEventDispatcher(),
                        commandCall.getCommands(), userDir.getPath(),
                        profileManager, session.getExecutionProperties(),
                        project.getProperties(), true);
                if (!commandCall.isRecursive()) {
                    request.setRecursive(false);
                }
                request.setPomFile(new File(currentProject.getBasedir(),
                        "pom.xml").getPath());
                embeddedMaven.execute(request);
                getLog().info("Current project: " + project.getArtifactId());
            } catch (Exception e) {
                getLog().error(
                        "Failed to execute '" + commandCall.getCommands()
                                + "' on '" + currentProject.getArtifactId()
                                + "'");
            }
        }
    }
}
