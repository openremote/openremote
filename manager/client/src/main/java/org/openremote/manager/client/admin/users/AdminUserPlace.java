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
package org.openremote.manager.client.admin.users;

import com.google.gwt.place.shared.PlaceTokenizer;
import com.google.gwt.place.shared.Prefix;
import org.openremote.manager.client.admin.AdminPlace;

public class AdminUserPlace extends AdminPlace {

    final String realm;
    final String userId;

    public AdminUserPlace(String realm) {
        this.realm = realm;
        this.userId = null;
    }

    public AdminUserPlace(String realm, String userId) {
        this.realm = realm;
        this.userId = userId;
    }

    public String getRealm() {
        return realm;
    }

    public String getUserId() {
        return userId;
    }

    @Prefix("user")
    public static class Tokenizer implements PlaceTokenizer<AdminUserPlace> {

        @Override
        public AdminUserPlace getPlace(String token) {
            if (token == null) {
                throw new IllegalArgumentException("Invalid empty token");
            }
            String[] fields = token.split(":");
            if (fields.length == 1) {
                return new AdminUserPlace(fields[0]);
            } else if (fields.length == 2) {
                return new AdminUserPlace(fields[0], fields[1]);
            } else {
                throw new IllegalArgumentException("Invalid token: " + token);
            }
        }

        @Override
        public String getToken(AdminUserPlace place) {
            if (place.getRealm() == null) {
                return "";
            }
            return place.getRealm() + ":" + (place.getUserId() != null ? place.getUserId() : "");
        }
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
            "realm='" + realm + '\'' +
            ", userId='" + userId + '\'' +
            "}";
    }
}
