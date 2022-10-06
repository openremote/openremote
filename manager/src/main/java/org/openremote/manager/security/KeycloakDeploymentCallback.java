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

import org.keycloak.adapters.KeycloakDeployment;

import javax.security.auth.callback.Callback;

/**
 * A {@link Callback} that should return the {@link org.keycloak.adapters.KeycloakDeployment} for the specified realm
 */
public class KeycloakDeploymentCallback implements Callback, java.io.Serializable {

    protected KeycloakDeployment deployment;

    public KeycloakDeploymentCallback setDeployment(KeycloakDeployment deployment) {
        this.deployment = deployment;
        return this;
    }

    public KeycloakDeployment getDeployment() {
        return deployment;
    }
}
