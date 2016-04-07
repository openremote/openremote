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
