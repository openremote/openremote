/*
 * Copyright 2022, OpenRemote Inc.
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
package org.openremote.manager.mqtt;

import org.apache.activemq.artemis.core.config.impl.SecurityConfiguration;
import org.apache.activemq.artemis.core.security.CheckType;
import org.apache.activemq.artemis.core.security.Role;
import org.apache.activemq.artemis.core.server.ActiveMQServer;
import org.apache.activemq.artemis.spi.core.protocol.RemotingConnection;
import org.apache.activemq.artemis.spi.core.security.ActiveMQJAASSecurityManager;
import org.keycloak.KeycloakSecurityContext;
import org.keycloak.adapters.KeycloakDeployment;
import org.openremote.container.security.keycloak.KeycloakIdentityProvider;
import org.openremote.manager.security.AuthorisationService;
import org.openremote.manager.security.MultiTenantJaasCallbackHandler;
import org.openremote.manager.security.RemotingConnectionPrincipal;
import org.openremote.model.syslog.SyslogCategory;

import javax.security.auth.Subject;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;
import java.util.Set;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.apache.activemq.artemis.core.remoting.CertificateUtil.getCertsFromConnection;
import static org.openremote.manager.mqtt.MQTTBrokerService.connectionToString;
import static org.openremote.model.syslog.SyslogCategory.API;

/**
 * A security manager that uses the {@link org.openremote.manager.security.MultiTenantJaasCallbackHandler} with a
 * dynamic {@link org.keycloak.adapters.KeycloakDeployment} resolver.
 *
 * Unfortunately lots of private methods and fields in super class.
 */
public class ActiveMQORSecurityManager extends ActiveMQJAASSecurityManager {

    private static final Logger LOG = SyslogCategory.getLogger(API, ActiveMQORSecurityManager.class);
    protected AuthorisationService authorisationService;
    protected MQTTBrokerService brokerService;
    protected Function<String, KeycloakDeployment> deploymentResolver;

    // Duplicate fields due to being private in super class
    protected String certificateConfigName;
    protected String configName;
    protected SecurityConfiguration config;
    protected SecurityConfiguration certificateConfig;
    protected ActiveMQServer server;

    public ActiveMQORSecurityManager(AuthorisationService authorisationService, MQTTBrokerService brokerService, Function<String, KeycloakDeployment> deploymentResolver, String configurationName, SecurityConfiguration configuration) {
        super(configurationName, configuration);
        this.authorisationService = authorisationService;
        this.brokerService = brokerService;
        this.deploymentResolver = deploymentResolver;
        this.configName = configurationName;
        this.config = configuration;
    }

    @Override
    public Subject authenticate(String user, String password, RemotingConnection remotingConnection, String securityDomain) {
        try {
            return remotingConnection.getSubject() != null ? remotingConnection.getSubject() : getAuthenticatedSubject(user, password, remotingConnection, securityDomain);
        } catch (LoginException e) {
            return null;
        }
    }

