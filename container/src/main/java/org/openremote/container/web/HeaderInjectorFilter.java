/*
 * Copyright 2017, OpenRemote Inc.
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
package org.openremote.container.web;

import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A filter for injecting headers into the request.
 */
public class HeaderInjectorFilter implements ClientRequestFilter {

    protected Set<Map.Entry<String, List<String>>> headers;

    public HeaderInjectorFilter(Map<String, List<String>> headers) {
        this.headers = headers.entrySet();
    }

    @Override
    public void filter(ClientRequestContext requestContext) throws IOException {
        headers.forEach(headerAndValues -> {
                if (headerAndValues.getValue().isEmpty()) {
                    requestContext.getHeaders().remove(headerAndValues.getKey());
                } else {
                    requestContext
                        .getHeaders()
                        .addAll(headerAndValues.getKey(), headerAndValues.getValue().toArray());
                }
            }
        );
    }
}
