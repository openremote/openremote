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
public class WaterMeterSontexSQ1 extends Asset<WaterMeterSontexSQ1> {
    public static final AttributeDescriptor<Double> VOLUME_TOTALIZER = new AttributeDescriptor<>("volumeTotalizer", ValueType.POSITIVE_NUMBER,
        new MetaItem<>(MetaItemType.AGENT_LINK_CONFIG, new ValueType.ObjectMap() {{
            putAll(Map.of(
                "uplinkPort", 1,
                "valueFilterJsonPath", "$.object.volume_totalizer.value"
            ));
        }})
    ).withUnits(UNITS_METRE, UNITS_CUBED);
    public static final AttributeDescriptor<Double> VOLUME_TOTALIZER_AT_SET_DAY = new AttributeDescriptor<>("volumeTotalizerAtSetDay", ValueType.POSITIVE_NUMBER,
        new MetaItem<>(MetaItemType.AGENT_LINK_CONFIG, new ValueType.ObjectMap() {{
            putAll(Map.of(
                "uplinkPort", 1,
                "valueFilterJsonPath", "$.object.volume_totalizer_at_set_day.value"
            ));
        }})
    ).withUnits(UNITS_METRE, UNITS_CUBED);
    public static final AttributeDescriptor<String> SET_DAY = new AttributeDescriptor<>("setDay", ValueType.TEXT,
        new MetaItem<>(MetaItemType.AGENT_LINK_CONFIG, new ValueType.ObjectMap() {{
            putAll(Map.of(
                "uplinkPort", 1,
                "valueFilterJsonPath", "$.object.set_day"
            ));
        }})
    );
    public static final AttributeDescriptor<Integer> FABRICATION_NUMBER = new AttributeDescriptor<>("fabricationNumber", ValueType.POSITIVE_INTEGER,
        new MetaItem<>(MetaItemType.AGENT_LINK_CONFIG, new ValueType.ObjectMap() {{
            putAll(Map.of(
                "uplinkPort", 1,
                "valueFilterJsonPath", "$.object.fabrication_number"
            ));
        }})
    );
    public static final AttributeDescriptor<Integer> INTERNAL_VERSION = new AttributeDescriptor<>("internalVersion", ValueType.POSITIVE_INTEGER,
        new MetaItem<>(MetaItemType.AGENT_LINK_CONFIG, new ValueType.ObjectMap() {{
            putAll(Map.of(
                "uplinkPort", 1,
                "valueFilterJsonPath", "$.object.internal_version"
            ));
        }})
    );
    public static final AttributeDescriptor<String> COMMISSIONING_DAY = new AttributeDescriptor<>("commissioningDay", ValueType.TEXT,
        new MetaItem<>(MetaItemType.AGENT_LINK_CONFIG, new ValueType.ObjectMap() {{
            putAll(Map.of(
                "uplinkPort", 1,
                "valueFilterJsonPath", "$.object.commissioning_day"
            ));
        }})
    );
    public static final AttributeDescriptor<String> CURRENT_DATE_AND_TIME = new AttributeDescriptor<>("currentDateAndTime", ValueType.TEXT,
        new MetaItem<>(MetaItemType.AGENT_LINK_CONFIG, new ValueType.ObjectMap() {{
            putAll(Map.of(
                "uplinkPort", 1,
                "valueFilterJsonPath", "$.object.current_date_and_time"
            ));
        }})
    );
    public static final AttributeDescriptor<Boolean> DATE_AND_TIME_VALID = new AttributeDescriptor<>("dateAndTimeValid", ValueType.BOOLEAN,
        new MetaItem<>(MetaItemType.AGENT_LINK_CONFIG, new ValueType.ObjectMap() {{
            putAll(Map.of(
                "uplinkPort", 1,
                "valueFilterJsonPath", "$.object.date_and_time_valid",
                "valueConverter", new ValueType.ObjectMap() {{
                    putAll(Map.of(
                        "TRUE", "true",
                        "FALSE", "false"
                    ));
                }}
            ));
        }})
    );
    public static final AttributeDescriptor<Boolean> DAYLIGHT_SAVING_TIME = new AttributeDescriptor<>("daylightSavingTime", ValueType.BOOLEAN,
        new MetaItem<>(MetaItemType.AGENT_LINK_CONFIG, new ValueType.ObjectMap() {{
            putAll(Map.of(
                "uplinkPort", 1,
                "valueFilterJsonPath", "$.object.daylight_saving_time",
                "valueConverter", new ValueType.ObjectMap() {{
                    putAll(Map.of(
                        "TRUE", "true",
                        "FALSE", "false"
                    ));
                }}
            ));
        }})
    );
    public static final AttributeDescriptor<String> DETAILED_ERRORS = new AttributeDescriptor<>("detailedErrors", ValueType.TEXT,
        new MetaItem<>(MetaItemType.AGENT_LINK_CONFIG, new ValueType.ObjectMap() {{
            putAll(Map.of(
                "uplinkPort", 1,
                "valueFilterJsonPath", "$.object.detailed_errors"
            ));
        }})
    );

