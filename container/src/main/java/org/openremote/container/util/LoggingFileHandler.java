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

    private static String pattern() throws IOException {
        String prefix = LoggingFileHandler.class.getName();
        String v = LogManager.getLogManager().getProperty(prefix +".pattern");
        return v.replace("${" + LogUtil.OR_LOGGING_PROPERTY_NAME + "}", System.getProperty(LogUtil.OR_LOGGING_PROPERTY_NAME));
    }

    public LoggingFileHandler() throws IOException {
        super(pattern());
    }
}
