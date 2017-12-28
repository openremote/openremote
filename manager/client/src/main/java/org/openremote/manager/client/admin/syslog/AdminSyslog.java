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
package org.openremote.manager.client.admin.syslog;

import org.openremote.manager.client.admin.AdminContent;
import org.openremote.model.syslog.SyslogEvent;
import org.openremote.model.syslog.SyslogLevel;

public interface AdminSyslog extends AdminContent {

    interface Presenter {

        void onClearLog();

        void onContinueLog();

        void onPauseLog();

        void onFilterLimitChanged(int limit);

        void onFilterLevelChanged(SyslogLevel level);

        void saveSettings();

        void removeAll();
    }

    void setPresenter(Presenter presenter);

    void setStoredLevel(SyslogLevel level);

    SyslogLevel geStoredLevel();

    void setStoredMinutes(int minutes);

    int getStoredMinutes();

    SyslogLevel getFilterLevel();

    int getFilterLimit();

    void showEvents(SyslogEvent... events);

    void clearEvents();

}
