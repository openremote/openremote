/*
 * Copyright 2016, OpenRemote Inc.
 *
 * See the CONTRIBUTORS.txt file in the distribution for a
 * full listing of individual contributors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.openremote.manager.client.app;

import com.google.gwt.place.shared.Place;
import com.google.gwt.place.shared.PlaceController;
import com.google.inject.Inject;
import org.openremote.manager.client.event.GoToPlaceEvent;
import org.openremote.manager.client.event.UserChangeEvent;
import org.openremote.manager.client.event.bus.EventBus;
import org.openremote.manager.client.service.SecurityService;
import org.openremote.manager.client.user.UserControls;
import org.openremote.manager.client.util.JsUtil;

import static org.openremote.manager.shared.Constants.MANAGER_CLIENT_ID;

public class HeaderPresenter implements HeaderView.Presenter {

    final protected HeaderView view;
    final protected UserControls.Presenter userControlsPresenter;
    final protected PlaceController placeController;
    final protected SecurityService securityService;

    @Inject
    public HeaderPresenter(HeaderView view,
                           UserControls.Presenter userControlsPresenter,
                           SecurityService securityService,
                           PlaceController placeController,
                           EventBus eventBus) {
        this.view = view;
        this.userControlsPresenter = userControlsPresenter;
        this.placeController = placeController;
        this.securityService = securityService;

        view.setPresenter(this);

        eventBus.register(
            GoToPlaceEvent.class,
            event -> view.onPlaceChange(event.getNewPlace())
        );

        view.setUsername(securityService.getParsedToken().getPreferredUsername());
        eventBus.register(
            UserChangeEvent.class,
            event -> view.setUsername(event.getUsername())
        );
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
    public UserControls getUserControls() {
        return userControlsPresenter.getView();
    }

    @Override
    public boolean isUserInRole(String role) {
        return securityService.hasResourceRoleOrIsAdmin(role, MANAGER_CLIENT_ID);
    }
}
