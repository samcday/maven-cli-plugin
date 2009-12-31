package org.twdata.maven.cli;

import java.io.File;
import org.apache.maven.Maven;
import org.apache.maven.execution.DefaultMavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.profiles.DefaultProfileManager;
import org.apache.maven.profiles.ProfileManager;
import org.apache.maven.project.MavenProject;
import org.twdata.maven.cli.console.CliConsole;

public class PhaseCallRunner {
    private final MavenSession session;
    private final MavenProject project;
    private final File userDir;
    private boolean pluginExecutionOfflineMode;

    public PhaseCallRunner(MavenSession session, MavenProject project) {
        this.session = session;
        this.project = project;
        this.userDir = new File(System.getProperty("user.dir"));
        this.pluginExecutionOfflineMode = session.getSettings().isOffline();
    }

    public boolean run(MavenProject currentProject, PhaseCall phaseCall, CliConsole console) {
        try {
            // QUESTION: which should it be?
            session.getExecutionProperties().putAll(phaseCall.getProperties());
            //project.getProperties().putAll(commandCall.getProperties());

            session.setCurrentProject(currentProject);
            session.getSettings().setOffline(phaseCall.isOffline() ? true : pluginExecutionOfflineMode);
            ProfileManager profileManager = new DefaultProfileManager(session.getContainer());
            profileManager.explicitlyActivate(phaseCall.getProfiles());
            MavenExecutionRequest request = new DefaultMavenExecutionRequest();
            if (!phaseCall.isRecursive()) {
                request.setRecursive(false);
            }
            //request.setPomFile(new File(currentProject.getBasedir(), "pom.xml").getPath());
            //((Maven) session.lookup(Maven.ROLE)).execute(request);
            console.writeInfo("Current project: " + project.getArtifactId());
            return true;
        } catch (Exception e) {
            console.writeError("Failed to execute '" + phaseCall.getPhases() + "' on '"
                    + currentProject.getArtifactId() + "'");
            return false;
        }
    }
}
