package org.openremote.controller.command;

public interface ExecutableCommand extends Command {

    /**
     * Send some executable command to device.
     */
    void send(String value);
}
