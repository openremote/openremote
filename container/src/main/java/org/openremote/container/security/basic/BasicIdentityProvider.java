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
import org.openremote.container.security.IdentityProvider;

import java.util.logging.Logger;

public abstract class BasicIdentityProvider implements IdentityProvider {

    private static final Logger LOG = Logger.getLogger(BasicIdentityProvider.class.getName());

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

    abstract protected Account verifyAccount(String username, char[] password);
}
