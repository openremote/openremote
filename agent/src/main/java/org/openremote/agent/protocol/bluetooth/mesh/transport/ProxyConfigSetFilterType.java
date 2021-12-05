package org.openremote.agent.protocol.bluetooth.mesh.transport;

import org.openremote.agent.protocol.bluetooth.mesh.opcodes.ProxyConfigMessageOpCodes;
import org.openremote.agent.protocol.bluetooth.mesh.utils.ProxyFilterType;

/**
 * To be used as a wrapper class to create the ProxyConfigSetFilterType message.
 */
@SuppressWarnings({"WeakerAccess"})
public class ProxyConfigSetFilterType extends ProxyConfigMessage {

    private final ProxyFilterType filterType;

    /**
     * Sets the proxy filter
     *
     * @param filterType Filter type set by the proxy configuration
     */
    public ProxyConfigSetFilterType(final ProxyFilterType filterType) {
        this.filterType = filterType;
        assembleMessageParameters();
    }

    @Override
    void assembleMessageParameters() {
        mParameters = new byte[]{(byte) filterType.getType()};
    }

    @Override
    public int getOpCode() {
        return ProxyConfigMessageOpCodes.SET_FILTER_TYPE;
    }

    @Override
    byte[] getParameters() {
        return mParameters;
    }
}
