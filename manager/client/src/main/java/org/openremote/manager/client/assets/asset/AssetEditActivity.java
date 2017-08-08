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
import org.openremote.manager.client.assets.AssetArrayMapper;
import org.openremote.manager.client.assets.AssetMapper;
import org.openremote.manager.client.assets.AssetQueryMapper;
import org.openremote.manager.client.assets.AssetsDashboardPlace;
import org.openremote.manager.client.assets.attributes.AttributesEditor;
import org.openremote.manager.client.assets.browser.*;
import org.openremote.manager.client.assets.tenant.AssetsTenantPlace;
import org.openremote.manager.client.event.ShowFailureEvent;
import org.openremote.manager.client.event.ShowSuccessEvent;
import org.openremote.manager.client.interop.value.ObjectValueMapper;
import org.openremote.manager.shared.asset.AssetResource;
import org.openremote.manager.shared.map.MapResource;
import org.openremote.manager.shared.security.Tenant;
import org.openremote.manager.shared.validation.ConstraintViolation;
import org.openremote.model.ValueHolder;
import org.openremote.model.asset.*;
import org.openremote.model.asset.agent.AgentLink;
import org.openremote.model.asset.agent.ProtocolConfiguration;
import org.openremote.model.attribute.AttributeType;
import org.openremote.model.attribute.MetaItem;
import org.openremote.model.event.bus.EventBus;
import org.openremote.model.event.bus.EventRegistration;
import org.openremote.model.util.Pair;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static org.openremote.manager.client.http.RequestExceptionHandler.handleRequestException;

