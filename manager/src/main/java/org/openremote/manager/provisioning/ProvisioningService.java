/*
 * Copyright 2021, OpenRemote Inc.
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
package org.openremote.manager.provisioning;

import static org.openremote.container.persistence.PersistenceService.PERSISTENCE_TOPIC;
import static org.openremote.container.persistence.PersistenceService.isPersistenceEventForEntityType;

import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.camel.builder.RouteBuilder;
import org.openremote.container.message.MessageBrokerService;
import org.openremote.container.persistence.PersistenceService;
import org.openremote.container.timer.TimerService;
import org.openremote.manager.security.ManagerIdentityService;
import org.openremote.manager.web.ManagerWebService;
import org.openremote.model.Container;
import org.openremote.model.ContainerService;
import org.openremote.model.provisioning.ProvisioningConfig;
import org.openremote.model.util.ValueUtil;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;

@SuppressWarnings("rawtypes")
public class ProvisioningService extends RouteBuilder implements ContainerService {

  protected static final Logger LOG = Logger.getLogger(ProvisioningService.class.getName());
  protected PersistenceService persistenceService;
  protected ManagerIdentityService identityService;
  protected List<ProvisioningConfig> provisioningConfigs;

  @Override
  public int getPriority() {
    return 0;
  }

  @Override
  public void configure() throws Exception {
    from(PERSISTENCE_TOPIC)
        .routeId("Persistence-MonitorProvisioningConfigs")
        .filter(isPersistenceEventForEntityType(ProvisioningConfig.class))
        .process(
            exchange -> {
              synchronized (this) {
                provisioningConfigs = null;
              }
            });
  }

  @Override
  public void init(Container container) throws Exception {
    persistenceService = container.getService(PersistenceService.class);
    identityService = container.getService(ManagerIdentityService.class);
    TimerService timerService = container.getService(TimerService.class);

    container
        .getService(ManagerWebService.class)
        .addApiSingleton(new ProvisioningResourceImpl(this, timerService, identityService));

    container.getService(MessageBrokerService.class).getContext().addRoutes(this);
  }

  @Override
  public void start(Container container) throws Exception {}

  @Override
  public void stop(Container container) throws Exception {}

  public <T extends ProvisioningConfig<?, ?>> T merge(T provisioningConfig) {
    return persistenceService.doReturningTransaction(
        entityManager -> {

          // Do standard JSR-380 validation on the config
          Set<ConstraintViolation<ProvisioningConfig<?, ?>>> validationFailures =
              ValueUtil.validate(provisioningConfig);

          if (!validationFailures.isEmpty()) {
            String msg =
                "Provisioning config merge failed as failed constraint validation: config="
                    + provisioningConfig;
            ConstraintViolationException ex = new ConstraintViolationException(validationFailures);
            LOG.log(Level.WARNING, msg + ", exception=" + ex.getMessage(), ex);
            throw ex;
          }

          T mergedConfig = entityManager.merge(provisioningConfig);
          if (provisioningConfig.getId() != null) {
            LOG.fine("Provisioning config updated: " + provisioningConfig);
          } else {
            LOG.fine("Provisioning config created: " + provisioningConfig);
          }

          return mergedConfig;
        });
  }

  public void delete(Long id) {
    persistenceService.doTransaction(
        entityManager -> {
          ProvisioningConfig<?, ?> provisioningConfig =
              entityManager.find(ProvisioningConfig.class, id);
          if (provisioningConfig != null) entityManager.remove(provisioningConfig);
        });
  }

  public synchronized List<ProvisioningConfig> getProvisioningConfigs() {
    if (provisioningConfigs == null) {
      updateProvisioningConfigs();
    }

    return provisioningConfigs;
  }

  protected void updateProvisioningConfigs() {
    provisioningConfigs =
        persistenceService.doReturningTransaction(
            entityManager ->
                entityManager
                    .createQuery(
                        "select pc from "
                            + ProvisioningConfig.class.getSimpleName()
                            + " pc "
                            + "order by pc.name desc",
                        ProvisioningConfig.class)
                    .getResultList());
  }
}
