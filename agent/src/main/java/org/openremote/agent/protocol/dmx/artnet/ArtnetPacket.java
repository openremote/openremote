package org.openremote.agent.protocol.dmx.artnet;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.netty.buffer.ByteBuf;
import org.apache.commons.lang.ArrayUtils;

public class ArtnetPacket {
    protected static byte[] prefix = { 65, 114, 116, 45, 78, 101, 116, 0, 0, 80, 0, 14 };

    public static void writePrefix(ByteBuf buf, int universe)
    {
        buf.writeBytes(prefix);
        buf.writeByte(0); // Sequence
        buf.writeByte(0); // Physical
        buf.writeByte((universe >> 8) & 0xff);
        buf.writeByte(universe & 0xff);
        buf.writeByte(0); // dummy length hi
        buf.writeByte(0); // dummy length lo
    }

    // Required as we do not know how many light ids we will need to send
    public static void updateLength(ByteBuf buf)
    {
        int len_idx = prefix.length + 4;
        int len = buf.writerIndex() - len_idx - 2;
        buf.setByte(len_idx, (len >> 8) & 0xff);
        buf.setByte(len_idx+1, len & 0xff);
    }

    public static void writeLight(ByteBuf buf, Byte[] light, int repeat)
    {
        byte[] vals = ArrayUtils.toPrimitive(light);
        for(int i = 0; i < repeat; i++) {
            buf.writeBytes(vals);
        }
    }
}
