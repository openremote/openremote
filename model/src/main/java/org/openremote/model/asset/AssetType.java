/*
 * Copyright 2017, OpenRemote Inc.
 *
 * See the CONTRIBUTORS.txt file in the distribution for a
 * full listing of individual contributors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.openremote.model.asset;

import org.openremote.model.attribute.AttributeDescriptor;
import org.openremote.model.attribute.AttributeDescriptorImpl;
import org.openremote.model.value.Values;

import java.util.Arrays;
import java.util.Optional;

import static org.openremote.model.Constants.*;
import static org.openremote.model.attribute.AttributeType.*;
import static org.openremote.model.attribute.AttributeValueType.*;
import static org.openremote.model.attribute.MetaItemType.*;

/**
 * Asset type is an arbitrary string. It should be URI. This enum contains the well-known URIs for functionality we want
 * to depend on in our platform.
 * <p>
 * TODO https://people.eecs.berkeley.edu/~arka/papers/buildsys2015_metadatasurvey.pdf
 */
public enum AssetType implements AssetDescriptor {

    BUILDING(ASSET_NAMESPACE + ":building", "office-building", "4b5966",
        SURFACE_AREA,
        GEO_STREET,
        GEO_CITY,
        GEO_COUNTRY,
        GEO_POSTAL_CODE),

    CITY(ASSET_NAMESPACE + ":city", "city", null,
        GEO_CITY,
        GEO_COUNTRY),

    AREA(ASSET_NAMESPACE + ":area", "home-city", "095F6B",
        GEO_CITY,
        GEO_COUNTRY,
        GEO_POSTAL_CODE),

    FLOOR(ASSET_NAMESPACE + ":floor", "stairs", null),

    RESIDENCE(ASSET_NAMESPACE + ":residence", "home", "4c879b"),

    ROOM(ASSET_NAMESPACE + ":room", "door", "2eaaa2"),

    AGENT(ASSET_NAMESPACE + ":agent", "cogs", null),

    CONSOLE(ASSET_NAMESPACE + ":console", "monitor-cellphone", null),

    MICROPHONE(ASSET_NAMESPACE + ":microphone", "microphone", "47A5FF",
        new AttributeDescriptorImpl("microphoneLevel", SOUND, null,
            LABEL.withInitialValue(Values.create("Microphone Level")),
            READ_ONLY
        )
    ),

    SOUND_EVENT(ASSET_NAMESPACE + ":soundEvent", "surround-sound", "5BBBD1",
        new AttributeDescriptorImpl("lastAlarmEvent", OBJECT, null,
            LABEL.withInitialValue(Values.create("Last alarm event")),
            READ_ONLY),
        new AttributeDescriptorImpl("lastAggressionEvent", OBJECT, null,
            LABEL.withInitialValue(Values.create("Last aggression event")),
            READ_ONLY),
        new AttributeDescriptorImpl("lastBreakingGlassEvent", OBJECT, null,
            LABEL.withInitialValue(Values.create("Last breaking glass event")),
            READ_ONLY),
        new AttributeDescriptorImpl("lastGunshotEvent", OBJECT, null,
            LABEL.withInitialValue(Values.create("Last gunshot event")),
            READ_ONLY),
        new AttributeDescriptorImpl("lastIntensityEvent", OBJECT, null,
            LABEL.withInitialValue(Values.create("Last intensity event")),
            READ_ONLY),
        new AttributeDescriptorImpl("lastEvent", OBJECT, null,
            LABEL.withInitialValue(Values.create("Last event")),
            READ_ONLY)
    ),

