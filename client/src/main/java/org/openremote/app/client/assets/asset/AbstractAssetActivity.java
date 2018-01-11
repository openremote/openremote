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
import org.openremote.app.client.assets.AssetBrowsingActivity;
import org.openremote.app.client.assets.attributes.AbstractAttributeViewExtension;
import org.openremote.app.client.assets.attributes.AttributeView;
import org.openremote.app.client.assets.attributes.AttributeViewImpl;
import org.openremote.app.client.assets.browser.AssetBrowser;
import org.openremote.app.client.assets.browser.AssetBrowserSelection;
import org.openremote.app.client.assets.browser.AssetTreeNode;
import org.openremote.app.client.assets.browser.TenantTreeNode;
import org.openremote.app.client.assets.navigation.AssetNavigation;
import org.openremote.app.client.assets.tenant.AssetsTenantPlace;
import org.openremote.app.client.event.GoToPlaceEvent;
import org.openremote.app.client.event.ShowFailureEvent;
import org.openremote.app.client.event.ShowInfoEvent;
import org.openremote.app.client.event.ShowSuccessEvent;
import org.openremote.app.client.interop.value.ObjectValueMapper;
import org.openremote.app.client.mvp.AcceptsView;
import org.openremote.app.client.mvp.AppActivity;
import org.openremote.app.client.widget.FormButton;
import org.openremote.app.client.widget.FormOutputText;
import org.openremote.model.ValidationFailure;
import org.openremote.model.ValueHolder;
import org.openremote.model.asset.Asset;
import org.openremote.model.asset.AssetAttribute;
import org.openremote.model.asset.AssetMeta;
import org.openremote.model.asset.agent.ProtocolConfiguration;
import org.openremote.model.attribute.*;
import org.openremote.model.event.bus.EventBus;
import org.openremote.model.event.bus.EventRegistration;
import org.openremote.model.interop.Consumer;
import org.openremote.model.map.MapResource;
import org.openremote.model.security.Tenant;
import org.openremote.model.value.*;

import java.util.*;

import static org.openremote.app.client.widget.ValueEditors.*;
import static org.openremote.model.util.TextUtil.isNullOrEmpty;

