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
import org.openremote.model.util.EnumUtil;
import org.openremote.model.value.ValueType;
import org.openremote.model.util.ValueUtil;

import java.util.*;

import static org.openremote.agent.protocol.velbus.AbstractVelbusProtocol.LOG;

public class ThermostatProcessor extends FeatureProcessor {

    public enum TemperatureMode {
        CURRENT(-1, -1, null ,null),
        COOL_COMFORT(0xC0, 7, VelbusPacket.OutboundCommand.TEMP_MODE1_COOL, VelbusPacket.OutboundCommand.TEMP_MODE2_COMFORT),
        COOL_DAY(0xA0, 8, VelbusPacket.OutboundCommand.TEMP_MODE1_COOL, VelbusPacket.OutboundCommand.TEMP_MODE2_DAY),
        COOL_NIGHT(0x90, 9, VelbusPacket.OutboundCommand.TEMP_MODE1_COOL, VelbusPacket.OutboundCommand.TEMP_MODE2_NIGHT),
        COOL_SAFE(0x80, 10, VelbusPacket.OutboundCommand.TEMP_MODE1_COOL, VelbusPacket.OutboundCommand.TEMP_MODE2_SAFE),
        HEAT_COMFORT(0x40, 1, VelbusPacket.OutboundCommand.TEMP_MODE1_HEAT, VelbusPacket.OutboundCommand.TEMP_MODE2_COMFORT),
        HEAT_DAY(0x20, 2, VelbusPacket.OutboundCommand.TEMP_MODE1_HEAT, VelbusPacket.OutboundCommand.TEMP_MODE2_DAY),
        HEAT_NIGHT(0x10, 3, VelbusPacket.OutboundCommand.TEMP_MODE1_HEAT, VelbusPacket.OutboundCommand.TEMP_MODE2_NIGHT),
        HEAT_SAFE(0x00, 4, VelbusPacket.OutboundCommand.TEMP_MODE1_HEAT, VelbusPacket.OutboundCommand.TEMP_MODE2_SAFE);

        private static final TemperatureMode[] values = values();
        private final int code;
        private final int pointerIndex;
        private final VelbusPacket.OutboundCommand mode1Command;
        private final VelbusPacket.OutboundCommand mode2Command;

        TemperatureMode(int code, int pointerIndex, VelbusPacket.OutboundCommand mode1Command, VelbusPacket.OutboundCommand mode2Command) {
            this.code = code;
            this.pointerIndex = pointerIndex;
            this.mode1Command = mode1Command;
            this.mode2Command = mode2Command;
        }

        public int getPointerIndex() {
            return this.pointerIndex;
        }

        public int getCode() {
            return this.code;
        }

        public VelbusPacket.OutboundCommand getMode1Command() {
            return mode1Command;
        }

        public VelbusPacket.OutboundCommand getMode2Command() {
            return mode2Command;
        }

        public static Optional<TemperatureMode> fromCode(int integer) {
            for (TemperatureMode value : values) {
                if (value.getCode() == integer) {
                    return Optional.of(value);
                }
            }
            return Optional.empty();
        }
    }

    public enum TemperatureState {
        DISABLED(0x06),
        MANUAL(0x02),
        TIMER(0x04),
        NORMAL(0x00);

        private static final TemperatureState[] values = values();
        private final int code;

        TemperatureState(int code) {
            this.code = code;
        }

        public int getCode() {
            return this.code;
        }

        public static Optional<TemperatureState> fromCode(int integer) {
            for (TemperatureState value : values) {
                if (value.getCode() == integer) {
                    return Optional.of(value);
                }
            }
            return Optional.empty();
        }
    }

