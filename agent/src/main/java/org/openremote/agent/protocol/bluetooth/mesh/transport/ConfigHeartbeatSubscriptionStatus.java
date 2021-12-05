package org.openremote.agent.protocol.bluetooth.mesh.transport;

import org.openremote.agent.protocol.bluetooth.mesh.opcodes.ConfigMessageOpCodes;
import org.openremote.agent.protocol.bluetooth.mesh.utils.HeartbeatSubscription;
import org.openremote.agent.protocol.bluetooth.mesh.utils.MeshParserUtils;

import java.util.logging.Logger;

/**
 * ConfigHeartbeatPublicationStatus message.
 */
@SuppressWarnings({"WeakerAccess"})
public class ConfigHeartbeatSubscriptionStatus extends ConfigStatusMessage {

    public static final Logger LOG = Logger.getLogger(ConfigHeartbeatSubscriptionStatus.class.getName());
    private static final int OP_CODE = ConfigMessageOpCodes.CONFIG_HEARTBEAT_SUBSCRIPTION_STATUS;
    private HeartbeatSubscription heartbeatSubscription;

    /**
     * Constructs ConfigHeartbeatSubscriptionStatus message.
     *
     * @param message Message
     * @throws IllegalArgumentException if any illegal arguments are passed
     */
    public ConfigHeartbeatSubscriptionStatus(final AccessMessage message) {
        super(message);
        this.mParameters = message.getParameters();
        parseStatusParameters();
    }

    @Override
    public int getOpCode() {
        return OP_CODE;
    }

    @Override
    void parseStatusParameters() {
        mStatusCode = mParameters[0];
        mStatusCodeName = getStatusCodeName(mStatusCode);
        final int srcAddress = MeshParserUtils.unsignedBytesToInt(mParameters[1], mParameters[2]);
        final int dstAddress = MeshParserUtils.unsignedBytesToInt(mParameters[3], mParameters[4]);
        final int periodLog = MeshParserUtils.unsignedByteToInt(mParameters[5]);
        final int countLog = MeshParserUtils.unsignedByteToInt(mParameters[6]);
        final int minHops = MeshParserUtils.unsignedByteToInt(mParameters[7]);
        final int maxHops = MeshParserUtils.unsignedByteToInt(mParameters[8]);

        heartbeatSubscription = new HeartbeatSubscription(srcAddress, dstAddress, (byte)periodLog, (byte)countLog, minHops, maxHops);
        LOG.info("Status code: " + mStatusCode);
        LOG.info("Status message: " + mStatusCodeName);
        LOG.info("Heartbeat subscription: " + heartbeatSubscription.toString());
    }

    /**
     * Returns the Heartbeat subscription.
     */
    public HeartbeatSubscription getHeartbeatSubscription() {
        return heartbeatSubscription;
    }

    /**
     * Returns if the message was successful
     *
     * @return true if the message was successful or false otherwise
     */
    public final boolean isSuccessful() {
        return mStatusCode == 0x00;
    }
}

