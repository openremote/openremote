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

import com.google.gwt.place.shared.PlaceController;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import org.openremote.manager.client.assets.asset.Asset;
import org.openremote.manager.client.assets.browser.AssetBrowser;
import org.openremote.manager.client.assets.browser.AssetBrowsingActivity;
import org.openremote.manager.client.event.GoToPlaceEvent;
import org.openremote.manager.client.event.bus.EventBus;
import org.openremote.manager.client.event.bus.EventRegistration;
import org.openremote.manager.client.i18n.ManagerMessages;
import org.openremote.manager.client.interop.elemental.JsonObjectMapper;
import org.openremote.manager.client.service.RequestService;
import org.openremote.manager.shared.map.MapResource;

import javax.inject.Inject;
import java.util.Collection;

import static org.openremote.manager.client.http.RequestExceptionHandler.handleRequestException;

public class MapActivity extends AssetBrowsingActivity<MapView, MapPlace> implements MapView.Presenter {

    final MapResource mapResource;
    final ManagerMessages managerMessages;
    final RequestService requestService;
    final PlaceController placeController;
    final JsonObjectMapper jsonObjectMapper;

    @Inject
    public MapActivity(AssetBrowser.Presenter assetBrowserPresenter,
                       MapView view,
                       MapResource mapResource,
                       ManagerMessages managerMessages,
                       RequestService requestService,
                       PlaceController placeController,
                       JsonObjectMapper jsonObjectMapper) {
        super(view, assetBrowserPresenter);
        this.mapResource = mapResource;
        this.managerMessages = managerMessages;
        this.requestService = requestService;
        this.placeController = placeController;
        this.jsonObjectMapper = jsonObjectMapper;
    }

    @Override
    public void start(AcceptsOneWidget container, EventBus eventBus, Collection<EventRegistration> registrations) {
        super.start(container, eventBus, registrations);

        registrations.add(eventBus.register(GoToPlaceEvent.class, event -> view.refresh()));

        if (getView().isMapInitialised()) {
            getView().refresh();
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
    protected void onAssetReady() {
        getView().addPopup(asset.getDisplayName(), asset.getLocation());
        // TODO Write data
    }

    @Override
    protected void onAssetsDeselected() {
        placeController.goTo(new MapPlace());
    }

    @Override
    protected void onAssetSelectionChange(Asset newSelection) {
        placeController.goTo(new MapPlace(newSelection.getId()));
    }

}
