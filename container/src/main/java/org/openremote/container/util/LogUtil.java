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
 * but WITHOUT ANY WARRANTY without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.openremote.container.util;

import org.openremote.model.Container;
import org.openremote.model.util.TextUtil;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.logging.LogManager;

import static org.openremote.model.Container.OR_DEV_MODE;

public class LogUtil {

    public static final String OR_LOGGING_CONFIG_FILE = "OR_LOGGING_CONFIG_FILE";

    protected LogUtil() {
    }

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
    public static void configureLogging() throws ExceptionInInitializerError {
        // If no JUL configuration is provided
        if (System.getProperty("java.util.logging.config.file") == null) {
            // Load the logging configuration file specified with an environment variable
            if (!TextUtil.isNullOrEmpty(System.getenv(OR_LOGGING_CONFIG_FILE))) {
                Path loggingConfigFile = Paths.get(System.getenv(OR_LOGGING_CONFIG_FILE));
                if (!Files.isReadable(loggingConfigFile)) {
                    throw new ExceptionInInitializerError("OR_LOGGING_CONFIG_FILE is not readable: " + loggingConfigFile.toAbsolutePath());
                }
                try (InputStream is = Files.newInputStream(loggingConfigFile)) {
                    System.out.println("Using logging configuration: " + loggingConfigFile.toAbsolutePath());
                    LogManager.getLogManager().readConfiguration(is);
                } catch (Exception ex) {
                    throw new ExceptionInInitializerError(ex);
                }
            } else {
                // Try to find /deployment/manager/logging.properties
                if (Files.isReadable(Paths.get("/deployment/manager/logging.properties"))) {
                    try (InputStream is = Files.newInputStream(Paths.get("/deployment/manager/logging.properties"))) {
                        System.out.println("Using logging configuration: /deployment/manager/logging.properties");
                        LogManager.getLogManager().readConfiguration(is);
                    } catch (Exception ex) {
                        throw new ExceptionInInitializerError(ex);
                    }
                } else {
                    // Or load a default configuration from the classpath
                    String devModeStr = System.getenv(OR_DEV_MODE);
                    boolean isDevMode = devModeStr == null || "TRUE".equals(devModeStr.toUpperCase(Locale.ROOT));
                    String loggingFile = isDevMode ? "logging-dev.properties" : "logging.properties";
                    try (InputStream is = Container.class.getClassLoader().getResourceAsStream(loggingFile)) {
                        if (is != null) {
                            System.out.println("Using logging configuration on classpath: " + loggingFile);
                            LogManager.getLogManager().readConfiguration(is);
                        }
                    } catch (Exception ex) {
                        throw new ExceptionInInitializerError(ex);
                    }
                }
            }
        }
    }
}
