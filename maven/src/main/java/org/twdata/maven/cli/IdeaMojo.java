package org.twdata.maven.cli;

import static org.twdata.maven.mojoexecutor.MojoExecutor.artifactId;
import static org.twdata.maven.mojoexecutor.MojoExecutor.configuration;
import static org.twdata.maven.mojoexecutor.MojoExecutor.executeMojo;
import static org.twdata.maven.mojoexecutor.MojoExecutor.executionEnvironment;
import static org.twdata.maven.mojoexecutor.MojoExecutor.goal;
import static org.twdata.maven.mojoexecutor.MojoExecutor.groupId;
import static org.twdata.maven.mojoexecutor.MojoExecutor.plugin;
import static org.twdata.maven.mojoexecutor.MojoExecutor.version;

import java.io.*;
import java.util.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

import jline.ConsoleReader;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.PluginManager;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.StringUtils;

/**
 * Installs an IDEA plugin that sends commands to a listening CLI port
 *
 * @requiresDependencyResolution execute
 * @aggregator true
 * @goal idea
 */
public class IdeaMojo extends AbstractMojo
{
    public void execute() throws MojoExecutionException
    {
        String path = System.getProperty("idea.home");
        if (path == null)
        {
            throw new MojoExecutionException("The IDEA home directory must be specified via the 'idea.home' property");
        }
        File ideaHome = new File(path);
        if (!ideaHome.exists())
        {
            throw new MojoExecutionException("The IDEA home directory doesn't exist");
        }

        File pluginsDir = new File(ideaHome, "plugins");
        if (!pluginsDir.exists())
        {
            throw new MojoExecutionException("The IDEA plugins directory cannot be found");
        }

        File pluginFile = new File(pluginsDir, "maven-cli.jar");
        if (pluginFile.exists())
        {
            pluginFile.delete();
        }

        InputStream source = null;
        OutputStream dest = null;
        try
        {
            dest = new FileOutputStream(pluginFile);
            source = getClass().getResourceAsStream("/maven-cli.jar");
            byte[] buffer = new byte[1024];
            int len;
            while ((len = source.read(buffer)) > 0)
            {
                dest.write(buffer, 0, len);
            }
        } catch (IOException e) {
            throw new MojoExecutionException("Unable to copy IDEA plugin", e);
        }
        finally
        {
            if (source != null)
            {
                    try {
                    source.close();
                } catch (IOException e) {
                    e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                }
            }
            if (dest != null)
            {
                    try {
                    dest.close();
                } catch (IOException e) {
                    e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                }
            }
        }
        getLog().info("IDEA plugin installed");
    }
}