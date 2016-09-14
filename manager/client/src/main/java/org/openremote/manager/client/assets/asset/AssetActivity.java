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
package org.openremote.manager.client.assets.asset;

import com.google.gwt.user.client.ui.AcceptsOneWidget;
import org.openremote.manager.client.Environment;
import org.openremote.manager.client.assets.AssetMapper;
import org.openremote.manager.client.assets.AssetsDashboardPlace;
import org.openremote.manager.client.assets.agent.AgentAttributesEditor;
import org.openremote.manager.client.assets.agent.ConnectorArrayMapper;
import org.openremote.manager.client.assets.browser.AssetBrowser;
import org.openremote.manager.client.assets.browser.AssetBrowsingActivity;
import org.openremote.manager.client.assets.device.DeviceAttributesEditor;
import org.openremote.manager.client.event.bus.EventBus;
import org.openremote.manager.client.event.bus.EventRegistration;
import org.openremote.manager.client.interop.elemental.JsonObjectMapper;
import org.openremote.manager.client.widget.AttributesEditor;
import org.openremote.manager.shared.asset.Asset;
import org.openremote.manager.shared.asset.AssetModifiedEvent;
import org.openremote.manager.shared.asset.AssetResource;
import org.openremote.manager.shared.asset.AssetType;
import org.openremote.manager.shared.attribute.Attributes;
import org.openremote.manager.shared.connector.ConnectorResource;
import org.openremote.manager.shared.event.ui.ShowInfoEvent;
import org.openremote.manager.shared.map.MapResource;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.Collection;
import java.util.logging.Logger;

import static org.openremote.manager.client.http.RequestExceptionHandler.handleRequestException;
import static org.openremote.manager.shared.asset.AssetModifiedEvent.Cause.*;