    public static final AttributeDescriptor<String> DEV_EUI = new AttributeDescriptor<>("devEUI", ValueType.TEXT, new MetaItem<>(MetaItemType.READ_ONLY));
    public static final AttributeDescriptor<String> VENDOR_ID = new AttributeDescriptor<>("vendorId", ValueType.TEXT, new MetaItem<>(MetaItemType.READ_ONLY));
    public static final AttributeDescriptor<String> MODEL_ID = new AttributeDescriptor<>("modelId", ValueType.TEXT, new MetaItem<>(MetaItemType.READ_ONLY));
    public static final AttributeDescriptor<String> FIRMWARE_VERSION = new AttributeDescriptor<>("firmwareVersion", ValueType.TEXT, new MetaItem<>(MetaItemType.READ_ONLY));
    public static final AttributeDescriptor<Boolean> SUPPORTS_CLASS_C = new AttributeDescriptor<>("supportsClassC", ValueType.BOOLEAN, new MetaItem<>(MetaItemType.READ_ONLY));

    public static final AssetDescriptor<WaterMeterSontexSQ1> DESCRIPTOR = new AssetDescriptor<>("water-outline", "95d0df", WaterMeterSontexSQ1.class);

    /**
     * For use by hydrators (i.e. JPA/Jackson)
     */
    protected WaterMeterSontexSQ1() {
    }

    public WaterMeterSontexSQ1(String name) {
        super(name);
        setShowOnMap(false);
    }

    public WaterMeterSontexSQ1 setShowOnMap(boolean showOnMap) {
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

    public Optional<Double> getVolumeTotalizer() {
        return getAttributes().getValue(VOLUME_TOTALIZER);
    }

    public Optional<Double> getVolumeTotalizerAtSetDay() {
        return getAttributes().getValue(VOLUME_TOTALIZER_AT_SET_DAY);
    }

    public Optional<String> getSetDay() {
        return getAttributes().getValue(SET_DAY);
    }

    public Optional<Integer> getFabricationNumber() {
        return getAttributes().getValue(FABRICATION_NUMBER);
    }

    public Optional<Integer> getInternalVersion() {
        return getAttributes().getValue(INTERNAL_VERSION);
    }

    public Optional<String> getCommissioningDay() {
        return getAttributes().getValue(COMMISSIONING_DAY);
    }

    public Optional<String> getCurrentDateAndTime() {
        return getAttributes().getValue(CURRENT_DATE_AND_TIME);
    }

    public Optional<Boolean> isDateAndTimeValid() {
        return getAttributes().getValue(DATE_AND_TIME_VALID);
    }

    public Optional<Boolean> isDaylightSavingTime() {
        return getAttributes().getValue(DAYLIGHT_SAVING_TIME);
    }

    public Optional<String> getDetailedErrors() {
        return getAttributes().getValue(DETAILED_ERRORS);
    }
}