    protected static final List<PropertyDescriptor> THERMOSTAT_PROPERTIES = Arrays.asList(
        new PropertyDescriptor("heater", "Heater", "HEATER", ValueType.TEXT, true),
        new PropertyDescriptor("boost", "Boost", "BOOST", ValueType.TEXT, true),
        new PropertyDescriptor("pump", "Pump", "PUMP", ValueType.TEXT, true),
        new PropertyDescriptor("cooler", "Cooler", "COOLER", ValueType.TEXT, true),
        new PropertyDescriptor("tempAlarm1", "Temp Alarm 1", "TEMP_ALARM1", ValueType.TEXT, true),
        new PropertyDescriptor("tempAlarm2", "Temp Alarm 2", "TEMP_ALARM2", ValueType.TEXT, true),
        new PropertyDescriptor("tempAlarm3", "Temp Alarm 3", "TEMP_ALARM3", ValueType.TEXT, true),
        new PropertyDescriptor("tempAlarm4", "Temp Alarm 4", "TEMP_ALARM4", ValueType.TEXT, true),
        new PropertyDescriptor("tempState", "Thermostat State", "TEMP_STATE", ValueType.TEXT),
        new PropertyDescriptor("tempStateDisable", "Thermostat Disable (s)", "TEMP_STATE_DISABLE_SECONDS", ValueType.POSITIVE_INTEGER),
        // Get current mode or set mode until next program step
        new PropertyDescriptor("tempMode", "Thermostat Mode", "TEMP_MODE", ValueType.TEXT),
        // MINS VALUE: 0 = Until next program step, -1 = Permanent, 1-65279 = for N mins
        new PropertyDescriptor("coolComfortMins", "Thermostat Cool Comfort (mins)", "TEMP_MODE_COOL_COMFORT_MINS", ValueType.POSITIVE_INTEGER),
        new PropertyDescriptor("coolDayMins", "Cool Day (mins)", "TEMP_MODE_COOL_DAY_MINS", ValueType.POSITIVE_INTEGER),
        new PropertyDescriptor("coolNightMins", "Cool Night (mins)", "TEMP_MODE_COOL_NIGHT_MINS", ValueType.POSITIVE_INTEGER),
        new PropertyDescriptor("coolSafeMins", "Cool Safe (mins)", "TEMP_MODE_COOL_SAFE_MINS", ValueType.POSITIVE_INTEGER),
        new PropertyDescriptor("heatComfortMins", "Heat Comfort (mins)", "TEMP_MODE_HEAT_COMFORT_MINS", ValueType.POSITIVE_INTEGER),
        new PropertyDescriptor("heatDayMins", "Heat Day (mins)", "TEMP_MODE_HEAT_DAY_MINS", ValueType.POSITIVE_INTEGER),
        new PropertyDescriptor("heatNightMins", "Heat Night (mins)", "TEMP_MODE_HEAT_NIGHT_MINS", ValueType.POSITIVE_INTEGER),
        new PropertyDescriptor("heatSafeMins", "Heat Safe (mins)", "TEMP_MODE_HEAT_SAFE_MINS", ValueType.POSITIVE_INTEGER),
        new PropertyDescriptor("tempTargetCurrent", "Temp Target Current", "TEMP_TARGET_CURRENT", ValueType.NUMBER),
        new PropertyDescriptor("tempTargetCoolComfort", "Temp Target Cool Comfort", "TEMP_TARGET_COOL_COMFORT", ValueType.NUMBER),
        new PropertyDescriptor("tempTargetCoolDay", "Temp Target Cool Day", "TEMP_TARGET_COOL_DAY", ValueType.NUMBER),
        new PropertyDescriptor("tempTargetCoolNight", "Temp Target Cool Night", "TEMP_TARGET_COOL_NIGHT", ValueType.NUMBER),
        new PropertyDescriptor("tempTargetCoolSafe", "Temp Target Cool Safe", "TEMP_TARGET_COOL_SAFE", ValueType.NUMBER),
        new PropertyDescriptor("tempTargetHeatComfort", "Temp Target Heat Comfort", "TEMP_TARGET_HEAT_COMFORT", ValueType.NUMBER),
        new PropertyDescriptor("tempTargetHeatDay", "Temp Target Heat Day", "TEMP_TARGET_HEAT_DAY", ValueType.NUMBER),
        new PropertyDescriptor("tempTargetHeatNight", "Temp Target Heat Night", "TEMP_TARGET_HEAT_NIGHT", ValueType.NUMBER),
        new PropertyDescriptor("tempTargetHeatSafe", "Temp Target Heat Safe", "TEMP_TARGET_HEAT_SAFE", ValueType.NUMBER)
    );

    @Override
    public List<PropertyDescriptor> getPropertyDescriptors(VelbusDeviceType deviceType) {
        return THERMOSTAT_PROPERTIES;
    }

