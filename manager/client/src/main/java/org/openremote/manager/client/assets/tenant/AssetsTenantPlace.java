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
package org.openremote.manager.client.assets.tenant;

import com.google.gwt.place.shared.Place;
import com.google.gwt.place.shared.PlaceTokenizer;
import com.google.gwt.place.shared.Prefix;
import org.openremote.manager.client.assets.AssetsPlace;

public class AssetsTenantPlace extends Place implements AssetsPlace {

    final String realm;

    public AssetsTenantPlace(String realm) {
        this.realm = realm;
    }

    public String getRealm() {
        return realm;
    }

    @Prefix("assetsTenant")
    public static class Tokenizer implements PlaceTokenizer<AssetsTenantPlace> {

        @Override
        public AssetsTenantPlace getPlace(String token) {
            return new AssetsTenantPlace(token != null && token.length() > 0 ? token : null);
        }

        @Override
        public String getToken(AssetsTenantPlace place) {
            return place.getRealm() != null ? place.getRealm() : "";
        }
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
            "realm='" + realm + '\'' +
            '}';
    }
}
