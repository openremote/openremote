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

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.hibernate.Session;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.openremote.container.Container;
import org.openremote.container.ContainerService;
import org.openremote.container.message.MessageBrokerService;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.Persistence;
import javax.ws.rs.core.UriBuilder;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.openremote.container.util.MapAccess.*;

/**
 * Uses the SQL database schema {@link #DEFAULT_SCHEMA_NAME} for all operations.
 */
public class PersistenceService implements ContainerService {

    private static final Logger LOG = Logger.getLogger(PersistenceService.class.getName());

    /**
     * We must use a different schema than Keycloak to store our tables, or FlywayDB
     * will wipe Keycloak tables. And since Keycloak doesn't call CREATE SCHEMA IF NOT EXISTS
     * on startup (Liquibase is not as good as FlywayDB? They both seem to suck... can't exclude
     * database artifacts from wipe/migrate in FlywayDB...) you must have it in the 'public'
     * schema. Hence we need a different schema here.
     */
    public static final String DEFAULT_SCHEMA_NAME = "openremote";
    public static final String SETUP_WIPE_CLEAN_INSTALL = "SETUP_WIPE_CLEAN_INSTALL";
    public static final String PERSISTENCE_UNIT_NAME = "PERSISTENCE_UNIT_NAME";
    public static final String PERSISTENCE_UNIT_NAME_DEFAULT = "OpenRemotePU";
    public static final String DATABASE_PRODUCT = "DATABASE_PRODUCT";
    public static final String DATABASE_PRODUCT_DEFAULT = Database.Product.POSTGRES.name();
    public static final String DATABASE_CONNECTION_URL = "DATABASE_CONNECTION_URL";
    public static final String DATABASE_CONNECTION_URL_DEFAULT = "jdbc:postgresql://localhost:5432/openremote";
    public static final String DATABASE_USERNAME = "DATABASE_USERNAME";
    public static final String DATABASE_USERNAME_DEFAULT = "postgres";
    public static final String DATABASE_PASSWORD = "DATABASE_PASSWORD";
    public static final String DATABASE_PASSWORD_DEFAULT = "CHANGE_ME_DB_USER_PASSWORD";
    public static final String DATABASE_MIN_POOL_SIZE = "DATABASE_MIN_POOL_SIZE";
    public static final int DATABASE_MIN_POOL_SIZE_DEFAULT = 5;
    public static final String DATABASE_MAX_POOL_SIZE = "DATABASE_MAX_POOL_SIZE";
    public static final int DATABASE_MAX_POOL_SIZE_DEFAULT = 20;
    public static final String DATABASE_CONNECTION_TIMEOUT_SECONDS = "DATABASE_CONNECTION_TIMEOUT_SECONDS";
    public static final int DATABASE_CONNECTION_TIMEOUT_SECONDS_DEFAULT = 5;
    public static final int PRIORITY = Integer.MIN_VALUE + 100;

    protected MessageBrokerService messageBrokerService;
    protected Database database;
    protected String persistenceUnitName;
    protected Map<String, Object> persistenceUnitProperties;
    protected EntityManagerFactory entityManagerFactory;

    protected Flyway flyway;
    protected boolean forceClean;
    protected Set<String> defaultSchemaLocations = new HashSet<>();
    protected Set<String> schemas = new HashSet<>();

    @Override
    public int getPriority() {
        return PRIORITY;
    }

    @Override
    public void init(Container container) throws Exception {
        this.messageBrokerService = container.hasService(MessageBrokerService.class)
            ? container.getService(MessageBrokerService.class)
            : null;

        String databaseProduct = getString(container.getConfig(), DATABASE_PRODUCT, DATABASE_PRODUCT_DEFAULT);
        LOG.info("Preparing persistence service for database: " + databaseProduct);
        database = Database.Product.valueOf(databaseProduct);

        persistenceUnitProperties = database.createProperties();

        if (messageBrokerService != null) {
            persistenceUnitProperties.put(
                org.hibernate.cfg.AvailableSettings.SESSION_SCOPED_INTERCEPTOR,
                PersistenceEventInterceptor.class.getName()
            );
        }

        persistenceUnitName = getString(container.getConfig(), PERSISTENCE_UNIT_NAME, PERSISTENCE_UNIT_NAME_DEFAULT);

        forceClean = getBoolean(container.getConfig(), SETUP_WIPE_CLEAN_INSTALL, container.isDevMode());

        openDatabase(container, database);

        this.entityManagerFactory =
            Persistence.createEntityManagerFactory(persistenceUnitName, persistenceUnitProperties);
    }


    @Override
    public void start(Container container) throws Exception {

    }

    @Override
    public void stop(Container container) throws Exception {
        if (entityManagerFactory != null) {
            entityManagerFactory.close();
        }
        if (database != null) {
            database.close();
        }
    }

    public boolean isSetupWipeCleanInstall() {
        return forceClean;
    }

