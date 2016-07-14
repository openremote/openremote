package org.openremote.manager.client.assets.browser;

import org.openremote.manager.client.assets.asset.Asset;
import org.openremote.manager.shared.event.Event;

public class AssetSelectedEvent extends Event {

    final protected Asset asset;

    public AssetSelectedEvent(Asset asset) {
        this.asset = asset;
    }

    public Asset getAsset() {
        return asset;
    }
}
