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
package org.openremote.agent.protocol.bluetooth.mesh;

/**
 * Abstract class containing mesh beacon information
 */
public abstract class MeshBeacon {

    private static final String TAG = MeshBeacon.class.getSimpleName();
    static final int MESH_BEACON = 0x2B;
    final byte[] beaconData;
    final int beaconType;


    /**
     * Constructs a {@link MeshBeacon} object
     *
     * @param beaconData beacon data advertised by the mesh beacon
     * @throws IllegalArgumentException if beacon data provided is empty or null
     */
    MeshBeacon(final byte[] beaconData) {
        if (beaconData == null)
            throw new IllegalArgumentException("Invalid beacon data");
        this.beaconData = beaconData;
        beaconType = beaconData[0];
    }

    /**
     * Returns the beacon type value
     */
    public abstract int getBeaconType();

}
