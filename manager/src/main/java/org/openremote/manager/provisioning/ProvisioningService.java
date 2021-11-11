/*
 * Copyright 2021, OpenRemote Inc.
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
package org.openremote.manager.provisioning;

import org.openremote.container.persistence.PersistenceService;
import org.openremote.container.timer.TimerService;
import org.openremote.manager.security.ManagerIdentityService;
import org.openremote.manager.web.ManagerWebService;
import org.openremote.model.Container;
import org.openremote.model.ContainerService;
import org.openremote.model.provisioning.ProvisioningConfig;
import org.openremote.model.util.ValueUtil;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ProvisioningService implements ContainerService {

    protected static final Logger LOG = Logger.getLogger(ProvisioningService.class.getName());
    protected PersistenceService persistenceService;
    protected ManagerIdentityService identityService;

    @Override
    public int getPriority() {
        return 0;
    }

    @Override
    public void init(Container container) throws Exception {
        persistenceService = container.getService(PersistenceService.class);
        identityService = container.getService(ManagerIdentityService.class);
        TimerService timerService = container.getService(TimerService.class);

        container.getService(ManagerWebService.class).getApiSingletons().add(
            new ProvisioningResourceImpl(this, timerService, identityService)
        );
    }

    @Override
    public void start(Container container) throws Exception {

    }

    @Override
    public void stop(Container container) throws Exception {

    }

    public <T extends ProvisioningConfig<?, ?>> T merge(T provisioningConfig) {
        return persistenceService.doReturningTransaction(entityManager -> {

            // Do standard JSR-380 validation on the config
            Set<ConstraintViolation<ProvisioningConfig<?, ?>>> validationFailures = ValueUtil.validate(provisioningConfig);

            if (validationFailures.size() > 0) {
                String msg = "Provisioning config merge failed as failed constraint validation: config=" + provisioningConfig;
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
        persistenceService.doTransaction(entityManager -> {
            ProvisioningConfig<?, ?> provisioningConfig = entityManager.find(ProvisioningConfig.class, id);
            if (provisioningConfig != null)
                entityManager.remove(provisioningConfig);
        });
    }

    public List<ProvisioningConfig> getProvisioningConfigs() {
            return persistenceService.doReturningTransaction(entityManager ->
                entityManager.createQuery(
                    "select pc from " + ProvisioningConfig.class.getSimpleName() + " pc " +
                        "order by pc.name desc",
                    ProvisioningConfig.class)
                    .getResultList());
    }
}
