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

import org.openremote.agent.protocol.bluetooth.mesh.opcodes.ProxyConfigMessageOpCodes;
import org.openremote.agent.protocol.bluetooth.mesh.utils.AddressArray;

import java.util.ArrayList;
import java.util.List;

/**
 * To be used as a wrapper class to create the ProxyConfigSetFilterType message.
 */
public class ProxyConfigRemoveAddressFromFilter extends ProxyConfigMessage {

    private final List<AddressArray> addresses;

    /**
     * Sets the proxy filter
     *
     * @param addresses List of addresses to be added to the filter
     */
    public ProxyConfigRemoveAddressFromFilter(final List<AddressArray> addresses) throws IllegalArgumentException {
        this.addresses = new ArrayList<>();
        this.addresses.addAll(addresses);
        assembleMessageParameters();
    }

    @Override
    void assembleMessageParameters() throws IllegalArgumentException {
        if (addresses.isEmpty())
            throw new IllegalArgumentException("Address list cannot be empty!");
        final int length = (int) Math.pow(2, addresses.size());
        mParameters = new byte[length];
        int count = 0;
        for (AddressArray addressArray : addresses) {
            mParameters[count] = addressArray.getAddress()[0];
            mParameters[count + 1] = addressArray.getAddress()[1];
            count += 2;
        }
    }

    @Override
    public int getOpCode() {
        return ProxyConfigMessageOpCodes.REMOVE_ADDRESS;
    }

    @Override
    byte[] getParameters() {
        return mParameters;
    }

    /**
     * Returns the addresses that were added to the proxy filter
     */
    public List<AddressArray> getAddresses() {
        return addresses;
    }
}
