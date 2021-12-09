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

import org.openremote.agent.protocol.bluetooth.mesh.Features;
import org.openremote.agent.protocol.bluetooth.mesh.opcodes.ConfigMessageOpCodes;
import org.openremote.agent.protocol.bluetooth.mesh.utils.DeviceFeatureUtils;
import org.openremote.agent.protocol.bluetooth.mesh.utils.HeartbeatPublication;
import org.openremote.agent.protocol.bluetooth.mesh.utils.MeshParserUtils;

import java.util.logging.Logger;

/**
 * ConfigHeartbeatPublicationStatus message.
 */
public class ConfigHeartbeatPublicationStatus extends ConfigStatusMessage {

    public static final Logger LOG = Logger.getLogger(ConfigHeartbeatPublicationStatus.class.getName());

    private static final int OP_CODE = ConfigMessageOpCodes.CONFIG_HEARTBEAT_PUBLICATION_STATUS;
    private HeartbeatPublication heartbeatPublication;

    /**
     * Constructs ConfigHeartbeatPublicationStatus message.
     *
     * @param message Message
     * @throws IllegalArgumentException if any illegal arguments are passed
     */
    public ConfigHeartbeatPublicationStatus(final AccessMessage message) {
        super(message);
        this.mParameters = message.getParameters();
        parseStatusParameters();
    }

    @Override
    public int getOpCode() {
        return OP_CODE;
    }

    @Override
    void parseStatusParameters() {
        mStatusCode = mParameters[0];
        mStatusCodeName = getStatusCodeName(mStatusCode);
        final int dst = MeshParserUtils.unsignedBytesToInt(mParameters[1], mParameters[2]);
        final int countLog = MeshParserUtils.unsignedByteToInt(mParameters[3]);
        final int periodLog = MeshParserUtils.unsignedByteToInt(mParameters[4]);
        final int ttl = MeshParserUtils.unsignedByteToInt(mParameters[5]);

        final int featuresInt = MeshParserUtils.unsignedBytesToInt(mParameters[6], mParameters[7]);
        final Features features = new Features(DeviceFeatureUtils.getFriendFeature(featuresInt),
            DeviceFeatureUtils.getLowPowerFeature(featuresInt),
            DeviceFeatureUtils.getProxyFeature(featuresInt),
            DeviceFeatureUtils.getRelayFeature(featuresInt));
        final int netKeyIndex = MeshParserUtils.unsignedBytesToInt((mParameters[8]), mParameters[9]);
        heartbeatPublication = new HeartbeatPublication(dst, (byte) countLog, (byte) periodLog, ttl, features, netKeyIndex);
        LOG.info("Status code: " + mStatusCode);
        LOG.info("Status message: " + mStatusCodeName);
        LOG.info("Heartbeat publication: " + heartbeatPublication.toString());
    }

    /**
     * Returns if the message was successful
     *
     * @return true if the message was successful or false otherwise
     */
    public final boolean isSuccessful() {
        return mStatusCode == 0x00;
    }

    public HeartbeatPublication getHeartbeatPublication() {
        return heartbeatPublication;
    }
}

