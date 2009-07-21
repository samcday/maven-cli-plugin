package org.twdata.maven.cli;

import org.twdata.maven.cli.commands.Command;
import org.twdata.maven.cli.console.CliConsole;
import java.util.HashMap;
import java.util.Map;
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

    @Override
    protected void beforeExecuteSetup() {
        resolveUserAliases();
    }

    private void resolveUserAliases() {
        if (userAliases == null) {
            userAliases = new HashMap<String, String>();
        }

        compactWhiteSpacesInUserAliases();
    }

    private void compactWhiteSpacesInUserAliases() {
        for (String key : userAliases.keySet()) {
            String value = userAliases.get(key).replaceAll("\\s{2,}", " ");
            userAliases.put(key, value);
        }
    }

    @Override
    protected void afterExecuteSetup() {
        console = new JLineCliConsole(System.in, System.out, getLog(), getCommandsCompletor(), prompt);
        new CliShell(cliCommands, console).run();
    }

    @Override
    protected Command getSpecializedCliMojoCommand() {
        PhaseCallBuilder commandCallBuilder = new PhaseCallBuilder(project, modules, userAliases);
        PhaseCallRunner runner = new PhaseCallRunner(session, project, getLog());

        return new ExecutePhaseCommand(modules.keySet(), commandCallBuilder, runner);
    }
}
