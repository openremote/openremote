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
import org.openremote.model.util.Pair;
import org.openremote.model.util.ValueUtil;
import org.openremote.model.value.ValueType;

import java.util.*;
import java.util.logging.Level;

import static org.openremote.agent.protocol.velbus.AbstractVelbusProtocol.LOG;

public class ProgramsProcessor extends ChannelProcessor {

    enum Program {
        NONE(0x00),
        SUMMER(0x01),
        WINTER(0x02),
        HOLIDAY(0x03);

        private final int code;

        Program(int code) {
            this.code = code;
        }

        public int getCode() {
            return this.code;
        }

        @Override
        public String toString() {
            return super.toString() + "(" + code + ")";
        }

        public static Program fromCode(int code) {
            for (Program type : Program.values()) {
                if (type.getCode() == code) {
                    return type;
                }
            }

            return NONE;
        }
    }

    protected static final String PROGRAM_STEPS_ENABLED_SUFFIX = "_PROGRAM_STEPS_ENABLED";
    protected static final String PROGRAM_STEPS_DISABLED_SECONDS_SUFFIX = "_PROGRAM_STEPS_DISABLED_SECONDS";
    protected static final List<PropertyDescriptor> STATIC_PROPERTIES = Arrays.asList(
        new PropertyDescriptor("allProgramStepsEnabled", "All Program Steps Enabled", "ALL" + PROGRAM_STEPS_ENABLED_SUFFIX, ValueType.BOOLEAN),
        new PropertyDescriptor("allProgramStepsDisableSeconds", "All Program Steps Disable (s)", "ALL" + PROGRAM_STEPS_DISABLED_SECONDS_SUFFIX, ValueType.NUMBER),
        new PropertyDescriptor("sunriseEnabled", "Sunrise Enabled", "SUNRISE_ENABLED", ValueType.BOOLEAN),
        new PropertyDescriptor("sunsetEnabled", "Sunset Enabled", "SUNSET_ENABLED", ValueType.BOOLEAN),
        new PropertyDescriptor("alarm1Enabled", "Alarm 1 Enabled", "ALARM1_ENABLED", ValueType.BOOLEAN),
        new PropertyDescriptor("alarm2Enabled", "Alarm 2 Enabled", "ALARM2_ENABLED", ValueType.BOOLEAN),
        new PropertyDescriptor("alarm1Master", "Alarm 1 Master", "ALARM1_MASTER", ValueType.BOOLEAN),
        new PropertyDescriptor("alarm2Master", "Alarm 2 Master", "ALARM2_MASTER", ValueType.BOOLEAN),
        new PropertyDescriptor("alarm1WakeTime", "Alarm 1 Wake Time", "ALARM1_WAKE_TIME", ValueType.TEXT),
        new PropertyDescriptor("alarm2WakeTime", "Alarm 2 Wake Time", "ALARM2_WAKE_TIME", ValueType.TEXT),
        new PropertyDescriptor("alarm1BedTime", "Alarm 1 Bed Time", "ALARM1_BED_TIME", ValueType.TEXT),
        new PropertyDescriptor("alarm2BedTime", "Alarm 2 Bed Time", "ALARM2_BED_TIME", ValueType.TEXT),
        new PropertyDescriptor("program", "Current Program", "Program", ValueType.TEXT)
    );


    @Override
    public List<PropertyDescriptor> getPropertyDescriptors(VelbusDeviceType deviceType) {
        int maxChannelNumber = ChannelProcessor.getMaxChannelNumber(deviceType);
        List<PropertyDescriptor> properties = new ArrayList<>(maxChannelNumber * 2 + STATIC_PROPERTIES.size());

        for (int i=1; i<=maxChannelNumber; i++) {
            properties.add(new PropertyDescriptor("ch" + i + "ProgramStepsEnabled", "CH" + i + " Program Steps Enabled", "CH" + i + PROGRAM_STEPS_ENABLED_SUFFIX, ValueType.BOOLEAN));
            properties.add(new PropertyDescriptor("ch" + i + "ProgramStepsDisableSeconds", "CH" + i + " Program Steps Disable (s)", "CH" + i + PROGRAM_STEPS_DISABLED_SECONDS_SUFFIX, ValueType.NUMBER));
        }

        properties.addAll(STATIC_PROPERTIES);
        return properties;
    }

