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

import jakarta.persistence.*;
import jakarta.persistence.spi.ClassTransformer;
import jakarta.persistence.spi.PersistenceUnitTransactionType;
import jakarta.ws.rs.core.UriBuilder;
import org.apache.camel.FluentProducerTemplate;
import org.apache.camel.Predicate;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.output.MigrateResult;
import org.flywaydb.core.internal.sqlscript.FlywaySqlScriptException;
import org.hibernate.Session;
import org.hibernate.annotations.Formula;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.jpa.boot.internal.EntityManagerFactoryBuilderImpl;
import org.hibernate.jpa.boot.internal.PersistenceUnitInfoDescriptor;
import org.openremote.container.message.MessageBrokerService;
import org.openremote.model.Container;
import org.openremote.model.ContainerService;
import org.openremote.model.EntityClassProvider;
import org.openremote.model.PersistenceEvent;
import org.openremote.model.alarm.AlarmAssetLink;
import org.openremote.model.alarm.AlarmUserLink;
import org.openremote.model.alarm.SentAlarm;
import org.openremote.model.apps.ConsoleAppConfig;
import org.openremote.model.asset.Asset;
import org.openremote.model.asset.AssetDescriptor;
import org.openremote.model.asset.UserAssetLink;
import org.openremote.model.asset.impl.UnknownAsset;
import org.openremote.model.dashboard.Dashboard;
import org.openremote.model.datapoint.AssetDatapoint;
import org.openremote.model.datapoint.AssetPredictedDatapoint;
import org.openremote.model.gateway.GatewayConnection;
import org.openremote.model.notification.SentNotification;
import org.openremote.model.provisioning.ProvisioningConfig;
import org.openremote.model.provisioning.X509ProvisioningConfig;
import org.openremote.model.rules.AssetRuleset;
import org.openremote.model.rules.GlobalRuleset;
import org.openremote.model.rules.RealmRuleset;
import org.openremote.model.security.Realm;
import org.openremote.model.security.RealmRole;
import org.openremote.model.security.User;
import org.openremote.model.security.UserAttribute;
import org.openremote.model.syslog.SyslogEvent;
import org.openremote.model.util.ValueUtil;

import javax.sql.DataSource;
import java.io.File;
import java.lang.reflect.Field;
import java.net.URL;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.IntStream;

import static org.openremote.container.util.MapAccess.*;

public class PersistenceService implements ContainerService, Consumer<PersistenceEvent<?>> {

    /**
     * Programmatic definition of OpenRemotePU for hibernate
     */
    public static class PersistenceUnitInfo implements jakarta.persistence.spi.PersistenceUnitInfo {

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
            props.put(AvailableSettings.HBM2DDL_IMPORT_FILES_SQL_EXTRACTOR, "org.openremote.container.persistence.EnhancedSqlScriptExtractor");

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

    // TODO: Make configurable
    public static final String PERSISTENCE_TOPIC =
        "seda://PersistenceTopic?multipleConsumers=true&concurrentConsumers=1&waitForTaskToComplete=NEVER&purgeWhenStopping=true&discardIfNoConsumers=true&limitConcurrentConsumers=false&size=25000";
    public static final String HEADER_ENTITY_TYPE = PersistenceEvent.class.getSimpleName() + ".ENTITY_TYPE";

    private static final Logger LOG = Logger.getLogger(PersistenceService.class.getName());

    /**
     * We must use a different schema than Keycloak to store our tables, or FlywayDB
     * will wipe Keycloak tables. And since Keycloak doesn't call CREATE SCHEMA IF NOT EXISTS
     * on startup (Liquibase is not as good as FlywayDB? They both seem to suck... can't exclude
     * database artifacts from wipe/migrate in FlywayDB...) you must have it in the 'public'
     * schema. Hence we need a different schema here.
     */
    public static final String OR_SETUP_RUN_ON_RESTART = "OR_SETUP_RUN_ON_RESTART";
    public static final String PERSISTENCE_UNIT_NAME = "PERSISTENCE_UNIT_NAME";
    public static final String PERSISTENCE_UNIT_NAME_DEFAULT = "OpenRemotePU";
    public static final String OR_DB_VENDOR = "OR_DB_VENDOR";
    public static final String OR_DB_VENDOR_DEFAULT = Database.Product.POSTGRES.name();
    public static final String OR_DB_HOST = "OR_DB_HOST";
    public static final String OR_DB_HOST_DEFAULT = "localhost";
    public static final String OR_DB_PORT = "OR_DB_PORT";
    public static final int OR_DB_PORT_DEFAULT = 5432;
    public static final String OR_DB_NAME = "OR_DB_NAME";
    public static final String OR_DB_NAME_DEFAULT = "openremote";
    public static final String OR_DB_SCHEMA = "OR_DB_SCHEMA";
    public static final String OR_DB_SCHEMA_DEFAULT = "openremote";
    public static final String OR_DB_USER = "OR_DB_USER";
    public static final String OR_DB_USER_DEFAULT = "postgres";
    public static final String OR_DB_PASSWORD = "OR_DB_PASSWORD";
    public static final String OR_DB_PASSWORD_DEFAULT = "postgres";
    public static final String OR_DB_MIN_POOL_SIZE = "OR_DB_MIN_POOL_SIZE";
    public static final int OR_DB_MIN_POOL_SIZE_DEFAULT = 5;
    public static final String OR_DB_MAX_POOL_SIZE = "OR_DB_MAX_POOL_SIZE";
    public static final int OR_DB_MAX_POOL_SIZE_DEFAULT = 20;
    public static final String OR_DB_CONNECTION_TIMEOUT_SECONDS = "OR_DB_CONNECTION_TIMEOUT_SECONDS";
    public static final int OR_DB_CONNECTION_TIMEOUT_SECONDS_DEFAULT = 300;
    public static final String OR_STORAGE_DIR = "OR_STORAGE_DIR";
    public static final String OR_STORAGE_DIR_DEFAULT = "tmp";
    public static final String OR_DB_FLYWAY_OUT_OF_ORDER = "OR_DB_FLYWAY_OUT_OF_ORDER";
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
    protected Path storageDir;

