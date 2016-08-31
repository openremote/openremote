package org.openremote.manager.client.assets.event;

import org.openremote.manager.shared.asset.Asset;
import org.openremote.manager.shared.event.Event;

public class AssetsModifiedEvent extends Event {

    Asset asset;

    public AssetsModifiedEvent(Asset asset) {
        this.asset = asset;
    }

    public Asset getAsset() {
        return asset;
    }
}
