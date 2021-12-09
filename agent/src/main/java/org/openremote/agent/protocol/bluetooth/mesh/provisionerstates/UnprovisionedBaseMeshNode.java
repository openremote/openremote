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
package org.openremote.agent.protocol.bluetooth.mesh.provisionerstates;

import org.openremote.agent.protocol.bluetooth.mesh.utils.AuthenticationOOBMethods;
import org.openremote.agent.protocol.bluetooth.mesh.utils.InputOOBAction;
import org.openremote.agent.protocol.bluetooth.mesh.utils.OutputOOBAction;
import org.openremote.agent.protocol.bluetooth.mesh.utils.StaticOOBType;

import java.util.UUID;
import java.util.logging.Logger;

public class UnprovisionedBaseMeshNode {

    public static final Logger LOG = Logger.getLogger(UnprovisionedBaseMeshNode.class.getName());

    private int mConfigurationSrc;
    protected byte[] ivIndex;
    boolean isProvisioned;
    boolean isConfigured;
    protected String nodeName = "My Node";
    byte[] provisionerPublicKeyXY;
    byte[] provisioneePublicKeyXY;
    byte[] sharedECDHSecret;
    byte[] provisionerRandom;
    byte[] provisioneeConfirmation;
    byte[] authenticationValue;
    byte[] provisioneeRandom;
    protected byte[] networkKey;
    byte[] identityKey;
    protected int keyIndex;
    byte[] mFlags;
    protected int unicastAddress;
    byte[] deviceKey;
    protected int ttl = 5;
    private String bluetoothDeviceAddress;
    long mTimeStampInMillis;
    ProvisioningCapabilities provisioningCapabilities;
    int numberOfElements;
    UUID deviceUuid;
    byte[] provisioningInvitePdu;
    //capabilties pdu received by the provisioner
    byte[] provisioningCapabilitiesPdu;
    //provisioning start pdu sent by the provisioner
    byte[] provisioningStartPdu;
    AuthenticationOOBMethods authMethodUsed = AuthenticationOOBMethods.NO_OOB_AUTHENTICATION;
    short authActionUsed;
    byte[] inputAuthentication;

    UnprovisionedBaseMeshNode(final UUID uuid) {
        deviceUuid = uuid;
    }

    public boolean isProvisioned() {
        return isProvisioned;
    }

    public final boolean isConfigured() {
        return isConfigured;
    }

    public final void setConfigured(final boolean configured) {
        isConfigured = configured;
    }

    public final String getNodeName() {
        return nodeName;
    }

    public final void setNodeName(final String nodeName) {
        if (nodeName != null && nodeName.length() > 0)
            this.nodeName = nodeName;
    }

    public final int getUnicastAddress() {
        return unicastAddress;
    }

    public final void setUnicastAddress(final int unicastAddress) {
        this.unicastAddress = unicastAddress;
    }

    public byte[] getDeviceKey() {
        return deviceKey;
    }

    public int getTtl() {
        return ttl;
    }

    public final byte[] getIdentityKey() {
        return identityKey;
    }

    public final int getKeyIndex() {
        return keyIndex;
    }

    public final void setKeyIndex(final int keyIndex) {
        this.keyIndex = keyIndex;
    }

    public final byte[] getFlags() {
        return mFlags;
    }

    public final void setFlags(final byte[] flags) {
        this.mFlags = flags;
    }

    public final byte[] getIvIndex() {
        return ivIndex;
    }

    public final void setIvIndex(final byte[] ivIndex) {
        this.ivIndex = ivIndex;
    }

    public void setTtl(final int ttl) {
        this.ttl = ttl;
    }

    public long getTimeStamp() {
        return mTimeStampInMillis;
    }

    public final int getConfigurationSrc() {
        return mConfigurationSrc;
    }

    public final void setConfigurationSrc(final int src) {
        mConfigurationSrc = src;
    }

    public ProvisioningCapabilities getProvisioningCapabilities() {
        return provisioningCapabilities;
    }

    public int getNumberOfElements() {
        return numberOfElements;
    }

    public UUID getDeviceUuid() {
        return deviceUuid;
    }

    public byte[] getProvisioningInvitePdu() {
        return provisioningInvitePdu;
    }

    void setProvisioningInvitePdu(final byte[] provisioningInvitePdu) {
        this.provisioningInvitePdu = provisioningInvitePdu;
    }

    public byte[] getProvisioningStartPdu() {
        return provisioningStartPdu;
    }

    void setProvisioningStartPdu(final byte[] provisioningStartPdu) {
        this.provisioningStartPdu = provisioningStartPdu;
    }

    public byte[] getProvisioningCapabilitiesPdu() {
        return provisioningCapabilitiesPdu;
    }

    void setProvisioningCapabilitiesPdu(final byte[] provisioningCapabilitiesPdu) {
        this.provisioningCapabilitiesPdu = provisioningCapabilitiesPdu;
    }

    /**
     * Returns the authentication method used during the provisioning process
     */
    public AuthenticationOOBMethods getAuthMethodUsed() {
        return authMethodUsed;
    }

    /**
     * Sets the authentication method used during the provisioning process
     *
     * @param authMethodUsed {@link AuthenticationOOBMethods} authentication methods
     */
    void setAuthMethodUsed(final AuthenticationOOBMethods authMethodUsed) {
        this.authMethodUsed = authMethodUsed;
    }

    /**
     * Returns the auth action value and this depends on the {@link AuthenticationOOBMethods} used and the possible values are
     * {@link StaticOOBType}
     * {@link OutputOOBAction}
     * {@link InputOOBAction}
     */
    public short getAuthActionUsed() {
        return authActionUsed;
    }

    /**
     * Sets the authentication action used when sending the provisioning invite.
     *
     * @param authActionUsed auth action used
     */
    void setAuthActionUsed(final short authActionUsed) {
        this.authActionUsed = authActionUsed;
    }

    /**
     * Returns the input authentication value to be input by the provisioner if Input OOB was selected
     */
    public byte[] getInputAuthentication() {
        return inputAuthentication;
    }

    /**
     * Sets the input authentication value to be input by the provisioner if Input OOB was selected
     *
     * @param inputAuthentication generated input authentication
     */
    void setInputAuthentication(final byte[] inputAuthentication) {
        this.inputAuthentication = inputAuthentication;
    }
}
