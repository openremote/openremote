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
package org.openremote.agent.protocol.bluetooth.mesh.utils;

import java.util.ArrayList;
import java.util.logging.Logger;

public enum AlgorithmType {


    /**
     * Static OOB Type
     */
    NONE((short) 0x0000),
    FIPS_P_256_ELLIPTIC_CURVE((short) 0x0001);

    private static final Logger LOG = java.util.logging.Logger.getLogger(AlgorithmType.class.getName());
    private short algorithmType;

    AlgorithmType(final short algorithmType) {
        this.algorithmType = algorithmType;
    }

    /**
     * Returns the algorithm oob type value
     */
    public short getAlgorithmType() {
        return algorithmType;
    }


    /**
     * Returns the oob method used for authentication
     *
     * @param method auth method used
     */
    public static AlgorithmType fromValue(final short method) {
        switch (method) {
            default:
            case 0x0000:
                return NONE;
            case 0x0001:
                return FIPS_P_256_ELLIPTIC_CURVE;
        }
    }

    /**
     * Parses the output oob action value
     *
     * @param algorithmTypeValue algorithm type
     * @return selected output action type
     */
    public static ArrayList<AlgorithmType> getAlgorithmTypeFromBitMask(final short algorithmTypeValue) {
        final AlgorithmType[] algorithmTypes = {FIPS_P_256_ELLIPTIC_CURVE};
        final ArrayList<AlgorithmType> supportedAlgorithms = new ArrayList<>();
        for (AlgorithmType algorithmType : algorithmTypes) {
            if ((algorithmTypeValue & algorithmType.ordinal()) == algorithmType.ordinal()) {
                supportedAlgorithms.add(algorithmType);
                LOG.info("Supported output oob action type: " + getAlgorithmTypeDescription(algorithmType));
            }
        }
        return supportedAlgorithms;
    }

    /**
     * Returns the algorithm description
     *
     * @param type {@link AlgorithmType} type
     * @return Input OOB type description
     */
    public static String getAlgorithmTypeDescription(final AlgorithmType type) {
        switch (type) {
            case FIPS_P_256_ELLIPTIC_CURVE:
                return "FIPS P-256 Elliptic Curve";
            default:
                return "Unknown";
        }
    }

    public static byte getAlgorithmValue(final short type) {
        switch (fromValue(type)) {
            case FIPS_P_256_ELLIPTIC_CURVE:
                return 0;
            default:
                return 1;
        }
    }
}
