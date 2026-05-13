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

import org.openremote.container.persistence.PersistenceService;
import org.openremote.model.util.Config;
import org.openremote.model.util.TextUtil;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.logging.LogManager;

import static java.lang.System.Logger.Level.INFO;
import static org.openremote.model.util.Config.OR_DEV_MODE;

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

    protected static final System.Logger LOG = System.getLogger(LogUtil.class.getName());
    public static final String OR_LOGGING_CONFIG_FILE = "OR_LOGGING_CONFIG_FILE";
    public static final String OR_LOGGING_DIR = "OR_LOGGING_DIR";
    public static final String OR_LOGGING_DIR_DEFAULT = "logs";
    public static final String OR_LOGGING_PROPERTY_NAME = "or.logging.dir";

    public static void initialiseJUL() throws ExceptionInInitializerError {

        // Ensure the standard logging directory exists as JUL FileHandler will not create it
        String loggingDir = System.getenv(OR_LOGGING_DIR);
        Path loggingDirPath;
        if (TextUtil.isNullOrEmpty(loggingDir)) {
            String storageDir = System.getenv(PersistenceService.OR_STORAGE_DIR);
            if (TextUtil.isNullOrEmpty(storageDir)) {
                storageDir = PersistenceService.OR_STORAGE_DIR_DEFAULT;
            }
            loggingDirPath = Paths.get(storageDir, OR_LOGGING_DIR_DEFAULT);
        } else {
            loggingDirPath = Paths.get(loggingDir);
        }
        try {
            Files.createDirectories(loggingDirPath);
            LOG.log(INFO, "OR_LOGGING_DIR: " + loggingDirPath);
        } catch (IOException e) {
            LOG.log(INFO, "Failed to create OR_LOGGING_DIR: " + loggingDirPath, e);
        }
        // Set the path as system property so it can be interpolated in logging properties file
        if (TextUtil.isNullOrEmpty(System.getProperty(OR_LOGGING_PROPERTY_NAME))) {
            System.setProperty(OR_LOGGING_PROPERTY_NAME, loggingDirPath.toString());
        }

        // Don't do anything if standard JUL system properties set
        if (!TextUtil.isNullOrEmpty(System.getProperty("java.util.logging.config.class"))) {
            LOG.log(INFO,"Using specified java.util.logging.config.class system property: " + System.getProperty("java.util.logging.config.class"));
            return;
        }
        if (!TextUtil.isNullOrEmpty(System.getProperty("java.util.logging.config.file"))) {
            LOG.log(INFO,"Using specified java.util.logging.config.file system property: " + System.getProperty("java.util.logging.config.file"));
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
        boolean envConfigFileSet = !TextUtil.isNullOrEmpty(Config.getString(OR_LOGGING_CONFIG_FILE, null));
        if (envConfigFileSet) {
            InputStream configFile = getFileInputStream(Config.getString(OR_LOGGING_CONFIG_FILE, null));

            if (configFile != null) {
                LOG.log(INFO, "Using logging configuration: " + System.getenv(OR_LOGGING_CONFIG_FILE));
                return configFile;
            }

            // Look for the file on the classpath
            configFile = Thread.currentThread().getContextClassLoader().getResourceAsStream(System.getenv(OR_LOGGING_CONFIG_FILE));
            if (configFile != null) {
                LOG.log(INFO, "Using logging configuration from classpath: " + System.getenv(OR_LOGGING_CONFIG_FILE));
                return configFile;
            }
        }

        // Use built in config on the classpath
        String devModeStr = System.getenv(OR_DEV_MODE);
        boolean isDevMode = devModeStr == null || "TRUE".equals(devModeStr.toUpperCase(Locale.ROOT));
        String loggingFile = isDevMode ? "logging-dev.properties" : "logging.properties";
        InputStream configFile = org.openremote.model.Container.class.getClassLoader().getResourceAsStream(loggingFile);

        if (configFile != null) {
            LOG.log(INFO, "Using built in logging configuration from classpath: " + loggingFile);
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
