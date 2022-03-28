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
package org.openremote.container.security;

import io.undertow.servlet.api.DeploymentInfo;
import org.openremote.model.Container;

/**
 * SPI for implementations used by {@link IdentityService}.
 */
public interface IdentityProvider {

    String OR_ADMIN_PASSWORD = "OR_ADMIN_PASSWORD";
    String OR_ADMIN_PASSWORD_DEFAULT = "secret";

    void init(Container container) throws Exception;

    void start(Container container) throws Exception;

    void stop(Container container) throws Exception;

    void secureDeployment(DeploymentInfo deploymentInfo);
}
