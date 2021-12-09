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

/**
 * To be used as a wrapper class for when creating the ConfigRelayStatus message.
 */
public final class ConfigProxyStatus extends ConfigStatusMessage {

    private static final String TAG = ConfigProxyStatus.class.getSimpleName();
    private static final int OP_CODE = ConfigMessageOpCodes.CONFIG_GATT_PROXY_STATUS;
    private int mProxyState;

    /**
     * Constructs a ConfigRelayStatus message.
     *
     * @param message Access message received
     * @throws IllegalArgumentException if any illegal arguments are passed
     */
    public ConfigProxyStatus(final AccessMessage message) {
        super(message);
        this.mParameters = message.getParameters();
        parseStatusParameters();
    }

    @Override
    public int getOpCode() {
        return OP_CODE;
    }

    @Override
    final void parseStatusParameters() {
        final byte[] payload = ((AccessMessage) mMessage).getAccessPdu();
        mProxyState = payload[2];
    }


    /**
     * Returns the current ConfigProxySet.ProxyState of the node
     */
    /* @ConfigProxySet.ProxyState */
    public int getProxyState() {
        return mProxyState;
    }

    /**
     * Returns true if the proxy feature is currently enabled on the node and false otherwise
     */
    public boolean isProxyFeatureEnabled() {
        return mProxyState == ConfigProxySet.PROXY_FEATURE_ENABLED;
    }
}

