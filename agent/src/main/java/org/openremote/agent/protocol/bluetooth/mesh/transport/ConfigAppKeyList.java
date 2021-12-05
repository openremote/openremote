package org.openremote.agent.protocol.bluetooth.mesh.transport;

import org.openremote.agent.protocol.bluetooth.mesh.opcodes.ConfigMessageOpCodes;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Creates the ConfigAppKeyList Message.
 */
public class ConfigAppKeyList extends ConfigStatusMessage  {

    public static final Logger LOG = Logger.getLogger(ConfigAppKeyList.class.getName());
    private static final int OP_CODE = ConfigMessageOpCodes.CONFIG_APPKEY_LIST;
    private int mNetKeyIndex;
    private final List<Integer> mKeyIndexes;

    /**
     * Constructs the ConfigNetKeyList mMessage.
     *
     * @param message Access Message
     */
    public ConfigAppKeyList(final AccessMessage message) {
        super(message);
        mKeyIndexes = new ArrayList<>();
        this.mParameters = message.getParameters();
        parseStatusParameters();
    }

    @Override
    final void parseStatusParameters() {
        mStatusCode = mParameters[0];
        mStatusCodeName = getStatusCodeName(mStatusCode);
        mNetKeyIndex = decode(3, 1).get(0);
        LOG.info("NetKey Index: " + Integer.toHexString(mNetKeyIndex));
        mKeyIndexes.addAll(decode(mParameters.length, 3));
        for (Integer keyIndex : mKeyIndexes) {
            LOG.info("AppKey Index: " + Integer.toHexString(keyIndex));
        }
    }

    @Override
    public final int getOpCode() {
        return OP_CODE;
    }

    /**
     * Returns if the message was successful
     *
     * @return true if the message was successful or false otherwise
     */
    public final boolean isSuccessful() {
        return mStatusCode == 0x00;
    }

    /**
     * Returns the NetKey index to which the app keys are bound
     */
    public int getNetKeyIndex() {
        return mNetKeyIndex;
    }

    /**
     * Returns the bound app key indexes
     */
    public List<Integer> getKeyIndexes() {
        return mKeyIndexes;
    }
}

