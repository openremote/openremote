package org.openremote.manager.client.presenter;

import com.google.gwt.activity.shared.Activity;
import com.google.gwt.activity.shared.ActivityMapper;
import com.google.gwt.place.shared.Place;
import com.google.inject.Inject;
import com.google.inject.Provider;
import org.openremote.manager.client.service.SecurityService;

/**
 * Created by Richard on 12/02/2016.
 */
public class MainContentActivityMapper implements ActivityMapper {
    private final Provider<AssetDetailActivity> assetsActivityProvider;
    private final Provider<MapActivity> mapActivityProvider;
    private final Provider<LoginPresenter> loginActivityProvider;
    private final SecurityService securityService;

    @Inject
    public MainContentActivityMapper(Provider<AssetDetailActivity> assetsActivityProvider,
                                     Provider<MapActivity> mapActivityProvider,
                                     Provider<LoginPresenter> loginActivityProvider,
                                     SecurityService securityService) {
        this.assetsActivityProvider = assetsActivityProvider;
        this.mapActivityProvider = mapActivityProvider;
        this.loginActivityProvider = loginActivityProvider;
        this.securityService = securityService;
    }

    public Activity getActivity(Place place) {
        if (place instanceof AssetsPlace) {
            return assetsActivityProvider.get().doInit((AssetsPlace) place);
        }
        if (place instanceof OverviewPlace) {
            return mapActivityProvider.get().doInit((OverviewPlace) place);
        }
        return null;
    }
}
