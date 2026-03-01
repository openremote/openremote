package org.openremote.model.telematics.protocol;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;

/**
 * Simple command wrapper for telematics devices.
 */
public class DeviceCommand {

    private final String command;

    public DeviceCommand(String command) {
        this.command = Objects.requireNonNull(command, "command cannot be null").trim();
        if (this.command.isEmpty()) {
            throw new IllegalArgumentException("command cannot be empty");
        }
    }

    public static DeviceCommand setOutput(int outputNumber, boolean state) {
        return new DeviceCommand("setdigout " + outputNumber + " " + (state ? "1" : "0"));
    }

    public static DeviceCommand text(String command) {
        return new DeviceCommand(command);
    }

    public static DeviceCommand of(String command) {
        return new DeviceCommand(command);
    }

    public String getCommand() {
        return command;
    }

    @Override
    public String toString() {
        return "DeviceCommand{" + "command='" + command + '\'' + '}';
    }
}
