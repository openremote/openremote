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
package org.openremote.app.client.admin.users.edit;

import com.google.gwt.place.shared.PlaceTokenizer;
import com.google.gwt.place.shared.Prefix;
import org.openremote.app.client.admin.users.AbstractAdminUsersPlace;

public class AdminUserEditPlace extends AbstractAdminUsersPlace {

    public AdminUserEditPlace(AbstractAdminUsersPlace place) {
        this(place.getRealm(), place.getUserId());
    }

    public AdminUserEditPlace(String realm) {
        super(realm);
    }

    public AdminUserEditPlace(String realm, String userId) {
        super(realm, userId);
    }

    @Prefix("adminUserEdit")
    public static class Tokenizer implements PlaceTokenizer<AdminUserEditPlace> {

        @Override
        public AdminUserEditPlace getPlace(String token) {
            if (token == null) {
                throw new IllegalArgumentException("Invalid empty token");
            }
            String[] fields = token.split(":");
            if (fields.length == 1) {
                return new AdminUserEditPlace(fields[0]);
            } else if (fields.length == 2) {
                return new AdminUserEditPlace(fields[0], fields[1]);
            } else {
                throw new IllegalArgumentException("Invalid token: " + token);
            }
        }

        @Override
        public String getToken(AdminUserEditPlace place) {
            if (place.getRealm() == null) {
                return "";
            }
            return place.getRealm() + ":" + (place.getUserId() != null ? place.getUserId() : "");
        }
    }

}
