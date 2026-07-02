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

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.sql.Statement;

/**
 * Flyway migration that adds the {@code read:services} and {@code write:services} client roles for every
 * {@code openremote} client. The roles are inserted straight into the Keycloak {@code keycloak_role} table rather
 * than via the Keycloak admin API, so the migration doesn't depend on Keycloak being reachable or on admin
 * credentials, and runs transactionally.
 * <p>
 * This must stay a Java migration and not be converted to a {@code .sql} file. Flyway records the applied type
 * (JDBC) and a null checksum for {@link BaseJavaMigration}s; a {@code .sql} file for the same version is type SQL
 * with a real checksum, so converting it would fail validation with a checksum/type mismatch on any database that
 * already applied this (released) version. The body may still be edited freely - Java migrations have no checksum,
 * so changing the SQL below doesn't affect already-migrated databases.
 */
public class V20250916_01__AddServiceRoles extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        String sql = """
            DO $$
            DECLARE
                v_client RECORD;
            BEGIN
                FOR v_client IN
                    SELECT id, realm_id
                    FROM client
                    WHERE client_id = 'openremote'
                LOOP
                    IF NOT EXISTS (
                        SELECT 1 FROM keycloak_role
                        WHERE name = 'read:services' AND client = v_client.id
                    ) THEN
                        INSERT INTO keycloak_role (id, client_realm_constraint, client_role, description, name, realm_id, client)
                        VALUES (gen_random_uuid()::varchar(36), v_client.id, true, 'View services', 'read:services', v_client.realm_id, v_client.id);
                    END IF;

                    IF NOT EXISTS (
                        SELECT 1 FROM keycloak_role
                        WHERE name = 'write:services' AND client = v_client.id
                    ) THEN
                        INSERT INTO keycloak_role (id, client_realm_constraint, client_role, description, name, realm_id, client)
                        VALUES (gen_random_uuid()::varchar(36), v_client.id, true, 'Write service data', 'write:services', v_client.realm_id, v_client.id);
                    END IF;
                END LOOP;
            END $$;
            """;

        try (Statement statement = context.getConnection().createStatement()) {
            statement.execute(sql);
        }
    }
}
