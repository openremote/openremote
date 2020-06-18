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
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriBuilder;
import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A filter for injecting query parameters into the request URI. If {@link #dynamicPlaceholderRegex} is set any request
 * passing through this filter will be checked for a property called {@link #DYNAMIC_VALUE} if set then any occurrence
 * of {@link #dynamicPlaceholderRegex} in the query parameter values will be replaced with the dynamic property value.
 */
public class QueryParameterInjectorFilter implements ClientRequestFilter {

    /**
     * Set a property on the request using this name to inject dynamic string values into
     */
    public static final String DYNAMIC_VALUE = QueryParameterInjectorFilter.class.getName() + ".dynamicValue";
    protected MultivaluedMap<String, String> queryParameters;
    protected String dynamicPlaceholderRegex;
    protected boolean dynamic;
    protected String dynamicTimePlaceHolderRegex;

    public QueryParameterInjectorFilter(MultivaluedMap<String, String> queryParameters,
                                        String dynamicPlaceholderRegex, String dynamicTimePlaceHolderRegex) {
        this.queryParameters = queryParameters;
        this.dynamicPlaceholderRegex = dynamicPlaceholderRegex;
        this.dynamicTimePlaceHolderRegex = dynamicTimePlaceHolderRegex;

        dynamic = queryParameters != null && dynamicPlaceholderRegex != null
            && queryParameters
            .entrySet()
            .stream()
            .anyMatch(paramNameAndValues ->
                paramNameAndValues.getValue() != null
                    && paramNameAndValues.getValue()
                    .stream()
                    .anyMatch(val -> val.matches(dynamicPlaceholderRegex)));
    }

    @Override
    public void filter(ClientRequestContext requestContext) throws IOException {
        if (queryParameters != null) {
            boolean dynamic = this.dynamic;
            String dynamicValue = null;

            if (dynamic) {
                dynamicValue = (String) requestContext.getProperty(DYNAMIC_VALUE);
                dynamic = dynamicValue != null;
            }

            UriBuilder uriBuilder = UriBuilder.fromUri(requestContext.getUri());
            boolean finalDynamic = dynamic;
            String finalDynamicValue = dynamicValue;

            queryParameters.forEach((name, values) -> {
                String[] valueArr = values.toArray(new String[values.size()]);
                if (finalDynamic) {
                    for (int i = 0; i < valueArr.length; i++) {
                        valueArr[i] = valueArr[i].replaceAll(dynamicPlaceholderRegex, finalDynamicValue);
                    }
                }
                if (dynamicTimePlaceHolderRegex != null) {
                    for (int i = 0; i < valueArr.length; i++) {
                        if (valueArr[i].matches(dynamicTimePlaceHolderRegex)) {
                            Matcher matcher = Pattern.compile(dynamicTimePlaceHolderRegex).matcher(valueArr[i]);
                            long millisToAdd = 0;
                            DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
                            if (matcher.find()) {
                                if (matcher.groupCount() > 0) {
                                    dateTimeFormatter = DateTimeFormatter.ofPattern(matcher.group(1));
                                }
                                if (matcher.groupCount() == 2) {
                                    millisToAdd = Long.parseLong(matcher.group(2));
                                }
                            }
                            valueArr[i] = dateTimeFormatter.format(Instant.now().plusMillis(millisToAdd).atZone(ZoneId.systemDefault()));
                        }
                    }
                }
                uriBuilder.queryParam(name, (Object[]) valueArr);
            });
            requestContext.setUri(uriBuilder.build());
        }
    }
}
