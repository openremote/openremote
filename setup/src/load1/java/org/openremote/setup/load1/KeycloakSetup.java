/*
 * Copyright 2022, OpenRemote Inc.
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
package org.openremote.setup.load1;

import org.keycloak.representations.idm.ClientRepresentation;
import org.openremote.manager.setup.AbstractKeycloakSetup;
import org.openremote.model.Constants;
import org.openremote.model.Container;
import org.openremote.model.security.User;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import static org.openremote.model.util.MapAccess.getInteger;
import static org.openremote.model.Constants.KEYCLOAK_CLIENT_ID;
import static org.openremote.model.Constants.MASTER_REALM;

public class KeycloakSetup extends AbstractKeycloakSetup {

    /**
     * How many regular users to provision
     */
    public static final String OR_SETUP_REGULAR_USERS = "OR_SETUP_REGULAR_USERS";

    /**
     * How many service users to provision
     */
    public static final String OR_SETUP_SERVICE_USERS = "OR_SETUP_SERVICE_USERS";
    public List<User> users;
    protected ExecutorService executor;

    public KeycloakSetup(Container container, ExecutorService executor) {
        super(container);
        this.executor = executor;
    }

    @Override
    public void onStart() throws Exception {

        if (!container.isDevMode()) {
            // Re-enable direct access login
            ClientRepresentation clientRepresentation = keycloakProvider.getClient(MASTER_REALM, KEYCLOAK_CLIENT_ID);
            clientRepresentation.setDirectAccessGrantsEnabled(true);
            keycloakProvider.createUpdateClient(
                MASTER_REALM,
                clientRepresentation
            );
        }

        int regularUsers = getInteger(container.getConfig(), OR_SETUP_REGULAR_USERS, 0);
        int serviceUsers = getInteger(container.getConfig(), OR_SETUP_SERVICE_USERS, 0);
        AtomicInteger createdUsers = new AtomicInteger(0);

        if (regularUsers > 0) {
            IntStream.rangeClosed(1, regularUsers).forEach(i ->
                executor.submit(() -> {
                    createUser(Constants.MASTER_REALM, "user" + i, "user" + i, "User " + i, "", "user" + i + "@openremote.local", true, REGULAR_USER_ROLES);
                    createdUsers.incrementAndGet();
                })
            );
        }
        if (serviceUsers > 0) {
            IntStream.rangeClosed(1, regularUsers).forEach(i ->
                executor.submit(() -> {
                    createUser(Constants.MASTER_REALM, User.SERVICE_ACCOUNT_PREFIX + "serviceuser" + i, null, null, null, null, true, REGULAR_USER_ROLES);
                    createdUsers.incrementAndGet();
                })
            );
        }

        // Wait until all users created
        int waitCounter = 0;
        while (createdUsers.get() < regularUsers+serviceUsers) {
            if (waitCounter > 1000) {
                throw new IllegalStateException("Failed to add all requested user in the specified time");
            }
            waitCounter++;
            Thread.sleep(1000);
        }
    }
}
