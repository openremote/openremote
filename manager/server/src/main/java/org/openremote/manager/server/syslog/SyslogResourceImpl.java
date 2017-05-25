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

import org.openremote.container.web.WebResource;
import org.openremote.manager.shared.http.RequestParams;
import org.openremote.manager.shared.syslog.SyslogConfig;
import org.openremote.manager.shared.syslog.SyslogResource;
import org.openremote.model.syslog.SyslogEvent;
import org.openremote.model.syslog.SyslogLevel;

import javax.ws.rs.BeanParam;
import java.util.List;

import static org.openremote.manager.shared.syslog.SyslogConfig.DEFAULT_LEVEL;
import static org.openremote.manager.shared.syslog.SyslogConfig.DEFAULT_LIMIT;

public class SyslogResourceImpl extends WebResource implements SyslogResource {

    final protected SyslogService syslogService;

    public SyslogResourceImpl(SyslogService syslogService) {
        this.syslogService = syslogService;
    }

    @Override
    public SyslogEvent[] getEvents(@BeanParam RequestParams requestParams, SyslogLevel level, Integer limit) {
        List<SyslogEvent> events = syslogService.getLastStoredEvents(
            level != null ? level : DEFAULT_LEVEL,
            limit != null ? limit : DEFAULT_LIMIT
        );
        return events.toArray(new SyslogEvent[events.size()]);
    }

    @Override
    public void clearEvents(@BeanParam RequestParams requestParams) {
        syslogService.clearStoredEvents();
    }

    @Override
    public SyslogConfig getConfig(@BeanParam RequestParams requestParams) {
        return syslogService.getConfig();
    }

    @Override
    public void updateConfig(@BeanParam RequestParams requestParams, SyslogConfig config) {
        syslogService.setConfig(config);
    }
}
