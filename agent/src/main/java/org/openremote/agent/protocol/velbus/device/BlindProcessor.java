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
import org.openremote.model.attribute.AttributeValueType;
import org.openremote.model.util.EnumUtil;
import org.openremote.model.util.Pair;
import org.openremote.model.value.Value;
import org.openremote.model.value.ValueType;
import org.openremote.model.value.Values;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.openremote.agent.protocol.velbus.VelbusPacket.OutboundCommand.*;
import static org.openremote.model.util.TextUtil.toProperCase;
import static org.openremote.model.util.TextUtil.toUpperCamelCase;

public class BlindProcessor extends OutputChannelProcessor {

    public enum ChannelState implements DevicePropertyValue<ChannelState> {
        UP,
        DOWN,
        HALT;

        @Override
        public Value toValue(ValueType valueType) {
            switch (valueType) {
                case BOOLEAN:
                    switch (this) {
                        case DOWN:
                            return Values.create(true);
                        case UP:
                            return Values.create(false);
                        case HALT:
                            return null;
                    }
                default:
                    return EnumUtil.enumToValue(this, valueType);
            }
        }

        @Override
        public ChannelState getPropertyValue() {
            return this;
        }

        public static Optional<ChannelState> fromValue(Value value) {
            if (value == null) {
                return Optional.of(HALT);
            }

            switch (value.getType()) {
                case BOOLEAN:
                    return fromBoolean(Values.getBoolean(value).orElse(null));
                default:
                    return EnumUtil.enumFromValue(ChannelState.class, value);
            }
        }

        public static Optional<ChannelState> fromBoolean(Boolean value) {
            if (value == null) {
                return Optional.of(HALT);
            } else if (value) {
                return Optional.of(DOWN);
            } else {
                return Optional.of(UP);
            }
        }
    }

    public enum ChannelSetting implements DevicePropertyValue<ChannelSetting> {
        NORMAL(0x00),
        INHIBITED(0x01),
        INHIBITED_DOWN(0x02),
        INHIBITED_UP(0x03),
        FORCED_DOWN(0x04),
        FORCED_UP(0x05),
        LOCKED(0x06);

        private int code;

        ChannelSetting(int code) {
            this.code = code;
        }

        public int getCode() {
            return this.code;
        }

        @Override
        public Value toValue(ValueType valueType) {
            return EnumUtil.enumToValue(this, valueType);
        }

        @Override
        public ChannelSetting getPropertyValue() {
            return this;
        }

        public static ChannelSetting fromCode(int code) {
            for (ChannelSetting type : ChannelSetting.values()) {
                if (type.getCode() == code) {
                    return type;
                }
            }

            return NORMAL;
        }
    }

    protected final static List<Pair<String, AttributeValueType>> CHANNEL_PROPERTIES = Arrays.asList(
        // RW - ChannelState
        new Pair<>("", AttributeValueType.STRING),
        // R - ChannelSetting
        new Pair<>("_SETTING", AttributeValueType.STRING),
        // R - Read LED status for up
        new Pair<>("_LED_UP", AttributeValueType.STRING),
       // R - Read LED status for down
        new Pair<>("_LED_DOWN", AttributeValueType.STRING),
        // RW - True/False
        new Pair<>("_LOCKED", AttributeValueType.BOOLEAN),
        // RW - True/False
        new Pair<>("_INHIBITED", AttributeValueType.BOOLEAN),
        // W - Position 0-100% (0 = halt)
        new Pair<>("_POSITION", AttributeValueType.NUMBER),
        // W - Up for specified time in seconds (0 = halt, -1 = indefinitely)
        new Pair<>("_UP", AttributeValueType.NUMBER),
        // W - Down for specified time in seconds (0 = halt, -1 = indefinitely)
        new Pair<>("_DOWN", AttributeValueType.NUMBER),
        // W - Forced up for specified time in seconds (0 = halt, -1 = indefinitely)
        new Pair<>("_FORCE_UP", AttributeValueType.NUMBER),
        // W - Forced down for specified time in seconds (0 = cancel, -1 = indefinitely)
        new Pair<>("_FORCE_DOWN", AttributeValueType.NUMBER),
        // W - Lock (force off) for specified time in seconds (0 = unlock, -1 = indefinitely)
        new Pair<>("_LOCK", AttributeValueType.NUMBER),
        // W - Inhibit for specified time in seconds (0 = unlock, -1 = indefinitely)
        new Pair<>("_INHIBIT", AttributeValueType.NUMBER),
        // W - Inhibit up for specified time in seconds (0 = un-inhibit, -1 = indefinitely)
        new Pair<>("_INHIBIT_UP", AttributeValueType.NUMBER),
        // W - Inhibit down for specified time in seconds (0 = un-inhibit, -1 = indefinitely)
        new Pair<>("_INHIBIT_DOWN", AttributeValueType.NUMBER)
    );

