package org.openremote.manager.client.app;

import com.google.gwt.activity.shared.Activity;
import com.google.gwt.activity.shared.ActivityMapper;
import com.google.gwt.place.shared.Place;
import com.google.inject.Inject;
import com.google.inject.Provider;
import org.openremote.manager.client.assets.AssetDetailActivity;
import org.openremote.manager.client.assets.AssetsPlace;
import org.openremote.manager.client.map.MapActivity;
import org.openremote.manager.client.map.MapPlace;

public class MainContentActivityMapper implements ActivityMapper {
    private final Provider<AssetDetailActivity> assetsActivityProvider;
    private final Provider<MapActivity> mapActivityProvider;

    @Inject
    public MainContentActivityMapper(Provider<AssetDetailActivity> assetsActivityProvider,
                                     Provider<MapActivity> mapActivityProvider) {
        this.assetsActivityProvider = assetsActivityProvider;
        this.mapActivityProvider = mapActivityProvider;
    }

    public Activity getActivity(Place place) {
        if (place instanceof AssetsPlace) {
            return assetsActivityProvider.get().doInit((AssetsPlace) place);
        }
        if (place instanceof MapPlace) {
            return mapActivityProvider.get().doInit((MapPlace) place);
        }
        return null;
    }
}
