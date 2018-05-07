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
package org.openremote.app.client.assets.asset;

import com.google.gwt.user.client.ui.IsWidget;
import com.google.inject.Provider;
import org.openremote.app.client.Environment;
import org.openremote.app.client.app.dialog.JsonEditor;
import org.openremote.app.client.assets.*;
import org.openremote.app.client.assets.attributes.*;
import org.openremote.app.client.assets.browser.AssetBrowser;
import org.openremote.app.client.assets.browser.AssetTreeNode;
import org.openremote.app.client.assets.browser.BrowserTreeNode;
import org.openremote.app.client.assets.browser.TenantTreeNode;
import org.openremote.app.client.event.ShowFailureEvent;
import org.openremote.app.client.event.ShowSuccessEvent;
import org.openremote.app.client.interop.jackson.FileInfoMapper;
import org.openremote.app.client.interop.value.ObjectValueMapper;
import org.openremote.app.client.widget.AttributeLinkEditor;
import org.openremote.app.client.widget.AttributeRefEditor;
import org.openremote.app.client.widget.FormButton;
import org.openremote.app.client.widget.ValueEditors;
import org.openremote.model.ValueHolder;
import org.openremote.model.asset.*;
import org.openremote.model.asset.agent.AgentLink;
import org.openremote.model.asset.agent.AgentResource;
import org.openremote.model.asset.agent.ProtocolConfiguration;
import org.openremote.model.asset.agent.ProtocolDescriptor;
import org.openremote.model.attribute.*;
import org.openremote.model.http.ConstraintViolation;
import org.openremote.model.interop.Consumer;
import org.openremote.model.map.MapResource;
import org.openremote.model.util.EnumUtil;
import org.openremote.model.util.Pair;
import org.openremote.model.value.Value;
import org.openremote.model.value.ValueType;

import javax.inject.Inject;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static org.openremote.model.asset.AssetAttribute.attributesFromJson;
import static org.openremote.model.asset.AssetQuery.*;
import static org.openremote.model.attribute.Attribute.ATTRIBUTE_NAME_VALIDATOR;
import static org.openremote.model.attribute.Attribute.isAttributeNameEqualTo;
import static org.openremote.model.attribute.MetaItem.isMetaNameEqualTo;
import static org.openremote.model.util.TextUtil.isNullOrEmpty;

