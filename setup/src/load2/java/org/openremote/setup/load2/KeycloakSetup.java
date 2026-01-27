/*
 * Copyright 2025, OpenRemote Inc.
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
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package org.openremote.setup.load2;

import static org.openremote.model.Constants.KEYCLOAK_CLIENT_ID;
import static org.openremote.model.Constants.MASTER_REALM;
import static org.openremote.model.Constants.RESTRICTED_USER_REALM_ROLE;
import static org.openremote.model.util.MapAccess.getInteger;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import org.keycloak.representations.idm.ClientRepresentation;
import org.openremote.manager.setup.AbstractKeycloakSetup;
import org.openremote.model.Constants;
import org.openremote.model.Container;
import org.openremote.model.security.ClientRole;
import org.openremote.model.security.User;

public class KeycloakSetup extends AbstractKeycloakSetup {
  private static final ClientRole[] CLIENT_ROLES =
      new ClientRole[] {ClientRole.READ_ASSETS, ClientRole.WRITE_ATTRIBUTES};

  /** How many accounts to provision */
  public static final String OR_SETUP_USERS = "OR_SETUP_USERS";

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
      ClientRepresentation clientRepresentation =
          keycloakProvider.getClient(MASTER_REALM, KEYCLOAK_CLIENT_ID);
      clientRepresentation.setDirectAccessGrantsEnabled(true);
      keycloakProvider.createUpdateClient(MASTER_REALM, clientRepresentation);
    }

    int accounts = getInteger(container.getConfig(), OR_SETUP_USERS, 0);
    AtomicInteger createdUsers = new AtomicInteger(0);

    if (accounts > 0) {
      IntStream.rangeClosed(1, accounts)
          .forEach(
              i ->
                  executor.submit(
                      () -> {
                        createUser(
                            Constants.MASTER_REALM,
                            "user" + i,
                            "user" + i,
                            "User " + i,
                            "",
                            "user" + i + "@openremote.local",
                            true,
                            REGULAR_USER_ROLES);
                        createdUsers.incrementAndGet();
                      }));
      IntStream.rangeClosed(1, accounts)
          .forEach(
              i ->
                  executor.submit(
                      () -> {
                        User user =
                            createUser(
                                Constants.MASTER_REALM,
                                User.SERVICE_ACCOUNT_PREFIX + "serviceuser" + i,
                                "serviceuser" + i,
                                null,
                                null,
                                null,
                                true,
                                CLIENT_ROLES);
                        keycloakProvider.updateUserRealmRoles(
                            MASTER_REALM, user.getId(), RESTRICTED_USER_REALM_ROLE);
                        createdUsers.incrementAndGet();
                      }));
    }

    // Wait until all users created
    int waitCounter = 0;
    while (createdUsers.get() < 2 * accounts) {
      if (waitCounter > 1000) {
        throw new IllegalStateException("Failed to add all requested user in the specified time");
      }
      waitCounter++;
      Thread.sleep(1000);
    }
  }
}
