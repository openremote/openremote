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

import com.google.gwt.user.client.ui.IsWidget;
import org.openremote.manager.client.assets.attributes.AttributesEditor;
import org.openremote.manager.client.assets.browser.BrowserTreeNode;
import org.openremote.manager.client.widget.FormView;
import org.openremote.model.asset.AssetType;
import org.openremote.model.geo.GeoJSON;
import org.openremote.model.value.ObjectValue;

import java.util.Date;

public interface AssetEdit extends FormView, IsWidget {

    interface Presenter {

        void onMapReady();

        void onParentSelection(BrowserTreeNode treeNode);

        void centerMap();

        void onMapClicked(double lng, double lat);

        void onAssetTypeSelected(AssetType value);

        void update();

        void create();

        void delete();
    }

    void setAssetViewHistoryToken(String token);

    void setPresenter(Presenter presenter);

    void setName(String name);

    String getName();

    void setNameError(boolean error);

    void setCreatedOn(Date createdOn);

    void setParentNode(BrowserTreeNode treeNode);

    void initialiseMap(ObjectValue mapOptions);

    boolean isMapInitialised();

    void setLocation(double[] coordinates);

    void showMapPopup(double lng, double lat, String text);

    void hideMapPopup();

    void showDroppedPin(GeoJSON geoFeature);

    void flyTo(double[] coordinates);

    void selectWellKnownType(AssetType assetType);

    void setAvailableWellKnownTypes(AssetType[] assetTypes);

    void setType(String type);

    void setTypeEditable(boolean editable);

    String getType();

    void setTypeError(boolean error);

    AttributesEditor.Container getAttributesEditorContainer();

    void setAttributesEditor(AttributesEditor attributesEditor);

    void enableCreate(boolean enable);

    void enableUpdate(boolean enable);

    void enableDelete(boolean enable);

}
