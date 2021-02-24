package org.openremote.agent.protocol.artnet;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.openremote.model.asset.AssetDescriptor;
import org.openremote.model.asset.impl.LightAsset;
import org.openremote.model.value.AttributeDescriptor;
import org.openremote.model.value.ValueType;

import javax.persistence.Entity;
import javax.persistence.Transient;
import java.util.Map;
import java.util.Optional;

@Entity
public class ArtnetLightAsset extends LightAsset {

    public static final AttributeDescriptor<Integer> LED_COUNT = new AttributeDescriptor<>("lEDCount", ValueType.POSITIVE_INTEGER);
    public static final AttributeDescriptor<Integer> LIGHT_ID = new AttributeDescriptor<>("lightId", ValueType.POSITIVE_INTEGER);
    public static final AttributeDescriptor<Integer> GROUP_ID = new AttributeDescriptor<>("groupId", ValueType.POSITIVE_INTEGER);
    public static final AttributeDescriptor<Integer> UNIVERSE = new AttributeDescriptor<>("universe", ValueType.POSITIVE_INTEGER);
    public static final AttributeDescriptor<String[]> REQUIRED_VALUES = new AttributeDescriptor<>("requiredValues", ValueType.TEXT.asArray());

    public static final AssetDescriptor<ArtnetLightAsset> DESCRIPTOR = new AssetDescriptor<>(
        "lightbulb", "e6688a", ArtnetLightAsset.class
    );

    @Transient
    @JsonIgnore
    private Map<String, Integer> receivedValues;

    /**
     * For use by hydrators (i.e. JPA/Jackson)
     */
    protected ArtnetLightAsset() {
    }

    protected ArtnetLightAsset(String name) {
        super(name);
    }

    public Optional<Integer> getLightId() {
        return getAttributes().getValue(LIGHT_ID);
    }

    public void setLightId(int lightId) {
        getAttributes().getOrCreate(LIGHT_ID).setValue(lightId);
    }

    public Optional<Integer> getGroupId() {
        return getAttributes().getValue(GROUP_ID);
    }

    public void setGroupId(int groupId) {
        getAttributes().getOrCreate(GROUP_ID).setValue(groupId);
    }

    public Optional<Integer> getLEDCount() {
        return getAttributes().getValue(LED_COUNT);
    }

    public void setLEDCount(int count) {
        getAttributes().getOrCreate(LED_COUNT).setValue(count);
    }

    public Optional<Integer> getUniverse() {
        return getAttributes().getValue(UNIVERSE);
    }

    public void setUniverse(int universe) {
        getAttributes().getOrCreate(UNIVERSE).setValue(universe);
    }

    public Optional<String[]> getRequiredValues() {
        return getAttributes().getValue(REQUIRED_VALUES);
    }

    public void setRequiredValues(String[] requiredValues) {
        getAttributes().getOrCreate(REQUIRED_VALUES).setValue(requiredValues);
    }

    public void setReceivedValues(Map<String, Integer> receivedValues) {
        this.receivedValues = receivedValues;
    }

    public Byte[] getValues() {
        int enable = getOnOff().orElse(false) ? 1 : 0;
        if (receivedValues == null) {
            return null;
        }

        return receivedValues.values().stream()
            .map(y -> (byte)(y * (((double)this.getBrightness().orElse(0))/100d) * enable))
            .toArray(Byte[]::new);
    }

    // This method was never called anywhere which means received values always contains 0 values
//    public void fromAttribute(AttributeEvent event, Attribute attr) {
//        AttributeRef reference = event.getAttributeRef();
//        MetaItem metaItem = attr.getMetaItem("lightId").orElse(null);
//        int lampId = metaItem.getValueAsInteger().orElse(-1);
//
//        if (lampId != this.getLightId()) return;
//
//        //DIM ATTRIBUTE
//        if(attr.getType().get().getValueType() == ValueType.NUMBER)
//            if(attr.getName().get().equalsIgnoreCase("Dim")) {
//                String val = event.getAttributeState().getValue().get().toString();
//                this.dim = Math.floor((double)Double.parseDouble(val));
//            }
//        //VALUES ATTRIBUTE
//        if(attr.getType().get().getValueType() == ValueType.OBJECT)
//            if(attr.getName().get().equalsIgnoreCase("Values")) {
//                Value brouh = event.getAttributeState().getValue().orElse(null);
//                ObjectMapper mapper = new ObjectMapper();
//                try {
//                    JsonNode node = mapper.readTree(brouh.toJson());
//                    new HashMap<String, Integer>(this.receivedValues).keySet().forEach(keyIndicator -> {
//                        this.receivedValues.put(keyIndicator, node.get(keyIndicator).asInt());
//                    });
//                } catch (JsonProcessingException e) {
//                    e.printStackTrace();
//                }
//            }
//        //SWITCH ATTRIBUTE
//        if(attr.getType().get().getValueType() == ValueType.BOOLEAN)
//            if(attr.getName().get().equalsIgnoreCase("Switch")) {
//                String val = event.getAttributeState().getValue().get().toString();
//                boolean switchState = (boolean) Boolean.parseBoolean(val);
//                if(switchState) {
//                    this.enabled = true;
//                }else{
//                    this.enabled = false;
//                }
//            }
//    }
}
