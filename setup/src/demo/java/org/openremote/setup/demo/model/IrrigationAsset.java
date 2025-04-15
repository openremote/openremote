package org.openremote.setup.demo.model;

import org.openremote.model.asset.Asset;
import org.openremote.model.asset.AssetDescriptor;
import org.openremote.model.value.AttributeDescriptor;
import org.openremote.model.value.ValueType;
import org.openremote.model.value.ValueFormat;

import jakarta.persistence.Entity;
import java.util.Optional;

import static org.openremote.model.Constants.*;

@Entity
public class IrrigationAsset extends Asset<IrrigationAsset> {

    public static final AttributeDescriptor<Double> FLOW_WATER = new AttributeDescriptor<>("flowWater",
            ValueType.POSITIVE_NUMBER).withUnits(UNITS_LITRE, UNITS_PER, UNITS_HOUR);
    public static final AttributeDescriptor<Double> FLOW_NUTRIENTS = new AttributeDescriptor<>("flowNutrients",
            ValueType.POSITIVE_NUMBER).withUnits(UNITS_LITRE, UNITS_PER, UNITS_HOUR);
    public static final AttributeDescriptor<Double> FLOW_TOTAL = new AttributeDescriptor<>("flowTotal",
            ValueType.POSITIVE_NUMBER).withUnits(UNITS_LITRE, UNITS_PER, UNITS_HOUR);
    public static final AttributeDescriptor<Double> TANK_LEVEL = new AttributeDescriptor<>("tankLevel",
            ValueType.POSITIVE_NUMBER).withUnits(UNITS_LITRE);

    // Add the onOff attribute
    public static final AttributeDescriptor<Boolean> ON_OFF = new AttributeDescriptor<>("onOff", ValueType.BOOLEAN)
            .withFormat(ValueFormat.BOOLEAN_ON_OFF());

    public static final AssetDescriptor<IrrigationAsset> DESCRIPTOR = new AssetDescriptor<>("water-pump", "3d85c6", IrrigationAsset.class);

    /**
     * For use by hydrators (i.e. JPA/Jackson)
     */
    protected IrrigationAsset() {
    }

    public IrrigationAsset(String name) {
        super(name);
    }

    // Add a getter for the onOff attribute
    public Optional<Boolean> getOnOff() {
        return getAttributes().getValue(ON_OFF);
    }
}
