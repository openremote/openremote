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
import org.openremote.container.persistence.PersistenceService;
import org.openremote.container.web.WebService;

import java.util.logging.Logger;

public class NotificationService implements ContainerService {

    private static final Logger LOG = Logger.getLogger(NotificationService.class.getName());

    protected PersistenceService persistenceService;

    @Override
    public void init(Container container) throws Exception {
        this.persistenceService = container.getService(PersistenceService.class);

        container.getService(WebService.class).getApiSingletons().add(
            new NotificationResourceImpl(this)
        );
    }

    @Override
    public void start(Container container) throws Exception {
    }

    @Override
    public void stop(Container container) throws Exception {
    }

    public void storeDeviceToken(String deviceId, String userId, String token) {
        persistenceService.doTransaction(entityManager -> {
            DeviceNotificationToken.Id id = new DeviceNotificationToken.Id(deviceId, userId);
            DeviceNotificationToken deviceToken = new DeviceNotificationToken(id, token);
            entityManager.merge(deviceToken);
        });
    }

    public String findDeviceToken(String deviceId, String userId) {
        return persistenceService.doReturningTransaction(entityManager -> {
            DeviceNotificationToken.Id id = new DeviceNotificationToken.Id(deviceId, userId);
            DeviceNotificationToken deviceToken = entityManager.find(DeviceNotificationToken.class, id);
            return deviceToken != null ? deviceToken.getToken() : null;
        });
    }
}