package org.openremote.agent.protocol.controller.command;

import org.openremote.agent.protocol.controller.ControllerCommand;
import org.openremote.agent.protocol.http.HTTPProtocol;
import org.openremote.model.attribute.AttributeRef;

import java.util.function.Consumer;

/**
 * A ControllerCommandBasic represent a configuration to execute a Controller 2.5 command with just a command name
 * (command parameter is managed at launch depending on linked attribute value {@link
 * org.openremote.agent.protocol.controller.ControllerProtocol#executeAttributeWriteRequest(HTTPProtocol.HttpClientRequest,
 * AttributeRef, Consumer)}).
 */
public class ControllerCommandBasic extends ControllerCommand {
    private String commandName;

    public ControllerCommandBasic() {
        super();
        this.commandName = "";
    }

    public ControllerCommandBasic(String deviceName, String commandName) {
        super(deviceName);
        this.commandName = commandName;
    }

    public String getCommandName() {
        return commandName;
    }

    public void setCommandName(String commandName) {
        this.commandName = commandName;
    }
}