    @Override
    public List<VelbusPacket> getStatusRequestPackets(VelbusDevice device) {
       return Collections.singletonList(
           new VelbusPacket(device.getBaseAddress(), VelbusPacket.OutboundCommand.SENSOR_SETTINGS.getCode(), (byte)0x00)
       );
    }

    @Override
    public List<VelbusPacket> getPropertyWritePackets(VelbusDevice device, String property, Object value) {

        if (property.startsWith("TEMP_TARGET_")) {
            String modeStr = property.substring("TEMP_TARGET_".length());
            return EnumUtil
                .enumFromString(TemperatureMode.class, modeStr)
                .map(mode -> ValueUtil.getDoubleCoerced(value)
                    .map(newTemp -> Math.round(newTemp*2d))
                    .map(newTemp -> {
                        int busValue = Math.toIntExact(newTemp);
                        if (busValue >= -110 && busValue <= 125) {
                            return Collections.singletonList(
                                new VelbusPacket(device.getBaseAddress(), VelbusPacket.OutboundCommand.TEMP_SET.getCode(), VelbusPacket.PacketPriority.LOW, (byte)mode.getPointerIndex(), (byte)busValue)
                            );
                        }
                        return null;
                    })
                    .orElse(null))
                .orElse(null);

        } else if("TEMP_MODE".equals(property)) {
            return EnumUtil
                .enumFromValue(TemperatureMode.class, value)
                .map(mode -> getTempModePackets(device, mode, 0xFF00)) // Do this as a program step
                .orElse(null);
        }  else if(property.startsWith("TEMP_MODE_") && property.endsWith("_MINS")) {
            return ValueUtil
                .getIntegerCoerced(value)
                .map(durationMinutes -> {
                    String modeStr = property.substring("TEMP_MODE_".length(), property.length() - "_MINS".length());
                    return EnumUtil
                        .enumFromString(TemperatureMode.class, modeStr)
                        .map(mode -> getTempModePackets(device, mode, durationMinutes))
                        .orElse(null);
                })
                .orElse(null);
        } else if ("TEMP_STATE".equals(property)) {
            return EnumUtil
                .enumFromValue(TemperatureState.class, value)
                .map(state -> state == TemperatureState.TIMER ? null : state) // Cannot set timer mode this way
                .map(state -> getTempStatePackets(device, state, -1))
                .orElse(null);
        } else if ("TEMP_STATE_DISABLE_SECONDS".equals(property)) {
            return ValueUtil
                .getIntegerCoerced(value)
                .map(durationSeconds -> getTempStatePackets(device, TemperatureState.DISABLED, durationSeconds))
                .orElse(null);
        }

        return null;
    }

