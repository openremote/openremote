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
package org.openremote.app.client.notifications;

import com.google.gwt.place.shared.Place;
import com.google.gwt.place.shared.PlaceTokenizer;
import com.google.gwt.place.shared.Prefix;
import org.openremote.model.util.TextUtil;

public class NotificationsPlace extends Place {

    final String realm;
    final String assetId;

    public NotificationsPlace(String realm, String assetId) {
        this.realm = realm;
        this.assetId = assetId;
    }

    public NotificationsPlace() {
        this.realm = null;
        this.assetId = null;
    }

    public String getRealm() {
        return realm;
    }

    public String getAssetId() {
        return assetId;
    }

    @Prefix("notifications")
    public static class Tokenizer implements PlaceTokenizer<NotificationsPlace> {

        @Override
        public NotificationsPlace getPlace(String token) {
            if (TextUtil.isNullOrEmpty(token)) {
                return new NotificationsPlace();
            }

            String[] tokenArr = token.split(":");

            if (tokenArr.length < 1 || tokenArr.length > 2 || TextUtil.isNullOrEmpty(tokenArr[0])) {
                return new NotificationsPlace();
            }

            String realm = tokenArr[0];
            String assetId = tokenArr.length == 2 ? tokenArr[1] : null;

            return new NotificationsPlace(realm, assetId);
        }

        @Override
        public String getToken(NotificationsPlace place) {
            StringBuilder sb = new StringBuilder();
            if (!TextUtil.isNullOrEmpty(place.realm)) {
                sb.append(place.realm);
            }

            if (!TextUtil.isNullOrEmpty(place.assetId)) {
                sb.append(":");
                sb.append(place.assetId);
            }

            return sb.toString();
        }
    }
}
