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

import org.openremote.agent.protocol.bluetooth.mesh.utils.MeshParserUtils;

import java.nio.ByteBuffer;
import java.util.Calendar;

/**
 * Contains the information related to a secure network beacon.
 */
public class SecureNetworkBeacon extends MeshBeacon {
    public static final int BEACON_DATA_LENGTH = 22;
    private final int flags;
    private boolean isKeyRefreshActive;
    private final byte[] networkId = new byte[8];
    private final IvIndex ivIndex;
    private final byte[] authenticationValue = new byte[8];

    /**
     * Constructs a {@link SecureNetworkBeacon} object
     *
     * @param beaconData beacon data advertised by the mesh beacon
     * @throws IllegalArgumentException if service data provide is invalid
     */
    public SecureNetworkBeacon(final byte[] beaconData) {
        super(beaconData);
        if (beaconData.length != BEACON_DATA_LENGTH)
            throw new IllegalArgumentException("Incorrect Secure Network Beacon length: " + beaconData.length
                + ", expected: " + BEACON_DATA_LENGTH + ". Check MTU and Proxy Protocol segmentation.");

        final ByteBuffer byteBuffer = ByteBuffer.wrap(beaconData);
        byteBuffer.position(1);
        flags = byteBuffer.get();
        isKeyRefreshActive = (flags & 0x01) == 1;
        final boolean isIvUpdateActive = ((flags & 0x02) >> 1) == BaseMeshNetwork.IV_UPDATE_ACTIVE;
        byteBuffer.get(networkId, 0, 8);
        ivIndex = new IvIndex(byteBuffer.getInt(), isIvUpdateActive, Calendar.getInstance());
        byteBuffer.get(authenticationValue, 0, 8);
    }

    @Override
    public String toString() {
        return "SecureNetworkBeacon {" +
            " KeyRefreshActive: " + isKeyRefreshActive +
            ", IV Index: " + ivIndex +
            ", Authentication Value: " + MeshParserUtils.bytesToHex(authenticationValue, true) + "}";
    }

    @Override
    public int getBeaconType() {
        return beaconType;
    }

    /**
     * Returns the flags of the secure network beacon
     */
    public int getFlags() {
        return flags;
    }

    /**
     * Returns true if Key Refresh Procedure is active.
     */
    public boolean isKeyRefreshActive() {
        return isKeyRefreshActive;
    }

    /**
     * Returns the network id of the beacon or the node
     */
    public byte[] getNetworkId() {
        return networkId;
    }

    /**
     * Returns the iv index of the beacon or the node
     */
    public IvIndex getIvIndex() {
        return ivIndex;
    }

    /**
     * Returns the authentication value of the beacon
     */
    public byte[] getAuthenticationValue() {
        return authenticationValue;
    }

    /**
     * This method returns whether the received Secure Network Beacon can override
     * the current IV Index.
     * <p>
     * The following restrictions apply:
     * 1. Normal Operation state must last for at least 96 hours.
     * 2. IV Update In Progress state must take at least 96 hours and may not be longer than 144h.
     * 3. IV Index must not decrease.
     * 4. If received Secure Network Beacon has IV Index greater than current IV Index + 1, the
     * device will go into IV Index Recovery procedure. In this state, the 96h rule does not apply
     * and the IV Index or IV Update Active flag may change before 96 hours.
     * 5. If received Secure Network Beacon has IV Index greater than current IV Index + 42, the
     * beacon should be ignored (unless a setting in MeshNetworkManager is set to disable this rule).
     * 6. The node shall not execute more than one IV Index Recovery within a period of 192 hours.
     * <p>
     * Note: Library versions before 2.2.2 did not store the last IV Index, so the date and IV Recovery
     * flag are optional.
     * <p>
     * - parameters:
     * - target: The IV Index to compare.
     * - date: The date of the most recent transition to the current IV Index.
     * - ivRecoveryActive: True if the IV Recovery procedure was used to restore
     * the IV Index on the previous connection.
     * - ivTestMode: True, if IV Update test mode is enabled; false otherwise.
     * - ivRecoveryOver42Allowed: Whether the IV Index Recovery procedure should be limited
     * to allow maximum increase of IV Index by 42.
     * - returns: True, if the Secure Network beacon can be applied; false otherwise.
     * - since: 2.2.2
     * - seeAlso: Bluetooth Mesh Profile 1.0.1, section 3.10.5.
     */
    protected boolean canOverwrite(final IvIndex ivIndex, final Calendar updatedAt,
                                   final boolean ivRecoveryActive,
                                   final boolean isTestMode,
                                   final boolean ivRecoveryOver42Allowed) {
        // IV Index must increase, or, in case it's equal to the current one,
        // the IV Update Active flag must change from true to false.
        // The new index must not be greater than the current one + 42,
        // unless this rule is disabled.
        if ((this.ivIndex.getIvIndex() > ivIndex.getIvIndex() &&
            (ivRecoveryOver42Allowed || this.ivIndex.getIvIndex() <= ivIndex.getIvIndex() + 42)) ||
            (this.ivIndex.getIvIndex() == ivIndex.getIvIndex() &&
                (ivIndex.isIvUpdateActive() || !this.ivIndex.isIvUpdateActive()))) {

            // Before version 2.2.2 the timestamp was not stored. The initial
            // Secure Network Beacon is assumed to be valid.
            return isMinimumTimeRequirementCompleted(ivIndex, updatedAt, ivRecoveryActive, isTestMode);
        } else {
            return false;
        }
    }

    private boolean isMinimumTimeRequirementCompleted(final IvIndex ivIndex,
                                                      final Calendar updatedAt,
                                                      final boolean isIvRecoveryActive,
                                                      final boolean isTestMode) {
        // Let's define a "state" as a pair of IV and IV Update Active flag.
        // "States" change as follows:
        // 1. IV = X,   IVUA = false (Normal Operation)
        // 2. IV = X+1, IVUA = true  (Update In Progress)
        // 3. IV = X+1, IVUA = false (Normal Operation)
        // 4. IV = X+2, IVUA = true  (Update In Progress)
        // 5. ...

        // Calculate number of states between the state defined by the target
        // IV Index and this Secure Network Beacon.
        int stateDiff = (this.ivIndex.getIvIndex() - ivIndex.getIvIndex()) * 2 - 1
            + (ivIndex.isIvUpdateActive() ? 1 : 0)
            + (this.ivIndex.isIvUpdateActive() ? 0 : 1)
            - (isIvRecoveryActive || isTestMode ? 1 : 0); // this may set stateDiff = -1

        // Each "state" must last for at least 96 hours.
        // Calculate the minimum number of hours that had to pass since last state
        // change for the Secure Network Beacon to be assumed valid.
        // If more has passed, it's also valid, as Normal Operation has no maximum
        // time duration.
        int numberOfHoursRequired = stateDiff * 96;

        // Get the number of hours since the state changed last time.
        final long timeDifference = Calendar.getInstance().getTimeInMillis() - updatedAt.getTimeInMillis();
        final int numberOfHoursSinceDate = (int) (timeDifference / (3600 * 1000));

        // The node shall not execute more than one IV Index Recovery within a
        // period of 192 hours.
        if (isIvRecoveryActive && stateDiff > 1 && numberOfHoursSinceDate < 192) {
            return false;
        }

        return numberOfHoursSinceDate >= numberOfHoursRequired;
    }

}

