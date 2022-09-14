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

import com.google.common.collect.Sets;
import org.keycloak.KeycloakPrincipal;
import org.keycloak.adapters.AdapterUtils;
import org.keycloak.adapters.RefreshableKeycloakSecurityContext;
import org.keycloak.adapters.jaas.DirectAccessGrantsLoginModule;
import org.keycloak.representations.AccessToken;

import javax.security.auth.Subject;
import javax.security.auth.callback.*;
import javax.security.auth.login.LoginException;
import java.util.Map;
import java.util.Set;

/**
 * Extension of the Keycloak {@link DirectAccessGrantsLoginModule} that supports lookup of
 * {@link org.keycloak.adapters.KeycloakDeployment} by using the {@link KeycloakDeploymentCallback}.
 *
 * Also supports including the realm roles (as well as resource roles) by setting the
 */
public class MultiTenantDirectAccessGrantsLoginModule extends DirectAccessGrantsLoginModule {

    public static final String INCLUDE_REALM_ROLES_OPTION = "includeRealmRoles";
    protected boolean includeRealmRoles;

    @Override
    public void initialize(Subject subject, CallbackHandler callbackHandler, Map<String, ?> sharedState, Map<String, ?> options) {
        super.initialize(subject, callbackHandler, sharedState, options);

        includeRealmRoles = Boolean.parseBoolean((String) options.get(INCLUDE_REALM_ROLES_OPTION));
    }

    @Override
    public boolean login() throws LoginException {
        // get username and password and deployment (if not set)

        boolean hasDeployment = deployment != null;
        Callback[] callbacks = new Callback[(hasDeployment ? 2 : 3)];
        callbacks[0] = new NameCallback("username");
        callbacks[1] = new PasswordCallback("password", false);
        if (!hasDeployment) {
            callbacks[2] = new KeycloakDeploymentCallback();
        }

        try {
            callbackHandler.handle(callbacks);
            String username = ((NameCallback) callbacks[0]).getName();
            char[] tmpPassword = ((PasswordCallback) callbacks[1]).getPassword();
            String password = new String(tmpPassword);
            ((PasswordCallback) callbacks[1]).clearPassword();

            if (!hasDeployment) {
                deployment = ((KeycloakDeploymentCallback) callbacks[2]).getDeployment();

                if (deployment == null) {
                    getLogger().warn("Unable to resolve keycloak deployment");
                    return false;
                }
            }

            Auth auth = doAuth(username, password);
            if (auth != null) {
                this.auth = auth;
                return true;
            } else {
                return false;
            }
        } catch (UnsupportedCallbackException uce) {
            getLogger().warn("Error: " + uce.getCallback().toString()
                + " not available to gather authentication information from the user");
            return false;
        } catch (Exception e) {
            LoginException le = new LoginException(e.toString());
            le.initCause(e);
            throw le;
        }
    }

    @Override
    protected Auth postTokenVerification(String tokenString, AccessToken token) {
        boolean verifyCaller;
        if (deployment.isUseResourceRoleMappings()) {
            verifyCaller = token.isVerifyCaller(deployment.getResourceName());
        } else {
            verifyCaller = token.isVerifyCaller();
        }
        if (verifyCaller) {
            throw new IllegalStateException("VerifyCaller not supported yet in login module");
        }

        RefreshableKeycloakSecurityContext skSession = new RefreshableKeycloakSecurityContext(deployment, null, tokenString, token, null, null, null);
        String principalName = AdapterUtils.getPrincipalName(deployment, token);
        final KeycloakPrincipal<RefreshableKeycloakSecurityContext> principal = new KeycloakPrincipal<RefreshableKeycloakSecurityContext>(principalName, skSession);
        Set<String> roles;
        roles = AdapterUtils.getRolesFromSecurityContext(skSession);

        if (includeRealmRoles && !deployment.isUseResourceRoleMappings()) {
            AccessToken accessToken = skSession.getToken();
            roles = Sets.union(roles, accessToken.getRealmAccess().getRoles());
        }
        return new Auth(principal, roles, tokenString);
    }
}
