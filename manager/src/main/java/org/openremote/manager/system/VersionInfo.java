/*
 *
 *  * Copyright 2026, OpenRemote Inc.
 *  *
 *  * See the CONTRIBUTORS.txt file in the distribution for a
 *  * full listing of individual contributors.
 *  *
 *  * This program is free software: you can redistribute it and/or modify
 *  * it under the terms of the GNU Affero General Public License as
 *  * published by the Free Software Foundation, either version 3 of the
 *  * License, or (at your option) any later version.
 *  *
 *  * This program is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  * GNU Affero General Public License for more details.
 *  *
 *  * You should have received a copy of the GNU Affero General Public License
 *  * along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 */

package org.openremote.manager.system;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class VersionInfo {

    public static final String VERSION = loadVersion();
    public static final String GATEWAY_API_VERSION = "1.1.0";

    public static String getManagerVersion() {
        return VERSION;
    }

    public static String getGatewayApiVersion() {
        return GATEWAY_API_VERSION;
    }

    protected static String loadVersion() {
        try (InputStream resourceStream = VersionInfo.class.getClassLoader().getResourceAsStream("version.properties")) {
            if (resourceStream != null) {
                Properties versionProps = new Properties();
                versionProps.load(resourceStream);
                return versionProps.getProperty("version");
            } else {
                throw new RuntimeException("Could not load version.properties");
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
