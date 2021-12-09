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
package org.openremote.agent.protocol.bluetooth.mesh;

import org.openremote.agent.protocol.bluetooth.mesh.utils.SecureUtils;

import javax.validation.constraints.NotNull;

/**
 * Wrapper class for network key
 */
public final class NetworkKey extends MeshKey {

    // Key refresh phases
    public static final int NORMAL_OPERATION = 0;
    public static final int KEY_DISTRIBUTION = 1;
    public static final int USING_NEW_KEYS = 2;
    //public static final int REVOKING_OLD_KEYS   = 3; This phase is instantaneous.

    // Transitions
    public static final int USE_NEW_KEYS = 2; //Normal operation
    public static final int REVOKE_OLD_KEYS = 3; //Key Distribution

    private int phase = NORMAL_OPERATION;

    private boolean minSecurity;

    private long timestamp;

    private byte[] identityKey;

    private byte[] oldIdentityKey;

    private SecureUtils.K2Output derivatives;

    private SecureUtils.K2Output oldDerivatives;

    /**
     * Constructs a NetworkKey object with a given key index and network key
     *
     * @param keyIndex 12-bit network key index
     * @param key      16-byte network key
     */
    public NetworkKey(final int keyIndex, @NotNull final byte[] key) {
        super(keyIndex, key);
        name = "Network Key " + (keyIndex + 1);
        identityKey = SecureUtils.calculateIdentityKey(key);
        derivatives = SecureUtils.calculateK2(key, SecureUtils.K2_MASTER_INPUT);
        timestamp = System.currentTimeMillis();
    }

    /**
     * Returns the key refresh phase of the network key
     *
     * @return int phase
     */
    public int getPhase() {
        return phase;
    }

    public void setPhase(final int phase) {
        this.phase = phase;
    }

    @Override
    public void setKey(@NotNull final byte[] key) {
        super.setKey(key);
        identityKey = SecureUtils.calculateIdentityKey(key);
        derivatives = SecureUtils.calculateK2(key, SecureUtils.K2_MASTER_INPUT);
    }

    @Override
    public void setOldKey(final byte[] oldKey) {
        super.setOldKey(oldKey);
        oldIdentityKey = SecureUtils.calculateIdentityKey(oldKey);
        oldDerivatives = SecureUtils.calculateK2(oldKey, SecureUtils.K2_MASTER_INPUT);
    }

    /**
     * Returns the NetworkKey based on the key refresh procedure phase.
     *
     * @return key
     */
    public byte[] getTxNetworkKey() {
        switch (phase) {
            case KEY_DISTRIBUTION:
                return oldKey;
            case USING_NEW_KEYS:
            default:
                return key;
        }
    }

    /**
     * Returns the NetworkKey based on the key refresh procedure phase.
     *
     * @return key
     */
    public SecureUtils.K2Output getTxDerivatives() {
        switch (phase) {
            case KEY_DISTRIBUTION:
                return oldDerivatives;
            case USING_NEW_KEYS:
            default:
                return derivatives;
        }
    }

    /**
     * Uses min security
     *
     * @return true if minimum security or false otherwise
     */
    public boolean isMinSecurity() {
        return minSecurity;
    }

    /**
     * Sets  the minimum security.
     *
     * @param minSecurity true if minimum security or false if insecure.
     */
    public void setMinSecurity(final boolean minSecurity) {
        this.minSecurity = minSecurity;
    }

    /**
     * Returns the identity key derived from the current key
     */
    public byte[] getIdentityKey() {
        return identityKey;
    }

    /**
     * Returns the identity key derived from the Old Key
     */
    public byte[] getOldIdentityKey() {
        return oldIdentityKey;
    }

    /**
     * Returns the timestamp of the phase change
     *
     * @return timestamp
     */
    public long getTimestamp() {
        return timestamp;
    }

    /**
     * Returns the current phase description
     */
    public String getPhaseDescription() {
        switch (phase) {
            default:
            case NORMAL_OPERATION:
                return "Normal Operation";
            case KEY_DISTRIBUTION:
                return "Key Distribution";
            case USING_NEW_KEYS:
                return "Using New Keys";
        }
    }

    /**
     * Set the timestamp when the the phase change happened
     *
     * @param timestamp timestamp
     */
    public void setTimestamp(final long timestamp) {
        this.timestamp = timestamp;
    }

    public NetworkKey clone() throws CloneNotSupportedException {
        return (NetworkKey) super.clone();
    }

    /**
     * Updates the currently used {@link #key} with the newKey  and sets the currently used key as the {@link #oldKey}
     *
     * @param newKey New NetworkKey value
     * @return true if successful or false otherwise
     * @throws IllegalArgumentException if a NetworkKey distribution is attempted twice with different key
     *                                  values during a single Key refresh procedure
     */
    protected boolean distributeKey(@NotNull final byte[] newKey) throws IllegalArgumentException {
        if (valid(newKey)) {
            if (phase == 0 || phase == 1) {
                phase = 1;
                timestamp = System.currentTimeMillis();
                return super.distributeKey(newKey);
            } else {
                throw new IllegalArgumentException("A NetworkKey can only be updated once during a Key Refresh Procedure.");
            }
        }
        return false;
    }

    /**
     * Switch to New Key.
     *
     * @return true if successful or false otherwise.
     */
    protected boolean switchToNewKey() {
        if (phase == 1) {
            setPhase(USING_NEW_KEYS);
            timestamp = System.currentTimeMillis();
            return true;
        }
        return false;
    }

    /**
     * Revokes old key by switching the phase to {KEY_DISTRIBUTION or USING_NEW_KEYS}
     *
     * @return true if successful or false otherwise.
     */
    protected boolean revokeOldKey() {
        if (phase == KEY_DISTRIBUTION || phase == USING_NEW_KEYS) {
            phase = NORMAL_OPERATION;
            timestamp = System.currentTimeMillis();
            return true;
        }
        return false;
    }

    protected byte[] getNetworkId() {
        return SecureUtils.calculateK3(key);
    }

    protected byte[] getOldNetworkId() {
        return SecureUtils.calculateK3(oldKey);
    }


    /**
     * Returns the derivatives from the network key
     *
     * @return {@link SecureUtils.K2Output}
     */
    public SecureUtils.K2Output getDerivatives() {
        return derivatives;
    }

    /**
     * Returns the derivatives from the old network key
     *
     * @return {@link SecureUtils.K2Output}
     */
    public SecureUtils.K2Output getOldDerivatives() {
        return oldDerivatives;
    }
}

