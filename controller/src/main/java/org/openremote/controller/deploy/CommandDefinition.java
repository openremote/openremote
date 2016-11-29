package org.openremote.controller.deploy;

import java.util.Locale;
import java.util.Map;

/**
 * Command deployment model
 */
public class CommandDefinition {

    public final static String NAME_PROPERTY = "name";
    public final static String NAME_PROPERTY_DEFAULT_VALUE = "<No Name>";
    public final static String DEVICE_NAME_PROPERTY = "urn:openremote:device-command:device-name";
    public final static String DEVICE_ID_PROPERTY = "urn:openremote:device-command:device-id";

    final protected String protocolType;
    final protected int commandID;
    final protected Map<String, String> properties; // Note: properties are not case-sensitive, we convert to lowercase!

    /**
     * @param commandID    This represents the unique identifier of the command element in controller's
     *                     XML definition (corresponding to 'id' attribute of {@code <command>} element)
     * @param protocolType Protocol type identifier for this command. The protocol type identifier is used
     *                     with the command factory to identify which plugin (Java bean implementation) is
     *                     used to construct command's protocol specific execution model.
     * @param properties   Command's properties. Arbitrary list of properties that can be used to configure
     *                     command instances. Certain property values such as {@link #NAME_PROPERTY}
     *                     may have special meaning.
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

    public String getProtocolType() {
        return protocolType;
    }

    public int getCommandID() {
        return commandID;
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

    /**
     * @return commands name, or a default {@link #NAME_PROPERTY_DEFAULT_VALUE} string if name property is not present
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

