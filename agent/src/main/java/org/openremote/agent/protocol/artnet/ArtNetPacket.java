package org.openremote.agent.protocol.artnet;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.netty.buffer.ByteBuf;

import org.openremote.model.value.ArrayValue;
import org.openremote.model.value.Value;
import org.openremote.model.value.Values;

import java.nio.ByteBuffer;
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

    public static Optional<ArtNetPacket> fromValue(Value value) {

        Optional<ArtNetPacket> a = Values.getObject(value).flatMap(obj -> {
            int _universe = obj.getNumber("universe").orElse(0.).intValue();
            double  _dim = obj.getNumber("dim").orElse(1.);
/*
            int[] _values = obj.getArray("values").get()
                    .stream()
                    .mapToInt(num -> num.asAny().asByte())
                    .toArray();
*/
            int _r = obj.getNumber("r").orElse(0.).intValue();
            int _g = obj.getNumber("g").orElse(0.).intValue();
            int _b = obj.getNumber("b").orElse(0.).intValue();
            int _w = obj.getNumber("w").orElse(0.).intValue();
            return Optional.of(new ArtNetPacket(_universe, _dim,  _r, _g, _b, _w));
        });

        return  a;
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

    public ByteBuf[] assemblePacket(ByteBuf buf, ArrayList<ArtNetDMXLight> lights)
    {
        //Detect highest universe
        int highestUniverse = Collections.max(lights, Comparator.comparing(l -> l.universe)).universe;

        //Create a packet for each universe
        for (int u = 0; u <= highestUniverse; u++)
        {
            ArrayList<ArtNetDMXLight> universeLights = new ArrayList<>();

            int finalU = u;//Required for lambda statements.
            List<ArtNetDMXLight> filteredLights =
                    lights.stream().filter(x -> lights.stream().anyMatch(y -> y.universe == finalU)).collect(Collectors.toList());

            universeLights.addAll(filteredLights);

            /*TODO: Add to a buffer array
            //Prefix package
            buf.writeBytes(prefix);
            buf.writeByte(0); // Sequence
            buf.writeByte(0); // Physical
            buf.writeByte((universe >> 8) & 0xff);
            buf.writeByte(universe & 0xff);
            */
        }

        for (ArtNetDMXLight light : lights)
        {
            buf = light.toBuffer(buf);//toBuffer effectively appends to the buffer.
        }

        return new ByteBuf[1];//TEST data
    }
}