public class AssetActivity
    extends AssetBrowsingActivity<AssetView, AssetPlace>
    implements AssetView.Presenter {

    private static final Logger LOG = Logger.getLogger(AssetActivity.class.getName());

    final protected MapResource mapResource;
    final protected JsonObjectMapper jsonObjectMapper;

    final protected ConnectorArrayMapper connectorArrayMapper;
    final protected ConnectorResource connectorResource;

    protected boolean isCreateAsset;
    protected double[] selectedCoordinates;
    protected Asset parentAsset;
    protected boolean isParentSelection;
    protected AttributesEditor attributesEditor;

    @Inject
    public AssetActivity(Environment environment,
                         AssetView view,
                         AssetBrowser.Presenter assetBrowserPresenter,
                         AssetResource assetResource,
                         AssetMapper assetMapper,
                         MapResource mapResource,
                         JsonObjectMapper jsonObjectMapper,
                         ConnectorArrayMapper connectorArrayMapper,
                         ConnectorResource connectorResource) {
        super(environment, view, assetBrowserPresenter, assetResource, assetMapper);
        this.mapResource = mapResource;
        this.jsonObjectMapper = jsonObjectMapper;
        this.connectorArrayMapper = connectorArrayMapper;
        this.connectorResource = connectorResource;
    }

    @Override
    public void start(AcceptsOneWidget container, EventBus eventBus, Collection<EventRegistration> registrations) {
        super.start(container, eventBus, registrations);

        if (!getView().isMapInitialised()) {
            environment.getRequestService().execute(
                jsonObjectMapper,
                mapResource::getSettings,
                200,
                view::initialiseMap,
                ex -> handleRequestException(ex, environment)
            );
        } else {
            view.refreshMap();
        }

        view.setAvailableTypes(AssetType.editable());
    }

    @Override
    protected void startCreateAsset() {
        super.startCreateAsset();
        isCreateAsset = true;
        view.setFormBusy(true);
        asset = new Asset();
        asset.setName("My New Asset");
        asset.setType(AssetType.CUSTOM);
        writeToView();
        clearViewFieldErrors();
        view.clearFormMessages();
        writeParentToView();
        view.setTypeSelectionEnabled(true);
        writeTypeToView();
        writeAttributesEditorToView();
        view.setFormBusy(false);
    }

    @Override
    protected void onAssetLoaded() {
        isCreateAsset = false;
        writeToView();
        clearViewFieldErrors();
        view.clearFormMessages();
        view.setTypeSelectionEnabled(false);
        view.setEditable(asset.getWellKnownType().isEditable());
        writeTypeToView();
        writeAttributesEditorToView();
        if (asset.getParentId() != null) {
            loadAsset(asset.getParentId(), loadedParentAsset -> {
                this.parentAsset = loadedParentAsset;
                writeParentToView();
                view.setFormBusy(false);
            });
        } else {
            writeParentToView();
            view.setFormBusy(false);
        }
    }

    @Override
    protected void onAssetsDeselected() {
    }

    @Override
    protected void onAssetSelectionChange(String selectedAssetId) {
        if (isParentSelection) {
            loadAsset(selectedAssetId, loadedParentAsset -> {
                if (Arrays.asList(loadedParentAsset.getPath()).contains(asset.getId())) {
                    environment.getEventBus().dispatch(
                        new ShowInfoEvent(environment.getMessages().invalidAssetParent())
                    );
                } else {
                    parentAsset = loadedParentAsset;
                    writeParentToView();
                }
            });
        } else {
            environment.getPlaceController().goTo(new AssetPlace(selectedAssetId));
        }
    }

    @Override
    protected void onBeforeAssetLoad() {
        view.setFormBusy(true);
    }

    @Override
    public void onStop() {
        super.onStop();
        clearViewFieldErrors();

        view.setPresenter(null);
        view.clearFormMessages();
    }

    @Override
    public void onMapClicked(double lng, double lat) {
        selectedCoordinates = new double[]{lng, lat};
        view.showMapPopup(lng, lat, environment.getMessages().selectedLocation());
        view.setLocation(getLocation(selectedCoordinates));
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
        view.clearFormMessages();
        clearViewFieldErrors();

        // If the asset was in the root of the tree (so this check must be before
        // we read the new parent asset state from the view), force an asset tree
        // root refresh when the update is complete
        boolean forceRootRefresh = asset.getParentId() == null;

        readFromView();

        environment.getRequestService().execute(
            assetMapper,
            requestParams -> {
                assetResource.update(requestParams, assetId, asset);
            },
            204,
            () -> {
                view.setFormBusy(false);
                environment.getEventBus().dispatch(new ShowInfoEvent(
                    environment.getMessages().assetUpdated(asset.getName())
                ));
                environment.getEventBus().dispatch(new AssetModifiedEvent(asset, UPDATE, forceRootRefresh));
                environment.getPlaceController().goTo(new AssetPlace(assetId));
            },
            ex -> handleRequestException(ex, environment)
        );
    }

    @Override
    public void create() {
        view.setFormBusy(true);
        view.clearFormMessages();
        clearViewFieldErrors();
        readFromView();
        environment.getRequestService().execute(
            assetMapper,
            requestParams -> {
                assetResource.create(requestParams, asset);
            },
            204,
            () -> {
                view.setFormBusy(false);
                environment.getEventBus().dispatch(new ShowInfoEvent(
                    environment.getMessages().assetCreated(asset.getName())
                ));
                environment.getEventBus().dispatch(new AssetModifiedEvent(asset, CREATE));
                environment.getPlaceController().goTo(new AssetsDashboardPlace());
            },
            ex -> handleRequestException(ex, environment)
        );
    }

    @Override
    public void delete() {
        view.showConfirmation(
            environment.getMessages().confirmation(),
            environment.getMessages().confirmationDelete(asset.getName()),
            () -> {
                view.setFormBusy(true);
                view.clearFormMessages();
                clearViewFieldErrors();
                environment.getRequestService().execute(
                    requestParams -> {
                        assetResource.delete(requestParams, this.assetId);
                    },
                    204,
                    () -> {
                        view.setFormBusy(false);
                        environment.getEventBus().dispatch(new ShowInfoEvent(
                            environment.getMessages().assetDeleted(asset.getName())
                        ));
                        environment.getEventBus().dispatch(new AssetModifiedEvent(asset, DELETE));
                        environment.getPlaceController().goTo(new AssetsDashboardPlace());
                    },
                    ex -> handleRequestException(ex, environment)
                );
            }
        );
    }

    @Override
    public void beginParentSelection() {
        isParentSelection = true;
        view.setParentSelection(true);
    }

    @Override
    public void confirmParentSelection() {
        isParentSelection = false;
        assetBrowserPresenter.selectAsset(asset.getId(), asset.getPath());
        view.setParentSelection(false);
    }

    @Override
    public void setRootParentSelection() {
        this.parentAsset = null;
        writeParentToView();
    }

    @Override
    public void resetParentSelection() {
        isParentSelection = false;
        assetBrowserPresenter.selectAsset(asset.getId(), asset.getPath());
        view.setParentSelection(false);
        if (asset.getParentId() != null) {
            loadAsset(asset.getParentId(), loadedParentAsset -> {
                this.parentAsset = loadedParentAsset;
                writeParentToView();
            });
        }
    }

    @Override
    public void centerMap() {
        if (selectedCoordinates != null) {
            view.flyTo(selectedCoordinates);
        } else if (asset.getCoordinates() != null) {
            view.flyTo(asset.getCoordinates());
        }
    }

    protected void writeToView() {
        view.setName(asset.getName());
        view.setCreatedOn(asset.getCreatedOn());
        view.setLocation(getLocation(asset.getCoordinates()));
        if (asset != null && asset.getId() != null) {
            view.showFeaturesSelection(getFeature(asset));
            view.flyTo(asset.getCoordinates());
        } else {
            view.hideFeaturesSelection();
        }
        view.enableCreate(isCreateAsset);
        view.enableUpdate(!isCreateAsset);
        view.enableDelete(!isCreateAsset);
    }

    protected void writeParentToView() {
        view.setParent(
            parentAsset != null ? parentAsset.getName() : environment.getMessages().assetHasNoParent()
        );
    }

    protected void writeTypeToView() {
        AssetType assetType = asset.getWellKnownType();
        view.selectType(assetType);
        view.setTypeInputVisible(AssetType.CUSTOM.equals(assetType));
        view.setType(asset.getType());
    }

    protected void writeAttributesEditorToView() {
        switch(asset.getWellKnownType()) {
            case DEVICE:
                attributesEditor = new DeviceAttributesEditor(
                    environment,
                    view.getDeviceAttributesEditorContainer(),
                    new Attributes(asset.getAttributes())
                );
                break;
            case AGENT:
                attributesEditor = new AgentAttributesEditor(
                    environment,
                    view.getAttributesEditorContainer(),
                    new Attributes(asset.getAttributes()),
                    connectorResource,
                    connectorArrayMapper
                );
                break;
            default:
                attributesEditor = new AttributesEditor<>(
                    environment,
                    view.getAttributesEditorContainer(),
                    new Attributes(asset.getAttributes())
                );
        }
        view.setAttributesEditor(attributesEditor);
    }

    protected void readFromView() {
        asset.setName(view.getName());
        if (AssetType.CUSTOM.equals(asset.getWellKnownType())) {
            asset.setType(view.getType());
        }
        if (selectedCoordinates != null) {
            asset.setCoordinates(selectedCoordinates);
        }
        asset.setParentId(parentAsset != null ? parentAsset.getId() : null);
        asset.setAttributes(
            attributesEditor != null ? attributesEditor.getAttributes().getJsonObject() : null
        );
    }

    protected void clearViewFieldErrors() {
        // TODO: Validation
    }

    protected String getLocation(double[] coordinates) {
        if (coordinates != null && coordinates.length == 2) {
            return coordinates[0] + " " + coordinates[1];
        }
        return null;
    }

}
