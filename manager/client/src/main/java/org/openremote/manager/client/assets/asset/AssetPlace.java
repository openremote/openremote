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

import com.google.gwt.place.shared.Place;
import com.google.gwt.place.shared.PlaceTokenizer;
import com.google.gwt.place.shared.Prefix;
import org.openremote.manager.client.assets.AssetsPlace;

public class AssetPlace extends Place implements AssetsPlace {

    final String assetId;

    public AssetPlace(String assetId) {
        this.assetId = assetId;
    }

    public AssetPlace() {
        this.assetId = null;
    }

    public String getAssetId() {
        return assetId;
    }

    @Prefix("asset")
    public static class Tokenizer implements PlaceTokenizer<AssetPlace> {

        @Override
        public AssetPlace getPlace(String token) {
            return new AssetPlace(token != null && token.length() > 0 ? token : null);
        }

        @Override
        public String getToken(AssetPlace place) {
            return place.getAssetId() != null ? place.getAssetId() : "";
        }
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
            "assetId='" + assetId + '\'' +
            '}';
    }
}
