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
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;
import org.jboss.logging.Logger;
import org.keycloak.KeycloakPrincipal;
import org.keycloak.OAuth2Constants;
import org.keycloak.adapters.AdapterUtils;
import org.keycloak.adapters.RefreshableKeycloakSecurityContext;
import org.keycloak.adapters.jaas.AbstractKeycloakLoginModule;
import org.keycloak.adapters.rotation.AdapterTokenVerifier;
import org.keycloak.common.VerificationException;
import org.keycloak.protocol.oidc.client.authentication.ClientCredentialsProviderUtils;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.representations.idm.OAuth2ErrorRepresentation;
import org.keycloak.util.JsonSerialization;

import javax.security.auth.Subject;
import javax.security.auth.callback.*;
import javax.security.auth.login.LoginException;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.net.URI;
import java.util.*;

/**
 * A version of the Keycloak {@link org.keycloak.adapters.jaas.AbstractKeycloakLoginModule} that supports client
 * credentials grant and lookup of {@link org.keycloak.adapters.KeycloakDeployment} by using
 * the {@link KeycloakDeploymentCallback}. Copied from {@link org.keycloak.adapters.jaas.DirectAccessGrantsLoginModule}
 * which has private members un-fortunately.
 *
 * Also supports including the realm roles (as well as resource roles) by setting the
 * {@link #INCLUDE_REALM_ROLES_OPTION}.
 */
public class MultiTenantClientCredentialsGrantsLoginModule extends AbstractKeycloakLoginModule {

    public static final String INCLUDE_REALM_ROLES_OPTION = "includeRealmRoles";
    public static final String SCOPE_OPTION = "scope";
    private static final Logger log = Logger.getLogger(MultiTenantClientCredentialsGrantsLoginModule.class);
    protected boolean includeRealmRoles;
    protected String scope;
    protected String refreshToken;

    @Override
    public void initialize(Subject subject, CallbackHandler callbackHandler, Map<String, ?> sharedState, Map<String, ?> options) {
        super.initialize(subject, callbackHandler, sharedState, options);
        this.scope = (String)options.get(SCOPE_OPTION);

        // This is used just for logout
        Iterator<MultiTenantClientCredentialsGrantsLoginModule.RefreshTokenHolder> iterator = subject.getPrivateCredentials(MultiTenantClientCredentialsGrantsLoginModule.RefreshTokenHolder.class).iterator();
        if (iterator.hasNext()) {
            refreshToken = iterator.next().refreshToken;
        }

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
    protected Logger getLogger() {
        return log;
    }

    @Override
    protected Auth doAuth(String username, String password) throws IOException, VerificationException {
        return clientCredentialsAuth(username, password);
    }

    @Override
    public boolean commit() throws LoginException {
        boolean superCommit = super.commit();

        // refreshToken will be saved to privateCreds of Subject for now
        if (refreshToken != null) {
            MultiTenantClientCredentialsGrantsLoginModule.RefreshTokenHolder refreshTokenHolder = new MultiTenantClientCredentialsGrantsLoginModule.RefreshTokenHolder();
            refreshTokenHolder.refreshToken = refreshToken;
            subject.getPrivateCredentials().add(refreshTokenHolder);
        }

        return superCommit;
    }

    protected Auth clientCredentialsAuth(String username, String password) throws IOException, VerificationException {
        String authServerBaseUrl = deployment.getAuthServerBaseUrl();
        HttpPost post = new HttpPost(deployment.getTokenUrl());
        List<NameValuePair> formparams = new ArrayList<NameValuePair>();
        formparams.add(new BasicNameValuePair(OAuth2Constants.GRANT_TYPE, OAuth2Constants.CLIENT_CREDENTIALS));
        formparams.add(new BasicNameValuePair("client_id", username));
        formparams.add(new BasicNameValuePair("client_secret", password));

        if (scope != null) {
            formparams.add(new BasicNameValuePair(OAuth2Constants.SCOPE, scope));
        }

        //ClientCredentialsProviderUtils.setClientCredentials(deployment, post, formparams);

        UrlEncodedFormEntity form = new UrlEncodedFormEntity(formparams, "UTF-8");
        post.setEntity(form);

        HttpClient client = deployment.getClient();
        HttpResponse response = client.execute(post);
        int status = response.getStatusLine().getStatusCode();
        HttpEntity entity = response.getEntity();
        if (status != 200) {
            StringBuilder errorBuilder = new StringBuilder("Login failed. Invalid status: " + status);
            if (entity != null) {
                InputStream is = entity.getContent();
                OAuth2ErrorRepresentation errorRep = JsonSerialization.readValue(is, OAuth2ErrorRepresentation.class);
                errorBuilder.append(", OAuth2 error. Error: " + errorRep.getError())
                    .append(", Error description: " + errorRep.getErrorDescription());
            }
            String error = errorBuilder.toString();
            log.warn(error);
            throw new IOException(error);
        }

        if (entity == null) {
            throw new IOException("No Entity");
        }

        InputStream is = entity.getContent();
        AccessTokenResponse tokenResponse = JsonSerialization.readValue(is, AccessTokenResponse.class);

        // refreshToken will be saved to privateCreds of Subject for now
        refreshToken = tokenResponse.getRefreshToken();

        AdapterTokenVerifier.VerifiedTokens tokens = AdapterTokenVerifier.verifyTokens(tokenResponse.getToken(), tokenResponse.getIdToken(), deployment);
        return postTokenVerification(tokenResponse.getToken(), tokens.getAccessToken());
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


    @Override
    public boolean logout() throws LoginException {
        if (refreshToken != null) {
            try {
                URI logoutUri = deployment.getLogoutUrl().clone().build();
                HttpPost post = new HttpPost(logoutUri);

                List<NameValuePair> formparams = new ArrayList<>();
                AdapterUtils.setClientCredentials(deployment, post, formparams);
                formparams.add(new BasicNameValuePair(OAuth2Constants.REFRESH_TOKEN, refreshToken));

                UrlEncodedFormEntity form = new UrlEncodedFormEntity(formparams, "UTF-8");
                post.setEntity(form);

                HttpClient client = deployment.getClient();
                HttpResponse response = client.execute(post);
                int status = response.getStatusLine().getStatusCode();
                HttpEntity entity = response.getEntity();
                if (status != 204) {
                    StringBuilder errorBuilder = new StringBuilder("Logout of refreshToken failed. Invalid status: " + status);
                    if (entity != null) {
                        InputStream is = entity.getContent();
                        if (status == 400) {
                            OAuth2ErrorRepresentation errorRep = JsonSerialization.readValue(is, OAuth2ErrorRepresentation.class);
                            errorBuilder.append(", OAuth2 error. Error: " + errorRep.getError())
                                .append(", Error description: " + errorRep.getErrorDescription());

                        } else {
                            if (is != null) is.close();
                        }
                    }

                    // Should do something better than warn if logout failed? Perhaps update of refresh tokens on existing subject might be supported too...
                    log.warn(errorBuilder.toString());
                }
            } catch (IOException ioe) {
                log.warn(ioe);
            }
        }

        return super.logout();
    }

    private static class RefreshTokenHolder implements Serializable {
        private String refreshToken;
    }
}
