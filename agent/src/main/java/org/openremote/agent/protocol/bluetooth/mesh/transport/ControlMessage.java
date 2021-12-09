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

