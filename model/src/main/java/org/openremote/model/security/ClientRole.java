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
package org.openremote.model.security;

import com.fasterxml.jackson.annotation.JsonValue;
import org.keycloak.representations.idm.RoleRepresentation;
import org.openremote.model.Constants;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Roles available for our client application on Keycloak.
 */
public enum ClientRole {

    READ_ADMIN(Constants.READ_ADMIN_ROLE, "Read system settings, realms, and users"),
    READ_LOGS(Constants.READ_LOGS_ROLE, "Read logs and log settings"),
    READ_USERS(Constants.READ_USERS_ROLE, "Read limited set of user details for use in rules etc."),
    READ_MAP(Constants.READ_MAP_ROLE, "View map"),
    READ_ASSETS(Constants.READ_ASSETS_ROLE, "Read asset data"),
    READ_RULES(Constants.READ_RULES_ROLE, "Read rulesets"),

    WRITE_USER(Constants.WRITE_USER_ROLE, "Write data of the authenticated user"),
    WRITE_ADMIN(Constants.WRITE_ADMIN_ROLE, "Write system settings, realms, and users"),
    WRITE_LOGS(Constants.WRITE_LOGS_ROLE, "Write log settings"),
    WRITE_ASSETS(Constants.WRITE_ASSETS_ROLE, "Write asset data"),
    WRITE_ATTRIBUTES(Constants.WRITE_ATTRIBUTES_ROLE, "Write attribute data"),
    WRITE_RULES(Constants.WRITE_RULES_ROLE, "Write rulesets (NOTE: effectively super-user access!)"),

    READ("read", "Read all data", new ClientRole[]{
        READ_ADMIN,
        READ_LOGS,
        READ_USERS,
        READ_MAP,
        READ_ASSETS,
        READ_RULES
    }),

    WRITE("write", "Write all data", new ClientRole[]{
        READ_ADMIN,
        READ_LOGS,
        READ_USERS,
        READ_MAP,
        READ_ASSETS,
        READ_RULES,
        WRITE_USER,
        WRITE_ADMIN,
        WRITE_LOGS,
        WRITE_ASSETS,
        WRITE_ATTRIBUTES,
        WRITE_RULES
    });

    // Only individual roles, not composites
    public static final Set<String> ALL_ROLES = new HashSet<String>() {{
        for (ClientRole clientRole : values()) {
            if (clientRole.composites == null) {
                add(clientRole.value);
            }
        }
    }};

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

    @JsonValue
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
