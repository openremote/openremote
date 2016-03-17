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
    public static final String DATABASE_PRODUCT_DEFAULT = "H2";
    public static final String DATABASE_CONNECTION_URL = "DATABASE_CONNECTION_URL";
    public static final String DATABASE_CONNECTION_URL_DEFAULT = "jdbc:h2:mem:test";
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

    @Override
    public void prepare(Container container) {
        String databaseProduct = container.getConfig(DATABASE_PRODUCT, DATABASE_PRODUCT_DEFAULT);

        LOG.info("Preparing persistence service for database: " + databaseProduct);

        database = Database.Product.valueOf(databaseProduct);

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
    public void start(Container container) {
        this.entityManagerFactory =
            Persistence.createEntityManagerFactory(persistenceUnitName, persistenceUnitProperties);
    }

    @Override
    public void stop(Container container) {
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
