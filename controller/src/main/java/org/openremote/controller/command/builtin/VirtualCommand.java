package org.openremote.controller.command.builtin;

import org.openremote.controller.command.ExecutableCommand;
import org.openremote.controller.command.PullCommand;
import org.openremote.controller.model.*;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * OpenRemote virtual command implementation.
 * <p>
 * Maintains a virtual-machine-wide state for each address. This can be used for testing.
 * Incoming read() and send() requests operate on a map where the command's {@code <address>}
 * field is used as a key and either the command name, or command parameter (if provided) is
 * stored as a virtual device state value for that address.
 */
public class VirtualCommand implements ExecutableCommand, PullCommand {

    private static final Logger LOG = Logger.getLogger(VirtualCommand.class.getName());

    private final static Map<String, String> virtualDevices = new ConcurrentHashMap<String, String>(20);
    private String address = null;
    private String command = null;
    private String commandParam = null;

    public VirtualCommand(CommandDefinition commandDefinition) {
        this(
            commandDefinition.getProperty("address"),
            commandDefinition.getProperty("status")
        );
    }

    /**
     * Constructs a new "virtual" device command with a given address and command.
     * <p>
     * When this command's {@link #send} method is called, the command name is stored as a
     * virtual device state value for the given device address.
     *
     * @param address arbitrary address string that is used to store the command value in memory
     * @param command arbitrary command string that is stored in memory and can be later retrieved
     *                via invoking {@link #read}.
     */
    public VirtualCommand(String address, String command) {
        this.address = address;
        this.command = command;
    }

    /**
     * Constructs a new "virtual" device command with a given address, command, and command parameter.
     * <p>
     * When this command's {@link #send(String)} method is called, the command's parameter value is stored
     * as a virtual device state value for the given address. This type of command can therefore
     * be used to test components such as sliders that send values as command parameters.
     *
     * @param address      arbitrary address string that is used to store the command parameter in
     *                     memory
     * @param command      command name TODO (not used)
     * @param commandParam command parameter value -- stored as a virtual device state value in
     *                     memory and can be later retrieved using this command's {@link #read}
     *                     method.
     */
    public VirtualCommand(String address, String command, String commandParam) {
        this(address, command);
        this.commandParam = commandParam;
    }

    /**
     * Allows this command implementation to be used with components that require write command
     * implementations.
     * <p>
     * Stores the command name (or command parameter, if provided) as a "virtual" device state
     * value when a send() is triggered. The state is stored and indexed by this command's address.
     * <p>
     * If command parameter has been provided (such as slider value) then that is stored as device
     * state instead of command's name.
     */
    @Override
    public void send(String arg) {
        // TODO arg
        if (commandParam == null) {
            virtualDevices.put(address, command);
        } else {
            virtualDevices.put(address, commandParam);
        }
    }

    /**
     * Allows this command implementation to be used as pull commands for sensors.
     * <p>
     * Stored values are looked up using address string as key on read() requests. Device state
     * values can be stored for any given address using {@link #send} method of this class.
     * <p>
     * If the sensor associated with this command is of 'switch' type, the stored value should
     * match 'on' or 'off' ('off' will be returned if the stored value cannot be translated or
     * if no value has been stored for this command's address yet).
     * <p>
     * If the sensor associated with this command is of 'level' type, the stored value should
     * be an integer string within range of [0-100]. String '0' will be returned if the stored
     * device value for this address cannot be translated to integer or if no value has been
     * stored for this command's addres yet.
     * <p>
     * Range behaves the same as 'level' but allows arbitrary integer to be stored.
     * <p>
     * Sensor type 'custom' allows arbitrary string values to be returned from this implementation.
     *
     * @param sensor the sensor
     * @return the value stored for this command's address (in memory) or a default value based
     * on the associated sensor type, if no value has been stored yet in memory
     */
    @Override
    public String read(Sensor sensor) {
        String state = virtualDevices.get(address);

        if (sensor instanceof SwitchSensor) {
            if (state == null) {
                return "off";
            } else if (state.trim().equalsIgnoreCase("on")) {
                return "on";
            } else if (state.trim().equalsIgnoreCase("off")) {
                return "off";
            } else {
                LOG.warning("Was expecting either 'on' or 'off' for SwitchSensor, got: " + state);
                return "off";
            }
        } else if (sensor instanceof LevelSensor) {
            if (state == null) {
                return "0";
            } else {
                try {
                    int value = Integer.parseInt(state.trim());

                    if (value > 100) {
                        return "100";
                    }

                    if (value < 0) {
                        return "0";
                    }

                    return "" + value;
                } catch (NumberFormatException e) {
                    LOG.warning("Can't parse LevelSensor value into a valid number: " + e.getMessage());
                    return "0";
                }
            }
        } else if (sensor instanceof RangeSensor) {
            if (state == null) {
                return "0";
            } else {
                try {
                    int value = Integer.parseInt(state.trim());

                    return "" + value;
                } catch (NumberFormatException e) {
                    LOG.warning("Can't parse RangeSensor value into a valid number: " + e.getMessage());
                    return "0";
                }
            }
        } else if (sensor instanceof CustomStateSensor) {
            return (state == null) ? "" : state;
        } else {
            throw new Error(
                "Unrecognized sensor type '" + sensor + "'. Virtual command implementation must " +
                    "be updated to support this sensor type."
            );
        }
    }

    @Override
    public String toString() {
        return "VirtualCommand{" +
            "address='" + address + '\'' +
            ", command='" + command + '\'' +
            ", commandParam='" + commandParam + '\'' +
            '}';
    }
}

