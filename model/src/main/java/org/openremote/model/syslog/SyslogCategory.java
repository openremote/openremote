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

import java.util.logging.LogRecord;

public enum SyslogCategory {

    ASSET_STORAGE(
        "AssetStorageService"
    ),
    ASSET_PROCESSING(
        "AssetProcessingService"
    ),
    RULES(
        "RulesService",
        "RulesEngine"
    );

    protected final String[] mappedLoggerNames;

    SyslogCategory(String... mappedLoggerNames) {
        this.mappedLoggerNames = mappedLoggerNames;
    }

    public boolean isMappedLoggerName(String name) {
        for (String mappedLoggerName : mappedLoggerNames) {
            if (name.endsWith(mappedLoggerName))
                return true;
        }
        return false;
    }

    public static SyslogEvent mapSyslogEvent(LogRecord record) {
        SyslogLevel level = SyslogLevel.getLevel(record.getLevel().intValue());
        if (level == null)
            return null;
        for (SyslogCategory category : values()) {
            if (category.isMappedLoggerName(record.getLoggerName())) {
                return new SyslogEvent(level, category, record.getMessage());
            }
        }
        return null;
    }
}
