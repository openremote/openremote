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
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import org.openremote.manager.client.assets.asset.Asset;
import org.openremote.manager.client.assets.browser.AssetBrowser;
import org.openremote.manager.client.assets.browser.AssetSelectedEvent;
import org.openremote.manager.client.event.GoToPlaceEvent;
import org.openremote.manager.client.event.bus.EventBus;
import org.openremote.manager.client.event.bus.EventRegistration;
import org.openremote.manager.client.i18n.ManagerMessages;
import org.openremote.manager.client.interop.elemental.JsonObjectMapper;
import org.openremote.manager.client.interop.mapbox.LngLat;
import org.openremote.manager.client.mvp.AppActivity;
import org.openremote.manager.client.service.RequestService;
import org.openremote.manager.shared.map.MapResource;

import javax.inject.Inject;
import java.util.Collection;
import java.util.logging.Logger;

import static org.openremote.manager.client.http.RequestExceptionHandler.handleRequestException;

public class MapActivity extends AppActivity<MapPlace> implements MapView.Presenter {

    private static final Logger LOG = Logger.getLogger(MapActivity.class.getName());

    final MapView view;
    final AssetBrowser.Presenter assetBrowserPresenter;
    final MapResource mapResource;
    final ManagerMessages managerMessages;
    final RequestService requestService;
    final PlaceController placeController;
    final JsonObjectMapper jsonObjectMapper;

    protected EventRegistration<AssetSelectedEvent> assetSelectionRegistration;
    protected String assetId;
    protected Asset asset;

    @Inject
    public MapActivity(AssetBrowser.Presenter assetBrowserPresenter,
                       MapView view,
                       MapResource mapResource,
                       ManagerMessages managerMessages,
                       RequestService requestService,
                       PlaceController placeController,
                       JsonObjectMapper jsonObjectMapper) {
        this.assetBrowserPresenter = assetBrowserPresenter;
        this.view = view;
        this.mapResource = mapResource;
        this.managerMessages = managerMessages;
        this.requestService = requestService;
        this.placeController = placeController;
        this.jsonObjectMapper = jsonObjectMapper;
    }

    @Override
    protected AppActivity<MapPlace> init(MapPlace place) {
        this.assetId = place.getAssetId();
        return this;
    }

    @Override
    protected String[] getRequiredRoles() {
        return new String[]{"read:map"};
    }

    @Override
    public void start(AcceptsOneWidget container, EventBus eventBus, Collection<EventRegistration> registrations) {
        view.setPresenter(this);
        container.setWidget(view.asWidget());

        assetSelectionRegistration = assetBrowserPresenter.onSelection(
            event -> {
                if (event.getAsset() == null) {
                    placeController.goTo(new MapPlace());
                } else {
                    if (assetId == null || !assetId.equals(event.getAsset().getId())) {
                        placeController.goTo(new MapPlace(event.getAsset().getId()));
                    }
                }
                eventBus.dispatch(event);
            }
        );

        asset = null;
        if (assetId != null) {
            loadAsset();
        }

        registrations.add(eventBus.register(GoToPlaceEvent.class, event -> view.refresh()));

        if (view.isMapInitialised()) {
            view.refresh();
            return;
        }

        requestService.execute(
            jsonObjectMapper,
            mapResource::getSettings,
            200,
            view::initialiseMap,
            ex -> handleRequestException(ex, eventBus, managerMessages)
        );
    }

    @Override
    public void onStop() {
        if (assetSelectionRegistration != null) {
            assetBrowserPresenter.removeRegistration(assetSelectionRegistration);
        }
        super.onStop();
    }

    @Override
    public void goTo(Place place) {
        placeController.goTo(place);
    }

    protected void loadAsset() {
        new Timer() {
            public void run() {
                LOG.info("### TODO: Load Asset");
                asset = new Asset(assetId, "DUMMY", "DUMMY FOR ID " + assetId, null);
                assetBrowserPresenter.selectAsset(asset);
                writeToView();
            }
        }.schedule(100);

        /* TODO query
        requestService.execute(
            assetMapper,
            requestParams -> assetResource.get(requestParams, assetId),
            200,
            user -> {
                this.user = user;
                this.realm = user.getRealm();
                loadRoles(() -> {
                    writeToView();
                });
            },
            ex -> handleRequestException(ex, eventBus, managerMessages)
        );
        */
    }

    protected void writeToView() {
        view.addPopup(asset.getDisplayName(), new LngLat(5.460315214821094, 51.44541688237109));
        // TODO Write data
    }

}
