package org.openremote.controller.command;

import org.openremote.controller.model.CommandDefinition;

public abstract class CommandFactory {

    public Command getCommand(CommandDefinition commandDefinition) throws Exception {
        if (commandDefinition == null) {
            throw new Exception("Null reference trying to create a protocol command");
        }

        String protocolType = commandDefinition.getProtocolType();

        if (protocolType == null || protocolType.equals("")) {
            throw new Exception(
                "Protocol attribute is missing: " + commandDefinition
            );
        }

        return buildCommand(commandDefinition);
    }

    protected abstract Command buildCommand(CommandDefinition commandDefinition);
}
