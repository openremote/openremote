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

import org.apache.activemq.artemis.core.config.WildcardConfiguration;
import org.apache.activemq.artemis.core.config.impl.SecurityConfiguration;
import org.apache.activemq.artemis.core.protocol.mqtt.MQTTUtil;
import org.apache.activemq.artemis.core.security.CheckType;
import org.apache.activemq.artemis.core.security.Role;
import org.apache.activemq.artemis.core.server.ActiveMQServer;
import org.apache.activemq.artemis.spi.core.protocol.RemotingConnection;
import org.apache.activemq.artemis.spi.core.security.ActiveMQJAASSecurityManager;
import org.keycloak.KeycloakSecurityContext;
import org.keycloak.adapters.KeycloakDeployment;
import org.openremote.container.security.keycloak.KeycloakIdentityProvider;
import org.openremote.manager.security.ManagerIdentityProvider;
import org.openremote.manager.security.MultiTenantJaasCallbackHandler;
import org.openremote.manager.security.RemotingConnectionPrincipal;
import org.openremote.model.protocol.mqtt.Topic;
import org.openremote.model.security.User;
import org.openremote.model.syslog.SyslogCategory;

import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;
import javax.security.auth.Subject;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;
import java.security.cert.X509Certificate;
import java.util.Set;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.apache.activemq.artemis.core.remoting.CertificateUtil.getCertsFromConnection;
import static org.openremote.model.security.User.SERVICE_ACCOUNT_PREFIX;
import static org.openremote.model.syslog.SyslogCategory.API;
import static org.openremote.manager.mqtt.MQTTBrokerService.connectionToString;

/**
 * A security manager that uses the {@link org.openremote.manager.security.MultiTenantJaasCallbackHandler} with a
 * dynamic {@link org.keycloak.adapters.KeycloakDeployment} resolver.
 *
 * Unfortunately lots of private methods and fields in super class.
 */
public class ActiveMQORSecurityManager extends ActiveMQJAASSecurityManager {

    private static final Logger LOG = SyslogCategory.getLogger(API, ActiveMQORSecurityManager.class);
    protected ManagerIdentityProvider identityProvider;
    protected MQTTBrokerService brokerService;
    protected Function<String, KeycloakDeployment> deploymentResolver;

    // Duplicate fields due to being private in super class
    protected String certificateConfigName;
    protected String configName;
    protected SecurityConfiguration config;
    protected SecurityConfiguration certificateConfig;
    protected ActiveMQServer server;

    public ActiveMQORSecurityManager(ManagerIdentityProvider identityProvider, MQTTBrokerService brokerService, Function<String, KeycloakDeployment> deploymentResolver, String configurationName, SecurityConfiguration configuration) {
        super(configurationName, configuration);
        this.identityProvider = identityProvider;
        this.brokerService = brokerService;
        this.deploymentResolver = deploymentResolver;
        this.configName = configurationName;
        this.config = configuration;
    }

    protected static Topic fromAddress(String address, WildcardConfiguration wildcardConfiguration) throws IllegalArgumentException {
        return Topic.parse(MQTTUtil.getMqttTopicFromCoreAddress(address, wildcardConfiguration));
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
        X509Certificate[] certs = getCertsFromConnection(remotingConnection);
        if (user != null) {
            String[] realmAndUsername = user.split(":");
            if (realmAndUsername.length == 2) {
                realm = realmAndUsername[0];
                user = realmAndUsername[1];
            }
        }else if (certs != null && certs.length > 0) {

            X509Certificate leaf = certs[0];
            String dn = leaf.getSubjectX500Principal().getName();
            try {
                // Check for Client Authentication EKU (This denotes that this is the actual client certificate to enforce)
                if(!leaf.getExtendedKeyUsage().contains("1.3.6.1.5.5.7.3.2")) {
                    // TODO: Not sure about what extent to which we would like to enforce this.
                    // I have seen codebases use this EKU key to ensure that the certificate they are examining for client
                    // auth is the correct one to inspect.I will have to perform some more research about it, but I think
                    // that the client certificate is always going to be the leaf. For now, log a warning about it.
                    LOG.log(Level.WARNING, "Presented certificate DOES NOT have Client Authentication Extended Key Usage. " +
                            "Attempting to use, but please fix this for subsequent requests.");
                }
                LdapName ldapName = new LdapName(dn);
                for (Rdn rdn : ldapName.getRdns()) {
                    String type = rdn.getType();
                    String value = rdn.getValue().toString();
                    if ("OU".equalsIgnoreCase(type) && realm == null) {
                        realm = value;
                    } else if ("CN".equalsIgnoreCase(type) && (user == null || user.isEmpty())) {
                        user = value;
                    }
                }
            } catch (InvalidNameException e) {
                LOG.log(Level.FINE, "Failed to parse subject DN from client certificate: " + dn, e);
            } catch (Exception e) {
                LOG.log(Level.SEVERE, "Failed to process given client certificate");
            }

            if(!(user == null || user.isEmpty())) {
                User dbUser = identityProvider.getUserByUsername(
                        realm,
                        SERVICE_ACCOUNT_PREFIX + user
                );
                if (dbUser != null) {
                    password = dbUser.getSecret();
                } else {
                    LOG.log(Level.WARNING, "Client certificate was provided, but no service user found. " +
                            "Allowing anonymous login for autoprovisioning. username=" + user);
                }
            } else {
                LOG.log(Level.WARNING, "Client certificate was provided, but no username found in certificate subject. " +
                        "Allowing anonymous login for autoprovisioning. Subject DN=" + dn);
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
                LOG.log(Level.WARNING, "Failed to authenticate user: " + user, e);
                throw e;
            }
            Subject subject = lc.getSubject();

            LOG.warning(subject.toString());

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
            case MANAGE, BROWSE, VIEW, EDIT -> false;
        };
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    protected boolean verifyRights(Subject subject, String address, boolean isWrite) {
        Topic topic;

        try {
            // Get MQTT topic from address
            topic = fromAddress(address, brokerService.getWildcardConfiguration());
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
