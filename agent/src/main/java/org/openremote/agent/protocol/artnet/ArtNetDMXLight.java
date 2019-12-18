package org.openremote.agent.protocol.artnet;

import io.netty.buffer.ByteBuf;

public class ArtNetDMXLight
{
    protected int id;
    protected int universe;
    protected int adresses;
    protected int dim;//Maybe outsource this to the individual Lights
    protected int red;
    protected int green;
    protected int blue;
    protected int white;

    ArtNetDMXLight(int id, int universe, int adresses, int dim, int red, int green, int blue, int white)
    {
        this.id = id;
        this.universe = universe;
        this.adresses = adresses;
        this.dim = dim;
        this.red = red;
        this.green = green;
        this.blue = blue;
        this.white = white;
    }

    ByteBuf toBuffer(ByteBuf buf)
    {
        int[] values = { green, red, blue, white };

        buf.writeByte((values.length >> 8) & 0xff);
        buf.writeByte(values.length & 0xff);

        for (int x = 0; x < adresses; x++)//Send the same values to all of the led within the lamp.
        {
            for (int value : values) {
                buf.writeByte(value);
            }
        }

        return buf;
    }
}
