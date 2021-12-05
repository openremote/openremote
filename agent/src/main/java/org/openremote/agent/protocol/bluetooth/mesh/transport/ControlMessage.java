package org.openremote.agent.protocol.bluetooth.mesh.transport;

import org.openremote.agent.protocol.bluetooth.mesh.control.TransportControlMessage;

import java.util.HashMap;
import java.util.Map;

public final class ControlMessage extends Message {

    protected Map<Integer, byte[]> lowerTransportControlPdu = new HashMap<>();
    private byte[] transportControlPdu;
    private TransportControlMessage transportControlMessage;

    public ControlMessage() {
        this.ctl = 1;
    }

    @Override
    public int getCtl() {
        return ctl;
    }

    public byte[] getTransportControlPdu() {
        return transportControlPdu;
    }

    public void setTransportControlPdu(final byte[] transportControlPdu) {
        this.transportControlPdu = transportControlPdu;
    }

    public Map<Integer, byte[]> getLowerTransportControlPdu() {
        return lowerTransportControlPdu;
    }

    public void setLowerTransportControlPdu(final Map<Integer, byte[]> segmentedAccessMessages) {
        this.lowerTransportControlPdu = segmentedAccessMessages;
    }

    public TransportControlMessage getTransportControlMessage() {
        return transportControlMessage;
    }

    public void setTransportControlMessage(final TransportControlMessage transportControlMessage) {
        this.transportControlMessage = transportControlMessage;
    }
}

