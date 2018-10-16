package org.openremote.agent.protocol.controller;

/**
 * ControllerCommand represent an executable command on the Controller
 *
 * Depending if the command to send to the Controller is based on META {@link ControllerProtocol#META_ATTRIBUTE_COMMAND_NAME } or {@link ControllerProtocol#META_ATTRIBUTE_COMMANDS_MAP}, we'll create an instance of ControllerCommand as {@link org.openremote.agent.protocol.controller.command.ControllerCommandBasic} or {@link org.openremote.agent.protocol.controller.command.ControllerCommandMapped}
 *
 * <p>
 * Date : 31-Aug-18
 *
 * @author jerome.vervier
 */
public class ControllerCommand {
    private String deviceName;

    public ControllerCommand() {
        this.deviceName = "";
    }

    public ControllerCommand(String deviceName) {
        this.deviceName = deviceName;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }
}
