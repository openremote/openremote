package org.openremote.agent.protocol.bluetooth.mesh.transport;

import org.openremote.agent.protocol.bluetooth.mesh.ApplicationKey;
import org.openremote.agent.protocol.bluetooth.mesh.utils.SecureUtils;

import java.util.logging.Logger;

/**
 * To be used as a wrapper class when creating an acknowledged VendorMode message.
 */
public class VendorModelMessageAcked extends GenericMessage {

    public static final Logger LOG = Logger.getLogger(VendorModelMessageAcked.class.getName());
    private static final int VENDOR_MODEL_OPCODE_LENGTH = 4;

    private final int mModelIdentifier;
    private final int mCompanyIdentifier;
    private final int mOpCode;

    /**
     * Constructs VendorModelMessageAcked message.
     *
     * @param appKey            {@link ApplicationKey} for this message
     * @param modelId           32-bit Model identifier
     * @param companyIdentifier 16-bit Company identifier of the vendor model
     * @throws IllegalArgumentException if any illegal arguments are passed
     */
    public VendorModelMessageAcked(final ApplicationKey appKey,
                                   final int modelId,
                                   final int companyIdentifier,
                                   final int opCode,
                                   final byte[] parameters) {
        super(appKey);
        this.mModelIdentifier = modelId;
        this.mCompanyIdentifier = companyIdentifier;
        this.mOpCode = opCode;
        mParameters = parameters;
        assembleMessageParameters();
    }

    @Override
    public int getOpCode() {
        return mOpCode;
    }

    @Override
    void assembleMessageParameters() {
        mAid = SecureUtils.calculateK4(mAppKey.getKey());
    }


    /**
     * Returns the company identifier of the model
     *
     * @return 16-bit company identifier
     */
    public final int getCompanyIdentifier() {
        return mCompanyIdentifier;
    }

    /**
     * Returns the model identifier for this message
     */
    public int getModelIdentifier() {
        return mModelIdentifier;
    }
}
