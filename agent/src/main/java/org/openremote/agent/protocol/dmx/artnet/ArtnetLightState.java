package org.openremote.agent.protocol.dmx.artnet;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.openremote.model.attribute.*;
import org.openremote.agent.protocol.dmx.AbstractArtnetLightState;
import org.openremote.model.value.Value;
import org.openremote.model.value.ValueType;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class ArtnetLightState extends AbstractArtnetLightState {

    private Map<String, Integer> receivedValues;
    private double dim;
    private boolean enabled;

    public ArtnetLightState(int lightId, Map<String, Integer> receivedValues, double dim, boolean enabled) {
        super(lightId);
        this.receivedValues = receivedValues;
        this.dim = dim;
        this.enabled = enabled;
    }

    public Map<String, Integer> getReceivedValues() {
        return this.receivedValues;
    }

    public double getDim() {
        return dim;
    }

    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public Byte[] getValues() {
        int enable = this.enabled? 1 : 0;
        return Arrays.asList(this.getReceivedValues().values().toArray(new Integer[this.getReceivedValues().size()]))
            .stream()
            .map(y -> (byte)(y * (this.dim/100.) * enable))
            .toArray(size -> new Byte[size]);
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
                this.dim = Math.floor((double)Double.parseDouble(val));
            }
        //VALUES ATTRIBUTE
        if(attr.getType().get().getValueType() == ValueType.OBJECT)
            if(attr.getName().get().equalsIgnoreCase("Values")) {
                Value brouh = event.getAttributeState().getValue().orElse(null);
                JsonObject jobject = new JsonParser().parse(brouh.toJson()).getAsJsonObject();
                new HashMap<String, Integer>(this.receivedValues).keySet().forEach(keyIndicator -> {
                    this.receivedValues.put(keyIndicator, jobject.get(keyIndicator).getAsInt());
                });
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
