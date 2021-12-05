package org.openremote.agent.protocol.bluetooth.mesh.transport;

import org.openremote.agent.protocol.bluetooth.mesh.opcodes.ConfigMessageOpCodes;
import org.openremote.agent.protocol.bluetooth.mesh.utils.MeshParserUtils;

/**
 * Creates the ConfigLowPowerNodePollTimeoutStatus message.
 */
public class ConfigLowPowerNodePollTimeoutStatus extends ConfigStatusMessage {

    private static final String TAG = ConfigLowPowerNodePollTimeoutStatus.class.getSimpleName();
    private static final int OP_CODE = ConfigMessageOpCodes.CONFIG_LOW_POWER_NODE_POLLTIMEOUT_STATUS;

    private int address;
    private int pollTimeout;

    /**
     * Constructs ConfigLowPowerNodePollTimeoutStatus message.
     *
     * @param message {@link AccessMessage}
     */
    public ConfigLowPowerNodePollTimeoutStatus(final AccessMessage message) {
        super(message);
        mParameters = message.getParameters();
        parseStatusParameters();
    }

    @Override
    void parseStatusParameters() {
        address = MeshParserUtils.unsignedBytesToInt(mParameters[0], mParameters[1]);
        pollTimeout = MeshParserUtils.convert24BitsToInt(new byte[] {mParameters[2], mParameters[3], mParameters[4]});
    }

    @Override
    public int getOpCode() {
        return OP_CODE;
    }

    /**
     * Returns the Unicast address of the low power node
     */
    public int getAddress() {
        return address;
    }

    /**
     * Returns the Poll Timeout value for Low Power Node.
     */
    public int getPollTimeout() {
        return pollTimeout;
    }
}

