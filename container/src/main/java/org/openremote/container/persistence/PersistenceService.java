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

import org.apache.camel.ExchangePattern;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.hibernate.Session;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.jpa.boot.internal.EntityManagerFactoryBuilderImpl;
import org.hibernate.jpa.boot.internal.PersistenceUnitInfoDescriptor;
import org.openremote.container.message.MessageBrokerService;
import org.openremote.model.Container;
import org.openremote.model.ContainerService;
import org.openremote.model.EntityClassProvider;
import org.openremote.model.apps.ConsoleAppConfig;
import org.openremote.model.asset.Asset;
import org.openremote.model.asset.AssetDescriptor;
import org.openremote.model.asset.UserAssetLink;
import org.openremote.model.datapoint.AssetDatapoint;
import org.openremote.model.gateway.GatewayConnection;
import org.openremote.model.notification.SentNotification;
import org.openremote.model.datapoint.AssetPredictedDatapoint;
import org.openremote.model.provisioning.ProvisioningConfig;
import org.openremote.model.provisioning.X509ProvisioningConfig;
import org.openremote.model.rules.AssetRuleset;
import org.openremote.model.rules.GlobalRuleset;
import org.openremote.model.rules.TenantRuleset;
import org.openremote.model.security.RealmRole;
import org.openremote.model.security.Tenant;
import org.openremote.model.security.User;
import org.openremote.model.security.UserAttribute;
import org.openremote.model.syslog.SyslogEvent;
import org.openremote.model.util.ValueUtil;

import javax.persistence.*;
import javax.persistence.spi.ClassTransformer;
import javax.persistence.spi.PersistenceUnitTransactionType;
import javax.sql.DataSource;
import javax.ws.rs.core.UriBuilder;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.IntStream;

import static org.openremote.container.util.MapAccess.*;

public class PersistenceService implements ContainerService {

    /**
     * Programmatic definition of OpenRemotePU for hibernate
     */
    public static class PersistenceUnitInfo implements javax.persistence.spi.PersistenceUnitInfo {

        List<String> managedClassNames;
        Properties properties;


        public PersistenceUnitInfo(List<String> managedClassNames, Properties properties) {
            // Configure default props
            Properties props = new Properties();
            props.put(AvailableSettings.FORMAT_SQL, "true");
            props.put(AvailableSettings.USE_SQL_COMMENTS, "true");
            props.put(AvailableSettings.SCANNER_DISCOVERY, "none");
            //props.put(AvailableSettings.SHOW_SQL, "true");
            props.put(AvailableSettings.CURRENT_SESSION_CONTEXT_CLASS, "thread");
            props.put(AvailableSettings.HBM2DDL_IMPORT_FILES_SQL_EXTRACTOR, "org.openremote.container.persistence.EnhancedImportSqlCommandExtractor");

            // Add custom properties
            props.putAll(properties);

            this.managedClassNames = managedClassNames;
            this.properties = props;
        }

        @Override
        public String getPersistenceUnitName() {
            return PERSISTENCE_UNIT_NAME_DEFAULT;
        }

        @Override
        public String getPersistenceProviderClassName() {
            return "org.hibernate.jpa.HibernatePersistenceProvider";
        }

        @Override
        public PersistenceUnitTransactionType getTransactionType() {
            return PersistenceUnitTransactionType.RESOURCE_LOCAL;
        }

        @Override
        public DataSource getJtaDataSource() {
            return null;
        }

        @Override
        public DataSource getNonJtaDataSource() {
            return null;
        }

        @Override
        public List<String> getMappingFileNames() {
            return null;
        }

        @Override
        public List<URL> getJarFileUrls() {
            return null;
        }

        @Override
        public URL getPersistenceUnitRootUrl() {
            return null;
        }

        @Override
        public List<String> getManagedClassNames() {
            return managedClassNames;
        }

        @Override
        public boolean excludeUnlistedClasses() {
            return true;
        }

        @Override
        public SharedCacheMode getSharedCacheMode() {
            return null;
        }

        @Override
        public ValidationMode getValidationMode() {
            return null;
        }

        @Override
        public Properties getProperties() {
            return properties;
        }

        @Override
        public String getPersistenceXMLSchemaVersion() {
            return null;
        }

        @Override
        public ClassLoader getClassLoader() {
            return null;
        }

        @Override
        public void addTransformer(ClassTransformer transformer) {

        }

        @Override
        public ClassLoader getNewTempClassLoader() {
            return null;
        }
    }

    private static final Logger LOG = Logger.getLogger(PersistenceService.class.getName());

