package org.openremote.agent.protocol.bluetooth.mesh.transport;

import org.openremote.agent.protocol.bluetooth.mesh.opcodes.ConfigMessageOpCodes;
import org.openremote.agent.protocol.bluetooth.mesh.utils.MeshParserUtils;

import java.util.logging.Logger;

/**
 * Creates the ConfigFriendStatus message.
 */
public class ConfigFriendStatus extends ConfigStatusMessage {

    public static final Logger LOG = Logger.getLogger(ConfigFriendStatus.class.getName());
    private static final int OP_CODE = ConfigMessageOpCodes.CONFIG_FRIEND_STATUS;

    private boolean enable;

    /**
     * Constructs ConfigFriendStatus message.
     *
     * @param message {@link AccessMessage}
     */
    public ConfigFriendStatus(final AccessMessage message) {
        super(message);
        mParameters = message.getParameters();
        parseStatusParameters();
    }

    @Override
    void parseStatusParameters() {
        enable = MeshParserUtils.unsignedByteToInt(mParameters[0]) == ProvisionedBaseMeshNode.ENABLED;
        LOG.info("Friend status: " + enable);
    }

    @Override
    public int getOpCode() {
        return OP_CODE;
    }

    /**
     * Returns the true if the Friend feature is enabled or not.
     */
    public boolean isEnable() {
        return enable;
    }
}

