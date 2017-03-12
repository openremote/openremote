package org.openremote.manager.client.assets.browser;

import com.fasterxml.jackson.annotation.JsonIgnoreType;
import org.openremote.model.asset.AssetInfo;
import org.openremote.model.Event;

@JsonIgnoreType
public class AssetSelectedEvent extends Event {

    final protected AssetInfo assetInfo;

    public AssetSelectedEvent(AssetInfo assetInfo) {
        this.assetInfo = assetInfo;
    }

    public AssetInfo getAssetInfo() {
        return assetInfo;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
            "assetInfo=" + assetInfo +
            "}";
    }
}
