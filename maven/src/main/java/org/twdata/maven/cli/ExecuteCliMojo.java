package org.twdata.maven.cli;

import java.util.HashMap;
import java.util.Map;

import org.twdata.maven.cli.commands.Command;
import org.twdata.maven.cli.commands.ExecuteGoalCommand;

/**
 * Provides an interactive command line interface for Maven plugins, allowing
 * users to execute plugins directly.
 *
 * @requiresDependencyResolution execute
 * @aggregator true
 * @goal execute
 */
public class ExecuteCliMojo extends AbstractCliMojo {
    /**
     * Command aliases. Commands should be in the form GROUP_ID:ARTIFACT_ID:GOAL
     *
     * @parameter
     */
    private Map<String, String> commands = new HashMap<String, String>();

    @Override
    protected void beforeExecute() {
        // do nothing
    }

    @Override
    protected Command getSpecializedCliMojoCommand() {
        return new ExecuteGoalCommand(project, session, commands, mavenPluginManager);
    }
}
