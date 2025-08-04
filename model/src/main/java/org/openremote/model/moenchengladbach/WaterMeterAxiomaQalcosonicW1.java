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
public class WaterMeterAxiomaQalcosonicW1 extends Asset<WaterMeterAxiomaQalcosonicW1> {
    public static final AttributeDescriptor<String> TIMESTAMP = new AttributeDescriptor<>("timestamp", ValueType.TIMESTAMP_ISO8601,
        new MetaItem<>(MetaItemType.AGENT_LINK_CONFIG, new ValueType.ObjectMap() {{
            putAll(Map.of(
                "uplinkPort", 100,
                "valueFilterJsonPath", "$.object.dateAndTime"
            ));
        }})
    );
    public static final AttributeDescriptor<String> LOG_TIMESTAMP = new AttributeDescriptor<>("logTimestamp", ValueType.TIMESTAMP_ISO8601,
        new MetaItem<>(MetaItemType.AGENT_LINK_CONFIG, new ValueType.ObjectMap() {{
            putAll(Map.of(
                "uplinkPort", 100,
                "valueFilterJsonPath", "$.object.logDateAndTime"
            ));
        }})
    );
    public static final AttributeDescriptor<String> STATUS = new AttributeDescriptor<>("status", ValueType.TEXT,
        new MetaItem<>(MetaItemType.READ_ONLY),
        new MetaItem<>(MetaItemType.AGENT_LINK_CONFIG, new ValueType.ObjectMap() {{
            putAll(Map.of(
                "uplinkPort", 100,
                "valueFilterJsonPath", "$.object.status[0]"
            ));
        }})
    );
    public static final AttributeDescriptor<Double> VOLUME = new AttributeDescriptor<>("volume", ValueType.POSITIVE_NUMBER,
        new MetaItem<>(MetaItemType.READ_ONLY),
        new MetaItem<>(MetaItemType.AGENT_LINK_CONFIG, new ValueType.ObjectMap() {{
            putAll(Map.of(
                "uplinkPort", 100,
                "valueFilterJsonPath", "$.object.volume.value"
            ));
        }})
    ).withUnits(UNITS_METRE, UNITS_CUBED);
    public static final AttributeDescriptor<Double> LOG_VOLUME = new AttributeDescriptor<>("logVolume", ValueType.POSITIVE_NUMBER,
        new MetaItem<>(MetaItemType.READ_ONLY),
        new MetaItem<>(MetaItemType.AGENT_LINK_CONFIG, new ValueType.ObjectMap() {{
            putAll(Map.of(
                "uplinkPort", 100,
                "valueFilterJsonPath", "$.object.volumeAtLogDateAndTime.value"
            ));
        }})
    ).withUnits(UNITS_METRE, UNITS_CUBED);
    public static final AttributeDescriptor<Double> DELTA_PERIOD_1 = new AttributeDescriptor<>("deltaPeriod1", ValueType.NUMBER,
        new MetaItem<>(MetaItemType.READ_ONLY),
        new MetaItem<>(MetaItemType.AGENT_LINK_CONFIG, new ValueType.ObjectMap() {{
            putAll(Map.of(
                "uplinkPort", 100,
                "valueFilterJsonPath", "$.object.deltaVolumes[?(@.delta_period == 1)].value"
            ));
        }})
    ).withUnits(UNITS_METRE, UNITS_CUBED);
    public static final AttributeDescriptor<Double> DELTA_PERIOD_2 = new AttributeDescriptor<>("deltaPeriod2", ValueType.NUMBER,
        new MetaItem<>(MetaItemType.READ_ONLY),
        new MetaItem<>(MetaItemType.AGENT_LINK_CONFIG, new ValueType.ObjectMap() {{
            putAll(Map.of(
                "uplinkPort", 100,
                "valueFilterJsonPath", "$.object.deltaVolumes[?(@.delta_period == 2)].value"
            ));
        }})
    ).withUnits(UNITS_METRE, UNITS_CUBED);
    public static final AttributeDescriptor<Double> DELTA_PERIOD_3 = new AttributeDescriptor<>("deltaPeriod3", ValueType.NUMBER,
        new MetaItem<>(MetaItemType.READ_ONLY),
        new MetaItem<>(MetaItemType.AGENT_LINK_CONFIG, new ValueType.ObjectMap() {{
            putAll(Map.of(
                "uplinkPort", 100,
                "valueFilterJsonPath", "$.object.deltaVolumes[?(@.delta_period == 3)].value"
            ));
        }})
    ).withUnits(UNITS_METRE, UNITS_CUBED);
    public static final AttributeDescriptor<Double> DELTA_PERIOD_4 = new AttributeDescriptor<>("deltaPeriod4", ValueType.NUMBER,
        new MetaItem<>(MetaItemType.READ_ONLY),
        new MetaItem<>(MetaItemType.AGENT_LINK_CONFIG, new ValueType.ObjectMap() {{
            putAll(Map.of(
                "uplinkPort", 100,
                "valueFilterJsonPath", "$.object.deltaVolumes[?(@.delta_period == 4)].value"
            ));
        }})
    ).withUnits(UNITS_METRE, UNITS_CUBED);
    public static final AttributeDescriptor<Double> DELTA_PERIOD_5 = new AttributeDescriptor<>("deltaPeriod5", ValueType.NUMBER,
        new MetaItem<>(MetaItemType.READ_ONLY),
        new MetaItem<>(MetaItemType.AGENT_LINK_CONFIG, new ValueType.ObjectMap() {{
            putAll(Map.of(
                "uplinkPort", 100,
                "valueFilterJsonPath", "$.object.deltaVolumes[?(@.delta_period == 5)].value"
            ));
        }})
    ).withUnits(UNITS_METRE, UNITS_CUBED);
    public static final AttributeDescriptor<Double> DELTA_PERIOD_6 = new AttributeDescriptor<>("deltaPeriod6", ValueType.NUMBER,
        new MetaItem<>(MetaItemType.READ_ONLY),
        new MetaItem<>(MetaItemType.AGENT_LINK_CONFIG, new ValueType.ObjectMap() {{
            putAll(Map.of(
                "uplinkPort", 100,
                "valueFilterJsonPath", "$.object.deltaVolumes[?(@.delta_period == 6)].value"
            ));
        }})
    ).withUnits(UNITS_METRE, UNITS_CUBED);
    public static final AttributeDescriptor<Double> DELTA_PERIOD_7 = new AttributeDescriptor<>("deltaPeriod7", ValueType.NUMBER,
        new MetaItem<>(MetaItemType.READ_ONLY),
        new MetaItem<>(MetaItemType.AGENT_LINK_CONFIG, new ValueType.ObjectMap() {{
            putAll(Map.of(
                "uplinkPort", 100,
                "valueFilterJsonPath", "$.object.deltaVolumes[?(@.delta_period == 7)].value"
            ));
        }})
    ).withUnits(UNITS_METRE, UNITS_CUBED);
    public static final AttributeDescriptor<Double> DELTA_PERIOD_8 = new AttributeDescriptor<>("deltaPeriod8", ValueType.NUMBER,
        new MetaItem<>(MetaItemType.READ_ONLY),
        new MetaItem<>(MetaItemType.AGENT_LINK_CONFIG, new ValueType.ObjectMap() {{
            putAll(Map.of(
                "uplinkPort", 100,
                "valueFilterJsonPath", "$.object.deltaVolumes[?(@.delta_period == 8)].value"
            ));
        }})
    ).withUnits(UNITS_METRE, UNITS_CUBED);
    public static final AttributeDescriptor<Double> DELTA_PERIOD_9 = new AttributeDescriptor<>("deltaPeriod9", ValueType.NUMBER,
        new MetaItem<>(MetaItemType.READ_ONLY),
        new MetaItem<>(MetaItemType.AGENT_LINK_CONFIG, new ValueType.ObjectMap() {{
            putAll(Map.of(
                "uplinkPort", 100,
                "valueFilterJsonPath", "$.object.deltaVolumes[?(@.delta_period == 9)].value"
            ));
        }})
    ).withUnits(UNITS_METRE, UNITS_CUBED);
    public static final AttributeDescriptor<Double> DELTA_PERIOD_10 = new AttributeDescriptor<>("deltaPeriod10", ValueType.NUMBER,
        new MetaItem<>(MetaItemType.READ_ONLY),
        new MetaItem<>(MetaItemType.AGENT_LINK_CONFIG, new ValueType.ObjectMap() {{
            putAll(Map.of(
                "uplinkPort", 100,
                "valueFilterJsonPath", "$.object.deltaVolumes[?(@.delta_period == 10)].value"
            ));
        }})
    ).withUnits(UNITS_METRE, UNITS_CUBED);
    public static final AttributeDescriptor<Double> DELTA_PERIOD_11 = new AttributeDescriptor<>("deltaPeriod11", ValueType.NUMBER,
        new MetaItem<>(MetaItemType.READ_ONLY),
        new MetaItem<>(MetaItemType.AGENT_LINK_CONFIG, new ValueType.ObjectMap() {{
            putAll(Map.of(
                "uplinkPort", 100,
                "valueFilterJsonPath", "$.object.deltaVolumes[?(@.delta_period == 11)].value"
            ));
        }})
    ).withUnits(UNITS_METRE, UNITS_CUBED);
    public static final AttributeDescriptor<Double> DELTA_PERIOD_12 = new AttributeDescriptor<>("deltaPeriod12", ValueType.NUMBER,
        new MetaItem<>(MetaItemType.READ_ONLY),
        new MetaItem<>(MetaItemType.AGENT_LINK_CONFIG, new ValueType.ObjectMap() {{
            putAll(Map.of(
                "uplinkPort", 100,
                "valueFilterJsonPath", "$.object.deltaVolumes[?(@.delta_period == 12)].value"
            ));
        }})
    ).withUnits(UNITS_METRE, UNITS_CUBED);
    public static final AttributeDescriptor<Double> DELTA_PERIOD_13 = new AttributeDescriptor<>("deltaPeriod13", ValueType.NUMBER,
        new MetaItem<>(MetaItemType.READ_ONLY),
        new MetaItem<>(MetaItemType.AGENT_LINK_CONFIG, new ValueType.ObjectMap() {{
            putAll(Map.of(
                "uplinkPort", 100,
                "valueFilterJsonPath", "$.object.deltaVolumes[?(@.delta_period == 13)].value"
            ));
        }})
    ).withUnits(UNITS_METRE, UNITS_CUBED);
    public static final AttributeDescriptor<Double> DELTA_PERIOD_14 = new AttributeDescriptor<>("deltaPeriod14", ValueType.NUMBER,
        new MetaItem<>(MetaItemType.READ_ONLY),
        new MetaItem<>(MetaItemType.AGENT_LINK_CONFIG, new ValueType.ObjectMap() {{
            putAll(Map.of(
                "uplinkPort", 100,
                "valueFilterJsonPath", "$.object.deltaVolumes[?(@.delta_period == 14)].value"
            ));
        }})
    ).withUnits(UNITS_METRE, UNITS_CUBED);
    public static final AttributeDescriptor<Double> DELTA_PERIOD_15 = new AttributeDescriptor<>("deltaPeriod15", ValueType.NUMBER,
        new MetaItem<>(MetaItemType.READ_ONLY),
        new MetaItem<>(MetaItemType.AGENT_LINK_CONFIG, new ValueType.ObjectMap() {{
            putAll(Map.of(
                "uplinkPort", 100,
                "valueFilterJsonPath", "$.object.deltaVolumes[?(@.delta_period == 15)].value"
            ));
        }})
    ).withUnits(UNITS_METRE, UNITS_CUBED);


