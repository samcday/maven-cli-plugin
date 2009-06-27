package org.twdata.maven.cli;

import org.apache.maven.project.MavenProject;

public class CommandCallTestDataBuilder {
    private PhaseCall commandCall = new PhaseCall();

    public CommandCallTestDataBuilder hasPhases(String... phases) {
        for (String phase : phases) {
            commandCall.getPhases().add(phase);
        }

        return this;
    }

    public PhaseCall build() {
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

    public CommandCallTestDataBuilder notRecursing() {
        commandCall.doNotRecurse();
        return this;
    }

    public CommandCallTestDataBuilder skippingTests() {
        commandCall.getProperties().put("maven.test.skip", "true");
        return this;
    }

    public CommandCallTestDataBuilder hasProfiles(String... profiles) {
        for (String profile : profiles) {
            commandCall.getProfiles().add(profile);
        }

        return this;
    }

    public CommandCallTestDataBuilder hasProperties(String... properties) {
        for (String property : properties) {
            String[] propValue = property.split("=");
            commandCall.getProperties().put(propValue[0], propValue[1]);
        }

        return this;
    }

    public static CommandCallTestDataBuilder aCommandCall() {
        return new CommandCallTestDataBuilder();
    }
}
