package org.openremote.manager.shared.device;

import org.openremote.manager.shared.attribute.Attributes;

// TODO Not used
public interface CapabilityDefinition {
    String getType();

    String getDescription();

    Attributes getResources();
}
