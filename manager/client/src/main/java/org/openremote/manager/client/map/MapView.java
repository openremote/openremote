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
package org.openremote.manager.client.map;

import com.google.gwt.user.client.ui.IsWidget;
import elemental.json.JsonObject;
import org.openremote.manager.client.util.TextUtil;
import org.openremote.manager.shared.map.GeoJSON;
import org.openremote.manager.shared.map.GeoJSONFeature;
import org.openremote.manager.shared.map.GeoJSONGeometry;
import org.openremote.model.asset.Asset;

public interface MapView extends IsWidget {

    static GeoJSON getFeature(Asset asset) {
        if (asset == null
            || asset.getId() == null
            || asset.getName() == null
            || asset.getCoordinates() == null)
            return GeoJSON.EMPTY_FEATURE_COLLECTION;

        return new GeoJSON().setType("FeatureCollection").setFeatures(
            new GeoJSONFeature().setType("Feature")
                .setProperty("id", asset.getId())
                .setProperty("title", TextUtil.ellipsize(asset.getName(), 20))
                .setGeometry(
                    new GeoJSONGeometry().setPoint(
                        asset.getCoordinates()
                    )
                )
        );
    }

    interface Presenter {
    }

    void setPresenter(Presenter presenter);

    void initialiseMap(JsonObject mapOptions);

    boolean isMapInitialised();

    void showInfo(String text);

    void showFeaturesAll(GeoJSON mapFeatures);

    void hideFeaturesAll();

    void showFeaturesSelection(GeoJSON mapFeatures);

    void hideFeaturesSelection();

    void flyTo(double[] coordinates);

}
