package org.openremote.agent.protocol.bluetooth.mesh.transport;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class AccessMessage extends Message {

    private UUID label;                                // Label UUID for destination address
    protected Map<Integer, byte[]> lowerTransportAccessPdu = new HashMap<>();
    private byte[] accessPdu;
    private byte[] transportPdu;

    public AccessMessage() {
        this.ctl = 0;
    }

    @Override
    public int getCtl() {
        return ctl;
    }

    public UUID getLabel() {
        return label;
    }

    public void setLabel(final UUID label) {
        this.label = label;
    }

    public final byte[] getAccessPdu() {
        return accessPdu;
    }

    public final void setAccessPdu(final byte[] accessPdu) {
        this.accessPdu = accessPdu;
    }

    public final byte[] getUpperTransportPdu() {
        return transportPdu;
    }

    public final void setUpperTransportPdu(final byte[] transportPdu) {
        this.transportPdu = transportPdu;
    }

    public final Map<Integer, byte[]> getLowerTransportAccessPdu() {
        return lowerTransportAccessPdu;
    }

    public final void setLowerTransportAccessPdu(final Map<Integer, byte[]> lowerTransportAccessPdu) {
        this.lowerTransportAccessPdu = lowerTransportAccessPdu;
    }
}
