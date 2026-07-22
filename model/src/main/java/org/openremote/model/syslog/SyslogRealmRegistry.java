/*
 * Copyright 2026, OpenRemote Inc.
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

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Registry that maps JUL logger names to the realm their log records belong to; used by
 * {@link SyslogCategory#mapSyslogEvent} to attribute a {@link SyslogEvent} to a realm.
 * Components that log through a dedicated per-realm logger (e.g. realm rules engines,
 * protocol instances) register their logger name here on start and unregister on stop.
 * Log records from unregistered loggers are treated as system logs (no realm).
 */
public final class SyslogRealmRegistry {

    private static final ConcurrentMap<String, String> LOGGER_REALMS = new ConcurrentHashMap<>();

    private SyslogRealmRegistry() {
    }

    public static void register(String loggerName, String realm) {
        if (loggerName != null && realm != null) {
            LOGGER_REALMS.put(loggerName, realm);
        }
    }

    public static void unregister(String loggerName) {
        if (loggerName != null) {
            LOGGER_REALMS.remove(loggerName);
        }
    }

    public static String getRealm(String loggerName) {
        return loggerName == null ? null : LOGGER_REALMS.get(loggerName);
    }
}
