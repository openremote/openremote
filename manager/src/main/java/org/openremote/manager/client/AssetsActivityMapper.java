package org.openremote.manager.client;

import com.google.gwt.activity.shared.Activity;
import com.google.gwt.activity.shared.ActivityMapper;
import com.google.gwt.place.shared.Place;
import com.google.inject.Inject;
import com.google.inject.Provider;
import org.openremote.manager.client.presenter.AssetsActivity;
import org.openremote.manager.client.presenter.AssetsPlace;

/**
 * Created by Richard on 12/02/2016.
 */
public class AssetsActivityMapper implements ActivityMapper {
    private final Provider<AssetsActivity> assetsActivityProvider;

    @Inject
    public AssetsActivityMapper(Provider<AssetsActivity> assetsActivityProvider) {
        this.assetsActivityProvider = assetsActivityProvider;
    }

    public Activity getActivity(Place place) {
        Activity activity = null;

        if (place instanceof AssetsPlace) {
            AssetsPlace assetsPlace = (AssetsPlace)place;
            activity = assetsActivityProvider.get().doInit(assetsPlace);
            return activity;
        }

        return null;
    }
}
