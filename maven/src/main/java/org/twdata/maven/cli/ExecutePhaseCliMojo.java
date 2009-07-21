package org.twdata.maven.cli;

import org.twdata.maven.cli.console.CliConsole;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.PluginManager;
import org.codehaus.plexus.util.StringUtils;
import org.twdata.maven.cli.commands.Command;
import org.twdata.maven.cli.commands.ExecutePhaseCommand;
import org.twdata.maven.cli.commands.ExitCommand;
import org.twdata.maven.cli.commands.ListProjectsCommand;
import org.twdata.maven.cli.commands.PhaseCallBuilder;
import org.twdata.maven.cli.console.JLineCliConsole;

/**
 * Provides an interactive command line interface for Maven plugins, allowing
 * users to execute plugins directly.
 *
 * @requiresDependencyResolution execute
 * @aggregator true
 * @goal execute-phase
 */
public class ExecutePhaseCliMojo extends AbstractCliMojo {
    /**
     * Command aliases. Commands should be in the form GROUP_ID:ARTIFACT_ID:GOAL
     *
     * @parameter
     */
    private Map<String, String> userAliases;

    /**
     * The Maven PluginManager Object
     *
     * @component
     * @required
     */
    protected PluginManager pluginManager;

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

    private void resolveUserAliases() {
        if (userAliases == null) {
            userAliases = new HashMap<String, String>();
        }

        compactWhiteSpacesInUserAliases();
    }

    private void buildCommands() throws MojoExecutionException {
        cliCommands.add(new ExitCommand());
        cliCommands.add(new ListProjectsCommand(modules.keySet(), console));

        PhaseCallBuilder commandCallBuilder = new PhaseCallBuilder(project, modules, userAliases);
        PhaseCallRunner runner = new PhaseCallRunner(session, project, getLog());

        cliCommands.add(new ExecutePhaseCommand(modules.keySet(), commandCallBuilder, runner, console));
    }

    private List<String> buildValidCommandTokens() {
        List<String> availableCommands = new ArrayList<String>();
        availableCommands.addAll(userAliases.keySet());
        availableCommands.addAll(modules.keySet());

        for (Command command : cliCommands) {
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
                } else if (interpretCommand(line.replaceAll(" {2,}", " "), console) == false) {
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
}
