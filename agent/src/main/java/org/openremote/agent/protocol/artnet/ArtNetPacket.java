package org.openremote.agent.protocol.artnet;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.netty.buffer.ByteBuf;

import org.openremote.model.value.ArrayValue;
import org.openremote.model.value.Value;
import org.openremote.model.value.Values;

import java.util.ArrayList;
import java.util.Optional;
import java.util.stream.Collectors;

public class ArtNetPacket {
    protected int universe;
    protected double dim;
    protected int[] values;
    protected static byte[] prefix = { 65, 114, 116, 45, 78, 101, 116, 0, 0, 80, 0, 14 };

    @JsonCreator
    public ArtNetPacket(@JsonProperty("universe") int universe,
                       @JsonProperty("dim") double dim,
                       @JsonProperty("values") int[] values) {
        this.universe = universe;
        this.dim = dim;
        this.values = values;
    }

    public static Optional<ArtNetPacket> fromValue(Value value) {
        return Values.getObject(value).flatMap(obj -> {
            int _universe = obj.getNumber("universe").orElse(0.).intValue();
            double  _dim = obj.getNumber("dim").orElse(1.);
            ArrayValue _values = obj.getArray("values").get();
            int[] values = new int[_values.length()];
            for (int i = 0; i < values.length; i++) {
                values[i] = _values.get(i).get().asAny().asInt();
            }

            return Optional.of(new ArtNetPacket(_universe, _dim,  values));
        });
    }

    public void toBuffer(ByteBuf buf) {
        buf.writeBytes(prefix);
        buf.writeByte(0); // Sequence
        buf.writeByte(0); // Physical
        buf.writeByte((universe >> 8) & 0xff);
        buf.writeByte(universe & 0xff);
        buf.writeByte((values.length >> 8) & 0xff);
        buf.writeByte(values.length & 0xff);
        for(int i = 0; i <= values.length; i++) {
            buf.writeByte(values[i]);
        }
    }
}
