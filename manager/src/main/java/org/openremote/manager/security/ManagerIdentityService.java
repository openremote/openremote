/*
 * Copyright 2017, OpenRemote Inc.
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
package org.openremote.manager.security;

import static org.openremote.model.util.MapAccess.getString;

import java.util.Locale;
import java.util.logging.Logger;

import org.openremote.container.security.IdentityService;
import org.openremote.container.timer.TimerService;
import org.openremote.manager.mqtt.MQTTBrokerService;
import org.openremote.manager.web.ManagerWebService;
import org.openremote.model.Container;

public class ManagerIdentityService extends IdentityService {

  private static final Logger LOG = Logger.getLogger(ManagerIdentityService.class.getName());

  protected ManagerIdentityProvider identityProvider;

  @Override
  public void init(Container container) throws Exception {
    super.init(container);
    MQTTBrokerService mqttBrokerService = container.getService(MQTTBrokerService.class);
    ManagerWebService managerWebService = container.getService(ManagerWebService.class);

    managerWebService.addApiSingleton(
        new RealmResourceImpl(container.getService(TimerService.class), this, container));
    managerWebService.addApiSingleton(
        new UserResourceImpl(container.getService(TimerService.class), this, mqttBrokerService));
  }

  public ManagerIdentityProvider getIdentityProvider() {
    return identityProvider;
  }

  @Override
  public ManagerIdentityProvider createIdentityProvider(Container container) {
    if (identityProvider == null) {
      String identityProviderType =
          getString(container.getConfig(), OR_IDENTITY_PROVIDER, OR_IDENTITY_PROVIDER_DEFAULT);

      switch (identityProviderType.toLowerCase(Locale.ROOT)) {
        case "keycloak" -> {
          LOG.info("Enabling Keycloak identity provider");
          this.identityProvider = new ManagerKeycloakIdentityProvider();
        }
        case "basic" -> {
          LOG.info("Enabling basic identity provider");
          this.identityProvider = new ManagerBasicIdentityProvider();
        }
        default ->
            throw new UnsupportedOperationException(
                "Unknown identity provider: " + identityProviderType);
      }
    }
    return identityProvider;
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "{" + "identityProvider=" + identityProvider + '}';
  }
}
