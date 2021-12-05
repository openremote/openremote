package org.openremote.agent.protocol.bluetooth.mesh.transport;

import org.openremote.agent.protocol.bluetooth.mesh.NetworkKey;
import org.openremote.agent.protocol.bluetooth.mesh.opcodes.ConfigMessageOpCodes;
import org.openremote.agent.protocol.bluetooth.mesh.utils.MeshParserUtils;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.logging.Logger;

public class ConfigNetKeyDelete extends ConfigMessage {

    public static final Logger LOG = Logger.getLogger(ConfigNetKeyDelete.class.getName());
    private static final int OP_CODE = ConfigMessageOpCodes.CONFIG_NETKEY_DELETE;

    private final NetworkKey mNetKey;

    /**
     * Constructs ConfigNetKeyDelete message.
     *
     * @param networkKey Network key for this message
     * @throws IllegalArgumentException if any illegal arguments are passed
     */
    public ConfigNetKeyDelete(final NetworkKey networkKey) throws IllegalArgumentException {
        if (networkKey.getKey().length != 16)
            throw new IllegalArgumentException("Network key must be 16 bytes");

        this.mNetKey = networkKey;
        assembleMessageParameters();
    }

    @Override
    public int getOpCode() {
        return OP_CODE;
    }


    @Override
    void assembleMessageParameters() {
        LOG.info("NetKeyIndex: " + mNetKey.getKeyIndex());
        final byte[] netKeyIndex = MeshParserUtils.addKeyIndexPadding(mNetKey.getKeyIndex());

        final ByteBuffer paramsBuffer = ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN);
        paramsBuffer.put(netKeyIndex[1]);
        paramsBuffer.put((byte) ((netKeyIndex[0] & 0xFF) & 0x0F));
        mParameters = paramsBuffer.array();
    }
}
