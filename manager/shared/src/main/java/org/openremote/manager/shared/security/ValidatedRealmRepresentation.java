package org.openremote.manager.shared.security;

import org.keycloak.representations.idm.RealmRepresentation;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

public class ValidatedRealmRepresentation extends RealmRepresentation {

    @NotNull(message = "{RealmRepresentation.displayName.NotNull}")
    @Size(min = 3, max = 255, message = "{RealmRepresentation.displayName.Size}")
    @Override
    public String getDisplayName() {
        return super.getDisplayName();
    }

    @NotNull(message = "{RealmRepresentation.realm.NotNull}")
    @Size(min = 3, max = 255, message = "{RealmRepresentation.realm.Size}")
    @Override
    public String getRealm() {
        return super.getRealm();
    }
}
