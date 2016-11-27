package org.openremote.controller.deploy;

import java.util.Locale;
import java.util.Map;

/**
 * Command deployment model
 */
public class CommandDefinition {

    /**
     * Command property name that contains a logical command name that can be used by scripts,
     * rules, REST API calls, etc.
     */
    public final static String NAME_PROPERTY = "name";

    /**
     * Default value returned by {@link #getName()} if no name has been set in command's properties.
     */
    public final static String NAME_PROPERTY_DEFAULT_VALUE = "<No Name>";

    /**
     * Command property that contains the name of the related device.
     */
    public final static String DEVICE_NAME_PROPERTY = "urn:openremote:device-command:device-name";

    /**
     * Command property that contains the ID of the related device.
     */
    public final static String DEVICE_ID_PROPERTY = "urn:openremote:device-command:device-id";

    /**
     * Command's protocol identifier, such as 'knx', 'x10', 'onewire', etc. This identifier is
     * used to determine which protocol builder plugin is used to construct executable commands.
     */
    final protected String protocolType;

    /**
     * A unique command identifier. This corresponds to the 'id' attribute in controller XML
     * definition's {@code <command>} element.
     */
    final protected int commandID;

    /**
     * List of generic command properties. The correspond to {@code <property>} elements nested
     * within {@code <command>} element in controller's XML definition.
     * <p>
     * An arbitrary list of properties is allowed. Specific property names (such as
     * {@link #NAME_PROPERTY}) may have a special meaning.
     * <p>
     * NOTE: property *names* are *not* case sensitive. All property names are converted to lower
     * case.
     */
    final protected Map<String, String> properties;

    /**
     * Constructs a command data model.
     *
     * @param commandID                       This represents the unique identifier of the command element in controller's
     *                                 XML definition (corresponding to 'id' attribute of {@code <command>} element)
     * @param protocolType             Protocol type identifier for this command. The protocol type identifier is used
     *                                 with the command factory to identify which plugin (Java bean implementation) is
     *                                 used to construct command's protocol specific execution model.
     * @param properties               Command's properties. Arbitrary list of properties that can be used to configure
     *                                 command instances. Certain property values such as {@link #NAME_PROPERTY}
     *                                 may have special meaning.
     */
    public CommandDefinition(int commandID, String protocolType, Map<String, String> properties) {
        if (protocolType == null)
            throw new IllegalArgumentException("Command protocol type can't be null");
        this.commandID = commandID;
        this.protocolType = protocolType;
        if (properties == null)
            throw new IllegalArgumentException("Command definition properties are required: " + this);
        this.properties = properties;
        if (getDeviceID() == null)
            throw new IllegalStateException("Command definition must have device identifier: " + this);
        if (getDeviceName() == null)
            throw new IllegalStateException("Command definition must have device name: " + this);
    }

    /**
     * Returns the unique identifier of the command.
     */
    public int getCommandID() {
        return commandID;
    }

    /**
     * Returns a command's name property if present.
     *
     * @return commands name, or a default {@link #NAME_PROPERTY_DEFAULT_VALUE} string if
     * name property is not present
     */
    public String getName() {
        String name = getProperty(NAME_PROPERTY);
        if (name.length() == 0) {
            return NAME_PROPERTY_DEFAULT_VALUE;
        }
        return name;
    }

    public String getDeviceName() {
        return getProperty(CommandDefinition.DEVICE_NAME_PROPERTY).trim();
    }

    public Integer getDeviceID() {
        try {
            return Integer.parseInt(getProperty(CommandDefinition.DEVICE_ID_PROPERTY));
        } catch (NumberFormatException ex) {
            return null;
        }
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
        return getProperty(name, "");
    }

    public String getProperty(String name, String defaultValue) {
        String value = properties.get(name.trim().toLowerCase(Locale.ROOT));
        return (value == null) ? defaultValue : value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CommandDefinition that = (CommandDefinition) o;
        return commandID == that.commandID;
    }

    @Override
    public int hashCode() {
        return commandID;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
            "protocolType='" + protocolType + '\'' +
            ", ID=" + getCommandID() +
            ", name=" + getName() +
            ", properties=" + properties +
            '}';
    }
}

