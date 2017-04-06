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
import org.openremote.manager.client.assets.AssetsDashboardPlace;
import org.openremote.manager.client.assets.attributes.AttributesEditor;
import org.openremote.manager.client.assets.browser.*;
import org.openremote.manager.client.assets.tenant.AssetsTenantPlace;
import org.openremote.manager.client.event.ShowFailureEvent;
import org.openremote.manager.client.event.ShowSuccessEvent;
import org.openremote.manager.client.interop.elemental.JsonObjectMapper;
import org.openremote.manager.client.map.MapView;
import org.openremote.manager.shared.asset.AssetResource;
import org.openremote.manager.shared.map.MapResource;
import org.openremote.manager.shared.security.Tenant;
import org.openremote.manager.shared.validation.ConstraintViolation;
import org.openremote.model.Consumer;
import org.openremote.model.asset.Asset;
import org.openremote.model.asset.AssetAttributes;
import org.openremote.model.asset.AssetType;
import org.openremote.model.event.bus.EventBus;
import org.openremote.model.event.bus.EventRegistration;

import javax.inject.Inject;
import java.util.Collection;

import static org.openremote.manager.client.http.RequestExceptionHandler.handleRequestException;

public class AssetEditActivity
    extends AbstractAssetActivity<AssetEditPlace>
    implements AssetEdit.Presenter {

    final AssetEdit view;
    final AssetResource assetResource;
    final AssetMapper assetMapper;
    final MapResource mapResource;
    final JsonObjectMapper jsonObjectMapper;
    final protected Consumer<ConstraintViolation[]> validationErrorHandler;

    double[] selectedCoordinates;
    AttributesEditor attributesEditor;

    @Inject
    public AssetEditActivity(Environment environment,
                             Tenant currentTenant,
                             AssetBrowser.Presenter assetBrowserPresenter,
                             AssetEdit view,
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

        validationErrorHandler = violations -> {
            for (ConstraintViolation violation : violations) {
                if (violation.getPath() != null) {
                    if (violation.getPath().endsWith("name")) {
                        view.setNameError(true);
                    } else if (violation.getPath().endsWith("type")) {
                        view.setTypeError(true);
                    }
                }
                view.addFormMessageError(violation.getMessage());
            }
            view.setFormBusy(false);
        };

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
                            new AssetEditPlace(event.getSelectedNode().getId())
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
                startEditAsset();
            });
        } else {
            startCreateAsset();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (attributesEditor != null) {
            attributesEditor.close();
        }
        clearViewMessages();
        view.setPresenter(null);
    }

    @Override
    public void view() {
        if (assetId != null) {
            environment.getPlaceController().goTo(new AssetViewPlace(assetId));
        }
    }

    @Override
    public void onParentSelection(BrowserTreeNode treeNode) {
        if (treeNode instanceof TenantTreeNode) {
            asset.setRealmId(treeNode.getId());
            asset.setTenantDisplayName(treeNode.getLabel());
            parentAsset = null;
        } else if (treeNode instanceof AssetTreeNode) {
            assetBrowserPresenter.loadAsset(treeNode.getId(), loadedAsset -> {
                // The selected parent can not be our child, or a leaf, or the same
                if (loadedAsset.pathContains(asset.getId())
                    || treeNode.isLeaf()
                    || loadedAsset.getId().equals(asset.getId())) {
                    environment.getEventBus().dispatch(
                        new ShowFailureEvent(environment.getMessages().invalidAssetParent(), 3000)
                    );
                    writeParentToView();
                } else {
                    parentAsset = loadedAsset;
                }
            });
        }
    }

    @Override
    public void onMapClicked(double lng, double lat) {
        selectedCoordinates = new double[]{lng, lat};
        view.showMapPopup(lng, lat, environment.getMessages().selectedLocation());
        view.setLocation(selectedCoordinates);
    }

    @Override
    public void onAssetTypeSelected(AssetType type) {
        asset.setType(type);
        writeTypeToView();
        writeAttributesEditorToView();
    }

    @Override
    public void update() {
        view.setFormBusy(true);
        clearViewMessages();
        readFromView();
        environment.getRequestService().execute(
            assetMapper,
            requestParams -> assetResource.update(requestParams, assetId, asset),
            204,
            () -> {
                view.setFormBusy(false);
                environment.getEventBus().dispatch(new ShowSuccessEvent(
                    environment.getMessages().assetUpdated(asset.getName())
                ));
                environment.getPlaceController().goTo(new AssetViewPlace(assetId));
            },
            ex -> handleRequestException(ex, environment.getEventBus(), environment.getMessages(), validationErrorHandler)
        );
    }

    @Override
    public void create() {
        view.setFormBusy(true);
        clearViewMessages();
        readFromView();
        environment.getRequestService().execute(
            assetMapper,
            requestParams -> assetResource.create(requestParams, asset),
            204,
            () -> {
                view.setFormBusy(false);
                environment.getEventBus().dispatch(new ShowSuccessEvent(
                    environment.getMessages().assetCreated(asset.getName())
                ));
                environment.getPlaceController().goTo(new AssetsDashboardPlace());
            },
            ex -> handleRequestException(ex, environment.getEventBus(), environment.getMessages(), validationErrorHandler)
        );
    }

    @Override
    public void delete() {
        view.showConfirmation(
            environment.getMessages().confirmation(),
            environment.getMessages().confirmationDelete(asset.getName()),
            () -> {
                view.setFormBusy(true);
                clearViewMessages();
                environment.getRequestService().execute(
                    requestParams -> assetResource.delete(requestParams, this.assetId),
                    204,
                    () -> {
                        view.setFormBusy(false);
                        environment.getEventBus().dispatch(new ShowSuccessEvent(
                            environment.getMessages().assetDeleted(asset.getName())
                        ));
                        environment.getPlaceController().goTo(new AssetsDashboardPlace());
                    },
                    ex -> handleRequestException(ex, environment.getEventBus(), environment.getMessages(), validationErrorHandler)
                );
            }
        );
    }

    @Override
    public void centerMap() {
        if (selectedCoordinates != null) {
            view.flyTo(selectedCoordinates);
        } else if (asset.getCoordinates() != null) {
            view.flyTo(asset.getCoordinates());
        }
    }

    protected void startCreateAsset() {
        assetBrowserPresenter.clearSelection();
        asset = new Asset();
        asset.setName("My New Asset");
        asset.setRealmId(currentTenant.getId());
        asset.setTenantDisplayName(currentTenant.getDisplayName());
        asset.setType("urn:mydomain:customtype");
        clearViewMessages();
        view.setTypeSelectionEnabled(true);
        writeAssetToView();
        writeParentToView();
        writeTypeToView();
        writeAttributesEditorToView();
        view.setFormBusy(false);
    }

    protected void startEditAsset() {
        clearViewMessages();
        view.setTypeSelectionEnabled(false);
        writeAssetToView();
        writeTypeToView();
        writeAttributesEditorToView();
        if (asset.getParentId() != null) {
            assetBrowserPresenter.loadAsset(asset.getParentId(), loadedAsset -> {
                this.parentAsset = loadedAsset;
                writeParentToView();
                view.setFormBusy(false);
            });
        } else {
            writeParentToView();
            view.setFormBusy(false);
        }
    }

    protected void writeAssetToView() {
        view.setName(asset.getName());
        view.setCreatedOn(asset.getCreatedOn());
        view.setLocation(asset.getCoordinates());
        view.showFeaturesSelection(MapView.getFeature(asset));
        view.flyTo(asset.getCoordinates());
        view.enableCreate(assetId == null);
        view.enableUpdate(assetId != null);
        view.enableDelete(assetId != null);
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

    protected void writeTypeToView() {
        AssetType assetType = asset.getWellKnownType();
        view.selectType(assetType);
        view.setAvailableTypes(AssetType.valuesSorted());
        view.setTypeInputVisible(AssetType.CUSTOM.equals(assetType));
        view.setType(asset.getType());
    }

    protected void writeAttributesEditorToView() {
        attributesEditor = new AttributesEditor(
            environment,
            view.getAttributesEditorContainer(),
            assetId == null
                ? asset.getWellKnownType().getDefaultAttributes()
                : new AssetAttributes(asset),
            assetId == null
        );
        view.setAttributesEditor(attributesEditor);
        attributesEditor.build();
    }

    protected void readFromView() {
        asset.setName(view.getName());
        if (parentAsset != null) {
            asset.setRealmId(parentAsset.getRealmId());
            asset.setTenantDisplayName(parentAsset.getTenantDisplayName());
            asset.setParentId(parentAsset.getId());
        } else {
            asset.setParentId(null);
        }
        if (AssetType.CUSTOM.equals(asset.getWellKnownType())) {
            asset.setType(view.getType());
        }
        if (selectedCoordinates != null) {
            asset.setCoordinates(selectedCoordinates);
        }
        asset.setAttributes(
            attributesEditor != null ? attributesEditor.getAttributes().getJsonObject() : null
        );
    }

    protected void clearViewMessages() {
        view.clearFormMessages();
        clearViewFieldErrors();
    }

    protected void clearViewFieldErrors() {
        view.setNameError(false);
        view.setTypeError(false);
    }

}
