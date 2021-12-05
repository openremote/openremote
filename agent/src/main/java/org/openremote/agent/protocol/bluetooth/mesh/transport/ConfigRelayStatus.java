package org.openremote.agent.protocol.bluetooth.mesh.transport;

import org.openremote.agent.protocol.bluetooth.mesh.opcodes.ConfigMessageOpCodes;

import java.util.logging.Logger;

/**
 * To be used as a wrapper class for when creating the ConfigRelayStatus message.
 */
@SuppressWarnings({"WeakerAccess"})
public final class ConfigRelayStatus extends ConfigStatusMessage {

    public static final Logger LOG = Logger.getLogger(ConfigRelayStatus.class.getName());
    private static final int OP_CODE = ConfigMessageOpCodes.CONFIG_RELAY_STATUS;
    private int mRelay;
    private int mRelayRetransmitCount;
    private int mRelayRetransmitIntervalSteps;

    /**
     * Constructs a ConfigRelayStatus message.
     *
     * @param message Access message received
     * @throws IllegalArgumentException if any illegal arguments are passed
     */
    public ConfigRelayStatus(final AccessMessage message) {
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
        mRelay = payload[2];
        mRelayRetransmitCount = payload[3] & 0b111;
        mRelayRetransmitIntervalSteps = (payload[3] >> 3) & 0b11111;
        LOG.info("Relay: " + mRelay);
        LOG.info("Retransmit count: " + mRelayRetransmitCount);
        LOG.info("Retransmit Interval steps: " + mRelayRetransmitIntervalSteps);
    }


    /**
     * Returns the RelaySettings.RelayState of the node
     */
    /* @RelaySettings.RelayState */
    public int getRelay() {
        return mRelay;
    }

    /**
     * Returns the Relay retransmit count set in this message
     *
     * @return Relay retransmit count
     */
    public int getRelayRetransmitCount() {
        return mRelayRetransmitCount;
    }

    /**
     * Returns the Relay retransmit interval steps set in this message
     *
     * @return Relay retransmit interval steps
     */
    public int getRelayRetransmitIntervalSteps() {
        return mRelayRetransmitIntervalSteps;
    }
}
