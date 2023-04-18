package org.openremote.model.asset.impl;

import jakarta.persistence.Entity;
import org.openremote.model.Constants;
import org.openremote.model.asset.Asset;
import org.openremote.model.asset.AssetDescriptor;
import org.openremote.model.attribute.MetaItem;
import org.openremote.model.value.AttributeDescriptor;
import org.openremote.model.value.MetaItemType;
import org.openremote.model.value.ValueFormat;
import org.openremote.model.value.ValueType;

import static org.openremote.model.Constants.*;

@Entity
public class HeatMeterAsset extends Asset<HeatMeterAsset> {

    public static final AttributeDescriptor<String> METER_POINT_ADMINISTRATION_NUMBER = new AttributeDescriptor<>("meterPointAdministrationNumber", ValueType.TEXT,
            new MetaItem<>(MetaItemType.LABEL, "Meter Point Administration Number"),
            new MetaItem<>(MetaItemType.READ_ONLY));

    public static final AttributeDescriptor<Double> HEAT_METER_READING = new AttributeDescriptor<>("heatMeterReading", ValueType.NUMBER,
            new MetaItem<>(MetaItemType.LABEL, "Heat Meter Reading"),
            new MetaItem<>(MetaItemType.READ_ONLY),
            new MetaItem<>(MetaItemType.UNITS, Constants.units(UNITS_KILO, UNITS_WATT, UNITS_HOUR)))
            .withFormat(ValueFormat.NUMBER_3_DP_MAX());

    public static final AttributeDescriptor<Double> FLOW_TEMPERATURE = new AttributeDescriptor<>("flowTemperature", ValueType.NUMBER,
            new MetaItem<>(MetaItemType.LABEL, "Flow Temperature"),
            new MetaItem<>(MetaItemType.UNITS, Constants.units(UNITS_CELSIUS)),
            new MetaItem<>(MetaItemType.READ_ONLY))
            .withFormat(ValueFormat.NUMBER_3_DP_MAX());

    public static final AttributeDescriptor<Double> RETURN_TEMPERATURE = new AttributeDescriptor<>("returnTemperature", ValueType.NUMBER,
            new MetaItem<>(MetaItemType.LABEL, "Return Temperature"),
            new MetaItem<>(MetaItemType.UNITS, Constants.units(UNITS_CELSIUS)),
            new MetaItem<>(MetaItemType.READ_ONLY))
            .withFormat(ValueFormat.NUMBER_3_DP_MAX());

    public static final AttributeDescriptor<Double> POWER = new AttributeDescriptor<>("power", ValueType.NUMBER,
            new MetaItem<>(MetaItemType.LABEL, "Power"),
            new MetaItem<>(MetaItemType.UNITS, Constants.units(UNITS_WATT)),
            new MetaItem<>(MetaItemType.READ_ONLY))
            .withFormat(ValueFormat.NUMBER_3_DP_MAX());

    public static final AttributeDescriptor<Double> VOLUME_FLOW = new AttributeDescriptor<>("volumeFlow", ValueType.NUMBER,
            new MetaItem<>(MetaItemType.LABEL, "Volume Flow"),
            new MetaItem<>(MetaItemType.UNITS, Constants.units(UNITS_LITRE, UNITS_PER, UNITS_HOUR)),
            new MetaItem<>(MetaItemType.READ_ONLY))
            .withFormat(ValueFormat.NUMBER_3_DP_MAX());

    public static final AssetDescriptor<HeatMeterAsset> DESCRIPTOR = new AssetDescriptor<>("counter", "8A293D", HeatMeterAsset.class);

    /**
     * For use by hydrators (i.e. JPA/Jackson)
     */
    protected HeatMeterAsset() {
    }

    public HeatMeterAsset(String name) {
        super(name);
    }
}
