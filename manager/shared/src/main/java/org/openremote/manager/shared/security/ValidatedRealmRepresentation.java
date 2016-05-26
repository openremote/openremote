package org.openremote.manager.shared.security;

import org.keycloak.representations.idm.RealmRepresentation;

import javax.validation.constraints.NotNull;

public class ValidatedRealmRepresentation extends RealmRepresentation {

    @NotNull
    @Override
    public String getRealm() {
        return super.getRealm();
    }

    @Override
    public void setRealm(String realm) {
        super.setRealm(realm);
    }
}
