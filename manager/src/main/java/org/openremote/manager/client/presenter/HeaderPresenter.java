package org.openremote.manager.client.presenter;

import com.google.gwt.place.shared.Place;
import com.google.gwt.place.shared.PlaceController;
import com.google.inject.Inject;
import org.openremote.manager.client.service.SecurityService;
import org.openremote.manager.client.view.HeaderView;

/**
 * Created by Richard on 24/02/2016.
 */
public class HeaderPresenter implements HeaderView.Presenter {
    private HeaderView view;
    private PlaceController placeController;
    private SecurityService securityService;

    @Inject
    public HeaderPresenter(HeaderView view,
                           SecurityService securityService,
                           PlaceController placeController) {
        this.view = view;
        this.placeController = placeController;
        this.securityService = securityService;
        view.setPresenter(this);
        view.setUsername(securityService.getUsername());
    }

    @Override
    public HeaderView getView() {
        return view;
    }

    @Override
    public void goTo(Place place) {
        placeController.goTo(place);
    }

    @Override
    public void onPlaceChange(Place place) {
        view.onPlaceChange(place);
    }

    @Override
    public void doLogout() {
        securityService.logout();

        //Reload current place to cause login dialog to reappear
        goTo(placeController.getWhere());
    }

    @Override
    public void setUsername(String username) {
        view.setUsername(username);
    }
}
