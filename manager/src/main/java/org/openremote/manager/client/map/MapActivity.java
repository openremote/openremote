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
package org.openremote.manager.client.map;

import com.google.gwt.place.shared.Place;
import com.google.gwt.place.shared.PlaceController;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import org.openremote.manager.client.event.GoToPlaceEvent;
import org.openremote.manager.client.event.bus.EventBus;
import org.openremote.manager.client.event.bus.EventRegistration;
import org.openremote.manager.client.i18n.ManagerMessages;
import org.openremote.manager.client.mvp.AppActivity;
import org.openremote.manager.client.service.RequestService;
import org.openremote.manager.shared.event.ui.ShowFailureEvent;
import org.openremote.manager.shared.http.JsonObjectCallback;
import org.openremote.manager.shared.map.MapResource;

import javax.inject.Inject;
import java.util.Collection;

public class MapActivity extends AppActivity<MapPlace> implements MapView.Presenter {

    final MapView view;
    final MapResource mapResource;
    final ManagerMessages managerMessages;
    final RequestService requestService;
    final PlaceController placeController;

    @Inject
    public MapActivity(MapView view,
                       MapResource mapResource,
                       ManagerMessages managerMessages,
                       RequestService requestService,
                       PlaceController placeController) {
        this.view = view;
        this.mapResource = mapResource;
        this.managerMessages = managerMessages;
        this.requestService = requestService;
        this.placeController = placeController;
    }

    @Override
    public AppActivity<MapPlace> init(MapPlace place) {
        return this;
    }

    @Override
    public void start(AcceptsOneWidget container, EventBus eventBus, Collection<EventRegistration> registrations) {
        view.setPresenter(this);
        container.setWidget(view.asWidget());

        registrations.add(eventBus.register(GoToPlaceEvent.class, event -> view.refresh()));

        if (view.isMapInitialised())
            return;

        /* TODO This is the generic resteasy client
        Request<JsonObject> request = requestService.createRequest(true);
        request.setURI("http://localhost:8080/master/map");
        request.setMethod("GET");
        request.setAccepts("application/json");
        request.execute(new JsonObjectCallback(
            200,
            view::initialiseMap,
                ex -> eventBus.dispatch(new ShowFailureEvent(
                    managerMessages.failureLoadingMapSettings(ex.getMessage())
                ))
        ));
        */

        // This is the strongly typed client
        mapResource.getSettings(requestService.createRequestParams(new JsonObjectCallback(
                200,
                view::initialiseMap,
                ex -> eventBus.dispatch(new ShowFailureEvent(
                    managerMessages.failureLoadingMapSettings(ex.getMessage())
                ))
            )
        ));
    }

    @Override
    public void goTo(Place place) {
        placeController.goTo(place);
    }
}
