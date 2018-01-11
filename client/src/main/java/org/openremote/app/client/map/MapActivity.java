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
package org.openremote.app.client.map;

import org.openremote.app.client.Environment;
import org.openremote.app.client.assets.AssetBrowsingActivity;
import org.openremote.app.client.assets.AssetMapper;
import org.openremote.app.client.assets.asset.AssetViewPlace;
import org.openremote.app.client.assets.browser.AssetBrowser;
import org.openremote.app.client.assets.browser.AssetBrowserSelection;
import org.openremote.app.client.assets.browser.AssetTreeNode;
import org.openremote.app.client.assets.browser.TenantTreeNode;
import org.openremote.app.client.interop.value.ObjectValueMapper;
import org.openremote.app.client.mvp.AcceptsView;
import org.openremote.app.client.mvp.AppActivity;
import org.openremote.model.asset.Asset;
import org.openremote.model.asset.AssetAttribute;
import org.openremote.model.asset.AssetResource;
import org.openremote.model.attribute.AttributeEvent;
import org.openremote.model.attribute.AttributeType;
import org.openremote.model.event.bus.EventBus;
import org.openremote.model.event.bus.EventRegistration;
import org.openremote.model.geo.GeoJSON;
import org.openremote.model.map.MapResource;
import org.openremote.model.value.Values;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static org.openremote.model.attribute.Attribute.isAttributeNameEqualTo;

public class MapActivity extends AssetBrowsingActivity<MapPlace> implements MapView.Presenter {

    final MapView view;
    final AssetResource assetResource;
    final AssetMapper assetMapper;
    final MapResource mapResource;
    final ObjectValueMapper objectValueMapper;

    String assetId;
    String realmId;
    Asset asset;
    List<AssetAttribute> dashboardAttributes = new ArrayList<>();

    @Inject
    public MapActivity(Environment environment,
                       AssetBrowser.Presenter assetBrowserPresenter,
                       MapView view,
                       AssetResource assetResource,
                       AssetMapper assetMapper,
                       MapResource mapResource,
                       ObjectValueMapper objectValueMapper) {
        super(environment, assetBrowserPresenter);
        this.view = view;
        this.assetResource = assetResource;
        this.assetMapper = assetMapper;
        this.mapResource = mapResource;
        this.objectValueMapper = objectValueMapper;
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
    public void start(AcceptsView container, EventBus eventBus, Collection<EventRegistration> registrations) {
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
                    || dashboardAttributes.stream().noneMatch(attribute -> isAttributeNameEqualTo(attribute, event.getAttributeName())))
                    return;

                dashboardAttributes
                    .stream()
                    .filter(isAttributeNameEqualTo(event.getAttributeName()))
                    .findFirst()
                    .map(attribute -> {
                        attribute.setValue(event.getValue().orElse(null), event.getTimestamp());
                        return attribute;
                    });

                showAssetInfoItems();
            }
        ));

        if (!view.isMapInitialised()) {
            environment.getApp().getRequestService().sendAndReturn(
                objectValueMapper::read,
                mapResource::getSettings,
                200,
                view::initialiseMap
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
                this.dashboardAttributes = asset.getAttributesStream()
                    .filter(AssetAttribute::isShowOnDashboard)
                    .collect(Collectors.toList());
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
        List<MapInfoItem> infoItems = dashboardAttributes.stream()
            .filter(attribute -> attribute.getLabel().isPresent())
            .map(attribute -> new MapInfoItem(
                    attribute.getType().map(AttributeType::getIcon).orElse(AttributeType.DEFAULT_ICON),
                    attribute.getLabel().get(),
                    attribute.getFormat().orElse(null),
                    attribute.getValue().orElse(null)
                )
            )
            .sorted(Comparator.comparing(MapInfoItem::getLabel))
            .collect(Collectors.toList());
        if (asset.hasGeoFeature()) {
            infoItems.add(0, new MapInfoItem(
                "map-marker",
                environment.getMessages().location(),
                null,
                Values.create(asset.getCoordinatesLabel())
            ));
        }
        view.showInfoItems(infoItems);
    }

}
