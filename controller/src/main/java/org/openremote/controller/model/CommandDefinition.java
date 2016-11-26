package org.openremote.controller.model;

import org.openremote.controller.command.Command;
import org.openremote.controller.command.CommandFactory;
import org.openremote.controller.command.ExecutableCommand;
import org.openremote.controller.command.PullCommand;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This is a command model that represents the command properties as an in-memory model for
 * rules, scripts, etc. This is different from the execution model that is currently defined
 * in the org.openremote.controller.command package.
 */
public class CommandDefinition {

    private static final Logger LOG = Logger.getLogger(CommandDefinition.class.getName());

    /**
     * Command property name of dynamic command value for slider, colorpicker.
     * This property is temporary for holding dynamic control command value from REST API.
     * Take slider for example: 
     * REST API: http://localhost:8080/controller/rest/control/{slider_id}/10 
     * <b>10</b> means control command value of slider
     *
     */
    public static final String DYNAMIC_VALUE_PROPERTY = "dynamicValue";

    /**
     * Default value returned by {@link #getName()} if no name has been set in command's properties.
     */
    public final static String DEFAULT_NAME_PROPERTY_VALUE = "<no name>";

    /**
     * Command property name that contains a logical command name that can be used by scripts,
     * rules, REST API calls, etc.
     */
    public final static String COMMAND_NAME_PROPERTY = "name";

    /**
     * Command factory delegates creation of command execution model to specific protocol specific
     * builders (Such as X10, KNX, one-wire, etc.)
     */
    final private CommandFactory commandFactory;

    /**
     * Command's protocol identifier, such as 'knx', 'x10', 'onewire', etc. This identifier is
     * used to determine which protocol builder plugin is used to construct executable commands.
     */
    final private String protocolType;

    /**
     * A unique command identifier. This corresponds to the 'id' attribute in controller XML
     * definition's {@code <command>} element.
     */
    final private int id;

    /**
     * List of generic command properties. The correspond to {@code <property>} elements nested
     * within {@code <command>} element in controller's XML definition. 
     * 
     * An arbitrary list of properties is allowed. Specific property names (such as
     * {@link #COMMAND_NAME_PROPERTY}) may have a special meaning. 
     * 
     * NOTE: property *names* are *not* case sensitive. All property names are converted to lower
     * case.
     */
    final private Map<String, String> properties;

    /**
     * Constructs a command data model.
     *
     * @param commandFactory Command factory is used to construct a protocol specific command execution model
     *                       (for example, KNX specific commands or X10 specific commands).
     * @param id             This represents the unique identifier of the command element in controller's
     *                       XML definition (corresponding to 'id' attribute of {@code <command>} element)
     * @param protocolType   Protocol type identifier for this command. The protocol type identifier is used
     *                       with the command factory to identify which plugin (Java bean implementation) is
     *                       used to construct command's protocol specific execution model.
     * @param properties     Command's properties. Arbitrary list of properties that can be used to configure
     *                       command instances. Certain property values such as {@link #COMMAND_NAME_PROPERTY}
     *                       may have special meaning. The property name,value pairs match the
     *                       {@code <property>} elements nested within {@code <command>} elements within
     *                       controller's XML document model.
     */
    public CommandDefinition(CommandFactory commandFactory, int id, String protocolType, Map<String, String> properties) {
        this.commandFactory = commandFactory;
        this.id = id;
        this.protocolType = protocolType;
        this.properties = properties;
    }

    public CommandDefinition(CommandDefinition original) {
        this(
            original.commandFactory,
            original.id,
            original.protocolType,
            new HashMap<>(original.properties)
        );
    }

    /**
     * Returns the unique identifier of the command that corresponds to 'id' attribute of
     * {@code <command>} element in controller's XML definition.
     *
     * @return unique identifier of the command
     */
    public int getID() {
        return id;
    }

    /**
     * Returns a command's name property if present.
     *
     * @return commands name, or a default {@link #DEFAULT_NAME_PROPERTY_VALUE} string if
     * name property is not present
     */
    public String getName() {
        String name = getProperty(COMMAND_NAME_PROPERTY);
        if (name == null || name.equals("")) {
            return DEFAULT_NAME_PROPERTY_VALUE;
        }
        return name;
    }

    /**
     * Returns the protocol type of the command that corresponds to 'protocol' attribute of
     * {@code <command>} element in controller's XML definition.
     *
     * @return the protocol type
     */
    public String getProtocolType() {
        return protocolType;
    }

    /**
     * Returns a command property value.
     *
     * @param name Name of the command property to return. Note that command property names are
     *             not case-sensitive. All names are converted to lower case characters.
     * @return Command property value, or empty string if not found.
     */
    public String getProperty(String name) {
        String value = properties.get(name.trim().toLowerCase(Locale.ROOT));
        return (value == null) ? "" : value;
    }

    /**
     * Executes a command. This normally means a device protocol is used to communicate with
     * some physical device.
     */
    public void execute() {
        execute(null);    // null == no param
    }

    /**
     * Executes a parameterized command. This normally means a device protocol is used to
     * communicate with some physical device. Command parameterization is typically used
     * with commands that allow setting distinct values, such as setting a volume to a specific
     * distinct level, setting blinds to a specific distinct location, etc.
     *
     * @param param command parameter value
     */
    public void execute(String param) {
        try {
            // Work on a copy of this, so we can modify it for this execution (bad design)
            CommandDefinition commandDefinition = new CommandDefinition(this);
            if (param != null) {
                commandDefinition.properties.put(CommandDefinition.DYNAMIC_VALUE_PROPERTY, param);
            }
            Command command = commandFactory.getCommand(commandDefinition);

            if (command instanceof ExecutableCommand) {
                ExecutableCommand writeCommand = (ExecutableCommand) command;
                writeCommand.send();
            } else if (command instanceof PullCommand) {
                LOG.severe("Execution of read commands not implemented yet.");
            } else {
                LOG.log(Level.SEVERE, "Cannot execute: " + this);
            }
        } catch (Throwable t) {
            LOG.log(Level.SEVERE, "Unable to execute command: " + this, t);
        }
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
            "protocolType='" + protocolType + '\'' +
            ", id=" + id +
            ", properties=" + properties +
            '}';
    }
}

