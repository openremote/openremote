package org.openremote.model.telematics;

import org.openremote.model.value.ValueDescriptor;

import java.util.Map;

public interface Payload extends Map<ValueDescriptor<?>, Object> {

    Long getTimestamp();

}
