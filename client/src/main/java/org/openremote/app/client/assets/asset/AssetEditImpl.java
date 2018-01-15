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
package org.openremote.app.client.assets.asset;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.text.shared.AbstractRenderer;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.*;
import com.google.inject.Provider;
import org.openremote.app.client.widget.*;
import org.openremote.app.client.widget.PushButton;
import org.openremote.app.client.Environment;
import org.openremote.app.client.app.dialog.Confirmation;
import org.openremote.app.client.app.dialog.JsonEditor;
import org.openremote.app.client.assets.attributes.AttributeView;
import org.openremote.app.client.assets.browser.AssetBrowser;
import org.openremote.app.client.assets.browser.AssetSelector;
import org.openremote.app.client.assets.browser.BrowserTreeNode;
import org.openremote.app.client.assets.navigation.AssetNavigation;
import org.openremote.model.Constants;
import org.openremote.model.asset.Asset;
import org.openremote.model.asset.AssetType;
import org.openremote.model.geo.GeoJSON;
import org.openremote.model.util.Pair;
import org.openremote.model.value.ObjectValue;

import javax.inject.Inject;
import java.util.*;

public class AssetEditImpl extends FormViewImpl implements AssetEdit {

    interface UI extends UiBinder<FlexSplitPanel, AssetEditImpl> {
    }

    interface Style extends CssResource, AttributeView.Style {

        String mapWidget();

        String nameInput();

        String stringEditor();

        String numberEditor();

        String booleanEditor();

        String metaItemNameEditor();

        String metaItemValueEditor();

        String agentLinkEditor();

        String attributeView();
    }

    @UiField
    Style style;

    @UiField
    FlexSplitPanel splitPanel;

    @UiField
    HTMLPanel sidebarContainer;

    @UiField
    SimplePanel assetNavigationContainer;

    @UiField
    Headline headline;

    /* ############################################################################ */

    @UiField
    FormGroup nameGroup;
    @UiField
    TextBox nameInput;

    @UiField
    FormGroup createdOnGroup;
    @UiField
    FormOutputText createdOnOutput;
    @UiField
    FormButton showHistoryButton;

    @UiField(provided = true)
    AssetSelector parentAssetSelector;

    @UiField
    FormGroup locationGroup;
    @UiField
    FormOutputLocation locationOutput;
    @UiField
    FormButton centerMapButton;
    @UiField
    MapWidget mapWidget;

    @UiField
    FormGroup accessPublicReadGroup;
    @UiField
    FormCheckBox accessPublicReadCheckBox;

    /* ############################################################################ */

    @UiField
    FormGroup typeGroup;
    @UiField(provided = true)
    FormValueListBox<AssetType> typeListBox;
    @UiField
    FormInputText typeInput;
    @UiField
    Label customTypeInfoLabel;
    @UiField
    FlowPanel attributeViewContainer;

    /* ############################################################################ */

    @UiField
    FormGroup newAttributeFormGroup;
    @UiField
    FormInputText newAttributeNameInputText;
    @UiField
    FormListBox newAttributeTypeListBox;
    @UiField
    FormButton addAttributeButton;

    /* ############################################################################ */

    @UiField
    FormGroup submitButtonGroup;
    @UiField
    PushButton createButton;
    @UiField
    PushButton updateButton;
    @UiField
    PushButton deleteButton;

    final AssetBrowser assetBrowser;
    final AssetNavigation assetNavigation;
    final Provider<JsonEditor> jsonEditorProvider;
    final Environment environment;
    Presenter presenter;
    Asset asset;
    final List<AttributeView> attributeViews = new ArrayList<>();

    @Inject
    public AssetEditImpl(AssetBrowser assetBrowser,
                         AssetNavigation assetNavigation,
                         Provider<Confirmation> confirmationDialogProvider,
                         Provider<JsonEditor> jsonEditorProvider,
                         Environment environment) {
        super(confirmationDialogProvider, environment.getWidgetStyle());
        this.jsonEditorProvider = jsonEditorProvider;
        this.assetBrowser = assetBrowser;
        this.assetNavigation = assetNavigation;
        this.environment = environment;

        parentAssetSelector = new AssetSelector(
            assetBrowser.getPresenter(),
            environment.getMessages(),
            environment.getMessages().parentAsset(),
            environment.getMessages().selectAssetDescription(),
            false,
            treeNode -> {
                if (presenter != null) {
                    presenter.onParentSelection(treeNode);
                }
            }
        ) {
            @Override
            public void beginSelection() {
                AssetEditImpl.this.setDisabled(true);
                super.beginSelection();
            }

            @Override
            public void endSelection() {
                super.endSelection();
                AssetEditImpl.this.setDisabled(false);
            }
        };

        typeListBox = new FormValueListBox<>(
            new AbstractRenderer<AssetType>() {
                @Override
                public String render(AssetType assetType) {
                    if (assetType == null)
                        assetType = AssetType.CUSTOM;
                    return environment.getMessages().assetTypeLabel(assetType.name());
                }
            }
        );

        typeListBox.addValueChangeHandler(event -> {
            if (presenter != null) {
                presenter.onAssetTypeSelected(event.getValue());
            }
        });

        UI ui = GWT.create(UI.class);
        initWidget(ui.createAndBindUi(this));

        accessPublicReadCheckBox.addValueChangeHandler(event -> {
            if (presenter != null) {
                presenter.onAccessPublicRead(event.getValue());
            }
        });

        splitPanel.setOnResize(() -> mapWidget.resize());
    }

