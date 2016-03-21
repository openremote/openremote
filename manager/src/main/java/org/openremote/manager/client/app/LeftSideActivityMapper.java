package org.openremote.manager.client.app;

import com.google.gwt.place.shared.Place;
import com.google.inject.Inject;
import com.google.inject.Provider;
import org.openremote.manager.client.assets.AssetListActivity;
import org.openremote.manager.client.assets.AssetsPlace;
import org.openremote.manager.client.mvp.AppActivity;
import org.openremote.manager.client.mvp.AppActivityMapper;

public class LeftSideActivityMapper implements AppActivityMapper {
    private final Provider<AssetListActivity> assetsActivityProvider;

    @Inject
    public LeftSideActivityMapper(Provider<AssetListActivity> assetsActivityProvider) {
        this.assetsActivityProvider = assetsActivityProvider;
    }

    public AppActivity getActivity(Place place) {
        if (place instanceof AssetsPlace) {
            AssetsPlace assetsPlace = (AssetsPlace) place;
            return assetsActivityProvider.get().doInit(assetsPlace);
        }

        return null;
    }
}
