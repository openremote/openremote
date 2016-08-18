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

import org.openremote.container.Container;
import org.openremote.container.ContainerService;

import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

public class PersistenceService implements ContainerService {

    private static final Logger LOG = Logger.getLogger(PersistenceService.class.getName());

    public static final String PERSISTENCE_UNIT_NAME = "PERSISTENCE_UNIT_NAME";
    public static final String PERSISTENCE_UNIT_NAME_DEFAULT = "OpenRemotePU";
    public static final String DATABASE_PRODUCT = "DATABASE_PRODUCT";
    public static final String DATABASE_PRODUCT_DEFAULT = Database.Product.MYSQL.name();
    public static final String DATABASE_CONNECTION_URL = "DATABASE_CONNECTION_URL";
    public static final String DATABASE_CONNECTION_URL_DEFAULT = "jdbc:mysql://192.168.99.100:8082/openremote?useSSL=false";
    public static final String DATABASE_USERNAME = "DATABASE_USERNAME";
    public static final String DATABASE_USERNAME_DEFAULT = "openremote";
    public static final String DATABASE_PASSWORD = "DATABASE_PASSWORD";
    public static final String DATABASE_PASSWORD_DEFAULT = "secret";
    public static final String DATABASE_MIN_POOL_SIZE = "DATABASE_MIN_POOL_SIZE";
    public static final int DATABASE_MIN_POOL_SIZE_DEFAULT = 5;
    public static final String DATABASE_MAX_POOL_SIZE = "DATABASE_MAX_POOL_SIZE";
    public static final int DATABASE_MAX_POOL_SIZE_DEFAULT = 20;
    public static final String DATABASE_CREATE_SCHEMA = "DATABASE_CREATE_SCHEMA";
    public static final boolean DATABASE_CREATE_SCHEMA_DEFAULT = false;

    protected Database database;
    protected String persistenceUnitName;
    protected Map<String, Object> persistenceUnitProperties;
    protected EntityManagerFactory entityManagerFactory;

    @Override
    public void init(Container container) throws Exception {
        String databaseProduct = container.getConfig(DATABASE_PRODUCT, DATABASE_PRODUCT_DEFAULT);
        LOG.info("Preparing persistence service for database: " + databaseProduct);
        database = Database.Product.valueOf(databaseProduct);
    }

    @Override
    public void configure(Container container) throws Exception {
        String connectionUrl = container.getConfig(DATABASE_CONNECTION_URL, DATABASE_CONNECTION_URL_DEFAULT);
        LOG.info("Using database connection URL: " + connectionUrl);

        String databaseUsername = container.getConfig(DATABASE_USERNAME, DATABASE_USERNAME_DEFAULT);
        String databasePassword = container.getConfig(DATABASE_PASSWORD, DATABASE_PASSWORD_DEFAULT);
        int databaseMinPoolSize = container.getConfigInteger(DATABASE_MIN_POOL_SIZE, DATABASE_MIN_POOL_SIZE_DEFAULT);
        int databaseMaxPoolSize = container.getConfigInteger(DATABASE_MAX_POOL_SIZE, DATABASE_MAX_POOL_SIZE_DEFAULT);
        persistenceUnitProperties =
            database.open(connectionUrl, databaseUsername, databasePassword, databaseMinPoolSize, databaseMaxPoolSize);

        persistenceUnitName = container.getConfig(PERSISTENCE_UNIT_NAME, PERSISTENCE_UNIT_NAME_DEFAULT);
    }

    @Override
    public void start(Container container) throws Exception {
        this.entityManagerFactory =
            Persistence.createEntityManagerFactory(persistenceUnitName, persistenceUnitProperties);

        if (container.isDevMode()
            || container.getConfigBoolean(DATABASE_CREATE_SCHEMA, DATABASE_CREATE_SCHEMA_DEFAULT)) {
            LOG.info("Dropping and re-creating database schema");
            dropSchema();
            createSchema();
        }
    }

    @Override
    public void stop(Container container) throws Exception {
        LOG.info("Stopping persistence service...");
        if (entityManagerFactory != null) {
            entityManagerFactory.close();
        }
        if (database != null) {
            database.close();
        }
    }
    
    public EntityManagerFactory getEntityManagerFactory() {
        return entityManagerFactory;
    }

    public void createSchema() {
        generateSchema("create");
    }

    public void dropSchema() {
        generateSchema("drop");
    }

    protected void generateSchema(String action) {
        // Take exiting EMF properties, override the schema generation setting on a copy
        Map<String, Object> createSchemaProperties = new HashMap<>(persistenceUnitProperties);
        createSchemaProperties.put(
            "javax.persistence.schema-generation.database.action",
            action
        );
        Persistence.generateSchema(persistenceUnitName, createSchemaProperties);
    }
}
