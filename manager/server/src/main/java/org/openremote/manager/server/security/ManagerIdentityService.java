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
package org.openremote.manager.server.security;

import org.openremote.container.Container;
import org.openremote.container.persistence.PersistenceService;
import org.openremote.container.security.IdentityService;
import org.openremote.container.timer.TimerService;
import org.openremote.container.web.WebService;

import java.util.Locale;
import java.util.logging.Logger;

import static org.openremote.container.util.MapAccess.getString;

public class ManagerIdentityService extends IdentityService {

    private static final Logger LOG = Logger.getLogger(ManagerIdentityService.class.getName());

    public static final String MANAGER_IDENTITY_PROVIDER = "MANAGER_IDENTITY_PROVIDER";
    public static final String MANAGER_IDENTITY_PROVIDER_DEFAULT = "keycloak";

    protected PersistenceService persistenceService;
    protected ManagerIdentityProvider identityProvider;

    @Override
    public void init(Container container) throws Exception {
        super.init(container);

        this.persistenceService = container.getService(PersistenceService.class);

        container.getService(WebService.class).getApiSingletons().add(
            new TenantResourceImpl(container.getService(TimerService.class), this)
        );
        container.getService(WebService.class).getApiSingletons().add(
            new UserResourceImpl(container.getService(TimerService.class), this)
        );

        String identityProviderType = getString(container.getConfig(), MANAGER_IDENTITY_PROVIDER, MANAGER_IDENTITY_PROVIDER_DEFAULT);
        switch (identityProviderType.toLowerCase(Locale.ROOT)) {
            case "keycloak":
                LOG.info("Enabling Keycloak identity provider");
                this.identityProvider = new ManagerKeycloakIdentityProvider(getExternalServerUri(), container);
                break;
            case "basic":
                LOG.info("Enabling basic identity provider");
                this.identityProvider = new ManagerBasicIdentityProvider(container);
                break;
            default:
                throw new UnsupportedOperationException("Unknown identity provider: " + identityProviderType);
        }
    }

    @Override
    public ManagerIdentityProvider getIdentityProvider() {
        return identityProvider;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
            "identityProvider=" + identityProvider +
            '}';
    }
}
