/*
 * Copyright 2025, OpenRemote Inc.
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
package org.openremote.container.util;

import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.LogManager;

/**
 * This {@link FileHandler} allows injection of system property into the pattern
 */
public class LoggingFileHandler extends FileHandler {

    private static String getProperty(String propertyName) {
        return LogManager.getLogManager().getProperty(LoggingFileHandler.class.getName() + propertyName);
    }

    private static int getIntProperty(String propertyName, int defaultValue) {
        String val = getProperty(propertyName);
        if (val == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(val.trim());
        } catch (Exception e) {
            return defaultValue;
        }
    }

    private static int count() {
        return Math.max(1, getIntProperty(".count", 1));
    }

    private static int limit() {
        return Math.max(0, getIntProperty(".limit", 0));
    }

    private static String pattern() {
        return getProperty(".pattern").replace("${" + LogUtil.OR_LOGGING_PROPERTY_NAME + "}",System.getProperty(LogUtil.OR_LOGGING_PROPERTY_NAME));
    }

    public LoggingFileHandler() throws IOException {
        // The FileHandler(String) constructor cannot be used because this constructor sets the limit and count after configuration.
        super(pattern(), limit(), count());
    }
}
