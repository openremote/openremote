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
package org.openremote.manager.security;

import org.apache.activemq.artemis.spi.core.protocol.RemotingConnection;
import org.apache.activemq.artemis.spi.core.security.jaas.CertificateCallback;
import org.apache.activemq.artemis.spi.core.security.jaas.JaasCallbackHandler;
import org.apache.activemq.artemis.spi.core.security.jaas.PrincipalsCallback;
import org.keycloak.adapters.KeycloakDeployment;

import javax.security.auth.Subject;
import javax.security.auth.callback.*;
import javax.security.auth.kerberos.KerberosPrincipal;
import java.io.IOException;
import java.security.Principal;
import java.util.Set;
import java.util.function.Function;

import static org.apache.activemq.artemis.core.remoting.CertificateUtil.getCertsFromConnection;
import static org.apache.activemq.artemis.core.remoting.CertificateUtil.getPeerPrincipalFromConnection;

/**
 * A version of {@link JaasCallbackHandler} that supports multi-tenancy by introducing support for
 * {@link KeycloakDeploymentCallback}. Unfortunately the fields are private so had to copy most of the code below.
 */
public class MultiTenantJaasCallbackHandler implements CallbackHandler {

    protected final String username;
    protected final String password;
    final RemotingConnection remotingConnection;
    protected final String realm;
    protected final Function<String, KeycloakDeployment> deploymentResolver;

    public MultiTenantJaasCallbackHandler(Function<String, KeycloakDeployment> deploymentResolver, String realm, String username, String password, RemotingConnection remotingConnection) {
        this.username = username;
        this.password = password;
        this.remotingConnection = remotingConnection;
        this.realm = realm;
        this.deploymentResolver = deploymentResolver;
    }

    @Override
    public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
        for (Callback callback : callbacks) {
            if (callback instanceof PasswordCallback) {
                PasswordCallback passwordCallback = (PasswordCallback) callback;
                if (password == null) {
                    passwordCallback.setPassword(null);
                } else {
                    passwordCallback.setPassword(password.toCharArray());
                }
            } else if (callback instanceof NameCallback) {
                NameCallback nameCallback = (NameCallback) callback;
                if (username == null) {
                    nameCallback.setName(null);
                } else {
                    nameCallback.setName(username);
                }
            } else if (callback instanceof CertificateCallback) {
                CertificateCallback certCallback = (CertificateCallback) callback;

                certCallback.setCertificates(getCertsFromConnection(remotingConnection));
            } else if (callback instanceof PrincipalsCallback) {
                PrincipalsCallback principalsCallback = (PrincipalsCallback) callback;

                Subject peerSubject = remotingConnection.getSubject();
                if (peerSubject != null) {
                    for (KerberosPrincipal principal : peerSubject.getPrivateCredentials(KerberosPrincipal.class)) {
                        principalsCallback.setPeerPrincipals(new Principal[]{principal});
                        return;
                    }
                    Set<Principal> principals = peerSubject.getPrincipals();
                    if (principals.size() > 0) {
                        principalsCallback.setPeerPrincipals(principals.toArray(new Principal[0]));
                        return;
                    }
                }

                Principal peerPrincipalFromConnection = getPeerPrincipalFromConnection(remotingConnection);
                if (peerPrincipalFromConnection != null) {
                    principalsCallback.setPeerPrincipals(new Principal[]{peerPrincipalFromConnection});
                }
            } else if (callback instanceof KeycloakDeploymentCallback keycloakDeploymentCallback) {
                keycloakDeploymentCallback.setDeployment(deploymentResolver.apply(realm));
            } else {
                throw new UnsupportedCallbackException(callback);
            }
        }
    }
}
