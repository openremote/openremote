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
