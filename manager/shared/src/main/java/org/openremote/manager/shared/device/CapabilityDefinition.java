package org.openremote.manager.shared.device;

import java.util.Map;

public interface CapabilityDefinition {
    int getId();

    String getName();

    String getDescription();

    Map<Integer, Resource> getResources();
}
