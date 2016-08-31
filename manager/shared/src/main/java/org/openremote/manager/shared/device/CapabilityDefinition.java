package org.openremote.manager.shared.device;

import org.openremote.manager.shared.attribute.Attributes;

public interface CapabilityDefinition {
    String getType();

    String getDescription();

    Attributes getResources();
}
