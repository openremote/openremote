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

import org.openremote.container.timer.TimerService;
import org.openremote.manager.security.ManagerIdentityService;
import org.openremote.manager.web.ManagerWebResource;
import org.openremote.model.http.RequestParams;
import org.openremote.model.provisioning.ProvisioningConfig;
import org.openremote.model.provisioning.ProvisioningResource;

import javax.ws.rs.ForbiddenException;

public class ProvisioningResourceImpl extends ManagerWebResource implements ProvisioningResource {

    protected final ProvisioningService provisioningService;
    protected final ManagerIdentityService identityService;

    public ProvisioningResourceImpl(ProvisioningService provisioningService, TimerService timerService, ManagerIdentityService identityService) {
        super(timerService, identityService);
        this.provisioningService = provisioningService;
        this.identityService = identityService;
    }

    @Override
    public ProvisioningConfig<?, ?>[] getProvisioningConfigs() {
        if (!isSuperUser()) {
            throw new ForbiddenException("Only super admin can get provisioning configurations");
        }
        return provisioningService.getProvisioningConfigs().toArray(new ProvisioningConfig[0]);
    }

    @Override
    public long createProvisioningConfig(ProvisioningConfig<?, ?> provisioningConfig) {
        if (!isSuperUser()) {
            throw new ForbiddenException("Only super admin can create provisioning configurations");
        }
        provisioningConfig = provisioningService.merge(provisioningConfig);
        return provisioningConfig.getId();
    }

    @Override
    public void updateProvisioningConfig(RequestParams requestParams, Long id, ProvisioningConfig<?, ?> provisioningConfig) {
        if (!isSuperUser()) {
            throw new ForbiddenException("Only super admin can modify provisioning configurations");
        }
        provisioningService.merge(provisioningConfig);
    }

    @Override
    public void deleteProvisioningConfig(RequestParams requestParams, Long id) {
        if (!isSuperUser()) {
            throw new ForbiddenException("Only super admin can delete provisioning configurations");
        }
        provisioningService.delete(id);
    }
}
