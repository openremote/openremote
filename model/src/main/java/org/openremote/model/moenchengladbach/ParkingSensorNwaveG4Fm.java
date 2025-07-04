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
import static org.openremote.model.Constants.UNITS_VOLT;

@Entity
public class ParkingSensorNwaveG4Fm extends Asset<ParkingSensorNwaveG4Fm> {
    public static final AttributeDescriptor<Boolean> OCCUPIED = new AttributeDescriptor<>("occupied", ValueType.BOOLEAN,
        new MetaItem<>(MetaItemType.AGENT_LINK_CONFIG, new ValueType.ObjectMap() {{
            putAll(Map.of(
                "valueFilterJsonPath", "$.object.occupied",
                "valueConverter", new ValueType.ObjectMap() {{
                    putAll(Map.of(
                        "TRUE", "true",
                        "FALSE", "false"
                    ));
                }}
            ));
        }})
    );
    public static final AttributeDescriptor<Double> TEMPERATURE = new AttributeDescriptor<>("temperature", ValueType.NUMBER,
        new MetaItem<>(MetaItemType.READ_ONLY),
        new MetaItem<>(MetaItemType.AGENT_LINK_CONFIG, new ValueType.ObjectMap() {{
            putAll(Map.of(
                "uplinkPort", 2,
                "valueFilterJsonPath", "$.object.temperature"
            ));
        }})
    ).withUnits(UNITS_CELSIUS);
    public static final AttributeDescriptor<String> BATTERY_HEALTH = new AttributeDescriptor<>("batteryHealth", ValueType.TEXT,
        new MetaItem<>(MetaItemType.READ_ONLY),
        new MetaItem<>(MetaItemType.AGENT_LINK_CONFIG, new ValueType.ObjectMap() {{
            putAll(Map.of(
                "uplinkPort", 2,
                "valueFilterJsonPath", "$.object.battery_health"
            ));
        }})
    );
    public static final AttributeDescriptor<Double> BATTERY_VOLTAGE = new AttributeDescriptor<>("batteryVoltage", ValueType.POSITIVE_NUMBER,
        new MetaItem<>(MetaItemType.READ_ONLY),
        new MetaItem<>(MetaItemType.AGENT_LINK_CONFIG, new ValueType.ObjectMap() {{
            putAll(Map.of(
                "uplinkPort", 2,
                "valueFilterJsonPath", "$.object.battery_voltage"
            ));
        }})
    ).withUnits(UNITS_VOLT);
    public static final AttributeDescriptor<Integer> HW_HEALTH_STATUS = new AttributeDescriptor<>("hwHealthStatus", ValueType.POSITIVE_INTEGER,
        new MetaItem<>(MetaItemType.READ_ONLY),
        new MetaItem<>(MetaItemType.AGENT_LINK_CONFIG, new ValueType.ObjectMap() {{
            putAll(Map.of(
                "uplinkPort", 2,
                "valueFilterJsonPath", "$.object.hw_health_status"
            ));
        }})
    );

    public static final AttributeDescriptor<String> DEV_EUI = new AttributeDescriptor<>("devEUI", ValueType.TEXT, new MetaItem<>(MetaItemType.READ_ONLY));
    public static final AttributeDescriptor<String> VENDOR_ID = new AttributeDescriptor<>("vendorId", ValueType.TEXT, new MetaItem<>(MetaItemType.READ_ONLY));
    public static final AttributeDescriptor<String> MODEL_ID = new AttributeDescriptor<>("modelId", ValueType.TEXT, new MetaItem<>(MetaItemType.READ_ONLY));
    public static final AttributeDescriptor<String> FIRMWARE_VERSION = new AttributeDescriptor<>("firmwareVersion", ValueType.TEXT, new MetaItem<>(MetaItemType.READ_ONLY));
    public static final AttributeDescriptor<Boolean> SUPPORTS_CLASS_C = new AttributeDescriptor<>("supportsClassC", ValueType.BOOLEAN, new MetaItem<>(MetaItemType.READ_ONLY));

    public static final AssetDescriptor<ParkingSensorNwaveG4Fm> DESCRIPTOR = new AssetDescriptor<>("parking", "0260ae", ParkingSensorNwaveG4Fm.class);

    /**
     * For use by hydrators (i.e. JPA/Jackson)
     */
    protected ParkingSensorNwaveG4Fm() {
    }

    public ParkingSensorNwaveG4Fm(String name) {
        super(name);
    }

    public Optional<Boolean> getOccupied() {
        return getAttributes().getValue(OCCUPIED);
    }

    public Optional<Double> getTemperature() {
        return getAttributes().getValue(TEMPERATURE);
    }

    public Optional<String> getBatteryHealth() {
        return getAttributes().getValue(BATTERY_HEALTH);
    }

    public Optional<Double> getBatteryVoltage() {
        return getAttributes().getValue(BATTERY_VOLTAGE);
    }

    public Optional<Integer> getHwHealthStatus() {
        return getAttributes().getValue(HW_HEALTH_STATUS);
    }
}
