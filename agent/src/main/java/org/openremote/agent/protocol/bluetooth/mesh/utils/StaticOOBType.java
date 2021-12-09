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
package org.openremote.agent.protocol.bluetooth.mesh.utils;

public enum StaticOOBType {

    /**
     * Static OOB Type
     */
    STATIC_OOB_AVAILABLE((short) 0x0001);

    private static final String TAG = StaticOOBType.class.getSimpleName();
    private short staticOobType;

    StaticOOBType(final short staticOobType) {
        this.staticOobType = staticOobType;
    }

    public static String parseStaticOOBActionInformation(final StaticOOBType type) {
        switch (type) {
            case STATIC_OOB_AVAILABLE:
                return "Static OOB Actions available";
            default:
                return "Static OOB Actions unavailable";
        }
    }

    /**
     * Returns the static oob type value
     */
    public short getStaticOobType() {
        return staticOobType;
    }
}
