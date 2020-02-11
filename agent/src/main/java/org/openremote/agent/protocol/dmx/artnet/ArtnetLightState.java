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

    public int getR() {
        return r;
    }

    public int getG() {
        return g;
    }

    public void setR(int r) {
        this.r = r;
    }

    public void setG(int g) {
        this.g = g;
    }

    public void setB(int b) {
        this.b = b;
    }

    public void setW(int w) {
        this.w = w;
    }

    public void setDim(double dim) {
        this.dim = dim;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getB() {
        return b;
    }

    public int getW() {
        return w;
    }

    public double getDim() {
        return dim;
    }

    public boolean isEnabled() {
        return enabled;
    }

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
        return Arrays.asList(new Byte[] {(byte)this.getG(), (byte)this.getR(), (byte)this.getB(), (byte)this.getW()}).stream().map(y -> (byte)(y * (this.getDim()/100) * enable)).toArray(size -> new Byte[size]);
        //return new Byte[]{(byte)this.getG(),(byte)this.getR(),(byte)this.getB(),(byte)this.getW()};
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
                Byte r = jobject.get("r").getAsByte();
                Byte g = jobject.get("g").getAsByte();
                Byte b = jobject.get("b").getAsByte();
                Byte w = jobject.get("w").getAsByte();
                this.setR(r);
                this.setG(g);
                this.setB(b);
                this.setW(w);
            }
        //SWITCH ATTRIBUTE
        if(attr.getType().get().getValueType() == ValueType.BOOLEAN)
            if(attr.getName().get().equalsIgnoreCase("Switch")) {
                String val = event.getAttributeState().getValue().get().toString();
                boolean switchState = (boolean) Boolean.parseBoolean(val);
                if(switchState) {
                    this.setEnabled(true);
                }else{
                    this.setEnabled(false);
                }
            }
    }
}
