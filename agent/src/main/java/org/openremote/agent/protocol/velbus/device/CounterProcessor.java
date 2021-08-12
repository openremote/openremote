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
import org.openremote.model.attribute.AttributeExecuteStatus;
import org.openremote.model.value.ValueType;
import org.openremote.model.util.ValueUtil;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class CounterProcessor extends FeatureProcessor {

    public enum CounterUnits {
        RESERVED(0x00),
        LITRES(0x01),
        CUBICMETRES(0x02),
        KILOWATTS(0x03);

        private int code;

        CounterUnits(int code) {
            this.code = code;
        }



        public int getCode() {
            return this.code;
        }

        public static CounterUnits fromCode(int code) {
            for (CounterUnits type : CounterUnits.values()) {
                if (type.getCode() == code) {
                    return type;
                }
            }

            return RESERVED;
        }
    }

    public static final List<PropertyDescriptor> SUPPORTED_PROPERTIES = Arrays.asList(
        new PropertyDescriptor("counter1Enabled", "Counter 1 Enabled", "COUNTER1_ENABLED", ValueType.BOOLEAN, true),
        new PropertyDescriptor("counter2Enabled", "Counter 2 Enabled", "COUNTER2_ENABLED", ValueType.BOOLEAN, true),
        new PropertyDescriptor("counter3Enabled", "Counter 3 Enabled", "COUNTER3_ENABLED", ValueType.BOOLEAN, true),
        new PropertyDescriptor("counter4Enabled", "Counter 4 Enabled", "COUNTER4_ENABLED", ValueType.BOOLEAN, true),
        new PropertyDescriptor("counter1Units", "Counter 1 Units", "COUNTER1_UNITS", ValueType.TEXT, true),
        new PropertyDescriptor("counter2Units", "Counter 2 Units", "COUNTER2_UNITS", ValueType.TEXT, true),
        new PropertyDescriptor("counter3Units", "Counter 3 Units", "COUNTER3_UNITS", ValueType.TEXT, true),
        new PropertyDescriptor("counter4Units", "Counter 4 Units", "COUNTER4_UNITS", ValueType.TEXT, true),
        new PropertyDescriptor("counter1Instant", "Counter 1 Instant", "COUNTER1_INSTANT", ValueType.NUMBER, true),
        new PropertyDescriptor("counter2Instant", "Counter 2 Instant", "COUNTER2_INSTANT", ValueType.NUMBER, true),
        new PropertyDescriptor("counter3Instant", "Counter 3 Instant", "COUNTER3_INSTANT", ValueType.NUMBER, true),
        new PropertyDescriptor("counter4Instant", "Counter 4 Instant", "COUNTER4_INSTANT", ValueType.NUMBER, true),
        new PropertyDescriptor("counter1", "Counter 1", "COUNTER1", ValueType.NUMBER),
        new PropertyDescriptor("counter2", "Counter 2", "COUNTER2", ValueType.NUMBER),
        new PropertyDescriptor("counter3", "Counter 3", "COUNTER3", ValueType.NUMBER),
        new PropertyDescriptor("counter4", "Counter 4", "COUNTER4", ValueType.NUMBER)
    );

    @Override
    public List<PropertyDescriptor> getPropertyDescriptors(VelbusDeviceType deviceType) {
        return SUPPORTED_PROPERTIES;
    }

    @Override
    public List<VelbusPacket> getStatusRequestPackets(VelbusDevice device) {
        // Push default counter values as these aren't received unless the counter is enabled
        device.setProperty("COUNTER1_ENABLED", false);
        device.setProperty("COUNTER2_ENABLED", false);
        device.setProperty("COUNTER3_ENABLED", false);
        device.setProperty("COUNTER4_ENABLED", false);
        device.setProperty("COUNTER1", 0d);
        device.setProperty("COUNTER2", 0d);
        device.setProperty("COUNTER3", 0d);
        device.setProperty("COUNTER4", 0d);
        device.setProperty("COUNTER1_INSTANT", 0d);
        device.setProperty("COUNTER2_INSTANT", 0d);
        device.setProperty("COUNTER3_INSTANT", 0d);
        device.setProperty("COUNTER4_INSTANT", 0d);

        return Arrays.asList(
            new VelbusPacket(device.getBaseAddress(), VelbusPacket.OutboundCommand.COUNTER_STATUS.getCode(), (byte)0x0F, (byte)0x00),
            new VelbusPacket(device.getBaseAddress(), VelbusPacket.OutboundCommand.READ_MEMORY.getCode(), (byte)0x03, (byte)0xFE)
        );
    }

    @Override
    public List<VelbusPacket> getPropertyWritePackets(VelbusDevice device, String property, Object value) {

        if (!property.endsWith("_RESET")) {
            return null;
        }

        return ValueUtil.getValueCoerced(value, AttributeExecuteStatus.class).map(attributeExecuteStatus -> {

            if (attributeExecuteStatus != AttributeExecuteStatus.REQUEST_START) {
                return null;
            }
            Integer counter = null;

            if ("COUNTER1_RESET".equals(property)) {
                counter = 1;
            } else if ("COUNTER2_RESET".equals(property)) {
                counter = 2;
            } else if ("COUNTER3_RESET".equals(property)) {
                counter = 3;
            } else if ("COUNTER4_RESET".equals(property)) {
                counter = 4;
            }

            if (counter != null) {
                // Reset the counter
                return Collections.singletonList(
                    new VelbusPacket(device.getBaseAddress(), VelbusPacket.OutboundCommand.COUNTER_RESET.getCode(), (byte) (counter - 1))
                );
            }

            return null;
        }).orElse(null);
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean processReceivedPacket(VelbusDevice device, VelbusPacket packet) {

        VelbusPacket.InboundCommand packetCommand = VelbusPacket.InboundCommand.fromCode(packet.getCommand());

        switch (packetCommand) {
            case COUNTER_STATUS:
                int channelNumber = (packet.getByte(1) & 0x03) + 1;
                int pulses = (packet.getByte(1) >> 2) * 100;
                int counter = ((packet.getByte(2) & 0xFF) << 24) + ((packet.getByte(3) & 0xFF) << 16) + ((packet.getByte(4) & 0xFF) << 8) + ((packet.getByte(5) & 0xFF));
                int period = ((packet.getByte(6) & 0xFF) << 8) + ((packet.getByte(7) & 0xFF));

                CounterUnits units = (CounterUnits)device.getPropertyValue("COUNTER" + channelNumber + "_UNITS");
                boolean isElectric = units != null && units == CounterUnits.KILOWATTS;

                double value = ((double)counter / pulses);
                value = (double)Math.round(value*100) / 100;
                device.setProperty("COUNTER" + channelNumber, value);

                double instant = (double)1000 * 3600 * (isElectric ? 1000 : 1);
                instant = instant / (period * pulses);
                instant = (double)Math.round(instant * 100) / 100;
                device.setProperty("COUNTER" + channelNumber + "_ENABLED", true);
                device.setProperty("COUNTER" + channelNumber + "_INSTANT", instant);
                return true;
            case MEMORY_DATA:
                if ((packet.getByte(1) & 0xFF) == 0x03 && (packet.getByte(2) & 0xFF) == 0xFE) {

                    // Read Counter units
                    int counterUnits = packet.getByte(3);
                    CounterUnits[] counters = new CounterUnits[4];
                    counters[0] = Optional.ofNullable((Boolean)device.getPropertyValue("COUNTER1_ENABLED")).orElse(false) ? CounterUnits.fromCode(counterUnits & 0x03) : null;
                    counters[1] = Optional.ofNullable((Boolean)device.getPropertyValue("COUNTER2_ENABLED")).orElse(false) ? CounterUnits.fromCode((counterUnits & 0x0C) >> 2) : null;
                    counters[2] = Optional.ofNullable((Boolean)device.getPropertyValue("COUNTER3_ENABLED")).orElse(false) ? CounterUnits.fromCode((counterUnits & 0x30) >> 4) : null;
                    counters[3] = Optional.ofNullable((Boolean)device.getPropertyValue("COUNTER4_ENABLED")).orElse(false) ? CounterUnits.fromCode((counterUnits & 0xC0) >> 6) : null;

                    // Put values directly into cache no sensors will be linked to these values
                    device.setProperty("COUNTER1_UNITS", counters[0]);
                    device.setProperty("COUNTER2_UNITS", counters[1]);
                    device.setProperty("COUNTER3_UNITS", counters[2]);
                    device.setProperty("COUNTER4_UNITS", counters[3]);

                    // Try and update the counter instant values if any counter is a kilowatt counter
                    for (int i=0; i<4; i++) {
                        if (counters[i] == CounterUnits.KILOWATTS) {
                            double val = Optional.ofNullable((Double)device.getPropertyValue("COUNTER" + (i + 1) + "_INSTANT")).orElse(0d);
                            val = val * 1000;
                            device.setProperty("COUNTER" + (i + 1) + "_INSTANT", val);
                        }
                    }

                    return true;
                }
        }

        return false;
    }
}
