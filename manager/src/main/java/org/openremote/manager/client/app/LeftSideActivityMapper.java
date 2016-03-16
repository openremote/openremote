package org.openremote.manager.client.app;

import com.google.gwt.activity.shared.Activity;
import com.google.gwt.activity.shared.ActivityMapper;
import com.google.gwt.place.shared.Place;
import com.google.inject.Inject;
import com.google.inject.Provider;
import org.openremote.manager.client.assets.AssetListActivity;
import org.openremote.manager.client.assets.AssetsPlace;

/**
 * Created by Richard on 12/02/2016.
 */
public class LeftSideActivityMapper implements ActivityMapper {
    private final Provider<AssetListActivity> assetsActivityProvider;

    @Inject
    public LeftSideActivityMapper(Provider<AssetListActivity> assetsActivityProvider) {
        this.assetsActivityProvider = assetsActivityProvider;
    }

    public Activity getActivity(Place place) {
        if (place instanceof AssetsPlace) {
            AssetsPlace assetsPlace = (AssetsPlace) place;
            Activity activity = assetsActivityProvider.get().doInit(assetsPlace);
            return activity;
        }

        return null;
    }
}
