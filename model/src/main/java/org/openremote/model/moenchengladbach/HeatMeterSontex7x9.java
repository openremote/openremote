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
public class HeatMeterSontex7x9 extends Asset<HeatMeterSontex7x9> {
    public static final AttributeDescriptor<Double> HEAT_ENERGY = new AttributeDescriptor<>("heatEnergy", ValueType.POSITIVE_NUMBER,
        new MetaItem<>(MetaItemType.AGENT_LINK_CONFIG, new ValueType.ObjectMap() {{
            putAll(Map.of(
                "uplinkPort", 2,
                "valueFilterJsonPath", "$.object.heat_energy.value"
            ));
        }})
    ).withUnits(UNITS_KILO, UNITS_WATT, UNITS_HOUR);
    public static final AttributeDescriptor<Double> VOLUME = new AttributeDescriptor<>("volume", ValueType.POSITIVE_NUMBER,
        new MetaItem<>(MetaItemType.AGENT_LINK_CONFIG, new ValueType.ObjectMap() {{
            putAll(Map.of(
                "uplinkPort", 2,
                "valueFilterJsonPath", "$.object.volume.value"
            ));
        }})
    ).withUnits(UNITS_METRE, UNITS_CUBED);
    public static final AttributeDescriptor<Integer> FABRICATION_NUMBER = new AttributeDescriptor<>("fabricationNumber", ValueType.POSITIVE_INTEGER,
        new MetaItem<>(MetaItemType.AGENT_LINK_CONFIG, new ValueType.ObjectMap() {{
            putAll(Map.of(
                "uplinkPort", 2,
                "valueFilterJsonPath", "$.object.fabrication_number.value"
            ));
        }})
    );
    public static final AttributeDescriptor<Boolean> VALID_DATE_AND_TIME = new AttributeDescriptor<>("validDateAndTime", ValueType.BOOLEAN,
        new MetaItem<>(MetaItemType.AGENT_LINK_CONFIG, new ValueType.ObjectMap() {{
            putAll(Map.of(
                "uplinkPort", 2,
                "valueFilterJsonPath", "$.object.valid_date_and_time.value",
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
                "uplinkPort", 2,
                "valueFilterJsonPath", "$.object.daylight_saving_time.value",
                "valueConverter", new ValueType.ObjectMap() {{
                    putAll(Map.of(
                        "TRUE", "true",
                        "FALSE", "false"
                    ));
                }}
            ));
        }})
    );
    public static final AttributeDescriptor<String> DATE_AND_TIME = new AttributeDescriptor<>("dateAndTime", ValueType.TEXT,
        new MetaItem<>(MetaItemType.AGENT_LINK_CONFIG, new ValueType.ObjectMap() {{
            putAll(Map.of(
                "uplinkPort", 2,
                "valueFilterJsonPath", "$.object.date_and_time.value"
            ));
        }})
    );
    public static final AttributeDescriptor<String> DETAILED_ERRORS = new AttributeDescriptor<>("detailedErrors", ValueType.TEXT,
        new MetaItem<>(MetaItemType.AGENT_LINK_CONFIG, new ValueType.ObjectMap() {{
            putAll(Map.of(
                "uplinkPort", 2,
                "valueFilterJsonPath", "$.object.detailed_errors.value"
            ));
        }})
    );
    public static final AttributeDescriptor<String> TARGET_DAY = new AttributeDescriptor<>("targetDay", ValueType.TEXT,
        new MetaItem<>(MetaItemType.AGENT_LINK_CONFIG, new ValueType.ObjectMap() {{
            putAll(Map.of(
                "uplinkPort", 2,
                "valueFilterJsonPath", "$.object.target_day.value"
            ));
        }})
    );
    public static final AttributeDescriptor<Double> ENERGY_TOTALIZER_HEATING_AT_TARGET_DAY = new AttributeDescriptor<>("energyTotalizerHeatingAtTargetDay", ValueType.POSITIVE_NUMBER,
        new MetaItem<>(MetaItemType.AGENT_LINK_CONFIG, new ValueType.ObjectMap() {{
            putAll(Map.of(
                "uplinkPort", 2,
                "valueFilterJsonPath", "$.object.energy_totalizer_heating_at_target_day.value"
            ));
        }})
    ).withUnits(UNITS_KILO, UNITS_WATT, UNITS_HOUR);
    public static final AttributeDescriptor<Double> VOLUME_TOTALIZER_AT_TARGET_DAY = new AttributeDescriptor<>("volumeTotalizerAtTargetDay", ValueType.POSITIVE_NUMBER,
        new MetaItem<>(MetaItemType.AGENT_LINK_CONFIG, new ValueType.ObjectMap() {{
            putAll(Map.of(
                "uplinkPort", 2,
                "valueFilterJsonPath", "$.object.volume_totalizer_at_target_day.value"
            ));
        }})
    ).withUnits(UNITS_METRE, UNITS_CUBED);
    public static final AttributeDescriptor<Double> HIGH_TEMPERATURE = new AttributeDescriptor<>("highTemperature", ValueType.NUMBER,
        new MetaItem<>(MetaItemType.AGENT_LINK_CONFIG, new ValueType.ObjectMap() {{
            putAll(Map.of(
                "uplinkPort", 2,
                "valueFilterJsonPath", "$.object.high_temperature.value"
            ));
        }})
    ).withUnits(UNITS_CELSIUS);
    public static final AttributeDescriptor<Double> LOW_TEMPERATURE = new AttributeDescriptor<>("lowTemperature", ValueType.NUMBER,
        new MetaItem<>(MetaItemType.AGENT_LINK_CONFIG, new ValueType.ObjectMap() {{
            putAll(Map.of(
                "uplinkPort", 2,
                "valueFilterJsonPath", "$.object.low_temperature.value"
            ));
        }})
    ).withUnits(UNITS_CELSIUS);
    public static final AttributeDescriptor<Double> FLOW = new AttributeDescriptor<>("flow", ValueType.POSITIVE_NUMBER,
        new MetaItem<>(MetaItemType.AGENT_LINK_CONFIG, new ValueType.ObjectMap() {{
            putAll(Map.of(
                "uplinkPort", 2,
                "valueFilterJsonPath", "$.object.flow.value"
            ));
        }})
    ).withUnits(UNITS_METRE, UNITS_CUBED, UNITS_PER, UNITS_HOUR);
    public static final AttributeDescriptor<Double> POWER = new AttributeDescriptor<>("power", ValueType.POSITIVE_NUMBER,
        new MetaItem<>(MetaItemType.AGENT_LINK_CONFIG, new ValueType.ObjectMap() {{
            putAll(Map.of(
                "uplinkPort", 2,
                "valueFilterJsonPath", "$.object.power.value"
            ));
        }})
    ).withUnits(UNITS_KILO, UNITS_WATT);

