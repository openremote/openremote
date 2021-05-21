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
import org.keycloak.representations.idm.ClientRepresentation;
import org.openremote.manager.security.ManagerKeycloakIdentityProvider;
import org.openremote.model.security.Tenant;
import org.openremote.model.security.User;
import org.openremote.model.syslog.SyslogCategory;
import org.openremote.model.util.TextUtil;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.logging.Logger;

import static org.openremote.model.syslog.SyslogCategory.API;

public class KeycloakAuthenticator implements IAuthenticator {

    private static final Logger LOG = SyslogCategory.getLogger(API, KeycloakAuthenticator.class);

    protected final ManagerKeycloakIdentityProvider identityProvider;
    protected final Map<String, MqttConnection> sessionIdConnectionMap;

    public KeycloakAuthenticator(ManagerKeycloakIdentityProvider managerIdentityProvider, Map<String, MqttConnection> sessionIdConnectionMap) {
        identityProvider = managerIdentityProvider;
        this.sessionIdConnectionMap = sessionIdConnectionMap;
    }

    @Override
    public boolean checkValid(String clientId, String username, byte[] password) {
        String[] realmAndClientId = username.split(":");
        String realm = realmAndClientId[0];
        username = realmAndClientId[1]; // This is OAuth clientId
        String suppliedClientSecret = new String(password, StandardCharsets.UTF_8);

        if (TextUtil.isNullOrEmpty(realm)
            || TextUtil.isNullOrEmpty(username)
            || TextUtil.isNullOrEmpty(suppliedClientSecret)) {
            LOG.warning("Realm, client ID and/or client secret missing");
            return false;
        }

        if (sessionIdConnectionMap.containsKey(clientId)) {
            LOG.warning("Client already connected");
            return false;
        }

        Tenant tenant = identityProvider.getTenant(realm);

        if (tenant == null || !tenant.getEnabled()) {
            LOG.warning("Realm not found or is inactive: " + realm);
            return false;
        }

        User user = identityProvider.getUserByUsername(realm, User.SERVICE_ACCOUNT_PREFIX + username);
        if (user == null || user.getEnabled() == null || !user.getEnabled() || TextUtil.isNullOrEmpty(user.getSecret())) {
            LOG.warning("User not found, disabled or doesn't support client credentials grant type");
            return false;
        }

        return suppliedClientSecret.equals(user.getSecret());
    }
}
