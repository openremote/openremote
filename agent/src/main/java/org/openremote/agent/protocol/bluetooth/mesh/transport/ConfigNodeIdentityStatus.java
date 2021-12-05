package org.openremote.agent.protocol.bluetooth.mesh.transport;

import org.openremote.agent.protocol.bluetooth.mesh.opcodes.ConfigMessageOpCodes;
import org.openremote.agent.protocol.bluetooth.mesh.utils.MeshParserUtils;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.logging.Logger;

/**
 * Creates the ConfigNodeIdentityStatus message.
 */
public class ConfigNodeIdentityStatus extends ConfigStatusMessage {

    public static final Logger LOG = Logger.getLogger(ConfigNodeIdentityStatus.class.getName());
    private static final int OP_CODE = ConfigMessageOpCodes.CONFIG_BEACON_STATUS;
    private int netKeyIndex;
    /* @NodeIdentityState */
    private int nodeIdentityState;

    /**
     * Constructs ConfigNodeIdentityStatus message.
     *
     * @param message {@link AccessMessage}
     */
    public ConfigNodeIdentityStatus(final AccessMessage message) {
        super(message);
        mParameters = message.getParameters();
        parseStatusParameters();
    }

    @Override
    void parseStatusParameters() {
        mStatusCode = mParameters[0];
        final byte[] netKeyIndex = new byte[]{(byte) (mParameters[2] & 0x0F), mParameters[1]};
        this.netKeyIndex = ByteBuffer.wrap(netKeyIndex).order(ByteOrder.BIG_ENDIAN).getShort();
        nodeIdentityState = MeshParserUtils.unsignedByteToInt(mParameters[3]);
        LOG.info("Status: " + mStatusCode);
        LOG.info("Node Identity State: " + nodeIdentityState);
    }


    @Override
    public int getOpCode() {
        return OP_CODE;
    }

    /**
     * Returns if the message was successful.
     *
     * @return true if the message was successful or false otherwise.
     */
    public final boolean isSuccessful() {
        return mStatusCode == 0x00;
    }

    /**
     * Returns the NodeIdentityState
     */
    /* @NodeIdentityState */
    public int getNodeIdentityState() {
        return nodeIdentityState;
    }

    /**
     * Returns the net key index.
     */
    public int getNetKeyIndex() {
        return netKeyIndex;
    }
}

