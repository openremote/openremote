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

import org.openremote.agent.protocol.bluetooth.mesh.ApplicationKey;
import org.openremote.agent.protocol.bluetooth.mesh.NetworkKey;

import java.util.Map;

public abstract class Message {

    protected int ctl;                                  // If ctl = 0 access message and ctl = 1 control message
    protected Map<Integer, byte[]> networkLayerPdu;     // Mesh pdu
    private int pduType;                                // PDU Type
    private int ttl = 100;                              // Time to live
    private int src;                                    // Source address
    private int dst;                                    // Destination address
    private byte[] mSequenceNumber;                     // unique 24-bit value for each message
    private byte[] deviceKey;                           // Used for transport layer encryption of configuration messages
    private ApplicationKey applicationKey;              // Used for transport layer encryption of application messages
    private NetworkKey networkKey;                      // Used for transport layer encryption of application messages
    private byte[] encryptionKey;                       // Derived from network key using k2 function
    private byte[] privacyKey;                          // Derived from privacy key using k2 function
    private int akf;                                    // Use device key for encryption if akf = 0 or application key otherwise
    private int aid;                                    // Used to identify the application key generated using k4 function
    private int aszmic;                                 // if aszmic = 0 the transmic is 32-bits, if aszmic = 1 transmic 64-bits this is usually for a segmented message
    private int opCode;                                 // Opcode of message
    private byte[] parameters;                          // Parameters of the message
    private int companyIdentifier;                      // Company identifier for vendor model messages
    private byte[] ivIndex;                             // IV Index of the network
    private boolean segmented;

    Message() {
    }

    public abstract int getCtl();

    public int getPduType() {
        return pduType;
    }

    public void setPduType(final int pduType) {
        this.pduType = pduType;
    }

    public final int getTtl() {
        return ttl;
    }

    public final void setTtl(final int ttl) {
        this.ttl = ttl;
    }

    public final int getSrc() {
        return src;
    }

    public final void setSrc(final int src) {
        this.src = src;
    }

    public final int getDst() {
        return dst;
    }

    public final void setDst(final int dst) {
        this.dst = dst;
    }

    public final byte[] getSequenceNumber() {
        return mSequenceNumber;
    }

    public final void setSequenceNumber(final byte[] sequenceNumber) {
        this.mSequenceNumber = sequenceNumber;
    }

    public final byte[] getDeviceKey() {
        return deviceKey;
    }

    public final void setDeviceKey(final byte[] deviceKey) {
        this.deviceKey = deviceKey;
    }

    public final ApplicationKey getApplicationKey() {
        return applicationKey;
    }

    public final void setApplicationKey(final ApplicationKey key) {
        this.applicationKey = key;
    }

    public NetworkKey getNetworkKey() {
        return networkKey;
    }

    public void setNetworkKey(final NetworkKey networkKey) {
        this.networkKey = networkKey;
    }

    public final byte[] getEncryptionKey() {
        return encryptionKey;
    }

    public final void setEncryptionKey(final byte[] encryptionKey) {
        this.encryptionKey = encryptionKey;
    }

    public final byte[] getPrivacyKey() {
        return privacyKey;
    }

    public final void setPrivacyKey(final byte[] privacyKey) {
        this.privacyKey = privacyKey;
    }

    public final int getAkf() {
        return akf;
    }

    public final void setAkf(final int akf) {
        this.akf = akf;
    }

    public final int getAid() {
        return aid;
    }

    public final void setAid(final int aid) {
        this.aid = aid;
    }

    public final int getAszmic() {
        return aszmic;
    }

    public final void setAszmic(final int aszmic) {
        this.aszmic = aszmic;
    }

    public final int getOpCode() {
        return opCode;
    }

    public final void setOpCode(final int opCode) {
        this.opCode = opCode;
    }

    public final byte[] getParameters() {
        return parameters;
    }

    public final void setParameters(final byte[] parameters) {
        this.parameters = parameters;
    }

    public final int getCompanyIdentifier() {
        return companyIdentifier;
    }

    public final void setCompanyIdentifier(final int companyIdentifier) {
        this.companyIdentifier = companyIdentifier;
    }

    public final byte[] getIvIndex() {
        return ivIndex;
    }

    public final void setIvIndex(final byte[] ivIndex) {
        this.ivIndex = ivIndex;
    }

    public final boolean isSegmented() {
        return segmented;
    }

    final void setSegmented(final boolean segmented) {
        this.segmented = segmented;
    }

    public final Map<Integer, byte[]> getNetworkLayerPdu() {
        return networkLayerPdu;
    }

    final void setNetworkLayerPdu(final Map<Integer, byte[]> pdu) {
        networkLayerPdu = pdu;
    }
}
