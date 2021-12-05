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
