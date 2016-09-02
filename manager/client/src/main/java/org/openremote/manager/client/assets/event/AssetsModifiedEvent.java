package org.openremote.manager.client.assets.event;

import org.openremote.manager.shared.asset.Asset;
import org.openremote.manager.shared.event.Event;

public class AssetsModifiedEvent extends Event {

    final protected Asset asset;
    final protected boolean forceRootRefresh;

    public AssetsModifiedEvent(Asset asset) {
        this(asset, false);
    }

    public AssetsModifiedEvent(Asset asset, boolean forceRootRefresh) {
        this.asset = asset;
        this.forceRootRefresh = forceRootRefresh;
    }

    public Asset getAsset() {
        return asset;
    }

    public boolean isForceRootRefresh() {
        return forceRootRefresh;
    }
}
