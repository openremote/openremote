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

import org.openremote.agent.protocol.velbus.VelbusPacket;
import org.openremote.model.value.ValueType;

import java.util.Arrays;
import java.util.List;

public class TemperatureProcessor extends FeatureProcessor {

    protected static final List<PropertyDescriptor> SUPPORTED_PROPERTIES = Arrays.asList(
        new PropertyDescriptor("tempCurrent", "Temp Current", "TEMP_CURRENT", ValueType.NUMBER, true),
        new PropertyDescriptor("tempMin", "Temp Min", "TEMP_MIN", ValueType.NUMBER, true),
        new PropertyDescriptor("tempMax", "Temp Max", "TEMP_MAX", ValueType.NUMBER, true)
    );

    @Override
    public List<PropertyDescriptor> getPropertyDescriptors(VelbusDeviceType deviceType) {
        return SUPPORTED_PROPERTIES;
    }

    @Override
    public List<VelbusPacket> getStatusRequestPackets(VelbusDevice device) {

        return Arrays.asList(
            new VelbusPacket(device.getBaseAddress(), VelbusPacket.OutboundCommand.MODULE_STATUS.getCode(), (byte) 0x00),
            new VelbusPacket(device.getBaseAddress(), VelbusPacket.OutboundCommand.SENSOR_READOUT.getCode(), (byte) 0x00)
        );
    }

    @Override
    public List<VelbusPacket> getPropertyWritePackets(VelbusDevice device, String property, Object value) {
        return null;
    }

    @Override
    public boolean processReceivedPacket(VelbusDevice device, VelbusPacket packet) {
        VelbusPacket.InboundCommand packetCommand = VelbusPacket.InboundCommand.fromCode(packet.getCommand());

        switch (packetCommand) {
            case CURRENT_TEMP_STATUS:
                processCurrentTemp(device, packet);
                return true;
        }

        return false;
    }

    protected static void processCurrentTemp(VelbusDevice device, VelbusPacket packet) {
        short currentValue = (short)(packet.getByte(1) << 8 | (short)packet.getByte(2) & 0xFF);
        short minValue = (short)(packet.getByte(3) << 8 | (short)packet.getByte(4) & 0xFF);
        short maxValue = (short)(packet.getByte(5) << 8 | (short)packet.getByte(6) & 0xFF);

        byte msb = (byte)(currentValue >> 15);
        if (msb > 0) {
            currentValue -= 1;
            currentValue = (short)~currentValue;
            currentValue*=-1;
        }
        msb = (byte)(minValue >> 15);
        if (msb > 0) {
            minValue -= 1;
            minValue = (short)~minValue;
            minValue*=-1;
        }
        msb = (byte)(maxValue >> 15);
        if (msb > 0) {
            maxValue -= 1;
            maxValue = (short)~maxValue;
            maxValue*=-1;
        }

        currentValue = (short)(currentValue >> 5);
        minValue = (short)(minValue >> 5);
        maxValue = (short)(maxValue >> 5);
        double current = ((double)Math.round(0.625 * currentValue)) / 10;
        double min = ((double)Math.round(0.625 * minValue)) / 10;
        double max = ((double)Math.round(0.625 * maxValue)) / 10;
        device.setProperty("TEMP_CURRENT",  current);
        device.setProperty("TEMP_MIN",  min);
        device.setProperty("TEMP_MAX",  max);
    }
}
