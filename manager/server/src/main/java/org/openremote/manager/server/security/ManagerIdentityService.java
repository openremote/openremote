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
package org.openremote.manager.server.security;

import org.keycloak.admin.client.resource.RealmsResource;
import org.openremote.container.Container;
import org.openremote.container.security.IdentityService;
import org.openremote.container.security.KeycloakResource;
import org.openremote.container.web.WebService;
import org.openremote.manager.shared.Constants;
import org.openremote.manager.shared.http.RequestParams;

public class ManagerIdentityService extends IdentityService {

    @Override
    public void init(Container container) throws Exception {
        setClientId(Constants.MANAGER_CLIENT_ID);
        super.init(container);
        setKeycloakReverseProxy(true);
    }

    @Override
    public void configure(Container container) throws Exception {
        super.configure(container);

        container.getService(WebService.class).getApiSingletons().add(
            new TenantResourceImpl(this)
        );
    }

    /**
     * Pass access token from external request to Keycloak request, emulate a reverse proxy.
     */
    public KeycloakResource getKeycloak(RequestParams requestParams) {
        return getKeycloak(requestParams.getBearerAuth(), true);
    }

    /**
     * Pass access token from external request to Keycloak request, emulate a reverse proxy.
     */
    public RealmsResource getRealms(RequestParams requestParams) {
        return getRealms(requestParams.getBearerAuth(), true);
    }
}
