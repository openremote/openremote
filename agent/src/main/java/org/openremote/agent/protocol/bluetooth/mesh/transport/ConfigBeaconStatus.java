package org.openremote.agent.protocol.bluetooth.mesh.transport;

import org.openremote.agent.protocol.bluetooth.mesh.opcodes.ConfigMessageOpCodes;
import org.openremote.agent.protocol.bluetooth.mesh.utils.MeshParserUtils;

import java.util.logging.Logger;

/**
 * Creates the ConfigBeaconStatus message.
 */
public class ConfigBeaconStatus extends ConfigStatusMessage {

    public static final Logger LOG = Logger.getLogger(ConfigBeaconStatus.class.getName());
    private static final int OP_CODE = ConfigMessageOpCodes.CONFIG_BEACON_STATUS;

    private boolean enable;

    /**
     * Constructs ConfigBeaconStatus message.
     *
     * @param message {@link AccessMessage}
     */
    public ConfigBeaconStatus(final AccessMessage message) {
        super(message);
        mParameters = message.getParameters();
        parseStatusParameters();
    }

    @Override
    void parseStatusParameters() {
        enable = MeshParserUtils.unsignedByteToInt(mParameters[0]) == ProvisionedBaseMeshNode.ENABLED;
        LOG.info("Secure Network Beacon State: " + enable);
    }

    @Override
    public int getOpCode() {
        return OP_CODE;
    }

    /**
     * Returns the true if the Secure Network beacon State is set to send periodic Secure Network Beacons or false otherwise.
     */
    public boolean isEnable() {
        return enable;
    }
}

