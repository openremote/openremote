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
package org.openremote.model.syslog;

public enum SyslogLevel {

    INFO("info-circle"),
    WARN("exclamation-circle"),
    ERROR("exclamation-triangle");

    final String icon;

    SyslogLevel(String icon) {
        this.icon = icon;
    }

    public String getIcon() {
        return icon;
    }

    static public SyslogLevel getLevel(int level) {
        if (level == 1000) {
            return ERROR;
        } else if (level == 900) {
            return WARN;
        } else if (level == 800 || level == 700) {
            return INFO;
        }
        // Ignore FINE, FINER, FINEST because we shouldn't store or publish debug logging (too much data)
        return null;
    }

    public boolean isLoggable(SyslogEvent event) {
        return event.getLevel().ordinal() >= ordinal();
    }

}
