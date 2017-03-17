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
import org.openremote.manager.client.assets.AssetBrowsingActivity;
import org.openremote.manager.client.assets.AssetMapper;
import org.openremote.manager.client.assets.browser.AssetBrowser;
import org.openremote.manager.client.assets.browser.AssetBrowserSelection;
import org.openremote.manager.client.event.ShowInfoEvent;
import org.openremote.manager.client.event.bus.EventBus;
import org.openremote.manager.client.event.bus.EventRegistration;
import org.openremote.manager.client.interop.elemental.JsonObjectMapper;
import org.openremote.manager.client.mvp.AppActivity;
import org.openremote.manager.shared.asset.AssetResource;
import org.openremote.manager.shared.map.MapResource;
import org.openremote.model.Consumer;
import org.openremote.model.asset.Asset;

import javax.inject.Inject;
import java.util.Collection;

import static org.openremote.manager.client.http.RequestExceptionHandler.handleRequestException;

public class MapActivity extends AssetBrowsingActivity<MapPlace> implements MapView.Presenter {

    final MapView view;
    final AssetResource assetResource;
    final AssetMapper assetMapper;
    final MapResource mapResource;
    final JsonObjectMapper jsonObjectMapper;

    String assetId;
    Asset asset;
    String realm;

    @Inject
    public MapActivity(Environment environment,
                       AssetBrowser.Presenter assetBrowserPresenter,
                       MapView view,
                       AssetResource assetResource,
                       AssetMapper assetMapper,
                       MapResource mapResource,
                       JsonObjectMapper jsonObjectMapper) {
        super(environment, assetBrowserPresenter);
        this.view = view;
        this.assetResource = assetResource;
        this.assetMapper = assetMapper;
        this.mapResource = mapResource;
        this.jsonObjectMapper = jsonObjectMapper;
    }

    @Override
    protected AppActivity<MapPlace> init(MapPlace place) {
        if (place instanceof MapAssetPlace) {
            MapAssetPlace mapAssetPlace = (MapAssetPlace) place;
            assetId = mapAssetPlace.getAssetId();
        } else if (place instanceof MapTenantPlace) {
            MapTenantPlace mapTenantPlace = (MapTenantPlace) place;
            realm = mapTenantPlace.getRealmId();
        }
        return this;
    }

    @Override
    public void start(AcceptsOneWidget container, EventBus eventBus, Collection<EventRegistration> registrations) {
        view.setPresenter(this);
        container.setWidget(view.asWidget());

        registrations.add(eventBus.register(AssetBrowserSelection.class, event -> {
            if (event.isTenantSelection()) {
                onTenantSelected(event.getSelectedNode().getId());
            } else if (event.isAssetSelection()) {
                onAssetSelected(event.getSelectedNode().getId());
            }
        }));

        if (view.isMapInitialised()) {
            view.refresh();
        } else {
            environment.getRequestService().execute(
                jsonObjectMapper,
                mapResource::getSettings,
                200,
                mapSettings -> {
                    view.initialiseMap(mapSettings);
                    if (asset != null) {
                        showAssetOnMap();
                    }
                },
                ex -> handleRequestException(ex, environment)
            );
        }

        hideAssetOnMap();

        asset = null;
        if (assetId != null) {
            loadAsset(assetId, loadedAsset -> {
                this.asset = loadedAsset;
                if (asset != null) {
                    assetBrowserPresenter.selectAsset(asset);
                    showAssetOnMap();
                }
            });
        } else if (realm != null) {
            environment.getEventBus().dispatch(
                new ShowInfoEvent("TODO: Showing all assets on map for tenant: " + realm)
            );
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        view.setPresenter(null);
    }

    protected void onAssetSelected(String assetId) {
        environment.getPlaceController().goTo(new MapAssetPlace(assetId));
    }

    protected void onTenantSelected(String realmId) {
        environment.getPlaceController().goTo(new MapTenantPlace(realmId));
    }

    protected void loadAsset(String id, Consumer<Asset> assetConsumer) {
        environment.getRequestService().execute(
            assetMapper,
            requestParams -> assetResource.get(requestParams, id),
            200,
            assetConsumer,
            ex -> handleRequestException(ex, environment)
        );
    }

    protected void showAssetOnMap() {
        if (asset != null && asset.getCoordinates() != null) {
            view.showFeaturesSelection(MapView.getFeature(asset));
            view.flyTo(asset.getCoordinates());
        }
    }

    protected void hideAssetOnMap() {
        view.hideFeaturesSelection();
    }
}
