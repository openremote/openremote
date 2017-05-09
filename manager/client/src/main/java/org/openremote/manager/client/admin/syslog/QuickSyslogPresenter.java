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
package org.openremote.manager.client.admin.syslog;

import com.google.inject.Inject;
import org.openremote.manager.client.Environment;
import org.openremote.manager.client.ManagerHistoryMapper;
import org.openremote.model.syslog.SyslogEvent;
import org.openremote.model.syslog.SyslogLevel;

import java.util.logging.Logger;

public class QuickSyslogPresenter implements QuickSyslog.Presenter {

    private static final Logger LOG = Logger.getLogger(QuickSyslogPresenter.class.getName());

    final protected Environment environment;
    final protected ManagerHistoryMapper managerHistoryMapper;
    final protected QuickSyslog view;

    @Inject
    public QuickSyslogPresenter(Environment environment,
                                ManagerHistoryMapper managerHistoryMapper,
                                QuickSyslog view) {
        this.environment = environment;
        this.managerHistoryMapper = managerHistoryMapper;
        this.view = view;

        view.setPresenter(this);

        environment.getEventBus().register(SyslogEvent.class, view::addEvent);
    }

    @Override
    public QuickSyslog getView() {
        return view;
    }

    @Override
    public void onOpen() {
        environment.getEventService().subscribe(
            SyslogEvent.class,
            new SyslogEvent.LevelCategoryFilter(view.getLogLevel())
        );
    }

    @Override
    public void onClose() {
        environment.getEventService().unsubscribe(SyslogEvent.class);
    }

    @Override
    public void onLogLevelChanged(SyslogLevel level) {
        environment.getEventService().unsubscribe(SyslogEvent.class);
        environment.getEventService().subscribe(
            SyslogEvent.class,
            new SyslogEvent.LevelCategoryFilter(level)
        );
    }
}
