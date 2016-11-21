/*
 * OpenRemote, the Home of the Digital Home.
 * Copyright 2008-2016, OpenRemote Inc.
 *
 * See the contributors.txt file in the distribution for a
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
package org.openremote.controllercommand;

import org.glassfish.jersey.server.ResourceConfig;
import org.hibernate.Session;
import org.hibernate.jdbc.Work;
import org.hibernate.tool.hbm2ddl.MultipleLinesSqlCommandExtractor;
import org.openremote.beehive.EntityTransactionFilter;
import org.openremote.controllercommand.proxy.ProxyServer;
import org.openremote.controllercommand.resources.ControllerCommandResource;
import org.openremote.controllercommand.resources.ControllerCommandsResource;
import org.openremote.controllercommand.service.AccountService;
import org.openremote.controllercommand.service.ControllerCommandService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PreDestroy;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.Persistence;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.*;
import java.util.Properties;

public class ControllerProxyAndCommandServiceApplication extends ResourceConfig {
    static private EntityManagerFactory entityManagerFactory;

    protected final static Logger LOG = LoggerFactory.getLogger(ControllerProxyAndCommandServiceApplication.class);

    public ControllerProxyAndCommandServiceApplication() {

        Properties config = new Properties();
        try {
            config.load(getClassLoader().getResourceAsStream("config.properties"));
        } catch (IOException e) {
            // TODO: log
            e.printStackTrace();
        }

        entityManagerFactory = Persistence.createEntityManagerFactory(config.getProperty("persistenceUnitName", "CCS-MySQL"));
        // TODO THis is never called, no idea why....
        if (config.getProperty("persistenceCreateSchema", "false").equals("true")) {
            LOG.info("### Creating database schema");
            createSchema();
        } else {
            LOG.info("### Not creating database schema");
        }

        LOG.info("EntityManagerFactory has been created");
        register(EntityPersistence.class);

        GenericDAO genericDAO = new GenericDAO();
        final AccountService accountService = new AccountService();
        accountService.setGenericDAO(genericDAO);
        final ControllerCommandService controllerCommandService = new ControllerCommandService();
        controllerCommandService.setGenericDAO(genericDAO);

        String proxyHostname = config.getProperty("proxy.hostname", "localhost");
        Integer proxyTimeout = getIntegerConfiguration(config, "proxy.timeout", 10000);
        Integer proxyPort = getIntegerConfiguration(config, "proxy.port", 10000);
        String proxyClientPortRange = config.getProperty("proxy.clientPortRange", "30000-30010");
        Boolean useSSL = getBooleanConfiguration(config, "proxy.useSSL", true);
        String keystore = config.getProperty("proxy.keystore", "keystore.jks");
        String keystorePassword = config.getProperty("proxy.keystorePassword", "storepass");
        ProxyServer ps = new ProxyServer(proxyHostname, proxyTimeout, proxyPort, proxyClientPortRange, useSSL, keystore, keystorePassword, controllerCommandService, accountService, this);
        ps.start();

        register(ControllerCommandResource.class);
        register(ControllerCommandsResource.class);

        register(new org.glassfish.hk2.utilities.binding.AbstractBinder() {
            @Override
            protected void configure() {
                bind(accountService);
                bind(controllerCommandService);
            }
        });
    }

    @PreDestroy
    private void testDestroy() {
        try {
            entityManagerFactory.close();
            LOG.info("EntityManagerFactory has been closed");
        } catch (Exception e) {
            LOG.warn("Could not close EntityManagerFactory");
        }
    }

    private Integer getIntegerConfiguration(Properties config, String propertyName, Integer defaultValue) {
        String stringValue = config.getProperty(propertyName);
        if (stringValue == null) {
            return defaultValue;
        }
        Integer integerValue = null;
        try {
            integerValue = Integer.parseInt(stringValue);
        } catch (NumberFormatException e) {
            integerValue = defaultValue;
        }
        return integerValue;
    }

    private Boolean getBooleanConfiguration(Properties config, String propertyName, Boolean defaultValue) {
        String stringValue = config.getProperty(propertyName);
        if (stringValue == null) {
            return defaultValue;
        }
        Boolean booleanValue = null;
        try {
            booleanValue = Boolean.parseBoolean(stringValue);
        } catch (NumberFormatException e) {
            booleanValue = defaultValue;
        }
        return booleanValue;
    }

    public EntityManager createEntityManager() {
        LOG.trace(">>createEntityManager");
        EntityManager entityManager = null;
        try {
            LOG.trace("Before createEntityManager");
            entityManager = entityManagerFactory.createEntityManager();
            LOG.debug("Got entityManager " + entityManager);
            EntityTransaction tx = entityManager.getTransaction();
            LOG.debug("Got transaction " + tx);
            tx.begin();
            LOG.debug("Begun transaction");
        } catch (Exception e) {
            LOG.error("Failed to create an EntityManager", e);
        }
        LOG.trace("<<createEntityManager");
        return entityManager;
    }

    public void commitEntityManager(EntityManager entityManager) {
        LOG.trace(">>commitEntityManager");
        if (entityManager.isOpen()) {
            LOG.debug("entityManager opened, commit transaction");
            entityManager.getTransaction().commit();
            entityManager.close();
        }
        LOG.trace("<<commitEntityManager");
    }

    public void rollbackEntityManager(EntityManager entityManager) {
        LOG.trace(">>rollbackEntityManager");
        if (entityManager.isOpen()) {
            LOG.debug("entityManager opened, rollback transaction");
            entityManager.getTransaction().rollback();
            entityManager.close();
        }
        LOG.trace("<<rollbackEntityManager");
    }

    /**
     * This container filter implements a managed persistence context for JPA entities used
     * in JAX-RS resources.
     */
    private static class EntityPersistence implements ContainerRequestFilter, ContainerResponseFilter {
        // Class Members ------------------------------------------------------------------------------

        // ContainerRequestFilter Implementation ------------------------------------------------------

        /**
         * Passes the entity manager reference as a request property to the resource classes to use.
         * Also begins the transaction boundary for JPA entities here.
         */
        @Override
        public void filter(ContainerRequestContext request) {
            EntityManager entityManager = null;
            try {
                entityManager = entityManagerFactory.createEntityManager();
                request.setProperty(EntityTransactionFilter.PERSISTENCE_ENTITY_MANAGER_LOOKUP, entityManager);
                entityManager.getTransaction().begin();
            } catch (Throwable throwable) {

                LOG.error("Failed to create EntityManager", throwable);

                if (entityManager != null) {
                    EntityTransaction tx = entityManager.getTransaction();
                    if (tx != null && tx.isActive()) {
                        try {
                            tx.rollback();
                        } catch (Exception e) {
                            LOG.warn("Failed to rollback transaction ", e);
                        }
                    }
                }
            }
        }

        /**
         * Manages the entity transaction boundary on return request. If entity transaction has
         * been marked for rollback, or we are returning an HTTP error code 400 or above, roll back
         * the entity transaction.
         */
        @Override
        public void filter(ContainerRequestContext request, ContainerResponseContext response) {
            EntityManager entityManager = (EntityManager) request.getProperty(EntityTransactionFilter.PERSISTENCE_ENTITY_MANAGER_LOOKUP);
            if (entityManager == null) {
                return;
            }

            EntityTransaction tx = entityManager.getTransaction();

            LOG.debug("Transaction is " + tx + " , active ? " + (tx.isActive() ? "yes" : "no"));

            if (tx != null && tx.isActive()) {
                if (tx.getRollbackOnly() || response.getStatus() >= 400) {
                    LOG.debug("Rolling back transaction");
                    try {
                        tx.rollback();
                    } catch (Exception e) {
                        LOG.warn("Failed to rollback transaction ", e);
                    }
                } else {
                    LOG.debug("Commit transaction");
                    try {
                        tx.commit();
                    } catch (Exception e) {
                        LOG.error("Failed to commit transaction ", e);
                    }
                }
            }
            if (entityManager.isOpen()) {
                try {
                    entityManager.close();
                } catch (Exception e) {
                    LOG.warn("Failed to closed EntityManager", e);
                }
            }
        }
    }

    private void createSchema() {
        // Check if the CONTROLLER table exists
        entityManagerFactory.createEntityManager().unwrap(Session.class).doWork(new Work() {
            @Override
            public void execute(Connection connection) throws SQLException {
                DatabaseMetaData meta;
                ResultSet rs = null;
                try {
                    meta = connection.getMetaData();
                    rs = meta.getTables(null, null, "controller", new String[]{"TABLE"});
                    if (!rs.next()) {
                        rs.close();
                        LOG.info("Table CONTROLLER not found, attempting to create database schema");
                        MultipleLinesSqlCommandExtractor extractor = new MultipleLinesSqlCommandExtractor();
                        String[] sqlCommands = extractor.extractCommands(
                            new BufferedReader(new InputStreamReader(
                                getClass().getClassLoader().getResourceAsStream("sql/10-CreateSchema.sql"))
                            )
                        );
                        for (String sqlCommand : sqlCommands) {
                            Statement stmt = null;
                            try {
                                stmt = connection.createStatement();
                                rs = stmt.executeQuery(sqlCommand);
                            } finally {
                                if (stmt != null) {
                                    stmt.close();
                                }
                            }
                        }
                    } else {
                        LOG.info("Table CONTROLLER found in database schema, not creating schema");
                    }
                } finally {
                    if (rs != null)
                        rs.close();
                }
            }
        });
    }

}
