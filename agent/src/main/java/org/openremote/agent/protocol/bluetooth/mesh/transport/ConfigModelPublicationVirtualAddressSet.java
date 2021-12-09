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
import java.util.UUID;
import java.util.logging.Logger;

/**
 * This is the message class for setting a virtual address as a publication address
 */
public class ConfigModelPublicationVirtualAddressSet extends ConfigMessage {

    public static final Logger LOG = Logger.getLogger(ConfigModelPublicationVirtualAddressSet.class.getName());
    private static final int OP_CODE = ConfigMessageOpCodes.CONFIG_MODEL_PUBLICATION_VIRTUAL_ADDRESS_SET;

    private static final int SIG_MODEL_PUBLISH_SET_PARAMS_LENGTH = 25;
    private static final int VENDOR_MODEL_PUBLISH_SET_PARAMS_LENGTH = 27;

    private final int elementAddress;
    private final UUID labelUuid;
    private final int appKeyIndex;
    private final boolean credentialFlag;
    private final int publishTtl;
    private final int publicationSteps;
    private final int publicationResolution;
    private final int publishRetransmitCount;
    private final int publishRetransmitIntervalSteps;
    private final int modelIdentifier;

    /**
     * Constructs a ConfigModelPublicationVirtualAddressSet message
     *
     * @param elementAddress          Element address that should publish
     * @param labelUuid               Value of the Label UUID publish address
     * @param appKeyIndex             Index of the application key
     * @param credentialFlag          Credentials flag define which credentials to be used, set true to use friendship credentials and false
     *                                for master credentials. Currently supports only master credentials
     * @param publishTtl              Publication ttl
     * @param publicationSteps        Publication steps for the publication period
     * @param publicationResolution   Publication resolution of the publication period
     * @param retransmitCount         Number of publication retransmits
     * @param retransmitIntervalSteps Publish retransmit interval steps
     * @param modelIdentifier         identifier for this model that will do publication
     * @throws IllegalArgumentException for invalid arguments
     */
    public ConfigModelPublicationVirtualAddressSet(final int elementAddress,
                                                   /* @NonNull */ final UUID labelUuid,
                                                   final int appKeyIndex,
                                                   final boolean credentialFlag,
                                                   final int publishTtl,
                                                   final int publicationSteps,
                                                   final int publicationResolution,
                                                   final int retransmitCount,
                                                   final int retransmitIntervalSteps,
                                                   final int modelIdentifier) throws IllegalArgumentException {
        if (!MeshAddress.isValidUnicastAddress(elementAddress))
            throw new IllegalArgumentException("Invalid unicast address, unicast address must be a 16-bit value, and must range from 0x0001 to 0x7FFF");
        this.elementAddress = elementAddress;
        this.labelUuid = labelUuid;
        this.credentialFlag = credentialFlag;
        this.publishTtl = publishTtl;
        this.publicationSteps = publicationSteps;
        this.publicationResolution = publicationResolution;
        this.publishRetransmitCount = retransmitCount;
        this.publishRetransmitIntervalSteps = retransmitIntervalSteps;
        this.modelIdentifier = modelIdentifier;
        this.appKeyIndex = appKeyIndex;
        assembleMessageParameters();
    }

    @Override
    public int getOpCode() {
        return OP_CODE;
    }


    @Override
    void assembleMessageParameters() {
        final ByteBuffer paramsBuffer;
        final byte[] applicationKeyIndex = MeshParserUtils.addKeyIndexPadding(appKeyIndex);
        LOG.info("AppKeyIndex: " + appKeyIndex);
        LOG.info("Element address: " + MeshAddress.formatAddress(elementAddress, true));
        LOG.info("Label UUID: " + labelUuid.toString());
        LOG.info("Publish ttl: " + publishTtl);
        LOG.info("Publish steps: " + publicationSteps);
        LOG.info("Publish resolution: " + publicationResolution);
        LOG.info("Retransmission count: " + publishRetransmitCount);
        LOG.info("Retransmission interval: " + publishRetransmitIntervalSteps);
        LOG.info("Model: " + MeshParserUtils.bytesToHex(MeshAddress.addressIntToBytes(modelIdentifier), false));
        final byte[] publishAddress = MeshParserUtils.uuidToBytes(labelUuid);
        final int rfu = 0; // We ignore the rfu here
        final int octet5 = applicationKeyIndex[0] | ((credentialFlag ? 0b01 : 0b00) << 4);
        final byte publishPeriod = (byte) ((publicationResolution << 6) | (publicationSteps & 0x3F));
        final int octet8 = (publishRetransmitIntervalSteps << 3) | (publishRetransmitCount & 0x07);
        //We check if the model identifier value is within the range of a 16-bit value here. If it is then it is a sig model
        if (modelIdentifier >= Short.MIN_VALUE && modelIdentifier <= Short.MAX_VALUE) {
            paramsBuffer = ByteBuffer.allocate(SIG_MODEL_PUBLISH_SET_PARAMS_LENGTH).order(ByteOrder.LITTLE_ENDIAN);
            paramsBuffer.putShort((short) elementAddress);
            paramsBuffer.put(publishAddress);
            paramsBuffer.put(applicationKeyIndex[1]);
            paramsBuffer.put((byte) octet5);
            paramsBuffer.put((byte) publishTtl);
            paramsBuffer.put(publishPeriod);
            paramsBuffer.put((byte) octet8);
            paramsBuffer.putShort((short) modelIdentifier);
            mParameters = paramsBuffer.array();
        } else {
            paramsBuffer = ByteBuffer.allocate(VENDOR_MODEL_PUBLISH_SET_PARAMS_LENGTH).order(ByteOrder.LITTLE_ENDIAN);
            paramsBuffer.putShort((short) elementAddress);
            paramsBuffer.put(publishAddress);
            paramsBuffer.put(applicationKeyIndex[1]);
            paramsBuffer.put((byte) octet5);
            paramsBuffer.put((byte) publishTtl);
            paramsBuffer.put(publishPeriod);
            paramsBuffer.put((byte) octet8);
            final byte[] modelIdentifier = new byte[]{(byte) ((this.modelIdentifier >> 24) & 0xFF),
                (byte) ((this.modelIdentifier >> 16) & 0xFF), (byte) ((this.modelIdentifier >> 8) & 0xFF), (byte) (this.modelIdentifier & 0xFF)};
            paramsBuffer.put(modelIdentifier[1]);
            paramsBuffer.put(modelIdentifier[0]);
            paramsBuffer.put(modelIdentifier[3]);
            paramsBuffer.put(modelIdentifier[2]);
            mParameters = paramsBuffer.array();
        }
        LOG.info("Publication set: " + MeshParserUtils.bytesToHex(mParameters, false));
    }

    /**
     * Returns the element address to which the app key must be bound.
     *
     * @return element address
     */
    public int getElementAddress() {
        return elementAddress;
    }

    /**
     * Returns the value of the Label UUID publish address
     */
    public UUID getLabelUuid() {
        return labelUuid;
    }

    /**
     * Returns the global index of the app key to be used for publication.
     *
     * @return app key index
     */
    public int getAppKeyIndex() {
        return appKeyIndex;
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
        return modelIdentifier;
    }
}

