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
package org.openremote.manager.setup.database;

import java.util.List;
import org.openremote.model.security.ClientRole;

/**
 * Flyway migration that adds the {@code read:tunnels} and {@code write:tunnels} client roles to the
 * {@code openremote} client of every realm and wires them into the existing {@code read}/{@code
 * write} composite roles. See {@link AbstractKeycloakRolesMigration} for the credential handling
 * and why this must stay a Java migration.
 *
 * <p>Users provisioned with individual roles rather than a composite do not gain these, so accounts
 * that used the tunnel endpoints without holding {@code read}/{@code write} need them assigned.
 */
public class V20260903_1000__AddTunnelRoles extends AbstractKeycloakRolesMigration {

  @Override
  protected List<ClientRole> getReadRoles() {
    return List.of(ClientRole.READ_TUNNELS);
  }

  @Override
  protected List<ClientRole> getWriteRoles() {
    return List.of(ClientRole.WRITE_TUNNELS);
  }
}
