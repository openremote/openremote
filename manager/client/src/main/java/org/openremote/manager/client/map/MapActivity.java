/*
 * Copyright 2017, OpenRemote Inc.
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
import org.openremote.manager.client.assets.asset.AssetViewPlace;
import org.openremote.manager.client.assets.browser.AssetBrowser;
import org.openremote.manager.client.assets.browser.AssetBrowserSelection;
import org.openremote.manager.client.assets.browser.AssetTreeNode;
import org.openremote.manager.client.assets.browser.TenantTreeNode;
import org.openremote.manager.client.interop.elemental.JsonObjectMapper;
import org.openremote.manager.client.mvp.AppActivity;
import org.openremote.manager.shared.asset.AssetResource;
import org.openremote.manager.shared.map.MapResource;
import org.openremote.manager.shared.security.Tenant;
import org.openremote.model.AttributeEvent;
import org.openremote.model.Pair;
import org.openremote.model.asset.Asset;
import org.openremote.model.asset.AssetAttribute;
import org.openremote.model.event.bus.EventBus;
import org.openremote.model.event.bus.EventRegistration;
import org.openremote.model.geo.GeoJSON;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static org.openremote.manager.client.http.RequestExceptionHandler.handleRequestException;

public class MapActivity extends AssetBrowsingActivity<MapPlace> implements MapView.Presenter {

    private static final Logger LOG = Logger.getLogger(MapActivity.class.getName());

    final MapView view;
    final AssetResource assetResource;
    final AssetMapper assetMapper;
    final MapResource mapResource;
    final JsonObjectMapper jsonObjectMapper;

    String assetId;
    String realmId;
    Asset asset;
    List<AssetAttribute> dashboardAttributes = new ArrayList<>();

    @Inject
    public MapActivity(Environment environment,
                       Tenant currentTenant,
                       AssetBrowser.Presenter assetBrowserPresenter,
                       MapView view,
                       AssetResource assetResource,
                       AssetMapper assetMapper,
                       MapResource mapResource,
                       JsonObjectMapper jsonObjectMapper) {
        super(environment, currentTenant, assetBrowserPresenter);
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
            realmId = mapTenantPlace.getRealmId();
        }
        return this;
    }

    @Override
    public void start(AcceptsOneWidget container, EventBus eventBus, Collection<EventRegistration> registrations) {
        view.setPresenter(this);
        container.setWidget(view.asWidget());

        registrations.add(eventBus.register(AssetBrowserSelection.class, event -> {
            if (event.getSelectedNode() instanceof TenantTreeNode) {
                environment.getPlaceController().goTo(
                    new MapTenantPlace(event.getSelectedNode().getId())
                );
            } else if (event.getSelectedNode() instanceof AssetTreeNode) {
                environment.getPlaceController().goTo(
                    new MapAssetPlace(event.getSelectedNode().getId())
                );
            }
        }));

        registrations.add(environment.getEventBus().register(
            AttributeEvent.class,
            event -> {
                if (asset == null
                    || !event.getEntityId().equals(asset.getId())
                    || dashboardAttributes.stream().noneMatch(attribute -> attribute.getName().equals(event.getAttributeName())))
                    return;

                dashboardAttributes.stream()
                    .filter(attribute -> attribute.getName().equals(event.getAttributeName())).findFirst().get()
                    .setValueUnchecked(
                        event.getValue(), event.getTimestamp()
                    );
                showAssetInfoItems();
            }
        ));

        if (!view.isMapInitialised()) {
            environment.getRequestService().execute(
                jsonObjectMapper,
                mapResource::getSettings,
                200,
                view::initialiseMap,
                ex -> handleRequestException(ex, environment)
            );
        } else {
            onMapReady();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        view.setPresenter(null);

        if (asset != null && asset.hasGeoFeature() || dashboardAttributes.size() > 0) {
            environment.getEventService().unsubscribe(
                AttributeEvent.class
            );
        }
    }

    @Override
    public void onMapReady() {
        if (assetId != null) {
            assetBrowserPresenter.loadAsset(assetId, loadedAsset -> {
                this.asset = loadedAsset;
                this.dashboardAttributes = asset.getAttributes().stream()
                            .filter(AssetAttribute::isShowOnDashboard).collect(Collectors.toList());
                assetBrowserPresenter.selectAsset(asset);
                view.setAssetViewHistoryToken(environment.getPlaceHistoryMapper().getToken(
                    new AssetViewPlace(assetId)
                ));
                showAssetOnMap();
                if (asset.hasGeoFeature() || dashboardAttributes.size() > 0) {
                    showAssetInfoItems();
                    environment.getEventService().subscribe(
                        AttributeEvent.class,
                        new AttributeEvent.EntityIdFilter(asset.getId())
                    );
                }
            });
        } else if (realmId != null) {
            // TODO: Tenant map not implemented
        }
    }

    protected void showAssetOnMap() {
        if (asset.hasGeoFeature()) {
            GeoJSON geoFeature = asset.getGeoFeature(30);
            view.showDroppedPin(geoFeature);
            view.flyTo(asset.getCoordinates());
        }
    }

    protected void showAssetInfoItems() {
        List<Pair<String, String>> infoItems = dashboardAttributes.stream()
            .map(attribute -> new Pair<>(attribute.getLabel(), format(attribute.getFormat(), attribute.getValueAsString())))
            .collect(Collectors.toList());
        if (asset.hasGeoFeature()) {
            infoItems.add(0, new Pair<>(
                environment.getMessages().location(),
                asset.getCoordinatesLabel()
            ));

        }
        view.showInfoItems(infoItems);
    }

    protected native static String format(String formatString, String value) /*-{
        if (formatString === null)
            return value;
        return $wnd.sprintf(formatString, value);
    }-*/;


}
