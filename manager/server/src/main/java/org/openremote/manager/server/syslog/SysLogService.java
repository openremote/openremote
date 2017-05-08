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
package org.openremote.manager.server.syslog;

import org.openremote.container.Container;
import org.openremote.container.ContainerService;
import org.openremote.manager.server.event.EventService;
import org.openremote.model.syslog.SyslogCategory;
import org.openremote.model.syslog.SyslogEvent;

import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

/**
 * Act as a JUL handler and publish (some) log messages on the client event bus.
 */
public class SysLogService extends Handler implements ContainerService {

    private static final Logger LOG = Logger.getLogger(SysLogService.class.getName());

    protected EventService eventService;

    @Override
    public void init(Container container) throws Exception {
        if (container.hasService(EventService.class)) {
            LOG.info("Syslog publisher enabled");
            eventService = container.getService(EventService.class);
        } else {
            LOG.info("Syslog publisher not enabled, event service not available");
        }

        if (eventService != null) {
            eventService.addSubscriptionAuthorizer((auth, subscription) -> {
                // Only superuser can get logging events
                return subscription.isEventType(SyslogEvent.class) && auth.isSuperUser();
            });
        }
    }

    @Override
    public void start(Container container) throws Exception {
    }

    @Override
    public void stop(Container container) throws Exception {
    }

    @Override
    public void flush() {
    }

    @Override
    public void close() throws SecurityException {
    }

    @Override
    public void publish(LogRecord record) {
        if (eventService == null)
            return;
        SyslogEvent syslogEvent = SyslogCategory.mapSyslogEvent(record);
        if (syslogEvent != null) {
            eventService.publishEvent(syslogEvent);
        }
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{}";
    }
}
