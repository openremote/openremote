/*
 * Copyright 2017, OpenRemote Inc.
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
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package org.openremote.agent.protocol.velbus;

import static org.openremote.agent.protocol.velbus.VelbusPacket.MAX_PACKET_SIZE;

import java.util.List;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

public final class VelbusPacketDecoder extends ByteToMessageDecoder {

  @Override
  protected void decode(ChannelHandlerContext ctx, ByteBuf buf, List<Object> messages)
      throws Exception {
    int startIndex = buf.indexOf(0, buf.capacity() - 1, VelbusPacket.STX);

    if (startIndex < 0) {
      return;
    }

    if (startIndex > 0) {
      buf.readerIndex(startIndex);
      buf.discardReadBytes();
    }

    if (buf.readableBytes() < 4) {
      return;
    }

    int dataSize = buf.getByte(3);

    if (buf.readableBytes() < 6 + dataSize) {
      return;
    }

    // Find end of packet
    int endIndex = buf.indexOf(4 + dataSize, MAX_PACKET_SIZE, VelbusPacket.ETX);

    if (endIndex < 0) {
      if (buf.readableBytes() > MAX_PACKET_SIZE) {
        buf.readerIndex(MAX_PACKET_SIZE);
        buf.discardReadBytes();
      }
      return;
    }

    byte[] packetBytes = new byte[endIndex + 1];
    buf.readBytes(packetBytes);
    buf.discardReadBytes();
    VelbusPacket packet = new VelbusPacket(packetBytes);

    if (packet.isValid()) {
      messages.add(packet);
    }
  }
}
