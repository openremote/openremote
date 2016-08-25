package org.openremote.manager.client.assets.browser;

import org.openremote.manager.shared.event.Event;

public class AssetSelectedEvent extends Event {

    final protected String assetId;

    public AssetSelectedEvent(String assetId) {
        this.assetId = assetId;
    }

    public String getAssetId() {
        return assetId;
    }
}