    ENVIRONMENT_SENSOR(ASSET_NAMESPACE + ":enviroment", "periodic-table-co2", "f18546",
        new AttributeDescriptorImpl("temperature", TEMPERATURE, null,
            LABEL.withInitialValue(Values.create("Temperature")),
            READ_ONLY
        ),
        new AttributeDescriptorImpl("nO2", DENSITY, null,
            LABEL.withInitialValue(Values.create("Nitrogen Level")),
            UNIT_TYPE.withInitialValue(Values.create(UNITS_DENSITY_MICROGRAMS_CUBIC_M)),
            READ_ONLY
        ),
        new AttributeDescriptorImpl("relHumidity", PERCENTAGE, null,
            LABEL.withInitialValue(Values.create("Humidity")),
            READ_ONLY
        ),
        new AttributeDescriptorImpl("ozone", DENSITY, null,
            LABEL.withInitialValue(Values.create("Ozone Level")),
            UNIT_TYPE.withInitialValue(Values.create(UNITS_DENSITY_MICROGRAMS_CUBIC_M)),
            READ_ONLY
        ),
        new AttributeDescriptorImpl("particlesPM1", DENSITY, null,
            LABEL.withInitialValue(Values.create("Particles PM1")),
            UNIT_TYPE.withInitialValue(Values.create(UNITS_DENSITY_MICROGRAMS_CUBIC_M)),
            READ_ONLY
        ),
        new AttributeDescriptorImpl("particlesPM2_5", DENSITY, null,
            LABEL.withInitialValue(Values.create("Particles PM2.5")),
            UNIT_TYPE.withInitialValue(Values.create(UNITS_DENSITY_MICROGRAMS_CUBIC_M)),
            READ_ONLY
        ),
        new AttributeDescriptorImpl("particlesPM10", DENSITY, null,
            LABEL.withInitialValue(Values.create("Particles PM10")),
            UNIT_TYPE.withInitialValue(Values.create(UNITS_DENSITY_MICROGRAMS_CUBIC_M)),
            READ_ONLY
        )
    ),

    LIGHT(ASSET_NAMESPACE + ":light", "lightbulb", "e6688a",
        new AttributeDescriptorImpl("lightStatus", SWITCH_TOGGLE, null,
            LABEL.withInitialValue(Values.create("Light status")),
            DESCRIPTION.withInitialValue(Values.create("Indicates if the light is on or off"))
        ),
        new AttributeDescriptorImpl("lightDimLevel", PERCENTAGE, null,
            LABEL.withInitialValue(Values.create("Light Dim Level")),
            DESCRIPTION.withInitialValue(Values.create("The level of dimming of the light"))
        ),
        new AttributeDescriptorImpl("colorRGBW", COLOR_RGBW, null,
            LABEL.withInitialValue(Values.create("Color RGBW")),
            DESCRIPTION.withInitialValue(Values.create("The RGBW color of the light"))
        ),
        new AttributeDescriptorImpl("groupNumber", NUMBER, null,
            LABEL.withInitialValue(Values.create("Group number")),
            DESCRIPTION.withInitialValue(Values.create("Which group this light belongs to"))
        ),
        new AttributeDescriptorImpl("Scenario", STRING, null,
            LABEL.withInitialValue(Values.create("Scenario")),
            DESCRIPTION.withInitialValue(Values.create("The scenario the light are setup to"))
        )
    ),

    LIGHT_CONTROLLER(ASSET_NAMESPACE + ":lightController", "light-switch", "9e5de2",
        new AttributeDescriptorImpl("lightAllStatus", SWITCH_TOGGLE, null,
            LABEL.withInitialValue(Values.create("Light all status")),
            DESCRIPTION.withInitialValue(Values.create("Indicates if all lights are on or off"))
        ),
        new AttributeDescriptorImpl("lightAllDimLevel", PERCENTAGE, null,
            LABEL.withInitialValue(Values.create("Light Dim Level")),
            DESCRIPTION.withInitialValue(Values.create("The level of dimming of all the lights"))
        ),
        new AttributeDescriptorImpl("colorAllRGBW", COLOR_RGBW, null,
            LABEL.withInitialValue(Values.create("Color RGBW")),
            DESCRIPTION.withInitialValue(Values.create("The RGBW color of all the lights"))
        ),
        new AttributeDescriptorImpl("Scenario", STRING, null,
            LABEL.withInitialValue(Values.create("Scenario")),
            DESCRIPTION.withInitialValue(Values.create("The scenario the lights are setup to"))
        )
    ),

