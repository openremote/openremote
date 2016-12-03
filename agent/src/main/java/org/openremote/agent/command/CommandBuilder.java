package org.openremote.agent.command;

import org.openremote.agent.command.builtin.DateTimeCommand;
import org.openremote.agent.command.builtin.VirtualCommand;
import org.openremote.agent.deploy.CommandDefinition;

/**
 * Build protocol- and device-specific command implementation.
 */
public class CommandBuilder {

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
