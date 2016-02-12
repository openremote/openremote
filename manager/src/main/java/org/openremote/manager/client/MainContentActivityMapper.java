package org.openremote.manager.client;

import com.google.gwt.activity.shared.Activity;
import com.google.gwt.activity.shared.ActivityMapper;
import com.google.gwt.place.shared.Place;
import com.google.inject.Inject;
import com.google.inject.Provider;
import org.openremote.manager.client.presenter.AssetsPlace;
import org.openremote.manager.client.presenter.MapActivity;
import org.openremote.manager.client.presenter.MapPlace;

/**
 * Created by Richard on 12/02/2016.
 */
public class MainContentActivityMapper implements ActivityMapper {
    private final AssetsActivityMapper assetsActivityMapper;
    private final Provider<MapActivity> mapActivityProvider;

    @Inject
    public MainContentActivityMapper(AssetsActivityMapper assetsActivityMapper, Provider<MapActivity> mapActivityProvider) {
        this.assetsActivityMapper = assetsActivityMapper;
        this.mapActivityProvider = mapActivityProvider;
    }

    public Activity getActivity(Place place) {
        if (place instanceof AssetsPlace) {
            return assetsActivityMapper.getActivity(place);
        }
        if (place instanceof MapPlace) {
            return mapActivityProvider.get().doInit((MapPlace)place);
        }

        return null;
    }
}
