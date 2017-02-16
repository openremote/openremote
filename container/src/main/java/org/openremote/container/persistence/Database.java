/*
 * Copyright 2016, OpenRemote Inc.
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
package org.openremote.container.persistence;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.spatial.dialect.postgis.PostgisDialect;

import java.util.HashMap;
import java.util.Map;

public interface Database {

    /**
     * @return Persistence unit properties you want to use for this database (e.g. Hibernate dialect already set), will be passed into {@link #open}.
     */
    Map<String, Object> createProperties();

    /**
     * Modify persistence properties (e.g. set datasource) used to call <code>Persistence.createEntityManagerFactory(persistenceUnitName, persistenceUnitProperties)</code>
     */
    void open(Map<String, Object> properties, String connectionUrl, String username, String password, int connectionTimeoutSeconds, int minIdle, int maxPoolSize);

    void close();

    enum Product implements Database {

        POSTGRES {
            protected HikariConfig hikariConfig;
            protected HikariDataSource hikariDataSource;

            @Override
            public Map<String, Object> createProperties() {
                Map<String, Object> properties = new HashMap<>();
                properties.put(AvailableSettings.DIALECT, PostgisDialect.class.getName());
                return properties;
            }

            @Override
            public void open(Map<String, Object> properties, String connectionUrl, String username, String password, int connectionTimeoutSeconds, int minIdle, int maxPoolSize) {
                hikariConfig = new HikariConfig();
                hikariConfig.setDataSourceClassName("org.postgresql.ds.PGSimpleDataSource");
                hikariConfig.addDataSourceProperty("url", connectionUrl);
                hikariConfig.setUsername(username);
                hikariConfig.setPassword(password);
                hikariConfig.setConnectionTimeout(connectionTimeoutSeconds * 1000);
                hikariConfig.setMinimumIdle(minIdle);
                hikariConfig.setMaximumPoolSize(maxPoolSize);

                hikariDataSource = new HikariDataSource(hikariConfig);

                properties.put(AvailableSettings.DATASOURCE, hikariDataSource);
            }

            @Override
            public void close() {
                if (hikariDataSource != null)
                    hikariDataSource.close();
                hikariConfig = null;
                hikariDataSource = null;
            }
        }
    }

}