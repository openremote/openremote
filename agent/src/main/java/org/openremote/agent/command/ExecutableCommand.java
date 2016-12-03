package org.openremote.agent.command;

public interface ExecutableCommand extends Command {

    /**
     * Send some executable command to device.
     */
    void send(String value);
}
