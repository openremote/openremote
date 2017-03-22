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
import elemental.json.JsonObject;
import org.openremote.manager.client.app.dialog.ConfirmationDialog;
import org.openremote.manager.client.assets.attributes.AttributesEditor;
import org.openremote.manager.client.assets.browser.AssetBrowser;
import org.openremote.manager.client.assets.browser.AssetSelector;
import org.openremote.manager.client.assets.browser.BrowserTreeNode;
import org.openremote.manager.client.i18n.ManagerMessages;
import org.openremote.manager.client.widget.*;
import org.openremote.manager.client.widget.PushButton;
import org.openremote.manager.shared.map.GeoJSON;
import org.openremote.model.Constants;
import org.openremote.model.asset.AssetType;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;

public class AssetEditImpl extends FormViewImpl implements AssetEdit {

    interface UI extends UiBinder<FlexSplitPanel, AssetEditImpl> {
    }

    interface Style extends CssResource {

        String navItem();

        String formMessages();

        String mapWidget();
    }

    interface AttributesEditorStyle extends CssResource, AttributesEditor.Style {

        String integerEditor();

        String decimalEditor();

        String stringEditor();

        String booleanEditor();
    }

    @UiField
    Style style;

    @UiField
    AttributesEditorStyle attributesEditorStyle;

    @UiField
    FlexSplitPanel splitPanel;

    @UiField
    HTMLPanel sidebarContainer;

    @UiField
    InlineLabel headlineLabel;

    @UiField
    FormButton viewButton;

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

    /* ############################################################################ */

    @UiField
    Form attributesForm;
    @UiField
    FormGroup typeGroup;
    @UiField(provided = true)
    FormValueListBox<AssetType> typeListBox;
    @UiField
    FormInputText typeInput;
    @UiField
    Label customTypeInfoLabel;
    @UiField
    FlowPanel attributesEditorContainer;

    /* ############################################################################ */

    @UiField
    Form submitForm;
    @UiField
    FormGroup submitButtonGroup;
    @UiField
    PushButton createButton;
    @UiField
    PushButton updateButton;
    @UiField
    PushButton deleteButton;

    final AssetBrowser assetBrowser;
    Presenter presenter;
    AttributesEditor attributesEditor;

