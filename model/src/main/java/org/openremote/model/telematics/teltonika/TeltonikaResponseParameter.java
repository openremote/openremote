package org.openremote.model.telematics.teltonika;

import org.openremote.model.telematics.DeviceParameter;
import org.openremote.model.value.AttributeDescriptor;
import org.openremote.model.value.ValueType;

import java.util.function.Function;

public class TeltonikaResponseParameter implements DeviceParameter {

    public static final TeltonikaResponseParameter Instance = new TeltonikaResponseParameter();
    @Override
    public AttributeDescriptor<?> getDescriptor() {
        return new AttributeDescriptor<>("response", ValueType.TEXT);
    }

    @Override
    public Function<Object, Object> getValue() {
        return value -> value;
    }
}
