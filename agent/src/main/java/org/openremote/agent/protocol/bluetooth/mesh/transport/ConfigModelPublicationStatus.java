/*
 * Copyright 2021, OpenRemote Inc.
 *
 * See the CONTRIBUTORS.txt file in the distribution for a
 * full listing of individual contributors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.openremote.agent.protocol.bluetooth.mesh.transport;

import org.openremote.agent.protocol.bluetooth.mesh.opcodes.ConfigMessageOpCodes;
import org.openremote.agent.protocol.bluetooth.mesh.utils.MeshAddress;
import org.openremote.agent.protocol.bluetooth.mesh.utils.MeshParserUtils;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.logging.Logger;

/**
 * To be used as a wrapper class for when creating the ConfigModelAppStatus Message.
 */
public class ConfigModelPublicationStatus extends ConfigStatusMessage {

    public static final Logger LOG = Logger.getLogger(ConfigModelPublicationStatus.class.getName());

    private static final int OP_CODE = ConfigMessageOpCodes.CONFIG_MODEL_PUBLICATION_STATUS;
    private static final int CONFIG_MODEL_PUBLICATION_STATUS_SIG_MODEL_PDU_LENGTH = 12;
    private static final int CONFIG_MODEL_APP_BIND_STATUS_VENDOR_MODEL_PDU_LENGTH = 14;
    private int mElementAddress;
    private int publishAddress;
    private int mAppKeyIndex;
    private boolean credentialFlag;
    private int publishTtl;
    private int publicationSteps;
    private int publicationResolution;
    private int publishRetransmitCount;
    private int publishRetransmitIntervalSteps;
    private int mModelIdentifier; //16-bit SIG Model or 32-bit Vendor Model identifier

    /**
     * Constructs the ConfigModelAppStatus mMessage.
     *
     * @param message Access Message
     */
    public ConfigModelPublicationStatus(final AccessMessage message) {
        super(message);
        this.mParameters = message.getParameters();
        parseStatusParameters();
    }

    @Override
    final void parseStatusParameters() {
        final AccessMessage message = (AccessMessage) mMessage;
        mStatusCode = mParameters[0];
        mStatusCodeName = getStatusCodeName(mStatusCode);
        mElementAddress = MeshParserUtils.unsignedBytesToInt(mParameters[1], mParameters[2]);
        publishAddress = MeshParserUtils.unsignedBytesToInt(mParameters[3], mParameters[4]);
        final byte[] appKeyIndex = new byte[]{(byte) (mParameters[6] & 0x0F), mParameters[5]};
        mAppKeyIndex = ByteBuffer.wrap(appKeyIndex).order(ByteOrder.BIG_ENDIAN).getShort();
        credentialFlag = (mParameters[6] & 0xF0) >> 4 == 1;
        publishTtl = MeshParserUtils.unsignedByteToInt(mParameters[7]);
        final int publishPeriod = MeshParserUtils.unsignedByteToInt(mParameters[8]);
        publicationSteps = publishPeriod & 0x3F;
        publicationResolution = publishPeriod >> 6;
        final int publishRetransmission = MeshParserUtils.unsignedByteToInt(mParameters[9]);
        publishRetransmitCount = publishRetransmission & 0x07;
        publishRetransmitIntervalSteps = publishRetransmission >> 3;

        final byte[] modelIdentifier;
        if (mParameters.length == CONFIG_MODEL_PUBLICATION_STATUS_SIG_MODEL_PDU_LENGTH) {
            mModelIdentifier = MeshParserUtils.unsignedBytesToInt(mParameters[10], mParameters[11]);
        } else {
            modelIdentifier = new byte[]{mParameters[11], mParameters[10], mParameters[13], mParameters[12]};
            mModelIdentifier = ByteBuffer.wrap(modelIdentifier).order(ByteOrder.BIG_ENDIAN).getInt();
        }

        LOG.info("Status code: " + mStatusCode);
        LOG.info("Status message: " + mStatusCodeName);
        LOG.info("Element address: " + MeshAddress.formatAddress(mElementAddress, false));
        LOG.info("Publish Address: " + MeshAddress.formatAddress(publishAddress, false));
        LOG.info("App key index: " + MeshParserUtils.bytesToHex(appKeyIndex, false));
        LOG.info("Credential Flag: " + credentialFlag);
        LOG.info("Publish TTL: " + publishTtl);
        LOG.info("Publish Period where steps: " + publicationSteps + " and resolution: " + publicationResolution);
        LOG.info("Publish Retransmit Count: " + publishRetransmitCount);
        LOG.info("Publish Retransmit Interval Steps: " + publishRetransmitIntervalSteps);
        LOG.info("Model Identifier: " + Integer.toHexString(mModelIdentifier));
        LOG.info("Publication status: " + MeshParserUtils.bytesToHex(mParameters, false));
    }

    @Override
    public int getOpCode() {
        return OP_CODE;
    }

    /**
     * Returns the element address that the key was bound to
     *
     * @return element address
     */
    public int getElementAddress() {
        return mElementAddress;
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
     * Returns if the message was successful or not.
     *
     * @return true if successful or false otherwise
     */
    public boolean isSuccessful() {
        return mStatusCode == 0x00;
    }

    /**
     * Returns the publish address to which the model must publish to
     */
    public int getPublishAddress() {
        return publishAddress;
    }

    /**
     * Returns the credential flag to be used for this message.
     *
     * @return true if friendship credentials to be used or false if master credentials is to be used.
     */
    public boolean getCredentialFlag() {
        return credentialFlag;
    }

    /**
     * Returns the ttl of publication messages
     *
     * @return publication ttl
     */
    public int getPublishTtl() {
        return publishTtl;
    }

    /**
     * Returns the number of publication steps.
     *
     * @return number of steps
     */
    public int getPublicationSteps() {
        return publicationSteps;
    }

    /**
     * Returns the resolution for the publication steps.
     *
     * @return resolution
     */
    public int getPublicationResolution() {
        return publicationResolution;
    }

    /**
     * Returns the number of retransmissions for each published message.
     *
     * @return number of retransmits
     */
    public int getPublishRetransmitCount() {
        return publishRetransmitCount;
    }

    /**
     * Returns the number of 50-milliseconds steps between retransmissions.
     *
     * @return retransmit interval steps
     */
    public int getPublishRetransmitIntervalSteps() {
        return publishRetransmitIntervalSteps;
    }

    /**
     * Returns the model identifier to which the key is to be bound.
     *
     * @return 16-bit or 32-bit vendor model identifier
     */
    public int getModelIdentifier() {
        return mModelIdentifier;
    }

}
