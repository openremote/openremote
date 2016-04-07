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
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import org.openremote.manager.client.event.bus.EventBus;
import org.openremote.manager.client.event.bus.EventRegistration;
import org.openremote.manager.client.mvp.AppActivity;
import org.openremote.manager.client.service.RequestService;
import org.openremote.manager.shared.http.JsonObjectCallback;
import org.openremote.manager.shared.map.MapResource;

import javax.inject.Inject;
import java.util.Collection;
import java.util.logging.Logger;

public class MapActivity
    extends AppActivity<MapPlace>
    implements MapView.Presenter {

    private static final Logger LOG = Logger.getLogger(MapActivity.class.getName());

    private final MapView view;
    private final MapResource mapResource;
    private final RequestService requestService;
    private final PlaceController placeController;

    @Inject
    public MapActivity(MapView view,
                       MapResource mapResource,
                       RequestService requestService,
                       PlaceController placeController) {
        this.view = view;
        this.requestService = requestService;
        this.mapResource = mapResource;
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

        if (!view.isMapInitialised()) {

            /* TODO This is the generic resteasy client
            Request<JsonObject> request = requestService.createRequest(true);
            request.setURI("http://localhost:8080/master/map");
            request.setMethod("GET");
            request.setAccepts("application/json");
            request.execute(new JsonObjectCallback(
                200,
                view::initialiseMap,
                ex -> {
                    // TODO: Handle map settings failure
                    Window.alert(ex.getMessage());
                }
            ));
            */

            // This is the strongly typed client
            mapResource.getSettings(requestService.createRequestParams(new JsonObjectCallback(
                    200,
                    view::initialiseMap,
                    ex -> {
                        // TODO: Handle map settings failure
                        Window.alert(ex.getMessage());
                    }
                )
            ));

        } else {
            //TODO: Reconfigure the map
        }
    }

    @Override
    public void goTo(Place place) {
        placeController.goTo(place);
    }
}
