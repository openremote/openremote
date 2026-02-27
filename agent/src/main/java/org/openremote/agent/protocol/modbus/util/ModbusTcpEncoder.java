/*
 * Copyright 2026, OpenRemote Inc.
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
package org.openremote.agent.protocol.modbus.util;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import org.openremote.model.syslog.SyslogCategory;
import java.util.logging.Logger;
import static org.openremote.model.syslog.SyslogCategory.PROTOCOL;

public class ModbusTcpEncoder extends io.netty.handler.codec.MessageToByteEncoder<ModbusTcpFrame> {

    public static final Logger LOG = SyslogCategory.getLogger(PROTOCOL, ModbusTcpEncoder.class);

    @Override
    protected void encode(ChannelHandlerContext ctx, ModbusTcpFrame frame, ByteBuf out) {
        // MBAP Header (7 bytes)
        out.writeShort(frame.getTransactionId());  // Transaction ID
        out.writeShort(frame.getProtocolId());     // Protocol ID (0 for Modbus)
        out.writeShort(frame.getLength());          // Length (Unit ID + PDU)
        out.writeByte(frame.getUnitId());          // Unit ID

        // PDU
        if (frame.getPdu() != null) {
            out.writeBytes(frame.getPdu());
        }

        LOG.finest(() -> String.format("Encoded Modbus TCP frame: TxID=%d, UnitID=%d, FC=0x%02X, PDU length=%d",
                frame.getTransactionId(), frame.getUnitId(), frame.getFunctionCode(),
                frame.getPdu() != null ? frame.getPdu().length : 0));
    }
}