    protected Subject getAuthenticatedSubject(String user,
                                            String password,
                                            final RemotingConnection remotingConnection,
                                            final String securityDomain) throws LoginException {
        LoginContext lc;
        String realm = null;
        ClassLoader currentLoader = Thread.currentThread().getContextClassLoader();
        ClassLoader thisLoader = this.getClass().getClassLoader();

        if (user != null) {
            String[] realmAndUsername = user.split(":");
            if (realmAndUsername.length == 2) {
                realm = realmAndUsername[0];
                user = realmAndUsername[1];
            }
        }

        try {
            if (thisLoader != currentLoader) {
                Thread.currentThread().setContextClassLoader(thisLoader);
            }
            if (securityDomain != null) {
                lc = new LoginContext(securityDomain, null, new MultiTenantJaasCallbackHandler(deploymentResolver, realm, user, password, remotingConnection), null);
            } else if (certificateConfigName != null && certificateConfigName.length() > 0 && getCertsFromConnection(remotingConnection) != null) {
                lc = new LoginContext(certificateConfigName, null, new MultiTenantJaasCallbackHandler(deploymentResolver, realm, user, password, remotingConnection), certificateConfig);
            } else {
                lc = new LoginContext(configName, null, new MultiTenantJaasCallbackHandler(deploymentResolver, realm, user, password, remotingConnection), config);
            }
            try {
                lc.login();
            } catch (LoginException e) {
                throw e;
            }
            Subject subject = lc.getSubject();

            if (subject != null) {
                // Set subject here so any code that calls this method behaves like a normal ActiveMQ SecurityStoreImpl::authenticate call
                remotingConnection.setSubject(subject);
                subject.getPrincipals().add(new RemotingConnectionPrincipal(remotingConnection));
            }

            return subject;
        } finally {
            if (thisLoader != currentLoader) {
                Thread.currentThread().setContextClassLoader(currentLoader);
            }
        }
    }

    @Override
    public boolean authorize(Subject subject, Set<Role> roles, CheckType checkType, String address) {

        return switch (checkType) {
            case SEND -> verifyRights(subject, address, true);
            case CONSUME -> {
                int index = address.indexOf("::");
                address = address.substring(0, index);
                yield verifyRights(subject, address, false);
            }
            case CREATE_ADDRESS, DELETE_ADDRESS, CREATE_DURABLE_QUEUE, DELETE_DURABLE_QUEUE, CREATE_NON_DURABLE_QUEUE, DELETE_NON_DURABLE_QUEUE ->
                // All MQTT clients must be able to create addresses and queues (every session and subscription will create a queue within the topic address)
                true;
            case MANAGE, BROWSE -> false;
        };
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    protected boolean verifyRights(Subject subject, String address, boolean isWrite) {
        Topic topic;

        try {
            // Get MQTT topic from address
            topic = Topic.fromAddress(address, brokerService.getWildcardConfiguration());
        } catch (IllegalArgumentException e) {
            LOG.log(Level.FINE, "Invalid topic provided by client '" + address, e);
            return false;
        }

        KeycloakSecurityContext securityContext = KeycloakIdentityProvider.getSecurityContext(subject);
        String topicClientID = MQTTHandler.topicClientID(topic);

        if (topicClientID == null) {
            LOG.fine("Client ID not found but it must be included as the second token in the topic: topic=" + topic);
            return false;
        }

        RemotingConnection connection = RemotingConnectionPrincipal.getRemotingConnectionFromSubject(subject);

        if (connection == null) {
            LOG.info("Failed to find connection for the specified client ID: clientID=" + topicClientID);
            return false;
        }

        if (isWrite && topic.hasWildcard()) {
            return false;
        }

        // See if a custom handler wants to handle authorisation for this topic pub/sub
        for (MQTTHandler handler : brokerService.getCustomHandlers()) {
            if (handler.handlesTopic(topic)) {
                LOG.finest("Passing topic to handler for " + (isWrite ? "pub" : "sub") + ": handler=" + handler.getName() + ", topic=" + topic + ", " + connectionToString(connection));
                boolean result;

                if (isWrite) {
                    result = handler.checkCanPublish(connection, securityContext, topic);
                } else {
                    result = handler.checkCanSubscribe(connection, securityContext, topic);
                }
                if (result) {
                    LOG.finest("Handler '" + handler.getName() + "' has authorised " + (isWrite ? "pub" : "sub") + ": topic=" + topic + ", " + connectionToString(connection));
                } else {
                    LOG.finest("Handler '" + handler.getName() + "' has not authorised " + (isWrite ? "pub" : "sub") + ": topic=" + topic + ", " + connectionToString(connection));
                }
                return result;
            }
        }

        LOG.info("Un-supported request " + (isWrite ? "pub" : "sub") + ": topic=" + topic + ", " + connectionToString(connection));
        return false;
    }

}
