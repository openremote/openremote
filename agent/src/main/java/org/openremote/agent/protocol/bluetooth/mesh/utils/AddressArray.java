package org.openremote.agent.protocol.bluetooth.mesh.utils;

/**
 * Wrapper class for addresses
 */
public class AddressArray {

    final byte[] address = new byte[2];

    /**
     * Constructs the AddressArray
     *
     * @param b1 address byte1
     * @param b2 address byte2
     */
    public AddressArray(final byte b1, final byte b2) {
        address[0] = b1;
        address[1] = b2;
    }

    /**
     * Returns address used in the filter message
     */
    public byte[] getAddress() {
        return address;
    }
}
