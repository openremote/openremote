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
import org.openremote.manager.client.rules.RulesEditorPlace;

public class AssetRulesEditorPlace extends RulesEditorPlace {

    final protected String assetId;

    public AssetRulesEditorPlace(String assetId) {
        super();
        this.assetId = assetId;
    }

    public AssetRulesEditorPlace(String assetId, String definitionId) {
        super(definitionId);
        this.assetId = assetId;
    }

    public AssetRulesEditorPlace(String assetId, Long definitionId) {
        super(definitionId);
        this.assetId = assetId;
    }

    public String getAssetId() {
        return assetId;
    }

    @Prefix("assetRulesEditor")
    public static class Tokenizer implements PlaceTokenizer<AssetRulesEditorPlace> {

        @Override
        public AssetRulesEditorPlace getPlace(String token) {
            if (token == null) {
                throw new IllegalArgumentException("Invalid empty token");
            }
            String[] fields = token.split(":");
            if (fields.length == 1) {
                return new AssetRulesEditorPlace(fields[0]);
            } else if (fields.length == 2) {
                return new AssetRulesEditorPlace(fields[0], fields[1]);
            } else {
                throw new IllegalArgumentException("Invalid token: " + token);
            }
        }

        @Override
        public String getToken(AssetRulesEditorPlace place) {
            if (place.getAssetId() == null) {
                return "";
            }
            return place.getAssetId() + ":" + (place.getDefinitionId() != null ? place.getDefinitionId() : "");
        }
    }
}