    public static final AttributeDescriptor<String> DEV_EUI = new AttributeDescriptor<>("devEUI", ValueType.TEXT, new MetaItem<>(MetaItemType.READ_ONLY));
    public static final AttributeDescriptor<String> VENDOR_ID = new AttributeDescriptor<>("vendorId", ValueType.TEXT, new MetaItem<>(MetaItemType.READ_ONLY));
    public static final AttributeDescriptor<String> MODEL_ID = new AttributeDescriptor<>("modelId", ValueType.TEXT, new MetaItem<>(MetaItemType.READ_ONLY));
    public static final AttributeDescriptor<String> FIRMWARE_VERSION = new AttributeDescriptor<>("firmwareVersion", ValueType.TEXT, new MetaItem<>(MetaItemType.READ_ONLY));
    public static final AttributeDescriptor<Boolean> SUPPORTS_CLASS_C = new AttributeDescriptor<>("supportsClassC", ValueType.BOOLEAN, new MetaItem<>(MetaItemType.READ_ONLY));

    public static final AssetDescriptor<HeatMeterSontex7x9> DESCRIPTOR = new AssetDescriptor<>("fire", "e6688a", HeatMeterSontex7x9.class);

    /**
     * For use by hydrators (i.e. JPA/Jackson)
     */
    protected HeatMeterSontex7x9() {
    }

    public HeatMeterSontex7x9(String name) {
        super(name);
    }

    public Optional<Double> getHeatEnergy() {
        return getAttributes().getValue(HEAT_ENERGY);
    }

    public Optional<Double> getVolume() {
        return getAttributes().getValue(VOLUME);
    }

    public Optional<Integer> getFabricationNumber() {
        return getAttributes().getValue(FABRICATION_NUMBER);
    }

    public Optional<Boolean> isValidDateAndTime() {
        return getAttributes().getValue(VALID_DATE_AND_TIME);
    }

    public Optional<Boolean> isDaylightSavingTime() {
        return getAttributes().getValue(DAYLIGHT_SAVING_TIME);
    }

    public Optional<String> getDateAndTime() {
        return getAttributes().getValue(DATE_AND_TIME);
    }

    public Optional<String> getDetailedErrors() {
        return getAttributes().getValue(DETAILED_ERRORS);
    }

    public Optional<String> getTargetDay() {
        return getAttributes().getValue(TARGET_DAY);
    }

    public Optional<Double> getEnergyTotalizerHeatingAtTargetDay() {
        return getAttributes().getValue(ENERGY_TOTALIZER_HEATING_AT_TARGET_DAY);
    }

    public Optional<Double> getVolumeTotalizerAtTargetDay() {
        return getAttributes().getValue(VOLUME_TOTALIZER_AT_TARGET_DAY);
    }

    public Optional<Double> getHighTemperature() {
        return getAttributes().getValue(HIGH_TEMPERATURE);
    }

    public Optional<Double> getLowTemperature() {
        return getAttributes().getValue(LOW_TEMPERATURE);
    }

    public Optional<Double> getFlow() {
        return getAttributes().getValue(FLOW);
    }

    public Optional<Double> getPower() {
        return getAttributes().getValue(POWER);
    }
}
