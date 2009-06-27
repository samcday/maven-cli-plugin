package org.twdata.maven.cli;

import java.io.File;
import org.apache.maven.Maven;
import org.apache.maven.execution.DefaultMavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.profiles.DefaultProfileManager;
import org.apache.maven.profiles.ProfileManager;
import org.apache.maven.project.MavenProject;

class PhaseCallRunner {
    private final MavenSession session;
    private final MavenProject project;
    private final Log logger;
    private final File userDir;
    private boolean pluginExecutionOfflineMode;

    public PhaseCallRunner(MavenSession session, MavenProject project,
            Log logger) throws MojoExecutionException {
        this.session = session;
        this.project = project;
        this.logger = logger;
        this.userDir = new File(System.getProperty("user.dir"));
        this.pluginExecutionOfflineMode = session.getSettings().isOffline();
    }

    public void executeCommand(PhaseCall commandCall) {
        for (MavenProject currentProject : commandCall.getProjects()) {
            try {
                // QUESTION: which should it be?
                session.getExecutionProperties().putAll(commandCall.getProperties());
                //project.getProperties().putAll(commandCall.getProperties());

                session.setCurrentProject(currentProject);
                session.getSettings().setOffline(commandCall.isOffline() ? true : pluginExecutionOfflineMode);
                ProfileManager profileManager = new DefaultProfileManager(session.getContainer(),
                        commandCall.getProperties());
                profileManager.explicitlyActivate(commandCall.getProfiles());
                MavenExecutionRequest request = new DefaultMavenExecutionRequest(
                        session.getLocalRepository(), session.getSettings(),
                        session.getEventDispatcher(),
                        commandCall.getPhases(), userDir.getPath(),
                        profileManager, session.getExecutionProperties(),
                        project.getProperties(), true);
                if (!commandCall.isRecursive()) {
                    request.setRecursive(false);
                }
                request.setPomFile(new File(currentProject.getBasedir(),
                        "pom.xml").getPath());
                ((Maven) session.lookup(Maven.ROLE)).execute(request);
                logger.info("Current project: " + project.getArtifactId());
            } catch (Exception e) {
                logger.error(
                        "Failed to execute '" + commandCall.getPhases()
                                + "' on '" + currentProject.getArtifactId()
                                + "'");
            }
        }
    }
}
