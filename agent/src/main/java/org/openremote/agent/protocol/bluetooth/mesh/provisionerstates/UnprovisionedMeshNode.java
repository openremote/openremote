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

import org.openremote.agent.protocol.bluetooth.mesh.utils.SecureUtils;

import java.util.UUID;

public final class UnprovisionedMeshNode extends UnprovisionedBaseMeshNode {

    public UnprovisionedMeshNode(final UUID uuid) {
        super(uuid);
    }

    public final byte[] getSharedECDHSecret() {
        return sharedECDHSecret;
    }

    final void setSharedECDHSecret(final byte[] sharedECDHSecret) {
        this.sharedECDHSecret = sharedECDHSecret;
    }

    public final byte[] getProvisionerPublicKeyXY() {
        return provisionerPublicKeyXY;
    }

    public final void setProvisionerPublicKeyXY(final byte[] rawProvisionerKey) {
        this.provisionerPublicKeyXY = rawProvisionerKey;
    }

    public final byte[] getProvisioneePublicKeyXY() {
        return provisioneePublicKeyXY;
    }

    final void setProvisioneePublicKeyXY(final byte[] provisioneePublicKeyXY) {
        this.provisioneePublicKeyXY = provisioneePublicKeyXY;
    }

    public final byte[] getProvisionerRandom() {
        return provisionerRandom;
    }

    final void setProvisionerRandom(final byte[] provisionerRandom) {
        this.provisionerRandom = provisionerRandom;
    }

    public final byte[] getProvisioneeConfirmation() {
        return provisioneeConfirmation;
    }

    final void setProvisioneeConfirmation(final byte[] provisioneeConfirmation) {
        this.provisioneeConfirmation = provisioneeConfirmation;
    }

    /**
     * Returns the 128-bit authentication value generated based on the user selected OOB type
     */
    public final byte[] getAuthenticationValue() {
        return authenticationValue;
    }

    /**
     * Sets the 128-bit authentication value generated based on the user input if the user input was selected
     * @param authenticationValue 128-bit auth value
     */
    final void setAuthenticationValue(final byte[] authenticationValue) {
        this.authenticationValue = authenticationValue;
    }

    public final byte[] getProvisioneeRandom() {
        return provisioneeRandom;
    }

    final void setProvisioneeRandom(final byte[] provisioneeRandom) {
        this.provisioneeRandom = provisioneeRandom;
    }

    public final byte[] getNetworkKey() {
        return networkKey;
    }

    public final void setNetworkKey(final byte[] networkKey) {
        this.networkKey = networkKey;
    }

    final void setDeviceKey(final byte[] deviceKey) {
        this.deviceKey = deviceKey;
    }

    final void setProvisionedTime(final long timeStampInMillis) {
        mTimeStampInMillis = timeStampInMillis;
    }

    void setProvisioningCapabilities(final ProvisioningCapabilities provisioningCapabilities) {
        numberOfElements = provisioningCapabilities.getNumberOfElements();
        this.provisioningCapabilities = provisioningCapabilities;
    }

    final void setIsProvisioned(final boolean isProvisioned) {
        this.isProvisioned = isProvisioned;
        if (isProvisioned) {
            identityKey = SecureUtils.calculateIdentityKey(networkKey);
        }
    }
}