public abstract class AbstractAssetActivity<V
    extends AssetBaseView.Presenter, U extends AssetBaseView<V>, PLACE extends AssetPlace> extends AssetBrowsingActivity<PLACE>
    implements AssetBaseView.Presenter, AssetNavigation.Presenter {

    protected final List<AttributeView> attributeViews = new ArrayList<>();
    protected final Provider<JsonEditor> jsonEditorProvider;
    protected final boolean editMode;
    protected final ObjectValueMapper objectValueMapper;
    protected final MapResource mapResource;
    protected AssetPlace activePlace;
    protected String assetId;
    protected Asset asset;
    protected Asset parentAsset;
    protected Collection<EventRegistration> registrations;
    protected U view;
    protected V presenter;

    public AbstractAssetActivity(Environment environment,
                                 AssetBrowser.Presenter assetBrowserPresenter,
                                 Provider<JsonEditor> jsonEditorProvider,
                                 ObjectValueMapper objectValueMapper,
                                 MapResource mapResource,
                                 boolean editMode) {
        super(environment, assetBrowserPresenter);
        this.objectValueMapper = objectValueMapper;
        this.mapResource = mapResource;
        this.editMode = editMode;
        this.jsonEditorProvider = jsonEditorProvider;
    }

    @Override
    protected AppActivity<PLACE> init(PLACE place) {
        this.activePlace = place;
        this.assetId = place.getAssetId();
        assetBrowserPresenter.setCreateAsset(place instanceof AssetEditPlace && this.assetId == null);
        return this;
    }

    @Override
    public void start(AcceptsView container, EventBus eventBus, Collection<EventRegistration> registrations) {
        this.registrations = registrations;
        view.setPresenter(presenter);
        view.getAssetNavigation().setPresenter(this);
        container.setWidget(view.asWidget());

        registrations.add(eventBus.register(
            AssetBrowserSelection.class, event -> {
                if (event.getSelectedNode() instanceof TenantTreeNode) {
                    environment.getPlaceController().goTo(
                        new AssetsTenantPlace(event.getSelectedNode().getId())
                    );
                } else if (event.getSelectedNode() instanceof AssetTreeNode) {
                    String selectedAssetId = event.getSelectedNode().getId();
                    if (this.assetId == null || !this.assetId.equals(selectedAssetId)) {
                        if (!editMode || assetId == null) {
                            environment.getPlaceController().goTo(new AssetViewPlace(selectedAssetId));
                        } else {
                            environment.getPlaceController().goTo(new AssetEditPlace(selectedAssetId));
                        }
                    }
                }
            }
        ));

        registrations.add(eventBus.register(
            GoToPlaceEvent.class,
            event -> {
                assetBrowserPresenter.setCreateAsset(false);
                if (event.getPlace() instanceof AssetEditPlace) {
                    AssetEditPlace place = (AssetEditPlace) event.getPlace();
                    if (place.getAssetId() == null) {
                        assetBrowserPresenter.setCreateAsset(true);
                    }
                }
            }
        ));

        if (!view.isMapInitialised()) {
            environment.getApp().getRequestService().sendAndReturn(
                objectValueMapper,
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
        view.getAssetNavigation().setPresenter(null);
    }

    @Override
    public void onMapReady() {
        asset = null;
        if (!isNullOrEmpty(assetId)) {
            assetBrowserPresenter.loadAsset(assetId, loadedAsset -> {
                this.asset = loadedAsset;
                assetBrowserPresenter.selectAsset(asset);
                start();
            });
        } else {
            start();
        }
    }

    @Override
    public void loadParent() {
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
    }

    @Override
    public void writeParentToView() {
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

    @Override
    public void writeAssetToView() {
        view.getAssetNavigation().setEnabled(assetId != null);
        view.setName(asset.getName());
        view.setCreatedOn(asset.getCreatedOn());
        view.setLocation(asset.getCoordinates());
        view.showDroppedPin(asset.getGeoFeature(20));
        view.flyTo(asset.getCoordinates());
        view.setAccessPublicRead(asset.isAccessPublicRead());
    }

    @Override
    public void writeAttributesToView() {
        if (asset != null && asset.getAttributesList().size() > 0) {
            for (AssetAttribute attribute : asset.getAttributesList()) {
                writeAttributeToView(attribute, false);
            }
        }

        view.setAttributeViews(attributeViews);
        validateAttributes(true, this::processValidationResults);
    }


    @Override
    public String getAssetViewPlaceToken() {
        return environment.getPlaceHistoryMapper().getToken(new AssetViewPlace(assetId));
    }

    @Override
    public String getAssetEditPlaceToken() {
        return environment.getPlaceHistoryMapper().getToken(new AssetEditPlace(assetId));
    }

    @Override
    public AssetPlace getActivePlace() {
        return activePlace;
    }

    protected void writeAttributeToView(AssetAttribute attribute, boolean addToView) {
        AttributeView attributeView = createAttributeView(attribute);
        attributeViews.add(attributeView);

        if (addToView) {
            view.addAttributeViews(Collections.singletonList(attributeView));
            validateAttribute(true, attribute, result -> processValidationResults(Collections.singletonList(result)));
        }
    }

    protected AttributeView createAttributeView(AssetAttribute attribute) {

        AttributeViewImpl attributeView = new AttributeViewImpl(environment,
            view.getStyle(),
            attribute);

        attributeView.setAttributeActions(createAttributeActions(attribute, attributeView));

        List<AbstractAttributeViewExtension> extensions = createAttributeExtensions(attribute, attributeView);
        if (extensions != null && !extensions.isEmpty()) {
            extensions.forEach(this::linkAttributeView);
        }

        attributeView.setAttributeExtensions(extensions);
        linkAttributeView(attributeView);
        return attributeView;
    }

    abstract protected List<AbstractAttributeViewExtension> createAttributeExtensions(AssetAttribute attribute, AttributeViewImpl view);

    abstract protected List<FormButton> createAttributeActions(AssetAttribute attribute, AttributeViewImpl view);

    protected void linkAttributeView(AttributeView attributeView) {
        attributeView.setValueEditorSupplier(this::createValueEditor);
        attributeView.setEditMode(editMode);
        attributeView.setAttributeModifiedCallback(this::onAttributeModified);
        attributeView.setValidationErrorConsumer(this::showValidationError);
    }

    /**
     * Attribute view action button is requesting the value be written to the server
     */
    protected void writeAttributeValue(AssetAttribute attribute) {

        getAttributeView(attribute)
            .ifPresent(
                attributeView -> {

                    attributeView.setBusy(true);
                    validateAttribute(false, attribute, result -> {

                        // Notify the view of the result
                        attributeView.onValidationStateChange(result);
                        attributeView.setBusy(false);

                        if (result.isValid()) {
                            attribute
                                .getReference()
                                .map(attributeRef -> {
                                    // Clear timestamp and let the server set it
                                    attribute.setValueTimestamp(0);
                                    return new AttributeState(attributeRef, attribute.getValue().orElse(null));
                                })
                                .map(attributeState -> new AttributeEvent(attributeState, attribute.getValueTimestamp().orElse(0L)))
                                .ifPresent(attributeEvent -> {
                                    environment.getEventService().dispatch(attributeEvent);
                                    if (attribute.isExecutable()) {
                                        showSuccess(environment
                                            .getMessages()
                                            .commandRequestSent(attribute.getLabelOrName().orElse("")));
                                    } else {
                                        showSuccess(
                                            environment
                                                .getMessages()
                                                .attributeWriteSent(attribute.getLabelOrName().orElse("")));
                                    }
                                });
                        }
                    });
                }
            );

    }

    protected void validateAttributes(boolean clientSideOnly, Consumer<List<AttributeValidationResult>> resultsConsumer) {
        if (asset.getAttributesList().isEmpty()) {
            resultsConsumer.accept(Collections.emptyList());
            return;
        }

        List<AttributeValidationResult> results = new ArrayList<>(asset.getAttributesList().size());
        Consumer<AttributeValidationResult> resultConsumer = attributeValidationResult -> {
            results.add(attributeValidationResult);
            if (results.size() == asset.getAttributesList().size()) {
                resultsConsumer.accept(results);
            }
        };

        asset.getAttributesList().forEach(attribute -> validateAttribute(clientSideOnly, attribute, resultConsumer));
    }

    protected void validateAttribute(boolean clientSideOny, AssetAttribute attribute, Consumer<AttributeValidationResult> resultConsumer) {
        List<ValidationFailure> attributeFailures = attribute.getValidationFailures(false);
        Map<Integer, List<ValidationFailure>> metaFailures = new HashMap<>(attribute.getMeta().size());
        String attributeName = attribute.getName().orElseThrow(() -> new IllegalStateException("Attribute name cannot be null"));

        for (int i=0; i<attribute.getMeta().size(); i++) {
            MetaItem item = attribute.getMeta().get(i);
            List<ValidationFailure> metaItemFailures = attribute.getMetaItemValidationFailures(item, getMetaItemDescriptor(item));

            if (!metaItemFailures.isEmpty()) {
                metaFailures.put(i, metaItemFailures);
            }
        }

        resultConsumer.accept(new AttributeValidationResult(attributeName, attributeFailures, metaFailures));
    }

    protected Optional<MetaItemDescriptor> getMetaItemDescriptor(MetaItem item) {
        // TODO Should use meta item descriptors from server
        return Arrays.stream(AssetMeta.values())
            .filter(assetMeta -> assetMeta.getUrn().equals(item.getName().orElse("")))
            .findFirst()
            .map(assetMeta -> (MetaItemDescriptor)assetMeta);
    }

    protected void processValidationResults(List<AttributeValidationResult> results) {
        // Update each of the attribute views (the views are responsible for displaying any failure messages)
        view.setFormBusy(false);

        results.forEach(result -> view.getAttributeViews()
            .stream()
            .filter(attrView -> attrView
                    .getAttribute()
                    .getName()
                    .map(name -> name.equals(result.getAttributeName())).orElse(false))
            .findFirst()
            .ifPresent(attrView -> attrView.onValidationStateChange(result)));
    }

    protected Optional<AttributeView> getAttributeView(AssetAttribute attribute) {
        return attributeViews
            .stream()
            .filter(attributeView -> attributeView.getAttribute() == attribute)
            .findFirst();
    }

    protected void showInfo(String text) {
        environment.getEventBus().dispatch(new ShowInfoEvent(text));
    }

    protected void showSuccess(String text) {
        environment.getEventBus().dispatch(new ShowSuccessEvent(text));
    }

    protected boolean isValueReadOnly(ValueHolder valueHolder) {

        // Meta item value is read only if value is fixed
        if (valueHolder instanceof MetaItem) {
            // TODO Should use meta item descriptors from server
            return Arrays.stream(AssetMeta.values())
                .filter(assetMeta -> assetMeta.getUrn().equals(((MetaItem) valueHolder).getName()))
                .map(AssetMeta::isValueFixed)
                .findFirst().orElse(false);
        }

        if (valueHolder instanceof AssetAttribute) {
            // Attribute Value is read only if it is readonly or a protocol configuration
            AssetAttribute attribute = (AssetAttribute) valueHolder;
            return attribute.isReadOnly() || ProtocolConfiguration.isProtocolConfiguration(attribute);
        }

        return false;
    }

    protected Optional<Long> getTimestamp(ValueHolder valueHolder) {
        return editMode || !(valueHolder instanceof AssetAttribute) ?
            Optional.empty() :
            ((AssetAttribute)valueHolder).getValueTimestamp();
    }

    abstract protected void onAttributeModified(AssetAttribute attribute);

    /**
     * Creates editors for {@link ValueHolder}s.
     * <p>
     * Custom {@link MetaItem}s don't contain any value type information so we need this provided based on user's
     * type selection.
     */
    protected IsWidget createValueEditor(ValueHolder valueHolder, ValueType valueType, AttributeView.Style style, AttributeView parentView, Consumer<Value> onValueModified) {

        // TODO: Implement support for setting access permissions on individual meta item instances
//            // Super users can edit any meta items but other users can only edit non-restricted meta items
//            // Individual instances of meta items can be set to restricted by a super user.
//            boolean isEditable = environment.getSecurityService().isSuperUser() ||
//                (item.hasRestrictedFlag() && !item.isRestricted()) ||
//                assetMeta.map(AssetMeta::isRestricted).orElse(true);

        boolean isReadOnly = isValueReadOnly(valueHolder);
        Optional<Long> timestamp = getTimestamp(valueHolder);

        switch(valueType) {
            case OBJECT:
                ObjectValue currentValueObj = valueHolder.getValueAsObject().orElse(null);
                String label = environment.getMessages().jsonObject();
                String title = environment.getMessages().edit() + " " + environment.getMessages().jsonObject();
                return createObjectEditor(
                    currentValueObj,
                    onValueModified::accept,
                    timestamp,
                    isReadOnly,
                    label,
                    title,
                    jsonEditorProvider.get()
                );
            case ARRAY:
                ArrayValue currentValueArray = valueHolder.getValueAsArray().orElse(null);
                label = environment.getMessages().jsonArray();
                title = environment.getMessages().edit() + " " + environment.getMessages().jsonArray();
                return createArrayEditor(
                    currentValueArray,
                    onValueModified::accept,
                    timestamp,
                    isReadOnly,
                    label,
                    title,
                    jsonEditorProvider.get()
                );
            case STRING:
                StringValue currentValueStr = (StringValue)valueHolder.getValue().orElse(null);
                return createStringEditor(
                    currentValueStr,
                    onValueModified::accept,
                    timestamp,
                    isReadOnly,
                    style.stringEditor()
                );
            case NUMBER:
                NumberValue currentValueNumber = (NumberValue)valueHolder.getValue().orElse(null);
                return createNumberEditor(
                    currentValueNumber,
                    onValueModified::accept,
                    timestamp,
                    isReadOnly,
                    style.numberEditor()
                );
            case BOOLEAN:
                BooleanValue currentValueBool = (BooleanValue)valueHolder.getValue().orElse(null);
                return createBooleanEditor(
                    currentValueBool,
                    onValueModified::accept,
                    timestamp,
                    isReadOnly,
                    style.booleanEditor()
                );
            default:
                return new FormOutputText(valueHolder instanceof MetaItem ?
                    environment.getMessages().unsupportedMetaItemType(valueType.name()) :
                    environment.getMessages().unsupportedValueType(valueType.name())
                );
        }
    }

    public void showValidationError(String attributeName, String metaItemName, ValidationFailure validationFailure) {
        StringBuilder error = new StringBuilder();

        if (!isNullOrEmpty(metaItemName)) {
            error.append(
                environment.getMessages().validationFailureOnMetaItem(
                    attributeName,
                    metaItemName
                )
            );
        } else {
            error.append(environment.getMessages().validationFailureOnAttribute(
                attributeName
            ));
        }

        error.append(": ");

        String parameterStr = validationFailure.getParameter()
            .map(parameter -> {
                String str = environment.getMessages().validationFailureParameter(parameter);
                return isNullOrEmpty(str) ? parameter : str;
            })
            .orElse("Value");

        error.append(
            environment.getMessages().validationFailure(parameterStr, validationFailure.getReason().name())
        );

        showFailureMessage(error.toString());
    }

    public void showFailureMessage(String failureMessage) {
        environment.getEventBus().dispatch(new ShowFailureEvent(failureMessage, 5000));
    }
}