    PEOPLE_COUNTER(ASSET_NAMESPACE + ":peopleCounter", "account-multiple", "4b5966", false,
        new AttributeDescriptorImpl("peopleCountIn", INTEGER, null,
            LABEL.withInitialValue(Values.create("People Count In")),
            DESCRIPTION.withInitialValue(Values.create("Cumulative number of people going into area")),
            READ_ONLY
        ),
        new AttributeDescriptorImpl("peopleCountOut", INTEGER, null,
            LABEL.withInitialValue(Values.create("People Count Out")),
            DESCRIPTION.withInitialValue(Values.create("Cumulative number of people leaving area")),
            READ_ONLY
        ),
        new AttributeDescriptorImpl("peopleCountInMinute", INTEGER, null,
            LABEL.withInitialValue(Values.create("People Count In Minute")),
            DESCRIPTION.withInitialValue(Values.create("Number of people going into area per minute")),
            UNIT_TYPE.withInitialValue(UNITS_COUNT_PER_MINUTE),
            READ_ONLY
        ),
        new AttributeDescriptorImpl("peopleCountOutMinute", INTEGER, null,
            LABEL.withInitialValue(Values.create("People Count Out Minute")),
            DESCRIPTION.withInitialValue(Values.create("Number of people leaving area per minute")),
            UNIT_TYPE.withInitialValue(UNITS_COUNT_PER_MINUTE),
            READ_ONLY
        ),
        new AttributeDescriptorImpl("peopleCountTotal", INTEGER, null,
            LABEL.withInitialValue(Values.create("People Count Total")),
            DESCRIPTION.withInitialValue(Values.create("cameraCountIn - cameraCountOut")),
            READ_ONLY
        ),
        new AttributeDescriptorImpl("peopleCountGrowth", INTEGER, null,
            LABEL.withInitialValue(Values.create("People Count Growth")),
            DESCRIPTION.withInitialValue(Values.create("cameraCountIn - cameraCountOut")),
            READ_ONLY
        )
    ),

    GATEWAY(ASSET_NAMESPACE + ":gateway", "router-wireless", null,
        new AttributeDescriptorImpl("clientId", STRING, null,
            LABEL.withInitialValue(Values.create("Client ID")),
            DESCRIPTION.withInitialValue(Values.create("Client ID for gateway to authorise")),
            READ_ONLY
        ),
        new AttributeDescriptorImpl("clientSecret", STRING, null,
            LABEL.withInitialValue(Values.create("Client Secret")),
            DESCRIPTION.withInitialValue(Values.create("Client secret for gateway to authorise")),
            READ_ONLY,
            SECRET
        ),
        new AttributeDescriptorImpl("status", CONNECTION_STATUS, null,
            LABEL.withInitialValue(Values.create("Connection status")),
            DESCRIPTION.withInitialValue(Values.create("Connection status of the gateway")),
            READ_ONLY
        ),
        new AttributeDescriptorImpl("disabled", BOOLEAN, null)
    ),

    GROUP(ASSET_NAMESPACE + ":group", "folder", "B3B3B3",
        new AttributeDescriptorImpl("childAssetType", STRING, null, READ_ONLY)
    ),

    THING(ASSET_NAMESPACE + ":thing", "cube-outline", null),

