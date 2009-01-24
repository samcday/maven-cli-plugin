package org.twdata.maven.cli;

import com.intellij.openapi.util.JDOMExternalizable;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.util.DefaultJDOMExternalizer;
import com.intellij.openapi.util.WriteExternalException;

import java.io.Serializable;

import org.jdom.Element;

/**
 * Created by IntelliJ IDEA.
 * User: mrdon
 * Date: 24/01/2009
 * Time: 2:36:16 PM
 * To change this template use File | Settings | File Templates.
 */
public class RemoteCommand implements Serializable, JDOMExternalizable {
    public String host;
    public int port;
    public String command;

    public RemoteCommand()
    {}

    public RemoteCommand(String host, int port, String command) {
        this.host = host;
        this.port = port;
        this.command = command;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getCommand() {
        return command;
    }

    public void setCommand(String command) {
        this.command = command;
    }

    public void readExternal(Element element) throws InvalidDataException {
        DefaultJDOMExternalizer.readExternal(this, element);
    }

    public void writeExternal(Element element) throws WriteExternalException {
        DefaultJDOMExternalizer.writeExternal(this, element);
    }
}
