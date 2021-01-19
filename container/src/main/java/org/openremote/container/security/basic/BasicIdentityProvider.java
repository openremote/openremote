/*
 * Copyright 2017, OpenRemote Inc.
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
package org.openremote.container.security.basic;

import io.undertow.UndertowMessages;
import io.undertow.security.api.AuthenticationMechanism;
import io.undertow.security.api.AuthenticationMechanismFactory;
import io.undertow.security.api.SecurityContext;
import io.undertow.security.idm.Account;
import io.undertow.security.idm.Credential;
import io.undertow.security.idm.IdentityManager;
import io.undertow.security.idm.PasswordCredential;
import io.undertow.security.impl.BasicAuthenticationMechanism;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.form.FormParserFactory;
import io.undertow.servlet.api.AuthMethodConfig;
import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.LoginConfig;
import org.openremote.model.Container;
import org.openremote.container.persistence.PersistenceService;
import org.openremote.container.security.IdentityProvider;
import org.openremote.model.Constants;

import javax.persistence.NoResultException;
import javax.persistence.NonUniqueResultException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import static io.undertow.util.Headers.AUTHORIZATION;

public abstract class BasicIdentityProvider implements IdentityProvider {

    /**
     * This is a fix for {@link BasicAuthenticationMechanism} which doesn't conform to RFC2617.
     * see: https://issues.redhat.com/browse/UNDERTOW-1727
     * <p>
     * When no {@link io.undertow.util.Headers#AUTHORIZATION} header is supplied then a 401 is returned with
     * {@link io.undertow.util.Headers#WWW_AUTHENTICATE} header unless {@link #silent} is true in which case
     * a 403 will be returned.
     * <p>
     * When an {@link io.undertow.util.Headers#AUTHORIZATION} header is supplied and is valid then the request
     * can proceed otherwise a 403 is returned.
     */
    protected static class BasicFixAuthenticationMechanism extends BasicAuthenticationMechanism {

        protected static class Factory implements AuthenticationMechanismFactory {
            @Override
            public AuthenticationMechanism create(String mechanismName, IdentityManager identityManager, FormParserFactory formParserFactory, Map<String, String> properties) {
                String realm = properties.get(REALM);
                String silent = properties.get(SILENT);
                String charsetString = properties.get(CHARSET);
                Charset charset = charsetString == null ? StandardCharsets.UTF_8 : Charset.forName(charsetString);
                Map<Pattern, Charset> userAgentCharsets = new HashMap<>();
                String userAgentString = properties.get(USER_AGENT_CHARSETS);
                if(userAgentString != null) {
                    String[] parts = userAgentString.split(",");
                    if(parts.length % 2 != 0) {
                        throw UndertowMessages.MESSAGES.userAgentCharsetMustHaveEvenNumberOfItems(userAgentString);
                    }
                    for(int i = 0; i < parts.length; i += 2) {
                        Pattern pattern = Pattern.compile(parts[i]);
                        Charset c = Charset.forName(parts[i + 1]);
                        userAgentCharsets.put(pattern, c);
                    }
                }
                return new BasicFixAuthenticationMechanism(realm, mechanismName, silent != null && silent.equals("true"), identityManager, charset, userAgentCharsets);
            }
        }

        // field is private in super class so need to redefine!!!
        private final boolean silent;
        public static Factory FACTORY = new Factory();

        public BasicFixAuthenticationMechanism(String realmName, String mechanismName, boolean silent, IdentityManager identityManager, Charset charset, Map<Pattern, Charset> userAgentCharsets) {
            super(realmName, mechanismName, false, identityManager, charset, userAgentCharsets);
            this.silent = silent;
        }

        @Override
        public AuthenticationMechanismOutcome authenticate(HttpServerExchange exchange, SecurityContext securityContext) {
            String authHeader = exchange.getRequestHeaders().getFirst(AUTHORIZATION);

            if (authHeader == null) {
                if (silent) {
                    return AuthenticationMechanismOutcome.NOT_ATTEMPTED;
                } else {
                    return AuthenticationMechanismOutcome.NOT_AUTHENTICATED;
                }
            }
            return super.authenticate(exchange, securityContext);
        }

        @Override
        public ChallengeResult sendChallenge(HttpServerExchange exchange, SecurityContext securityContext) {
            String authHeader = exchange.getRequestHeaders().getFirst(AUTHORIZATION);
            if (silent || authHeader != null) {
                return ChallengeResult.NOT_SENT;
            }
            return super.sendChallenge(exchange, securityContext);
        }
    }

    private static final Logger LOG = Logger.getLogger(BasicIdentityProvider.class.getName());
    protected PersistenceService persistenceService;

    @Override
    public void init(Container container) {
        this.persistenceService = container.getService(PersistenceService.class);
        // Add schema and scripts for the PUBLIC realm to replicate keycloak user tables
        this.persistenceService.getDefaultSchemaLocations().add(
            "classpath:org/openremote/container/persistence/schema/basicidentityprovider"
        );
        this.persistenceService.getSchemas().add("public");
    }

    @Override
    public void start(Container container) {

    }

    @Override
    public void stop(Container container) {

    }

    @Override
    public void secureDeployment(DeploymentInfo deploymentInfo) {
        LoginConfig loginConfig = new LoginConfig("OpenRemote");
        // Make it silent to prevent 401 WWW-Authenticate modal dialog
        deploymentInfo.addAuthenticationMechanism("BASIC-FIX", BasicFixAuthenticationMechanism.FACTORY);
        loginConfig.addFirstAuthMethod(new AuthMethodConfig("BASIC-FIX", Collections.singletonMap("silent", "true")));
        deploymentInfo.setLoginConfig(loginConfig);
        deploymentInfo.setIdentityManager(new IdentityManager() {
            @Override
            public Account verify(Account account) {
                return null;
            }

            @Override
            public Account verify(String id, Credential credential) {
                if (credential instanceof PasswordCredential) {
                    PasswordCredential passwordCredential = (PasswordCredential) credential;
                    return verifyAccount(id, passwordCredential.getPassword());
                } else {
                    LOG.fine("Verification of '" + id + "' failed, no password credentials found, but: " + credential);
                    return null;
                }
            }

            @Override
            public Account verify(Credential credential) {
                return null;
            }
        });
    }

    protected Account verifyAccount(String username, char[] password) {
        LOG.fine("Authentication attempt, querying user: " + username);
        Object[] idAndPassword = persistenceService.doReturningTransaction(em -> {
                try {
                    return (Object[]) em.createNativeQuery(
                        "select U.ID, U.PASSWORD from PUBLIC.USER_ENTITY U where U.USERNAME = :username"
                    ).setParameter("username", username).getSingleResult();
                } catch (NoResultException | NonUniqueResultException ex) {
                    return null;
                }
            }
        );
        if (idAndPassword == null) {
            LOG.fine("Authentication failed, no such user: " + username);
            return null;
        }
        LOG.fine("Authentication attempt, verifying password: " + username);
        if (!PasswordStorage.verifyPassword(password, idAndPassword[1].toString())) {
            LOG.fine("Authentication failed, invalid password: " + username);
            return null;
        }

        LOG.fine("Authentication successful: " + username);
        final BasicAuthContext principal = new BasicAuthContext(Constants.MASTER_REALM, idAndPassword[0].toString(), username);
        return new Account() {
            @Override
            public Principal getPrincipal() {
                return principal;
            }

            @Override
            public Set<String> getRoles() {
                return getDefaultRoles();
            }
        };
    }

    abstract protected Set<String> getDefaultRoles();
}