    @Override
    public List<VelbusPacket> getStatusRequestPackets(VelbusDevice device) {
        // Add alarm memory read requests
        Integer alarm1 = getAlarmMemoryLocation(1, device.getDeviceType());
        Integer alarm2 = getAlarmMemoryLocation(2, device.getDeviceType());
        if (alarm1 != null && alarm2 != null) {
            return Arrays.asList(
                new VelbusPacket(device.getBaseAddress(), VelbusPacket.OutboundCommand.READ_MEMORY_BLOCK.getCode(), (byte)(alarm1 >> 8), alarm1.byteValue()),
                new VelbusPacket(device.getBaseAddress(), VelbusPacket.OutboundCommand.READ_MEMORY_BLOCK.getCode(), (byte)(alarm2 >> 8), alarm2.byteValue())
            );
        }
        return Collections.emptyList();
    }

    @Override
    public List<VelbusPacket> getPropertyWritePackets(VelbusDevice device, String property, Object value) {

        Optional<Pair<Integer, String>> channelNumberAndPropertySuffix = getChannelNumberAndPropertySuffix(device, CHANNEL_REGEX, property);

        if (channelNumberAndPropertySuffix.isPresent()) {
            switch (channelNumberAndPropertySuffix.get().value) {
                case PROGRAM_STEPS_ENABLED_SUFFIX:
                    return ValueUtil.getBooleanCoerced(value)
                        .map(enabled -> getProgramStepsPackets(device, channelNumberAndPropertySuffix.get().key, enabled, 0xFFFFF))
                        .orElse(null);
                case PROGRAM_STEPS_DISABLED_SECONDS_SUFFIX:
                    return ValueUtil.getIntegerCoerced(value)
                        .map(durationSeconds -> getProgramStepsPackets(device, channelNumberAndPropertySuffix.get().key, false, durationSeconds))
                        .orElse(null);
            }
            return null;
        }

        if (property.equals("ALL" + PROGRAM_STEPS_ENABLED_SUFFIX)) {
            return ValueUtil.getBooleanCoerced(value)
                .map(enabled -> getProgramStepsPackets(device, 0xFF, enabled, 0xFFFFF))
                .orElse(null);
        }

        if (property.equals("ALL" + PROGRAM_STEPS_DISABLED_SECONDS_SUFFIX)) {
            return ValueUtil.getIntegerCoerced(value)
                .map(durationSeconds -> getProgramStepsPackets(device, 0xFF, false, durationSeconds))
                .orElse(null);
        }

        if (property.equals("PROGRAM")) {
            return EnumUtil.enumFromValue(Program.class, value)
                .map(program ->
                    Collections.singletonList(
                        new VelbusPacket(device.getBaseAddress(), VelbusPacket.OutboundCommand.PROGRAM_SELECT.getCode(), VelbusPacket.PacketPriority.LOW, (byte)program.getCode())
                    )
                )
                .orElse(null);
        }

        if (property.equals("SUNRISE_ENABLED") || property.equals("SUNSET_ENABLED")) {
            return ValueUtil.getBooleanCoerced(value)
                .map(enabled -> {
                    boolean sunriseEnabled = device.getPropertyValue("SUNRISE_ENABLED") != null && ((Boolean)device.getPropertyValue("SUNRISE_ENABLED"));
                    boolean sunsetEnabled = device.getPropertyValue("SUNSET_ENABLED") != null && ((Boolean)device.getPropertyValue("SUNSET_ENABLED"));

                    if (property.equals("SUNRISE_ENABLED")) {
                        sunriseEnabled = enabled;
                    } else {
                        sunsetEnabled = enabled;
                    }

                    int sunriseSunsetByte = 0;
                    if (sunriseEnabled) {
                        sunriseSunsetByte += 1;
                    }
                    if (sunsetEnabled) {
                        sunriseSunsetByte += 2;
                    }

                    // Push new values into the device cache so they are instantly available
                    device.setProperty("SUNRISE_ENABLED", sunriseEnabled);
                    device.setProperty("SUNSET_ENABLED", sunsetEnabled);

                    return Collections.singletonList(
                      new VelbusPacket(device.getBaseAddress(), VelbusPacket.OutboundCommand.SET_SUNRISE_SUNSET.getCode(), VelbusPacket.PacketPriority.LOW, (byte)0xFF, (byte)sunriseSunsetByte)
                    );
                })
                .orElse(null);
        }

        if (property.startsWith("ALARM")) {
            int alarmNumber;
            try {
                alarmNumber = Integer.parseInt(property.substring(5, 6));
            } catch (NumberFormatException e) {
                LOG.log(Level.INFO, "Alarm number must be number 1 or 2");
                return null;
            }

            String alarmProperty = property.substring(7);

            switch (alarmProperty) {
                case "ENABLED":
                case "WAKE_TIME":
                case "BED_TIME":
                    String alarmEnabledProperty = "ALARM" + alarmNumber + "_ENABLED";
                    String alarmWakeTimeProperty = "ALARM" + alarmNumber + "_WAKE_TIME";
                    String alarmBedTimeProperty = "ALARM" + alarmNumber + "_BED_TIME";
                    String alarmMasterProperty = "ALARM" + alarmNumber + "_MASTER";

                    if (!device.hasPropertyValue(alarmEnabledProperty)
                        || !device.hasPropertyValue(alarmWakeTimeProperty)
                        || !device.hasPropertyValue(alarmBedTimeProperty)
                        || !device.hasPropertyValue(alarmMasterProperty)
                    ) {
                        LOG.info("Device cache doesn't contain alarm enabled, type and/or time properties");
                        return null;
                    }

                    boolean enabled = Optional.ofNullable((Boolean)device.getPropertyValue(alarmEnabledProperty)).orElse(false);
                    String wakeTime = ((String)device.getPropertyValue(alarmWakeTimeProperty));
                    String bedTime = ((String)device.getPropertyValue(alarmBedTimeProperty));
                    boolean isGlobal = Optional.ofNullable((Boolean)device.getPropertyValue(alarmMasterProperty)).orElse(false);

                    if ("ENABLED".equals(alarmProperty)) {
                        Optional<Boolean> setEnabled = ValueUtil.getBooleanCoerced(value);
                        if (!setEnabled.isPresent()) {
                            return null;
                        }

                        enabled = setEnabled.get();
                    } else if ("WAKE_TIME".equals(alarmProperty)) {
                        if (value == null) {
                            return null;
                        }

                        wakeTime = value.toString();
                    } else if ("BED_TIME".equals(alarmProperty)) {
                        if (value == null) {
                            return null;
                        }

                        bedTime = value.toString();
                    }

                    String[] wakeTimeValues = wakeTime.split(":");
                    String[] bedTimeValues = bedTime.split(":");

                    if (wakeTimeValues.length != 2 || bedTimeValues.length != 2) {
                        LOG.info("Time property values must be in the format HH:mm");
                        return null;
                    }

                    int wakeHours, wakeMins, bedHours, bedMins;

                    try {
                        wakeHours = Integer.parseInt(wakeTimeValues[0]);
                        wakeMins = Integer.parseInt(wakeTimeValues[1]);
                        bedHours = Integer.parseInt(bedTimeValues[0]);
                        bedMins = Integer.parseInt(bedTimeValues[1]);
                        if (wakeHours < 0 || wakeHours > 23 || wakeMins < 0 || wakeMins > 59
                            || bedHours < 0 || bedHours > 23 || bedMins < 0 || bedMins > 59) {
                            throw new NumberFormatException("Hours and/or minutes out of allowed range");
                        }
                    } catch (NumberFormatException e) {
                        LOG.log(Level.INFO, "Time property values must be in the format HH:mm", e);
                        return null;
                    }

                    // Update property values and also send memory read request to ensure update occurred
                    device.setProperty(alarmEnabledProperty, enabled);
                    device.setProperty(alarmWakeTimeProperty, wakeTime);
                    device.setProperty(alarmBedTimeProperty, bedTime);

                    device.velbusNetwork.scheduleTask(() -> {
                        List<VelbusPacket> packets = getStatusRequestPackets(device);
                        device.velbusNetwork.sendPackets(packets.toArray(new VelbusPacket[0]));
                    }, 500);

                    return Collections.singletonList(
                        new VelbusPacket(
                            isGlobal ? 0x00 : device.getBaseAddress(),
                            VelbusPacket.OutboundCommand.SET_ALARM.getCode(),
                            VelbusPacket.PacketPriority.LOW,
                            (byte)alarmNumber,
                            (byte)wakeHours,
                            (byte)wakeMins,
                            (byte)bedHours,
                            (byte)bedMins,
                            (byte)(enabled ? 1 : 0)
                        )
                    );
            }
        }

        return null;
    }

