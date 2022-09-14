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
import org.apache.activemq.artemis.spi.core.protocol.RemotingConnection;
import org.apache.activemq.artemis.spi.core.security.ActiveMQJAASSecurityManager;
import org.keycloak.adapters.KeycloakDeployment;
import org.openremote.manager.security.MultiTenantJaasCallbackHandler;

import javax.security.auth.Subject;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

import java.util.function.Function;

import static org.apache.activemq.artemis.core.remoting.CertificateUtil.getCertsFromConnection;

/**
 * A security manager that uses the {@link org.openremote.manager.security.MultiTenantJaasCallbackHandler} with a
 * dynamic {@link org.keycloak.adapters.KeycloakDeployment} resolver.
 *
 * Unfortunately lots of private methods and fields in super class.
 */
public class ActiveMQORSecurityManager extends ActiveMQJAASSecurityManager {

    // Duplicate fields due to being private in super class
    protected String certificateConfigName;
    protected String configName;
    protected SecurityConfiguration config;
    protected SecurityConfiguration certificateConfig;

    protected Function<String, KeycloakDeployment> deploymentResolver;

    public ActiveMQORSecurityManager(Function<String, KeycloakDeployment> deploymentResolver) {
        this.deploymentResolver = deploymentResolver;
    }

    public ActiveMQORSecurityManager(Function<String, KeycloakDeployment> deploymentResolver, String configurationName) {
        super(configurationName);
        this.deploymentResolver = deploymentResolver;
        this.configName = configurationName;
    }

    public ActiveMQORSecurityManager(Function<String, KeycloakDeployment> deploymentResolver, String configurationName, String certificateConfigurationName) {
        super(configurationName, certificateConfigurationName);
        this.deploymentResolver = deploymentResolver;
        this.configName = configurationName;
        this.certificateConfigName = certificateConfigurationName;
    }

    public ActiveMQORSecurityManager(Function<String, KeycloakDeployment> deploymentResolver, String configurationName, SecurityConfiguration configuration) {
        super(configurationName, configuration);
        this.deploymentResolver = deploymentResolver;
        this.configName = configurationName;
        this.config = configuration;
    }

    public ActiveMQORSecurityManager(Function<String, KeycloakDeployment> deploymentResolver, String configurationName, String certificateConfigurationName, SecurityConfiguration configuration, SecurityConfiguration certificateConfiguration) {
        super(configurationName, certificateConfigurationName, configuration, certificateConfiguration);
        this.deploymentResolver = deploymentResolver;
        this.configName = configurationName;
        this.certificateConfigName = certificateConfigurationName;
        this.config = configuration;
        this.certificateConfig = certificateConfiguration;
    }

    @Override
    public Subject authenticate(String user, String password, RemotingConnection remotingConnection, String securityDomain) {
        try {
            return getAuthenticatedSubject(user, password, remotingConnection, securityDomain);
        } catch (LoginException e) {
            return null;
        }
    }

    private Subject getAuthenticatedSubject(String user,
                                            final String password,
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
            return lc.getSubject();
        } finally {
            if (thisLoader != currentLoader) {
                Thread.currentThread().setContextClassLoader(currentLoader);
            }
        }
    }
}
