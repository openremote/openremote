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

import static org.openremote.model.Constants.*;

@Entity
public class WaterMeterIntegraTopasSonic extends Asset<WaterMeterIntegraTopasSonic> {
    public static final AttributeDescriptor<String> TIMESTAMP = new AttributeDescriptor<>("timestamp", ValueType.TIMESTAMP_ISO8601,
        new MetaItem<>(MetaItemType.AGENT_LINK_CONFIG, new ValueType.ObjectMap() {{
            putAll(Map.of(
                "uplinkPort", 12,
                "valueFilterJsonPath", "$.object.dateAndTime"
            ));
        }})
    );

    public static final AttributeDescriptor<Double> DELTA_1 = new AttributeDescriptor<>("delta1", ValueType.NUMBER,
        new MetaItem<>(MetaItemType.READ_ONLY),
        new MetaItem<>(MetaItemType.AGENT_LINK_CONFIG, new ValueType.ObjectMap() {{
            putAll(Map.of(
                "uplinkPort", 12,
                "valueFilterJsonPath", "$.object.deltas[?(@.index == 1)].value"
            ));
        }})
    ).withUnits(UNITS_LITRE);

    public static final AttributeDescriptor<Double> DELTA_2 = new AttributeDescriptor<>("delta2", ValueType.NUMBER,
        new MetaItem<>(MetaItemType.READ_ONLY),
        new MetaItem<>(MetaItemType.AGENT_LINK_CONFIG, new ValueType.ObjectMap() {{
            putAll(Map.of(
                "uplinkPort", 12,
                "valueFilterJsonPath", "$.object.deltas[?(@.index == 2)].value"
            ));
        }})
    ).withUnits(UNITS_LITRE);

    public static final AttributeDescriptor<Double> DELTA_3 = new AttributeDescriptor<>("delta3", ValueType.NUMBER,
        new MetaItem<>(MetaItemType.READ_ONLY),
        new MetaItem<>(MetaItemType.AGENT_LINK_CONFIG, new ValueType.ObjectMap() {{
            putAll(Map.of(
                "uplinkPort", 12,
                "valueFilterJsonPath", "$.object.deltas[?(@.index == 3)].value"
            ));
        }})
    ).withUnits(UNITS_LITRE);

    public static final AttributeDescriptor<Double> DELTA_4 = new AttributeDescriptor<>("delta4", ValueType.NUMBER,
        new MetaItem<>(MetaItemType.READ_ONLY),
        new MetaItem<>(MetaItemType.AGENT_LINK_CONFIG, new ValueType.ObjectMap() {{
            putAll(Map.of(
                "uplinkPort", 12,
                "valueFilterJsonPath", "$.object.deltas[?(@.index == 4)].value"
            ));
        }})
    ).withUnits(UNITS_LITRE);

    public static final AttributeDescriptor<Double> DELTA_5 = new AttributeDescriptor<>("delta5", ValueType.NUMBER,
        new MetaItem<>(MetaItemType.READ_ONLY),
        new MetaItem<>(MetaItemType.AGENT_LINK_CONFIG, new ValueType.ObjectMap() {{
            putAll(Map.of(
                "uplinkPort", 12,
                "valueFilterJsonPath", "$.object.deltas[?(@.index == 5)].value"
            ));
        }})
    ).withUnits(UNITS_LITRE);

    public static final AttributeDescriptor<Double> DELTA_6 = new AttributeDescriptor<>("delta6", ValueType.NUMBER,
        new MetaItem<>(MetaItemType.READ_ONLY),
        new MetaItem<>(MetaItemType.AGENT_LINK_CONFIG, new ValueType.ObjectMap() {{
            putAll(Map.of(
                "uplinkPort", 12,
                "valueFilterJsonPath", "$.object.deltas[?(@.index == 6)].value"
            ));
        }})
    ).withUnits(UNITS_LITRE);