    public static final AttributeDescriptor<String> DEV_EUI = new AttributeDescriptor<>("devEUI", ValueType.TEXT, new MetaItem<>(MetaItemType.READ_ONLY));
    public static final AttributeDescriptor<String> VENDOR_ID = new AttributeDescriptor<>("vendorId", ValueType.TEXT, new MetaItem<>(MetaItemType.READ_ONLY));
    public static final AttributeDescriptor<String> MODEL_ID = new AttributeDescriptor<>("modelId", ValueType.TEXT, new MetaItem<>(MetaItemType.READ_ONLY));
    public static final AttributeDescriptor<String> FIRMWARE_VERSION = new AttributeDescriptor<>("firmwareVersion", ValueType.TEXT, new MetaItem<>(MetaItemType.READ_ONLY));
    public static final AttributeDescriptor<Boolean> SUPPORTS_CLASS_C = new AttributeDescriptor<>("supportsClassC", ValueType.BOOLEAN, new MetaItem<>(MetaItemType.READ_ONLY));

    public static final AssetDescriptor<WaterMeterAxiomaQalcosonicW1> DESCRIPTOR = new AssetDescriptor<>("water-outline", "95d0df", WaterMeterAxiomaQalcosonicW1.class);

    /**
     * For use by hydrators (i.e. JPA/Jackson)
     */
    protected WaterMeterAxiomaQalcosonicW1() {
    }

