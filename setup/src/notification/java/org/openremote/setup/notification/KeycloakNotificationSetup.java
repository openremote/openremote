/*
 * Copyright 2026, OpenRemote Inc.
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
package org.openremote.setup.notification;

import static org.openremote.model.Constants.MASTER_REALM;
import static org.openremote.model.Constants.RESTRICTED_USER_REALM_ROLE;

import java.util.logging.Logger;
import org.openremote.manager.setup.AbstractKeycloakSetup;
import org.openremote.model.Container;
import org.openremote.model.security.ClientRole;
import org.openremote.model.security.Realm;
import org.openremote.model.security.User;

/**
 * Creates test users covering each notification RBAC requirement:
 *
 * <ul>
 *   <li><b>read</b> — read:notifications only; sees only own notifications, no name resolution
 *   <li><b>assets</b> — read:notifications + read:assets; asset names resolved in the table
 *   <li><b>users</b> — read:notifications + read:users; user names resolved, users recipient option
 *       shown
 *   <li><b>view-users</b> — read:notifications + read:users + read:assets; full user/asset
 *       resolution
 *   <li><b>write</b> — write:notifications + write:notifications; can send and view own
 *       notifications
 *   <li><b>restricted</b> — write:notifications + restricted realm role; no send notification
 *       option
 *   <li><b>restricted</b> — write:notifications + restricted realm role; no user/realm recipient
 *       option in dialog
 * </ul>
 *
 * All users share the password {@code secret}.
 */
public class KeycloakNotificationSetup extends AbstractKeycloakSetup {

  private static final Logger LOG = Logger.getLogger(KeycloakNotificationSetup.class.getName());
  public static final String PASSWORD = "secret";
  public static final String SMARTCITY_REALM = "smartcity";

  public User notifRead;
  public User notifAssets;
  public User notifUsers;
  public User notifViewUsers;
  public User notifWrite;
  public User notifRestricted;
  public User notifRestrictedAssets;
  public Realm realmSmartCity;
  public User smartCityUser;

  public KeycloakNotificationSetup(Container container) {
    super(container);
  }

  @Override
  public void onStart() throws Exception {
    super.onStart();

    // Sees only own notifications; no asset/user name resolution
    notifRead =
        createUser(
            MASTER_REALM,
            "read",
            PASSWORD,
            "Notification",
            "Read",
            "read@openremote.local",
            true,
            new ClientRole[] {ClientRole.READ_NOTIFICATIONS});

    // Sees only own notifications; asset names resolved in the table
    notifAssets =
        createUser(
            MASTER_REALM,
            "assets",
            PASSWORD,
            "Notification",
            "Assets",
            "assets@openremote.local",
            true,
            new ClientRole[] {ClientRole.READ_NOTIFICATIONS, ClientRole.READ_ASSETS});

    // Sees only own notifications; user names resolved and users recipient option shown in dialog
    notifUsers =
        createUser(
            MASTER_REALM,
            "users",
            PASSWORD,
            "Notification",
            "Users",
            "users@openremote.local",
            true,
            new ClientRole[] {ClientRole.READ_NOTIFICATIONS, ClientRole.READ_USERS});

    // Sees only own notifications; user/asset names resolved, users recipient option shown;
    // read:admin needed for users page
    notifViewUsers =
        createUser(
            MASTER_REALM,
            "view-users",
            PASSWORD,
            "Notification",
            "ViewUsers",
            "view-users@openremote.local",
            true,
            new ClientRole[] {
              ClientRole.READ_NOTIFICATIONS, ClientRole.READ_USERS, ClientRole.READ_ASSETS,
              // ClientRole.READ_ADMIN
            });

    // Can send notifications and view own notifications
    notifWrite =
        createUser(
            MASTER_REALM,
            "write",
            PASSWORD,
            "Notification",
            "Write",
            "write@openremote.local",
            true,
            new ClientRole[] {ClientRole.READ_NOTIFICATIONS, ClientRole.WRITE_NOTIFICATIONS});

    // Restricted: no access to sending notifications (even though has write:notifications)
    notifRestricted =
        createUser(
            MASTER_REALM,
            "restricted",
            PASSWORD,
            "Notification",
            "Restricted",
            "restricted@openremote.local",
            true,
            new ClientRole[] {ClientRole.READ_NOTIFICATIONS, ClientRole.WRITE_NOTIFICATIONS});
    identityService
        .getIdentityProvider()
        .updateUserRealmRoles(
            MASTER_REALM,
            notifRestricted.getId(),
            identityService
                .getIdentityProvider()
                .addUserRealmRoles(
                    MASTER_REALM, notifRestricted.getId(), RESTRICTED_USER_REALM_ROLE));

    // Restricted Assets: no user or realm recipient option in the send dialog (only assets)
    notifRestrictedAssets =
        createUser(
            MASTER_REALM,
            "restricted-assets",
            PASSWORD,
            "Notification",
            "RestrictedAssets",
            "restricted-assets@openremote.local",
            true,
            new ClientRole[] {
              ClientRole.READ_NOTIFICATIONS, ClientRole.WRITE_NOTIFICATIONS, ClientRole.READ_ASSETS
            });
    identityService
        .getIdentityProvider()
        .updateUserRealmRoles(
            MASTER_REALM,
            notifRestrictedAssets.getId(),
            identityService
                .getIdentityProvider()
                .addUserRealmRoles(
                    MASTER_REALM, notifRestrictedAssets.getId(), RESTRICTED_USER_REALM_ROLE));

    realmSmartCity = createRealm(SMARTCITY_REALM, "Smart City", true);
    smartCityUser =
        createUser(
            SMARTCITY_REALM,
            "smartcity",
            PASSWORD,
            "Smart",
            "City",
            "smartcity@openremote.local",
            true,
            new ClientRole[] {
              ClientRole.READ_NOTIFICATIONS,
              ClientRole.WRITE_NOTIFICATIONS,
              ClientRole.READ_USERS,
              ClientRole.WRITE_USER,
              ClientRole.READ_ASSETS,
              ClientRole.WRITE_ASSETS
            });

    LOG.info("Notification RBAC test users created (password: " + PASSWORD + ")");
  }
}
