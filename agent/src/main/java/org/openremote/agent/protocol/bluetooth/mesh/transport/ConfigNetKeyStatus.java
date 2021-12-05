package org.openremote.agent.protocol.bluetooth.mesh.transport;

import org.openremote.agent.protocol.bluetooth.mesh.opcodes.ConfigMessageOpCodes;

import java.util.ArrayList;
import java.util.logging.Logger;

/**
 * Creates the ConfigNetKeyStatus Message.
 */
public class ConfigNetKeyStatus extends ConfigStatusMessage {

    public static final Logger LOG = Logger.getLogger(ConfigNetKeyStatus.class.getName());

    private static final int OP_CODE = ConfigMessageOpCodes.CONFIG_NETKEY_STATUS;
    private int mNetKeyIndex;

    /**
     * Constructs the ConfigNetKeyStatus mMessage.
     *
     * @param message Access Message
     */
    public ConfigNetKeyStatus(final AccessMessage message) {
        super(message);
        this.mParameters = message.getParameters();
        parseStatusParameters();
    }

    @Override
    final void parseStatusParameters() {
        mStatusCode = mParameters[0];
        mStatusCodeName = getStatusCodeName(mStatusCode);
        final ArrayList<Integer> keyIndexes = decode(mParameters.length, 1);
        //NetKey status will only contain one index so we just take the first element
        mNetKeyIndex = keyIndexes.get(0);//ByteBuffer.wrap(netKeyIndex).order(ByteOrder.BIG_ENDIAN).getShort();

        LOG.info("Status code: " + mStatusCode);
        LOG.info("Status message: " + mStatusCodeName);
        LOG.info("Net key index: " + Integer.toHexString(mNetKeyIndex));
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
     * Returns if the message was successful
     *
     * @return true if the message was successful or false otherwise
     */
    public final boolean isSuccessful() {
        return mStatusCode == 0x00;
    }
}

