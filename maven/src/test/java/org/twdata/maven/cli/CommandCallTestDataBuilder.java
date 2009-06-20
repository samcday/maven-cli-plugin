package org.twdata.maven.cli;

import org.apache.maven.project.MavenProject;

public class CommandCallTestDataBuilder {
    private CommandCall commandCall = new CommandCall();

    public CommandCallTestDataBuilder hasPhases(String... phases) {
        for (String phase : phases) {
            commandCall.getCommands().add(phase);
        }

        return this;
    }

    public CommandCall build() {
        return commandCall;
    }

    public CommandCallTestDataBuilder runsOffline() {
        commandCall.goOffline();
        return this;
    }

    public CommandCallTestDataBuilder hasProjects(MavenProject... projects) {
        for (MavenProject project : projects) {
            commandCall.getProjects().add(project);
        }

        return this;
    }

    public static CommandCallTestDataBuilder aCommandCall() {
        return new CommandCallTestDataBuilder();
    }
}