public class AssetEditActivity
    extends AbstractAssetActivity<AssetEditPlace>
    implements AssetEdit.Presenter {

    final AssetEdit view;
    final AssetResource assetResource;
    final AssetMapper assetMapper;
    final AssetArrayMapper assetArrayMapper;
    final AssetQueryMapper assetQueryMapper;
    final MapResource mapResource;
    final ObjectValueMapper objectValueMapper;
    final protected Consumer<ConstraintViolation[]> validationErrorHandler;
    protected final List<Consumer<Asset[]>> agentAssetConsumers = new ArrayList<>();
    protected final List<Consumer<Asset[]>> allAssetConsumers = new ArrayList<>();
    protected Asset[] agentAssets;
    protected Asset[] allAssets;

    double[] selectedCoordinates;
    AttributesEditor attributesEditor;

    @Inject
    public AssetEditActivity(Environment environment,
                             Tenant currentTenant,
                             AssetBrowser.Presenter assetBrowserPresenter,
                             AssetEdit view,
                             AssetResource assetResource,
                             AssetMapper assetMapper,
                             AssetArrayMapper assetArrayMapper,
                             AssetQueryMapper assetQueryMapper,
                             MapResource mapResource,
                             ObjectValueMapper objectValueMapper) {
        super(environment, currentTenant, assetBrowserPresenter);
        this.view = view;
        this.assetResource = assetResource;
        this.assetMapper = assetMapper;
        this.assetArrayMapper = assetArrayMapper;
        this.assetQueryMapper = assetQueryMapper;
        this.mapResource = mapResource;
        this.objectValueMapper = objectValueMapper;

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
                objectValueMapper,
                mapResource::getSettings,
                200,
                view::initialiseMap,
                ex -> handleRequestException(ex, environment)
            );
        } else {
            onMapReady();
        }

        view.setAvailableAttributeTypes(AttributeType.values());
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
    public void onMapReady() {
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
        writeTypeToView(true);
        writeAttributesEditorToView();
    }

    @Override
    public void addAttribute(String name, int attributeTypeIndex) {
        if (attributesEditor != null) {
            view.setNewAttributeError(
                !attributesEditor.addAttribute(name, attributeTypeIndex >= 0 ? AttributeType.values()[attributeTypeIndex] : null)
            );
        }
    }

    @Override
    public void update() {
        view.setFormBusy(true);
        clearViewMessages();
        if (attributesEditor != null && !attributesEditor.validateAttributes()) {
            view.setFormBusy(false);
            return;
        }
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
        if (attributesEditor != null && !attributesEditor.validateAttributes()) {
            view.setFormBusy(false);
            return;
        }
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
    public void getLinkableAssets(ValueHolder value, Consumer<Asset[]> assetConsumer) {
        List<Consumer<Asset[]>> consumers;
        Asset[] retrievedAssets;
        Consumer<Asset[]> assetStore;
        AssetQuery query;

        if ((value instanceof MetaItem) && AgentLink.isAgentLink((MetaItem) value)) {
            consumers = agentAssetConsumers;
            retrievedAssets = agentAssets;
            assetStore = assets -> agentAssets = assets;
            query = new AssetQuery()
                // There shouldn't be many agents so retrieve their attributes to speed up getting
                // Protocol Configuration attribute names
                .select(new AssetQuery.Select(AssetQuery.Include.ONLY_ID_AND_NAME_AND_ATTRIBUTES))
                // Limit to agents
                .type(AssetType.AGENT);
        } else {
            consumers = allAssetConsumers;
            retrievedAssets = allAssets;
            assetStore = assets -> allAssets = assets;
            // Limit to assets that have the same realm as the asset being edited
            query = new AssetQuery()
                .select(new AssetQuery.Select(AssetQuery.Include.ONLY_ID_AND_NAME_AND_ATTRIBUTE_NAMES, true));
        }

        if (retrievedAssets != null) {
            // Already retrieved so return results
            assetConsumer.accept(retrievedAssets);
            return;
        }

        consumers.add(assetConsumer);

        if (consumers.size() == 1) {
            // Do request
            environment.getRequestService().execute(
                assetArrayMapper,
                assetQueryMapper,
                requestParams -> assetResource.queryAssets(requestParams, query),
                Collections.singletonList(200),
                assets -> {
                    assetStore.accept(assets);
                    consumers.forEach(consumer -> consumer.accept(assets));
                    consumers.clear();
                },
                exception -> {
                    Asset[] assets = new Asset[0];
                    assetStore.accept(assets);
                    consumers.forEach(consumer -> consumer.accept(assets));
                    consumers.clear();
                }
            );
        }
    }

    @Override
    public void getLinkableAttributes(Pair<ValueHolder, Asset> valueAssetPair, Consumer<AssetAttribute[]> attributeConsumer) {
        ValueHolder value = valueAssetPair.key;
        Asset asset = valueAssetPair.value;

        if ((value instanceof MetaItem) && AgentLink.isAgentLink((MetaItem) value)) {
            // Asset should have fully populated attributes so filter them
            attributeConsumer.accept(
                asset.getAttributesStream()
                    .filter(ProtocolConfiguration::isProtocolConfiguration)
                    .toArray(AssetAttribute[]::new)
            );
        } else {
            // Asset should just have attribute names and labels loaded so return them
            attributeConsumer.accept(
                asset
                    .getAttributesStream()
                    .toArray(AssetAttribute[]::new)
            );
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

    protected void startCreateAsset() {
        assetBrowserPresenter.clearSelection();
        asset = new Asset();
        asset.setName("My New Asset");
        asset.setRealmId(currentTenant.getId());
        asset.setTenantDisplayName(currentTenant.getDisplayName());
        asset.setType("urn:mydomain:customtype");
        clearViewMessages();
        writeAssetToView();
        writeParentToView();
        writeTypeToView(true);
        writeAttributesEditorToView();
        view.setFormBusy(false);
    }

    protected void startEditAsset() {
        clearViewMessages();
        view.setAssetViewHistoryToken(environment.getPlaceHistoryMapper().getToken(new AssetViewPlace(assetId)));
        writeAssetToView();
        writeTypeToView(false);
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
        view.showDroppedPin(asset.getGeoFeature(20));
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

    protected void writeTypeToView(boolean typeEditable) {
        view.selectWellKnownType(asset.getWellKnownType());
        view.setAvailableWellKnownTypes(AssetType.valuesSorted());
        view.setType(asset.getType());
        view.setTypeEditable(typeEditable);
    }

    protected void writeAttributesEditorToView() {
        attributesEditor = new AttributesEditor(
            environment,
            view.getAttributesEditorContainer(),
            assetId == null
                ? asset.getWellKnownType().getDefaultAttributes().collect(Collectors.toList())
                : asset.getAttributesList(),
            assetId == null,
            this::getLinkableAssets,
            this::getLinkableAttributes
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
            attributesEditor != null ? attributesEditor.getAttributes() : null
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