    @Override
    public void setPresenter(Presenter presenter) {
        this.presenter = presenter;

        // Restore initial state of view
        sidebarContainer.clear();
        headline.setText(null);
        setFormBusy(true);
        nameGroup.setError(false);
        nameInput.setReadOnly(false);
        nameInput.setValue(null);
        createdOnOutput.setText(null);
        parentAssetSelector.init();
        locationOutput.setCoordinates(null, null);
        centerMapButton.setEnabled(false);
        accessPublicReadCheckBox.setValue(false);
        typeGroup.setVisible(false);
        typeGroup.setError(false);
        typeListBox.setValue(null);
        typeListBox.setAcceptableValues(new ArrayList<>());
        typeListBox.setEnabled(true);
        typeInput.setReadOnly(false);
        typeInput.setVisible(false);
        customTypeInfoLabel.setVisible(false);
        showDroppedPin(GeoJSON.EMPTY_FEATURE_COLLECTION);
        hideMapPopup();
        setDisabled(false);
        attributeViewContainer.clear();
        attributeViews.clear();
        newAttributeFormGroup.setError(false);
        newAttributeNameInputText.setValue(null);
        newAttributeTypeListBox.clear();

        if (presenter != null) {
            assetBrowser.asWidget().removeFromParent();
            sidebarContainer.add(assetBrowser.asWidget());
            assetNavigation.asWidget().removeFromParent();
            assetNavigationContainer.add(assetNavigation.asWidget());
        }
    }

    @Override
    public AssetNavigation getAssetNavigation() {
        return assetNavigation;
    }

    @Override
    public AttributeView.Style getStyle() {
        return style;
    }

    @Override
    public void setFormBusy(boolean busy) {
        super.setFormBusy(busy);
        headline.setVisible(!busy);
        mapWidget.setVisible(!busy);
        if (!busy)
            mapWidget.resize();
        addAttributeButton.setEnabled(!busy);
    }

    /* ############################################################################ */

    @Override
    public void setAsset(Asset asset) {
        this.asset = asset;
    }

    @Override
    public void setName(String name) {
        nameInput.setValue(name);
    }

    @Override
    public String getName() {
        return nameInput.getValue().length() > 0 ? nameInput.getValue() : null;
    }

    @Override
    public void setNameError(boolean error) {
        nameGroup.setError(error);
    }

    @Override
    public void setCreatedOn(Date createdOn) {
        createdOnOutput.setText(
            createdOn != null ? DateTimeFormat.getFormat(Constants.DEFAULT_DATETIME_FORMAT).format(createdOn) : ""
        );
    }

    @Override
    public void setParentNode(BrowserTreeNode treeNode) {
        parentAssetSelector.setSelectedNode(treeNode);
    }

    @Override
    public void setLocation(double[] coordinates) {
        if (locationOutput.setCoordinates(managerMessages.selectLocation(), coordinates)) {
            centerMapButton.setEnabled(true);
        } else {
            centerMapButton.setEnabled(false);
        }
    }

    @Override
    public void initialiseMap(ObjectValue mapOptions) {
        mapWidget.initialise(mapOptions, () -> {
            mapWidget.addNavigationControl();
            mapWidget.setClickListener((lng, lat) -> {
                presenter.onMapClicked(lng, lat);
            });
            if (presenter != null)
                presenter.onMapReady();
        });
    }

    @Override
    public boolean isMapInitialised() {
        return mapWidget.isInitialised();
    }

    @Override
    public void showMapPopup(double lng, double lat, String text) {
        if (mapWidget.isMapReady()) {
            mapWidget.showPopup(lng, lat, text);
        }
    }

    @Override
    public void hideMapPopup() {
        if (mapWidget.isMapReady()) {
            mapWidget.hidePopup();
        }
    }

    @Override
    public void showDroppedPin(GeoJSON geoFeature) {
        if (mapWidget.isMapReady()) {
            mapWidget.showFeature(MapWidget.FEATURE_SOURCE_DROPPED_PIN, geoFeature);
        }
    }

    @Override
    public void flyTo(double[] coordinates) {
        if (mapWidget.isMapReady()) {
            mapWidget.flyTo(coordinates);
        }
    }

    @Override
    public void setAttributeViews(List<AttributeView> attributeViews) {
        this.attributeViews.clear();
        this.attributeViews.addAll(attributeViews);
        refreshAttributeViewContainer();
    }