    ELECTRICITY_CONSUMER(ASSET_NAMESPACE + ":electricityConsumer", "power-plug", "8A293D", false,
        new AttributeDescriptorImpl("manufacturer", STRING, null,
            LABEL.withInitialValue("Manufacturer")),
        new AttributeDescriptorImpl("model", STRING, null,
            LABEL.withInitialValue("Model")),
        new AttributeDescriptorImpl("userNotes", STRING, null,
            LABEL.withInitialValue("User notes")),
        new AttributeDescriptorImpl("status", STRING, null,
            LABEL.withInitialValue("Status"),
            DESCRIPTION.withInitialValue("Status system"),
            READ_ONLY),
        new AttributeDescriptorImpl("demandResponseType", STRING, null,
            LABEL.withInitialValue("Demand response type"),
            ALLOWED_VALUES.withInitialValue(Values.createArray().addAll(Arrays.stream(ElectricityConsumerDemandResponseType.values()).map(Enum::name).toArray(String[]::new)))),
        new AttributeDescriptorImpl("energyTariffImport", CURRENCY, null,
            LABEL.withInitialValue("Energy tariff import"),
            UNIT_TYPE.withInitialValue(UNITS_EUR_PER_KILOWATT_HOUR)),
        new AttributeDescriptorImpl("energyTariffExport", CURRENCY, null,
            LABEL.withInitialValue("Energy tariff export"),
            UNIT_TYPE.withInitialValue(UNITS_EUR_PER_KILOWATT_HOUR)),
        new AttributeDescriptorImpl("energyCarbonImport", ENERGY, null,
            LABEL.withInitialValue("Energy carbon import"),
            UNIT_TYPE.withInitialValue(UNITS_KILOGRAM_CARBON_PER_KILOWATT_HOUR)),
        new AttributeDescriptorImpl("energyCarbonExport", ENERGY, null,
            LABEL.withInitialValue("Energy carbon export"),
            UNIT_TYPE.withInitialValue(UNITS_KILOGRAM_CARBON_PER_KILOWATT_HOUR)),
        new AttributeDescriptorImpl("totalPower", POWER, null,
            LABEL.withInitialValue("Total power"),
            READ_ONLY),
        new AttributeDescriptorImpl("forecastPowerDeviation", POWER, null,
            LABEL.withInitialValue("Forecast power deviation"),
            READ_ONLY),
        new AttributeDescriptorImpl("setPointPower", POWER, null,
            LABEL.withInitialValue("Setpoint power"),
            DESCRIPTION.withInitialValue("Positive is consuming, negative is delivering to network")),
        new AttributeDescriptorImpl("availableMaxPower", POWER, null,
            LABEL.withInitialValue("Available max power"),
            READ_ONLY),
        new AttributeDescriptorImpl("availableMinPower", POWER, null,
            LABEL.withInitialValue("Available min power"),
            READ_ONLY),
        new AttributeDescriptorImpl("totalEnergy", ENERGY, null,
            LABEL.withInitialValue("Total energy"),
            UNIT_TYPE.withInitialValue(UNITS_ENERGY_KILOWATT_HOUR),
            READ_ONLY)
    ),

    ELECTRICITY_PRODUCER(ASSET_NAMESPACE + ":electricityProducer", "white-balance-sunny", "CC9423", false,
        new AttributeDescriptorImpl("manufacturer", STRING, null,
            LABEL.withInitialValue("Manufacturer")),
        new AttributeDescriptorImpl("model", STRING, null,
            LABEL.withInitialValue("Model")),
        new AttributeDescriptorImpl("userNotes", STRING, null,
            LABEL.withInitialValue("User notes")),
        new AttributeDescriptorImpl("status", STRING, null,
            LABEL.withInitialValue("Status"),
            DESCRIPTION.withInitialValue("Status system"),
            READ_ONLY),
        new AttributeDescriptorImpl("installedCapacity", POWER, null,
            LABEL.withInitialValue("Installed capacity"),
            DESCRIPTION.withInitialValue("Power delivered at normalised eg. sun intensity, wind speeds")),
        new AttributeDescriptorImpl("systemEfficiency", PERCENTAGE, null,
            LABEL.withInitialValue("System efficiency")),
        new AttributeDescriptorImpl("totalPower", POWER, null,
            LABEL.withInitialValue("Total power"),
            READ_ONLY),
        new AttributeDescriptorImpl("forecastPowerDeviation", NUMBER, null,
            LABEL.withInitialValue("Forecast power deviation"),
            READ_ONLY),
        new AttributeDescriptorImpl("totalEnergy", ENERGY, null,
            LABEL.withInitialValue("Total energy"),
            UNIT_TYPE.withInitialValue(UNITS_ENERGY_KILOWATT_HOUR),
            READ_ONLY)
    ),

