package org.twdata.maven.cli.commands;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.PluginManager;
import org.apache.maven.project.MavenProject;
import org.twdata.maven.cli.MojoCall;
import org.twdata.maven.cli.console.CliConsole;

public class ExecuteGoalCommand implements Command {
    private final Map<String, String> defaultGoals = Collections
            .unmodifiableMap(new HashMap<String, String>() {
                {
                    put("compile",
                            "org.apache.maven.plugins:maven-compiler-plugin:compile");
                    put("testCompile",
                            "org.apache.maven.plugins:maven-compiler-plugin:testCompile");
                    put("jar", "org.apache.maven.plugins:maven-jar-plugin:jar");
                    put("war", "org.apache.maven.plugins:maven-war-plugin:war");
                    put("resources",
                            "org.apache.maven.plugins:maven-resources-plugin:resources");
                    put("testResources",
                            "org.apache.maven.plugins:maven-resources-plugin:testResources");
                    put("install",
                            "org.apache.maven.plugins:maven-install-plugin:install");
                    put("deploy",
                            "org.apache.maven.plugins:maven-deploy-plugin:deploy");
                    put("test",
                            "org.apache.maven.plugins:maven-surefire-plugin:test");
                    put("clean",
                            "org.apache.maven.plugins:maven-clean-plugin:clean");

                    //Help plugins not requiring parameters
                    put("help-system",
                            "org.apache.maven.plugins:maven-help-plugin:system");
                    put("help-effectivesettings",
                            "org.apache.maven.plugins:maven-help-plugin:effective-settings");
                    put("help-allprofiles",
                            "org.apache.maven.plugins:maven-help-plugin:all-profiles");

                    //Dependency plugins for analysis and management
                    put("dependency-tree",
                            "org.apache.maven.plugins:maven-dependency-plugin:tree");
                    put("dependency-resolve",
                            "org.apache.maven.plugins:maven-dependency-plugin:resolve");
                    put("dependency-resolve-plugins",
                            "org.apache.maven.plugins:maven-dependency-plugin:resolve-plugins");
                    put("dependency-purge",
                            "org.apache.maven.plugins:maven-dependency-plugin:purge-local-repository");
                    put("dependency-analyze",
                            "org.apache.maven.plugins:maven-dependency-plugin:analyze");
                }
            });
    private final Map<String, String> userDefinedAliases;
    private final MavenProject project;
    private final MavenSession session;
    private final CliConsole console;
    private final PluginManager pluginManager;

    public ExecuteGoalCommand(MavenProject project, MavenSession session,
            PluginManager pluginManager, CliConsole console, Map<String, String> userDefinedAliases) {
        this.project = project;
        this.session = session;
        this.pluginManager = pluginManager;
        this.console = console;
        this.userDefinedAliases = userDefinedAliases;
    }

    public void describe(CommandDescription description) {
        description.describeCommandName("Goal commands");

        for (String goal : defaultGoals.keySet()) {
            description.describeCommandToken(goal, defaultGoals.get(goal));
        }

        for (String alias : userDefinedAliases.keySet()) {
            description.describeCommandToken(alias, userDefinedAliases.get(alias));
        }
    }

    public Set<String> getCommandNames() {
        Set<String> commandNames = new HashSet<String>();
        commandNames.addAll(defaultGoals.keySet());
        commandNames.addAll(userDefinedAliases.keySet());
        return commandNames;
    }

    public boolean matchesRequest(String request) {
        for (String token : request.split(" ")) {
            if (!defaultGoals.containsKey(token) && !userDefinedAliases.containsKey(token)) {
                if (token.split(":").length < 3) {
                    return false;
                }
            }
        }

        return true;
    }

    public boolean run(String request) {
        for (String token : request.split(" ")) {
            try {
                if (defaultGoals.containsKey(token)) {
                    runMojo(defaultGoals.get(token));
                } else if (userDefinedAliases.containsKey(token)) {
                    run(userDefinedAliases.get(token));
                } else {
                    runMojo(token);
                }
            } catch (MojoExecutionException ex) {
                throw new RuntimeException(ex);
            }
        }

        return true;
    }

    private void runMojo(String mojoString) throws MojoExecutionException {
        String[] mojoInfo = mojoString.split(":");
        MojoCall call = new MojoCall(mojoInfo[0], mojoInfo[1], mojoInfo[2]);

        console.writeInfo("Executing: " + call);
        long start = System.currentTimeMillis();

        call.run(project, session, pluginManager);

        long now = System.currentTimeMillis();
        console.writeInfo("Current project: " + project.getArtifactId());
        console.writeInfo("Execution time: " + (now - start) + " ms");

    }
}
