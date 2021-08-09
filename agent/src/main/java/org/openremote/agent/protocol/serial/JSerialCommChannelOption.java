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
package org.openremote.agent.protocol.serial;

import io.netty.channel.ChannelOption;
import org.openremote.agent.protocol.serial.JSerialCommChannelConfig.Paritybit;
import org.openremote.agent.protocol.serial.JSerialCommChannelConfig.Stopbits;

/**
 * Option for configuring a serial port connection.
 */
public final class JSerialCommChannelOption<T> extends ChannelOption<T> {

    public static final ChannelOption<Integer> BAUD_RATE = valueOf("BAUD_RATE");
    public static final ChannelOption<Stopbits> STOP_BITS = valueOf("STOP_BITS");
    public static final ChannelOption<Integer> DATA_BITS = valueOf("DATA_BITS");
    public static final ChannelOption<Paritybit> PARITY_BIT = valueOf("PARITY_BIT");
    public static final ChannelOption<Integer> WAIT_TIME = valueOf("WAIT_TIME");
    public static final ChannelOption<Integer> READ_TIMEOUT = valueOf("READ_TIMEOUT");

    @SuppressWarnings({ "unused", "deprecation" })
    private JSerialCommChannelOption() {
        super(null);
    }
}