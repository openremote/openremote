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
package org.openremote.model.query.filter;

import org.openremote.model.query.BaseAssetQuery;

public class TenantPredicate {

    public String realmId;
    public String realm;

    public TenantPredicate() {
    }

    public TenantPredicate(String realmId) {
        this.realmId = realmId;
    }

    public TenantPredicate realmId(String id) {
        this.realmId = id;
        return this;
    }

    public TenantPredicate realm(String name) {
        this.realm = name;
        return this;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
            "realmId='" + realmId + '\'' +
            ", realm='" + realm + '\'' +
            '}';
    }
}
