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
package org.openremote.manager.setup.database;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.sql.Connection;
import java.sql.PreparedStatement;

/**
 * TODO An example in the right place
 */
public class Disabled_V20171031_03__Migration { //extends BaseJavaMigration {

    // @Override
    public void migrate(Context context) throws Exception {
        /* TODO: Enable to migrate beyond 3.0.0 when SQL files are not good enough
        try (PreparedStatement statement = context.getConnection().prepareStatement(
            "alter table openremote.ASSET add column TEST_COLUMN varchar(50)"
        )) {
            statement.execute();
        }
        */
    }
}
