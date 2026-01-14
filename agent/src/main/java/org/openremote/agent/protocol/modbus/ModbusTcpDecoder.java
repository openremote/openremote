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
package org.openremote.agent.protocol.modbus;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import org.openremote.model.syslog.SyslogCategory;
import java.util.List;
import java.util.logging.Logger;

import static org.openremote.model.syslog.SyslogCategory.PROTOCOL;

public class ModbusTcpDecoder extends io.netty.handler.codec.ByteToMessageDecoder {

    public static final Logger LOG = SyslogCategory.getLogger(PROTOCOL, ModbusTcpIOClient.class);

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
        // Need at least 7 bytes for MBAP header
        if (in.readableBytes() < 7) {
            return;
        }

        in.markReaderIndex();

        // Read MBAP header
        int transactionId = in.readUnsignedShort();
        int protocolId = in.readUnsignedShort();
        int length = in.readUnsignedShort();
        int unitId = in.readUnsignedByte();
        int pduLength = length - 1; // Length includes unit ID

        if (in.readableBytes() < pduLength) {
            // Not enough data yet, reset and wait
            in.resetReaderIndex();
            return;
        }

        byte[] pdu = new byte[pduLength];
        in.readBytes(pdu);

        ModbusTcpFrame frame = new ModbusTcpFrame(transactionId, protocolId, length, unitId, pdu);

        LOG.finest(() -> String.format("Decoded Modbus TCP frame: TxID=%d, UnitID=%d, FC=0x%02X, PDU length=%d, Exception=%b",
                frame.getTransactionId(), frame.getUnitId(), frame.getFunctionCode(),
                frame.getPdu().length, frame.isException()));

        out.add(frame);
    }
}