    @Override
    public boolean processReceivedPacket(VelbusDevice device, VelbusPacket packet) {

        VelbusPacket.InboundCommand packetCommand = VelbusPacket.InboundCommand.fromCode(packet.getCommand());

        switch (packetCommand) {
            case MODULE_STATUS:
                // Get start channel
                int startChannel = ChannelProcessor.getStartChannelNumber(device, packet.getAddress());
                int maxChannelNumber = ChannelProcessor.getMaxChannelNumber(device.getDeviceType());

                if (startChannel < 0 || startChannel > maxChannelNumber) {
                    return false;
                }

                int programByte;
                int alarmByte;

                // VMBMETEO and VMB4AN handle this differently to other devices
                if (device.getDeviceType() == VelbusDeviceType.VMBMETEO || device.getDeviceType() == VelbusDeviceType.VMB4AN) {
                    programByte = packet.getByte(3) & 0xFF;
                    alarmByte = packet.getByte(4) & 0xFF;
                } else {
                    programByte = packet.getByte(5) & 0xFF;
                    alarmByte = packet.getByte(6) & 0xFF;
                }

                for (int i=startChannel; i<startChannel+8; i++) {
                    boolean programEnabled = (programByte & 0x01) == 0;
                    programByte = programByte >>> 1;

                    device.setProperty("CH" + i + PROGRAM_STEPS_ENABLED_SUFFIX, programEnabled);
                }

                boolean allEnabled = true;

                for (int i=1; i<=maxChannelNumber; i++) {
                    boolean isChannelEnabled = Optional.ofNullable((Boolean)device.getPropertyValue("CH" + i + PROGRAM_STEPS_ENABLED_SUFFIX)).orElse(true);
                    if (!isChannelEnabled) {
                        allEnabled = false;
                        break;
                    }
                }
                device.setProperty("ALL" + PROGRAM_STEPS_ENABLED_SUFFIX, allEnabled);

                if (startChannel == 1) {
                    device.setProperty("PROGRAM", Program.fromCode(alarmByte & 0x03));
                    device.setProperty("ALARM1_ENABLED", (alarmByte & 0x04) == 0x04);
                    device.setProperty("ALARM2_ENABLED", (alarmByte & 0x10) == 0x10);
                    device.setProperty("ALARM1_MASTER", (alarmByte & 0x08) == 0x08);
                    device.setProperty("ALARM2_MASTER", (alarmByte & 0x20) == 0x20);
                    device.setProperty("SUNRISE_ENABLED", (alarmByte & 0x40) == 0x40);
                    device.setProperty("SUNSET_ENABLED", (alarmByte & 0x80) == 0x80);
                }

                // Note this packet command is used by several processors so don't return true
                packet.setHandled(true);
                return false;
            case MEMORY_BLOCK_DUMP:
                Integer alarm1 = getAlarmMemoryLocation(1, device.getDeviceType());
                Integer alarm2 = getAlarmMemoryLocation(2, device.getDeviceType());
                int memoryLocation = ((packet.getByte(1) & 0xFF) << 8) | (packet.getByte(2) & 0xFF);
                int alarmNumber = 0;

                if (alarm1 != null && alarm1 == memoryLocation) {
                    alarmNumber = 1;
                } else if (alarm2 != null && alarm2 == memoryLocation) {
                    alarmNumber = 2;
                }

                if (alarmNumber == 0) {
                    return false;
                }

                int wakeHours = packet.getByte(3) & 0xFF;
                int wakeMins = packet.getByte(4) & 0xFF;
                int bedHours = packet.getByte(5) & 0xFF;
                int bedMins = packet.getByte(6) & 0xFF;
                String wakeTime = String.format("%02d:%02d", wakeHours, wakeMins);
                String bedTime = String.format("%02d:%02d", bedHours, bedMins);
                device.setProperty("ALARM" + alarmNumber + "_WAKE_TIME", wakeTime);
                device.setProperty("ALARM" + alarmNumber + "_BED_TIME", bedTime);
                return true;
        }

        return false;
    }