    /**
     * We must use a different schema than Keycloak to store our tables, or FlywayDB
     * will wipe Keycloak tables. And since Keycloak doesn't call CREATE SCHEMA IF NOT EXISTS
     * on startup (Liquibase is not as good as FlywayDB? They both seem to suck... can't exclude
     * database artifacts from wipe/migrate in FlywayDB...) you must have it in the 'public'
     * schema. Hence we need a different schema here.
     */
    public static final String SETUP_WIPE_CLEAN_INSTALL = "SETUP_WIPE_CLEAN_INSTALL";
    public static final String PERSISTENCE_UNIT_NAME = "PERSISTENCE_UNIT_NAME";
    public static final String PERSISTENCE_UNIT_NAME_DEFAULT = "OpenRemotePU";
    public static final String DB_VENDOR = "DB_VENDOR";
    public static final String DB_VENDOR_DEFAULT = Database.Product.POSTGRES.name();
    public static final String DB_HOST = "DB_HOST";
    public static final String DB_HOST_DEFAULT = "localhost";
    public static final String DB_PORT = "DB_PORT";
    public static final int DB_PORT_DEFAULT = 5432;
    public static final String DB_NAME = "DB_NAME";
    public static final String DB_NAME_DEFAULT = "openremote";
    public static final String DB_SCHEMA = "DB_SCHEMA";
    public static final String DB_SCHEMA_DEFAULT = "openremote";
    public static final String DB_USERNAME = "DB_USERNAME";
    public static final String DB_USERNAME_DEFAULT = "postgres";
    public static final String DB_PASSWORD = "DB_PASSWORD";
    public static final String DB_PASSWORD_DEFAULT = "postgres";
    public static final String DB_MIN_POOL_SIZE = "DB_MIN_POOL_SIZE";
    public static final int DB_MIN_POOL_SIZE_DEFAULT = 5;
    public static final String DB_MAX_POOL_SIZE = "DB_MAX_POOL_SIZE";
    public static final int DB_MAX_POOL_SIZE_DEFAULT = 20;
    public static final String DB_CONNECTION_TIMEOUT_SECONDS = "DB_CONNECTION_TIMEOUT_SECONDS";
    public static final int DB_CONNECTION_TIMEOUT_SECONDS_DEFAULT = 300;
    public static final int PRIORITY = Integer.MIN_VALUE + 100;

    protected MessageBrokerService messageBrokerService;
    protected Database database;
    protected String persistenceUnitName;
    protected Properties persistenceUnitProperties;
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

        String dbVendor = getString(container.getConfig(), DB_VENDOR, DB_VENDOR_DEFAULT).toUpperCase(Locale.ROOT);
        LOG.info("Preparing persistence service for database: " + dbVendor);

        try {
            database = Database.Product.valueOf(dbVendor);
        } catch (Exception e) {
            LOG.severe("Requested DB_VENDOR is not supported: " + dbVendor);
            throw new UnsupportedOperationException("Requested DB_VENDOR is not supported: " + dbVendor);
        }

        String dbHost = getString(container.getConfig(), DB_HOST, DB_HOST_DEFAULT);
        int dbPort = getInteger(container.getConfig(), DB_PORT, DB_PORT_DEFAULT);
        String dbName = getString(container.getConfig(), DB_NAME, DB_NAME_DEFAULT);
        String dbSchema = getString(container.getConfig(), DB_SCHEMA, DB_SCHEMA_DEFAULT);
        String dbUsername = getString(container.getConfig(), DB_USERNAME, DB_USERNAME_DEFAULT);
        String dbPassword = getString(container.getConfig(), DB_PASSWORD, DB_PASSWORD_DEFAULT);
        String connectionUrl = "jdbc:" + database.getConnectorName() + "://" + dbHost + ":" + dbPort + "/" + dbName;
        connectionUrl = UriBuilder.fromUri(connectionUrl).replaceQueryParam("currentSchema", dbSchema).build().toString();

        persistenceUnitProperties = database.createProperties();

        if (messageBrokerService != null) {
            persistenceUnitProperties.put(
                org.hibernate.cfg.AvailableSettings.SESSION_SCOPED_INTERCEPTOR,
                PersistenceEventInterceptor.class.getName()
            );
        }

        persistenceUnitProperties.put(AvailableSettings.DEFAULT_SCHEMA, dbSchema);

        persistenceUnitName = getString(container.getConfig(), PERSISTENCE_UNIT_NAME, PERSISTENCE_UNIT_NAME_DEFAULT);

        forceClean = getBoolean(container.getConfig(), SETUP_WIPE_CLEAN_INSTALL, container.isDevMode());

        openDatabase(container, database, dbUsername, dbPassword, connectionUrl);
        prepareSchema(connectionUrl, dbUsername, dbPassword, dbSchema);

        // Register standard entity classes and also any Entity ClassProviders
        List<String> entityClasses = new ArrayList<>(50);
        entityClasses.add(Asset.class.getName());
        entityClasses.add(UserAssetLink.class.getName());
        entityClasses.add(AssetDatapoint.class.getName());
        entityClasses.add(SentNotification.class.getName());
        entityClasses.add(AssetPredictedDatapoint.class.getName());
        entityClasses.add(Tenant.class.getName());
        entityClasses.add(User.class.getName());
        entityClasses.add(UserAttribute.class.getName());
        entityClasses.add(RealmRole.class.getName());
        entityClasses.add(GlobalRuleset.class.getName());
        entityClasses.add(AssetRuleset.class.getName());
        entityClasses.add(TenantRuleset.class.getName());
        entityClasses.add(SyslogEvent.class.getName());
        entityClasses.add(GatewayConnection.class.getName());
        entityClasses.add(ConsoleAppConfig.class.getName());
        entityClasses.add(ProvisioningConfig.class.getName());
        entityClasses.add(X509ProvisioningConfig.class.getName());