    @Override
    public boolean processReceivedPacket(VelbusDevice device, VelbusPacket packet) {
        VelbusPacket.InboundCommand packetCommand = VelbusPacket.InboundCommand.fromCode(packet.getCommand());

        switch (packetCommand) {
            case PUSH_BUTTON_STATUS:
                if (isPacketForThermostat(device, packet.getAddress())) {

                    for (int i=0; i<2; i++) {

                        InputProcessor.ChannelState state = i == 0
                            ? InputProcessor.ChannelState.PRESSED
                            : InputProcessor.ChannelState.RELEASED;

                        byte stateByte = packet.getByte(i+1);

                        if ((stateByte & 0x01) == 0x01) {
                            device.setProperty("HEATER", state);
                        }
                        if ((stateByte & 0x02) == 0x02) {
                            device.setProperty("BOOST", state);
                        }
                        if ((stateByte & 0x04) == 0x04) {
                            device.setProperty("PUMP", state);
                        }
                        if ((stateByte & 0x08) == 0x08) {
                            device.setProperty("COOLER", state);
                        }
                        if ((stateByte & 0x10) == 0x10) {
                            device.setProperty("TEMP_ALARM1", state);
                        }
                        if ((stateByte & 0x20) == 0x20) {
                            device.setProperty("TEMP_ALARM2", state);
                        }
                        if ((stateByte & 0x40) == 0x40) {
                            device.setProperty("TEMP_ALARM3", state);
                        }
                        if ((stateByte & 0x80) == 0x80) {
                            device.setProperty("TEMP_ALARM4", state);
                        }
                    }

                    return true;
                }
                return false;
            case SENSOR_STATUS:
                // Extract TemperatureState
                TemperatureState temperatureState = TemperatureState.fromCode(packet.getByte(1) & 0x06).orElse(TemperatureState.NORMAL);
                device.setProperty("TEMP_STATE",  temperatureState);

                // Extract mode
                TemperatureMode mode = TemperatureMode.fromCode(packet.getByte(1) & 0xF0).orElse(TemperatureMode.HEAT_SAFE);
                device.setProperty("TEMP_MODE",  mode);

                // Extract button states
                device.setProperty("HEATER", (packet.getByte(3) & 0x01) == 0x01 ? InputProcessor.ChannelState.PRESSED : InputProcessor.ChannelState.RELEASED);
                device.setProperty("BOOST", (packet.getByte(3) & 0x02) == 0x02 ? InputProcessor.ChannelState.PRESSED : InputProcessor.ChannelState.RELEASED);
                device.setProperty("PUMP", (packet.getByte(3) & 0x04) == 0x04 ? InputProcessor.ChannelState.PRESSED : InputProcessor.ChannelState.RELEASED);
                device.setProperty("COOLER", (packet.getByte(3) & 0x08) == 0x08 ? InputProcessor.ChannelState.PRESSED : InputProcessor.ChannelState.RELEASED);

                device.setProperty("TEMP_CURRENT", (double) packet.getByte(4) / 2);
                device.setProperty("TEMP_TARGET_CURRENT", (double) packet.getByte(5) / 2);

                if (device.getDeviceType() == VelbusDeviceType.VMB1TS) {
                    device.setProperty("TEMP_ALARM1", (packet.getByte(3) & 0x20) == 0x20 ? InputProcessor.ChannelState.PRESSED : InputProcessor.ChannelState.RELEASED);
                    device.setProperty("TEMP_ALARM2", (packet.getByte(3) & 0x40) == 0x40 ? InputProcessor.ChannelState.PRESSED : InputProcessor.ChannelState.RELEASED);
                } else {
                    device.setProperty("TEMP_ALARM1", (packet.getByte(3) & 0x10) == 0x10 ? InputProcessor.ChannelState.PRESSED : InputProcessor.ChannelState.RELEASED);
                    device.setProperty("TEMP_ALARM2", (packet.getByte(3) & 0x20) == 0x20 ? InputProcessor.ChannelState.PRESSED : InputProcessor.ChannelState.RELEASED);
                    device.setProperty("TEMP_ALARM3", (packet.getByte(3) & 0x40) == 0x40 ? InputProcessor.ChannelState.PRESSED : InputProcessor.ChannelState.RELEASED);
                    device.setProperty("TEMP_ALARM4", (packet.getByte(3) & 0x80) == 0x80 ? InputProcessor.ChannelState.PRESSED : InputProcessor.ChannelState.RELEASED);
                }
                return true;
            case TEMP_SETTINGS1:
                device.setProperty("TEMP_TARGET_CURRENT",  (double)packet.getByte(1) / 2);
                device.setProperty("TEMP_TARGET_HEAT_COMFORT",  (double)packet.getByte(2) / 2);
                device.setProperty("TEMP_TARGET_HEAT_DAY",  (double)packet.getByte(3) / 2);
                device.setProperty("TEMP_TARGET_HEAT_NIGHT",  (double)packet.getByte(4) / 2);
                device.setProperty("TEMP_TARGET_HEAT_SAFE",  (double)packet.getByte(5) / 2);
                return true;
            case TEMP_SETTINGS2:
                device.setProperty("TEMP_TARGET_COOL_COMFORT",  (double)packet.getByte(1) / 2);
                device.setProperty("TEMP_TARGET_COOL_DAY",  (double)packet.getByte(2) / 2);
                device.setProperty("TEMP_TARGET_COOL_NIGHT",  (double)packet.getByte(3) / 2);
                device.setProperty("TEMP_TARGET_COOL_SAFE",  (double)packet.getByte(4) / 2);
                return true;
            default:
                return false;
        }
    }

    protected static boolean isPacketForThermostat(VelbusDevice device, int packetAddress) {
        int addressIndex = device.getAddressIndex(packetAddress);
        int thermoIndex = getThermostatAddressIndex(device);
        return addressIndex == thermoIndex;
    }

