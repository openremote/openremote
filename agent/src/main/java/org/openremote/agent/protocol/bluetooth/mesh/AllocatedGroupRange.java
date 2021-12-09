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

import org.openremote.agent.protocol.bluetooth.mesh.utils.MeshAddress;

public class AllocatedGroupRange extends AddressRange {

    @Override
    public final int getLowerBound() {
        return MeshAddress.START_GROUP_ADDRESS;
    }

    @Override
    public final int getUpperBound() {
        return MeshAddress.END_GROUP_ADDRESS;
    }

    /**
     * Constructs {@link AllocatedGroupRange} for provisioner
     *
     * @param lowAddress  low address of group range
     * @param highAddress high address of group range
     */
    public AllocatedGroupRange(final int lowAddress, final int highAddress) {
        lowerBound = MeshAddress.START_GROUP_ADDRESS;
        upperBound = MeshAddress.END_GROUP_ADDRESS;
        if (!MeshAddress.isValidGroupAddress(lowAddress))
            throw new IllegalArgumentException("Low address must range from 0xC000 to 0xFEFF");

        if (!MeshAddress.isValidGroupAddress(highAddress))
            throw new IllegalArgumentException("High address must range from 0xC000 to 0xFEFF");

        /*if(lowAddress > highAddress)
            throw new IllegalArgumentException("low address must be lower than the high address");*/

        this.lowAddress = lowAddress;
        this.highAddress = highAddress;
    }

    AllocatedGroupRange() {
    }

    @Override
    public int getLowAddress() {
        return lowAddress;
    }

    /**
     * Sets the low address of the allocated group address
     *
     * @param lowAddress of the group range
     */
    public void setLowAddress(final int lowAddress) {
        this.lowAddress = lowAddress;
    }

    @Override
    public int getHighAddress() {
        return highAddress;
    }

    /**
     * Sets the high address of the group address
     *
     * @param highAddress of the group range
     */
    public void setHighAddress(final int highAddress) {
        this.highAddress = highAddress;
    }
}
