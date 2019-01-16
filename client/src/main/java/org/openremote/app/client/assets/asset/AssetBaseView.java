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
import org.openremote.app.client.AppSecurity;
import org.openremote.app.client.assets.attributes.AttributeView;
import org.openremote.app.client.assets.browser.BrowserTreeNode;
import org.openremote.app.client.assets.navigation.AssetNavigation;
import org.openremote.model.asset.Asset;
import org.openremote.model.geo.GeoJSON;
import org.openremote.model.geo.GeoJSONPoint;
import org.openremote.model.value.ObjectValue;

import java.util.Date;
import java.util.List;

public interface AssetBaseView<P extends AssetBaseView.Presenter> extends IsWidget {

    interface Presenter {

        void onMapReady();

        AppSecurity getSecurity();

        void start();

        void centerMap();

        void loadParent();

        void writeParentToView();

        void writeAssetToView();

        void writeAttributesToView();
    }

    void setPresenter(P presenter);

    AssetNavigation getAssetNavigation();

    AttributeView.Style getStyle();

    void setFormBusy(boolean busy);

    void setAsset(Asset asset);

    void setName(String name);

    void setCreatedOn(Date createdOn);

    void setParentNode(BrowserTreeNode treeNode);

    void initialiseMap(ObjectValue mapOptions);

    boolean isMapInitialised();

    void setLocation(GeoJSONPoint point);

    void showDroppedPin(GeoJSON geoFeature);

    void flyTo(GeoJSONPoint point);

    void setAccessPublicRead(boolean enabled);

    void setAttributeViews(List<AttributeView> attributeViews);

    void addAttributeViews(List<AttributeView> attributeViews);

    void removeAttributeViews(List<AttributeView> attributeViews);

    List<AttributeView> getAttributeViews();
}
