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
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.inject.Provider;
import org.openremote.manager.client.Environment;
import org.openremote.manager.client.app.dialog.Confirmation;
import org.openremote.manager.client.assets.attributes.AttributeView;
import org.openremote.manager.client.assets.browser.AssetBrowser;
import org.openremote.manager.client.assets.browser.BrowserTreeNode;
import org.openremote.manager.client.assets.navigation.AssetNavigation;
import org.openremote.manager.client.widget.FlexSplitPanel;
import org.openremote.manager.client.widget.FormViewImpl;
import org.openremote.manager.client.widget.Headline;
import org.openremote.model.asset.Asset;
import org.openremote.model.geo.GeoJSON;
import org.openremote.model.value.ObjectValue;

import javax.inject.Inject;
import java.util.Date;
import java.util.List;

public class AssetLinkUsersImpl extends FormViewImpl implements AssetLinkUsers {

    interface UI extends UiBinder<FlexSplitPanel, AssetLinkUsersImpl> {
    }

    interface Style extends CssResource, AttributeView.Style {

        String navItem();

        String formMessages();

        String booleanEditor();

        String metaItemValueEditor();

        String stringEditor();

        String numberEditor();

        String metaItemNameEditor();

        String regularAttribute();

        String highlightAttribute();

        String agentLinkEditor();
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

    final AssetBrowser assetBrowser;
    final AssetNavigation assetNavigation;
    final Environment environment;
    Presenter presenter;
    Asset asset;

    @Inject
    public AssetLinkUsersImpl(AssetBrowser assetBrowser,
                              AssetNavigation assetNavigation,
                              Provider<Confirmation> confirmationDialogProvider,
                              Environment environment) {
        super(confirmationDialogProvider);
        this.assetBrowser = assetBrowser;
        this.assetNavigation = assetNavigation;
        this.environment = environment;

        UI ui = GWT.create(UI.class);
        initWidget(ui.createAndBindUi(this));
    }

    @Override
    public void setPresenter(Presenter presenter) {
        this.presenter = presenter;

        // Restore initial state of view
        sidebarContainer.clear();
        headline.setText(null);
        setFormBusy(true);
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
    }

    @Override
    public void showDroppedPin(GeoJSON geoFeature) {

    }

    @Override
    public void flyTo(double[] coordinates) {

    }

    @Override
    public void setAttributeViews(List<AttributeView> attributeViews) {

    }

    @Override
    public void addAttributeViews(List<AttributeView> attributeViews) {

    }

    @Override
    public void removeAttributeViews(List<AttributeView> attributeViews) {

    }

    @Override
    public List<AttributeView> getAttributeViews() {
        return null;
    }

    /* ############################################################################ */

    @Override
    public void setAsset(Asset asset) {
        this.asset = asset;
    }

    @Override
    public void setName(String name) {

    }

    @Override
    public void setCreatedOn(Date createdOn) {

    }

    @Override
    public void setParentNode(BrowserTreeNode treeNode) {

    }

    @Override
    public void initialiseMap(ObjectValue mapOptions) {

    }

    @Override
    public boolean isMapInitialised() {
        return true;
    }

    @Override
    public void setLocation(double[] coordinates) {

    }
}
