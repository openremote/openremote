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
import org.openremote.manager.client.assets.browser.AssetBrowser;
import org.openremote.manager.client.assets.device.DeviceAttributesEditor;
import org.openremote.manager.client.widget.*;
import org.openremote.manager.client.widget.PushButton;
import org.openremote.manager.shared.Runnable;
import org.openremote.manager.shared.asset.AssetType;
import org.openremote.manager.shared.map.GeoJSON;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.logging.Logger;

public class AssetViewImpl extends FormViewImpl implements AssetView {

    private static final Logger LOG = Logger.getLogger(AssetViewImpl.class.getName());

    interface UI extends UiBinder<FlexSplitPanel, AssetViewImpl> {
    }

    interface Style extends CssResource {

        String navItem();

        String formMessages();

        String mapWidget();

        String typeInput();
    }

    interface AttributesEditorStyle extends CssResource, AttributesEditor.Style {

        String attributeIntegerEditor();

        String attributeFloatEditor();

        String attributeStringEditor();

        String attributeBooleanEditor();
    }

    interface DeviceAttributesEditorStyle extends CssResource, DeviceAttributesEditor.Style {

        String attributeIntegerEditor();

        String attributeFloatEditor();

        String attributeStringEditor();

        String attributeBooleanEditor();

        String readWriteInput();

        String readButton();

        String writeButton();
    }

    @UiField
    Style style;

    @UiField
    AttributesEditorStyle attributesEditorStyle;

    @UiField
    DeviceAttributesEditorStyle deviceAttributesEditorStyle;

    @UiField
    FlexSplitPanel splitPanel;

    @UiField
    HTMLPanel sidebarContainer;

    /* ############################################################################ */

    @UiField
    FormGroup nameGroup;
    @UiField
    TextBox nameInput;

    @UiField
    FormGroup createdOnGroup;
    @UiField
    Label createdOnLabel;
    @UiField
    FormButton showHistoryButton;

    @UiField
    FormGroup realmGroup;
    @UiField
    Label realmLabel;

    @UiField
    TextBox parentLabel;
    @UiField
    PushButton selectParentButton;
    @UiField
    PushButton setRootParentSelectionButton;
    @UiField
    PushButton confirmParentSelectionButton;
    @UiField
    PushButton resetParentSelectionButton;
    @UiField
    Label selectParentInfoLabel;

    @UiField
    FormGroup locationGroup;
    @UiField
    Label locationLabel;
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
    FormDropDown<AssetType> typeDropDown;
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
    public AssetViewImpl(AssetBrowser assetBrowser, Provider<ConfirmationDialog> confirmationDialogProvider) {
        super(confirmationDialogProvider);
        this.assetBrowser = assetBrowser;

        typeDropDown = new FormDropDown<>(
            new AbstractRenderer<AssetType>() {
                @Override
                public String render(AssetType assetType) {
                    if (assetType == null)
                        return managerMessages.noTypeSelected();
                    return managerMessages.assetTypeLabel(assetType.name());
                }
            }
        );

        typeDropDown.addValueChangeHandler(event -> {
            if (presenter != null) {
                presenter.onAssetTypeSelected(event.getValue());
            }
        });

        UI ui = GWT.create(UI.class);
        initWidget(ui.createAndBindUi(this));

        splitPanel.setOnResize(this::refreshMap);
    }

