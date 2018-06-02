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
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.*;
import com.google.inject.Provider;
import org.openremote.app.client.widget.*;
import org.openremote.app.client.widget.PushButton;
import org.openremote.app.client.Environment;
import org.openremote.app.client.app.dialog.JsonEditor;
import org.openremote.app.client.assets.attributes.AttributeView;
import org.openremote.app.client.assets.browser.AssetBrowser;
import org.openremote.app.client.assets.browser.AssetSelector;
import org.openremote.app.client.assets.browser.BrowserTreeNode;
import org.openremote.app.client.assets.navigation.AssetNavigation;
import org.openremote.app.client.i18n.ManagerMessages;
import org.openremote.app.client.style.WidgetStyle;
import org.openremote.model.Constants;
import org.openremote.model.asset.Asset;
import org.openremote.model.asset.AssetType;
import org.openremote.model.geo.GeoJSON;
import org.openremote.model.geo.GeoJSONFeatureCollection;
import org.openremote.model.geo.GeoJSONPoint;
import org.openremote.model.value.ObjectValue;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class AssetViewImpl extends Composite implements AssetView {

    interface UI extends UiBinder<FlexSplitPanel, AssetViewImpl> {
    }

    interface Style extends CssResource, AttributeView.Style {

        String navItem();

        String mapWidget();

        String stringEditor();

        String numberEditor();

        String booleanEditor();

        String metaItemValueEditor();

        String metaItemNameEditor();

        String agentLinkEditor();

        String attributeView();
    }

    @UiField
    public WidgetStyle widgetStyle;

    @UiField
    public ManagerMessages managerMessages;

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

    @UiField
    FormGroup accessPublicReadGroup;
    @UiField
    FormCheckBox accessPublicReadCheckBox;
    @UiField
    FormAnchor accessPublicReadAnchor;

    /* ############################################################################ */

    FlowPanel liveUpdatesNavItem = new FlowPanel();
    PushButton liveUpdatesOnButton = new PushButton();
    PushButton liveUpdatesOffButton = new PushButton();
    PushButton refreshButton = new PushButton();

    /* ############################################################################ */

    @UiField
    FlowPanel attributeViewContainer;

    /* ############################################################################ */

    final AssetBrowser assetBrowser;
    final AssetNavigation assetNavigation;
    final Provider<JsonEditor> jsonEditorProvider;
    final Environment environment;
    final List<AttributeView> attributeViews = new ArrayList<>();
    protected Presenter presenter;
    protected Asset asset;

    @Inject
    public AssetViewImpl(AssetBrowser assetBrowser,
                         AssetNavigation assetNavigation,
                         Environment environment,
                         Provider<JsonEditor> jsonEditorProvider) {
        this.assetBrowser = assetBrowser;
        this.assetNavigation = assetNavigation;
        this.jsonEditorProvider = jsonEditorProvider;
        this.environment = environment;

        UI ui = GWT.create(UI.class);
        initWidget(ui.createAndBindUi(this));

        splitPanel.setOnResize(() -> mapWidget.resize());

        liveUpdatesOnButton.setText(managerMessages.showLiveUpdates());
        liveUpdatesOnButton.setVisible(false);
        liveUpdatesOnButton.addStyleName(style.navItem());
        liveUpdatesOnButton.addStyleName(widgetStyle.SecondaryNavItem());
        liveUpdatesOnButton.addStyleName("primary");
        liveUpdatesOnButton.setIcon("check-square");
        liveUpdatesOnButton.addClickHandler(event -> {
            if (presenter != null)
                presenter.enableLiveUpdates(false);
            liveUpdatesOnButton.setVisible(false);
            liveUpdatesOffButton.setVisible(true);
        });

        liveUpdatesOffButton.setText(managerMessages.showLiveUpdates());
        liveUpdatesOffButton.addStyleName(style.navItem());
        liveUpdatesOffButton.addStyleName(widgetStyle.SecondaryNavItem());
        liveUpdatesOffButton.setIcon("square-o");
        liveUpdatesOffButton.addClickHandler(event -> {
            if (presenter != null)
                presenter.enableLiveUpdates(true);
            liveUpdatesOnButton.setVisible(true);
            liveUpdatesOffButton.setVisible(false);
        });

        liveUpdatesNavItem.add(liveUpdatesOnButton);
        liveUpdatesNavItem.add(liveUpdatesOffButton);

        refreshButton.setText(managerMessages.refreshAllAttributes());
        refreshButton.addStyleName(style.navItem());
        refreshButton.addStyleName(widgetStyle.SecondaryNavItem());
        refreshButton.setIcon("refresh");
        refreshButton.addClickHandler(event -> {
            if (presenter != null)
                presenter.refresh();
        });

        setFormBusy(true);
    }

    @Override
    public void setPresenter(Presenter presenter) {
        this.presenter = presenter;

        // Restore initial state of view
        sidebarContainer.clear();
        setFormBusy(true);
        headline.setText(null);
        headline.setSub(null);
        // Live updates button state is not reset, it's static for AssetViewActivity
        createdOnOutput.setText(null);
        tenantDisplayName.setText(null);
        parentAssetName.setText(null);
        locationGroup.setVisible(false);
        locationOutput.setCoordinates(null, null);
        mapWidget.setVisible(false);
        showDroppedPin(GeoJSONFeatureCollection.EMPTY);

        accessPublicReadGroup.setVisible(false);
        accessPublicReadCheckBox.setValue(false);
        accessPublicReadAnchor.setHref("");

        attributeViews.clear();
        attributeViewContainer.clear();

        if (presenter != null) {
            assetBrowser.asWidget().removeFromParent();
            sidebarContainer.add(assetBrowser.asWidget());
            assetNavigation.asWidget().removeFromParent();
            assetNavigationContainer.add(assetNavigation.asWidget());
            assetNavigation.addNavItem(liveUpdatesNavItem.asWidget());
            assetNavigation.addNavItem(refreshButton.asWidget());
        }
    }

    @Override
    public AssetNavigation getAssetNavigation() {
        return assetNavigation;
    }

    @Override
    public void setFormBusy(boolean busy) {
        headline.setVisible(!busy);
        form.setBusy(busy);
        if (!busy && locationGroup.isVisible()) {
            mapWidget.setVisible(true);
            mapWidget.resize();
        } else {
            mapWidget.setVisible(false);
        }
    }

    /* ############################################################################ */

    @Override
    public AttributeView.Style getStyle() {
        return style;
    }

    @Override
    public void setAsset(Asset asset) {
        this.asset = asset;
    }

    @Override
    public void setName(String name) {
        headline.setText(name);
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
    public void setLocation(GeoJSONPoint point) {
        if (locationOutput.setCoordinates(managerMessages.selectLocation(), point)) {
            locationGroup.setVisible(true);
            mapWidget.setVisible(true);
            mapWidget.resize();
        } else {
            locationGroup.setVisible(false);
            mapWidget.setVisible(false);
        }
    }

    @Override
    public void initialiseMap(ObjectValue mapOptions) {
        mapWidget.initialise(mapOptions, () -> {
            mapWidget.addNavigationControl();
            if (presenter != null)
                presenter.onMapReady();
        });
    }

    @Override
    public boolean isMapInitialised() {
        return mapWidget.isInitialised();
    }

    @Override
    public void showDroppedPin(GeoJSON geoFeature) {
        if (mapWidget.isMapReady()) {
            mapWidget.showFeature(MapWidget.FEATURE_SOURCE_DROPPED_PIN, geoFeature);
        }
    }

    @Override
    public void flyTo(GeoJSONPoint point) {
        if (mapWidget.isMapReady()) {
            mapWidget.flyTo(point);
        }
    }

    @Override
    public void setAccessPublicRead(boolean enabled) {
        accessPublicReadGroup.setVisible(enabled);
        accessPublicReadCheckBox.setValue(enabled);
    }

    @Override
    public void setAccessPublicReadAnchor(String path) {
        accessPublicReadAnchor.setHref(Window.Location.getProtocol() + "//" + Window.Location.getHost() + path);
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

    @Override
    public void setIconAndType(String icon, String type) {
        headline.setIcon(icon);
        //TODO replace with AssetModel getValues, through a http request
        AssetType assetType = AssetType.getByValue(type).orElse(AssetType.CUSTOM);
        if (assetType == AssetType.CUSTOM) {
            headline.setSub(type);
        } else {
            headline.setSub(managerMessages.assetTypeLabel(assetType.name()));
        }
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
        // Executable commands first, then sort by label/name ascending
        attributeViews.sort((o1, o2) -> {
            if (o1.getAttribute().isExecutable() && !o2.getAttribute().isExecutable()) {
                return -1;
            } else if (!o1.getAttribute().isExecutable() && o2.getAttribute().isExecutable()) {
                return 1;
            } else {
                return o1.getAttribute().getLabelOrName().orElse("").compareTo(o2.getAttribute().getLabelOrName().orElse(""));
            }
        });
    }

    /* ############################################################################ */

    @UiHandler("centerMapButton")
    void centerMapClicked(ClickEvent e) {
        if (presenter != null)
            presenter.centerMap();
    }
}
