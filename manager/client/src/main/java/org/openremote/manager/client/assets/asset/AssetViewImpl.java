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
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.*;
import com.google.inject.Provider;
import elemental.json.JsonObject;
import org.openremote.manager.client.app.dialog.ConfirmationDialog;
import org.openremote.manager.client.assets.attributes.AttributesBrowser;
import org.openremote.manager.client.assets.browser.AssetBrowser;
import org.openremote.manager.client.assets.browser.AssetSelector;
import org.openremote.manager.client.assets.browser.BrowserTreeNode;
import org.openremote.manager.client.i18n.ManagerMessages;
import org.openremote.manager.client.style.WidgetStyle;
import org.openremote.manager.client.widget.*;
import org.openremote.manager.shared.map.GeoJSON;
import org.openremote.model.Constants;

import javax.inject.Inject;
import java.util.Date;

public class AssetViewImpl extends Composite implements AssetView {

    interface UI extends UiBinder<FlexSplitPanel, AssetViewImpl> {
    }

    interface Style extends CssResource {

        String navItem();

        String mapWidget();
    }

    interface AttributesBrowserStyle extends CssResource, AttributesBrowser.Style {

        String integerEditor();

        String decimalEditor();

        String stringEditor();

        String booleanEditor();
    }

    @UiField
    public WidgetStyle widgetStyle;

    @UiField
    public ManagerMessages managerMessages;

    @UiField
    Style style;

    @UiField
    AttributesBrowserStyle attributesBrowserStyle;

    @UiField
    FlexSplitPanel splitPanel;

    @UiField
    HTMLPanel sidebarContainer;

    @UiField
    InlineLabel headlineLabel;

    @UiField
    FormButton editButton;

    /* ############################################################################ */

    @UiField
    public Form form;

    @UiField
    FormGroup createdOnGroup;
    @UiField
    FormOutputText createdOnOutput;
    @UiField
    FormButton showHistoryButton;

    @UiField
    FormGroup parentGroup;
    @UiField
    FormOutputText tenantDisplayName;
    @UiField
    FormInputText parentAssetName;

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

    @UiField
    FormInputText typeLabel;

    @UiField
    FlowPanel attributesBrowserContainer;

    /* ############################################################################ */

    final AssetBrowser assetBrowser;
    final Provider<ConfirmationDialog> confirmationDialogProvider;
    Presenter presenter;
    AttributesBrowser attributesBrowser;

    @Inject
    public AssetViewImpl(AssetBrowser assetBrowser,
                         Provider<ConfirmationDialog> confirmationDialogProvider) {
        this.assetBrowser = assetBrowser;
        this.confirmationDialogProvider = confirmationDialogProvider;

        UI ui = GWT.create(UI.class);
        initWidget(ui.createAndBindUi(this));

        splitPanel.setOnResize(() -> mapWidget.resize());

        setFormBusy(true);
    }

    @Override
    public void setPresenter(Presenter presenter) {
        this.presenter = presenter;

        // Restore initial state of view
        sidebarContainer.clear();
        setFormBusy(true);
        headlineLabel.setText(null);
        createdOnOutput.setText(null);
        tenantDisplayName.setText(null);
        parentAssetName.setText(null);
        locationGroup.setVisible(false);
        locationOutput.setCoordinates(null, null);
        mapWidget.setVisible(false);
        showFeaturesSelection(GeoJSON.EMPTY_FEATURE_COLLECTION);
        typeLabel.setText(null);
        attributesBrowserContainer.clear();
        attributesBrowser = null;

        if (presenter != null) {
            assetBrowser.asWidget().removeFromParent();
            sidebarContainer.add(assetBrowser.asWidget());
        }
    }

    @Override
    public void setFormBusy(boolean busy) {
        form.setBusy(busy);
        attributesForm.setBusy(busy);
    }

    /* ############################################################################ */

    @Override
    public void setName(String name) {
        headlineLabel.setText(name);
    }

    @UiHandler("editButton")
    void editClicked(ClickEvent e) {
        if (presenter != null)
            presenter.edit();
    }

    @Override
    public void setCreatedOn(Date createdOn) {
        createdOnOutput.setText(
            createdOn != null ? DateTimeFormat.getFormat(Constants.DEFAULT_DATETIME_FORMAT).format(createdOn) : ""
        );
    }

    @Override
    public void setParentNode(BrowserTreeNode treeNode) {
        AssetSelector.renderTreeNode(managerMessages, treeNode, tenantDisplayName, parentAssetName);
    }

    /* ############################################################################ */

    @Override
    public void setLocation(double[] coordinates) {
        if (locationOutput.setCoordinates(managerMessages.selectLocation(), coordinates)) {
            locationGroup.setVisible(true);
            mapWidget.setVisible(true);
            mapWidget.resize();
        } else {
            locationGroup.setVisible(false);
            mapWidget.setVisible(false);
        }
    }

    @Override
    public void initialiseMap(JsonObject mapOptions) {
        mapWidget.initialise(mapOptions, () -> {
        });
        mapWidget.addNavigationControl();
    }

    @Override
    public boolean isMapInitialised() {
        return mapWidget.isInitialised();
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
    public void setType(String type) {
        typeLabel.setText(type);
    }

    @Override
    public AttributesBrowser.Container getAttributesBrowserContainer() {
        return new AttributesBrowser.Container() {

            @Override
            public AttributesBrowser.Style getStyle() {
                return attributesBrowserStyle;
            }

            @Override
            public InsertPanel getPanel() {
                return attributesBrowserContainer;
            }

            @Override
            public ManagerMessages getMessages() {
                return managerMessages;
            }
        };
    }

    @Override
    public void setAttributesBrowser(AttributesBrowser browser) {
        this.attributesBrowser = browser;
        attributesBrowserContainer.clear();
    }
}
