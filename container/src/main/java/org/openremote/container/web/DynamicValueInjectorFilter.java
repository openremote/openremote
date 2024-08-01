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

import org.openremote.model.Constants;

import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientRequestFilter;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.UriBuilder;
import jakarta.ws.rs.ext.Provider;

import java.io.IOException;
import java.util.List;


/**
 * A filter for injecting query parameters into the request URI. The query parameters are extracted from the request
 * property {@link #QUERY_PARAMETERS_PROPERTY}. Any {@link Constants#DYNAMIC_VALUE_PLACEHOLDER} in the query parameters
 * will be replaced with the {@link #DYNAMIC_VALUE_PROPERTY} from the request.
 *
 * Dynamic time replacement has been moved to {@link DynamicTimeInjectionFilter#filter(ClientRequestContext)}.
 * This filter is required to be ran <b>after</b> {@link DynamicTimeInjectionFilter}.
 */
@Provider
public class DynamicValueInjectorFilter implements ClientRequestFilter {

    /**
     * Set a property on the request using this name to inject dynamic string values into
     */
    public static final String DYNAMIC_VALUE_PROPERTY = DynamicValueInjectorFilter.class.getName() + ".dynamicValue";

    /**
     * Set a property on the request using this name to inject query parameters; the value should be a {@link
     * MultivaluedMap}.
     */
    public static final String QUERY_PARAMETERS_PROPERTY = DynamicValueInjectorFilter.class.getName() + ".params";

    @SuppressWarnings("unchecked")
    @Override
    public void filter(ClientRequestContext requestContext) throws IOException {

        // Query Parameters

        MultivaluedMap<String, String> queryParameters = (MultivaluedMap<String, String>) requestContext.getProperty(QUERY_PARAMETERS_PROPERTY);
        String dynamicValue = requestContext.getProperty(DYNAMIC_VALUE_PROPERTY) != null ? (String) requestContext.getProperty(DYNAMIC_VALUE_PROPERTY) : "";

        if (queryParameters == null) {
            return;
        }

        UriBuilder uriBuilder = UriBuilder.fromUri(requestContext.getUri());

        queryParameters.forEach((name, values) -> {
            Object[] formattedValues = values
                    .stream()
		            .map(v -> v.replaceAll(Constants.DYNAMIC_VALUE_PLACEHOLDER_REGEXP, dynamicValue))
		            .toArray();
            uriBuilder.queryParam(name, formattedValues);
        });

        requestContext.setUri(uriBuilder.build());

        // Headers

        if (requestContext.getHeaders() != null) {
            requestContext.getHeaders().forEach((key, values) -> {
                List<Object> replacedValues = values.stream().map(val -> {
                    if (val instanceof String) {
                        return ((String) val).replaceAll(Constants.DYNAMIC_VALUE_PLACEHOLDER_REGEXP, dynamicValue);
                    } else {
                        return val;
                    }
                }).toList();
                requestContext.getHeaders().replace(key, replacedValues);
            });
        }
    }
}
