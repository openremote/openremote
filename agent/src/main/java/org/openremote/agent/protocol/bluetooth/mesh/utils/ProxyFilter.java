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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Contains the proxy filter configuration set to a node
 */
public class ProxyFilter {
    private final ProxyFilterType filterType;
    private final List<AddressArray> addresses = new ArrayList<>();

    /**
     * Constructs the proxy filter
     *
     * @param filterType Filter type based on
     */
    public ProxyFilter(final ProxyFilterType filterType) {
        this.filterType = filterType;
    }

    /**
     * Returns the {@link ProxyFilterType} that was set
     */
    public ProxyFilterType getFilterType() {
        return filterType;
    }

    /**
     * Returns the list of addresses containing {@link AddressArray} added to the proxy filter
     */
    public List<AddressArray> getAddresses() {
        return Collections.unmodifiableList(addresses);
    }

    /**
     * Adds an address to the ProxyFilter
     *
     * @param addressArray address to be added
     */
    public void addAddress(final AddressArray addressArray) {
        if (!contains(addressArray)) {
            addresses.add(addressArray);
        }
    }

    /**
     * Checks is the address exists within the list of proxy filter addresses.
     *
     * @param addressArray address
     */
    private boolean contains(final AddressArray addressArray) {
        for (AddressArray arr : addresses) {
            if (Arrays.equals(addressArray.getAddress(), arr.getAddress())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks is the address exists within the list of proxy filter addresses.
     * <p>
     * Currently does not support virtual addresses.
     * </p>
     *
     * @param address Unicast, Group address
     */
    public final boolean contains(final byte[] address) {
        if (MeshAddress.isValidUnicastAddress(address) || MeshAddress.isValidSubscriptionAddress(address)) {
            final AddressArray addressArray = new AddressArray(address[0], address[1]);
            for (AddressArray arr : addresses) {
                if (Arrays.equals(addressArray.getAddress(), arr.getAddress())) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Removes an address from the ProxyFilter
     *
     * @param addressArray address to be removed
     */
    public void removeAddress(final AddressArray addressArray) {
        addresses.remove(addressArray);
    }

}
