package org.twdata.maven.cli;

import java.util.List;
import org.apache.maven.model.Plugin;
import org.apache.maven.project.MavenProject;

public class MojoCall {
    private final String groupId;
    private final String artifactId;
    private final String goal;

    public MojoCall(String groupId, String artifactId, String goal) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.goal = goal;
    }

    public String getGroupId() {
        return groupId;
    }

    public String getArtifactId() {
        return artifactId;
    }

    public String getGoal() {
        return goal;
    }

    /**
     * Tries to determine what version of the plugin has been already
     * configured for this project. If unknown, "RELEASE" is used.
     *
     * @param project The maven project
     * @return The discovered plugin version
     */
    public String getVersion(MavenProject project) {
        String version = null;

        @SuppressWarnings("unchecked")
        List<Plugin> plugins = project.getBuildPlugins();

        for (Plugin plugin : plugins) {
            if (groupId.equals(plugin.getGroupId())
                    && artifactId.equals(plugin.getArtifactId())) {
                version = plugin.getVersion();
                break;
            }
        }

        if (version == null) {
            plugins = project.getPluginManagement().getPlugins();
            for (Plugin plugin : plugins) {
                if (groupId.equals(plugin.getGroupId())
                        && artifactId.equals(plugin.getArtifactId())) {
                    version = plugin.getVersion();
                    break;
                }
            }
        }

        if (version == null) {
            version = "RELEASE";
        }
        return version;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(groupId).append(":").append(artifactId);
        sb.append(" [").append(goal).append("]");
        return sb.toString();
    }
}