    public static Predicate isPersistenceEventForEntityType(Class<?> type) {
        return exchange -> {
            Class<?> entityType = exchange.getIn().getHeader(HEADER_ENTITY_TYPE, Class.class);
            return type.isAssignableFrom(entityType);
        };
    }

    @Override
    public int getPriority() {
        return PRIORITY;
    }

    @Override
    public void init(Container container) throws Exception {
        this.messageBrokerService = container.hasService(MessageBrokerService.class)
            ? container.getService(MessageBrokerService.class)
            : null;

        String dbVendor = getString(container.getConfig(), OR_DB_VENDOR, OR_DB_VENDOR_DEFAULT).toUpperCase(Locale.ROOT);
        LOG.info("Preparing persistence service for database: " + dbVendor);

        try {
            database = Database.Product.valueOf(dbVendor);
        } catch (Exception e) {
            LOG.severe("Requested OR_DB_VENDOR is not supported: " + dbVendor);
            throw new UnsupportedOperationException("Requested OR_DB_VENDOR is not supported: " + dbVendor);
        }

        String dbHost = getString(container.getConfig(), OR_DB_HOST, OR_DB_HOST_DEFAULT);
        int dbPort = getInteger(container.getConfig(), OR_DB_PORT, OR_DB_PORT_DEFAULT);
        String dbName = getString(container.getConfig(), OR_DB_NAME, OR_DB_NAME_DEFAULT);
        String dbSchema = getString(container.getConfig(), OR_DB_SCHEMA, OR_DB_SCHEMA_DEFAULT);
        String dbUsername = getString(container.getConfig(), OR_DB_USER, OR_DB_USER_DEFAULT);
        String dbPassword = getString(container.getConfig(), OR_DB_PASSWORD, OR_DB_PASSWORD_DEFAULT);
        String connectionUrl = "jdbc:" + database.getConnectorName() + "://" + dbHost + ":" + dbPort + "/" + dbName;
        connectionUrl = UriBuilder.fromUri(connectionUrl).replaceQueryParam("currentSchema", dbSchema).build().toString();

        persistenceUnitProperties = database.createProperties();

        if (messageBrokerService != null) {
            persistenceUnitProperties.put(
                AvailableSettings.SESSION_SCOPED_INTERCEPTOR,
                PersistenceEventInterceptor.class.getName()
            );
        }

        persistenceUnitProperties.put(AvailableSettings.JSON_FORMAT_MAPPER, JsonFormatMapper.class.getName());
        // Disable JPA validation as it triggers on select for entities with JSON columns - we do our own validation
        persistenceUnitProperties.put(AvailableSettings.JAKARTA_VALIDATION_MODE, ValidationMode.NONE.name());

        persistenceUnitProperties.put(AvailableSettings.DEFAULT_SCHEMA, dbSchema);

        // Add custom integrator so we can register a custom flush entity event listener
        persistenceUnitProperties.put(EntityManagerFactoryBuilderImpl.INTEGRATOR_PROVIDER, IntegratorProvider.class.getName());

        persistenceUnitName = getString(container.getConfig(), PERSISTENCE_UNIT_NAME, PERSISTENCE_UNIT_NAME_DEFAULT);

        forceClean = getBoolean(container.getConfig(), OR_SETUP_RUN_ON_RESTART, container.isDevMode());

        storageDir = Paths.get(getString(container.getConfig(), OR_STORAGE_DIR, OR_STORAGE_DIR_DEFAULT));
        LOG.log(Level.INFO, "Setting storage directory to '" + storageDir.toAbsolutePath() + "'");

        if (!Files.exists(storageDir)) {
            Files.createDirectories(storageDir);
        }

        if (!Files.isDirectory(storageDir)) {
            String msg = "Specified OR_STORAGE_DIR '" + storageDir.toAbsolutePath() + "' is not a folder";
            LOG.log(Level.SEVERE, msg);
            throw new FileSystemNotFoundException(msg);
        }

        File testFile = storageDir.toFile();
        if (!testFile.canRead() || !testFile.canWrite()) {
            String msg = "Specified OR_STORAGE_DIR '" + storageDir.toAbsolutePath() + "' is not writable";
            LOG.log(Level.SEVERE, msg);
            throw new FileSystemNotFoundException(msg);
        }

        openDatabase(container, database, dbUsername, dbPassword, connectionUrl);
        prepareSchema(container, connectionUrl, dbUsername, dbPassword, dbSchema);
    }

