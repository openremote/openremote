/*
 * Copyright 2015, OpenRemote Inc.
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

package org.openremote.android.util;

import gumi.builders.UrlBuilder;

public class UrlUtil {

    public static UrlBuilder url(String baseUrl, String contextPath, String... pathSegments) {
        return UrlBuilder.fromString(baseUrl)
            .withPath(getPath(contextPath, pathSegments));
    }

    public static UrlBuilder url(String scheme, String host, String port, String contextPath, String... pathSegments) {
        return UrlBuilder.empty()
            .withScheme(scheme)
            .withHost(host)
            .withPort(port != null ? Integer.valueOf(port) : null)
            .withPath(getPath(contextPath, pathSegments));
    }

    protected static String getPath(String contextPath, String... pathSegments) {
        StringBuilder path = new StringBuilder();
        if (contextPath != null)
            path.append(contextPath);
        if (pathSegments != null) {
            for (String pathSegment : pathSegments) {
                path
                    .append(pathSegment.startsWith("/") ? "" : "/")
                    .append(pathSegment);
            }
        }
        return path.toString();
    }
}