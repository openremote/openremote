package org.openremote.manager.shared.security;

import org.keycloak.representations.idm.RoleRepresentation;

import java.util.ArrayList;
import java.util.List;

/**
 * Roles available for our client application on Keycloak.
 */
public enum ClientRole {

    READ_ADMIN("read:admin", "Read system settings, tenants, and users"),
    READ_CONSOLE("read:console", "Download console frontend resources"),
    READ_MAP("read:map", "View map"),
    READ_ASSETS("read:assets", "Read asset data"),

    WRITE_ADMIN("write:admin", "Write system settings, tenants, and users"),
    WRITE_ASSETS("write:assets", "Write asset data"),

    READ("read", "Read all data", new ClientRole[]{
        READ_ADMIN,
        READ_CONSOLE,
        READ_MAP,
        READ_ASSETS
    }),

    WRITE("write", "Write all data", new ClientRole[]{
        WRITE_ADMIN,
        WRITE_ASSETS
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
