package org.twdata.maven.cli.commands;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import org.twdata.maven.cli.CommandTokenCollector;
import org.twdata.maven.cli.console.CliConsole;
import org.twdata.maven.cli.PhaseCall;
import org.twdata.maven.cli.PhaseCallRunner;

public class ExecutePhaseCommand implements Command {
    private final Set<String> modules;
    private final PhaseCallBuilder commandCallBuilder;
    private final PhaseCallRunner runner;
    private final SortedSet<String> phasesAndProperties = new TreeSet<String>();

    public ExecutePhaseCommand(Set<String> modules, PhaseCallBuilder commandCallBuilder,
            PhaseCallRunner runner) {
        this.modules = modules;
        this.commandCallBuilder = commandCallBuilder;
        this.runner = runner;

        phasesAndProperties.add("clean");
        phasesAndProperties.add("compile");
        phasesAndProperties.add("validate");
        phasesAndProperties.add("generate-sources");
        phasesAndProperties.add("generate-resources");
        phasesAndProperties.add("test-compile");
        phasesAndProperties.add("test");
        phasesAndProperties.add("package");
        phasesAndProperties.add("integration-test");
        phasesAndProperties.add("install");
        phasesAndProperties.add("deploy");
        phasesAndProperties.add("site");
        phasesAndProperties.add("site-deploy");
        phasesAndProperties.add("-o"); // offline mode
        phasesAndProperties.add("-N"); // don't recurse
        phasesAndProperties.add("-S"); // skip tests
    }

    public void describe(CommandDescription description) {
        description.describeCommandName("Phase Commands");

        for (String phase : phasesAndProperties) {
            if (!phase.startsWith("-")) {
                description.describeCommandToken(phase, null);
            }
        }
    }

    public void collectCommandTokens(CommandTokenCollector collector) {
        collector.addCommandTokens(phasesAndProperties);
        collector.addCommandTokens(modules);
    }

    public boolean matchesRequest(String request) {
        for (String token : request.split(" ")) {
            if (!phasesAndProperties.contains(token) && !token.startsWith("-D")
                    && !token.startsWith("-P") && !matchesModules(token)) {
                return false;
            }
        }

        return true;
    }

    private boolean matchesModules(String token) {
        String regex = token.replaceAll("\\*", ".*").replaceAll("\\?", "\\\\?");
        for (String module : modules) {
            if (module.matches(regex)) {
                return true;
            }
        }

        return false;
    }

    public boolean run(String request, CliConsole console) {
        try {
            List<PhaseCall> calls = new ArrayList<PhaseCall>();
            calls = commandCallBuilder.parseCommand(request);

            for (PhaseCall call : calls) {
                console.writeDebug("Executing: " + call);
                long start = System.currentTimeMillis();
                call.run(runner, console);
                long now = System.currentTimeMillis();
                console.writeInfo("Execution time: " + (now - start) + " ms");
            }
        } catch (IllegalArgumentException ex) {
            console.writeError("Invalid command: " + request);
        } finally {
            return true;
        }
    }
}
