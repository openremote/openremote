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

import elemental.json.JsonObject;
import org.openremote.manager.client.assets.browser.AssetBrowsingView;
import org.openremote.manager.client.assets.device.DeviceAttributesEditor;
import org.openremote.manager.client.widget.AttributesEditor;
import org.openremote.manager.client.widget.FormView;
import org.openremote.manager.shared.asset.AssetType;
import org.openremote.manager.shared.map.GeoJSON;

import java.util.Date;

public interface AssetView extends AssetBrowsingView<AssetView.Presenter>, FormView {

    interface
    Presenter extends AssetBrowsingView.Presenter {

        void beginParentSelection();

        void confirmParentSelection();

        void setRootParentSelection();

        void resetParentSelection();

        void centerMap();

        void onMapClicked(double lng, double lat);

        void onAssetTypeSelected(AssetType value);

        void update();

        void create();

        void delete();

    }

    void setName(String name);

    String getName();

    void setRealm(String realm);

    void setCreatedOn(Date createdOn);

    void setParent(String name);

    void setParentSelection(boolean isSelecting);

    void initialiseMap(JsonObject mapOptions);

    boolean isMapInitialised();

    void setLocation(String location);

    void refreshMap();

    void showMapPopup(double lng, double lat, String text);

    void hideMapPopup();

    void showFeaturesSelection(GeoJSON mapFeatures);

    void hideFeaturesSelection();

    void flyTo(double[] coordinates);

    void setTypeSelectionEnabled(boolean enabled);

    void setEditable(boolean editable);

    void setAvailableTypes(AssetType[] assetTypes);

    void selectType(AssetType assetType);

    void setTypeInputVisible(boolean visible);

    void setType(String type);

    String getType();

    AttributesEditor.Container<AttributesEditor.Style> getAttributesEditorContainer();

    AttributesEditor.Container<DeviceAttributesEditor.Style> getDeviceAttributesEditorContainer();

    void setAttributesEditor(AttributesEditor attributesEditor);

    void enableCreate(boolean enable);

    void enableUpdate(boolean enable);

    void enableDelete(boolean enable);

}
