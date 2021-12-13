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

public class ConfigModelAppStatus extends ConfigStatusMessage {

    public static final Logger LOG = Logger.getLogger(ConfigModelAppStatus.class.getName());

    private static final String TAG = ConfigModelAppStatus.class.getSimpleName();
    private static final int OP_CODE = ConfigMessageOpCodes.CONFIG_MODEL_APP_STATUS;
    private static final int CONFIG_MODEL_APP_BIND_STATUS_SIG_MODEL = 7;
    private static final int CONFIG_MODEL_APP_BIND_STATUS_VENDOR_MODEL = 9;
    private int mElementAddress;
    private int mAppKeyIndex;
    private int mModelIdentifier;

    /**
     * Constructs the ConfigModelAppStatus mMessage.
     *
     * @param message Access Message
     */
    public ConfigModelAppStatus(final AccessMessage message) {
        super(message);
        this.mParameters = message.getParameters();
        parseStatusParameters();
    }

    @Override
    final void parseStatusParameters() {
        final AccessMessage message = (AccessMessage) mMessage;
        final ByteBuffer buffer = ByteBuffer.wrap(message.getParameters()).order(ByteOrder.LITTLE_ENDIAN);
        buffer.position(0);
        mStatusCode = buffer.get();
        mStatusCodeName = getStatusCodeName(mStatusCode);
        mElementAddress = MeshParserUtils.unsignedBytesToInt(mParameters[1], mParameters[2]);
        final byte[] appKeyIndex = new byte[]{(byte) (mParameters[4] & 0x0F), mParameters[3]};
        mAppKeyIndex = ByteBuffer.wrap(appKeyIndex).order(ByteOrder.BIG_ENDIAN).getShort();

        final byte[] modelIdentifier;
        if (mParameters.length == CONFIG_MODEL_APP_BIND_STATUS_SIG_MODEL) {
            mModelIdentifier = MeshParserUtils.unsignedBytesToInt(mParameters[5], mParameters[6]);
        } else {
            modelIdentifier = new byte[]{mParameters[6], mParameters[5], mParameters[8], mParameters[7]};
            mModelIdentifier = ByteBuffer.wrap(modelIdentifier).order(ByteOrder.BIG_ENDIAN).getInt();
        }

        LOG.info("Status code: " + mStatusCode);
        LOG.info("Status message: " + mStatusCodeName);
        LOG.info("Element address: " + MeshAddress.formatAddress(mElementAddress, false));
        LOG.info("App key index: " + MeshParserUtils.bytesToHex(appKeyIndex, false));
        LOG.info("Model identifier: " + Integer.toHexString(mModelIdentifier));
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
     * Returns the model identifier
     *
     * @return 16-bit sig model identifier or 32-bit vendor model identifier
     */
    public final int getModelIdentifier() {
        return mModelIdentifier;
    }

    /**
     * Returns if the message was successful or not.
     *
     * @return true if succesful or false otherwise
     */
    public boolean isSuccessful() {
        return mStatusCode == 0x00;
    }
}

