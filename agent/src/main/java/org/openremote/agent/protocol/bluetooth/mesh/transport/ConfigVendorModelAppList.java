package org.openremote.agent.protocol.bluetooth.mesh.transport;

import org.openremote.agent.protocol.bluetooth.mesh.opcodes.ConfigMessageOpCodes;
import org.openremote.agent.protocol.bluetooth.mesh.utils.CompositionDataParser;
import org.openremote.agent.protocol.bluetooth.mesh.utils.MeshAddress;
import org.openremote.agent.protocol.bluetooth.mesh.utils.MeshParserUtils;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Creates the ConfigAppKeyList Message.
 */
@SuppressWarnings({"unused", "WeakerAccess"})
public class ConfigVendorModelAppList extends ConfigStatusMessage {

    public static final Logger LOG = Logger.getLogger(ConfigVendorModelAppList.class.getName());
    private static final int OP_CODE = ConfigMessageOpCodes.CONFIG_VENDOR_MODEL_APP_LIST;
    private int mElementAddress;
    private int mModelIdentifier;
    private final List<Integer> mKeyIndexes;

    /**
     * Constructs the ConfigNetKeyList mMessage.
     *
     * @param message Access Message
     */
    public ConfigVendorModelAppList(final AccessMessage message) {
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
        final byte[] modelIdentifier = new byte[]{mParameters[4], mParameters[3], mParameters[6], mParameters[5]};
        mModelIdentifier = ByteBuffer.wrap(modelIdentifier).order(ByteOrder.BIG_ENDIAN).getInt();

        LOG.info("Status code: " + mStatusCode);
        LOG.info("Status message: " + mStatusCodeName);
        LOG.info("Element address: " + MeshAddress.formatAddress(mElementAddress, false));
        LOG.info("Model identifier: " + CompositionDataParser.formatModelIdentifier(mModelIdentifier, false));

        mKeyIndexes.addAll(decode(mParameters.length, 7));
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

