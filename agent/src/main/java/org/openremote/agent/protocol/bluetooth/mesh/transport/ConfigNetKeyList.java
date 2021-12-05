package org.openremote.agent.protocol.bluetooth.mesh.transport;

import org.openremote.agent.protocol.bluetooth.mesh.opcodes.ConfigMessageOpCodes;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Creates the ConfigNetKeyList Message.
 */
public class ConfigNetKeyList extends ConfigStatusMessage {

    public static final Logger LOG = Logger.getLogger(ConfigNetKeyList.class.getName());
    private static final int OP_CODE = ConfigMessageOpCodes.CONFIG_NETKEY_LIST;
    private final List<Integer> mKeyIndexes;

    /**
     * Constructs the ConfigNetKeyList mMessage.
     *
     * @param message Access Message
     */
    public ConfigNetKeyList(final AccessMessage message) {
        super(message);
        mKeyIndexes = new ArrayList<>();
        this.mParameters = message.getParameters();
        parseStatusParameters();
    }

    @Override
    final void parseStatusParameters() {
        mKeyIndexes.addAll(decode(mParameters.length, 0));
        for (Integer keyIndex : mKeyIndexes) {
            LOG.info("Key Index: " + Integer.toHexString(keyIndex));
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

    public List<Integer> getKeyIndexes() {
        return mKeyIndexes;
    }
}

