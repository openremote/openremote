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
 * Creates the ConfigModelSubscriptionVirtualAddressOverwrite Message.
 * <p>his message will clear the existing subscription list in a model and add a new one</p>
 */
@SuppressWarnings({"unused"})
public final class ConfigModelSubscriptionVirtualAddressOverwrite extends ConfigMessage {

    public static final Logger LOG = Logger.getLogger(ConfigModelSubscriptionVirtualAddressOverwrite.class.getName());
    private static final int OP_CODE = ConfigMessageOpCodes.CONFIG_MODEL_SUBSCRIPTION_VIRTUAL_ADDRESS_OVERWRITE;

    //Length in bytes
    private static final int CONFIG_MODEL_SUBSCRIPTION_VIRTUAL_ADDRESS_OVERWRITE_LENGTH = 20;
    private static final int VENDOR_MODEL_SUBSCRIPTION_VIRTUAL_ADDRESS_OVERWRITE_LENGTH = CONFIG_MODEL_SUBSCRIPTION_VIRTUAL_ADDRESS_OVERWRITE_LENGTH + 2;

    private final int mElementAddress;
    private final UUID labelUuid;
    private final int mModelIdentifier;

    /**
     * Constructs ConfigModelSubscriptionVirtualAddressOverwrite message.
     *
     * @param elementAddress  Address of the element to which the model belongs to.
     * @param labelUuid       Value of the Label UUID.
     * @param modelIdentifier Model identifier, 16-bit for Sig model and 32-bit model id for vendor models.
     * @throws IllegalArgumentException if any illegal arguments are passed
     */
    public ConfigModelSubscriptionVirtualAddressOverwrite(final int elementAddress,
                                                          /* @NonNull */ final UUID labelUuid,
                                                          final int modelIdentifier) throws IllegalArgumentException {

        if (!MeshAddress.isValidUnicastAddress(elementAddress))
            throw new IllegalArgumentException("Invalid unicast address, unicast address must be a 16-bit value, and must range from 0x0001 to 0x7FFF");
        this.mElementAddress = elementAddress;
        this.labelUuid = labelUuid;
        this.mModelIdentifier = modelIdentifier;
        assembleMessageParameters();
    }

    @Override
    public int getOpCode() {
        return OP_CODE;
    }

    @Override
    void assembleMessageParameters() {

        final ByteBuffer paramsBuffer;
        //We check if the model identifier value is within the range of a 16-bit value here. If it is then it is a sigmodel
        final byte[] elementAddress = MeshAddress.addressIntToBytes(mElementAddress);
        final byte[] subscriptionAddress = MeshParserUtils.uuidToBytes(labelUuid);
        if (mModelIdentifier >= Short.MIN_VALUE && mModelIdentifier <= Short.MAX_VALUE) {
            paramsBuffer = ByteBuffer.allocate(CONFIG_MODEL_SUBSCRIPTION_VIRTUAL_ADDRESS_OVERWRITE_LENGTH).order(ByteOrder.LITTLE_ENDIAN);
            paramsBuffer.put(elementAddress[1]);
            paramsBuffer.put(elementAddress[0]);
            paramsBuffer.put(subscriptionAddress);
            paramsBuffer.putShort((short) mModelIdentifier);
            mParameters = paramsBuffer.array();
        } else {
            paramsBuffer = ByteBuffer.allocate(VENDOR_MODEL_SUBSCRIPTION_VIRTUAL_ADDRESS_OVERWRITE_LENGTH).order(ByteOrder.LITTLE_ENDIAN);
            paramsBuffer.put(elementAddress[1]);
            paramsBuffer.put(elementAddress[0]);
            paramsBuffer.put(subscriptionAddress);
            final byte[] modelIdentifier = new byte[]{(byte) ((mModelIdentifier >> 24) & 0xFF),
                (byte) ((mModelIdentifier >> 16) & 0xFF), (byte) ((mModelIdentifier >> 8) & 0xFF), (byte) (mModelIdentifier & 0xFF)};
            paramsBuffer.put(modelIdentifier[1]);
            paramsBuffer.put(modelIdentifier[0]);
            paramsBuffer.put(modelIdentifier[3]);
            paramsBuffer.put(modelIdentifier[2]);
            mParameters = paramsBuffer.array();
        }
    }

    /**
     * Returns the value of the Label UUID of the subscription address
     */
    public UUID getLabelUuid() {
        return labelUuid;
    }
}

