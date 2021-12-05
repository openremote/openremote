package org.openremote.agent.protocol.bluetooth.mesh.opcodes;

public class ProxyConfigMessageOpCodes {

    /**
     * Opcode sent by a Proxy Client to set the proxy filter type.
     */
    public static final int SET_FILTER_TYPE = 0x00;

    /**
     * Opcode sent by a Proxy Client to add addresses to the proxy filter list.
     */
    public static final int ADD_ADDRESS = 0x01;

    /**
     * Opcode sent by a Proxy Client to remove addresses from the proxy filter list.
     */
    public static final int REMOVE_ADDRESS = 0x02;

    /**
     * Acknowledgment opcode sent by a Proxy Server to a Proxy Client to report the status of the proxy filter list.
     */
    public static final int FILTER_STATUS = 0x03;

}
