package org.openremote.manager.shared.device;

import org.openremote.model.Attributes;

// TODO Not used
public interface CapabilityDefinition {
    String getType();

    String getDescription();

    Attributes getResources();
}
