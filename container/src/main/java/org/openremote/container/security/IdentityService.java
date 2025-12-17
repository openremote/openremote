/*
 * Copyright 2016, OpenRemote Inc.
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
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package org.openremote.container.security;

import java.util.logging.Logger;

import org.openremote.container.persistence.PersistenceService;
import org.openremote.container.security.keycloak.KeycloakIdentityProvider;
import org.openremote.model.Container;
import org.openremote.model.ContainerService;

import io.undertow.servlet.api.DeploymentInfo;

public abstract class IdentityService implements ContainerService {

  public static final int PRIORITY = PersistenceService.PRIORITY + 10;
  private static final Logger LOG = Logger.getLogger(IdentityService.class.getName());
  public static final String OR_IDENTITY_PROVIDER = "OR_IDENTITY_PROVIDER";
  public static final String OR_IDENTITY_PROVIDER_DEFAULT = "keycloak";

  // The externally visible address of this installation
  protected IdentityProvider identityProvider;
  protected boolean devMode;

  @Override
  public int getPriority() {
    return PRIORITY;
  }

  @Override
  public void init(Container container) throws Exception {
    devMode = container.isDevMode();
    identityProvider = createIdentityProvider(container);
    identityProvider.init(container);
  }

  @Override
  public void start(Container container) throws Exception {
    identityProvider.start(container);
  }

  @Override
  public void stop(Container container) throws Exception {
    identityProvider.stop(container);
  }

  public void secureDeployment(DeploymentInfo deploymentInfo) {
    LOG.info("Securing web deployment: " + deploymentInfo.getContextPath());
    deploymentInfo.setSecurityDisabled(false);
    identityProvider.secureDeployment(deploymentInfo);
  }

  /** If Keycloak is enabled, support multi-tenancy. */
  public boolean isKeycloakEnabled() {
    return identityProvider instanceof KeycloakIdentityProvider;
  }

  /**
   * To configure the {@link IdentityProvider}, subclasses should override {@link #init(Container)}.
   */
  public abstract IdentityProvider createIdentityProvider(Container container);
}
