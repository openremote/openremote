/*
 * Copyright 2016, OpenRemote Inc.
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
package org.openremote.manager.setup;

import static org.openremote.container.security.keycloak.KeycloakIdentityProvider.DEFAULT_CLIENTS;
import static org.openremote.model.Constants.*;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.logging.Logger;
import org.openremote.manager.asset.AssetStorageService;
import org.openremote.model.Container;
import org.openremote.model.asset.Asset;
import org.openremote.model.query.AssetQuery;
import org.openremote.model.query.UserQuery;
import org.openremote.model.query.filter.RealmPredicate;

public class KeycloakCleanSetup extends AbstractKeycloakSetup {

  private static final Logger LOG = Logger.getLogger(KeycloakCleanSetup.class.getName());
  private final AssetStorageService assetStorageService;

  public KeycloakCleanSetup(Container container) {
    super(container);
    this.assetStorageService = container.getService(AssetStorageService.class);
  }

  @Override
  public void onStart() throws Exception {
    super.onStart();
    doClean();
  }

  protected void doClean() throws Exception {
    // Delete all realms that are not the master realm
    LOG.info("Deleting all non-master realms");
    Arrays.stream(keycloakProvider.getRealms())
        .forEach(
            realm -> {
              if (!realm.getName().equals(MASTER_REALM)) {
                deleteRealmAssets(realm.getName());
                keycloakProvider.deleteRealm(realm.getName());
              }
            });

    LOG.info("Deleting all non-master admin users");
    Arrays.stream(
            keycloakProvider.queryUsers(
                // Exclude service accounts (client deletion will remove the user)
                new UserQuery().realm(new RealmPredicate(MASTER_REALM)).serviceUsers(false)))
        .forEach(
            user -> {
              if (!user.getUsername().equals(MASTER_REALM_ADMIN_USER)
                  && !user.getUsername().equals(MANAGER_CLIENT_ID)) {
                LOG.info("Deleting user: " + user);
                keycloakProvider.deleteUser(MASTER_REALM, user.getId());
              }
            });

    // Delete all non built in clients
    LOG.info("Deleting all non default clients");
    Arrays.stream(keycloakProvider.getClients(MASTER_REALM))
        .forEach(
            client -> {
              if (!DEFAULT_CLIENTS.contains(client.getClientId())) {
                LOG.info("Deleting client: " + client.getClientId());
                keycloakProvider.deleteClient(MASTER_REALM, client.getClientId());
              }
            });

    // Delete IDPs
    keycloakProvider
        .getIdentityProviders(MASTER_REALM)
        .forEach(
            idp -> {
              LOG.info("Deleting IDP: " + idp.getAlias());
              keycloakProvider.deleteIdentityProvider(MASTER_REALM, idp.getAlias());
            });
  }

  protected void deleteRealmAssets(String realm) {
    List<String> assetIds =
        assetStorageService
            .findAll(
                new AssetQuery()
                    .select(new AssetQuery.Select().excludeAttributes())
                    .realm(new RealmPredicate(realm))
                    .includeDeletePending(true))
            .stream()
            .map(Asset::getId)
            .toList();

    if (assetIds.isEmpty()) {
      return;
    }

    LOG.info("Deleting all assets in realm '" + realm + "': count=" + assetIds.size());
    boolean deleted;
    try {
      deleted = assetStorageService.deleteUntilFinished(assetIds, true).get();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException(
          "Interrupted while deleting all assets in realm '" + realm + "'", e);
    } catch (ExecutionException e) {
      throw new IllegalStateException("Failed to delete all assets in realm '" + realm + "'", e);
    }

    if (!deleted) {
      throw new IllegalStateException("Failed to delete all assets in realm '" + realm + "'");
    }
  }
}
