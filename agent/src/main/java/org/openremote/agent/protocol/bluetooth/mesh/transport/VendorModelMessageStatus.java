package org.openremote.agent.protocol.bluetooth.mesh.transport;

import org.openremote.agent.protocol.bluetooth.mesh.utils.MeshParserUtils;

import java.util.logging.Logger;

/**
 * To be used as a wrapper class for when creating the VendorModelMessageStatus Message.
 */
public final class VendorModelMessageStatus extends GenericStatusMessage {

    public static final Logger LOG = Logger.getLogger(VendorModelMessageStatus.class.getName());
    private final int mModelIdentifier;

    /**
     * Constructs the VendorModelMessageStatus mMessage.
     *
     * @param message         Access Message
     * @param modelIdentifier model identifier
     */
    public VendorModelMessageStatus(final AccessMessage message, final int modelIdentifier) {
        super(message);
        this.mParameters = message.getParameters();
        this.mModelIdentifier = modelIdentifier;
        parseStatusParameters();
    }

    @Override
    void parseStatusParameters() {
        LOG.info("Received Vendor model status: " + MeshParserUtils.bytesToHex(mParameters, false));
    }

    @Override
    public int getOpCode() {
        return mMessage.getOpCode();
    }

    public final byte[] getAccessPayload() {
        return ((AccessMessage) mMessage).getAccessPdu();
    }

    /**
     * Returns the model identifier of model the for this message
     */
    public int getModelIdentifier() {
        return mModelIdentifier;
    }
}

