package org.openremote.agent.protocol.controller.command;

import org.openremote.agent.protocol.controller.ControllerCommand;

import java.util.HashMap;
import java.util.Map;

/**
 * A ControllerCommandMapped represent a configuration to execute a Controller 2.5 command where exact command name to execute depend on a linked
 * attribute value and the match between linked attribute value and command name is stored in a map where key is attribute value and value is the
 * command name to execute
 *
 * <h2>Example:</h2>
 * Control a fan with 3 speeds: low, mid, high
 * <ul>
 *     <li>Controller 2.x: a command to read the state of the fan and a sensor associated with it + commands with no parameter to turn fan off, set it
 *     to low speed, medium speed or high speed</li>
 *     <li>Manager 3.0: a read-write attribute (enum if it exists or string). Its value reflects the state of the sensor. If it is written to,
 *     appropriate command is sent to controller based on written value.</li>
 * </ul>
 * <h3>Mapping:</h3>
 * The end user defines the device_name and sensor_name to read the attribute value + the device_name for command execution and a mapping table for
 * the command execution e.g.
 * <blockquote><pre>
 * {@code
 * {
 *     "off": "cmd1",
 *     "low": "cmd2",
 *     "mid": "cmd3",
 *     "high": "cmd4"
 * }
 * }
 * </pre></blockquote>
 */
public class ControllerCommandMapped extends ControllerCommand {
    private Map<String, String> actionCommandLink;

    public ControllerCommandMapped() {
        super();
        this.actionCommandLink = new HashMap<>();
    }

    public ControllerCommandMapped(String deviceName, Map<String, String> actionCommandLink) {
        super(deviceName);
        this.actionCommandLink = actionCommandLink;
    }

    public Map<String, String> getActionCommandLink() {
        return actionCommandLink;
    }

    public void setActionCommandLink(Map<String, String> actionCommandLink) {
        this.actionCommandLink = actionCommandLink;
    }
}