    ELECTRICITY_STORAGE(ASSET_NAMESPACE + ":electricityStorage", "battery-charging", "1B7C89", false,
        new AttributeDescriptorImpl("manufacturer", STRING, null,
            LABEL.withInitialValue("Manufacturer")),
        new AttributeDescriptorImpl("model", STRING, null,
            LABEL.withInitialValue("Model")),
        new AttributeDescriptorImpl("userNotes", STRING, null,
            LABEL.withInitialValue("User notes")),
        new AttributeDescriptorImpl("status", STRING, null,
            LABEL.withInitialValue("Status"),
            DESCRIPTION.withInitialValue("Status system"),
            READ_ONLY),
        new AttributeDescriptorImpl("installedCapacity", NUMBER, null,
            LABEL.withInitialValue("Installed capacity"),
            UNIT_TYPE.withInitialValue(UNITS_ENERGY_KILOWATT_HOUR)),
        new AttributeDescriptorImpl("levelledCostOfStorage", CURRENCY, null,
            LABEL.withInitialValue("Levelised cost of storage"),
            UNIT_TYPE.withInitialValue(UNITS_CURRENCY_EUR)),
        new AttributeDescriptorImpl("maxCharge", POWER, null,
            LABEL.withInitialValue("Max charge"),
            READ_ONLY),
        new AttributeDescriptorImpl("maxDischarge", NUMBER, null,
            LABEL.withInitialValue("Max discharge"),
            READ_ONLY),
        new AttributeDescriptorImpl("chargeState", PERCENTAGE, null,
            LABEL.withInitialValue("Charge state"),
            READ_ONLY),
        new AttributeDescriptorImpl("chargedCapacity", CHARGE, null,
            LABEL.withInitialValue("Charged capacity"),
            UNIT_TYPE.withInitialValue(UNITS_ENERGY_KILOWATT_HOUR),
            READ_ONLY),
        new AttributeDescriptorImpl("freeCapacity", CHARGE, null,
            LABEL.withInitialValue("Free capacity"),
            UNIT_TYPE.withInitialValue(UNITS_ENERGY_KILOWATT_HOUR),
            READ_ONLY),
        new AttributeDescriptorImpl("totalPower", POWER, null,
            LABEL.withInitialValue("Total power"),
            READ_ONLY),
        new AttributeDescriptorImpl("setPointPower", NUMBER, null,
            LABEL.withInitialValue("Setpoint power"),
            DESCRIPTION.withInitialValue("Positive is consuming, negative is delivering to network")),
        new AttributeDescriptorImpl("command", NUMBER, null,
            LABEL.withInitialValue("Command"),
            DESCRIPTION.withInitialValue("0 = No command, 1 = Start, 2 = Stop")),
        new AttributeDescriptorImpl("energyCharged", CHARGE, null,
            LABEL.withInitialValue("Energy charged"),
            UNIT_TYPE.withInitialValue(UNITS_ENERGY_KILOWATT_HOUR),
            READ_ONLY),
        new AttributeDescriptorImpl("energyDischarged", CHARGE, null,
            LABEL.withInitialValue("Energy discharged"),
            UNIT_TYPE.withInitialValue(UNITS_ENERGY_KILOWATT_HOUR),
            READ_ONLY),
        new AttributeDescriptorImpl("chargeCycle", INTEGER, null,
            LABEL.withInitialValue("Charge cycle"),
            READ_ONLY),
        new AttributeDescriptorImpl("financialWallet", CURRENCY, null,
            LABEL.withInitialValue("Financial wallet"),
            READ_ONLY,
            UNIT_TYPE.withInitialValue(UNITS_CURRENCY_EUR)),
        new AttributeDescriptorImpl("carbonWallet", CO2, null,
            LABEL.withInitialValue("Carbon wallet"),
            UNIT_TYPE.withInitialValue(UNITS_MASS_KILOGRAM),
            READ_ONLY)
    ),

