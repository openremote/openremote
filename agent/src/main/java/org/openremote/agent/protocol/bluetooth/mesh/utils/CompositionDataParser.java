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

import java.util.Locale;

public class CompositionDataParser {

    public static String formatCompanyIdentifier(final int companyIdentifier, final boolean add0x) {
        return add0x ? "0x" + String.format(Locale.US, "%04X", companyIdentifier) : String.format(Locale.US, "%04X", companyIdentifier);
    }

    public static String formatProductIdentifier(final int productIdentifier, final boolean add0x) {
        return add0x ? "0x" + String.format(Locale.US, "%04X", productIdentifier) : String.format(Locale.US, "%04X", productIdentifier);
    }

    public static String formatVersionIdentifier(final int versionIdentifier, final boolean add0x) {
        return add0x ? "0x" + String.format(Locale.US, "%04X", versionIdentifier) : String.format(Locale.US, "%04X", versionIdentifier);
    }

    public static String formatReplayProtectionCount(final int replayProtectionCount, final boolean add0x) {
        return add0x ? "0x" + String.format(Locale.US, "%04X", replayProtectionCount) : String.format(Locale.US, "%04X", replayProtectionCount);
    }

    public static String formatFeatures(final int features, final boolean add0x) {
        return add0x ? "0x" + String.format(Locale.US, "%04X", features) : String.format(Locale.US, "%04X", features);
    }

    public static String formatModelIdentifier(final int modelId, final boolean add0x) {
        if (modelId < Short.MIN_VALUE || modelId > Short.MAX_VALUE) {
            return add0x ? "0x" + String.format(Locale.US, "%08X", modelId) : String.format(Locale.US, "%08X", modelId);
        } else {
            return add0x ? "0x" + String.format(Locale.US, "%04X", modelId) : String.format(Locale.US, "%04X", modelId);
        }
    }
}

