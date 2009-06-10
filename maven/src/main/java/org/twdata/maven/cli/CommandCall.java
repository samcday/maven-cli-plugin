package org.twdata.maven.cli;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
import org.apache.maven.project.MavenProject;


class CommandCall {
    private final List<String> commands;

    private final List<String> profiles;

    private final List<MavenProject> projects;

    private final Properties properties;

    private boolean offline;

    private boolean recursive;

    public CommandCall() {
        commands = new ArrayList<String>();
        profiles = new ArrayList<String>();
        projects = new ArrayList<MavenProject>();
        properties = new Properties();
        recursive = true;
        offline = false;
    }

    public List<MavenProject> getProjects() {
        return projects;
    }

    public List<String> getCommands() {
        return commands;
    }

    public List<String> getProfiles() {
        return profiles;
    }

    public Properties getProperties() {
        return properties;
    }

    public boolean isOffline() {
        return offline;
    }

    public void goOffline() {
        offline = true;
    }

    public boolean isRecursive() {
        return recursive;
    }

    public void doNotRecurse() {
        recursive = false;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (MavenProject project : projects) {
            sb.append("project: ").append(project.getArtifactId()).append(" ");
        }
        for (String command : commands) {
            sb.append("command: ").append(command).append(" ");
        }
        for (String profile : profiles) {
            sb.append("profile: ").append(profile).append(" ");
        }
        for (Enumeration propNames = properties.propertyNames(); propNames.hasMoreElements();) {
            String propName = (String) propNames.nextElement();
            sb.append("property: ").append(propName).append("=").append(properties.get(propName)).append(" ");
        }
        return sb.toString();
    }
}