    ELECTRICITY_SUPPLIER(ASSET_NAMESPACE + ":electricitySupplier", "upload-network", "9257A9", false,
        new AttributeDescriptorImpl("utilityProviderName", STRING, null,
            LABEL.withInitialValue("Provider name")),
        new AttributeDescriptorImpl("gridOperatorName", STRING, null,
            LABEL.withInitialValue("Grid operator name")),
        new AttributeDescriptorImpl("userNotes", STRING, null,
            LABEL.withInitialValue("User notes")),
        new AttributeDescriptorImpl("status", BOOLEAN, null,
            LABEL.withInitialValue("Status"),
            DESCRIPTION.withInitialValue("Status system (operational)")),
        new AttributeDescriptorImpl("connectionPower", POWER, null,
            LABEL.withInitialValue("Connection power")),
        new AttributeDescriptorImpl("setpointEdrPower", OBJECT, null,
            LABEL.withInitialValue("Setpoint EDR power"),
            UNIT_TYPE.withInitialValue(UNITS_POWER_KILOWATT),
            READ_ONLY),
        new AttributeDescriptorImpl("edrPrimaryReserve", POWER, null,
            LABEL.withInitialValue("EDR primary reserve"),
            UNIT_TYPE.withInitialValue(UNITS_POWER_KILOWATT)),
        new AttributeDescriptorImpl("edrMinimumPeriod", NUMBER, null,
            LABEL.withInitialValue("EDR minimum period"),
            UNIT_TYPE.withInitialValue(UNITS_TIME_SECONDS)),
        new AttributeDescriptorImpl("energyTariffImport", CURRENCY, null,
            LABEL.withInitialValue("Energy tariff import"),
            READ_ONLY,
            UNIT_TYPE.withInitialValue(UNITS_EUR_PER_KILOWATT_HOUR)),
        new AttributeDescriptorImpl("forecastEnergyTariffImportDeviation", NUMBER, null,
            LABEL.withInitialValue("Forecast energy tariff import deviation"),
            READ_ONLY),
        new AttributeDescriptorImpl("energyTariffExport", CURRENCY, null,
            LABEL.withInitialValue("Energy tariff export"),
            READ_ONLY,
            UNIT_TYPE.withInitialValue(UNITS_EUR_PER_KILOWATT_HOUR)),
        new AttributeDescriptorImpl("forecastEnergyTariffExportDeviation", NUMBER, null,
            LABEL.withInitialValue("Forecast energy tariff export deviation"),
            READ_ONLY),
        new AttributeDescriptorImpl("energyTax", CURRENCY, null,
            LABEL.withInitialValue("Energy tax"),
            READ_ONLY,
            UNIT_TYPE.withInitialValue(UNITS_EUR_PER_KILOWATT_HOUR)),
        new AttributeDescriptorImpl("gridCosts", CURRENCY, null,
            LABEL.withInitialValue("Grid costs"),
            UNIT_TYPE.withInitialValue(UNITS_EUR_PER_MONTH)),
        new AttributeDescriptorImpl("gridConnectCosts", CURRENCY, null,
            LABEL.withInitialValue("Grid connection costs"),
            UNIT_TYPE.withInitialValue(UNITS_CURRENCY_EUR)),
        new AttributeDescriptorImpl("energyCarbonImport", ENERGY, null,
            LABEL.withInitialValue("Energy carbon import"),
            UNIT_TYPE.withInitialValue(UNITS_KILOGRAM_CARBON_PER_KILOWATT_HOUR),
            READ_ONLY),
        new AttributeDescriptorImpl("energyCarbonExport", ENERGY, null,
            LABEL.withInitialValue("Energy carbon export"),
            UNIT_TYPE.withInitialValue(UNITS_KILOGRAM_CARBON_PER_KILOWATT_HOUR),
            READ_ONLY),
        new AttributeDescriptorImpl("totalPower", POWER, null,
            LABEL.withInitialValue("Total power"),
            READ_ONLY),
        new AttributeDescriptorImpl("forecastPowerDeviation", NUMBER, null,
            LABEL.withInitialValue("Forecast power deviation"),
            READ_ONLY),
        new AttributeDescriptorImpl("totalEnergyImport", ENERGY, null,
            LABEL.withInitialValue("Total energy import"),
            UNIT_TYPE.withInitialValue(UNITS_ENERGY_KILOWATT_HOUR),
            READ_ONLY),
        new AttributeDescriptorImpl("totalEnergyExport", ENERGY, null,
            LABEL.withInitialValue("Total energy export"),
            UNIT_TYPE.withInitialValue(UNITS_ENERGY_KILOWATT_HOUR),
            READ_ONLY),
        new AttributeDescriptorImpl("totalEnergyImportCosts", CURRENCY, null,
            LABEL.withInitialValue("Total energy import costs"),
            READ_ONLY,
            UNIT_TYPE.withInitialValue(UNITS_CURRENCY_EUR)),
        new AttributeDescriptorImpl("totalEnergyExportIncome", CURRENCY, null,
            LABEL.withInitialValue("Total energy export income"),
            READ_ONLY,
            UNIT_TYPE.withInitialValue(UNITS_CURRENCY_EUR)),
        new AttributeDescriptorImpl("totalCarbonCosts", CO2, null,
            LABEL.withInitialValue("Total carbon costs"),
            UNIT_TYPE.withInitialValue(UNITS_MASS_KILOGRAM),
            READ_ONLY)
    ),

