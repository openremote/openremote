package org.openremote.model.asset.impl;

import jakarta.persistence.Entity;
import org.openremote.model.SensorThings.Thing;
import org.openremote.model.SensorThings.UnitOfMeasurement;
import org.openremote.model.asset.Asset;
import org.openremote.model.asset.AssetDescriptor;
import org.openremote.model.geo.GeoJSONPoint;
import org.openremote.model.value.AttributeDescriptor;
import org.openremote.model.value.ValueDescriptor;
import org.openremote.model.value.ValueType;

@Entity
public class SensorThingsAsset extends Asset<SensorThingsAsset> {
    public static final AttributeDescriptor<GeoJSONPoint> location = new AttributeDescriptor<GeoJSONPoint>("area", ValueType.GEO_JSON_POINT);
    public static final AttributeDescriptor<String> iotSelfLink = new AttributeDescriptor<String>("SelfLink", ValueType.TEXT);
    public static final AttributeDescriptor<String> iotId = new AttributeDescriptor<String>("iotId", ValueType.TEXT);
    public static final AttributeDescriptor<String> description = new AttributeDescriptor<String>("description", ValueType.TEXT);
    public static final AttributeDescriptor<String> name = new AttributeDescriptor<String>("name", ValueType.TEXT);
    public static final AttributeDescriptor<String> observationType = new AttributeDescriptor<String>("observationType", ValueType.TEXT);

    //Change to time
    public static final AttributeDescriptor<String> phenomenonTime = new AttributeDescriptor<String>("phenomenonTime", ValueType.TEXT);
//    public static final AttributeDescriptor<UnitOfMeasurement> units = new AttributeDescriptor<>("SensorThingsUnitsOfMeasurement", ValueType.UnitOfMeasurement);
//    public static final AttributeDescriptor<Thing> thing = new AttributeDescriptor<>("thing", ValueType.SMART_CITY_THING);
    public static final AssetDescriptor<SensorThingsAsset> DESCRIPTOR = new AssetDescriptor<>("money", null, SensorThingsAsset.class);
    protected SensorThingsAsset(){

    }
    public SensorThingsAsset(String name) {super(name);}


}
