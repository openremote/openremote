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

import com.google.gwt.user.client.ui.AcceptsOneWidget;
import org.openremote.manager.client.Environment;
import org.openremote.manager.client.assets.AssetMapper;
import org.openremote.manager.client.assets.browser.AssetBrowser;
import org.openremote.manager.client.assets.browser.AssetBrowsingActivity;
import org.openremote.manager.client.event.bus.EventBus;
import org.openremote.manager.client.event.bus.EventRegistration;
import org.openremote.manager.client.interop.elemental.JsonObjectMapper;
import org.openremote.manager.shared.asset.AssetResource;
import org.openremote.manager.shared.map.MapResource;

import javax.inject.Inject;
import java.util.Collection;

import static org.openremote.manager.client.http.RequestExceptionHandler.handleRequestException;

public class MapActivity extends AssetBrowsingActivity<MapView, MapPlace> implements MapView.Presenter {

    final MapResource mapResource;
    final JsonObjectMapper jsonObjectMapper;

    @Inject
    public MapActivity(Environment environment,
                       MapView view,
                       AssetBrowser.Presenter assetBrowserPresenter,
                       AssetResource assetResource,
                       AssetMapper assetMapper,
                       MapResource mapResource,
                       JsonObjectMapper jsonObjectMapper) {
        super(environment, view, assetBrowserPresenter, assetResource, assetMapper);
        this.mapResource = mapResource;
        this.jsonObjectMapper = jsonObjectMapper;
    }

    @Override
    public void start(AcceptsOneWidget container, EventBus eventBus, Collection<EventRegistration> registrations) {
        super.start(container, eventBus, registrations);

        if (getView().isMapInitialised()) {
            getView().refresh();
            return;
        }

        environment.getRequestService().execute(
            jsonObjectMapper,
            mapResource::getSettings,
            200,
            mapSettings -> {
                view.initialiseMap(mapSettings);
                if (asset != null)
                    showAssetOnMap();
            },
            ex -> handleRequestException(ex, environment)
        );
    }

    @Override
    protected void onAssetLoaded() {
        showAssetOnMap();
    }

    @Override
    protected void onAssetsDeselected() {
        hideAssetOnMap();
    }

    @Override
    protected void onAssetSelectionChange(String selectedAssetId) {
        environment.getPlaceController().goTo(new MapPlace(selectedAssetId));
    }

    protected void showAssetOnMap() {
        if (asset != null && asset.getCoordinates() != null) {
            getView().showFeaturesSelection(getFeature(asset));
            getView().flyTo(asset.getCoordinates());
        }
    }

    protected void hideAssetOnMap() {
        getView().hideFeaturesSelection();
    }

}