    @Override
    public void setPresenter(Presenter presenter) {
        this.presenter = presenter;

        // Restore initial state of view
        sidebarContainer.clear();
        nameInput.setReadOnly(false);
        nameInput.setValue(null);
        realmLabel.setText("");
        createdOnLabel.setText("");
        parentLabel.setText("");
        selectParentButton.setVisible(true);
        selectParentButton.setEnabled(true);
        confirmParentSelectionButton.setVisible(false);
        resetParentSelectionButton.setVisible(false);
        setRootParentSelectionButton.setVisible(false);
        selectParentInfoLabel.setVisible(false);
        locationLabel.setText("");
        centerMapButton.setEnabled(false);
        typeDropDown.setValue(null);
        typeDropDown.setAcceptableValues(new ArrayList<>());
        typeDropDown.setEnabled(true);
        typeInput.setVisible(false);
        customTypeInfoLabel.setVisible(false);
        hideFeaturesSelection();
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
    public void setRealm(String realm) {
        realmLabel.setText(realm);
    }

    @Override
    public void setCreatedOn(Date createdOn) {
        createdOnLabel.setText(
            createdOn != null ? DateTimeFormat.getFormat("dd. MMM yyyy HH:mm:ss zzz").format(createdOn) : ""
        );
    }

    @Override
    public void setParent(String name) {
        parentLabel.setText(name != null ? name : "");
    }

    @Override
    public void setParentSelection(boolean isSelecting) {
        selectParentButton.setVisible(!isSelecting);
        confirmParentSelectionButton.setVisible(isSelecting);
        resetParentSelectionButton.setVisible(isSelecting);
        setRootParentSelectionButton.setVisible(isSelecting);
        selectParentInfoLabel.setVisible(isSelecting);
        setOpaque(isSelecting);
    }

    @Override
    public void setLocation(String location) {
        locationLabel.setText(location != null ? location : managerMessages.selectLocation());
        centerMapButton.setEnabled(location != null);
    }

    @Override
    public void initialiseMap(JsonObject mapOptions) {
        mapWidget.initialise(mapOptions);
        mapWidget.addNavigationControl();
        mapWidget.resize();

        mapWidget.setClickListener((lng, lat) -> {
            presenter.onMapClicked(lng, lat);
        });
    }

    @Override
    public boolean isMapInitialised() {
        return mapWidget.isInitialised();
    }

    @Override
    public void refreshMap() {
        mapWidget.resize();
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
    public void hideFeaturesSelection() {
        showFeaturesSelection(GeoJSON.EMPTY_FEATURE_COLLECTION);
    }

    @Override
    public void flyTo(double[] coordinates) {
        mapWidget.flyTo(coordinates);
    }

    @UiHandler("selectParentButton")
    void selectParentClicked(ClickEvent e) {
        if (presenter != null)
            presenter.beginParentSelection();
    }

    @UiHandler("setRootParentSelectionButton")
    void setRootParentSelectionClicked(ClickEvent e) {
        if (presenter != null)
            presenter.setRootParentSelection();
    }

    @UiHandler("confirmParentSelectionButton")
    void confirmParentSelectionClicked(ClickEvent e) {
        if (presenter != null)
            presenter.confirmParentSelection();
    }

    @UiHandler("resetParentSelectionButton")
    void resetParentSelectionClicked(ClickEvent e) {
        if (presenter != null)
            presenter.resetParentSelection();
    }

    @UiHandler("centerMapButton")
    void centerMapClicked(ClickEvent e) {
        if (presenter != null)
            presenter.centerMap();
    }

    /* ############################################################################ */

    @Override
    public void setTypeSelectionEnabled(boolean enabled) {
        typeDropDown.setEnabled(enabled);
    }

    @Override
    public void setEditable(boolean editable) {
        selectParentButton.setEnabled(editable);
        nameInput.setReadOnly(!editable);
    }

    @Override
    public void setAvailableTypes(AssetType[] assetTypes) {
        typeDropDown.setAcceptableValues(Arrays.asList(assetTypes));
    }

    @Override
    public void selectType(AssetType assetType) {
        typeDropDown.setValue(assetType);
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
    public AttributesEditor.Container<AttributesEditor.Style> getAttributesEditorContainer() {
        return new AttributesEditor.Container<AttributesEditor.Style>() {
            @Override
            public FormView getFormView() {
                return AssetViewImpl.this;
            }

            @Override
            public AttributesEditor.Style getStyle() {
                return attributesEditorStyle;
            }

            @Override
            public InsertPanel getPanel() {
                return attributesEditorContainer;
            }

            @Override
            public void showConfirmation(String title, String text, Runnable onConfirm) {
                AssetViewImpl.this.showConfirmation(title, text, onConfirm);
            }

            @Override
            public void showConfirmation(String title, String text, Runnable onConfirm, Runnable onCancel) {
                AssetViewImpl.this.showConfirmation(title, text, onConfirm, onCancel);
            }
        };
    }

    @Override
    public AttributesEditor.Container<DeviceAttributesEditor.Style> getDeviceAttributesEditorContainer() {
        return new AttributesEditor.Container<DeviceAttributesEditor.Style>() {
            @Override
            public FormView getFormView() {
                return AssetViewImpl.this;
            }

            @Override
            public DeviceAttributesEditor.Style getStyle() {
                return deviceAttributesEditorStyle;
            }

            @Override
            public InsertPanel getPanel() {
                return attributesEditorContainer;
            }

            @Override
            public void showConfirmation(String title, String text, Runnable onConfirm) {
                AssetViewImpl.this.showConfirmation(title, text, onConfirm);
            }

            @Override
            public void showConfirmation(String title, String text, Runnable onConfirm, Runnable onCancel) {
                AssetViewImpl.this.showConfirmation(title, text, onConfirm, onCancel);
            }
        };
    }

    @Override
    public void setAttributesEditor(AttributesEditor editor) {
        this.attributesEditor = editor;
        attributesEditorContainer.clear();
        editor.render();
    }

    /* ############################################################################ */

    @Override
    public void enableCreate(boolean enable) {
        createButton.setVisible(enable);
    }

    @Override
    public void enableUpdate(boolean enable) {
        updateButton.setVisible(enable);
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
