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
package org.openremote.manager.security;

import org.openremote.container.Container;
import org.openremote.container.persistence.PersistenceService;
import org.openremote.container.security.IdentityService;
import org.openremote.container.timer.TimerService;
import org.openremote.manager.web.ManagerWebService;

import javax.persistence.EntityManager;
import java.util.Locale;
import java.util.logging.Logger;

public class ManagerIdentityService extends IdentityService {

    private static final Logger LOG = Logger.getLogger(ManagerIdentityService.class.getName());

    protected ManagerIdentityProvider identityProvider;
    protected PersistenceService persistenceService;

    @Override
    public void init(Container container) throws Exception {
        super.init(container);
        persistenceService = container.getService(PersistenceService.class);

        container.getService(ManagerWebService.class).getApiSingletons().add(
            new TenantResourceImpl(container.getService(TimerService.class), this, container)
        );
        container.getService(ManagerWebService.class).getApiSingletons().add(
            new UserResourceImpl(container.getService(TimerService.class), this)
        );

    }

    public ManagerIdentityProvider getIdentityProvider() {
        return identityProvider;
    }

    @Override
    public ManagerIdentityProvider createIdentityProvider(Container container, String identityProviderType) {
        if (identityProvider == null) {
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
        return identityProvider;
    }

    public UserConfiguration getUserConfiguration(String userId) {
        return persistenceService.doReturningTransaction(entityManager -> getUserConfiguration(entityManager, userId));
    }

    protected UserConfiguration getUserConfiguration(EntityManager em, String userId) {
        UserConfiguration userConfiguration = em.find(UserConfiguration.class, userId);
        if (userConfiguration == null) {
            userConfiguration = new UserConfiguration(userId);
            userConfiguration = mergeUserConfiguration(em, userConfiguration);
        }
        return userConfiguration;
    }

    public UserConfiguration mergeUserConfiguration(UserConfiguration userConfiguration) {
        return persistenceService.doReturningTransaction(entityManager -> mergeUserConfiguration(entityManager, userConfiguration));
    }

    protected UserConfiguration mergeUserConfiguration(EntityManager em, UserConfiguration userConfiguration) {
        if (userConfiguration.getUserId() == null || userConfiguration.getUserId().length() == 0) {
            throw new IllegalArgumentException("User ID must be set on: " + userConfiguration);
        }
        return em.merge(userConfiguration);
    }


    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
            "identityProvider=" + identityProvider +
            '}';
    }
}
