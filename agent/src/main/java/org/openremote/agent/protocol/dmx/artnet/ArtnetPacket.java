package org.openremote.agent.protocol.dmx.artnet;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.netty.buffer.ByteBuf;
import org.apache.commons.lang.ArrayUtils;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class ArtnetPacket {

    private byte[] prefix = { 65, 114, 116, 45, 78, 101, 116, 0, 0, 80, 0, 14 };

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
        buf.writeBytes(prefix);
        buf.writeByte(0); // Sequence
        buf.writeByte(0); // Physical
        buf.writeByte((universe >> 8) & 0xff);
        buf.writeByte(universe & 0xff);
        buf.writeByte(0); // dummy length hi
        buf.writeByte(0); // dummy length lo
    }

    // Required as we do not know how many light ids we will need to send
    private void updateLength(ByteBuf buf)
    {
        int len_idx = prefix.length + 4;
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