    @Override
    public void addAttributeViews(List<AttributeView> attributeViews) {
        this.attributeViews.addAll(attributeViews);
        refreshAttributeViewContainer();
    }

    @Override
    public void removeAttributeViews(List<AttributeView> attributeViews) {
        this.attributeViews.removeAll(attributeViews);
        refreshAttributeViewContainer();
    }

    @Override
    public List<AttributeView> getAttributeViews() {
        return attributeViews;
    }

    protected void refreshAttributeViewContainer() {
        attributeViewContainer.clear();

        if (attributeViews.size() == 0) {
            Label emptyLabel = new Label(environment.getMessages().noAttributes());
            emptyLabel.addStyleName(environment.getWidgetStyle().FormListEmptyMessage());
            attributeViewContainer.add(emptyLabel);
        } else {
            sortAttributeViews();
            attributeViews.forEach(
                attributeView -> attributeViewContainer.add(attributeView)
            );
        }
    }

    protected void sortAttributeViews() {
        // Sort by label/name ascending
        attributeViews.sort(Comparator.comparing(o -> o.getAttribute().getLabelOrName().orElse("")));
    }

    @UiHandler("centerMapButton")
    void centerMapClicked(ClickEvent e) {
        if (presenter != null)
            presenter.centerMap();
    }

    @Override
    public void setAccessPublicRead(boolean enabled) {
        accessPublicReadCheckBox.setValue(enabled);
    }

    /* ############################################################################ */

    @Override
    public void selectWellKnownType(AssetType assetType) {
        typeListBox.setValue(assetType);
        typeInput.setVisible(assetType == AssetType.CUSTOM);
    }

    @Override
    public void setAvailableWellKnownTypes(AssetType[] assetTypes) {
        typeListBox.setAcceptableValues(Arrays.asList(assetTypes));
    }

    @Override
    public void setType(String type) {
        typeInput.setValue(type);
        AssetType assetType = AssetType.getByValue(type).orElse(AssetType.CUSTOM);
        if (assetType == AssetType.CUSTOM) {
            headline.setSub(type);
        } else {
            headline.setSub(managerMessages.assetTypeLabel(assetType.name()));
        }
    }

    @Override
    public void setTypeEditable(boolean editable) {
        typeGroup.setVisible(editable);
        typeListBox.setEnabled(editable);
        typeInput.setReadOnly(!editable);
        customTypeInfoLabel.setVisible(editable && typeListBox.getValue() == AssetType.CUSTOM);
    }

    @Override
    public String getType() {
        return typeInput.getValue().length() > 0 ? typeInput.getValue() : null;
    }

    @Override
    public void setTypeError(boolean error) {
        typeGroup.setError(error);
    }

    @Override
    public void setAvailableAttributeTypes(List<Pair<String,String>> displayNamesAndTypes) {
        newAttributeTypeListBox.clear();
        newAttributeTypeListBox.addItem(managerMessages.selectType(), "");
        displayNamesAndTypes.forEach(
            displayNameAndType ->
                newAttributeTypeListBox.addItem(
                    displayNameAndType.key, displayNameAndType.value
                )
        );
    }

    @UiHandler("addAttributeButton")
    public void addAttributeButtonClicked(ClickEvent e) {
        if (presenter != null) {
            if (presenter.addAttribute(
                newAttributeNameInputText.getValue(),
                newAttributeTypeListBox.getSelectedValue()
            )) {
                newAttributeNameInputText.setValue(null);
                newAttributeTypeListBox.setSelectedIndex(0);
            }
        }
    }

    /* ############################################################################ */

    @Override
    public void enableCreate(boolean enable) {
        createButton.setVisible(enable);
        headline.setText(enable ? managerMessages.createAsset() : managerMessages.editAsset());
    }

    @Override
    public void enableUpdate(boolean enable) {
        updateButton.setVisible(enable);
        headline.setText(enable ? managerMessages.editAsset() : managerMessages.createAsset());
    }

    @Override
    public void enableDelete(boolean enable) {
        deleteButton.setVisible(enable);
    }

    @UiHandler("updateButton")
    void updateClicked(ClickEvent e) {
        if (presenter != null)
            presenter.update();
    }

    @UiHandler("createButton")
    void createClicked(ClickEvent e) {
        if (presenter != null)
            presenter.create();
    }

    @UiHandler("deleteButton")
    void deleteClicked(ClickEvent e) {
        if (presenter != null)
            presenter.delete();
    }

    /* ############################################################################ */

    protected void setDisabled(boolean disabled) {
        nameGroup.setDisabled(disabled);
        createdOnGroup.setDisabled(disabled);
        locationGroup.setDisabled(disabled);
        mapWidget.setOpaque(disabled);
        typeGroup.setDisabled(disabled);
        newAttributeFormGroup.setDisabled(disabled);
        submitButtonGroup.setDisabled(disabled);

        attributeViews.forEach(attributeView -> attributeView.setDisabled(disabled));
    }
}
