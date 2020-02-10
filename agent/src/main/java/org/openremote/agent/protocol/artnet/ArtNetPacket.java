package org.openremote.agent.protocol.artnet;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.netty.buffer.ByteBuf;
import org.apache.commons.lang.ArrayUtils;

import org.openremote.model.value.Value;
import org.openremote.model.value.Values;

import java.util.*;
import java.util.stream.Collectors;

public class ArtNetPacket {

    protected int universe;
    protected double dim;
    protected int r;
    protected int g;
    protected int b;
    protected int w;
    protected static byte[] prefix = { 65, 114, 116, 45, 78, 101, 116, 0, 0, 80, 0, 14 };

    @JsonCreator
    public ArtNetPacket(@JsonProperty("universe") int universe,
                        @JsonProperty("dim") double dim,
                        @JsonProperty("r") int r,
                        @JsonProperty("g") int g,
                        @JsonProperty("b") int b,
                        @JsonProperty("w") int w) {
        this.universe = universe;
        this.dim = dim;
        this.r = r;
        this.g = g;
        this.b = b;
        this.w = w;
    }

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
        int len_idx = prefix.length() + 4;
        int len = buf.length() - len_idx - 2;
        buf.setByte(len_idx, (len >> 8) & 0xff);
        buf.setByte(len_idx+1, len & 0xff);
    }

    public static void writeLight(ByteBuf buf, Byte[] light, int repeat = 1) 
    {
        byte[] vals = ArrayUtils.toPrimitive(light);
        for(int i = 0; i < repeat; i++) {
            buf.writeBytes(vals);
        }
    }

    public ByteBuf toBuffer(ByteBuf buf) {
        int[] values = { g, r, b, w };
        buf.writeBytes(prefix);
        buf.writeByte(0); // Sequence
        buf.writeByte(0); // Physical
        buf.writeByte((universe >> 8) & 0xff);
        buf.writeByte(universe & 0xff);
        buf.writeByte((values.length >> 8) & 0xff);
        buf.writeByte(values.length & 0xff);
        for (int x = 0; x < 6; x++)//Each lamp has 6 lights
        {
            for(int i = 0; i < values.length; i++) {
                buf.writeByte(values[i]);
            }
        }
        return buf;
    }
}
