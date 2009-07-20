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
import org.twdata.maven.cli.commands.HelpCommand;
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

    private List<Command> cliCommands = new ArrayList<Command>();
    private Command listProjectsCommand = null;
    private Command exitCommand = null;
    private Command executeGoalCommand = null;
    private Command helpCommand = null;

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
        resolveUserDefinedGoals();

        buildCliCommands(console);
        console.setCompletor(new CommandsCompletor(buildValidCommandTokens()));

        console.writeInfo("Waiting for commands");
        String line;

        while ((line = console.readLine()) != null) {
            if (StringUtils.isEmpty(line)) {
                continue;
            } else if (exitCommand.matchesRequest(line)) {
                break;
            } else {
                interpretCommand(line, console);
            }
        }
    }

    private void buildCliCommands(CliConsole console) {
        Set<String> projectNames = new HashSet<String>();
        for (Object reactorProject : reactorProjects) {
            projectNames.add(((MavenProject) reactorProject).getArtifactId());
        }

        executeGoalCommand = new ExecuteGoalCommand(project, session, pluginManager, console, commands);
        listProjectsCommand = new ListProjectsCommand(projectNames, console);
        exitCommand = new ExitCommand();

        cliCommands.add(executeGoalCommand);
        cliCommands.add(listProjectsCommand);
        cliCommands.add(exitCommand);

        helpCommand = new HelpCommand(cliCommands, console);
        cliCommands.add(helpCommand);
    }

    private void resolveUserDefinedGoals() {
        if (commands == null) {
            commands = new HashMap<String, String>();
        }
    }

    private List<String> buildValidCommandTokens() {
        List<String> availableCommands = new ArrayList<String>();
        for (Command command : cliCommands) {
            availableCommands.addAll(command.getCommandNames());
        }

        return availableCommands;
    }

    private void interpretCommand(String line, CliConsole console) {
        for (Command command : cliCommands) {
            if (command.matchesRequest(line)) {
                command.run(line);
                return;
            }
        }

        console.writeError("Invalid command: " + line);
    }
}
