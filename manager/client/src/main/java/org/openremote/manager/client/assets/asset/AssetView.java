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
import org.openremote.manager.shared.map.GeoJSON;

import java.util.Date;

public interface AssetView extends AssetBrowsingView<AssetView.Presenter> {

    interface Presenter extends AssetBrowsingView.Presenter {

        void onMapClicked(double lng, double lat);

        void update();

        void create();

        void delete();
    }

    void setFormBusy(boolean busy);

    void setName(String name);

    String getName();

    void setType(String type);

    String getType();

    void setCreatedOn(Date createdOn);

    void initialiseMap(JsonObject mapOptions);

    boolean isMapInitialised();

    void showPopup(double lng, double lat, String text);

    void showFeaturesSelection(GeoJSON mapFeatures);

    void hideFeaturesSelection();

    void flyTo(double[] coordinates);

    void enableCreate(boolean enable);

    void enableUpdate(boolean enable);

    void enableDelete(boolean enable);


}
