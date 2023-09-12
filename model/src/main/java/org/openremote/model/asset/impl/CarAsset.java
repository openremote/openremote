package org.openremote.model.asset.impl;

import jakarta.persistence.Entity;
import org.openremote.model.asset.Asset;
import org.openremote.model.asset.AssetDescriptor;
import org.openremote.model.value.AttributeDescriptor;
import org.openremote.model.value.ValueType;
import org.openremote.model.value.impl.ColourRGB;

import java.util.Date;
import java.util.Optional;
@Entity
public class CarAsset extends Asset<CarAsset> {
    public static final AttributeDescriptor<String> IMEI = new AttributeDescriptor<>("IMEI", ValueType.TEXT);
    public static final AttributeDescriptor<Date> LAST_CONTACT = new AttributeDescriptor<>("lastContact", ValueType.DATE_AND_TIME);
    public static final AttributeDescriptor<String> MAKE_AND_MODEL = new AttributeDescriptor<>("makeAndModel", ValueType.TEXT).withOptional(true);
    public static final AttributeDescriptor<Integer> MODEL_YEAR = new AttributeDescriptor<>("modelYear", ValueType.INTEGER).withOptional(true);
    /**
     * If Rich sees this, sorry for the American spelling!
     */
    public static final AttributeDescriptor<ColourRGB> COLOR = new AttributeDescriptor<>("color", ValueType.COLOUR_RGB).withOptional(true);
    public static final AttributeDescriptor<String> LICENSE_PLATE = new AttributeDescriptor<>("licensePlate", ValueType.TEXT).withOptional(true);
    public static final AssetDescriptor<CarAsset> DESCRIPTOR = new AssetDescriptor<>("car", null, CarAsset.class);


    protected CarAsset(){
    }
    public CarAsset(String name){super(name);}

    public Optional<String> getIMEI() {
        return getAttributes().getValue(IMEI);
    }
    public Optional<Date> getLastContact() {
        return getAttributes().getValue(LAST_CONTACT);
    }
    public Optional<String> getMakeAndModel() {
        return getAttributes().getValue(MAKE_AND_MODEL);
    }
    public Optional<Integer> getModelYear() {
        return getAttributes().getValue(MODEL_YEAR);
    }
    public Optional<ColourRGB> getColor() {
        return getAttributes().getValue(COLOR);
    }

}
