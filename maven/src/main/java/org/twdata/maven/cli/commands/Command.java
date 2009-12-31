package org.twdata.maven.cli.commands;

import org.twdata.maven.cli.CommandTokenCollector;
import org.twdata.maven.cli.console.CliConsole;
import org.apache.maven.plugin.MojoFailureException;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;

public interface Command {
    void describe(CommandDescription description);

    void collectCommandTokens(CommandTokenCollector collector);
    
    /**
     *
     * @param request the command request.
     * @return true if the request is one of the command names this command will
     * respond to.
     */
    boolean matchesRequest(String request);

    /**
     *
     * @param request
     * @return false if the command wants to end the mojo after it finish
     * running run().
     */
    public boolean run(String request, CliConsole console) throws MojoFailureException, ComponentLookupException;
}
