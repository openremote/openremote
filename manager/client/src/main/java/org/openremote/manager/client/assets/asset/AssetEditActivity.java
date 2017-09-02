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

import com.google.gwt.user.client.ui.IsWidget;
import com.google.inject.Provider;
import org.openremote.manager.client.Environment;
import org.openremote.manager.client.app.dialog.JsonEditor;
import org.openremote.manager.client.assets.*;
import org.openremote.manager.client.assets.attributes.*;
import org.openremote.manager.client.assets.browser.AssetBrowser;
import org.openremote.manager.client.assets.browser.AssetTreeNode;
import org.openremote.manager.client.assets.browser.BrowserTreeNode;
import org.openremote.manager.client.assets.browser.TenantTreeNode;
import org.openremote.manager.client.event.ShowFailureEvent;
import org.openremote.manager.client.event.ShowSuccessEvent;
import org.openremote.manager.client.interop.value.ObjectValueMapper;
import org.openremote.manager.client.widget.FormButton;
import org.openremote.manager.client.widget.FormSectionLabel;
import org.openremote.manager.client.widget.ValueEditors;
import org.openremote.manager.shared.agent.AgentResource;
import org.openremote.manager.shared.asset.AssetResource;
import org.openremote.manager.shared.map.MapResource;
import org.openremote.manager.shared.security.Tenant;
import org.openremote.manager.shared.validation.ConstraintViolation;
import org.openremote.model.ValueHolder;
import org.openremote.model.asset.*;
import org.openremote.model.asset.agent.AgentLink;
import org.openremote.model.asset.agent.ProtocolConfiguration;
import org.openremote.model.asset.agent.ProtocolDescriptor;
import org.openremote.model.attribute.AttributeType;
import org.openremote.model.attribute.AttributeValidationResult;
import org.openremote.model.attribute.MetaItem;
import org.openremote.model.attribute.MetaItemDescriptor;
import org.openremote.model.util.EnumUtil;
import org.openremote.model.util.Pair;
import org.openremote.model.value.ValueType;

import javax.inject.Inject;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static org.openremote.manager.client.http.RequestExceptionHandler.handleRequestException;
import static org.openremote.manager.client.widget.ValueEditors.createAttributeRefEditor;
import static org.openremote.model.attribute.Attribute.ATTRIBUTE_NAME_VALIDATOR;
import static org.openremote.model.attribute.Attribute.isAttributeNameEqualTo;
import static org.openremote.model.util.TextUtil.isNullOrEmpty;

