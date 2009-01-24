package org.twdata.maven.cli;

import static org.twdata.maven.mojoexecutor.MojoExecutor.artifactId;
import static org.twdata.maven.mojoexecutor.MojoExecutor.configuration;
import static org.twdata.maven.mojoexecutor.MojoExecutor.executeMojo;
import static org.twdata.maven.mojoexecutor.MojoExecutor.executionEnvironment;
import static org.twdata.maven.mojoexecutor.MojoExecutor.goal;
import static org.twdata.maven.mojoexecutor.MojoExecutor.groupId;
import static org.twdata.maven.mojoexecutor.MojoExecutor.plugin;
import static org.twdata.maven.mojoexecutor.MojoExecutor.version;

import java.io.*;
import java.util.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

import jline.ConsoleReader;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Plugin;
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
 * @goal execute
 */
public class ExecuteCliMojo extends AbstractMojo
{
    private final Map<String, String> defaultAliases = Collections
            .unmodifiableMap(new HashMap<String, String>()
            {
                {
                    put("compile",
                            "org.apache.maven.plugins:maven-compiler-plugin:compile");
                    put("testCompile",
                            "org.apache.maven.plugins:maven-compiler-plugin:testCompile");
                    put("jar", "org.apache.maven.plugins:maven-jar-plugin:jar");
                    put("war", "org.apache.maven.plugins:maven-war-plugin:war");
                    put("resources",
                            "org.apache.maven.plugins:maven-resources-plugin:resources");
                    put("install",
                            "org.apache.maven.plugins:maven-install-plugin:install");
                    put("deploy",
                            "org.apache.maven.plugins:maven-deploy-plugin:deploy");
                    put("test",
                            "org.apache.maven.plugins:maven-surefire-plugin:test");
                    put("clean",
                            "org.apache.maven.plugins:maven-clean-plugin:clean");
                }
            });

    private final List<String> listCommands = Collections
            .unmodifiableList(new ArrayList<String>()
            {
                {
                    add("list");
                    add("ls");
                }
            });

    private final List<String> exitCommands = Collections
            .unmodifiableList(new ArrayList<String>()
            {
                {
                    add("quit");
                    add("exit");
                    add("bye");
                }
            });

    private static final String HELP_COMMAND = "help";

    /**
     * Command aliases. Commands should be in the form GROUP_ID:ARTIFACT_ID:GOAL
     *
     * @parameter
     */
    private Map<String, String> commands;

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


    public void execute() throws MojoExecutionException
    {
        Thread shell = new Thread()
        {
            @Override
            public void run()
            {
                try
                {
                    ExecuteCliMojo.this.displayShell(System.in, System.out);
                    acceptSocket = false;
                    if (server != null)
                    {
                        server.close();
                    }
                }
                catch (MojoExecutionException e)
                {
                    throw new RuntimeException(e);
                }
                catch (IOException e)
                {
                    throw new RuntimeException(e);
                }
            }
        };
        shell.start();

        if (port != null)
        {
            try
            {
                server = new ServerSocket(Integer.parseInt(port));
            }
            catch (IOException e)
            {
                System.out.println("Cannot open port "+port+" for cli server: "+e);
            }
            openSocket(server, Integer.parseInt(port));
        }
        try
        {
            shell.join();
        }
        catch (InterruptedException e)
        {
            // ignore
        }

    }

    private void openSocket(ServerSocket server, int port) throws MojoExecutionException
    {
        System.out.println("Opening port "+port+" for socket cli access");
        while (acceptSocket)
        {
            Socket connection = null;
            try
            {
                connection = server.accept();
                displayShell(connection.getInputStream(), new PrintStream(connection.getOutputStream()));
            }
            catch (IOException ex)
            {
                System.out.println("Server quit unexpectedly");
                ex.printStackTrace();

            }
            finally
            {
                if (connection != null)
                {
                    try
                    {
                        connection.close();
                    }
                    catch (IOException e)
                    {
                        // we really don't care
                    }
                }
            }
        }
    }

    private void displayShell(InputStream in, PrintStream out) throws MojoExecutionException
    {
        // build a list of command aliases
        Map<String, String> aliases = buildCommands();

        // build list of commands available for completion
        List<String> availableCommands = new ArrayList<String>();
        availableCommands.addAll(aliases.keySet());
        availableCommands.addAll(exitCommands);
        availableCommands.addAll(listCommands);

        getLog().info("Waiting for commands");
        try
        {
            ConsoleReader reader = new ConsoleReader(in,
                    new OutputStreamWriter(out));
            reader.addCompletor(new CommandsCompletor(availableCommands));
            reader.setDefaultPrompt("maven2> ");
            String line;

            while ((line = readCommand(reader)) != null)
            {
                if (StringUtils.isEmpty(line))
                {
                    continue;
                }
                else
                {
                    if (exitCommands.contains(line))
                    {
                        break;
                    }
                    else
                    {
                        if (listCommands.contains(line))
                        {
                            getLog().info("Listing available projects: ");
                            for (Object reactorProject : reactorProjects)
                            {
                                getLog().info(
                                        "* "
                                                + ((MavenProject) reactorProject)
                                                .getArtifactId());
                            }
                        }
                        else
                        {
                            if (HELP_COMMAND.equals(line))
                            {
                                printHelp();
                            }
                            else
                            {
                                List<MojoCall> calls = new ArrayList<MojoCall>();
                                try
                                {
                                    parseCommand(line, aliases, calls);
                                }
                                catch (IllegalArgumentException ex)
                                {
                                    getLog().error("Invalid command: " + line);
                                    continue;
                                }

                                for (MojoCall call : calls)
                                {
                                    getLog().info("Executing: " + call);
                                    long start = System.currentTimeMillis();
                                    executeMojo(plugin(groupId(call.getGroupId()),
                                            artifactId(call.getArtifactId()), version(call
                                            .getVersion(project))), goal(call
                                            .getGoal()), configuration(),
                                            executionEnvironment(project, session,
                                                    pluginManager));
                                    long now = System.currentTimeMillis();
                                    getLog().info(
                                            "Execution time: " + (now - start) + " ms");
                                }
                            }
                        }
                    }
                }
            }
        }
        catch (IOException e)
        {
            throw new MojoExecutionException("Unable to execute cli commands",
                    e);
        }
    }

