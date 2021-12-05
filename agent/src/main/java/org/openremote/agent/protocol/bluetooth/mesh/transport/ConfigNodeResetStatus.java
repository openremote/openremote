package org.openremote.agent.protocol.bluetooth.mesh.transport;

import org.openremote.agent.protocol.bluetooth.mesh.opcodes.ConfigMessageOpCodes;

import java.util.logging.Logger;

/**
 * To be used as a wrapper class for when creating the ConfigAppKeyStatus Message.
 */
@SuppressWarnings({"unused", "WeakerAccess"})
public class ConfigNodeResetStatus extends ConfigStatusMessage {

    public static final Logger LOG = Logger.getLogger(ConfigNodeResetStatus.class.getName());
    private static final int OP_CODE = ConfigMessageOpCodes.CONFIG_NODE_RESET_STATUS;

    /**
     * Constructs the ConfigAppKeyStatus mMessage.
     *
     * @param message Access Message
     */
    public ConfigNodeResetStatus(final AccessMessage message) {
        super(message);
        this.mParameters = message.getParameters();
        parseStatusParameters();
    }

    @Override
    final void parseStatusParameters() {
        //This message has empty parameters
    }

    @Override
    public int getOpCode() {
        return OP_CODE;
    }
}

