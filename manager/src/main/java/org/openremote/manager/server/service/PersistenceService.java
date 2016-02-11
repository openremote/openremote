package org.openremote.manager.server.service;

import io.vertx.core.json.JsonObject;
import org.openremote.manager.server.jpa.Database;

import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import static org.openremote.manager.server.Constants.DEV_MODE;
import static org.openremote.manager.server.Constants.DEV_MODE_DEFAULT;

public class PersistenceService {

    private static final Logger LOG = Logger.getLogger(PersistenceService.class.getName());

    public static final String PERSISTENCE_UNIT_NAME = "PERSISTENCE_UNIT_NAME";
    public static final String PERSISTENCE_UNIT_NAME_DEFAULT = "ManagerPU";
    public static final String DATABASE_PRODUCT = "DATABASE_PRODUCT";
    public static final String DATABASE_PRODUCT_DEFAULT = "H2";
    public static final String DATABASE_CONNECTION_URL = "DATABASE_CONNECTION_URL";
    public static final String DATABASE_CONNECTION_URL_DEFAULT = "jdbc:h2:file:./or-manager-database";
    public static final String DATABASE_CONNECTION_URL_DEV_MODE = "jdbc:h2:mem:test";
    public static final String DATABASE_USERNAME = "DATABASE_USERNAME";
    public static final String DATABASE_USERNAME_DEFAULT = "sa";
    public static final String DATABASE_PASSWORD = "DATABASE_PASSWORD";
    public static final String DATABASE_PASSWORD_DEFAULT = "";
    public static final String DATABASE_MIN_POOL_SIZE = "DATABASE_MIN_POOL_SIZE";
    public static final int DATABASE_MIN_POOL_SIZE_DEFAULT = 5;
    public static final String DATABASE_MAX_POOL_SIZE = "DATABASE_MAX_POOL_SIZE";
    public static final int DATABASE_MAX_POOL_SIZE_DEFAULT = 20;

    protected Database database;
    protected String persistenceUnitName;
    protected Map<String, Object> persistenceUnitProperties;
    protected EntityManagerFactory entityManagerFactory;

    public void start(JsonObject config) {
        boolean devMode = config.getBoolean(DEV_MODE, DEV_MODE_DEFAULT);
        String databaseProduct = config.getString(DATABASE_PRODUCT, DATABASE_PRODUCT_DEFAULT);

        LOG.info("Starting persistence service for database: " + databaseProduct);

        database = Database.Product.valueOf(databaseProduct);

        String connectionUrl = devMode
            ? DATABASE_CONNECTION_URL_DEV_MODE
            : config.getString(DATABASE_CONNECTION_URL, DATABASE_CONNECTION_URL_DEFAULT);
        LOG.info("Using database connection URL: " + connectionUrl);

        String databaseUsername = config.getString(DATABASE_USERNAME, DATABASE_USERNAME_DEFAULT);
        String databasePassword = config.getString(DATABASE_PASSWORD, DATABASE_PASSWORD_DEFAULT);
        int databaseMinPoolSize = config.getInteger(DATABASE_MIN_POOL_SIZE, DATABASE_MIN_POOL_SIZE_DEFAULT);
        int databaseMaxPoolSize = config.getInteger(DATABASE_MAX_POOL_SIZE, DATABASE_MAX_POOL_SIZE_DEFAULT);
        persistenceUnitProperties =
            database.open(connectionUrl, databaseUsername, databasePassword, databaseMinPoolSize, databaseMaxPoolSize);

        persistenceUnitName = config.getString(PERSISTENCE_UNIT_NAME, PERSISTENCE_UNIT_NAME_DEFAULT);
        this.entityManagerFactory =
            Persistence.createEntityManagerFactory(persistenceUnitName, persistenceUnitProperties);
    }

    public void stop() {
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