    @Override
    public List<VelbusPacket> getStatusRequestPackets(VelbusDevice device) {
        List<VelbusPacket> packets = new ArrayList<>();
        for (int i=1; i<= ChannelProcessor.getMaxChannelNumber(device.getDeviceType()); i++) {
            byte channelByte = (byte) Math.pow(2, i - 1);
            packets.add(new VelbusPacket(device.getBaseAddress(), VelbusPacket.OutboundCommand.MODULE_STATUS.getCode(), channelByte));
        }
        return packets;
    }

    @Override
    public List<PropertyDescriptor> getPropertyDescriptors(VelbusDeviceType deviceType) {

        return IntStream.rangeClosed(1, ChannelProcessor.getMaxChannelNumber(deviceType))
            .mapToObj(Integer::toString)
            .flatMap(chNumber ->
                CHANNEL_PROPERTIES.stream().map(propSuffix ->
                    new PropertyDescriptor(
                        "ch" + chNumber + toUpperCamelCase(propSuffix.key),
                        ("CH" + chNumber + " " + toProperCase(propSuffix.key, true)).trim(),
                        "CH" + chNumber + propSuffix,
                        propSuffix.value
                    )
                )
            )
            .collect(Collectors.toList());
    }

    @Override
    public List<VelbusPacket> getPropertyWritePackets(VelbusDevice device, String property, Value value) {
        return getChannelNumberAndPropertySuffix(device, CHANNEL_REGEX, property)
            .map(
                channelNumberAndPropertySuffix -> {
                    int channelNumber = channelNumberAndPropertySuffix.key;
                    VelbusPacket.OutboundCommand command = null;
                    // Duration, position
                    Integer[] params = new Integer[2];

                    switch (channelNumberAndPropertySuffix.value) {
                        case "":
                            command = ChannelState.fromValue(value)
                                .map(state -> {
                                    switch (state) {
                                        case UP:
                                            return BLIND_UP;
                                        case DOWN:
                                            return BLIND_DOWN;
                                        case HALT:
                                            return BLIND_HALT;
                                    }
                                    return null;
                                })
                                .orElse(null);
                            break;
                        case "LOCKED":
                            command = Values.getBoolean(value)
                                .map(locked -> {
                                    params[0] = 0xFFFFFF;
                                    return locked ? LOCK : LOCK_CANCEL;
                                })
                                .orElse(null);
                            break;
                        case "INHIBITED":
                            command = Values.getBoolean(value)
                                .map(inhibited -> {
                                    params[0] = 0xFFFFFF;
                                    return inhibited ? INHIBIT : INHIBIT_CANCEL;
                                })
                                .orElse(null);
                            break;
                        case "_POSITION":
                            command = Values.getIntegerCoerced(value)
                                .map(position -> {
                                    params[1] = position;
                                    return position < 0 ? BLIND_HALT : BLIND_POSITION;
                                })
                                .orElse(null);
                            break;
                        case "_LOCK":
                            command = Values.getIntegerCoerced(value)
                                .map(duration -> {
                                    params[0] = duration;
                                    return duration == 0 ? BLIND_LOCK_CANCEL : BLIND_LOCK;
                                })
                                .orElse(null);
                            break;
                        case "_UP":
                            command = Values.getIntegerCoerced(value)
                                .map(duration -> {
                                    params[0] = duration;
                                    if (duration == 0) {
                                        return BLIND_HALT;
                                    }

                                    return BLIND_UP;
                                })
                                .orElse(null);
                            break;
                        case "_DOWN":
                            command = Values.getIntegerCoerced(value)
                                .map(duration -> {
                                    params[0] = duration;
                                    if (duration == 0) {
                                        return BLIND_HALT;
                                    }

                                    return BLIND_DOWN;
                                })
                                .orElse(null);
                            break;
                        case "_FORCE_UP":
                            command = Values.getIntegerCoerced(value)
                                .map(duration -> {
                                    params[0] = duration;
                                    return duration == 0 ? BLIND_FORCE_UP_CANCEL : BLIND_FORCE_UP;
                                })
                                .orElse(null);
                            break;
                        case "_FORCE_DOWN":
                            command = Values.getIntegerCoerced(value)
                                .map(duration -> {
                                    params[0] = duration;
                                    return duration == 0 ? BLIND_FORCE_DOWN_CANCEL : BLIND_FORCE_DOWN;
                                })
                                .orElse(null);
                            break;
                        case "_INHIBIT":
                            command = Values.getIntegerCoerced(value)
                                .map(duration -> {
                                    params[0] = duration;
                                    return duration == 0 ? INHIBIT_CANCEL : INHIBIT;
                                })
                                .orElse(null);
                            break;
                        case "_INHIBIT_UP":
                            command = Values.getIntegerCoerced(value)
                                .map(duration -> {
                                    params[0] = duration;
                                    return duration == 0 ? INHIBIT_CANCEL : BLIND_INHIBIT_UP;
                                })
                                .orElse(null);
                            break;
                        case "_INHIBIT_DOWN":
                            command = Values.getIntegerCoerced(value)
                                .map(duration -> {
                                    params[0] = duration;
                                    return duration == 0 ? INHIBIT_CANCEL : BLIND_INHIBIT_DOWN;
                                })
                                .orElse(null);
                    }

                    if (command != null) {
                        return getPackets(
                            device,
                            channelNumber,
                            command,
                            params[0] == null ? 0 : params[0],
                            params[1] == null ? 0 : params[1]
                        );
                    }

                    return null;
                }
            )
            .orElse(null);
    }

