/*
 * Copyright 2016, OpenRemote Inc.
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
package org.openremote.manager.services;

import java.util.logging.Logger;

import org.openremote.container.timer.TimerService;
import org.openremote.manager.security.ManagerIdentityService;
import org.openremote.manager.web.ManagerWebResource;
import org.openremote.model.http.RequestParams;
import org.openremote.model.services.ServiceDescriptor;
import org.openremote.model.services.ServiceResource;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.ForbiddenException;

public class ServiceResourceImpl extends ManagerWebResource implements ServiceResource {

    protected ServiceRegistryService serviceRegistry;
    private static final Logger LOG = Logger.getLogger(ServiceResourceImpl.class.getName());

    public ServiceResourceImpl(TimerService timerService, ManagerIdentityService identityService,
            ServiceRegistryService serviceRegistry) {
        super(timerService, identityService);
        this.serviceRegistry = serviceRegistry;
    }

    @Override
    public boolean register(RequestParams requestParams,
            @NotNull @Valid ServiceDescriptor serviceDescriptor) {

        if (!isSuperUser()) {
            LOG.warning("Only super users can register services");
            throw new ForbiddenException("Only super users can register services");
        }

        return serviceRegistry.registerService(getClientRemoteAddress(), serviceDescriptor);
    }

    @Override
    public ServiceDescriptor[] list(RequestParams requestParams) {
        return serviceRegistry.getRegisteredServices();
    }

}
