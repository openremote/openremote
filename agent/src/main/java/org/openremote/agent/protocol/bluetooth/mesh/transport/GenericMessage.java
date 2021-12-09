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

import org.openremote.agent.protocol.bluetooth.mesh.ApplicationKey;

abstract class GenericMessage extends MeshMessage {

    public static final int GENERIC_TRANSITION_STEP_0 = 0;
    public static final int GENERIC_TRANSITION_STEP_1 = 1;
    public static final int GENERIC_TRANSITION_STEP_2 = 2;
    public static final int GENERIC_TRANSITION_STEP_3 = 3;

    final ApplicationKey mAppKey;
    byte mAid;

    /**
     * Constructs a generic message
     *
     * @param appKey application key
     */
    GenericMessage(final ApplicationKey appKey) {
        if (appKey.getKey().length != 16)
            throw new IllegalArgumentException("Application key must be 16 bytes");
        this.mAppKey = appKey;
    }

    @Override
    public final int getAkf() {
        return 1;
    }

    @Override
    public final int getAid() {
        return mAid;
    }

    /**
     * Returns the app key used in this message.
     *
     * @return app key
     */
    public final ApplicationKey getAppKey() {
        return mAppKey;
    }

    @Override
    public final byte[] getParameters() {
        return mParameters;
    }

    /**
     * Creates the parameters for a given mesh message.
     */
    abstract void assembleMessageParameters();
}

