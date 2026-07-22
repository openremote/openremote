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

import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

/**
 * Marker that can be passed as a {@link java.util.logging.LogRecord} parameter to attribute a
 * single log record to a realm, e.g. {@code LOG.log(Level.INFO, "Agent started", new SyslogRealmMarker(realm))}.
 * Takes precedence over any {@link SyslogRealmRegistry} entry for the logger. The marker is
 * ignored by message formatters unless the message contains an explicit {@code {N}} placeholder.
 */
public record SyslogRealmMarker(String realm) {

    /**
     * Logs a message with both a {@link Throwable} and a realm marker; the JUL convenience
     * methods cannot carry both, so this builds the {@link LogRecord} manually. The logger
     * name is set explicitly as {@link Logger#log(LogRecord)} does not fill it in.
     */
    public static void log(Logger logger, Level level, String message, Throwable thrown, String realm) {
        if (!logger.isLoggable(level)) {
            return;
        }
        LogRecord record = new LogRecord(level, message);
        record.setLoggerName(logger.getName());
        record.setThrown(thrown);
        record.setParameters(new Object[]{new SyslogRealmMarker(realm)});
        logger.log(record);
    }
}
