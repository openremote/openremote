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
package org.openremote.manager.mqtt;

import io.moquette.broker.security.IAuthenticator;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.ClientsResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.representations.idm.ClientRepresentation;
import org.openremote.container.web.ClientRequestInfo;
import org.openremote.manager.security.ManagerIdentityProvider;
import org.openremote.manager.security.ManagerKeycloakIdentityProvider;

import javax.ws.rs.NotFoundException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.logging.Logger;

public class KeycloakAuthenticator implements IAuthenticator {

    private static final Logger LOG = Logger.getLogger(KeycloakAuthenticator.class.getName());

    public static final String MQTT_CLIENT_ID_SEPARATOR = "_";

    final ManagerKeycloakIdentityProvider identityProvider;

    public KeycloakAuthenticator(ManagerKeycloakIdentityProvider managerIdentityProvider) {
        identityProvider = managerIdentityProvider;
    }

    @Override
    public boolean checkValid(String realm, String clientId, byte[] password) {
        int indexSplit = realm.indexOf(MQTT_CLIENT_ID_SEPARATOR);
        if (indexSplit > 0) {
            realm = realm.substring(0, indexSplit);
        }

        RealmResource realmResource = identityProvider.getRealms(getClientRequestInfo()).realm(realm);
        if (realmResource == null) {
            LOG.info("Realm not found");
            return false;
        }

        try {
            ClientResource clientResource = realmResource.clients().get(clientId);
            String suppliedClientSecret = new String(password, StandardCharsets.UTF_8);
            String clientSecret = clientResource.getSecret().getValue();

            return suppliedClientSecret.equals(clientSecret);
        } catch (NotFoundException ex) {
            LOG.info("Client not found");
        }
        return false;
    }

    private ClientRequestInfo getClientRequestInfo() {
        String accessToken = identityProvider.getAdminAccessToken(null);
        return new ClientRequestInfo(null, accessToken);
    }
}
