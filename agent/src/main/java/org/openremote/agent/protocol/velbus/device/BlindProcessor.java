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
import org.openremote.model.value.ValueDescriptor;
import org.openremote.model.value.ValueType;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.openremote.agent.protocol.velbus.VelbusPacket.OutboundCommand.*;
import static org.openremote.model.util.TextUtil.toProperCase;
import static org.openremote.model.util.TextUtil.toUpperCamelCase;

public class BlindProcessor extends OutputChannelProcessor {

    public enum ChannelState {
        UP,
        DOWN,
        HALT;

        public static Optional<ChannelState> fromValue(Object value) {
            if (value == null) {
                return Optional.of(HALT);
            }

            if (ValueUtil.isBoolean(value.getClass())) {
                return fromBoolean(ValueUtil.getBoolean(value).orElse(null));
            }

            return EnumUtil.enumFromValue(ChannelState.class, value);
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

    public enum ChannelSetting {
        NORMAL(0x00),
        INHIBITED(0x01),
        INHIBITED_DOWN(0x02),
        INHIBITED_UP(0x03),
        FORCED_DOWN(0x04),
        FORCED_UP(0x05),
        LOCKED(0x06);

        private final int code;

        ChannelSetting(int code) {
            this.code = code;
        }

        public int getCode() {
            return this.code;
        }

        public static ChannelSetting fromCode(int code) {
            for (ChannelSetting type : ChannelSetting.values()) {
                if (type.getCode() == code) {
                    return type;
                }
            }

            return NORMAL;
        }

        public static Optional<ChannelSetting> fromValue(Object value) {
            return EnumUtil.enumFromValue(ChannelSetting.class, value);
        }
    }

    protected final static List<Pair<String, ValueDescriptor<?>>> CHANNEL_PROPERTIES = Arrays.asList(
        // RW - ChannelState
        new Pair<>("", ValueType.TEXT),
        // R - ChannelSetting
        new Pair<>("_SETTING", ValueType.TEXT),
        // R - Read LED status for up
        new Pair<>("_LED_UP", ValueType.TEXT),
       // R - Read LED status for down
        new Pair<>("_LED_DOWN", ValueType.TEXT),
        // RW - True/False
        new Pair<>("_LOCKED", ValueType.BOOLEAN),
        // RW - True/False
        new Pair<>("_INHIBITED", ValueType.BOOLEAN),
        // W - Position 0-100% (0 = halt)
        new Pair<>("_POSITION", ValueType.NUMBER),
        // W - Up for specified time in seconds (0 = halt, -1 = indefinitely)
        new Pair<>("_UP", ValueType.NUMBER),
        // W - Down for specified time in seconds (0 = halt, -1 = indefinitely)
        new Pair<>("_DOWN", ValueType.NUMBER),
        // W - Forced up for specified time in seconds (0 = halt, -1 = indefinitely)
        new Pair<>("_FORCE_UP", ValueType.NUMBER),
        // W - Forced down for specified time in seconds (0 = cancel, -1 = indefinitely)
        new Pair<>("_FORCE_DOWN", ValueType.NUMBER),
        // W - Lock (force off) for specified time in seconds (0 = unlock, -1 = indefinitely)
        new Pair<>("_LOCK", ValueType.NUMBER),
        // W - Inhibit for specified time in seconds (0 = unlock, -1 = indefinitely)
        new Pair<>("_INHIBIT", ValueType.NUMBER),
        // W - Inhibit up for specified time in seconds (0 = un-inhibit, -1 = indefinitely)
        new Pair<>("_INHIBIT_UP", ValueType.NUMBER),
        // W - Inhibit down for specified time in seconds (0 = un-inhibit, -1 = indefinitely)
        new Pair<>("_INHIBIT_DOWN", ValueType.NUMBER)
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

    @SuppressWarnings("ConstantConditions")
    @Override
    public List<VelbusPacket> getPropertyWritePackets(VelbusDevice device, String property, Object value) {
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
                        case "_SETTING":
                            Optional<ChannelSetting> setting = ChannelSetting.fromValue(value);
                            if (!setting.isPresent()) {
                                return null;
                            }
                            switch (setting.get()) {
                                case NORMAL:
                                    return Stream.of(
                                        getPackets(device, channelNumber, INHIBIT_CANCEL, 0xFFFFFF, 0),
                                        getPackets(device, channelNumber, BLIND_FORCE_DOWN_CANCEL, 0xFFFFFF, 0),
                                        getPackets(device, channelNumber, BLIND_FORCE_UP_CANCEL, 0xFFFFFF, 0),
                                        getPackets(device, channelNumber, LOCK_CANCEL, 0xFFFFFF, 0)
                                    ).flatMap(List::stream).collect(Collectors.toList());
                                case INHIBITED:
                                    params[0] = 0xFFFFFF;
                                    command = INHIBIT;
                                    break;
                                case INHIBITED_DOWN:
                                    params[0] = 0xFFFFFF;
                                    command = BLIND_INHIBIT_DOWN;
                                    break;
                                case INHIBITED_UP:
                                    params[0] = 0xFFFFFF;
                                    command = BLIND_INHIBIT_UP;
                                    break;
                                case FORCED_DOWN:
                                    params[0] = 0xFFFFFF;
                                    command = BLIND_FORCE_DOWN;
                                    break;
                                case FORCED_UP:
                                    params[0] = 0xFFFFFF;
                                    command = BLIND_FORCE_UP;
                                    break;
                                case LOCKED:
                                    params[0] = 0xFFFFFF;
                                    command = LOCK;
                                    break;
                            }
                            break;
                        case "_LOCKED":
                            command = ValueUtil.getBoolean(value)
                                .map(locked -> {
                                    params[0] = 0xFFFFFF;
                                    return locked ? LOCK : LOCK_CANCEL;
                                })
                                .orElse(null);
                            break;
                        case "_INHIBITED":
                            command = ValueUtil.getBoolean(value)
                                .map(inhibited -> {
                                    params[0] = 0xFFFFFF;
                                    return inhibited ? INHIBIT : INHIBIT_CANCEL;
                                })
                                .orElse(null);
                            break;
                        case "_INHIBITED_UP":
                            command = ValueUtil.getBoolean(value)
                                .map(inhibited -> {
                                    params[0] = 0xFFFFFF;
                                    return inhibited ? BLIND_INHIBIT_UP : INHIBIT_CANCEL;
                                })
                                .orElse(null);
                            break;

                        case "_INHIBITED_DOWN":
                            command = ValueUtil.getBoolean(value)
                                .map(inhibited -> {
                                    params[0] = 0xFFFFFF;
                                    return inhibited ? BLIND_INHIBIT_DOWN : INHIBIT_CANCEL;
                                })
                                .orElse(null);
                            break;
                        case "_FORCED_DOWN":
                            command = ValueUtil.getBoolean(value)
                                .map(inhibited -> {
                                    params[0] = 0xFFFFFF;
                                    return inhibited ? BLIND_FORCE_DOWN : BLIND_FORCE_DOWN_CANCEL;
                                })
                                .orElse(null);
                            break;
                        case "_FORCED_UP":
                            command = ValueUtil.getBoolean(value)
                                .map(inhibited -> {
                                    params[0] = 0xFFFFFF;
                                    return inhibited ? BLIND_FORCE_UP : BLIND_FORCE_UP_CANCEL;
                                })
                                .orElse(null);
                            break;
                        case "_POSITION":
                            command = ValueUtil.getIntegerCoerced(value)
                                .map(position -> {
                                    params[1] = position;
                                    return position < 0 ? BLIND_HALT : BLIND_POSITION;
                                })
                                .orElse(null);
                            break;
                        case "_LOCK":
                            command = ValueUtil.getIntegerCoerced(value)
                                .map(duration -> {
                                    params[0] = duration;
                                    return duration == 0 ? BLIND_LOCK_CANCEL : BLIND_LOCK;
                                })
                                .orElse(null);
                            break;
                        case "_UP":
                            command = ValueUtil.getIntegerCoerced(value)
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
                            command = ValueUtil.getIntegerCoerced(value)
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
                            command = ValueUtil.getIntegerCoerced(value)
                                .map(duration -> {
                                    params[0] = duration;
                                    return duration == 0 ? BLIND_FORCE_UP_CANCEL : BLIND_FORCE_UP;
                                })
                                .orElse(null);
                            break;
                        case "_FORCE_DOWN":
                            command = ValueUtil.getIntegerCoerced(value)
                                .map(duration -> {
                                    params[0] = duration;
                                    return duration == 0 ? BLIND_FORCE_DOWN_CANCEL : BLIND_FORCE_DOWN;
                                })
                                .orElse(null);
                            break;
                        case "_INHIBIT":
                            command = ValueUtil.getIntegerCoerced(value)
                                .map(duration -> {
                                    params[0] = duration;
                                    return duration == 0 ? INHIBIT_CANCEL : INHIBIT;
                                })
                                .orElse(null);
                            break;
                        case "_INHIBIT_UP":
                            command = ValueUtil.getIntegerCoerced(value)
                                .map(duration -> {
                                    params[0] = duration;
                                    return duration == 0 ? INHIBIT_CANCEL : BLIND_INHIBIT_UP;
                                })
                                .orElse(null);
                            break;
                        case "_INHIBIT_DOWN":
                            command = ValueUtil.getIntegerCoerced(value)
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

        if (packetCommand == VelbusPacket.InboundCommand.BLIND_STATUS) {// Extract channel info
            int channelNumber = packet.getByte(1) & 0xFF;

            int blindPos = device.getDeviceType() == VelbusDeviceType.VMB2BLE ? packet.getByte(5) & 0xFF : 0;
            int stateValue = packet.getByte(3) & 0xFF;
            ChannelState state = stateValue == 0 ? ChannelState.HALT : stateValue == 1 ? ChannelState.UP : ChannelState.DOWN;
            LedState ledStateDown = LedState.fromCode(packet.getByte(4) & 0xFF);
            LedState ledStateUp = LedState.fromCode(packet.getByte(4) << 4 & 0xFF);
            ChannelSetting setting = ChannelSetting.fromCode(packet.getByte(6) & 0xFF);
            boolean locked = setting == ChannelSetting.LOCKED;
            boolean inhibited = setting == ChannelSetting.INHIBITED;
            boolean inhibitedUp = setting == ChannelSetting.INHIBITED_UP;
            boolean inhibitedDown = setting == ChannelSetting.INHIBITED_DOWN;

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