    @Inject
    public AssetEditImpl(AssetBrowser assetBrowser,
                         Provider<ConfirmationDialog> confirmationDialogProvider,
                         ManagerMessages managerMessages) {
        super(confirmationDialogProvider);
        this.assetBrowser = assetBrowser;

        parentAssetSelector = new AssetSelector(
            assetBrowser.getPresenter(),
            managerMessages,
            managerMessages.parentAsset(),
            managerMessages.selectAssetDescription(),
            treeNode -> {
                if (presenter != null) {
                    presenter.onParentSelection(treeNode);
                }
            }
        ) {
            @Override
            public void beginSelection() {
                AssetEditImpl.this.setOpaque(true);
                super.beginSelection();
            }

            @Override
            public void endSelection() {
                super.endSelection();
                AssetEditImpl.this.setOpaque(false);
            }
        };

        typeListBox = new FormValueListBox<>(
            new AbstractRenderer<AssetType>() {
                @Override
                public String render(AssetType assetType) {
                    if (assetType == null)
                        assetType = AssetType.CUSTOM;
                    return managerMessages.assetTypeLabel(assetType.name());
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

        splitPanel.setOnResize(() -> mapWidget.resize());
    }

    @Override
    public void setPresenter(Presenter presenter) {
        this.presenter = presenter;

        // Restore initial state of view
        sidebarContainer.clear();
        headlineLabel.setText(null);
        setFormBusy(true);
        nameGroup.setError(false);
        nameInput.setReadOnly(false);
        nameInput.setValue(null);
        createdOnOutput.setText(null);
        parentAssetSelector.init();
        locationOutput.setText(null);
        centerMapButton.setEnabled(false);
        typeGroup.setError(false);
        typeListBox.setValue(null);
        typeListBox.setAcceptableValues(new ArrayList<>());
        typeListBox.setEnabled(true);
        typeInput.setVisible(false);
        customTypeInfoLabel.setVisible(false);
        showFeaturesSelection(GeoJSON.EMPTY_FEATURE_COLLECTION);
        hideMapPopup();
        setOpaque(false);
        attributesEditorContainer.clear();
        attributesEditor = null;

        if (presenter != null) {
            assetBrowser.asWidget().removeFromParent();
            sidebarContainer.add(assetBrowser.asWidget());
        }
    }

    @Override
    public void setFormBusy(boolean busy) {
        super.setFormBusy(busy);
        attributesForm.setBusy(busy);
        submitForm.setBusy(busy);
    }

    @UiHandler("viewButton")
    void viewClicked(ClickEvent e) {
        if (presenter != null)
            presenter.view();
    }

    /* ############################################################################ */

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
    public void initialiseMap(JsonObject mapOptions) {
        mapWidget.initialise(mapOptions, () -> {
        });
        mapWidget.addNavigationControl();
        mapWidget.setClickListener((lng, lat) -> {
            presenter.onMapClicked(lng, lat);
        });
    }

    @Override
    public boolean isMapInitialised() {
        return mapWidget.isInitialised();
    }

    @Override
    public void showMapPopup(double lng, double lat, String text) {
        mapWidget.showPopup(lng, lat, text);
    }

    @Override
    public void hideMapPopup() {
        mapWidget.hidePopup();
    }

    @Override
    public void showFeaturesSelection(GeoJSON mapFeatures) {
        mapWidget.showFeatures(MapWidget.FEATURES_SOURCE_SELECTION, mapFeatures);
    }

    @Override
    public void flyTo(double[] coordinates) {
        mapWidget.flyTo(coordinates);
    }

    @UiHandler("centerMapButton")
    void centerMapClicked(ClickEvent e) {
        if (presenter != null)
            presenter.centerMap();
    }

    /* ############################################################################ */

    @Override
    public void setTypeSelectionEnabled(boolean enabled) {
        typeListBox.setEnabled(enabled);
    }

    @Override
    public void setAvailableTypes(AssetType[] assetTypes) {
        typeListBox.setAcceptableValues(Arrays.asList(assetTypes));
    }

    @Override
    public void selectType(AssetType assetType) {
        typeListBox.setValue(assetType);
    }

    @Override
    public void setTypeInputVisible(boolean visible) {
        typeInput.setVisible(visible);
        customTypeInfoLabel.setVisible(visible);
    }

    @Override
    public void setType(String type) {
        typeInput.setValue(type);
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
    public AttributesEditor.Container getAttributesEditorContainer() {
        return new AttributesEditor.Container() {
            @Override
            public AttributesEditor.Style getStyle() {
                return attributesEditorStyle;
            }

            @Override
            public InsertPanel getPanel() {
                return attributesEditorContainer;
            }

            @Override
            public ManagerMessages getMessages() {
                return managerMessages;
            }
        };
    }

    @Override
    public void setAttributesEditor(AttributesEditor editor) {
        this.attributesEditor = editor;
        attributesEditorContainer.clear();
    }

    /* ############################################################################ */

    @Override
    public void enableCreate(boolean enable) {
        createButton.setVisible(enable);
        headlineLabel.setText(enable ? managerMessages.createAsset() : managerMessages.editAsset());
    }

    @Override
    public void enableUpdate(boolean enable) {
        viewButton.setVisible(enable);
        updateButton.setVisible(enable);
        headlineLabel.setText(enable ? managerMessages.editAsset() : managerMessages.createAsset());
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

    protected void setOpaque(boolean opaque) {
        nameGroup.setOpaque(opaque);
        createdOnGroup.setOpaque(opaque);
        locationGroup.setOpaque(opaque);
        mapWidget.setOpaque(opaque);
        typeGroup.setOpaque(opaque);
        if (attributesEditor != null)
            attributesEditor.setOpaque(opaque);
        submitButtonGroup.setOpaque(opaque);
    }
}
