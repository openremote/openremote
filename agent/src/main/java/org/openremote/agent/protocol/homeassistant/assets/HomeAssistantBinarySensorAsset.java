package org.openremote.agent.protocol.homeassistant.assets;

import jakarta.persistence.Entity;
import org.openremote.model.asset.AssetDescriptor;

@Entity
public class HomeAssistantBinarySensorAsset extends HomeAssistantBaseAsset {

    public static final AssetDescriptor<HomeAssistantBinarySensorAsset> DESCRIPTOR = new AssetDescriptor<>("motion-sensor", "2386f0", HomeAssistantBinarySensorAsset.class);

    protected HomeAssistantBinarySensorAsset() {
    }

    public HomeAssistantBinarySensorAsset(String name, String entityId) {
        super(name, entityId);
    }

}
