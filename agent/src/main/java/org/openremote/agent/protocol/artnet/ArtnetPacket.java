package org.openremote.agent.protocol.artnet;

import io.netty.buffer.ByteBuf;
import org.apache.commons.lang.ArrayUtils;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class ArtnetPacket {

    private byte[] PREFIX = { 65, 114, 116, 45, 78, 101, 116, 0, 0, 80, 0, 14 };
    private byte SEQUENCE = 0;
    private byte PHYSICAL = 0;
    private byte DUMMY_LENGTH_HI = 0;
    private byte DUMMY_LENGTH_LO = 0;

    private int universe;
    private List<ArtnetLight> lights;

    public ArtnetPacket(int universe, List<ArtnetLight> lights) {
        this.universe = universe;
        Collections.sort(lights, Comparator.comparingInt(ArtnetLight ::getLightId));
        this.lights = lights;
    }

    public void toByteBuf(ByteBuf buf) {
        writePrefix(buf, this.universe);
        for(ArtnetLight light : lights)
            writeLight(buf, light.getLightState().getValues(), light.getAmountOfLeds());
        updateLength(buf);
    }


    private void writePrefix(ByteBuf buf, int universe)
    {
        buf.writeBytes(PREFIX);
        buf.writeByte(SEQUENCE);
        buf.writeByte(PHYSICAL);
        buf.writeByte((universe >> 8) & 0xff);
        buf.writeByte(universe & 0xff);
        buf.writeByte(DUMMY_LENGTH_HI);
        buf.writeByte(DUMMY_LENGTH_LO);
    }

    // Required as we do not know how many light ids we will need to send
    private void updateLength(ByteBuf buf)
    {
        int len_idx = PREFIX.length + 4;
        int len = buf.writerIndex() - len_idx - 2;
        buf.setByte(len_idx, (len >> 8) & 0xff);
        buf.setByte(len_idx+1, len & 0xff);
    }

    private void writeLight(ByteBuf buf, Byte[] light, int repeat)
    {
        byte[] values = ArrayUtils.toPrimitive(light);
        for(int i = 0; i < repeat; i++)
            buf.writeBytes(values);
    }
}