    public static final AttributeDescriptor<Double> DELTA_7 = new AttributeDescriptor<>("delta7", ValueType.NUMBER,
        new MetaItem<>(MetaItemType.READ_ONLY),
        new MetaItem<>(MetaItemType.AGENT_LINK_CONFIG, new ValueType.ObjectMap() {{
            putAll(Map.of(
                "uplinkPort", 12,
                "valueFilterJsonPath", "$.object.deltas[?(@.index == 7)].value"
            ));
        }})
    ).withUnits(UNITS_LITRE);

    public static final AttributeDescriptor<Double> DELTA_8 = new AttributeDescriptor<>("delta8", ValueType.NUMBER,
        new MetaItem<>(MetaItemType.READ_ONLY),
        new MetaItem<>(MetaItemType.AGENT_LINK_CONFIG, new ValueType.ObjectMap() {{
            putAll(Map.of(
                "uplinkPort", 12,
                "valueFilterJsonPath", "$.object.deltas[?(@.index == 8)].value"
            ));
        }})
    ).withUnits(UNITS_LITRE);

    public static final AttributeDescriptor<Double> DELTA_9 = new AttributeDescriptor<>("delta9", ValueType.NUMBER,
        new MetaItem<>(MetaItemType.READ_ONLY),
        new MetaItem<>(MetaItemType.AGENT_LINK_CONFIG, new ValueType.ObjectMap() {{
            putAll(Map.of(
                "uplinkPort", 12,
                "valueFilterJsonPath", "$.object.deltas[?(@.index == 9)].value"
            ));
        }})
    ).withUnits(UNITS_LITRE);

    public static final AttributeDescriptor<Double> DELTA_10 = new AttributeDescriptor<>("delta10", ValueType.NUMBER,
        new MetaItem<>(MetaItemType.READ_ONLY),
        new MetaItem<>(MetaItemType.AGENT_LINK_CONFIG, new ValueType.ObjectMap() {{
            putAll(Map.of(
                "uplinkPort", 12,
                "valueFilterJsonPath", "$.object.deltas[?(@.index == 10)].value"
            ));
        }})
    ).withUnits(UNITS_LITRE);

    public static final AttributeDescriptor<Double> DELTA_11 = new AttributeDescriptor<>("delta11", ValueType.NUMBER,
        new MetaItem<>(MetaItemType.READ_ONLY),
        new MetaItem<>(MetaItemType.AGENT_LINK_CONFIG, new ValueType.ObjectMap() {{
            putAll(Map.of(
                "uplinkPort", 12,
                "valueFilterJsonPath", "$.object.deltas[?(@.index == 11)].value"
            ));
        }})
    ).withUnits(UNITS_LITRE);

    public static final AttributeDescriptor<Double> DELTA_12 = new AttributeDescriptor<>("delta12", ValueType.NUMBER,
        new MetaItem<>(MetaItemType.READ_ONLY),
        new MetaItem<>(MetaItemType.AGENT_LINK_CONFIG, new ValueType.ObjectMap() {{
            putAll(Map.of(
                "uplinkPort", 12,
                "valueFilterJsonPath", "$.object.deltas[?(@.index == 12)].value"
            ));
        }})
    ).withUnits(UNITS_LITRE);

    public static final AttributeDescriptor<String> ERRORS_AND_ALARMS = new AttributeDescriptor<>("errorsAndAlarms", ValueType.TEXT,
        new MetaItem<>(MetaItemType.READ_ONLY),
        new MetaItem<>(MetaItemType.AGENT_LINK_CONFIG, new ValueType.ObjectMap() {{
            putAll(Map.of(
                "uplinkPort", 12,
                "valueFilterJsonPath", "$.object.errorsAndAlarms"
            ));
        }})
    );

