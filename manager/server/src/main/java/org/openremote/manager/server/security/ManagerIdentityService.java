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
import org.openremote.manager.shared.security.User;

public class ManagerIdentityService extends IdentityService {

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

        // TODO Configurable provider
        this.identityProvider = new ManagerKeycloakProvider(getExternalServerUri(), container);
    }

    @Override
    public ManagerIdentityProvider getIdentityProvider() {
        return identityProvider;
    }

    // TODO Should all User operations move to ManagerIdentityProvider?

    public boolean isRestrictedUser(String userId) {
        UserConfiguration userConfiguration = getUserConfiguration(userId);
        return userConfiguration.isRestricted();
    }

    public void setRestrictedUser(String userId, boolean restricted) {
        UserConfiguration userConfiguration = getUserConfiguration(userId);
        userConfiguration.setRestricted(restricted);
        mergeUserConfiguration(userConfiguration);
    }

    protected UserConfiguration getUserConfiguration(String userId) {
        UserConfiguration userConfiguration = persistenceService.doReturningTransaction(em -> em.find(UserConfiguration.class, userId));
        if (userConfiguration == null) {
            userConfiguration = new UserConfiguration(userId);
            userConfiguration = mergeUserConfiguration(userConfiguration);
        }
        return userConfiguration;
    }

    protected UserConfiguration mergeUserConfiguration(UserConfiguration userConfiguration) {
        if (userConfiguration.getUserId() == null || userConfiguration.getUserId().length() == 0) {
            throw new IllegalArgumentException("User ID must be set on: " + userConfiguration);
        }
        return persistenceService.doReturningTransaction(em -> em.merge(userConfiguration));
    }

    public boolean isUserInTenant(String userId, String realmId) {
        return persistenceService.doReturningTransaction(em -> {
            User user = em.find(User.class, userId);
            return (user != null&& realmId.equals(user.getRealmId()));
        });
    }


    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
            '}';
    }

}
