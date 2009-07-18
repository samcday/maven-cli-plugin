package org.twdata.maven.cli;

import java.io.*;
import java.util.*;
import java.net.ServerSocket;
import java.net.Socket;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.PluginManager;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.StringUtils;
import org.twdata.maven.cli.commands.Command;
import org.twdata.maven.cli.commands.ExecuteGoalCommand;
import org.twdata.maven.cli.commands.ExitCommand;
import org.twdata.maven.cli.commands.ListProjectsCommand;
import org.twdata.maven.cli.console.CliConsole;
import org.twdata.maven.cli.console.JLineCliConsole;

/**
 * Provides an interactive command line interface for Maven plugins, allowing
 * users to execute plugins directly.
 *
 * @requiresDependencyResolution execute
 * @aggregator true
 * @goal execute
 */
public class ExecuteCliMojo extends AbstractMojo {
    private final Map<String, String> defaultGoals = Collections
            .unmodifiableMap(new HashMap<String, String>() {
                {
                    put("compile",
                            "org.apache.maven.plugins:maven-compiler-plugin:compile");
                    put("testCompile",
                            "org.apache.maven.plugins:maven-compiler-plugin:testCompile");
                    put("jar", "org.apache.maven.plugins:maven-jar-plugin:jar");
                    put("war", "org.apache.maven.plugins:maven-war-plugin:war");
                    put("resources",
                            "org.apache.maven.plugins:maven-resources-plugin:resources");
                    put("testResources",
                            "org.apache.maven.plugins:maven-resources-plugin:testResources");
                    put("install",
                            "org.apache.maven.plugins:maven-install-plugin:install");
                    put("deploy",
                            "org.apache.maven.plugins:maven-deploy-plugin:deploy");
                    put("test",
                            "org.apache.maven.plugins:maven-surefire-plugin:test");
                    put("clean",
                            "org.apache.maven.plugins:maven-clean-plugin:clean");

                    //Help plugins not requiring parameters
                    put("help-system",
                            "org.apache.maven.plugins:maven-help-plugin:system");
                    put("help-effectivesettings",
                            "org.apache.maven.plugins:maven-help-plugin:effective-settings");
                    put("help-allprofiles",
                            "org.apache.maven.plugins:maven-help-plugin:all-profiles");

                    //Dependency plugins for analysis and management
                    put("dependency-tree",
                            "org.apache.maven.plugins:maven-dependency-plugin:tree");
                    put("dependency-resolve",
                            "org.apache.maven.plugins:maven-dependency-plugin:resolve");
                    put("dependency-resolve-plugins",
                            "org.apache.maven.plugins:maven-dependency-plugin:resolve-plugins");
                    put("dependency-purge",
                            "org.apache.maven.plugins:maven-dependency-plugin:purge-local-repository");
                    put("dependency-analyze",
                            "org.apache.maven.plugins:maven-dependency-plugin:analyze");
                }
            });

    private static final String HELP_COMMAND = "?";

    /**
     * Command aliases. Commands should be in the form GROUP_ID:ARTIFACT_ID:GOAL
     *
     * @parameter
     */
    private Map<String, String> commands;

    /**
     * Command prompt text.
     *
     * @parameter
     */
    private String prompt;

    /**
     * TCP port to listen to for shell access
     *
     * @parameter expression="${cli.port}"
     */
    private String port = null;

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

    private boolean acceptSocket = true;

    private ServerSocket server = null;
    private Command listProjectsCommand = null;
    private Command exitCommand = null;
    private Command executeGoalCommand = null;