    private Map<String, String> buildCommands
            ()
    {
        Map<String, String> aliases = new HashMap<String, String>();
        aliases.putAll(defaultAliases);
        if (commands != null)
        {
            aliases.putAll(commands);
        }
        return aliases;
    }

    private void printHelp
            ()
    {
        StringWriter writer = new StringWriter();
        PrintWriter pw = new PrintWriter(writer);

        Map<String, String> commands = buildCommands();
        int maxLength = 0;
        for (String key : commands.keySet())
        {
            maxLength = Math.max(maxLength, key.length());
        }

        pw.println("Commands: ");
        for (Map.Entry<String, String> cmd : buildCommands().entrySet())
        {
            pw.print("  ");
            pw.print(cmd.getKey());
            pw.print("  ");
            for (int x = 0; x < (maxLength - cmd.getKey().length()); x++)
            {
                pw.print(" ");
            }
            pw.println(cmd.getValue());
        }
        pw.println("Exit commands: ");
        pw.print("  ");
        pw.println(join(exitCommands));

        pw.println("List module commands: ");
        pw.print("  ");
        pw.print(join(listCommands));
        getLog().info(writer.toString());
    }


    private String join
            (List<String> list)
    {
        StringBuffer sb = new StringBuffer();
        for (int x = 0; x < list.size(); x++)
        {
            sb.append(list.get(x));
            if (x + 1 < list.size())
            {
                sb.append(", ");
            }
        }
        return sb.toString();
    }

    /**
     * Recursively parses commands to resolve all aliases
     *
     * @param text     The text to evaluate
     * @param aliases  The list of aliases available
     * @param commands The list of commands found so far
     */
    private static void parseCommand
            (String
                    text, Map<String, String> aliases,
                          List<MojoCall> commands)
    {
        String[] tokens = text.split(" ");
        if (tokens.length > 1)
        {
            for (String token : tokens)
            {
                parseCommand(token, aliases, commands);
            }
        }
        else
        {
            if (aliases.containsKey(text))
            {
                parseCommand(aliases.get(text), aliases, commands);
            }
            else
            {
                String[] parsed = text.split(":");
                if (parsed.length < 3)
                {
                    throw new IllegalArgumentException("Invalid command: " + text);
                }
                commands.add(new MojoCall(parsed[0], parsed[1], parsed[2]));
            }
        }
    }

    private String readCommand
            (ConsoleReader
                    reader) throws IOException
    {
        try
        {
            return reader.readLine();
        }
        catch (SocketException ex)
        {
            // swallow
            return null;
        }
    }

    private static class MojoCall
    {
        private final String groupId;
        private final String artifactId;
        private final String goal;

        public MojoCall(String groupId, String artifactId, String goal)
        {
            this.groupId = groupId;
            this.artifactId = artifactId;
            this.goal = goal;
        }

        public String getGroupId()
        {
            return groupId;
        }

        public String getArtifactId()
        {
            return artifactId;
        }

        public String getGoal()
        {
            return goal;
        }

        /**
         * Tries to determine what version of the plugin has been already
         * configured for this project. If unknown, "RELEASE" is used.
         *
         * @param project The maven project
         * @return The discovered plugin version
         */
        public String getVersion(MavenProject project)
        {
            String version = null;
            List<Plugin> plugins = project.getBuildPlugins();
            for (Plugin plugin : plugins)
            {
                if (groupId.equals(plugin.getGroupId())
                        && artifactId.equals(plugin.getArtifactId()))
                {
                    version = plugin.getVersion();
                    break;
                }
            }

            if (version == null)
            {
                plugins = project.getPluginManagement().getPlugins();
                for (Plugin plugin : plugins)
                {
                    if (groupId.equals(plugin.getGroupId())
                            && artifactId.equals(plugin.getArtifactId()))
                    {
                        version = plugin.getVersion();
                        break;
                    }
                }
            }

            if (version == null)
            {
                version = "RELEASE";
            }
            return version;
        }

        public String toString()
        {
            StringBuilder sb = new StringBuilder();
            sb.append(groupId).append(":").append(artifactId);
            sb.append(" [").append(goal).append("]");
            return sb.toString();
        }
    }
}
