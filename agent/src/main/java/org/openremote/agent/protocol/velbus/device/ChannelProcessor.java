/*
 * Copyright 2017, OpenRemote Inc.
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
package org.openremote.agent.protocol.velbus.device;

import org.openremote.model.util.Pair;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Abstract processor for channel related operations
 */
public abstract class ChannelProcessor extends FeatureProcessor {

    protected static final Pattern CHANNEL_REGEX = Pattern.compile("^CH(\\d+)(.*$|$)");

    protected ChannelProcessor() {}

    protected static int getMaxChannelNumber(VelbusDeviceType velbusDeviceType) {
        if (velbusDeviceType == VelbusDeviceType.VMBGPOD || velbusDeviceType == VelbusDeviceType.VMBGPO) {
            return 32;
        }

        if (velbusDeviceType == VelbusDeviceType.VMB7IN || velbusDeviceType == VelbusDeviceType.VMB1TS) {
            return 7;
        }

        if (velbusDeviceType == VelbusDeviceType.VMB4RYNO
            || velbusDeviceType == VelbusDeviceType.VMB4RYLD) {
            return 5;
        }

        if (velbusDeviceType == VelbusDeviceType.VMB1RY
            || velbusDeviceType == VelbusDeviceType.VMBDME
            || velbusDeviceType == VelbusDeviceType.VMBDMI
            || velbusDeviceType == VelbusDeviceType.VMBDMIR
            || velbusDeviceType == VelbusDeviceType.VMB1BL) {
            return 1;
        }

        if (velbusDeviceType == VelbusDeviceType.VMB4DC) {
            return 4;
        }

        if (velbusDeviceType == VelbusDeviceType.VMB2BLE) {
            return 2;
        }

        return 8;
    }

    protected static int getAddressForChannel(VelbusDevice device, int channelNumber) {
        if (channelNumber < 1 || channelNumber > getMaxChannelNumber(device.getDeviceType())) {
            return 0;
        }

        int addressIndex = Math.max(0, Math.min(3, (int)Math.floor((double)(channelNumber - 1) / 8)));
        return device.getAddress(addressIndex);
    }

    protected static boolean isChannelEnabled(VelbusDevice device, int channelNumber) {
        int channelAddress = getAddressForChannel(device, channelNumber);
        return channelAddress > 0 && channelAddress < 255;
    }

    protected static int getStartChannelNumber(VelbusDevice velbusDevice, int address) {
        return (velbusDevice.getAddressIndex(address) * 8) + 1;
    }

    protected Optional<Pair<Integer, String>> getChannelNumberAndPropertySuffix(VelbusDevice device, Pattern channelRegex, String property) {
        Matcher matcher = channelRegex.matcher(property);

        if (!matcher.matches()) {
            return Optional.empty();
        }

        // Check channel number is in range and enabled
        int channelNumber = Integer.parseInt(matcher.group(1));
        int address = ChannelProcessor.getAddressForChannel(device, channelNumber);

        if (address == 0 || address == 255) {
            return Optional.empty();
        }

        String propertySuffix = matcher.groupCount() == 2 ? matcher.group(2) : "";

        return Optional.of(new Pair<>(channelNumber, propertySuffix));
    }
}
