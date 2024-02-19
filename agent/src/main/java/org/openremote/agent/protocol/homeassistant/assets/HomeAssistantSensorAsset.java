package org.openremote.agent.protocol.homeassistant.assets;

import jakarta.persistence.Entity;
import org.openremote.model.asset.AssetDescriptor;

@Entity
public class HomeAssistantSensorAsset extends HomeAssistantBaseAsset {

    public static final AssetDescriptor<HomeAssistantSensorAsset> DESCRIPTOR = new AssetDescriptor<>("numeric", "2386f0", HomeAssistantSensorAsset.class);

    protected HomeAssistantSensorAsset() {
    }

    public HomeAssistantSensorAsset(String name, String entityId) {
        super(name, entityId);
    }

}
