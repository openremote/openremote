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
package org.openremote.manager.client.assets.asset;

import com.google.gwt.user.client.ui.AcceptsOneWidget;
import org.openremote.manager.client.Environment;
import org.openremote.manager.client.assets.AssetMapper;
import org.openremote.manager.client.assets.attributes.AttributesBrowser;
import org.openremote.manager.client.assets.browser.AssetBrowser;
import org.openremote.manager.client.assets.browser.AssetBrowserSelection;
import org.openremote.manager.client.assets.browser.AssetTreeNode;
import org.openremote.manager.client.assets.browser.TenantTreeNode;
import org.openremote.manager.client.assets.tenant.AssetsTenantPlace;
import org.openremote.manager.client.datapoint.NumberDatapointArrayMapper;
import org.openremote.manager.client.interop.value.ObjectValueMapper;
import org.openremote.manager.shared.asset.AssetResource;
import org.openremote.manager.shared.datapoint.AssetDatapointResource;
import org.openremote.manager.shared.map.MapResource;
import org.openremote.manager.shared.security.Tenant;
import org.openremote.model.asset.AssetAttribute;
import org.openremote.model.datapoint.DatapointInterval;
import org.openremote.model.datapoint.NumberDatapoint;
import org.openremote.model.event.bus.EventBus;
import org.openremote.model.event.bus.EventRegistration;

import javax.inject.Inject;
import java.util.Collection;
import java.util.function.Consumer;

import static org.openremote.manager.client.http.RequestExceptionHandler.handleRequestException;

public class AssetViewActivity
    extends AbstractAssetActivity<AssetViewPlace>
    implements AssetView.Presenter {

    final AssetView view;
    final AssetResource assetResource;
    final AssetMapper assetMapper;
    final AssetDatapointResource assetDatapointResource;
    final NumberDatapointArrayMapper numberDatapointArrayMapper;
    final MapResource mapResource;
    final ObjectValueMapper objectValueMapper;

    AttributesBrowser attributesBrowser;

    @Inject
    public AssetViewActivity(Environment environment,
                             Tenant currentTenant,
                             AssetBrowser.Presenter assetBrowserPresenter,
                             AssetView view,
                             AssetResource assetResource,
                             AssetMapper assetMapper,
                             AssetDatapointResource assetDatapointResource,
                             NumberDatapointArrayMapper numberDatapointArrayMapper,
                             MapResource mapResource,
                             ObjectValueMapper objectValueMapper) {
        super(environment, currentTenant, assetBrowserPresenter);
        this.view = view;
        this.assetResource = assetResource;
        this.assetMapper = assetMapper;
        this.assetDatapointResource = assetDatapointResource;
        this.numberDatapointArrayMapper = numberDatapointArrayMapper;
        this.mapResource = mapResource;
        this.objectValueMapper = objectValueMapper;
    }

    @Override
    public void start(AcceptsOneWidget container, EventBus eventBus, Collection<EventRegistration> registrations) {
        view.setPresenter(this);
        container.setWidget(view.asWidget());

        registrations.add(eventBus.register(
            AssetBrowserSelection.class, event -> {
                if (event.getSelectedNode() instanceof TenantTreeNode) {
                    environment.getPlaceController().goTo(
                        new AssetsTenantPlace(event.getSelectedNode().getId())
                    );
                } else if (event.getSelectedNode() instanceof AssetTreeNode) {
                    if (this.assetId == null || !this.assetId.equals(event.getSelectedNode().getId())) {
                        environment.getPlaceController().goTo(
                            new AssetViewPlace(event.getSelectedNode().getId())
                        );
                    }
                }
            }
        ));

        if (!view.isMapInitialised()) {
            environment.getRequestService().execute(
                objectValueMapper,
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
        if (attributesBrowser != null) {
            attributesBrowser.close();
        }
        view.setPresenter(null);
    }

    @Override
    public void onMapReady() {
        asset = null;
        if (assetId != null) {
            assetBrowserPresenter.loadAsset(assetId, loadedAsset -> {
                this.asset = loadedAsset;
                assetBrowserPresenter.selectAsset(asset);
                writeAssetToView();
                writeAttributesBrowserToView();
                // TODO This fails if the user is restricted, can't just load parent and assume we have access
                if (asset.getParentId() != null) {
                    assetBrowserPresenter.loadAsset(asset.getParentId(), loadedParentAsset -> {
                        this.parentAsset = loadedParentAsset;
                        writeParentToView();
                        view.setFormBusy(false);
                    });
                } else {
                    writeParentToView();
                    view.setFormBusy(false);
                }
            });
        }
    }

    @Override
    public void centerMap() {
        view.flyTo(asset.getCoordinates());
    }

    protected void writeAssetToView() {
        view.setAssetEditHistoryToken(environment.getPlaceHistoryMapper().getToken(new AssetEditPlace(assetId)));
        view.setName(asset.getName());
        view.setCreatedOn(asset.getCreatedOn());
        view.setLocation(asset.getCoordinates());
        view.showDroppedPin(asset.getGeoFeature(20));
        view.flyTo(asset.getCoordinates());
        view.setIconAndType(asset.getWellKnownType().getIcon(), asset.getType());
    }

    protected void writeParentToView() {
        if (parentAsset != null) {
            view.setParentNode(new AssetTreeNode(parentAsset));
        } else {
            view.setParentNode(
                new TenantTreeNode(
                    new Tenant(asset.getRealmId(), asset.getTenantRealm(), asset.getTenantDisplayName(), true)
                )
            );
        }
    }

    protected void writeAttributesBrowserToView() {
        attributesBrowser = new AttributesBrowser(
            environment,
            view.getAttributesBrowserContainer(),
            asset
        ) {
            @Override
            protected void getNumberDatapoints(AssetAttribute attribute,
                                               DatapointInterval interval,
                                               long timestamp,
                                               Consumer<NumberDatapoint[]> consumer) {
                environment.getRequestService().execute(
                    numberDatapointArrayMapper,
                    requestParams -> assetDatapointResource.getNumberDatapoints(
                        requestParams, assetId, attribute.getName(), interval, timestamp
                    ),
                    200,
                    consumer,
                    ex -> handleRequestException(ex, environment)
                );

            }
        };
        view.setAttributesBrowser(attributesBrowser);
        attributesBrowser.build();
    }
}
