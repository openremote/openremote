package org.openremote.container.security;

import org.keycloak.adapters.KeycloakDeployment;

public class ClientRealm {

    public static class Key {
        public final String realm;
        public final String clientId;

        public Key(String realm, String clientId) {
            this.realm = realm;
            this.clientId = clientId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Key that = (Key) o;

            if (!realm.equals(that.realm)) return false;
            return clientId.equals(that.clientId);

        }

        @Override
        public int hashCode() {
            int result = realm.hashCode();
            result = 31 * result + clientId.hashCode();
            return result;
        }
    }

    final public ClientInstall clientInstall;
    final public KeycloakDeployment keycloakDeployment;

    public ClientRealm(ClientInstall clientInstall, KeycloakDeployment keycloakDeployment) {
        this.clientInstall = clientInstall;
        this.keycloakDeployment = keycloakDeployment;
    }

}
