/*
 * Copyright 2025, OpenRemote Inc.
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
package org.openremote.manager.setup.database;

import org.openremote.model.security.ClientRole;

import java.util.List;

/**
 * Flyway migration that adds the {@code read:services} and {@code write:services} client roles to the
 * {@code openremote} client of every realm and wires them into the existing {@code read}/{@code write} composite
 * roles. See {@link AbstractKeycloakRolesMigration} for the credential handling and why this must stay a Java
 * migration.
 */
public class V20250916_01__AddServiceRoles extends AbstractKeycloakRolesMigration {

    @Override
    protected List<ClientRole> getReadRoles() {
        return List.of(ClientRole.READ_SERVICES);
    }

    @Override
    protected List<ClientRole> getWriteRoles() {
        return List.of(ClientRole.WRITE_SERVICES);
    }
}
