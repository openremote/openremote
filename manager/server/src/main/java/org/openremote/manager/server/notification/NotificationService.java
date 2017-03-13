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
package org.openremote.manager.server.notification;

import org.openremote.container.Container;
import org.openremote.container.ContainerService;
import org.openremote.container.web.WebService;
import org.openremote.manager.server.security.ManagerIdentityService;

import java.util.logging.Logger;

public class NotificationService implements ContainerService {

    private static final Logger LOG = Logger.getLogger(NotificationService.class.getName());

    @Override
    public void init(Container container) throws Exception {
       container.getService(WebService.class).getApiSingletons().add(
                new NotificationResourceImpl(container.getService(ManagerIdentityService.class))
        );
    }

    @Override
    public void start(Container container) throws Exception {
        LOG.info("Starting Notification Service ...");
    }

    @Override
    public void stop(Container container) throws Exception {
        LOG.info("Stopping Notification Service ...");
    }


}