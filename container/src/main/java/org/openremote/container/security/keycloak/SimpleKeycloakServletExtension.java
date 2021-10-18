/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openremote.container.security.keycloak;

import io.undertow.security.api.AuthenticationMechanism;
import io.undertow.security.api.AuthenticationMechanismFactory;
import io.undertow.security.idm.Account;
import io.undertow.security.idm.Credential;
import io.undertow.security.idm.IdentityManager;
import io.undertow.server.handlers.form.FormParserFactory;
import io.undertow.servlet.ServletExtension;
import io.undertow.servlet.api.*;
import io.undertow.servlet.util.ImmediateInstanceHandle;
import org.jboss.logging.Logger;
import org.keycloak.adapters.AdapterDeploymentContext;
import org.keycloak.adapters.KeycloakConfigResolver;
import org.keycloak.adapters.NodesRegistrationManagement;
import org.keycloak.adapters.undertow.*;

import javax.servlet.ServletContext;
import java.util.Map;

/**
 * Allow Java setup of config resolver. We don't want your stupid text files and properties.
 *
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class SimpleKeycloakServletExtension implements ServletExtension {

    public static final String AUTH_MECHANISM = "SIMPLE_KEYCLOAK";

    protected static Logger log = Logger.getLogger(KeycloakServletExtension.class);

    final protected KeycloakConfigResolver configResolver;
    final protected AdapterDeploymentContext deploymentContext;

    public SimpleKeycloakServletExtension(KeycloakConfigResolver configResolver) {
        this.configResolver = configResolver;
        deploymentContext = new AdapterDeploymentContext(configResolver);
    }

    @Override
    @SuppressWarnings("UseSpecificCatch")
    public void handleDeployment(DeploymentInfo deploymentInfo, ServletContext servletContext) {

        servletContext.setAttribute(AdapterDeploymentContext.class.getName(), deploymentContext);
        UndertowUserSessionManagement userSessionManagement = new UndertowUserSessionManagement();
        final NodesRegistrationManagement nodesRegistrationManagement = new NodesRegistrationManagement();
        final ServletKeycloakAuthMech mech = createAuthenticationMechanism(deploymentInfo, deploymentContext, userSessionManagement, nodesRegistrationManagement);

        UndertowAuthenticatedActionsHandler.Wrapper actions = new UndertowAuthenticatedActionsHandler.Wrapper(deploymentContext);

        // setup handlers

        deploymentInfo.addOuterHandlerChainWrapper(new ServletPreAuthActionsHandler.Wrapper(deploymentContext, userSessionManagement));
        deploymentInfo.addAuthenticationMechanism(AUTH_MECHANISM, new AuthenticationMechanismFactory() {
            @Override
            public AuthenticationMechanism create(String s, IdentityManager identityManager, FormParserFactory formParserFactory, Map<String, String> stringStringMap) {
                return mech;
            }
        }); // authentication
        deploymentInfo.addInnerHandlerChainWrapper(actions); // handles authenticated actions and cors.

        deploymentInfo.setIdentityManager(new IdentityManager() {
            @Override
            public Account verify(Account account) {
                return account;
            }

            @Override
            public Account verify(String id, Credential credential) {
                throw new IllegalStateException("Should never be called in Keycloak flow");
            }

            @Override
            public Account verify(Credential credential) {
                throw new IllegalStateException("Should never be called in Keycloak flow");
            }
        });

        ServletSessionConfig cookieConfig = deploymentInfo.getServletSessionConfig();
        if (cookieConfig == null) {
            cookieConfig = new ServletSessionConfig();
        }
        if (cookieConfig.getPath() == null) {
            log.debug("Setting jsession cookie path to: " + deploymentInfo.getContextPath());
            cookieConfig.setPath(deploymentInfo.getContextPath());
            deploymentInfo.setServletSessionConfig(cookieConfig);
        }
        ChangeSessionId.turnOffChangeSessionIdOnLogin(deploymentInfo);

        deploymentInfo.addListener(new ListenerInfo(UndertowNodesRegistrationManagementWrapper.class, (InstanceFactory<UndertowNodesRegistrationManagementWrapper>) () -> {
            UndertowNodesRegistrationManagementWrapper listener = new UndertowNodesRegistrationManagementWrapper(nodesRegistrationManagement);
            return new ImmediateInstanceHandle<>(listener);
        }));
    }

    protected ServletKeycloakAuthMech createAuthenticationMechanism(DeploymentInfo deploymentInfo, AdapterDeploymentContext deploymentContext, UndertowUserSessionManagement userSessionManagement,
                                                                    NodesRegistrationManagement nodesRegistrationManagement) {
        log.debug("creating ServletKeycloakAuthMech");
        String errorPage = getErrorPage(deploymentInfo);
        return new ServletKeycloakAuthMech(deploymentContext, userSessionManagement, nodesRegistrationManagement, deploymentInfo.getConfidentialPortManager(), errorPage);
    }

    protected String getErrorPage(DeploymentInfo deploymentInfo) {
        LoginConfig loginConfig = deploymentInfo.getLoginConfig();
        String errorPage = null;
        if (loginConfig != null) {
            errorPage = loginConfig.getErrorPage();
        }
        return errorPage;
    }

}