    public EntityManager createEntityManager() {
        EntityManager entityManager = getEntityManagerFactory().createEntityManager();

        if (messageBrokerService != null) {
            // The persistence event interceptor is scoped to an EntityManager, so each new EM needs
            // access to the dependencies of the interceptor
            Session session = entityManager.unwrap(Session.class);
            PersistenceEventInterceptor persistenceEventInterceptor =
                (PersistenceEventInterceptor) ((SharedSessionContractImplementor) session).getInterceptor();
            persistenceEventInterceptor.setMessageBrokerService(messageBrokerService);
        }

        return entityManager;
    }

    public void doTransaction(Consumer<EntityManager> entityManagerConsumer) {
        doReturningTransaction(entityManager -> {
            entityManagerConsumer.accept(entityManager);
            return null;
        });
    }

    public <R> R doReturningTransaction(Function<EntityManager, R> entityManagerFunction) {
        EntityManager em = createEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            R result = entityManagerFunction.apply(em);
            tx.commit();
            return result;
        } catch (Exception ex) {
            if (tx != null && tx.isActive()) {
                try {
                    LOG.log(Level.FINE, "Rolling back failed transaction, cause follows", ex);
                    tx.rollback();
                } catch (RuntimeException rbEx) {
                    LOG.log(Level.SEVERE, "Rollback of transaction failed!", rbEx);
                }
            }
            throw ex;
        } finally {
            em.close();
        }
    }

    public EntityManagerFactory getEntityManagerFactory() {
        return entityManagerFactory;
    }

    public Set<String> getDefaultSchemaLocations() {
        return defaultSchemaLocations;
    }

    public Set<String> getSchemas() {
        return schemas;
    }

    protected void openDatabase(Container container, Database database) {
        String connectionUrl = getString(container.getConfig(), DATABASE_CONNECTION_URL, DATABASE_CONNECTION_URL_DEFAULT);
        connectionUrl = UriBuilder.fromUri(connectionUrl).replaceQueryParam("currentSchema", DEFAULT_SCHEMA_NAME).build().toString();
        String databaseUsername = getString(container.getConfig(), DATABASE_USERNAME, DATABASE_USERNAME_DEFAULT);
        String databasePassword = getString(container.getConfig(), DATABASE_PASSWORD, DATABASE_PASSWORD_DEFAULT);
        int databaseMinPoolSize = getInteger(container.getConfig(), DATABASE_MIN_POOL_SIZE, DATABASE_MIN_POOL_SIZE_DEFAULT);
        int databaseMaxPoolSize = getInteger(container.getConfig(), DATABASE_MAX_POOL_SIZE, DATABASE_MAX_POOL_SIZE_DEFAULT);
        int connectionTimeoutSeconds = getInteger(container.getConfig(), DATABASE_CONNECTION_TIMEOUT_SECONDS, DATABASE_CONNECTION_TIMEOUT_SECONDS_DEFAULT);
        LOG.info("Opening database connection: " + connectionUrl);
        database.open(persistenceUnitProperties, connectionUrl, databaseUsername, databasePassword, connectionTimeoutSeconds, databaseMinPoolSize, databaseMaxPoolSize);

        prepareSchemas(connectionUrl, databaseUsername, databasePassword);
    }

    protected void prepareSchemas(String connectionUrl, String databaseUsername, String databasePassword) {
        LOG.fine("Preparing database schema");
        List<String> locations = new ArrayList<>();
        List<String> schemas = new ArrayList<>();
        schemas.add(DEFAULT_SCHEMA_NAME);
        appendSchemas(schemas);
        appendSchemaLocations(locations);

        flyway = Flyway.configure()
            .dataSource(connectionUrl, databaseUsername, databasePassword)
            .schemas(schemas.toArray(new String[0]))
            .locations(locations.toArray(new String[0]))
            .baselineOnMigrate(true)
            .load();

        MigrationInfo currentMigration = flyway.info().current();

        if (currentMigration == null && !forceClean) {
            LOG.warning("DB is empty so changing forceClean to true");
            forceClean = true;
        }

        if (forceClean) {
            LOG.warning("!!! Cleaning database !!!");
            flyway.clean();
        } else {
            LOG.fine("Not cleaning, using existing database");
        }

        for (MigrationInfo i : flyway.info().pending()) {
            LOG.info("Pending task: " + i.getVersion() + ", " + i.getDescription() + ", " + i.getScript());
        }
        int applied = flyway.migrate();
        LOG.info("Applied database schema migrations: " + applied);
        flyway.validate();
    }

    protected void appendSchemaLocations(List<String> locations) {
        locations.addAll(defaultSchemaLocations);
    }

    protected void appendSchemas(List<String> schemas) {
        schemas.addAll(this.schemas);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
            "database=" + database +
            ", persistenceUnitName='" + persistenceUnitName + '\'' +
            '}';
    }
}
