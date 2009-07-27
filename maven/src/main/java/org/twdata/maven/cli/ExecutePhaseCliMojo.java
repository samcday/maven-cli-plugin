package org.twdata.maven.cli;

import org.twdata.maven.cli.commands.Command;
import java.util.HashMap;
import java.util.Map;
import org.twdata.maven.cli.commands.ExecutePhaseCommand;
import org.twdata.maven.cli.commands.PhaseCallBuilder;

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
    private Map<String, String> userAliases = new HashMap<String, String>();

    @Override
    protected void beforeExecute() {
        compactWhiteSpacesInUserAliases();
    }

    private void compactWhiteSpacesInUserAliases() {
        for (String key : userAliases.keySet()) {
            String value = userAliases.get(key).replaceAll("\\s{2,}", " ");
            userAliases.put(key, value);
        }
    }

    @Override
    protected Command getSpecializedCliMojoCommand() {
        PhaseCallBuilder commandCallBuilder = new PhaseCallBuilder(project, modules, userAliases);
        PhaseCallRunner runner = new PhaseCallRunner(session, project);

        return new ExecutePhaseCommand(userAliases.keySet(), modules.keySet(), commandCallBuilder,
                runner);
    }
}