    public WaterMeterAxiomaQalcosonicW1(String name) {
        super(name);
    }

    public Optional<String> getTimestamp() {
        return getAttributes().getValue(TIMESTAMP);
    }

    public Optional<String> getLogTimestamp() {
        return getAttributes().getValue(LOG_TIMESTAMP);
    }

    public Optional<String> getStatus() {
        return getAttributes().getValue(STATUS);
    }

    public Optional<Double> getVolume() {
        return getAttributes().getValue(VOLUME);
    }

    public Optional<Double> getLogVolume() {
        return getAttributes().getValue(LOG_VOLUME);
    }

    public Optional<Double> getDeltaPeriod1() {
        return getAttributes().getValue(DELTA_PERIOD_1);
    }


    public Optional<Double> getDeltaPeriod2() {
        return getAttributes().getValue(DELTA_PERIOD_2);
    }


    public Optional<Double> getDeltaPeriod3() {
        return getAttributes().getValue(DELTA_PERIOD_3);
    }


    public Optional<Double> getDeltaPeriod4() {
        return getAttributes().getValue(DELTA_PERIOD_4);
    }


    public Optional<Double> getDeltaPeriod5() {
        return getAttributes().getValue(DELTA_PERIOD_5);
    }


    public Optional<Double> getDeltaPeriod6() {
        return getAttributes().getValue(DELTA_PERIOD_6);
    }


    public Optional<Double> getDeltaPeriod7() {
        return getAttributes().getValue(DELTA_PERIOD_7);
    }


    public Optional<Double> getDeltaPeriod8() {
        return getAttributes().getValue(DELTA_PERIOD_8);
    }


    public Optional<Double> getDeltaPeriod9() {
        return getAttributes().getValue(DELTA_PERIOD_9);
    }


    public Optional<Double> getDeltaPeriod10() {
        return getAttributes().getValue(DELTA_PERIOD_10);
    }


    public Optional<Double> getDeltaPeriod11() {
        return getAttributes().getValue(DELTA_PERIOD_11);
    }


    public Optional<Double> getDeltaPeriod12() {
        return getAttributes().getValue(DELTA_PERIOD_12);
    }


    public Optional<Double> getDeltaPeriod13() {
        return getAttributes().getValue(DELTA_PERIOD_13);
    }


    public Optional<Double> getDeltaPeriod14() {
        return getAttributes().getValue(DELTA_PERIOD_14);
    }


    public Optional<Double> getDeltaPeriod15() {
        return getAttributes().getValue(DELTA_PERIOD_15);
    }
}