public class AssetEditActivity
    extends AbstractAssetActivity<AssetEdit.Presenter, AssetEdit, AssetEditPlace>
    implements AssetEdit.Presenter {

    protected final AssetBrowser assetBrowser;
    protected final AssetResource assetResource;
    protected final AgentResource agentResource;
    protected final AssetMapper assetMapper;
    protected final AssetArrayMapper assetArrayMapper;
    protected final AssetQueryMapper assetQueryMapper;
    protected final ProtocolDescriptorArrayMapper protocolDescriptorArrayMapper;
    protected final ProtocolDescriptorMapMapper protocolDescriptorMapMapper;
    protected final FileInfoMapper fileInfoMapper;
    protected final AttributeValidationResultMapper attributeValidationResultMapper;
    protected final AssetAttributeMapper assetAttributeMapper;
    protected final Consumer<ConstraintViolation[]> validationErrorHandler;
    protected List<ProtocolDescriptor> protocolDescriptors = new ArrayList<>();
    protected List<MetaItemDescriptor> metaItemDescriptors = new ArrayList<>(Arrays.asList(AssetMeta.values())); // TODO Get meta item descriptors from server
    double[] selectedCoordinates;
    protected List<AssetAttribute> initialAssetAttributes;

    @Inject
    public AssetEditActivity(Environment environment,
                             AssetBrowser.Presenter assetBrowserPresenter,
                             Provider<JsonEditor> jsonEditorProvider,
                             AssetEdit view,
                             AssetBrowser assetBrowser,
                             AssetResource assetResource,
                             AgentResource agentResource,
                             AssetMapper assetMapper,
                             AssetArrayMapper assetArrayMapper,
                             AssetQueryMapper assetQueryMapper,
                             ProtocolDescriptorArrayMapper protocolDescriptorArrayMapper,
                             ProtocolDescriptorMapMapper protocolDescriptorMapMapper,
                             AttributeValidationResultMapper attributeValidationResultMapper,
                             AssetAttributeMapper assetAttributeMapper,
                             FileInfoMapper fileInfoMapper,
                             MapResource mapResource,
                             ObjectValueMapper objectValueMapper) {
        super(environment, assetBrowserPresenter, jsonEditorProvider, objectValueMapper, mapResource, true);
        this.presenter = this;
        this.view = view;
        this.assetBrowser = assetBrowser;
        this.assetResource = assetResource;
        this.agentResource = agentResource;
        this.assetMapper = assetMapper;
        this.assetArrayMapper = assetArrayMapper;
        this.assetQueryMapper = assetQueryMapper;
        this.protocolDescriptorArrayMapper = protocolDescriptorArrayMapper;
        this.protocolDescriptorMapMapper = protocolDescriptorMapMapper;
        this.attributeValidationResultMapper = attributeValidationResultMapper;
        this.assetAttributeMapper = assetAttributeMapper;
        this.fileInfoMapper = fileInfoMapper;

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
    protected String[] getRequiredRoles() {
        return new String[]{"read:assets", "write:assets"};
    }

    @Override
    public void onStop() {
        clearViewMessages();
        super.onStop();
    }

    @Override
    public void start() {
        if (isNullOrEmpty(assetId)) {
            assetBrowserPresenter.clearSelection();
            asset = new Asset();
            asset.setName("My New Asset");
            asset.setRealmId(environment.getApp().getTenant().getId());
            asset.setTenantDisplayName(environment.getApp().getTenant().getDisplayName());
            asset.setType(AssetType.THING);
        }

        initialAssetAttributes = attributesFromJson(asset.getAttributes(), assetId).collect(Collectors.toList());

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
        view.hideMapPopup();
        view.showMapPopup(lng, lat, environment.getMessages().selectedLocation());
        view.setLocation(selectedCoordinates);
    }

    @Override
    public void onAccessPublicRead(boolean enabled) {
        asset.setAccessPublicRead(enabled);
    }

    @Override
    public void onAssetTypeSelected(AssetTypeDescriptor type) {
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
                environment.getApp().getRequests().sendWith(
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
                    validationErrorHandler
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
                environment.getApp().getRequests().sendWithAndReturn(
                    assetMapper,
                    assetMapper,
                    requestParams -> assetResource.create(requestParams, asset),
                    200,
                    createdAsset -> {
                        environment.getEventBus().dispatch(new ShowSuccessEvent(
                            environment.getMessages().assetCreated(createdAsset.getName())
                        ));
                        environment.getPlaceController().goTo(new AssetViewPlace(createdAsset.getId()));
                    },
                    validationErrorHandler
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
                environment.getApp().getRequests().send(
                    requestParams -> assetResource.delete(requestParams, this.assetId),
                    204,
                    () -> {
                        view.setFormBusy(false);
                        environment.getEventBus().dispatch(new ShowSuccessEvent(
                            environment.getMessages().assetDeleted(asset.getName())
                        ));
                        environment.getPlaceController().goTo(new AssetsDashboardPlace());
                    }
                );
            }
        );
    }

    @Override
    public void getLinkableAssetsAndAttributes(ValueHolder valueHolder, Consumer<Map<AttributeRefEditor.AssetInfo, List<AttributeRefEditor.AttributeInfo>>> assetAttributeConsumer) {
        AssetQuery query;
        Predicate<AssetAttribute> attributeFilter = null;

        // Is it agent or attribute link?
        if ((valueHolder instanceof MetaItem) && AgentLink.isAgentLink((MetaItem) valueHolder)) {
            query = new AssetQuery()
                .select(new AssetQuery.Select(AssetQuery.Include.ONLY_ID_AND_NAME_AND_ATTRIBUTES))
                // Limit to agents
                .type(AssetType.AGENT);

            // Retrieve agents in the same realm as the asset (if it has been assigned a realm otherwise
            // the query will be automatically restricted to the logged in users realm)
            if (!isNullOrEmpty(asset.getRealmId())) {
                query.tenant(new TenantPredicate(asset.getRealmId()));
            }

            // Agents must have protocol configurations
            query.attributeMeta(new AttributeMetaPredicate(AssetMeta.PROTOCOL_CONFIGURATION, new BooleanPredicate(true)));

            // Only show protocol configurations
            attributeFilter = ProtocolConfiguration::isProtocolConfiguration;
        } else {
            query = new AssetQuery()
                .select(new AssetQuery.Select(AssetQuery.Include.ONLY_ID_AND_NAME_AND_ATTRIBUTE_NAMES));

            // Limit to assets that have the same realm as the asset being edited (if it has been assigned a realm
            // otherwise the query will be automatically restricted to the logged in users realm)
            if (!isNullOrEmpty(asset.getRealmId())) {
                query.tenant(new AssetQuery.TenantPredicate(asset.getRealmId()));
            }
        }

        // Do request
        final Predicate<AssetAttribute> finalAttributeFilter = attributeFilter;
        environment.getApp().getRequests().sendWithAndReturn(
            assetArrayMapper,
            assetQueryMapper,
            requestParams -> assetResource.queryAssets(requestParams, query),
            200,
            assets -> {
                Map<AttributeRefEditor.AssetInfo, List<AttributeRefEditor.AttributeInfo>> assetAttributeMap = Arrays
                    .stream(assets)
                    .filter(asset -> !asset.getAttributesList().isEmpty())
                    .collect(Collectors.toMap(
                        asset -> new AttributeRefEditor.AssetInfo(asset.getName(), asset.getId()),
                        asset ->
                            asset.getAttributesStream()
                                .filter(attribute -> finalAttributeFilter == null || finalAttributeFilter.test(attribute))
                                .map(attribute ->
                                    new AttributeRefEditor.AttributeInfo(
                                        attribute.getName().orElse(null),
                                        attribute.getLabelOrName().orElse(null)
                                    )
                                )
                                .collect(Collectors.toList())

                    ));

                assetAttributeConsumer.accept(assetAttributeMap);
            },
            exception -> assetAttributeConsumer.accept(new HashMap<>())
        );
    }

    @Override
    protected IsWidget createValueEditor(ValueHolder valueHolder, ValueType valueType, AttributeView.Style style, AttributeView parentView, Consumer<Value> onValueModified) {
        switch (valueType) {
            case ARRAY:
                if (valueHolder instanceof MetaItem) {
                    if (isMetaNameEqualTo((MetaItem) valueHolder, AssetMeta.AGENT_LINK)) {
                        boolean isReadOnly = isValueReadOnly(valueHolder);
                        String assetWatermark = environment.getMessages().selectAgent();
                        String attributeWatermark = environment.getMessages().selectProtocolConfiguration();
                        return new AttributeRefEditor(
                            valueHolder.getValue().flatMap(AttributeRef::fromValue).orElse(null),
                            attrRef -> onValueModified.accept(attrRef != null ? attrRef.toArrayValue() : null),
                            isReadOnly,
                            assetAttributeConsumer -> getLinkableAssetsAndAttributes(valueHolder, assetAttributeConsumer),
                            assetWatermark,
                            attributeWatermark,
                            style.agentLinkEditor()
                        );
                    }
                }
                break;
            case OBJECT:
                if (valueHolder instanceof MetaItem) {
                    if (isMetaNameEqualTo((MetaItem) valueHolder, AssetMeta.ATTRIBUTE_LINK)) {
                        boolean isReadOnly = isValueReadOnly(valueHolder);
                        String assetWatermark = environment.getMessages().selectAsset();
                        String attributeWatermark = environment.getMessages().selectAttribute();
                        return new AttributeLinkEditor(
                            environment,
                            style,
                            parentView,
                            this::createValueEditor,
                            this::showValidationError,
                            valueHolder.getValue().flatMap(AttributeLink::fromValue).orElse(null),
                            attrLink -> onValueModified.accept(attrLink != null ? attrLink.toObjectValue() : null),
                            isReadOnly,
                            assetAttributeConsumer -> getLinkableAssetsAndAttributes(valueHolder, assetAttributeConsumer),
                            assetWatermark,
                            attributeWatermark,
                            style.agentLinkEditor()
                        );
                    }
                }
        }
        return super.createValueEditor(valueHolder, valueType, style, parentView, onValueModified);
    }

    @Override
    public void centerMap() {
        if (selectedCoordinates != null) {
            view.flyTo(selectedCoordinates);
        }
        else if (asset.getCoordinates() != null) {
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
                        // This is too much work for now just auto import the assets
//                        extensions.add(
//                            new ProtocolLinksEditor(environment, this.view.getStyle(), view, attribute, protocolDescriptor, false)
//                        );

                        // Only add the import extension if the attribute existed when edit first started
                        boolean existingAttribute = initialAssetAttributes.stream().anyMatch(initialAttribute -> initialAttribute.getName().equals(attribute.getName()));

                        if (existingAttribute && (protocolDescriptor.isDeviceDiscovery() || protocolDescriptor.isDeviceImport())) {
                            extensions.add(
                                new ProtocolDiscoveryView(
                                    environment,
                                    this.view.getStyle(),
                                    view,
                                    attribute,
                                    assetBrowser,
                                    protocolDescriptor,
                                    this::doProtocolDiscovery
                                )
                            );
                        }
                    }
                );
        }

        extensions.add(new MetaEditor(environment, this.view.getStyle(), environment.getMessages().metaItems(), environment.getMessages().newMetaItems(), view, attribute, () -> protocolDescriptors));
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

            if (!clientSideOnly && validationResult.isValid() && ProtocolConfiguration.isProtocolConfiguration(attribute)) {
                // Ask the server to validate the protocol configuration
                environment.getApp().getRequests().sendWithAndReturn(
                    attributeValidationResultMapper,
                    assetAttributeMapper,
                    requestParams -> agentResource.validateProtocolConfiguration(requestParams, assetId, attribute),
                    200,
                    resultConsumer,
                    validationErrorHandler
                );
            } else {
                resultConsumer.accept(validationResult);
            }
        });

    }

    @Override
    protected Optional<MetaItemDescriptor> getMetaItemDescriptor(MetaItem item) {
        return metaItemDescriptors.stream()
            .filter(metaItemDescriptor -> metaItemDescriptor.getUrn().equals(item.getName().orElse("")))
            .findFirst();
    }

    protected void writeAttributeTypesToView(Runnable onComplete) {
        view.selectWellKnownType(asset.getWellKnownType());
        //TODO replace with AssetModel getValuesSorted, through a http request
        view.setAvailableWellKnownTypes(AssetType.valuesSorted());
        view.setType(asset.getType());
        view.setTypeEditable(isNullOrEmpty(assetId));

        // Populate add attributes drop down based on asset type
        if (asset.getWellKnownType() == AssetType.AGENT && !isNullOrEmpty(asset.getId())) {
            List<Pair<String, String>> displayNamesAndTypes = new ArrayList<>();
            displayNamesAndTypes.add(new Pair<>(ValueEditors.EMPTY_LINE, null));

            environment.getApp().getRequests().sendAndReturn(
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
                }
            );
        } else {
            // Get all protocol descriptors for all agents
            environment.getApp().getRequests().sendAndReturn(
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
                }
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

    protected void doProtocolDiscovery(ProtocolDiscoveryView.DiscoveryRequest request, Runnable callback) {
        showInfo(environment.getMessages().protocolLinkDiscoveryStarted());

        environment.getApp().getRequests().sendWithAndReturn(
            assetArrayMapper,
            fileInfoMapper,
            requestParams -> {
                if (request.getFileInfo() != null) {
                    agentResource.importLinkedAttributes(
                        requestParams, assetId,
                        request.getProtocolConfigurationName(),
                        request.getParentId(),
                        request.getRealmId(),
                        request.getFileInfo()
                    );
                } else {
                    agentResource.searchForLinkedAttributes(
                        requestParams, assetId,
                        request.getProtocolConfigurationName(),
                        request.getParentId(),
                        request.getRealmId()
                    );
                }
            },
            200,
            discoveredAssets -> {
                updateMetaItemDescriptors();
                view.setFormBusy(false);
                view.setAvailableAttributeTypes(attributeTypesToList());
                showSuccess(environment.getMessages().protocolLinkDiscoverySuccess(discoveredAssets.length));
                callback.run();
            }
        );
    }
}
