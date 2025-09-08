/*
 * Copyright 2023, OpenRemote Inc.
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
package org.openremote.container.security.keycloak;

import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import org.keycloak.adapters.NodesRegistrationManagement;

/**
 * This is a copy of the same class from the now obsolete keycloak-undertow-adapter but for jakarta EE.
 *
 */
// TODO: Move to a standard OIDC adapter
public class UndertowNodesRegistrationManagementWrapper implements ServletContextListener {

    private final NodesRegistrationManagement delegate;

    public UndertowNodesRegistrationManagementWrapper(NodesRegistrationManagement delegate) {
        this.delegate = delegate;
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        delegate.stop();
    }
}