public class AssetEditActivity
    extends AbstractAssetActivity<AssetEdit.Presenter, AssetEdit, AssetEditPlace>
    implements AssetEdit.Presenter {

    protected final AssetResource assetResource;
    protected final AgentResource agentResource;
    protected final AssetMapper assetMapper;
    protected final AssetArrayMapper assetArrayMapper;
    protected final AssetQueryMapper assetQueryMapper;
    protected final ProtocolDescriptorArrayMapper protocolDescriptorArrayMapper;
    protected final ProtocolDescriptorMapMapper protocolDescriptorMapMapper;
    protected final AttributeValidationResultMapper attributeValidationResultMapper;
    protected final AssetAttributeMapper assetAttributeMapper;
    protected final Consumer<ConstraintViolation[]> validationErrorHandler;
    protected final List<Consumer<Asset[]>> agentAssetConsumers = new ArrayList<>();
    protected final List<Consumer<Asset[]>> allAssetConsumers = new ArrayList<>();
    protected Asset[] agentAssets;
    protected Asset[] allAssets;
    protected List<ProtocolDescriptor> protocolDescriptors = new ArrayList<>();
    protected List<MetaItemDescriptor> metaItemDescriptors = new ArrayList<>(Arrays.asList(AssetMeta.values()));
    double[] selectedCoordinates;

    @Inject
    public AssetEditActivity(Environment environment,
                             Tenant currentTenant,
                             AssetBrowser.Presenter assetBrowserPresenter,
                             Provider<JsonEditor> jsonEditorProvider,
                             AssetEdit view,
                             AssetResource assetResource,
                             AgentResource agentResource,
                             AssetMapper assetMapper,
                             AssetArrayMapper assetArrayMapper,
                             AssetQueryMapper assetQueryMapper,
                             ProtocolDescriptorArrayMapper protocolDescriptorArrayMapper,
                             ProtocolDescriptorMapMapper protocolDescriptorMapMapper,
                             AttributeValidationResultMapper attributeValidationResultMapper,
                             AssetAttributeMapper assetAttributeMapper,
                             MapResource mapResource,
                             ObjectValueMapper objectValueMapper) {
        super(environment, currentTenant, assetBrowserPresenter, jsonEditorProvider, objectValueMapper, mapResource, true);
        this.presenter = this;
        this.view = view;
        this.assetResource = assetResource;
        this.agentResource = agentResource;
        this.assetMapper = assetMapper;
        this.assetArrayMapper = assetArrayMapper;
        this.assetQueryMapper = assetQueryMapper;
        this.protocolDescriptorArrayMapper = protocolDescriptorArrayMapper;
        this.protocolDescriptorMapMapper = protocolDescriptorMapMapper;
        this.attributeValidationResultMapper = attributeValidationResultMapper;
        this.assetAttributeMapper = assetAttributeMapper;

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
    public void onStop() {
        super.onStop();
        clearViewMessages();
        view.setPresenter(null);
    }

    @Override
    public void start() {
        if (isNullOrEmpty(assetId)) {
            assetBrowserPresenter.clearSelection();
            asset = new Asset();
            asset.setName("My New Asset");
            asset.setRealmId(currentTenant.getId());
            asset.setTenantDisplayName(currentTenant.getDisplayName());
            asset.setType("urn:mydomain:customtype");
        }

        clearViewMessages();
        writeAssetToView();
        writeAttributeTypesToView(() -> {
            writeAttributesToView();
            loadParent();
        });
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
        writeAttributeTypesToView(this::writeAttributesToView);
    }

    @Override
    public boolean addAttribute(String name, String type) {
        if (isNullOrEmpty(name) || isNullOrEmpty(type)) {
            return false;
        }

        if (asset.getAttributesStream().anyMatch(isAttributeNameEqualTo(name))) {
            showFailureMessage(environment.getMessages().duplicateAttributeName());
            return false;
        }

        if (!ATTRIBUTE_NAME_VALIDATOR.test(name)) {
            showFailureMessage(environment.getMessages().invalidAttributeName());
            return false;
        }
        AssetAttribute attribute;

        Optional<ProtocolDescriptor> protocolDescriptor = protocolDescriptors == null ?
            Optional.empty() :
            protocolDescriptors.stream()
                .filter(pd -> pd.getName().equals(type))
                .findFirst();

        if (protocolDescriptor.isPresent()) {
            // This is a protocol configuration add request
            attribute = protocolDescriptor.get().getConfigurationTemplate().deepCopy();
        } else {
            AttributeType attributeType = EnumUtil.enumFromString(AttributeType.class, type).orElse(null);

            if (attributeType == null) {
                showFailureMessage(environment.getMessages().invalidAttributeType());
                return false;
            }

            attribute = new AssetAttribute();
            attribute.setType(attributeType);
            attribute.addMeta(attributeType.getDefaultMetaItems());
        }

        attribute.setName(name);

        // Tell the server to set the timestamp when saving because we don't want to use browser time
        attribute.setValueTimestamp(0);

        asset.getAttributesList().add(attribute);
        writeAttributeToView(attribute, true);
        return true;
    }

    @Override
    public void removeAttribute(AssetAttribute attribute) {
        // Allow deleting any attributes for now
        asset.getAttributesList().remove(attribute);
        view.getAttributeViews()
            .stream()
            .filter(attributeView -> attributeView.getAttribute() == attribute)
            .findFirst()
            .ifPresent(attributeView -> view.removeAttributeViews(Collections.singletonList(attributeView)));
    }

    @Override
    public void update() {
        view.setFormBusy(true);
        clearViewMessages();
        validateAttributes(false, results -> {
            if (results.stream().anyMatch(result -> !result.isValid())) {
                view.setFormBusy(false);
                processValidationResults(results);
            } else {
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
                    ex -> {
                        view.setFormBusy(false);
                        handleRequestException(ex, environment.getEventBus(), environment.getMessages(), validationErrorHandler);
                    }
                );
            }
        });
    }

    @Override
    public void create() {
        view.setFormBusy(true);
        clearViewMessages();
        validateAttributes(false, results -> {
            if (results.stream().anyMatch(result -> !result.isValid())) {
                view.setFormBusy(false);
                processValidationResults(results);
            } else {
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
        });
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

            // Retrieve agents in the same realm as the asset (if it has been assigned a realm otherwise
            // the query will be automatically restricted to the logged in users realm)
            if (!isNullOrEmpty(asset.getRealmId())) {
                query.tenant(new AbstractAssetQuery.TenantPredicate(asset.getRealmId()));
            }
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
    protected IsWidget createValueEditor(ValueHolder valueHolder, ValueType valueType, AttributeView.Style style, Runnable onValueModified) {
        switch(valueType) {
            case ARRAY:
                if (valueHolder instanceof MetaItem) {
                    Optional<AssetMeta> assetMeta = AssetMeta.getAssetMeta(((MetaItem) valueHolder).getName().orElse(null));

                    if (assetMeta.map(am -> am == AssetMeta.AGENT_LINK).orElse(false)) {
                        boolean isReadOnly = isValueReadOnly(valueHolder);
                        String assetWatermark = environment.getMessages().selectAgent();
                        String attributeWatermark = environment.getMessages().selectProtocolConfiguration();
                        return createAttributeRefEditor(valueHolder, onValueModified, isReadOnly, this::getLinkableAssets, this::getLinkableAttributes, assetWatermark, attributeWatermark, style.agentLinkEditor());
                    }
                }
        }
        return super.createValueEditor(valueHolder, valueType, style, onValueModified);
    }

    @Override
    public void centerMap() {
        if (selectedCoordinates != null) {
            view.flyTo(selectedCoordinates);
        } else if (asset.getCoordinates() != null) {
            view.flyTo(asset.getCoordinates());
        }
    }

    @Override
    public void writeAssetToView() {
        super.writeAssetToView();
        view.enableCreate(assetId == null);
        view.enableUpdate(assetId != null);
        view.enableDelete(assetId != null);
    }

    @Override
    protected List<AbstractAttributeViewExtension> createAttributeExtensions(AssetAttribute attribute, AttributeViewImpl view) {
        List<AbstractAttributeViewExtension> extensions = new ArrayList<>();

        // if this is a protocol configuration then add a protocol link editor first
        if (ProtocolConfiguration.isProtocolConfiguration(attribute)) {
            protocolDescriptors
                .stream()
                .filter(protocolDescriptor -> protocolDescriptor.getName().equals(attribute.getValueAsString().orElse("")))
                .findFirst()
                .ifPresent(
                    protocolDescriptor -> {
                        extensions.add(
                            new ProtocolLinksEditor(environment, this.view.getStyle(), view, attribute, protocolDescriptor, false)
                        );

                        if (protocolDescriptor.isDeviceDiscovery() || protocolDescriptor.isDeviceImport()) {
                            extensions.add(
                                new ProtocolLinksEditor(environment, this.view.getStyle(), view, attribute, protocolDescriptor, true)
                            );
                        }
                    }
                );
        }

        extensions.add(new MetaEditor(environment, this.view.getStyle(), view, attribute, () -> protocolDescriptors));
        return extensions;
    }

    @Override
    protected List<FormButton> createAttributeActions(AssetAttribute attribute, AttributeViewImpl view) {
        FormButton deleteButton = new FormButton();
        deleteButton.setText(environment.getMessages().deleteAttribute());
        deleteButton.setIcon("remove");
        deleteButton.addClickHandler(clickEvent -> {
            removeAttribute(attribute);
            attribute.getName()
                .ifPresent(name ->
                    showInfo(environment.getMessages().attributeDeleted(name))
                );
        });

        return Collections.singletonList(deleteButton);
    }

    // TODO: Create a richer client side validation mechanism
    @Override
    protected void onAttributeModified(AssetAttribute attribute) {
        // Called when a view has modified the attribute so we need to do validation this is called a lot by value
        // editors (every key stroke) so use basic client side validation - use full validation before submitting
        // the asset to the server
        validateAttribute(true, attribute, result -> processValidationResults(Collections.singletonList(result)));
    }

    @Override
    protected void validateAttribute(boolean clientSideOnly, AssetAttribute attribute, Consumer<AttributeValidationResult> resultConsumer) {
        super.validateAttribute(clientSideOnly, attribute, validationResult -> {
            if (validationResult.isValid() && attribute.hasMetaItems()) {
                // Do additional validation on the meta items

                for (int i=0; i<attribute.getMeta().size(); i++) {
                    MetaItem metaItem = attribute.getMeta().get(i);
                    int finalI = i;
                    metaItemDescriptors.stream()
                        .filter(metaItemDescriptor -> metaItemDescriptor.getUrn().equals(metaItem.getName().orElse("")))
                        .findFirst()
                        .flatMap(metaItemDescriptor ->
                            MetaItemDescriptor.validateValue(metaItem.getValue().orElse(null), metaItemDescriptor)
                        )
                        .ifPresent(failure -> validationResult.addMetaFailure(finalI, failure));
                }
            }

            if (!clientSideOnly && validationResult.isValid() && ProtocolConfiguration.isProtocolConfiguration(attribute)) {
                // Ask the server to validate the protocol configuration
                environment.getRequestService().execute(
                    attributeValidationResultMapper,
                    assetAttributeMapper,
                    requestParams -> agentResource.validateProtocolConfiguration(requestParams, assetId, attribute),
                    Collections.singletonList(200),
                    resultConsumer,
                    ex -> handleRequestException(ex, environment.getEventBus(), environment.getMessages(), validationErrorHandler)
                );
            } else {
                resultConsumer.accept(validationResult);
            }
        });

    }

    protected void writeAttributeTypesToView(Runnable onComplete) {
        view.selectWellKnownType(asset.getWellKnownType());
        view.setAvailableWellKnownTypes(AssetType.valuesSorted());
        view.setType(asset.getType());
        view.setTypeEditable(isNullOrEmpty(assetId));

        // Populate add attributes drop down based on asset type
        if (asset.getWellKnownType() == AssetType.AGENT && !isNullOrEmpty(asset.getId())) {
            List<Pair<String, String>> displayNamesAndTypes = new ArrayList<>();
            displayNamesAndTypes.add(new Pair<>(ValueEditors.EMPTY_LINE, null));

            environment.getRequestService().execute(
                protocolDescriptorArrayMapper,
                requestParams -> agentResource.getSupportedProtocols(requestParams, assetId),
                200,
                protocolDescriptors -> {
                    this.protocolDescriptors.addAll(Arrays.asList(protocolDescriptors));
                    updateMetaItemDescriptors();
                    view.setFormBusy(false);
                    Arrays.stream(protocolDescriptors)
                        .sorted(Comparator.comparing(ProtocolDescriptor::getDisplayName))
                        .forEach(protocolDescriptor -> displayNamesAndTypes
                            .add(new Pair<>(protocolDescriptor.getDisplayName(), protocolDescriptor.getName()))
                        );

                    displayNamesAndTypes.add(new Pair<>(ValueEditors.EMPTY_LINE, null));
                    displayNamesAndTypes.addAll(attributeTypesToList());
                    view.setAvailableAttributeTypes(displayNamesAndTypes);
                    onComplete.run();
                },
                ex -> handleRequestException(ex, environment.getEventBus(), environment.getMessages(), validationErrorHandler)
            );
        } else {
            // Get all protocol descriptors for all agents
            environment.getRequestService().execute(
                protocolDescriptorMapMapper,
                agentResource::getAllSupportedProtocols,
                200,
                protocolDescriptorMap -> {
                    protocolDescriptorMap.forEach((id, descriptors) -> {
                        for (ProtocolDescriptor newDescriptor : descriptors) {
                            if (this.protocolDescriptors.stream().noneMatch(pd -> pd.getName().equals(newDescriptor.getName()))) {
                                this.protocolDescriptors.add(newDescriptor);
                            }
                        }
                    });
                    updateMetaItemDescriptors();
                    view.setFormBusy(false);
                    view.setAvailableAttributeTypes(attributeTypesToList());
                    onComplete.run();
                },
                ex -> handleRequestException(ex, environment.getEventBus(), environment.getMessages(), validationErrorHandler)
            );
        }
    }

    protected void updateMetaItemDescriptors() {
        if (protocolDescriptors != null) {
            for (ProtocolDescriptor descriptor : protocolDescriptors) {
                if (descriptor.getProtocolConfigurationMetaItems() != null) {
                    metaItemDescriptors.addAll(descriptor.getProtocolConfigurationMetaItems());
                }
                if (descriptor.getLinkedAttributeMetaItems() != null) {
                    descriptor.getLinkedAttributeMetaItems().forEach(newDescriptor -> {
                        if (metaItemDescriptors.stream().noneMatch(md -> md.getUrn().equals(newDescriptor.getUrn()))) {
                            metaItemDescriptors.add(newDescriptor);
                        }
                    });
                }
            }
        }
    }

    protected List<Pair<String, String>> attributeTypesToList() {
        return Arrays.stream(AttributeType.values())
            .map(Enum::name)
            .map(attrType -> new Pair<>(environment.getMessages().attributeType(attrType), attrType))
            .sorted(Comparator.comparing(a -> a.key))
            .collect(Collectors.toList());
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
