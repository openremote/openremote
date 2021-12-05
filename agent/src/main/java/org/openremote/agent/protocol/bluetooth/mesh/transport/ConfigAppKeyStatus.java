package org.openremote.agent.protocol.bluetooth.mesh.transport;

import org.openremote.agent.protocol.bluetooth.mesh.opcodes.ConfigMessageOpCodes;

import java.util.ArrayList;
import java.util.logging.Logger;

/**
 * To be used as a wrapper class for when creating the ConfigAppKeyStatus Message.
 */
public class ConfigAppKeyStatus extends ConfigStatusMessage {

    public static final Logger LOG = Logger.getLogger(ConfigAppKeyStatus.class.getName());
    private static final int OP_CODE = ConfigMessageOpCodes.CONFIG_APPKEY_STATUS;
    private int mNetKeyIndex;
    private int mAppKeyIndex;

    /**
     * Constructs the ConfigAppKeyStatus mMessage.
     *
     * @param message Access Message
     */
    public ConfigAppKeyStatus(final AccessMessage message) {
        super(message);
        this.mParameters = message.getParameters();
        parseStatusParameters();
    }

    @Override
    final void parseStatusParameters() {
        mStatusCode = mParameters[0];
        mStatusCodeName = getStatusCodeName(mStatusCode);

        final ArrayList<Integer> keyIndexes = decode(mParameters.length, 1);
        mNetKeyIndex = keyIndexes.get(0);
        mAppKeyIndex = keyIndexes.get(1);

        LOG.info("Status code: " + mStatusCode);
        LOG.info("Status message: " + mStatusCodeName);
        LOG.info("Net key index: " + Integer.toHexString(mNetKeyIndex));
        LOG.info("App key index: " + Integer.toHexString(mAppKeyIndex));
    }

    @Override
    public final int getOpCode() {
        return OP_CODE;
    }

    /**
     * Returns the global index of the net key.
     *
     * @return netkey index
     */
    public final int getNetKeyIndex() {
        return mNetKeyIndex;
    }

    /**
     * Returns the global app key index.
     *
     * @return appkey index
     */
    public final int getAppKeyIndex() {
        return mAppKeyIndex;
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

