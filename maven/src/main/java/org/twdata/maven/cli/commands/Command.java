package org.twdata.maven.cli.commands;

import java.util.Set;

public interface Command {
    void describe(CommandDescription description);
    
    /**
     * @return the set of command names this command will respond to.
     */
    Set<String> getCommandNames();

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
    boolean run(String request);
}
