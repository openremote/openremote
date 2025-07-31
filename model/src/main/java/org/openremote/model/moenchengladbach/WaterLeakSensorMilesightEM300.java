package org.openremote.model.moenchengladbach;

import jakarta.persistence.Entity;
import org.openremote.model.asset.Asset;
import org.openremote.model.asset.AssetDescriptor;
import org.openremote.model.attribute.MetaItem;
import org.openremote.model.value.AttributeDescriptor;
import org.openremote.model.value.MetaItemType;
import org.openremote.model.value.ValueType;

import java.util.Map;
import java.util.Optional;

import static org.openremote.model.Constants.UNITS_CELSIUS;
import static org.openremote.model.Constants.UNITS_PERCENTAGE;

@Entity
public class WaterLeakSensorMilesightEM300 extends Asset<WaterLeakSensorMilesightEM300> {

    public static final AttributeDescriptor<Double> TEMPERATURE = new AttributeDescriptor<>("temperature", ValueType.NUMBER,
        new MetaItem<>(MetaItemType.READ_ONLY),
        new MetaItem<>(MetaItemType.AGENT_LINK_CONFIG, new ValueType.ObjectMap() {{
            putAll(Map.of(
                "uplinkPort", 85,
                "valueFilterJsonPath", "$.object.temperature"
            ));
        }})
    ).withUnits(UNITS_CELSIUS);
    public static final AttributeDescriptor<Double> HUMIDITY = new AttributeDescriptor<>("humidity", ValueType.POSITIVE_NUMBER,
        new MetaItem<>(MetaItemType.READ_ONLY),
        new MetaItem<>(MetaItemType.AGENT_LINK_CONFIG, new ValueType.ObjectMap() {{
            putAll(Map.of(
                "uplinkPort", 85,
                "valueFilterJsonPath", "$.object.humidity"
            ));
        }})
    );
    public static final AttributeDescriptor<Double> BATTERY = new AttributeDescriptor<>("battery", ValueType.POSITIVE_NUMBER,
        new MetaItem<>(MetaItemType.READ_ONLY),
        new MetaItem<>(MetaItemType.AGENT_LINK_CONFIG, new ValueType.ObjectMap() {{
            putAll(Map.of(
                "uplinkPort", 85,
                "valueFilterJsonPath", "$.object.battery"
            ));
        }})
    ).withUnits(UNITS_PERCENTAGE);
    public static final AttributeDescriptor<String> WATER_LEAK = new AttributeDescriptor<>("waterLeak", ValueType.TEXT,
        new MetaItem<>(MetaItemType.READ_ONLY),
        new MetaItem<>(MetaItemType.AGENT_LINK_CONFIG, new ValueType.ObjectMap() {{
            putAll(Map.of(
                "uplinkPort", 85,
                "valueFilterJsonPath", "$.object.water_leak"
            ));
        }})
    );

    public static final AttributeDescriptor<String> DEV_EUI = new AttributeDescriptor<>("devEUI", ValueType.TEXT, new MetaItem<>(MetaItemType.READ_ONLY));
    public static final AttributeDescriptor<String> VENDOR_ID = new AttributeDescriptor<>("vendorId", ValueType.TEXT, new MetaItem<>(MetaItemType.READ_ONLY));
    public static final AttributeDescriptor<String> MODEL_ID = new AttributeDescriptor<>("modelId", ValueType.TEXT, new MetaItem<>(MetaItemType.READ_ONLY));
    public static final AttributeDescriptor<String> FIRMWARE_VERSION = new AttributeDescriptor<>("firmwareVersion", ValueType.TEXT, new MetaItem<>(MetaItemType.READ_ONLY));
    public static final AttributeDescriptor<Boolean> SUPPORTS_CLASS_C = new AttributeDescriptor<>("supportsClassC", ValueType.BOOLEAN, new MetaItem<>(MetaItemType.READ_ONLY));

    public static final AssetDescriptor<WaterLeakSensorMilesightEM300> DESCRIPTOR = new AssetDescriptor<>("pipe-leak", "3d85c6", WaterLeakSensorMilesightEM300.class);

    /**
     * For use by hydrators (i.e. JPA/Jackson)
     */
    protected WaterLeakSensorMilesightEM300() {
    }

    public WaterLeakSensorMilesightEM300(String name) {
        super(name);
    }

    public Optional<Double> getTemperature() {
        return getAttributes().getValue(TEMPERATURE);
    }

    public Optional<Double> getHumidity() {
        return getAttributes().getValue(HUMIDITY);
    }

    public Optional<Double> getBattery() {
        return getAttributes().getValue(BATTERY);
    }

    public Optional<String> getWaterLeak() {
        return getAttributes().getValue(WATER_LEAK);
    }
}
