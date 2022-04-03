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
package org.openremote.manager.system;

import org.openremote.model.Container;
import org.openremote.model.ContainerService;
import org.openremote.manager.web.ManagerWebService;
import org.openremote.model.system.HealthStatusProvider;
import org.openremote.model.system.StatusResource;

import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

/**
 * This service is here to initialise the {@link StatusResource}.
 */
public class HealthStatusService implements ContainerService {

    protected List<HealthStatusProvider> healthStatusProviderList = new ArrayList<>();

    @Override
    public int getPriority() {
        return ContainerService.DEFAULT_PRIORITY;
    }

    @Override
    public void init(Container container) throws Exception {

        ServiceLoader.load(HealthStatusProvider.class).forEach(healthStatusProviderList::add);

        for (HealthStatusProvider healthStatusProvider : healthStatusProviderList) {
            if (healthStatusProvider instanceof ContainerService) {
                ((ContainerService) healthStatusProvider).init(container);
            }
        }

        container.getService(ManagerWebService.class).addApiSingleton(
                new StatusResourceImpl(container, healthStatusProviderList)
        );
    }

    @Override
    public void start(Container container) throws Exception {
        for (HealthStatusProvider healthStatusProvider : healthStatusProviderList) {
            if (healthStatusProvider instanceof ContainerService) {
                ((ContainerService) healthStatusProvider).start(container);
            }
        }
    }

    @Override
    public void stop(Container container) throws Exception {
        for (HealthStatusProvider healthStatusProvider : healthStatusProviderList) {
            if (healthStatusProvider instanceof ContainerService) {
                ((ContainerService) healthStatusProvider).stop(container);
            }
        }
    }
}
