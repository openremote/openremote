package org.openremote.agent.protocol.dmx.artnet;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.openremote.model.attribute.*;
import org.openremote.agent.protocol.dmx.AbstractDMXLightState;
import org.openremote.model.value.Value;
import org.openremote.model.value.ValueType;

import java.util.Arrays;

public class ArtnetLightState extends AbstractDMXLightState {

    private int r;
    private int g;
    private int b;
    private int w;
    private double dim;
    private boolean enabled;

    public ArtnetLightState(int lightId, int r, int g, int b, int w, double dim, boolean enabled) {
        super(lightId);
        this.r = r;
        this.g = g;
        this.b = b;
        this.w = w;
        this.dim = dim;
        this.enabled = enabled;
    }

    @Override
    public Byte[] getValues() {
        int enable = this.enabled? 1 : 0;
        return Arrays.asList(this.getRawValues())
            .stream()
            .map(y -> (byte)(y * (this.dim/100) * enable))
            .toArray(size -> new Byte[size]);
    }

    private Byte[] getRawValues()
    {
        return new Byte[]{(byte)this.g,(byte)this.r,(byte)this.b,(byte)this.w};
    }

    @Override
    public void fromAttribute(AttributeEvent event, Attribute attr) {
        AttributeRef reference = event.getAttributeRef();
        MetaItem metaItem = attr.getMetaItem("lightId").orElse(null);
        int lampId = metaItem.getValueAsInteger().orElse(-1);

        if (lampId != this.getLightId()) return;

        //DIM ATTRIBUTE
        if(attr.getType().get().getValueType() == ValueType.NUMBER)
            if(attr.getName().get().equalsIgnoreCase("Dim")) {
                String val = event.getAttributeState().getValue().get().toString();
                Byte dimValue = (Byte)(byte)(int) Math.floor((double)Double.parseDouble(val));
                this.dim = dimValue;
            }
        //VALUES ATTRIBUTE
        if(attr.getType().get().getValueType() == ValueType.OBJECT)
            if(attr.getName().get().equalsIgnoreCase("Values")) {
                Value brouh = event.getAttributeState().getValue().orElse(null);
                JsonObject jobject = new JsonParser().parse(brouh.toJson()).getAsJsonObject();
                this.r = jobject.get("r").getAsByte();
                this.g = jobject.get("g").getAsByte();
                this.b = jobject.get("b").getAsByte();
                this.w = jobject.get("w").getAsByte();
            }
        //SWITCH ATTRIBUTE
        if(attr.getType().get().getValueType() == ValueType.BOOLEAN)
            if(attr.getName().get().equalsIgnoreCase("Switch")) {
                String val = event.getAttributeState().getValue().get().toString();
                boolean switchState = (boolean) Boolean.parseBoolean(val);
                if(switchState) {
                    this.enabled = true;
                }else{
                    this.enabled = false;
                }
            }
    }
}
