package org.twdata.maven.cli;

import org.twdata.maven.cli.console.CliConsole;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.PluginManager;
import org.codehaus.plexus.util.StringUtils;
import org.twdata.maven.cli.commands.ExecutePhaseCommand;
import org.twdata.maven.cli.commands.PhaseCallBuilder;
import org.twdata.maven.cli.console.JLineCliConsole;

/**
 * Provides an interactive command line interface for Maven plugins, allowing
 * users to execute phases directly.
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

    private CliConsole console;

    public void execute() throws MojoExecutionException {
        resolveModulesInProject();
        resolveUserAliases();
        console = new JLineCliConsole(System.in, System.out, getLog(), prompt);

        buildCliCommands();
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

    private void buildCliCommands() throws MojoExecutionException {
        PhaseCallBuilder commandCallBuilder = new PhaseCallBuilder(project, modules, userAliases);
        PhaseCallRunner runner = new PhaseCallRunner(session, project, getLog());

        cliCommands.add(new ExecutePhaseCommand(modules.keySet(), commandCallBuilder, runner));
        buildDefaultCommands();
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
