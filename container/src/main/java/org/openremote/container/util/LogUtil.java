/*
 * Copyright 2023, OpenRemote Inc.
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

import org.openremote.model.util.TextUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.logging.LogManager;

import static org.openremote.model.Container.OR_DEV_MODE;

/**
 * If system property <code>java.util.logging.config.file</code> or <code>java.util.logging.config.file</code> has been
 * set then the normal {@link LogManager#readConfiguration()} will be used, otherwise try to load the logging
 * configuration specified in environment variable <code>OR_LOGGING_CONFIG_FILE</code> as a file.
 * If this wasn't set, try to find the file <code>/deployment/manager/logging.properties</code>.
 * If this also wasn't found, load the given default logging configuration from the classpath
 * (logging-dev.properties when OR_DEV_MODE=true otherwise logging.properties).
 * <p>
 * This method should be called in a <code>static { ... }</code> block in the "first" class of your
 * application (typically where your <code>main()</code> method is located).
 */
public class LogUtil {

    protected static final Logger LOG = LoggerFactory.getLogger(LogUtil.class);
    public static final String OR_LOGGING_CONFIG_FILE = "OR_LOGGING_CONFIG_FILE";

    public static void initialiseJUL() throws ExceptionInInitializerError {

        // Don't do anything if standard JUL system properties set
        if (!TextUtil.isNullOrEmpty(System.getProperty("java.util.logging.config.class"))) {
            LOG.info("Using specified java.util.logging.config.class system property: " + System.getProperty("java.util.logging.config.class"));
            return;
        }
        if (!TextUtil.isNullOrEmpty(System.getProperty("java.util.logging.config.file"))) {
            LOG.info("Using specified java.util.logging.config.file system property: " + System.getProperty("java.util.logging.config.file"));
            return;
        }

        InputStream configFile = getConfigInputStream();
        if (configFile != null) {
            try {
                LogManager.getLogManager().readConfiguration(configFile);
            } catch (IOException ex) {
                throw new ExceptionInInitializerError(ex);
            }
        }
    }

    protected static InputStream getConfigInputStream() {

        // Look for env variable config file
        boolean envConfigFileSet = !TextUtil.isNullOrEmpty(System.getenv(OR_LOGGING_CONFIG_FILE));
        if (envConfigFileSet) {
            InputStream configFile = getFileInputStream(System.getenv(OR_LOGGING_CONFIG_FILE));

            if (configFile != null) {
                LOG.info("Using logging configuration: " + System.getenv(OR_LOGGING_CONFIG_FILE));
                return configFile;
            }

            // Look for the file on the classpath
            configFile = Thread.currentThread().getContextClassLoader().getResourceAsStream(System.getenv(OR_LOGGING_CONFIG_FILE));
            if (configFile != null) {
                LOG.info("Using logging configuration from classpath: " + System.getenv(OR_LOGGING_CONFIG_FILE));
                return configFile;
            }
        }

        // Use built in config on the classpath
        String devModeStr = System.getenv(OR_DEV_MODE);
        boolean isDevMode = devModeStr == null || "TRUE".equals(devModeStr.toUpperCase(Locale.ROOT));
        String loggingFile = isDevMode ? "logging-dev.properties" : "logging.properties";
        InputStream configFile = org.openremote.model.Container.class.getClassLoader().getResourceAsStream(loggingFile);

        if (configFile != null) {
            LOG.info("Using built in logging configuration from classpath: " + loggingFile);
            return configFile;
        }

        return null;
    }

    protected static InputStream getFileInputStream(String path) {
        if (TextUtil.isNullOrEmpty(path)) {
            return null;
        }

        Path loggingConfigFile = Paths.get(path);

        if (Files.isReadable(loggingConfigFile)) {
            try {
                return Files.newInputStream(loggingConfigFile);
            } catch (Exception ex) {
                throw new ExceptionInInitializerError(ex);
            }
        }

        return null;
    }
}
