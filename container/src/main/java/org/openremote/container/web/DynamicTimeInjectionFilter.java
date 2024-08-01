/*
 * Copyright 2024, OpenRemote Inc.
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
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.ext.Provider;
import org.openremote.model.protocol.ProtocolUtil;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static org.openremote.model.protocol.ProtocolUtil.doDynamicTimeReplace;

/**
 * {@link ClientRequestFilter} that allows users to insert timestamps dynamically into requests sent by an HTTP Agent.
 *
 * It performs the replacement using RegEx {@link org.openremote.model.Constants#DYNAMIC_TIME_REGEX} and only applies
 * changes in request headers and request URIs.
 *
 * Refer to {@link org.openremote.test.protocol.http.HttpClientProtocolTest#Check HTTP client dynamic time feature()} test
 * for more info when it comes to usage. {@code ${time}} injects the current timestamps in epoch millis format to the HTTP
 * request.
 *
 * Uses {@link ProtocolUtil#doDynamicTimeReplace} to perform the replacement.
 *
 */
@Provider
public class DynamicTimeInjectionFilter implements ClientRequestFilter {

    public Supplier<Long> currentMillisSupplier;

    public static final String CURRENT_MILLIS_SUPPLIER_PROPERTY = DynamicTimeInjectionFilter.class.getName()+".millisSupplier";

    @SuppressWarnings("unchecked")
    @Override
    public void filter(ClientRequestContext requestContext) throws IOException {

        // Request body is not processed in this filter

        if(!requestContext.getPropertyNames().contains(DynamicTimeInjectionFilter.CURRENT_MILLIS_SUPPLIER_PROPERTY)) return;

        this.currentMillisSupplier = (Supplier<Long>) requestContext.getProperty(DynamicTimeInjectionFilter.CURRENT_MILLIS_SUPPLIER_PROPERTY);

        // Query Parameters

        MultivaluedMap<String, String> queryParameters = (MultivaluedMap<String, String>) requestContext.getProperty(DynamicValueInjectorFilter.QUERY_PARAMETERS_PROPERTY);

        if (queryParameters != null) {
            queryParameters = queryParameters.entrySet().stream()
                    .map(entry -> Map.entry(
                            entry.getKey(),
                            entry.getValue().stream()
                                    .filter(Objects::nonNull)
                                    .map(val -> doDynamicTimeReplace(val, Instant.ofEpochMilli(currentMillisSupplier.get())))
                                    .toList()
                    )).collect(Collectors.toMap(
                            Map.Entry::getKey,
                            Map.Entry::getValue,
                            (oldValue, newValue) -> newValue,
                            MultivaluedHashMap::new
                    ));

            requestContext.setProperty(DynamicValueInjectorFilter.QUERY_PARAMETERS_PROPERTY, queryParameters);
        }

        // Headers

        if (requestContext.getHeaders() != null) {
            requestContext.getHeaders().forEach((key, values) -> {
                List<Object> replacedValues = values.stream().map(val -> {
                    if (val instanceof String) {
                        return doDynamicTimeReplace((String) val, Instant.ofEpochMilli(currentMillisSupplier.get()));
                    } else {
                        return val;
                    }
                }).toList();
                requestContext.getHeaders().replace(key, replacedValues);
            });
        }
    }
}
