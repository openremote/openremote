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
import org.openremote.agent.protocol.bluetooth.mesh.utils.MeshParserUtils;

import java.util.logging.Logger;

/**
 * Creates the ConfigBeaconStatus message.
 */
public class ConfigBeaconStatus extends ConfigStatusMessage {

    public static final Logger LOG = Logger.getLogger(ConfigBeaconStatus.class.getName());
    private static final int OP_CODE = ConfigMessageOpCodes.CONFIG_BEACON_STATUS;

    private boolean enable;

    /**
     * Constructs ConfigBeaconStatus message.
     *
     * @param message {@link AccessMessage}
     */
    public ConfigBeaconStatus(final AccessMessage message) {
        super(message);
        mParameters = message.getParameters();
        parseStatusParameters();
    }

    @Override
    void parseStatusParameters() {
        enable = MeshParserUtils.unsignedByteToInt(mParameters[0]) == ProvisionedBaseMeshNode.ENABLED;
        LOG.info("Secure Network Beacon State: " + enable);
    }

    @Override
    public int getOpCode() {
        return OP_CODE;
    }

    /**
     * Returns the true if the Secure Network beacon State is set to send periodic Secure Network Beacons or false otherwise.
     */
    public boolean isEnable() {
        return enable;
    }
}

