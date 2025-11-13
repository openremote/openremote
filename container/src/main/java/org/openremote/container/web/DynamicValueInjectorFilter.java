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

import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientRequestFilter;
import jakarta.ws.rs.ext.Provider;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import static org.openremote.model.util.ValueUtil.doDynamicValueReplace;

/**
 * A filter for replacing {@link org.openremote.model.Constants#DYNAMIC_VALUE_PLACEHOLDER_REGEXP} in the request URI and
 * headers with the {@link #DYNAMIC_VALUE_PROPERTY} from the request properties, this allows insertion of
 * dynamic values into HTTP requests.
 * <p>Dynamic time replacement has been moved to {@link DynamicTimeInjectorFilter#filter(ClientRequestContext)}.
 */
@Provider
public class DynamicValueInjectorFilter implements ClientRequestFilter {

    /**
     * Set a property on the request using this name to inject a dynamic value
     */
    public static final String DYNAMIC_VALUE_PROPERTY = DynamicValueInjectorFilter.class.getName() + ".dynamicValue";

    @Override
    public void filter(ClientRequestContext requestContext) throws IOException {

        if(!requestContext.getPropertyNames().contains(DYNAMIC_VALUE_PROPERTY)) return;

        String dynamicValue = requestContext.getProperty(DYNAMIC_VALUE_PROPERTY) != null ? (String) requestContext.getProperty(DYNAMIC_VALUE_PROPERTY) : "";

        try {

            // Query Parameters & Path

            URI oldURI = requestContext.getUri();
            String path = doDynamicValueReplace(oldURI.getPath(), dynamicValue);
            String query = doDynamicValueReplace(oldURI.getQuery(), dynamicValue);
            requestContext.setUri(new URI(
                oldURI.getScheme(),
                oldURI.getUserInfo(),
                oldURI.getHost(),
                oldURI.getPort(),
                path,
                query,
                oldURI.getFragment()
            ));

            // Headers

            if (requestContext.getHeaders() != null) {
                requestContext.getHeaders().forEach((key, values) -> {
                    List<Object> replacedValues = values.stream().map(val -> {
                        if (val instanceof String str) {
                            return doDynamicValueReplace(str, dynamicValue);
                        } else {
                            return val;
                        }
                    }).toList();
                    requestContext.getHeaders().replace(key, replacedValues);
                });
            }
        } catch (URISyntaxException ignored) {}
    }
}
