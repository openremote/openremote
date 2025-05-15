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
public class HeatCostAllocatorSontex868 extends Asset<HeatCostAllocatorSontex868> {
    public static final AttributeDescriptor<Boolean> VALID_DATE_AND_TIME = new AttributeDescriptor<>("validDateAndTime", ValueType.BOOLEAN,
        new MetaItem<>(MetaItemType.AGENT_LINK_CONFIG, new ValueType.ObjectMap() {{
            putAll(Map.of(
                "uplinkPort", 1,
                "valueFilterJsonPath", "$.object.valid_date_and_time",
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
    public static final AttributeDescriptor<String> DATE_AND_TIME = new AttributeDescriptor<>("dateAndTime", ValueType.TEXT,
        new MetaItem<>(MetaItemType.AGENT_LINK_CONFIG, new ValueType.ObjectMap() {{
            putAll(Map.of(
                "uplinkPort", 1,
                "valueFilterJsonPath", "$.object.date_and_time.value"
            ));
        }})
    );
    public static final AttributeDescriptor<Double> TOTALIZER_HEATING = new AttributeDescriptor<>("totalizerHeating", ValueType.POSITIVE_NUMBER,
        new MetaItem<>(MetaItemType.AGENT_LINK_CONFIG, new ValueType.ObjectMap() {{
            putAll(Map.of(
                "uplinkPort", 1,
                "valueFilterJsonPath", "$.object.totalizer_heating.value"
            ));
        }})
    );
    public static final AttributeDescriptor<String> SET_DAY = new AttributeDescriptor<>("setDay", ValueType.TEXT,
        new MetaItem<>(MetaItemType.AGENT_LINK_CONFIG, new ValueType.ObjectMap() {{
            putAll(Map.of(
                "uplinkPort", 1,
                "valueFilterJsonPath", "$.object.set_day.value"
            ));
        }})
    );
    public static final AttributeDescriptor<Double> HEAT_ENERGY = new AttributeDescriptor<>("heatEnergy", ValueType.POSITIVE_NUMBER,
        new MetaItem<>(MetaItemType.AGENT_LINK_CONFIG, new ValueType.ObjectMap() {{
            putAll(Map.of(
                "uplinkPort", 1,
                "valueFilterJsonPath", "$.object.heat_energy.value"
            ));
        }})
    );
    public static final AttributeDescriptor<Double> RADIATOR_MAX_TEMPERATURE_CURRENT_PERIOD = new AttributeDescriptor<>("radiatorMaxTemperatureCurrentPeriod", ValueType.NUMBER,
        new MetaItem<>(MetaItemType.AGENT_LINK_CONFIG, new ValueType.ObjectMap() {{
            putAll(Map.of(
                "uplinkPort", 1,
                "valueFilterJsonPath", "$.object.radiator_max_temperature_current_period.value"
            ));
        }})
    ).withUnits(UNITS_CELSIUS);
    public static final AttributeDescriptor<Double> RADIATOR_MAX_TEMPERATURE_PREVIOUS_PERIOD = new AttributeDescriptor<>("radiatorMaxTemperaturePreviousPeriod", ValueType.NUMBER,
        new MetaItem<>(MetaItemType.AGENT_LINK_CONFIG, new ValueType.ObjectMap() {{
            putAll(Map.of(
                "uplinkPort", 1,
                "valueFilterJsonPath", "$.object.radiator_max_temperature_previous_period.value"
            ));
        }})
    ).withUnits(UNITS_CELSIUS);
    public static final AttributeDescriptor<Double> UNITS_FACTOR_KC = new AttributeDescriptor<>("unitsFactorKc", ValueType.NUMBER,
        new MetaItem<>(MetaItemType.AGENT_LINK_CONFIG, new ValueType.ObjectMap() {{
            putAll(Map.of(
                "uplinkPort", 1,
                "valueFilterJsonPath", "$.object.units_factor_kc.value"
            ));
        }})
    );
    public static final AttributeDescriptor<Double> UNITS_FACTOR_KQ = new AttributeDescriptor<>("unitsFactorKq", ValueType.NUMBER,
        new MetaItem<>(MetaItemType.AGENT_LINK_CONFIG, new ValueType.ObjectMap() {{
            putAll(Map.of(
                "uplinkPort", 1,
                "valueFilterJsonPath", "$.object.units_factor_kq.value"
            ));
        }})
    );
    public static final AttributeDescriptor<Double> RADIATOR_TEMPERATURE = new AttributeDescriptor<>("radiatorTemperature", ValueType.NUMBER,
        new MetaItem<>(MetaItemType.AGENT_LINK_CONFIG, new ValueType.ObjectMap() {{
            putAll(Map.of(
                "uplinkPort", 1,
                "valueFilterJsonPath", "$.object.radiator_temperature.value"
            ));
        }})
    ).withUnits(UNITS_CELSIUS);
    public static final AttributeDescriptor<Double> FRAUD_DURATION = new AttributeDescriptor<>("fraudDuration", ValueType.POSITIVE_NUMBER,
        new MetaItem<>(MetaItemType.AGENT_LINK_CONFIG, new ValueType.ObjectMap() {{
            putAll(Map.of(
                "uplinkPort", 1,
                "valueFilterJsonPath", "$.object.fraud_duration.value"
            ));
        }})
    );
    public static final AttributeDescriptor<String> DATE_OF_LAST_FRAUD = new AttributeDescriptor<>("dateOfLastFraud", ValueType.TEXT,
        new MetaItem<>(MetaItemType.AGENT_LINK_CONFIG, new ValueType.ObjectMap() {{
            putAll(Map.of(
                "uplinkPort", 1,
                "valueFilterJsonPath", "$.object.date_of_last_fraud.value"
            ));
        }})
    );
    public static final AttributeDescriptor<String> COMMISSIONING_DATE = new AttributeDescriptor<>("commissioningDate", ValueType.TEXT,
        new MetaItem<>(MetaItemType.AGENT_LINK_CONFIG, new ValueType.ObjectMap() {{
            putAll(Map.of(
                "uplinkPort", 1,
                "valueFilterJsonPath", "$.object.commissioning_date.value"
            ));
        }})
    );
    public static final AttributeDescriptor<Integer> INTERNAL_VERSION = new AttributeDescriptor<>("internalVersion", ValueType.POSITIVE_INTEGER,
        new MetaItem<>(MetaItemType.AGENT_LINK_CONFIG, new ValueType.ObjectMap() {{
            putAll(Map.of(
                "uplinkPort", 1,
                "valueFilterJsonPath", "$.object.internal_version.value"
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
    public static final AttributeDescriptor<String> STATE_OF_PARAMETERS = new AttributeDescriptor<>("stateOfParameters", ValueType.TEXT,
        new MetaItem<>(MetaItemType.AGENT_LINK_CONFIG, new ValueType.ObjectMap() {{
            putAll(Map.of(
                "uplinkPort", 1,
                "valueFilterJsonPath", "$.object.state_of_parameters"
            ));
        }})
    );
    public static final AttributeDescriptor<Integer> FABRICATION_NUMBER = new AttributeDescriptor<>("fabricationNumber", ValueType.POSITIVE_INTEGER,
        new MetaItem<>(MetaItemType.AGENT_LINK_CONFIG, new ValueType.ObjectMap() {{
            putAll(Map.of(
                "uplinkPort", 1,
                "valueFilterJsonPath", "$.object.fabrication_number.value"
            ));
        }})
    );
    public static final AttributeDescriptor<Integer> FRAUD_COUNTER = new AttributeDescriptor<>("fraudCounter", ValueType.POSITIVE_INTEGER,
        new MetaItem<>(MetaItemType.AGENT_LINK_CONFIG, new ValueType.ObjectMap() {{
            putAll(Map.of(
                "uplinkPort", 1,
                "valueFilterJsonPath", "$.object.fraud_counter.value"
            ));
        }})
    );

    public static final AttributeDescriptor<String> DEV_EUI = new AttributeDescriptor<>("devEUI", ValueType.TEXT, new MetaItem<>(MetaItemType.READ_ONLY));
    public static final AttributeDescriptor<String> VENDOR_ID = new AttributeDescriptor<>("vendorId", ValueType.TEXT, new MetaItem<>(MetaItemType.READ_ONLY));
    public static final AttributeDescriptor<String> MODEL_ID = new AttributeDescriptor<>("modelId", ValueType.TEXT, new MetaItem<>(MetaItemType.READ_ONLY));
    public static final AttributeDescriptor<String> FIRMWARE_VERSION = new AttributeDescriptor<>("firmwareVersion", ValueType.TEXT, new MetaItem<>(MetaItemType.READ_ONLY));
    public static final AttributeDescriptor<Boolean> SUPPORTS_CLASS_C = new AttributeDescriptor<>("supportsClassC", ValueType.BOOLEAN, new MetaItem<>(MetaItemType.READ_ONLY));

    public static final AssetDescriptor<HeatCostAllocatorSontex868> DESCRIPTOR = new AssetDescriptor<>("fire", "e6688a", HeatCostAllocatorSontex868.class);

    /**
     * For use by hydrators (i.e. JPA/Jackson)
     */
    protected HeatCostAllocatorSontex868() {
    }

    public HeatCostAllocatorSontex868(String name) {
        super(name);
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

    public Optional<Double> getTotalizerHeating() {
        return getAttributes().getValue(TOTALIZER_HEATING);
    }

    public Optional<String> getSetDay() {
        return getAttributes().getValue(SET_DAY);
    }

    public Optional<Double> getHeatEnergy() {
        return getAttributes().getValue(HEAT_ENERGY);
    }

    public Optional<Double> getRadiatorMaxTemperatureCurrentPeriod() {
        return getAttributes().getValue(RADIATOR_MAX_TEMPERATURE_CURRENT_PERIOD);
    }

    public Optional<Double> getRadiatorMaxTemperaturePreviousPeriod() {
        return getAttributes().getValue(RADIATOR_MAX_TEMPERATURE_PREVIOUS_PERIOD);
    }

    public Optional<Double> getUnitsFactorKc() {
        return getAttributes().getValue(UNITS_FACTOR_KC);
    }

    public Optional<Double> getUnitsFactorKq() {
        return getAttributes().getValue(UNITS_FACTOR_KQ);
    }

    public Optional<Double> getRadiatorTemperature() {
        return getAttributes().getValue(RADIATOR_TEMPERATURE);
    }

    public Optional<Double> getFraudDuration() {
        return getAttributes().getValue(FRAUD_DURATION);
    }

    public Optional<String> getDateOfLastFraud() {
        return getAttributes().getValue(DATE_OF_LAST_FRAUD);
    }

    public Optional<String> getCommissioningDate() {
        return getAttributes().getValue(COMMISSIONING_DATE);
    }

    public Optional<Integer> getInternalVersion() {
        return getAttributes().getValue(INTERNAL_VERSION);
    }

    public Optional<String> getDetailedErrors() {
        return getAttributes().getValue(DETAILED_ERRORS);
    }

    public Optional<String> getStateOfParameters() {
        return getAttributes().getValue(STATE_OF_PARAMETERS);
    }

    public Optional<Integer> getFabricationNumber() {
        return getAttributes().getValue(FABRICATION_NUMBER);
    }

    public Optional<Integer> getFraudCounter() {
        return getAttributes().getValue(FRAUD_COUNTER);
    }
}
