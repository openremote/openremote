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

import org.openremote.agent.protocol.bluetooth.mesh.NetworkKey;
import org.openremote.agent.protocol.bluetooth.mesh.utils.MeshParserUtils;

import java.util.logging.Logger;

import static org.openremote.agent.protocol.bluetooth.mesh.opcodes.ConfigMessageOpCodes.CONFIG_KEY_REFRESH_PHASE_SET;

/**
 * Creates the ConfigKeyRefreshPhaseSet message.
 */
public class ConfigKeyRefreshPhaseSet extends ConfigMessage {

    public static final Logger LOG = Logger.getLogger(ConfigKeyRefreshPhaseSet.class.getName());
    private static final int OP_CODE = CONFIG_KEY_REFRESH_PHASE_SET;
    private final NetworkKey mNetKey;
    private final /* @KeyRefreshPhaseTransition */
    int transition;

    /**
     * Constructs ConfigKeyRefreshPhaseSet message.
     *
     * @param networkKey {@link NetworkKey}
     */
    public ConfigKeyRefreshPhaseSet(final NetworkKey networkKey, /* @KeyRefreshPhaseTransition */ final int transition) {
        mNetKey = networkKey;
        this.transition = transition;
        assembleMessageParameters();
    }

    @Override
    public int getOpCode() {
        return OP_CODE;
    }

    @Override
    void assembleMessageParameters() {
        LOG.info("NetKeyIndex: " + mNetKey.getKeyIndex());
        final byte[] netKeyIndex = MeshParserUtils.addKeyIndexPadding(mNetKey.getKeyIndex());
        mParameters = new byte[]{netKeyIndex[1], (byte) ((netKeyIndex[0] & 0xFF) & 0x0F), (byte) transition};
    }
}

