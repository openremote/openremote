package org.openremote.agent.protocol.homeassistant.assets;

import jakarta.persistence.Entity;
import org.openremote.model.asset.AssetDescriptor;

@Entity
public class HomeAssistantSwitchAsset extends HomeAssistantBaseAsset {

    public static final AssetDescriptor<HomeAssistantSwitchAsset> DESCRIPTOR = new AssetDescriptor<>("light-switch", "33323b", HomeAssistantSwitchAsset.class);

    protected HomeAssistantSwitchAsset() {
    }

    public HomeAssistantSwitchAsset(String name, String entityId) {
        super(name, entityId);
    }

}
