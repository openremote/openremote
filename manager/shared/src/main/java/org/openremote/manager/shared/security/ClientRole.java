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
package org.openremote.manager.shared.security;

import org.keycloak.representations.idm.RoleRepresentation;

import java.util.ArrayList;
import java.util.List;

/**
 * Roles available for our client application on Keycloak.
 */
public enum ClientRole {

    READ_ADMIN("read:admin", "Read system settings, tenants, and users"),
    READ_MAP("read:map", "View map"),
    READ_ASSETS("read:assets", "Read asset data"),
    READ_RULES("read:rules", "Read rule definitions"),

    WRITE_ADMIN("write:admin", "Write system settings, tenants, and users"),
    WRITE_ASSETS("write:assets", "Write asset data"),
    WRITE_RULES("write:rules", "Write rule definitions"),

    READ("read", "Read all data", new ClientRole[]{
        READ_ADMIN,
        READ_MAP,
        READ_ASSETS,
        READ_RULES
    }),

    WRITE("write", "Write all data", new ClientRole[]{
        WRITE_ADMIN,
        WRITE_ASSETS,
        WRITE_RULES
    });

    final protected String value;
    final protected String description;
    final protected ClientRole[] composites;

    ClientRole(String value, String description, ClientRole[] composites) {
        this.value = value;
        this.description = description;
        this.composites = composites;
    }

    ClientRole(String value, String description) {
        this.value = value;
        this.description = description;
        this.composites = null;
    }

    public String getValue() {
        return value;
    }

    public String getDescription() {
        return description;
    }

    public ClientRole[] getComposites() {
        return composites;
    }

    public RoleRepresentation getRepresentation() {
        return new RoleRepresentation(getValue(), getDescription(), false);
    }

    static public String[] valueOf(ClientRole... roles) {
        List<String> list = new ArrayList<>();
        if (roles != null) {
            for (ClientRole role : roles) {
                list.add(role.getValue());
            }
        }
        return list.toArray(new String[list.size()]);
    }
}
