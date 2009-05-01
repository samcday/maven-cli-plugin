package org.twdata.maven.cli;

import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Pattern;

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
        modules = new HashMap<String, MavenProject>();
        for (Object reactorProject : reactorProjects) {
            modules.put(((MavenProject) reactorProject).getArtifactId(),
                    (MavenProject) reactorProject);
        }

        if (userAliases == null) {
            userAliases = new HashMap<String, String>();
        }

        pluginExecutionOfflineMode = session.getSettings().isOffline();

        initEmbeddedMaven();

        // build list of commands available for completion
        List<String> availableCommands = new ArrayList<String>();
        availableCommands.addAll(defaultPhases);
        availableCommands.addAll(userAliases.keySet());
        availableCommands.addAll(exitCommands);
        availableCommands.addAll(listCommands);
        availableCommands.addAll(modules.keySet());
        availableCommands.addAll(defaultProperties);

        getLog().info("Waiting for commands");
        try {
            ConsoleReader reader = new ConsoleReader(System.in,
                    new OutputStreamWriter(System.out));
            reader.addCompletor(new CommandsCompletor(availableCommands));
            reader.setBellEnabled(false);
            reader.setDefaultPrompt("maven2> ");
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
                        getLog().info("Executing: " + call);
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

    /**
     * Recursively parses commands to resolve all aliases
     *
     * @param text     The text to evaluate
     * @param aliases  The list of aliases available
     * @param commands The list of commands found so far
     */
    private void parseCommand(String text, List<CommandCall> commands) {
        List<String> tokens = new ArrayList<String>(Arrays.asList(text.split(" ")));

        // resolve aliases
        int i = 0;
        while (i < tokens.size()) {
            String token = tokens.get(i);
            if (userAliases.containsKey(token)) {
                String alias = userAliases.get(token);
                List<String> aliasTokens = Arrays.asList(alias.split(" "));
                tokens.remove(i);
                tokens.addAll(i, aliasTokens);
            } else {
                i++;
            }
        }

        CommandCall currentCommandCall = null;
        for (String token : tokens) {
            if (modules.containsKey(token)) {
                currentCommandCall = addProject(commands, currentCommandCall,
                        modules.get(token));
            } else if (token.contains("*")) {
                String regexToken = token.replaceAll("\\*", ".*");
                for (String moduleName : modules.keySet()) {
                    if (Pattern.matches(regexToken, moduleName)) {
                        currentCommandCall = addProject(commands,
                                currentCommandCall, modules.get(moduleName));
                    }
                }
            } else if (token.equals("-o")) {
                goOffline(commands, currentCommandCall);
            } else if (token.equals("-N")) {
                disableRecursive(commands, currentCommandCall);
            } else if (token.equals("-S")) {
                addProperty(commands, currentCommandCall, "-Dmaven.test.skip=true");
            } else if (token.startsWith("-D")) {
                addProperty(commands, currentCommandCall, token);
            } else if (token.startsWith("-P")) {
                addProfile(commands, currentCommandCall, token);
            } else {
                currentCommandCall = addCommand(commands, currentCommandCall,
                        token);
            }
        }
    }

    private String readCommand(ConsoleReader reader) throws IOException {
        return reader.readLine();
    }

    private CommandCall addProject(List<CommandCall> commands,
                                   CommandCall currentCommandCall, MavenProject project) {
        if (currentCommandCall == null
                || !currentCommandCall.getCommands().isEmpty()) {
            currentCommandCall = new CommandCall();
            commands.add(currentCommandCall);
        }
        currentCommandCall.getProjects().add(project);
        return currentCommandCall;
    }

    private CommandCall addCommand(List<CommandCall> commands,
                                   CommandCall currentCommandCall, String command) {
        if (currentCommandCall == null) {
            currentCommandCall = new CommandCall();
            currentCommandCall.getProjects().add(this.project);
            commands.add(currentCommandCall);
        }
        currentCommandCall.getCommands().add(command);
        return currentCommandCall;
    }

    private CommandCall disableRecursive(List<CommandCall> commands,
                                    CommandCall currentCommandCall) {
        if (currentCommandCall == null) {
            currentCommandCall = new CommandCall();
            commands.add(currentCommandCall);
        }
        currentCommandCall.doNotRecurse();
        return currentCommandCall;
    }

    private CommandCall goOffline(List<CommandCall> commands,
                                    CommandCall currentCommandCall) {
        if (currentCommandCall == null) {
            currentCommandCall = new CommandCall();
            commands.add(currentCommandCall);
        }
        currentCommandCall.goOffline();
        return currentCommandCall;
    }

    private CommandCall addProfile(List<CommandCall> commands,
                                    CommandCall currentCommandCall, String profile) {
        if (currentCommandCall == null) {
            currentCommandCall = new CommandCall();
            commands.add(currentCommandCall);
        }
        // must have characters after -P
        if (profile.length() < 3) {
            return currentCommandCall;
        }

        profile = profile.substring(2);
        currentCommandCall.getProfiles().add(profile);
        return currentCommandCall;
    }

    private CommandCall addProperty(List<CommandCall> commands,
                                    CommandCall currentCommandCall, String property) {
        if (currentCommandCall == null) {
            currentCommandCall = new CommandCall();
            commands.add(currentCommandCall);
        }
        // must have characters after -D
        if (property.length() < 3) {
            return currentCommandCall;
        }

        property = property.substring(2);
        String key = property;
        String value = "1";
        if (property.indexOf("=") >= 0) {
            String[] propertyTokens = property.split("=");
            key = propertyTokens[0];
            if (propertyTokens.length > 1) {
                value = propertyTokens[1];
            }
        }
        currentCommandCall.getProperties().put(key, value);
        return currentCommandCall;
    }

    private void executeCommand(CommandCall commandCall) {
        for (MavenProject currentProject : commandCall.getProjects()) {
            try {
                session.getExecutionProperties().putAll(
                        commandCall.getProperties());
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
            } catch (Exception e) {
                getLog().error(
                        "Failed to execute '" + commandCall.getCommands()
                                + "' on '" + currentProject.getArtifactId()
                                + "'");
            }
        }
    }

    private static class CommandCall {
        private final List<String> commands;

        private final List<String> profiles;

        private final List<MavenProject> projects;

        private final Properties properties;

        private boolean offline;

        private boolean recursive;

        public CommandCall() {
            commands = new ArrayList<String>();
            profiles = new ArrayList<String>();
            projects = new ArrayList<MavenProject>();
            properties = new Properties();
            recursive = true;
            offline = false;
        }

        public List<MavenProject> getProjects() {
            return projects;
        }

        public List<String> getCommands() {
            return commands;
        }

        public List<String> getProfiles() {
            return profiles;
        }

        public Properties getProperties() {
            return properties;
        }

        public boolean isOffline() {
            return offline;
        }

        public void goOffline() {
            offline = true;
        }

        public boolean isRecursive() {
            return recursive;
        }

        public void doNotRecurse() {
            recursive = false;
        }
    }
}
