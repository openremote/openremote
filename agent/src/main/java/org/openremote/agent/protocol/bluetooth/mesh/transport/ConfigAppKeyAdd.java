package org.openremote.agent.protocol.bluetooth.mesh.transport;

import org.openremote.agent.protocol.bluetooth.mesh.ApplicationKey;
import org.openremote.agent.protocol.bluetooth.mesh.NetworkKey;
import org.openremote.agent.protocol.bluetooth.mesh.opcodes.ConfigMessageOpCodes;
import org.openremote.agent.protocol.bluetooth.mesh.utils.MeshParserUtils;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.logging.Logger;

/**
 * To be used as a wrapper class to create the ConfigAppKeyAdd message.
 */
public class ConfigAppKeyAdd extends ConfigMessage {

    public static final Logger LOG = Logger.getLogger(ConfigAppKeyAdd.class.getName());
    private static final int OP_CODE = ConfigMessageOpCodes.CONFIG_APPKEY_ADD;

    private final NetworkKey mNetKey;
    private final ApplicationKey mAppKey;

    /**
     * Constructs ConfigAppKeyAdd message.
     *
     * @param appKey application key for this message
     * @throws IllegalArgumentException if any illegal arguments are passed
     */
    public ConfigAppKeyAdd(final NetworkKey networkKey, final ApplicationKey appKey) throws IllegalArgumentException {
        if (networkKey.getKey().length != 16)
            throw new IllegalArgumentException("Network key must be 16 bytes");

        if (appKey.getKey().length != 16)
            throw new IllegalArgumentException("App key must be 16 bytes");

        this.mNetKey = networkKey;
        this.mAppKey = appKey;
        assembleMessageParameters();
    }

    /**
     * Returns the Network key that is needs to be sent to the node
     *
     * @return app key
     */
    public NetworkKey getNetKey() {
        return mNetKey;
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
        LOG.info("NetKeyIndex: " + mNetKey.getKeyIndex());
        LOG.info("AppKeyIndex: " + mAppKey.getKeyIndex());
        final byte[] netKeyIndex = MeshParserUtils.addKeyIndexPadding(mNetKey.getKeyIndex());
        final byte[] appKeyIndex = MeshParserUtils.addKeyIndexPadding(mAppKey.getKeyIndex());
        final ByteBuffer paramsBuffer = ByteBuffer.allocate(19).order(ByteOrder.LITTLE_ENDIAN);
        paramsBuffer.put(netKeyIndex[1]);
        paramsBuffer.put((byte) (((appKeyIndex[1] & 0xFF) << 4)  | (netKeyIndex[0] & 0xFF) & 0x0F));
        paramsBuffer.put((byte) (((appKeyIndex[0] & 0xFF) << 4) | (appKeyIndex[1] & 0xFF) >> 4));
        paramsBuffer.put(mAppKey.getKey());

        mParameters = paramsBuffer.array();
    }
}