    public static final AttributeDescriptor<Double> INDEX_NET = new AttributeDescriptor<>("indexNet", ValueType.NUMBER,
        new MetaItem<>(MetaItemType.READ_ONLY),
        new MetaItem<>(MetaItemType.AGENT_LINK_CONFIG, new ValueType.ObjectMap() {{
            putAll(Map.of(
                "uplinkPort", 12,
                "valueFilterJsonPath", "$.object.indexNet.value"
            ));
        }})
    ).withUnits(UNITS_LITRE);

    public static final AttributeDescriptor<String> DEV_EUI = new AttributeDescriptor<>("devEUI", ValueType.TEXT, new MetaItem<>(MetaItemType.READ_ONLY));
    public static final AttributeDescriptor<String> VENDOR_ID = new AttributeDescriptor<>("vendorId", ValueType.TEXT, new MetaItem<>(MetaItemType.READ_ONLY));
    public static final AttributeDescriptor<String> MODEL_ID = new AttributeDescriptor<>("modelId", ValueType.TEXT, new MetaItem<>(MetaItemType.READ_ONLY));
    public static final AttributeDescriptor<String> FIRMWARE_VERSION = new AttributeDescriptor<>("firmwareVersion", ValueType.TEXT, new MetaItem<>(MetaItemType.READ_ONLY));
    public static final AttributeDescriptor<Boolean> SUPPORTS_CLASS_C = new AttributeDescriptor<>("supportsClassC", ValueType.BOOLEAN, new MetaItem<>(MetaItemType.READ_ONLY));

    public static final AssetDescriptor<WaterMeterIntegraTopasSonic> DESCRIPTOR = new AssetDescriptor<>("water-outline", "95d0df", WaterMeterIntegraTopasSonic.class);

    /**
     * For use by hydrators (i.e. JPA/Jackson)
     */
    protected WaterMeterIntegraTopasSonic() {
    }

    public WaterMeterIntegraTopasSonic(String name) {
        super(name);
        setShowOnMap(false);
    }

    public WaterMeterIntegraTopasSonic setShowOnMap(boolean showOnMap) {
        getAttributes()
            .getOrCreate(LOCATION)
            .getMetaItem(MetaItemType.SHOW_ON_DASHBOARD)
            .ifPresentOrElse(
                meta -> meta.setValue(showOnMap),
                () -> getAttributes()
                    .getOrCreate(LOCATION)
                    .addMeta(new MetaItem<>(MetaItemType.SHOW_ON_DASHBOARD, showOnMap))
            );
        return this;
    }

    public Optional<String> getTimestamp() {
        return getAttributes().getValue(TIMESTAMP);
    }

    public Optional<Double> getDelta1() {
        return getAttributes().getValue(DELTA_1);
    }

    public Optional<Double> getDelta2() {
        return getAttributes().getValue(DELTA_2);
    }

    public Optional<Double> getDelta3() {
        return getAttributes().getValue(DELTA_3);
    }

    public Optional<Double> getDelta4() {
        return getAttributes().getValue(DELTA_4);
    }

    public Optional<Double> getDelta5() {
        return getAttributes().getValue(DELTA_5);
    }

    public Optional<Double> getDelta6() {
        return getAttributes().getValue(DELTA_6);
    }

    public Optional<Double> getDelta7() {
        return getAttributes().getValue(DELTA_7);
    }

    public Optional<Double> getDelta8() {
        return getAttributes().getValue(DELTA_8);
    }

    public Optional<Double> getDelta9() {
        return getAttributes().getValue(DELTA_9);
    }

    public Optional<Double> getDelta10() {
        return getAttributes().getValue(DELTA_10);
    }

    public Optional<Double> getDelta11() {
        return getAttributes().getValue(DELTA_11);
    }

    public Optional<Double> getDelta12() {
        return getAttributes().getValue(DELTA_12);
    }

    public Optional<String> getErrorsAndAlarms() {
        return getAttributes().getValue(ERRORS_AND_ALARMS);
    }

    public Optional<Double> getIndexNet() {
        return getAttributes().getValue(INDEX_NET);
    }
}
