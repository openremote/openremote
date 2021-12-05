package org.openremote.agent.protocol.bluetooth.mesh.transport;

import org.openremote.agent.protocol.bluetooth.mesh.ApplicationKey;
import org.openremote.agent.protocol.bluetooth.mesh.opcodes.ConfigMessageOpCodes;
import org.openremote.agent.protocol.bluetooth.mesh.utils.MeshParserUtils;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.logging.Logger;

/**
 * Creates the ConfigAppKeyUpdate message.
 */
public class ConfigAppKeyUpdate extends ConfigMessage {

    public static final Logger LOG = Logger.getLogger(ConfigAppKeyUpdate.class.getName());
    private static final int OP_CODE = ConfigMessageOpCodes.CONFIG_APPKEY_UPDATE;

    private final ApplicationKey mAppKey;

    /**
     * Constructs ConfigAppKeyUpdate message.
     *
     * @param appKey application key for this message
     * @throws IllegalArgumentException if any illegal arguments are passed
     */
    public ConfigAppKeyUpdate(final ApplicationKey appKey) throws IllegalArgumentException {
        if (appKey.getKey().length != 16)
            throw new IllegalArgumentException("App key must be 16 bytes");

        this.mAppKey = appKey;
        assembleMessageParameters();
    }

    /**
     * Returns the application key that is needs to be sent to the node
     *
     * @return app key
     */
    public ApplicationKey getAppKey() {
        return mAppKey;
    }

    @Override
    public int getOpCode() {
        return OP_CODE;
    }


    @Override
    void assembleMessageParameters() {
        LOG.info("NetKeyIndex: " + mAppKey.getBoundNetKeyIndex());
        LOG.info("AppKeyIndex: " + mAppKey.getKeyIndex());
        final byte[] netKeyIndex = MeshParserUtils.addKeyIndexPadding(mAppKey.getBoundNetKeyIndex());
        final byte[] appKeyIndex = MeshParserUtils.addKeyIndexPadding(mAppKey.getKeyIndex());
        final ByteBuffer paramsBuffer = ByteBuffer.allocate(19).order(ByteOrder.LITTLE_ENDIAN);
        paramsBuffer.put(netKeyIndex[1]);
        paramsBuffer.put((byte) (((appKeyIndex[1] & 0xFF) << 4) | (netKeyIndex[0] & 0xFF) & 0x0F));
        paramsBuffer.put((byte) (((appKeyIndex[0] & 0xFF) << 4) | (appKeyIndex[1] & 0xFF) >> 4));
        paramsBuffer.put(mAppKey.getKey());

        mParameters = paramsBuffer.array();
    }
}
