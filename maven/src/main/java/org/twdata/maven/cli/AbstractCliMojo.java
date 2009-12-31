package org.twdata.maven.cli;

import java.io.InputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MavenPluginManager;
import org.apache.maven.project.MavenProject;
import org.twdata.maven.cli.commands.Command;
import org.twdata.maven.cli.commands.ExitCommand;
import org.twdata.maven.cli.commands.HelpCommand;
import org.twdata.maven.cli.commands.ListProjectsCommand;
import org.twdata.maven.cli.console.JLineCliConsole;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Contextualizable;
import org.codehaus.plexus.context.ContextException;
import org.codehaus.plexus.context.Context;
import org.codehaus.plexus.PlexusConstants;
import org.codehaus.plexus.PlexusContainer;

public abstract class AbstractCliMojo extends AbstractMojo {
    /**
     * @component
     * @required
     */
    protected MavenPluginManager mavenPluginManager;

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
    private List reactorProjects;

    /**
     * Command prompt text.
     *
     * @parameter
     */
    private String prompt = "maven2";

    /**
     * TCP port to listen to for shell access
     *
     * @parameter expression="${cli.port}"
     */
    private String port = null;

    private boolean acceptSocket = true;
    private ServerSocket server = null;
    private CommandsCompletor commandsCompletor;

    protected Map<String, MavenProject> modules = new HashMap<String, MavenProject>();
    protected List<Command> cliCommands = new ArrayList<Command>();

    protected abstract void beforeExecute();
    protected abstract Command getSpecializedCliMojoCommand();

    public final void execute() throws MojoExecutionException {
        beforeExecute();
        resolveModulesInProject();
        buildCommands();
        buildCommandsCompletor();

        Thread consoleShell = displayConsoleCliShell();
        displaySocketCliShell();
        try {
            consoleShell.join();
        } catch (InterruptedException e) {
            // ignore
        }
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

    private void buildCommandsCompletor() {
        CommandTokenCollector collector = new CommandTokenCollector();
        for (Command command : cliCommands) {
            command.collectCommandTokens(collector);
        }

        commandsCompletor = new CommandsCompletor(collector.getCollectedTokens());
    }

    private Thread displayConsoleCliShell() {
        Thread shell = new Thread() {
            @Override
            public void run() {
                try {
                    displayShell(System.in, System.out);
                    acceptSocket = false;
                    if (server != null) {
                        server.close();
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        };
        shell.start();

        return shell;
    }

    private void displaySocketCliShell() {
        if (port != null) {
            try {
                server = new ServerSocket(Integer.parseInt(port));
            } catch (IOException e) {
                getLog().error("Cannot open port " + port + " for cli server: " + e);
            }
            openSocket(server, Integer.parseInt(port));
        }
    }

    private void displayShell(InputStream in, PrintStream out) {
        JLineCliConsole console = new JLineCliConsole(in, out, getLog(), commandsCompletor, prompt);
        new CliShell(cliCommands, console).run();
    }

    private void openSocket(ServerSocket server, int port) {
        getLog().info("Opening port " + port + " for socket cli access");
        while (acceptSocket) {
            Socket connection = null;
            try {
                connection = server.accept();
                displayShell(connection.getInputStream(), new PrintStream(connection.getOutputStream()));
            } catch (IOException ex) {
                getLog().error("Server quit unexpectedly");
                ex.printStackTrace();
            }
            finally {
                if (connection != null) {
                    try {
                        connection.close();
                    } catch (IOException e) {
                        // we really don't care
                    }
                }
            }
        }
    }
}