        // Add packages with package-info (don't think this is JPA spec but hibernate specific)
        entityClasses.add("org.openremote.container.persistence");
        entityClasses.add("org.openremote.container.util");

        // Get asset sub type entities from asset model
        Arrays.stream(ValueUtil.getAssetDescriptors(null))
            .map(AssetDescriptor::getType)
            .filter(assetClass -> assetClass.getAnnotation(Entity.class) != null)
            .map(Class::getName)
            .forEach(entityClasses::add);

        // Get any entity class providers from the service loader
        ServiceLoader<EntityClassProvider> entityClassProviders = ServiceLoader.load(EntityClassProvider.class);
        entityClassProviders.forEach(entityClassProvider -> entityClassProvider.getEntityClasses().stream()
            .filter(entityClass -> entityClass.getAnnotation(Entity.class) != null)
            .map(Class::getName).forEach(entityClasses::add));

        this.entityManagerFactory = getEntityManagerFactory(persistenceUnitProperties, entityClasses);
        //Persistence.createEntityManagerFactory(persistenceUnitName, persistenceUnitProperties);
    }

    protected EntityManagerFactory getEntityManagerFactory(Properties properties, List<String> classNames) {
        PersistenceUnitInfo persistenceUnitInfo = new PersistenceUnitInfo(classNames, properties);

        return new EntityManagerFactoryBuilderImpl(
            new PersistenceUnitInfoDescriptor(persistenceUnitInfo), null)
            .build();
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

    /**
     * Generate {@link PersistenceEvent}s for entities not managed by JPA (i.e. Keycloak entities)
     */
    public void publishPersistenceEvent(PersistenceEvent.Cause cause, Object currentEntity, Object previousEntity, Field[] propertyFields) {
        switch (cause) {
            case CREATE:
                publishPersistenceEvent(cause, currentEntity, null, null, null);
                break;
            case DELETE:
                publishPersistenceEvent(cause, previousEntity, null, null, null);
                break;
            case UPDATE:
                List<String> propertyNames = new ArrayList<>(propertyFields.length);
                List<Object> currentState = new ArrayList<>(propertyFields.length);
                List<Object> previousState = new ArrayList<>(propertyFields.length);
                IntStream.range(0, propertyFields.length).forEach(i -> {
                    Object currentValue = ValueUtil.getObjectFieldValue(currentEntity, propertyFields[i]);
                    Object previousValue = ValueUtil.getObjectFieldValue(previousEntity, propertyFields[i]);
                    if (!ValueUtil.objectsEquals(currentValue, previousValue)) {
                        propertyNames.add(propertyFields[i].getName());
                        currentState.add(currentValue);
                        previousState.add(previousValue);
                    }
                });
                publishPersistenceEvent(cause, currentEntity, propertyNames.toArray(new String[0]), currentState.toArray(), previousState.toArray());
                break;
        }
    }

    /**
     * Generate {@link PersistenceEvent}s for entities not managed by JPA (i.e. Keycloak entities)
     */
    public void publishPersistenceEvent(PersistenceEvent.Cause cause, Object entity, String[] propertyNames, Object[] currentState, Object[] previousState) {
        // Fire persistence event although we don't use database for Tenant CUD but call Keycloak API
        PersistenceEvent<?> persistenceEvent = new PersistenceEvent<>(cause, entity, propertyNames, currentState, previousState);

        if (messageBrokerService.getProducerTemplate() != null) {
            messageBrokerService.getProducerTemplate().sendBodyAndHeader(
                PersistenceEvent.PERSISTENCE_TOPIC,
                ExchangePattern.InOnly,
                persistenceEvent,
                PersistenceEvent.HEADER_ENTITY_TYPE,
                persistenceEvent.getEntity().getClass()
            );
        }
    }

    protected void openDatabase(Container container, Database database, String username, String password, String connectionUrl) {

        int databaseMinPoolSize = getInteger(container.getConfig(), DB_MIN_POOL_SIZE, DB_MIN_POOL_SIZE_DEFAULT);
        int databaseMaxPoolSize = getInteger(container.getConfig(), DB_MAX_POOL_SIZE, DB_MAX_POOL_SIZE_DEFAULT);
        int connectionTimeoutSeconds = getInteger(container.getConfig(), DB_CONNECTION_TIMEOUT_SECONDS, DB_CONNECTION_TIMEOUT_SECONDS_DEFAULT);
        LOG.info("Opening database connection: " + connectionUrl);
        database.open(persistenceUnitProperties, connectionUrl, username, password, connectionTimeoutSeconds, databaseMinPoolSize, databaseMaxPoolSize);
    }

    protected void prepareSchema(String connectionUrl, String databaseUsername, String databasePassword, String schemaName) {
        LOG.fine("Preparing database schema");
        List<String> locations = new ArrayList<>();
        List<String> schemas = new ArrayList<>();
        schemas.add(schemaName);
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
