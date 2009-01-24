package org.twdata.maven.cli;

/**
 * Created by IntelliJ IDEA.
 * User: mrdon
 * Date: 24/01/2009
 * Time: 2:36:16 PM
 * To change this template use File | Settings | File Templates.
 */
public class RemoteCommand {
    private String host;
    private int port;
    private String command;

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
}
