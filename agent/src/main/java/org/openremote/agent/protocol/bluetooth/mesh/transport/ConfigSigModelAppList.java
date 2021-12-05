package org.openremote.agent.protocol.bluetooth.mesh.transport;

import org.openremote.agent.protocol.bluetooth.mesh.opcodes.ConfigMessageOpCodes;
import org.openremote.agent.protocol.bluetooth.mesh.utils.CompositionDataParser;
import org.openremote.agent.protocol.bluetooth.mesh.utils.MeshAddress;
import org.openremote.agent.protocol.bluetooth.mesh.utils.MeshParserUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Creates the ConfigAppKeyList Message.
 */
@SuppressWarnings({"unused", "WeakerAccess"})
public class ConfigSigModelAppList extends ConfigStatusMessage {

    public static final Logger LOG = Logger.getLogger(ConfigSigModelAppList.class.getName());
    private static final int OP_CODE = ConfigMessageOpCodes.CONFIG_SIG_MODEL_APP_LIST;
    private int mElementAddress;
    private int mModelIdentifier;
    private final List<Integer> mKeyIndexes;

    /**
     * Constructs the ConfigNetKeyList mMessage.
     *
     * @param message Access Message
     */
    public ConfigSigModelAppList(final AccessMessage message) {
        super(message);
        mKeyIndexes = new ArrayList<>();
        this.mParameters = message.getParameters();
        parseStatusParameters();
    }

    @Override
    final void parseStatusParameters() {
        mStatusCode = mParameters[0];
        mStatusCodeName = getStatusCodeName(mStatusCode);
        mElementAddress = MeshParserUtils.unsignedBytesToInt(mParameters[1], mParameters[2]);
        mModelIdentifier = MeshParserUtils.unsignedBytesToInt(mParameters[3], mParameters[4]);

        LOG.info("Status code: " + mStatusCode);
        LOG.info("Status message: " + mStatusCodeName);
        LOG.info("Element address: " + MeshAddress.formatAddress(mElementAddress, false));
        LOG.info("Model identifier: " + CompositionDataParser.formatModelIdentifier(mModelIdentifier, false));

        mKeyIndexes.addAll(decode(mParameters.length, 5));
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
     * Returns the element address
     */
    public int getElementAddress() {
        return mElementAddress;
    }

    /**
     * Returns the model identifier
     */
    public int getModelIdentifier() {
        return mModelIdentifier;
    }

    /**
     * Returns the bound app key indexes
     */
    public List<Integer> getKeyIndexes() {
        return mKeyIndexes;
    }
}

