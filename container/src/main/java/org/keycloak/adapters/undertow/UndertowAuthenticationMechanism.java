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

package org.keycloak.adapters.undertow;

import io.undertow.security.api.SecurityContext;
import io.undertow.server.HttpServerExchange;
import org.keycloak.adapters.*;
import org.keycloak.adapters.undertow.UndertowHttpFacade;
import org.keycloak.adapters.undertow.UndertowUserSessionManagement;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class UndertowAuthenticationMechanism extends AbstractUndertowKeycloakAuthMech {
    protected NodesRegistrationManagement nodesRegistrationManagement;
    protected int confidentialPort;

    public UndertowAuthenticationMechanism(AdapterDeploymentContext deploymentContext, UndertowUserSessionManagement sessionManagement,
                                           NodesRegistrationManagement nodesRegistrationManagement, int confidentialPort, String errorPage) {
        super(deploymentContext, sessionManagement, errorPage);
        this.nodesRegistrationManagement = nodesRegistrationManagement;
        this.confidentialPort = confidentialPort;
    }

    @Override
    public AuthenticationMechanismOutcome authenticate(HttpServerExchange exchange, SecurityContext securityContext) {
        UndertowHttpFacade facade = createFacade(exchange);
        KeycloakDeployment deployment = deploymentContext.resolveDeployment(facade);
        if (!deployment.isConfigured()) {
            return AuthenticationMechanismOutcome.NOT_ATTEMPTED;
        }

        nodesRegistrationManagement.tryRegister(deployment);

        AdapterTokenStore tokenStore = getTokenStore(exchange, facade, deployment, securityContext);
        RequestAuthenticator authenticator = new UndertowRequestAuthenticator(facade, deployment, confidentialPort, securityContext, exchange, tokenStore);

        return keycloakAuthenticate(exchange, securityContext, authenticator);
    }

}