    @Override
    public boolean processReceivedPacket(VelbusDevice device, VelbusPacket packet) {
        VelbusPacket.InboundCommand packetCommand = VelbusPacket.InboundCommand.fromCode(packet.getCommand());

        switch (packetCommand) {
            case BLIND_STATUS:

                // Extract channel info
                int channelNumber = packet.getByte(1) & 0xFF;

                IntDevicePropertyValue blindPos = device.getDeviceType() == VelbusDeviceType.VMB2BLE ? new IntDevicePropertyValue(packet.getByte(5) & 0xFF) : IntDevicePropertyValue.ZERO;
                int stateValue = packet.getByte(3) & 0xFF;
                ChannelState state = stateValue == 0 ? ChannelState.HALT : stateValue == 1 ? ChannelState.UP : ChannelState.DOWN;
                LedState ledStateDown = LedState.fromCode(packet.getByte(4) & 0xFF);
                LedState ledStateUp = LedState.fromCode(packet.getByte(4) << 4 & 0xFF);
                ChannelSetting setting = ChannelSetting.fromCode(packet.getByte(6) & 0xFF);
                BooleanDevicePropertyValue locked = setting == ChannelSetting.LOCKED ? BooleanDevicePropertyValue.TRUE : BooleanDevicePropertyValue.FALSE;
                BooleanDevicePropertyValue inhibited = setting == ChannelSetting.INHIBITED ? BooleanDevicePropertyValue.TRUE : BooleanDevicePropertyValue.FALSE;
                BooleanDevicePropertyValue inhibitedUp = setting == ChannelSetting.INHIBITED_UP ? BooleanDevicePropertyValue.TRUE : BooleanDevicePropertyValue.FALSE;
                BooleanDevicePropertyValue inhibitedDown = setting == ChannelSetting.INHIBITED_DOWN ? BooleanDevicePropertyValue.TRUE : BooleanDevicePropertyValue.FALSE;

                // Push to device cache
                device.setProperty("CH" + channelNumber, state);
                device.setProperty("CH" + channelNumber + "_POSITION", blindPos);
                device.setProperty("CH" + channelNumber + "_SETTING", setting);
                device.setProperty("CH" + channelNumber + "_LED_DOWN", ledStateDown);
                device.setProperty("CH" + channelNumber + "_LED_UP", ledStateUp);
                device.setProperty("CH" + channelNumber + "_LOCKED", locked);
                device.setProperty("CH" + channelNumber + "_INHIBITED", inhibited);
                device.setProperty("CH" + channelNumber + "_INHIBITED_UP", inhibitedUp);
                device.setProperty("CH" + channelNumber + "_INHIBITED_DOWN", inhibitedDown);
                return true;

                // Don't use push button status as it only provides relay on/off info
//            case PUSH_BUTTON_STATUS:
//                // Update each of the dimmer channels
//                int onByte = packet.getByte(1) & 0xFF;
//                int offByte = packet.getByte(2) & 0xFF;
//
//                for (int i = 1; i <= ChannelProcessor.getMaxChannelNumber(device); i++) {
//                    if ((onByte & 0x01) == 1) {
//                        device.setProperty("CH" + i, BlindProcessor.ChannelState.ON);
//                    } else if ((offByte & 0x01) == 1) {
//                        device.setProperty("CH" + i, BlindProcessor.ChannelState.OFF);
//                    }
//
//                    onByte = onByte >>> 1;
//                    offByte = offByte >>> 1;
//                }
//                return true;
        }

        return false;
    }

