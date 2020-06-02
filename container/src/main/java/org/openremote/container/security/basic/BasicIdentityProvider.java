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

import io.undertow.security.idm.Account;
import io.undertow.security.idm.Credential;
import io.undertow.security.idm.IdentityManager;
import io.undertow.security.idm.PasswordCredential;
import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.LoginConfig;
import org.openremote.container.persistence.PersistenceService;
import org.openremote.container.security.IdentityProvider;
import org.openremote.model.Constants;

import javax.persistence.NoResultException;
import javax.persistence.NonUniqueResultException;
import java.security.Principal;
import java.util.Set;
import java.util.logging.Logger;

public abstract class BasicIdentityProvider implements IdentityProvider {

    private static final Logger LOG = Logger.getLogger(BasicIdentityProvider.class.getName());

    final protected PersistenceService persistenceService;

    public BasicIdentityProvider(PersistenceService persistenceService) {
        this.persistenceService = persistenceService;
        // Add schema and scripts for the PUBLIC realm to replicate keycloak user tables
        this.persistenceService.getDefaultSchemaLocations().add(
            "classpath:org/openremote/container/persistence/schema/basicidentityprovider"
        );
        this.persistenceService.getSchemas().add("public");
    }

    @Override
    public void init() {

    }

    @Override
    public void start() {

    }

    @Override
    public void stop() {

    }

    @Override
    public void secureDeployment(DeploymentInfo deploymentInfo) {
        LoginConfig loginConfig = new LoginConfig("BASIC", "OpenRemote");
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
