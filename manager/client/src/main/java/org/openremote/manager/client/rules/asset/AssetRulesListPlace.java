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
package org.openremote.manager.client.rules.asset;

import com.google.gwt.place.shared.PlaceTokenizer;
import com.google.gwt.place.shared.Prefix;
import org.openremote.manager.client.rules.RulesPlace;

public class AssetRulesListPlace extends RulesPlace {

    final String assetId;

    public AssetRulesListPlace(String assetId) {
        this.assetId = assetId;
    }

    public String getAssetId() {
        return assetId;
    }

    @Prefix("assetRules")
    public static class Tokenizer implements PlaceTokenizer<AssetRulesListPlace> {

        @Override
        public AssetRulesListPlace getPlace(String token) {
            return new AssetRulesListPlace(token != null && token.length() > 0 ? token : null);
        }

        @Override
        public String getToken(AssetRulesListPlace place) {
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
