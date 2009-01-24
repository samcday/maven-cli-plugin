package org.twdata.maven.cli;

import com.intellij.openapi.components.ProjectComponent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.util.JDOMExternalizable;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.util.DefaultJDOMExternalizer;
import com.intellij.openapi.util.WriteExternalException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nls;
import org.twdata.maven.cli.config.CliClientConfigurationForm;
import org.jdom.Element;

import javax.swing.*;
import java.net.Socket;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.IOException;

/**
 * Created by IntelliJ IDEA.
 * User: mrdon
 * Date: 24/01/2009
 * Time: 2:28:49 PM
 * To change this template use File | Settings | File Templates.
 */
public class CliClientProjectComponent implements ProjectComponent, Configurable, JDOMExternalizable {

    public RemoteCommand[] commands;

    private CliClientConfigurationForm form;

    public CliClientProjectComponent(Project project) {
    }

    public void sendCommand(int index) {
        RemoteCommand cmd = commands[index-1];
        try {
            Socket socket = new Socket(cmd.getHost(), cmd.getPort());
            OutputStream out = socket.getOutputStream();
            new PrintStream(out).append(cmd.getCommand()).append("\n");
            socket.close();
        } catch (IOException e1) {
            Messages.showMessageDialog(
                "Unable to send command to "+cmd.getHost()+" on port "+cmd.getPort()+": "+e1.getMessage(),
                "Error sending command",
                Messages.getErrorIcon()
            );
            e1.printStackTrace();
        }
    }

    public void initComponent() {
        if (commands == null) {
            commands = new RemoteCommand[] {
                new RemoteCommand("localhost", 4330, "compile resources jar install"),
                new RemoteCommand("localhost", 4331, "compile resources jar install"),
                new RemoteCommand("localhost", 4332, "compile resources jar install")
            };
        }
    }

    public void disposeComponent() {
        // TODO: insert component disposal logic here
    }

    @NotNull
    public String getComponentName() {
        return "org.twdata.maven.cli.CliClientProjectComponent";
    }

    public void projectOpened() {
        // called when project is opened
    }

    public void projectClosed() {
        // called when project is being closed
    }

    public RemoteCommand[] getRemoteCommands() {
        return commands;
    }

    @Nls
    public String getDisplayName() {
        return "Maven CLI Client";
    }

    public Icon getIcon() {
        return null;
    }

    public String getHelpTopic() {
        return null;
    }

    public JComponent createComponent() {
        if (form == null) {
            form = new CliClientConfigurationForm();
        }
        return form.getRootComponent();
    }

    public boolean isModified() {
        return form != null && form.isModified(this);
    }

    public void apply() throws ConfigurationException {
        if (form != null) {
           // Get data from form to component
           form.getData(this);
       }

    }

    public void reset() {
        if (form != null) {
           // Reset form data from component
           form.setData(this);
       }

    }

    public void disposeUIResources() {
        form = null;
    }

    public void readExternal(Element element) throws InvalidDataException {
        DefaultJDOMExternalizer.readExternal(this, element);
    }

    public void writeExternal(Element element) throws WriteExternalException {
        DefaultJDOMExternalizer.writeExternal(this, element);
    }
}
