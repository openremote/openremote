package org.openremote.controller.command;

import org.openremote.controller.command.builtin.DateTimeCommand;
import org.openremote.controller.command.builtin.VirtualCommand;
import org.openremote.controller.deploy.CommandDefinition;

/**
 * Build protocol- and device-specific command implementation.
 */
public class CommandFactory {

    public Command build(CommandDefinition commandDefinition) {

        // Handle some built-in protocols
        switch (commandDefinition.getProtocolType()) {
            case "virtual":
                return new VirtualCommand(commandDefinition);
            case "datetime":
                return new DateTimeCommand(commandDefinition);
        }

        return null;
    }

}
