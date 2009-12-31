package org.twdata.maven.cli;

import java.util.List;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.PluginManager;
import org.apache.maven.plugin.MavenPluginManager;
import org.apache.maven.project.MavenProject;
import static org.twdata.maven.cli.MojoExecutor.artifactId;
import static org.twdata.maven.cli.MojoExecutor.configuration;
import static org.twdata.maven.cli.MojoExecutor.executeMojo;
import static org.twdata.maven.cli.MojoExecutor.executionEnvironment;
import static org.twdata.maven.cli.MojoExecutor.goal;
import static org.twdata.maven.cli.MojoExecutor.groupId;
import static org.twdata.maven.cli.MojoExecutor.plugin;
import static org.twdata.maven.cli.MojoExecutor.version;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.component.annotations.Component;

public class MojoCall {
    private String groupId;
    private String artifactId;
    private String goal;
    private MavenPluginManager mavenPluginManager;

    public MojoCall(String groupId, String artifactId, String goal, MavenPluginManager mavenPluginManager) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.goal = goal;
        this.mavenPluginManager = mavenPluginManager;
    }

    public void run(MavenProject project, MavenSession session, PluginManager pluginManager)
            throws MojoExecutionException {
//      MojoExecutionException mojoDescriptor = new MojoDescriptor();
//      MojoExecution execution = new MojoExecution( mojoDescriptor, "default-cli", MojoExecution.Source.CLI );
//      pluginManager.executeMojo(project, execution, session);
        executeMojo(
                plugin(groupId(groupId), artifactId(artifactId), version(getVersion(project))),
                goal(goal),
                configuration(),
                executionEnvironment(project, session, pluginManager), mavenPluginManager);
    }

    /**
     * Tries to determine what version of the plugin has been already
     * configured for this project. If unknown, "RELEASE" is used.
     *
     * @param project The maven project
     * @return The discovered plugin version
     */
    private String getVersion(MavenProject project) {
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