    protected static List<VelbusPacket> getProgramStepsPackets(VelbusDevice device, int channelNumber, boolean enabled, int durationSeconds) {
        byte[] packetBytes = enabled ? new byte[1] : new byte[4];
        packetBytes[0] = (byte)channelNumber;

        if (!enabled) {
            durationSeconds = Math.min(durationSeconds, 0xFFFFFE);
            int duration = durationSeconds > 0 ? durationSeconds : 0xFFFFFF; // FFFFFF locks it permanently
            packetBytes[1] = (byte)(duration >> 16);
            packetBytes[2] = (byte)(duration >> 8);
            packetBytes[3] = (byte)(duration);
        }

        VelbusPacket.OutboundCommand command = enabled ? VelbusPacket.OutboundCommand.PROGRAM_STEPS_ENABLE : VelbusPacket.OutboundCommand.PROGRAM_STEPS_DISABLE;

        return Collections.singletonList(
            new VelbusPacket(device.getBaseAddress(), command.getCode(), VelbusPacket.PacketPriority.LOW, packetBytes)
        );
    }

    protected static Integer getAlarmMemoryLocation(int alarmNumber, VelbusDeviceType deviceType) {
        Integer location = null;

        if (deviceType == VelbusDeviceType.VMBGPO || deviceType == VelbusDeviceType.VMBGPOD) {
            location = alarmNumber == 2 ? 0x0289 : 0x0285;
        } else if (deviceType == VelbusDeviceType.VMB6PBN || deviceType == VelbusDeviceType.VMB8PBU || deviceType == VelbusDeviceType.VMB7IN) {
            location = alarmNumber == 2 ? 0x0098 : 0x0094;
        } else if (deviceType == VelbusDeviceType.VMBGP1 ||
            deviceType == VelbusDeviceType.VMBGP2 ||
            deviceType == VelbusDeviceType.VMBGP4 ||
            deviceType == VelbusDeviceType.VMBGP4PIR) {
            location = alarmNumber == 2 ? 0x00A9 : 0x00A5;
        } else if (deviceType == VelbusDeviceType.VMBPIRC ||
            deviceType == VelbusDeviceType.VMBPIRO ||
            deviceType == VelbusDeviceType.VMBPIRM) {
            location = alarmNumber == 2 ? 0x0036 : 0x0032;
        } else if (deviceType == VelbusDeviceType.VMBMETEO) {
            location = alarmNumber == 2 ? 0x0088 : 0x0084;
        } else if (deviceType == VelbusDeviceType.VMB4AN) {
            location = alarmNumber == 2 ? 0x004A : 0x0046;
        }

        return location;
    }
}
