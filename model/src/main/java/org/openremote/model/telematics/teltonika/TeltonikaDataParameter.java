package org.openremote.model.telematics.teltonika;

import org.openremote.model.telematics.DeviceParameter;
import org.openremote.model.value.AttributeDescriptor;
import org.openremote.model.value.ValueType;

import java.util.function.Function;

public class TeltonikaDataParameter implements DeviceParameter {

    protected String name;

    public TeltonikaDataParameter(String name) {
        this.name = name;
    }

    @Override
    public AttributeDescriptor<?> getDescriptor() {
        return new AttributeDescriptor<String>(name, ValueType.TEXT);
    }

    @Override
    public Function<Object, Object> getValue() {
        return value -> value;
    }
}
