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
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A filter for injecting query parameters into the request URI. The query parameters are extracted from the request
 * property {@link #QUERY_PARAMETERS_PROPERTY}. Any {@link Constants#DYNAMIC_VALUE_PLACEHOLDER} in the query parameters
 * will be replaced with the {@link #DYNAMIC_VALUE_PROPERTY} from the request. Any
 * {@link Constants#DYNAMIC_TIME_PLACEHOLDER_REGEXP} in the query parameters will be replaced with the current system
 * time in the specified format with optional offset.
 */
@Provider
public class QueryParameterInjectorFilter implements ClientRequestFilter {

    /**
     * Set a property on the request using this name to inject dynamic string values into
     */
    public static final String DYNAMIC_VALUE_PROPERTY = QueryParameterInjectorFilter.class.getName() + ".dynamicValue";

    /**
     * Set a property on the request using this name to inject query parameters; the value should be a {@link
     * MultivaluedMap}.
     */
    public static final String QUERY_PARAMETERS_PROPERTY = QueryParameterInjectorFilter.class.getName() + ".params";

    public static final Pattern DYNAMIC_TIME_PATTERN = Pattern.compile(Constants.DYNAMIC_TIME_PLACEHOLDER_REGEXP);

    @SuppressWarnings("unchecked")
    @Override
    public void filter(ClientRequestContext requestContext) throws IOException {

        MultivaluedMap<String, String> queryParameters = (MultivaluedMap<String, String>) requestContext.getProperty(QUERY_PARAMETERS_PROPERTY);
        String dynamicValue = requestContext.getProperty(DYNAMIC_VALUE_PROPERTY) != null ? (String) requestContext.getProperty(DYNAMIC_VALUE_PROPERTY) : "";

        if (queryParameters == null) {
            return;
        }

        UriBuilder uriBuilder = UriBuilder.fromUri(requestContext.getUri());

        queryParameters.forEach((name, values) -> {

            Object[] formattedValues = values.stream().map(v -> {
                v = v.replaceAll(Constants.DYNAMIC_VALUE_PLACEHOLDER_REGEXP, dynamicValue);

                Matcher matcher = DYNAMIC_TIME_PATTERN.matcher(v);

                if (matcher.find()) {
                    long millisToAdd = 0L;
                    DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

                    if (matcher.groupCount() > 0) {
                        dateTimeFormatter = DateTimeFormatter.ofPattern(matcher.group(1));
                    }
                    if (matcher.groupCount() == 2) {
                        millisToAdd = Long.parseLong(matcher.group(2));
                    }

                    v = v.replaceAll(Constants.DYNAMIC_TIME_PLACEHOLDER_REGEXP, dateTimeFormatter.format(Instant.now().plusMillis(millisToAdd).atZone(ZoneId.systemDefault())));
                }

                return v;
            }).toArray();

            uriBuilder.queryParam(name, formattedValues);
        });

        requestContext.setUri(uriBuilder.build());
    }
}