    protected EntityManagerFactory getEntityManagerFactory(Properties properties, List<String> classNames) {
        PersistenceUnitInfo persistenceUnitInfo = new PersistenceUnitInfo(classNames, properties);

        return new EntityManagerFactoryBuilderImpl(
            new PersistenceUnitInfoDescriptor(persistenceUnitInfo), null)
            .build();
    }


    @Override
    public void start(Container container) throws Exception {
        // Register standard entity classes and also any Entity ClassProviders
        List<String> entityClasses = new ArrayList<>(50);
        entityClasses.add(Asset.class.getName());
        entityClasses.add(UserAssetLink.class.getName());
        entityClasses.add(AssetDatapoint.class.getName());
        entityClasses.add(SentNotification.class.getName());
        entityClasses.add(AssetPredictedDatapoint.class.getName());
        entityClasses.add(Realm.class.getName());
        entityClasses.add(User.class.getName());
        entityClasses.add(UserAttribute.class.getName());
        entityClasses.add(RealmRole.class.getName());
        entityClasses.add(GlobalRuleset.class.getName());
        entityClasses.add(AssetRuleset.class.getName());
        entityClasses.add(RealmRuleset.class.getName());
        entityClasses.add(SyslogEvent.class.getName());
        entityClasses.add(GatewayConnection.class.getName());
        entityClasses.add(ConsoleAppConfig.class.getName());
        entityClasses.add(Dashboard.class.getName());
        entityClasses.add(ProvisioningConfig.class.getName());
        entityClasses.add(X509ProvisioningConfig.class.getName());
        entityClasses.add(SentAlarm.class.getName());
        entityClasses.add(AlarmAssetLink.class.getName());
        entityClasses.add(AlarmUserLink.class.getName());

        // Add packages with package-info (don't think this is JPA spec but hibernate specific)
        entityClasses.add("org.openremote.container.util");

        // Get asset sub type entities from asset model
        entityClasses.add(UnknownAsset.class.getName()); // This doesn't have an asset descriptor which is why it is specifically added
        Arrays.stream(ValueUtil.getAssetDescriptors(null))
            .map(AssetDescriptor::getType)
            .filter(assetClass -> assetClass != null && assetClass.getAnnotation(Entity.class) != null)
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

    @Override
    public void stop(Container container) throws Exception {
        if (entityManagerFactory != null) {
            entityManagerFactory.close();
        }
        if (database != null) {
            database.close();
        }
    }

    public boolean isCleanInstall() {
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
            persistenceEventInterceptor.setEventConsumer(this);
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
                    if (LOG.isLoggable(Level.FINER)) {
                        LOG.log(Level.FINE, "Rolling back failed transaction, cause follows", ex);
                    } else {
                        LOG.log(Level.FINE, "Rolling back failed transaction");
                    }
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
    public void publishPersistenceEvent(PersistenceEvent.Cause cause, Object currentEntity, Object previousEntity, Class<?> clazz, List<String> includeFields, List<String> excludeFields) {

        Field[] propertyFields = getEntityPropertyFields(clazz, includeFields, excludeFields);

        switch (cause) {
            case CREATE -> publishPersistenceEvent(cause, currentEntity, null, null, null);
            case DELETE -> publishPersistenceEvent(cause, previousEntity, null, null, null);
            case UPDATE -> {
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
            }
        }
    }

    /**
     * Generate {@link PersistenceEvent}s for entities not managed by JPA (i.e. Keycloak entities)
     */
    public void publishPersistenceEvent(PersistenceEvent.Cause cause, Object entity, String[] propertyNames, Object[] currentState, Object[] previousState) {
        // Fire persistence event although we don't use database for Realm CUD but call Keycloak API
        PersistenceEvent<?> persistenceEvent = new PersistenceEvent<>(cause, entity, propertyNames, currentState, previousState);

        if (messageBrokerService.getFluentProducerTemplate() != null) {
            messageBrokerService.getFluentProducerTemplate()
                .withBody(persistenceEvent)
                .withHeader(HEADER_ENTITY_TYPE, persistenceEvent.getEntity().getClass())
                .to(PERSISTENCE_TOPIC)
                .asyncSend();
        }
    }

    protected void openDatabase(Container container, Database database, String username, String password, String connectionUrl) {

        int databaseMinPoolSize = getInteger(container.getConfig(), OR_DB_MIN_POOL_SIZE, OR_DB_MIN_POOL_SIZE_DEFAULT);
        int databaseMaxPoolSize = getInteger(container.getConfig(), OR_DB_MAX_POOL_SIZE, OR_DB_MAX_POOL_SIZE_DEFAULT);
        int connectionTimeoutSeconds = getInteger(container.getConfig(), OR_DB_CONNECTION_TIMEOUT_SECONDS, OR_DB_CONNECTION_TIMEOUT_SECONDS_DEFAULT);
        LOG.info("Opening database connection: " + connectionUrl);
        database.open(persistenceUnitProperties, connectionUrl, username, password, connectionTimeoutSeconds, databaseMinPoolSize, databaseMaxPoolSize);
    }

    protected void prepareSchema(Container container, String connectionUrl, String databaseUsername, String databasePassword, String schemaName) {

        boolean outOfOrder = getBoolean(container.getConfig(), OR_DB_FLYWAY_OUT_OF_ORDER, false);

        LOG.fine("Preparing database schema");
        List<String> locations = new ArrayList<>();
        List<String> schemas = new ArrayList<>();
        schemas.add(schemaName);
        appendSchemas(schemas);
        appendSchemaLocations(locations);

        // Adding timescaledb extension to make sure it has been initialized. Flyway clean caused issues with relations to timescaledb tables.
        // Excluding the extension(s) in the cleanup process will not be added soon https://github.com/flyway/flyway/issues/2271
        // Now applied it here (so it is excluded for the migration process), to prevent that flyway drops the extension during cleanup.
        StringBuilder initSql = new StringBuilder();
        initSql.append("CREATE EXTENSION IF NOT EXISTS timescaledb SCHEMA public cascade;");
        initSql.append("CREATE EXTENSION IF NOT EXISTS timescaledb_toolkit SCHEMA public cascade;");

        flyway = Flyway.configure()
            .cleanDisabled(false)
            .validateMigrationNaming(true)
            .dataSource(connectionUrl, databaseUsername, databasePassword)
            .schemas(schemas.toArray(new String[0]))
            .locations(locations.toArray(new String[0]))
            .initSql(initSql.toString())
            .baselineOnMigrate(true)
            .outOfOrder(outOfOrder)
            .load();

        MigrationInfo currentMigration;
        try {
            currentMigration = flyway.info().current();
        } catch (FlywaySqlScriptException fssex) {
            if(fssex.getStatement().contains("CREATE EXTENSION IF NOT EXISTS timescaledb")) { // ... SCHEMA public cascade;
                LOG.severe("Timescale DB extension not found; please ensure you are using a postgres image with timescale DB extension included.");
            }
            throw fssex;
        }

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
        MigrateResult result = flyway.migrate();
        LOG.info("Applied database schema migrations: " + result.migrationsExecuted);
        flyway.validate();
    }

    protected void appendSchemaLocations(List<String> locations) {
        locations.addAll(defaultSchemaLocations);
    }

    protected void appendSchemas(List<String> schemas) {
        schemas.addAll(this.schemas);
    }

    @Override
    public void accept(PersistenceEvent<?> persistenceEvent) {

        FluentProducerTemplate producerTemplate = messageBrokerService.getFluentProducerTemplate();

        if (producerTemplate != null) {
            producerTemplate
                .withBody(persistenceEvent)
                .withHeader(HEADER_ENTITY_TYPE, persistenceEvent.getEntity().getClass())
                .to(PERSISTENCE_TOPIC)
                .asyncSend();
        }
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
            "database=" + database +
            ", persistenceUnitName='" + persistenceUnitName + '\'' +
            '}';
    }

    public Path getStorageDir() {
        return storageDir;
    }

    public static Field[] getEntityPropertyFields(Class<?> clazz, List<String> includeFields, List<String> excludeFields) {
        return Arrays.stream(clazz.getDeclaredFields())
            .filter(field -> ((field.isAnnotationPresent(Column.class) || field.isAnnotationPresent(EmbeddedId.class) || field.isAnnotationPresent(JoinColumn.class) || field.isAnnotationPresent(Formula.class)) && (excludeFields == null || !excludeFields.contains(field.getName())))
                || (includeFields != null && includeFields.contains(field.getName())))
            .toArray(Field[]::new);
    }
}