    protected static int getThermostatAddressIndex(VelbusDevice device) {
        if (device.getDeviceType() == VelbusDeviceType.VMBGPO || device.getDeviceType() == VelbusDeviceType.VMBGPOD) {
            return 4;
        } else if (device.getDeviceType() == VelbusDeviceType.VMB1TS) {
            return 0;
        } else {
            return 1;
        }
    }

    protected static Optional<Integer> getThermostatChannelNumber(VelbusDevice device) {
        int thermoIndex = getThermostatAddressIndex(device);
        return Optional.ofNullable(thermoIndex > 0 ? (thermoIndex * 8) + 1 : null);
    }

    protected static boolean isThermostatEnabled(VelbusDevice device) {
        int thermoIndex = getThermostatAddressIndex(device);
        int thermostatAddress = device.getAddress(thermoIndex);
        return thermostatAddress > 0 && thermostatAddress < 255;
    }

    protected static List<VelbusPacket> getTempModePackets(VelbusDevice device, TemperatureMode mode, int durationMinutes) {

        // Check thermostat is supported
        Optional<Integer> thermostatChannel = getThermostatChannelNumber(device);

        if (!thermostatChannel.isPresent()) {
            LOG.info("Thermostat mode is not supported on this device");
            return null;
        }

        durationMinutes = Math.min(durationMinutes, 0xFF00); // Max minutes = 65279 but 65280 means program step
        if (durationMinutes < 0) {
            durationMinutes = 0xFFFF; // Permanent change
        }


        List<VelbusPacket> packets = new ArrayList<>();

        // If thermostat is disabled we first need to enable it again
        if (device.getPropertyValue("TEMP_STATE") == TemperatureState.DISABLED) {
            packets.addAll(InputProcessor.getLockStatePackets(device, thermostatChannel.get(), 0));
        }

        packets.add(new VelbusPacket(device.getBaseAddress(), mode.getMode1Command().getCode(), VelbusPacket.PacketPriority.LOW, (byte)0x00));
        packets.add(new VelbusPacket(device.getBaseAddress(), mode.getMode2Command().getCode(), VelbusPacket.PacketPriority.LOW, (byte)(durationMinutes >> 8), (byte)durationMinutes));

        return packets;
    }

    protected static List<VelbusPacket> getTempStatePackets(VelbusDevice device, TemperatureState state, int durationSeconds) {
        // Action to perform depends on current value of the temp state
        // Normal -> Disabled: Send a lock command
        // Normal -> Manual: Send TEMP_MODE with no timer
        // Disabled -> Normal: Send an unlock command
        // Disabled -> Manual: ?
        // Timer -> Manual: send TEMP_MODE with no timer
        // Timer -> Disabled: ?
        // Timer -> Normal:
        // Manual -> Disabled: Send a lock command
        // Manual -> Normal: ?

        // Check thermostat is supported
        Optional<Integer> thermostatChannel = getThermostatChannelNumber(device);

        if (!thermostatChannel.isPresent()) {
            LOG.info("Thermostat state is not supported on this device");
            return null;
        }

        TemperatureState currentState = (TemperatureState)device.getPropertyValue("TEMP_STATE");

        if (state == currentState) {
            return null;
        }

        List<VelbusPacket> packets = new ArrayList<>();

        if (currentState == TemperatureState.DISABLED) {
            packets.addAll(InputProcessor.getLockStatePackets(device, thermostatChannel.get(), 0));
        }

        switch (state) {

            case DISABLED:
                // Need to lock the channel
                packets.addAll(InputProcessor.getLockStatePackets(device, thermostatChannel.get(), durationSeconds));
                break;
            case MANUAL:
                // Send TEMP_MODE with current MODE as permanent change
                TemperatureMode currentMode = (TemperatureMode)device.getPropertyValue("TEMP_MODE");
                List<VelbusPacket> tempModePackets = getTempModePackets(device, currentMode, -1);
                if (tempModePackets != null) {
                    packets.addAll(tempModePackets);
                }
                break;
            case NORMAL:
                // Send TEMP_MODE with current MODE as temp change
                currentMode = (TemperatureMode)device.getPropertyValue("TEMP_MODE");
                tempModePackets = getTempModePackets(device, currentMode, 0);
                if (tempModePackets != null) {
                    packets.addAll(tempModePackets);
                }
                break;
        }

        return packets;
    }
}