    WEATHER(ASSET_NAMESPACE + ":weather", "weather-partly-cloudy", "49B0D8", false,
        new AttributeDescriptorImpl("userNotes", STRING, null,
            LABEL.withInitialValue("User notes")),
        new AttributeDescriptorImpl("temperature", TEMPERATURE, null,
            LABEL.withInitialValue("Temperature"),
            READ_ONLY),
        new AttributeDescriptorImpl("sunUvIndex", INTEGER, null,
            LABEL.withInitialValue("UV Index of the sun"),
            READ_ONLY),
        new AttributeDescriptorImpl("sunIrradiance", NUMBER, null,
            LABEL.withInitialValue("Irradiance of the sun"),
            UNIT_TYPE.withInitialValue(UNITS_POWER_PER_SQUARE_M),
            READ_ONLY),
        new AttributeDescriptorImpl("sunAzimuth", INTEGER, null,
            LABEL.withInitialValue("Azimuth of the sun"),
            UNIT_TYPE.withInitialValue(UNITS_ANGLE_DEGREES),
            READ_ONLY),
        new AttributeDescriptorImpl("sunZenith", INTEGER, null,
            LABEL.withInitialValue("Zenith of the sun"),
            UNIT_TYPE.withInitialValue(UNITS_ANGLE_DEGREES),
            READ_ONLY),
        new AttributeDescriptorImpl("sunAltitude", NUMBER, null,
            LABEL.withInitialValue("Altitude of the sun"),
            UNIT_TYPE.withInitialValue(UNITS_ANGLE_DEGREES),
            READ_ONLY),
        new AttributeDescriptorImpl("windSpeed", SPEED, null,
            LABEL.withInitialValue("Wind speed"),
            READ_ONLY),
        new AttributeDescriptorImpl("windDirection", DIRECTION, null,
            LABEL.withInitialValue("Wind direction"),
            READ_ONLY),
        new AttributeDescriptorImpl("precipitation", RAINFALL, null,
            LABEL.withInitialValue("Precipitation"),
            READ_ONLY),
        new AttributeDescriptorImpl("humidity", PERCENTAGE, null,
            LABEL.withInitialValue("Humidity"),
            READ_ONLY)
    );

    final protected String type;
    final protected String icon;
    final protected String color;
    final protected boolean accessPublicRead;
    final protected AttributeDescriptor[] attributeDescriptors;

    AssetType(String type, String icon, String color, AttributeDescriptor... attributeDescriptors) {
        this(type, icon, color, false, attributeDescriptors);
    }

    AssetType(String type, String icon, String color, boolean accessPublicRead, AttributeDescriptor... attributeDescriptors) {
        this.type = type;
        this.icon = icon;
        this.color = color;
        this.accessPublicRead = accessPublicRead;
        this.attributeDescriptors = attributeDescriptors;
    }

    public static Optional<AssetType> getByValue(String value) {
        if (value == null)
            return Optional.empty();

        for (AssetType assetType : values()) {
            if (value.equals(assetType.getType()))
                return Optional.of(assetType);
        }
        return Optional.empty();
    }

    @Override
    public String getName() {
        return this.name();
    }

    @Override
    public String getType() {
        return type;
    }

    @Override
    public String getIcon() {
        return icon;
    }

    @Override
    public String getColor() {
        return color;
    }

    @Override
    public boolean getAccessPublicRead() {
        return accessPublicRead;
    }

    @Override
    public AttributeDescriptor[] getAttributeDescriptors() {
        return attributeDescriptors;
    }
}
