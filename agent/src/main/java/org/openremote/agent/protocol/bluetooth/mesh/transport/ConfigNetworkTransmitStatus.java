package org.openremote.agent.protocol.bluetooth.mesh.transport;

import org.openremote.agent.protocol.bluetooth.mesh.opcodes.ConfigMessageOpCodes;

import java.util.logging.Logger;

/**
 * To be used as a wrapper class for when creating the ConfigNetworkTransmitStatus message.
 */
public final class ConfigNetworkTransmitStatus extends ConfigStatusMessage {

    public static final Logger LOG = Logger.getLogger(ConfigNetworkTransmitStatus.class.getName());
    private static final int OP_CODE = ConfigMessageOpCodes.CONFIG_NETWORK_TRANSMIT_STATUS;
    private int mNetworkTransmitCount;
    private int mNetworkTransmitIntervalSteps;

    /**
     * Constructs a ConfigNetworkTransmitStatus message.
     *
     * @param message Access message received
     * @throws IllegalArgumentException if any illegal arguments are passed
     */
    public ConfigNetworkTransmitStatus(final AccessMessage message) {
        super(message);
        this.mParameters = message.getParameters();
        parseStatusParameters();
    }

    @Override
    public int getOpCode() {
        return OP_CODE;
    }

    @Override
    final void parseStatusParameters() {
        final byte[] payload = ((AccessMessage) mMessage).getAccessPdu();

        mNetworkTransmitCount = payload[2] & 0b111;
        mNetworkTransmitIntervalSteps = (payload[2] >> 3) & 0b11111;
    }

    /**
     * Returns the Network Transmit Count set in this message
     *
     * @return Network Transmit Count
     */
    public int getNetworkTransmitCount() {
        return mNetworkTransmitCount;
    }

    /**
     * Returns the Network Transmit Interval Steps set in this message
     *
     * @return Network Transmit Interval Steps
     */
    public int getNetworkTransmitIntervalSteps() {
        return mNetworkTransmitIntervalSteps;
    }
}
