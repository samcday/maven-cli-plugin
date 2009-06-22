package org.twdata.maven.cli;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.StringUtils;

class CommandCallBuilder {
    private final MavenProject defaultProject;
    private final Map<String, MavenProject> modules;
    private final Map<String, String> userAliases;

    public CommandCallBuilder(MavenProject project, Map<String, MavenProject> modules,
            Map<String, String> userAliases) {
        defaultProject = project;
        this.modules = modules;
        this.userAliases = userAliases;
    }

    public List<CommandCall> parseCommand(String text) {
        List<CommandCall> commands = new ArrayList<CommandCall>();
        CommandCall currentCommandCall = null;

        for (String token : resolveUserAliases(text)) {
            if (modules.containsKey(token)) {
                currentCommandCall = addProject(commands, currentCommandCall,
                        modules.get(token));
            } else if (token.contains("*")) {
                String regexToken = token.replaceAll("\\*", ".*");
                for (String moduleName : modules.keySet()) {
                    if (Pattern.matches(regexToken, moduleName)) {
                        currentCommandCall = addProject(commands,
                                currentCommandCall, modules.get(moduleName));
                    }
                }
            } else if (token.equals("-o")) {
                goOffline(commands, currentCommandCall);
            } else if (token.equals("-N")) {
                disableRecursive(commands, currentCommandCall);
            } else if (token.equals("-S")) {
                addProperty(commands, currentCommandCall, "-Dmaven.test.skip=true");
            } else if (token.startsWith("-D")) {
                addProperty(commands, currentCommandCall, token);
            } else if (token.startsWith("-P")) {
                addProfile(commands, currentCommandCall, token);
            } else {
                currentCommandCall = addCommand(commands, currentCommandCall,
                        token);
            }
        }

        return commands;
    }

    private List<String> resolveUserAliases(String text) {
        List<String> result = new ArrayList<String>();

        for (String token : text.split(" ")) {
            if (userAliases.containsKey(token)) {
                result.addAll(resolveUserAliases(userAliases.get(token)));
            } else if (!StringUtils.isEmpty(token)) {
                result.add(token);
            }
        }

        return result;
    }

    private CommandCall addProject(List<CommandCall> commands,
                                   CommandCall currentCommandCall, MavenProject project) {
        if (currentCommandCall == null
                || !currentCommandCall.getCommands().isEmpty()) {
            currentCommandCall = new CommandCall();
            commands.add(currentCommandCall);
        }
        currentCommandCall.getProjects().add(project);
        return currentCommandCall;
    }

    private CommandCall addCommand(List<CommandCall> commands,
                                   CommandCall currentCommandCall, String command) {
        if (currentCommandCall == null) {
            currentCommandCall = new CommandCall();
            currentCommandCall.getProjects().add(defaultProject);
            commands.add(currentCommandCall);
        }
        currentCommandCall.getCommands().add(command);
        return currentCommandCall;
    }

    private void disableRecursive(List<CommandCall> commands,
                                    CommandCall currentCommandCall) {
        if (currentCommandCall == null) {
            currentCommandCall = new CommandCall();
            commands.add(currentCommandCall);
        }
        currentCommandCall.doNotRecurse();
    }

    private void goOffline(List<CommandCall> commands,
                                    CommandCall currentCommandCall) {
        if (currentCommandCall == null) {
            currentCommandCall = new CommandCall();
            commands.add(currentCommandCall);
        }
        currentCommandCall.goOffline();
    }

    private void addProfile(List<CommandCall> commands,
                                    CommandCall currentCommandCall, String profile) {
        if (currentCommandCall == null) {
            currentCommandCall = new CommandCall();
            commands.add(currentCommandCall);
        }
        // must have characters after -P
        if (profile.length() < 3) {
            return;
        }

        profile = profile.substring(2);
        currentCommandCall.getProfiles().add(profile);
    }

    private void addProperty(List<CommandCall> commands,
                                    CommandCall currentCommandCall, String property) {
        if (property.length() < 3 || hasNoKeyValue(property)) {
            return;
        }

        if (currentCommandCall == null) {
            currentCommandCall = new CommandCall();
            commands.add(currentCommandCall);
        }
        property = property.substring(2);
        String key = property;
        String value = "1";
        if (property.indexOf("=") >= 0) {
            String[] propertyTokens = property.split("=");
            key = propertyTokens[0];
            if (propertyTokens.length > 1) {
                value = propertyTokens[1];
            }
        }
        currentCommandCall.getProperties().put(key, value);
    }

    private boolean hasNoKeyValue(String property) {
        return property.charAt(2) == '=' || !property.contains("=")
                || property.endsWith("=");
    }
}
