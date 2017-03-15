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
import org.openremote.manager.client.assets.AssetBrowsingActivity;
import org.openremote.manager.client.assets.AssetMapper;
import org.openremote.manager.client.assets.AssetsDashboardPlace;
import org.openremote.manager.client.assets.browser.AssetBrowser;
import org.openremote.manager.client.assets.browser.AssetBrowserSelection;
import org.openremote.manager.client.assets.tenant.AssetsTenantPlace;
import org.openremote.manager.client.event.ShowInfoEvent;
import org.openremote.manager.client.event.bus.EventBus;
import org.openremote.manager.client.event.bus.EventRegistration;
import org.openremote.manager.client.interop.elemental.JsonObjectMapper;
import org.openremote.manager.client.map.MapView;
import org.openremote.manager.client.mvp.AppActivity;
import org.openremote.manager.client.widget.AttributesEditor;
import org.openremote.manager.shared.asset.AssetResource;
import org.openremote.manager.shared.map.MapResource;
import org.openremote.model.Attributes;
import org.openremote.model.Consumer;
import org.openremote.model.asset.Asset;
import org.openremote.model.asset.AssetType;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.Collection;
import java.util.logging.Logger;

import static org.openremote.manager.client.http.RequestExceptionHandler.handleRequestException;

public class AssetActivity
    extends AssetBrowsingActivity<AssetPlace>
    implements AssetView.Presenter {

    private static final Logger LOG = Logger.getLogger(AssetActivity.class.getName());

    final AssetView view;
    final AssetResource assetResource;
    final AssetMapper assetMapper;
    final MapResource mapResource;
    final JsonObjectMapper jsonObjectMapper;

    String assetId;
    Asset asset;
    boolean isCreateAsset;
    double[] selectedCoordinates;
    String realm;
    Asset parentAsset;
    boolean isParentSelection;
    AttributesEditor attributesEditor;

    @Inject
    public AssetActivity(Environment environment,
                         AssetBrowser.Presenter assetBrowserPresenter,
                         AssetView view,
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
    protected AppActivity<AssetPlace> init(AssetPlace place) {
        this.assetId = place.getAssetId();
        return this;
    }

    @Override
    public void start(AcceptsOneWidget container, EventBus eventBus, Collection<EventRegistration> registrations) {
        view.setPresenter(this);
        container.setWidget(view.asWidget());

        registrations.add(eventBus.register(AssetBrowserSelection.class, event -> {
            if (event.isTenantSelection()) {
                onTenantSelected(event.getSelectedNode().getRealm());
            } else if (event.isAssetSelection()) {
                onAssetSelected(event.getSelectedNode().getId());
            }
        }));

        if (!view.isMapInitialised()) {
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

        asset = null;
        if (assetId != null) {
            onBeforeAssetLoad();
            loadAsset(assetId, loadedAsset -> {
                this.asset = loadedAsset;
                assetBrowserPresenter.selectAsset(asset);
                onAssetLoaded();
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
        clearViewFieldErrors();

        view.setPresenter(null);
        view.clearFormMessages();
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
        view.clearFormMessages();
        clearViewFieldErrors();
        readFromView();
        readParent();
        environment.getRequestService().execute(
            assetMapper,
            requestParams -> assetResource.update(requestParams, assetId, asset),
            204,
            () -> {
                view.setFormBusy(false);
                environment.getEventBus().dispatch(new ShowInfoEvent(
                    environment.getMessages().assetUpdated(asset.getName())
                ));
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
        readParent();
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
                        environment.getPlaceController().goTo(new AssetsDashboardPlace());
                    },
                    ex -> handleRequestException(ex, environment)
                );
            }
        );
    }

    @Override
    public void beginParentSelection() {
        if (!isCreateAsset) {
            assetBrowserPresenter.selectAsset(null);
        }
        view.setParentSelection(true);
        isParentSelection = true;
    }

    @Override
    public void confirmParentSelection() {
        if (!isCreateAsset) {
            assetBrowserPresenter.selectAsset(asset);
        } else {
            assetBrowserPresenter.selectAsset(null);
        }
        view.setParentSelection(false);
        isParentSelection = false;
    }

    @Override
    public void setRootParentSelection() {
        this.parentAsset = null;
        writeParentToView();
    }

    @Override
    public void resetParentSelection() {
        isParentSelection = false;
        assetBrowserPresenter.selectAsset(asset);
        view.setParentSelection(false);
        if (asset.getParentId() != null) {
            loadAsset(asset.getParentId(), loadedParentAsset -> {
                this.realm = loadedParentAsset.getRealm();
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

    protected void loadAsset(String id, Consumer<Asset> assetConsumer) {
        environment.getRequestService().execute(
            assetMapper,
            requestParams -> assetResource.get(requestParams, id),
            200,
            assetConsumer,
            ex -> handleRequestException(ex, environment)
        );
    }

    protected void startCreateAsset() {
        assetBrowserPresenter.clearSelection();
        isCreateAsset = true;
        realm = environment.getSecurityService().getAuthenticatedRealm();
        view.setFormBusy(true);
        asset = new Asset();
        asset.setName("My New Asset");
        asset.setRealm(realm);
        asset.setType("urn:mydomain:customtype");
        writeToView();
        clearViewFieldErrors();
        view.clearFormMessages();
        writeParentToView();
        view.setTypeSelectionEnabled(true);
        writeTypeToView();
        writeAttributesEditorToView();
        view.setFormBusy(false);
    }

    protected void onBeforeAssetLoad() {
        view.setFormBusy(true);
    }

    protected void onAssetLoaded() {
        isCreateAsset = false;
        realm = asset.getRealm();
        writeToView();
        clearViewFieldErrors();
        view.clearFormMessages();
        view.setTypeSelectionEnabled(false);
        view.setEditable(asset.getWellKnownType().isEditable());
        writeTypeToView();
        if (asset.getParentId() != null) {
            loadAsset(asset.getParentId(), loadedParentAsset -> {
                this.parentAsset = loadedParentAsset;
                writeParentToView();
                writeAttributesEditorToView();
                view.setFormBusy(false);
            });
        } else {
            writeParentToView();
            writeAttributesEditorToView();
            view.setFormBusy(false);
        }
    }

    protected void onTenantSelected(String realm) {
        if (isParentSelection) {
            this.parentAsset = null;
            this.realm = realm;
            writeParentToView();
        } else {
            environment.getPlaceController().goTo(new AssetsTenantPlace(this.realm));
        }
    }

    protected void onAssetSelected(String assetId) {
        if (isParentSelection) {
            loadAsset(assetId, loadedParentAsset -> {
                if (Arrays.asList(loadedParentAsset.getPath()).contains(asset.getId())) {
                    environment.getEventBus().dispatch(
                        new ShowInfoEvent(environment.getMessages().invalidAssetParent())
                    );
                } else {
                    this.realm = loadedParentAsset.getRealm();
                    parentAsset = loadedParentAsset;
                    writeParentToView();
                }
            });
        } else {
            environment.getPlaceController().goTo(new AssetPlace(assetId));
        }
    }

    protected void writeToView() {
        view.setName(asset.getName());
        view.setRealm(asset.getRealm());
        view.setCreatedOn(asset.getCreatedOn());
        view.setLocation(asset.getCoordinates());
        if (asset != null && asset.getId() != null) {
            view.showFeaturesSelection(MapView.getFeature(asset));
            view.flyTo(asset.getCoordinates());
        } else {
            view.hideFeaturesSelection();
        }
        view.enableCreate(isCreateAsset);
        view.enableUpdate(!isCreateAsset);
        view.enableDelete(!isCreateAsset);
    }

    protected void writeParentToView() {
        view.setParent(parentAsset != null ? parentAsset.getName() : environment.getMessages().assetHasNoParent());
        view.setRealm(realm);
    }

    protected void writeTypeToView() {
        AssetType assetType = asset.getWellKnownType();
        view.selectType(assetType);
        view.setTypeInputVisible(AssetType.CUSTOM.equals(assetType));
        view.setType(asset.getType());
    }

    protected void writeAttributesEditorToView() {
        switch (asset.getWellKnownType()) {
            /* TODO Implement asset type-specific editors
            case AGENT:
                attributesEditor = new AgentAttributesEditor(
                    environment,
                    view.getAttributesEditorContainer(),
                    new Attributes(asset.getAttributes()),
                    isCreateAsset,
                    asset,
                    connectorResource,
                    connectorArrayMapper
                );
                break;
            */
            default:
                attributesEditor = new AttributesEditor<>(
                    environment,
                    view.getAttributesEditorContainer(),
                    new Attributes(
                        isCreateAsset
                            ? asset.getWellKnownType().getDefaultAttributes()
                            : asset.getAttributes()
                    )
                );
        }
        view.setAttributesEditor(attributesEditor);
        attributesEditor.build();
    }

    protected void readFromView() {
        asset.setName(view.getName());
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

    protected void readParent() {
        if (parentAsset != null) {
            asset.setRealm(parentAsset.getRealm());
            asset.setParentId(parentAsset.getId());
        } else {
            asset.setRealm(realm);
            asset.setParentId(null);
        }
    }

    protected void clearViewFieldErrors() {
        // TODO: Validation
    }

}
