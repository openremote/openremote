/*
 * Copyright 2026, OpenRemote Inc.
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
package org.openremote.agent.protocol.artnet;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.commons.lang3.ArrayUtils;

import io.netty.buffer.ByteBuf;

public class ArtnetPacket {

  private byte[] PREFIX = {65, 114, 116, 45, 78, 101, 116, 0, 0, 80, 0, 14};
  private byte SEQUENCE = 0;
  private byte PHYSICAL = 0;
  private byte DUMMY_LENGTH_HI = 0;
  private byte DUMMY_LENGTH_LO = 0;

  private int universe;
  private List<ArtnetLightAsset> lights;

  public ArtnetPacket(int universe, List<ArtnetLightAsset> lights) {
    this.universe = universe;
    Collections.sort(lights, Comparator.comparingInt(light -> light.getLightId().orElse(0)));
    this.lights = lights;
  }

  public void toByteBuf(ByteBuf buf) {
    writePrefix(buf, this.universe);
    for (ArtnetLightAsset light : lights)
      writeLight(buf, light.getValues(), light.getLEDCount().orElse(0));
    updateLength(buf);
  }

  private void writePrefix(ByteBuf buf, int universe) {
    buf.writeBytes(PREFIX);
    buf.writeByte(SEQUENCE);
    buf.writeByte(PHYSICAL);
    buf.writeByte((universe >> 8) & 0xff);
    buf.writeByte(universe & 0xff);
    buf.writeByte(DUMMY_LENGTH_HI);
    buf.writeByte(DUMMY_LENGTH_LO);
  }

  // Required as we do not know how many light ids we will need to send
  private void updateLength(ByteBuf buf) {
    int len_idx = PREFIX.length + 4;
    int len = buf.writerIndex() - len_idx - 2;
    buf.setByte(len_idx, (len >> 8) & 0xff);
    buf.setByte(len_idx + 1, len & 0xff);
  }

  private void writeLight(ByteBuf buf, Byte[] light, int repeat) {
    byte[] values = ArrayUtils.toPrimitive(light);
    for (int i = 0; i < repeat; i++) buf.writeBytes(values);
  }
}
