package org.openremote.agent.command.builtin;

import org.openremote.agent.command.ExecutableCommand;
import org.openremote.agent.command.PullCommand;
import org.openremote.agent.deploy.CommandDefinition;
import org.openremote.agent.sensor.*;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * OpenRemote virtual command implementation.
 * <p>
 * Maintains a virtual-machine-wide state for each address. This can be used for testing.
 * Incoming read() and send() requests operate on a map where the command's {@code <address>}
 * field is used as a key and either the command name, or configured command parameter, or
 * the argument of send() is stored as a virtual device state value for that address.
 */
public class VirtualCommand implements ExecutableCommand, PullCommand {

    private static final Logger LOG = Logger.getLogger(VirtualCommand.class.getName());

    private final static Map<String, String> virtualDevices = new ConcurrentHashMap<>(20);
    private String address = null;
    private String command = null;

    public VirtualCommand(CommandDefinition commandDefinition) {
        this(
            commandDefinition.getProperty("address"),
            commandDefinition.getProperty("command")
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
     * Allows this command implementation to be used with components that require write command
     * implementations.
     * <p>
     * Stores the argument (or command parameter, if no argument is provided) as a "virtual" device state
     * value when a send() is triggered. The state is stored and indexed by this command's address.
     */
    @Override
    public void send(String arg) {
        virtualDevices.put(address, arg != null && arg.length() > 0 ? arg : command);
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
        } else if (sensor instanceof CustomSensor) {
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
            '}';
    }
}

