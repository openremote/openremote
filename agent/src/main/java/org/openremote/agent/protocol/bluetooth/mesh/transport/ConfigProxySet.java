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

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * To be used as a wrapper class to create a ConfigProxySet message.
 */
public class ConfigProxySet extends ConfigMessage {

    public static final int PROXY_FEATURE_DISABLED = 0x00;
    public static final int PROXY_FEATURE_ENABLED = 0x01;
    private static final String TAG = ConfigProxySet.class.getSimpleName();
    private static final int OP_CODE = ConfigMessageOpCodes.CONFIG_GATT_PROXY_SET;
    private final int proxyState;

    /**
     * Constructs ConfigNodeReset message.
     *
     * @throws IllegalArgumentException if any illegal arguments are passed
     */
    public ConfigProxySet(/* @ProxyState */ final int proxyState) throws IllegalArgumentException {
        if (proxyState != PROXY_FEATURE_DISABLED && proxyState != PROXY_FEATURE_ENABLED)
            throw new IllegalArgumentException("Invalid proxy state value.");
        this.proxyState = proxyState;
        assembleMessageParameters();
    }

    @Override
    public int getOpCode() {
        return OP_CODE;
    }

    @Override
    void assembleMessageParameters() {
        mParameters = new byte[]{(byte) proxyState};
    }

    // @Retention(RetentionPolicy.SOURCE)
    // @IntDef({PROXY_FEATURE_DISABLED, PROXY_FEATURE_ENABLED})
    // public @interface ProxyState {
    // }
}

