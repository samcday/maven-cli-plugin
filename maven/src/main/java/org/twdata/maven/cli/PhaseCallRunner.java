package org.twdata.maven.cli;

import java.io.File;
import org.apache.maven.Maven;
import org.apache.maven.execution.DefaultMavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.profiles.DefaultProfileManager;
import org.apache.maven.profiles.ProfileManager;
import org.apache.maven.project.MavenProject;

public class PhaseCallRunner {
    private final MavenSession session;
    private final MavenProject project;
    private final Log logger;
    private final File userDir;
    private boolean pluginExecutionOfflineMode;

    public PhaseCallRunner(MavenSession session, MavenProject project, Log logger) {
        this.session = session;
        this.project = project;
        this.logger = logger;
        this.userDir = new File(System.getProperty("user.dir"));
        this.pluginExecutionOfflineMode = session.getSettings().isOffline();
    }

    public void run(MavenProject currentProject, PhaseCall phaseCall) {
        try {
            // QUESTION: which should it be?
            session.getExecutionProperties().putAll(phaseCall.getProperties());
            //project.getProperties().putAll(commandCall.getProperties());

            session.setCurrentProject(currentProject);
            session.getSettings().setOffline(phaseCall.isOffline() ? true : pluginExecutionOfflineMode);
            ProfileManager profileManager = new DefaultProfileManager(session.getContainer(),
                    phaseCall.getProperties());
            profileManager.explicitlyActivate(phaseCall.getProfiles());
            MavenExecutionRequest request = new DefaultMavenExecutionRequest(
                    session.getLocalRepository(), session.getSettings(),
                    session.getEventDispatcher(),
                    phaseCall.getPhases(), userDir.getPath(),
                    profileManager, session.getExecutionProperties(),
                    project.getProperties(), true);
            if (!phaseCall.isRecursive()) {
                request.setRecursive(false);
            }
            request.setPomFile(new File(currentProject.getBasedir(), "pom.xml").getPath());
            ((Maven) session.lookup(Maven.ROLE)).execute(request);
            logger.info("Current project: " + project.getArtifactId());
        } catch (Exception e) {
            logger.error("Failed to execute '" + phaseCall.getPhases() + "' on '"
                    + currentProject.getArtifactId() + "'");
        }
    }
}
