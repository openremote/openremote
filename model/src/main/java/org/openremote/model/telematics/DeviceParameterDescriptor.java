package org.openremote.model.telematics;

import org.openremote.model.value.ValueDescriptor;

public interface DeviceParameterDescriptor {
    String getAttributeName();
    String getLabel();
    ValueDescriptor<?> getValueDescriptor();
}
