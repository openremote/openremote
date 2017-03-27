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
import elemental.json.Json;
import elemental.json.JsonValue;
import org.openremote.manager.client.Environment;
import org.openremote.manager.client.admin.TenantMapper;
import org.openremote.manager.client.assets.AssetMapper;
import org.openremote.manager.client.assets.attributes.AttributesBrowser;
import org.openremote.manager.client.assets.browser.AssetBrowser;
import org.openremote.manager.client.assets.browser.AssetBrowserSelection;
import org.openremote.manager.client.assets.browser.AssetTreeNode;
import org.openremote.manager.client.assets.browser.TenantTreeNode;
import org.openremote.manager.client.assets.tenant.AssetsTenantPlace;
import org.openremote.manager.client.event.bus.EventBus;
import org.openremote.manager.client.event.bus.EventRegistration;
import org.openremote.manager.client.interop.elemental.JsonObjectMapper;
import org.openremote.manager.client.map.MapView;
import org.openremote.manager.shared.asset.AssetResource;
import org.openremote.manager.shared.http.EntityWriter;
import org.openremote.manager.shared.map.MapResource;
import org.openremote.manager.shared.security.Tenant;
import org.openremote.manager.shared.security.TenantResource;
import org.openremote.model.Runnable;
import org.openremote.model.asset.AssetAttribute;
import org.openremote.model.asset.AssetAttributes;
import org.openremote.model.asset.AssetType;

import javax.inject.Inject;
import java.util.Collection;

import static org.openremote.manager.client.http.RequestExceptionHandler.handleRequestException;

public class AssetViewActivity
    extends AbstractAssetActivity<AssetViewPlace>
    implements AssetView.Presenter {

    final AssetView view;
    final AssetResource assetResource;
    final AssetMapper assetMapper;
    final MapResource mapResource;
    final JsonObjectMapper jsonObjectMapper;
    final TenantResource tenantResource;
    final TenantMapper tenantMapper;

    AttributesBrowser attributesBrowser;

    @Inject
    public AssetViewActivity(Environment environment,
                             AssetBrowser.Presenter assetBrowserPresenter,
                             AssetView view,
                             AssetResource assetResource,
                             AssetMapper assetMapper,
                             MapResource mapResource,
                             JsonObjectMapper jsonObjectMapper,
                             TenantResource tenantResource,
                             TenantMapper tenantMapper) {
        super(environment, assetBrowserPresenter);
        this.view = view;
        this.assetResource = assetResource;
        this.assetMapper = assetMapper;
        this.mapResource = mapResource;
        this.jsonObjectMapper = jsonObjectMapper;
        this.tenantResource = tenantResource;
        this.tenantMapper = tenantMapper;
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
                jsonObjectMapper,
                mapResource::getSettings,
                200,
                view::initialiseMap,
                ex -> handleRequestException(ex, environment)
            );
        }

        asset = null;
        if (assetId != null) {
            assetBrowserPresenter.loadAsset(assetId, loadedAsset -> {
                this.asset = loadedAsset;
                assetBrowserPresenter.selectAsset(asset);
                writeAssetToView();
                writeAttributesBrowserToView();
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
    public void onStop() {
        super.onStop();
        if (attributesBrowser != null) {
            attributesBrowser.close();
        }
        view.setPresenter(null);
    }

    @Override
    public void centerMap() {
        view.flyTo(asset.getCoordinates());
    }

    @Override
    public void edit() {
        environment.getPlaceController().goTo(new AssetEditPlace(assetId));
    }

    protected void writeAssetToView() {
        view.setName(asset.getName());
        view.setCreatedOn(asset.getCreatedOn());
        view.setLocation(asset.getCoordinates());
        view.showFeaturesSelection(MapView.getFeature(asset));
        view.flyTo(asset.getCoordinates());
        view.setType(asset.getWellKnownType() != AssetType.CUSTOM ? asset.getWellKnownType().name() : asset.getType());
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
            new AssetAttributes(asset.getAttributes())
        ) {
            @Override
            protected void readAttributeValue(AssetAttribute attribute, Runnable onSuccess) {
                environment.getRequestService().execute(
                    value -> value,
                    requestParams -> assetResource.readAttributeValue(requestParams, assetId, attribute.getName()),
                    200,
                    result -> {
                        JsonValue value = Json.instance().parse(result);
                        // TODO Bug in Elemental, can't check this value for null etc.
                        attribute.setValueUnchecked(value);
                        showInfo(container.getMessages().attributeValueRefreshed(attribute.getName()));
                        onSuccess.run();
                    },
                    ex -> handleRequestException(ex, environment)
                );
            }

            @Override
            protected void writeAttributeValue(AssetAttribute attribute) {
                environment.getRequestService().execute(
                    (EntityWriter<String>) value -> value,
                    requestParams -> assetResource.writeAttributeValue(
                        requestParams, assetId, attribute.getName(), attribute.getValue().toJson()
                    ),
                    204,
                    () -> showSuccess(container.getMessages().attributeValueStored(attribute.getName())),
                    ex -> handleRequestException(ex, environment)
                );
            }
        };
        view.setAttributesBrowser(attributesBrowser);
        attributesBrowser.build();
    }
}