    public void execute() throws MojoExecutionException {
        Thread shell = new Thread() {
            @Override
            public void run() {
                try {
                    ExecuteCliMojo.this.displayShell(System.in, System.out);
                    acceptSocket = false;
                    if (server != null) {
                        server.close();
                    }
                }
                catch (MojoExecutionException e) {
                    throw new RuntimeException(e);
                }
                catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        };
        shell.start();

        if (port != null) {
            try {
                server = new ServerSocket(Integer.parseInt(port));
            }
            catch (IOException e) {
                System.out.println("Cannot open port " + port + " for cli server: " + e);
            }
            openSocket(server, Integer.parseInt(port));
        }
        try {
            shell.join();
        }
        catch (InterruptedException e) {
            // ignore
        }

    }

    private void openSocket(ServerSocket server, int port) throws MojoExecutionException {
        System.out.println("Opening port " + port + " for socket cli access");
        while (acceptSocket) {
            Socket connection = null;
            try {
                connection = server.accept();
                displayShell(connection.getInputStream(), new PrintStream(connection.getOutputStream()));
            }
            catch (IOException ex) {
                System.out.println("Server quit unexpectedly");
                ex.printStackTrace();

            }
            finally {
                if (connection != null) {
                    try {
                        connection.close();
                    }
                    catch (IOException e) {
                        // we really don't care
                    }
                }
            }
        }
    }

    private void displayShell(InputStream in, PrintStream out) throws MojoExecutionException {
        JLineCliConsole console = new JLineCliConsole(in, out, getLog(), prompt);
        Map<String, String> goals = buildGoals();

        buildCliCommands(console);
        List<String> validCommandTokens = buildValidCommandTokens(goals.keySet());
        console.setCompletor(new CommandsCompletor(validCommandTokens));

        console.writeInfo("Waiting for commands");
        String line;

        while ((line = console.readLine()) != null) {
            if (StringUtils.isEmpty(line)) {
                continue;
            } else if (exitCommand.matchesRequest(line)) {
                break;
            } else {
                interpretCommand(line, goals, console);
            }
        }
    }

    private void buildCliCommands(CliConsole console) {
        Set<String> projectNames = new HashSet<String>();
        for (Object reactorProject : reactorProjects) {
            projectNames.add(((MavenProject) reactorProject).getArtifactId());
        }

        listProjectsCommand = new ListProjectsCommand(projectNames, console);
        exitCommand = new ExitCommand();
        executeGoalCommand = new ExecuteGoalCommand(project, session, pluginManager, console, commands);
    }

    private Map<String, String> buildGoals() {
        Map<String, String> goals = new HashMap<String, String>();
        goals.putAll(defaultGoals);
        if (commands == null) {
            commands = new HashMap<String, String>();
        }
        goals.putAll(commands);
        return goals;
    }

    private List<String> buildValidCommandTokens(Set<String> goalTokens) {
        List<String> availableCommands = new ArrayList<String>();
        availableCommands.addAll(goalTokens);
        availableCommands.addAll(exitCommand.getCommandNames());
        availableCommands.addAll(listProjectsCommand.getCommandNames());

        return availableCommands;
    }

    private void interpretCommand(String line, Map<String, String> goals, CliConsole console) {
        if (listProjectsCommand.matchesRequest(line)) {
            listProjectsCommand.run(line);
        } else if (HELP_COMMAND.equals(line)) {
            printHelp();
        } else if (executeGoalCommand.matchesRequest(line)) {
            executeGoalCommand.run(line);
        } else {
            console.writeError("Invalid command: " + line);
        }
    }

    private void printHelp() {
        StringWriter writer = new StringWriter();
        PrintWriter pw = new PrintWriter(writer);

        Map<String, String> goals = buildGoals();
        int maxLength = 0;
        for (String key : goals.keySet()) {
            maxLength = Math.max(maxLength, key.length());
        }

        pw.println("Commands: ");
        for (Map.Entry<String, String> cmd : buildGoals().entrySet()) {
            pw.print("  ");
            pw.print(cmd.getKey());
            pw.print("  ");
            for (int x = 0; x < (maxLength - cmd.getKey().length()); x++) {
                pw.print(" ");
            }
            pw.println(cmd.getValue());
        }
        pw.println("Exit commands: ");
        pw.print("  ");
        pw.println(join(exitCommand.getCommandNames()));

        pw.println("List module commands: ");
        pw.print("  ");
        pw.print(join(listProjectsCommand.getCommandNames()));
        getLog().info(writer.toString());
    }


    private String join(Set<String> stringSet) {
        if (stringSet.size() == 0) return "";

        StringBuffer sb = new StringBuffer();
        for (String value : stringSet) {
            sb.append(value).append(", ");
        }

        return sb.substring(0, sb.length() - 2);
    }

    /**
     * Recursively parses commands to resolve all aliases
     *
     * @param text     The text to evaluate
     * @param aliases  The list of aliases available
     * @param commands The list of commands found so far
     */
    private List<MojoCall> parseCommand(String text, Map<String, String> aliases) {
        List<MojoCall> calls = new ArrayList<MojoCall>();

        String[] tokens = text.split(" ");
        if (tokens.length > 1) {
            for (String token : tokens) {
                calls.addAll(parseCommand(token, aliases));
            }
        } else {
            if (aliases.containsKey(text)) {
                calls.addAll(parseCommand(aliases.get(text), aliases));
            } else {
                String[] parsed = text.split(":");
                if (parsed.length < 3) {
                    throw new IllegalArgumentException("Invalid command: " + text);
                }
                calls.add(new MojoCall(parsed[0], parsed[1], parsed[2]));
            }
        }

        return calls;
    }
}