    protected static List<VelbusPacket> getPackets(VelbusDevice velbusDevice, int channelNumber, VelbusPacket.OutboundCommand command, int durationSeconds, int position) {
        byte[] packetBytes = null;

        switch (command) {
            case BLIND_POSITION:
                position = Math.min(100, Math.max(0, position));
                packetBytes = new byte[2];
                packetBytes[1] = (byte)position;
                break;
            case BLIND_UP:
            case BLIND_DOWN:
            case BLIND_FORCE_UP:
            case BLIND_FORCE_DOWN:
            case BLIND_INHIBIT_UP:
            case BLIND_INHIBIT_DOWN:
            case LEVEL_ON_TIMER:
            case BLIND_LOCK:
            case INHIBIT:
                durationSeconds = durationSeconds == -1 ? 0xFFFFFF : durationSeconds;
                durationSeconds = Math.min(0xFFFFFF, Math.max(0, durationSeconds));
                packetBytes = new byte[4];
                packetBytes[1] = (byte)(durationSeconds >> 16);
                packetBytes[2] = (byte)(durationSeconds >> 8);
                packetBytes[3] = (byte)(durationSeconds);
                break;
            case BLIND_HALT:
            case INHIBIT_CANCEL:
            case BLIND_FORCE_UP_CANCEL:
            case BLIND_FORCE_DOWN_CANCEL:
            case LOCK_CANCEL:
            case BLIND_LOCK_CANCEL:
                packetBytes = new byte[1];
                break;
        }

        if (packetBytes != null) {
            packetBytes[0] = (byte) Math.pow(2, channelNumber - 1);

            return Collections.singletonList(
                new VelbusPacket(velbusDevice.getBaseAddress(), command.getCode(), VelbusPacket.PacketPriority.HIGH, packetBytes)
            );
        }

        return null;
    }
}
