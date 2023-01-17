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

import org.jboss.logmanager.ConfigurationLocator;
import org.openremote.model.util.TextUtil;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;

import static org.openremote.model.Container.OR_DEV_MODE;

/**
 * If system property <code>java.util.logging.config.file</code> has not been set, try to load the
 * logging configuration specified in environment variable <code>OR_LOGGING_CONFIG_FILE</code> as a file.
 * If this wasn't set, try to find the file <code>/deployment/manager/logging.properties</code>.
 * If this also wasn't found, load the given default logging configuration from the classpath
 * (logging-dev.properties when OR_DEV_MODE=true otherwise logging.properties).
 * <p>
 * This method should be called in a <code>static { ... }</code> block in the "first" class of your
 * application (typically where your <code>main()</code> method is located).
 */
public class LogConfigurationLocator implements ConfigurationLocator {

    public static final String OR_LOGGING_CONFIG_FILE = "OR_LOGGING_CONFIG_FILE";

    @Override
    public InputStream findConfiguration() throws IOException {

        // Use JUL configuration property if set
        boolean julConfigSet = !TextUtil.isNullOrEmpty(System.getProperty("java.util.logging.config.file"));
        if (julConfigSet) {
            InputStream configFile = getFileInputStream(System.getProperty("java.util.logging.config.file"));

            if (configFile != null) {
                System.out.println("Using logging configuration: " + System.getProperty("java.util.logging.config.file"));
                return configFile;
            }

            throw new IOException("Cannot access specified logging configuration: " + System.getProperty("java.util.logging.config.file"));
        }

        // Use env variable config file
        boolean envConfigFileSet = !TextUtil.isNullOrEmpty(System.getenv(OR_LOGGING_CONFIG_FILE));
        if (envConfigFileSet) {
            InputStream configFile = getFileInputStream(System.getenv(OR_LOGGING_CONFIG_FILE));

            if (configFile != null) {
                System.out.println("Using logging configuration: " + System.getenv(OR_LOGGING_CONFIG_FILE));
                return configFile;
            }

            // Look for the file on the classpath
            configFile = Thread.currentThread().getContextClassLoader().getResourceAsStream(System.getenv(OR_LOGGING_CONFIG_FILE));
            if (configFile != null) {
                System.out.println("Using logging configuration from classpath: " + System.getenv(OR_LOGGING_CONFIG_FILE));
                return configFile;
            }

            throw new IOException("Cannot access specified logging configuration: " + System.getenv(OR_LOGGING_CONFIG_FILE));
        }

        // Use built in config on the classpath
        String devModeStr = System.getenv(OR_DEV_MODE);
        boolean isDevMode = devModeStr == null || "TRUE".equals(devModeStr.toUpperCase(Locale.ROOT));
        String loggingFile = isDevMode ? "logging-dev.properties" : "logging.properties";
        InputStream configFile = org.openremote.model.Container.class.getClassLoader().getResourceAsStream(loggingFile);

        if (configFile != null) {
            System.out.println("Using built in logging configuration from classpath: " + loggingFile);
            return configFile;
        }

        throw new IOException("Cannot access specified logging configuration: " + loggingFile);
    }

    protected InputStream getFileInputStream(String path) {
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
