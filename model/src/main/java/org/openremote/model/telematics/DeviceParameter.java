package org.openremote.model.telematics;

import org.openremote.model.value.AttributeDescriptor;

import java.util.function.BiConsumer;
import java.util.function.Function;

public interface DeviceParameter {
    AttributeDescriptor<?> getDescriptor();
    Function<Object, Object> getValue();
}
