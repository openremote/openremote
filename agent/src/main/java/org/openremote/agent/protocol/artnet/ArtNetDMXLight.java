package org.openremote.agent.protocol.artnet;

import io.netty.buffer.ByteBuf;

public class ArtNetDMXLight
{
    protected int id;
    protected int universe;
    protected int addresses;
    protected int dim;//Maybe outsource this to the individual Lights
    protected int red;
    protected int green;
    protected int blue;
    protected int white;

    public int getId() {
        return id;
    }

    public int getUniverse() {
        return universe;
    }

    public int getAddresses() {
        return addresses;
    }

    public int getDim() {
        return dim;
    }

    public int getRed() {
        return red;
    }

    public int getGreen() {
        return green;
    }

    public int getBlue() {
        return blue;
    }

    public int getWhite() {
        return white;
    }

    ArtNetDMXLight(int id, int universe, int adresses, int dim, int red, int green, int blue, int white)
    {
        this.id = id;
        this.universe = universe;
        this.addresses = adresses;
        this.dim = dim;
        this.red = red;
        this.green = green;
        this.blue = blue;
        this.white = white;
    }

    ByteBuf appendToBuffer(ByteBuf buf)
    {
        int[] values = { green, red, blue, white };

        buf.writeByte((values.length >> 8) & 0xff);
        buf.writeByte(values.length & 0xff);

        for (int x = 0; x < addresses; x++)//Send the same values to all of the led within the lamp.
        {
            for (int value : values) {
                buf.writeByte(value);
            }
        }

        return buf;
    }
}
