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
import jakarta.ws.rs.ext.Provider;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Instant;
import java.util.List;
import java.util.function.Supplier;

import static org.openremote.model.util.ValueUtil.doDynamicTimeReplace;

/**
 * A filter for replacing {@link org.openremote.model.Constants#DYNAMIC_TIME_PLACEHOLDER_REGEXP} in the request URI and
 * headers with the {@link #INSTANT_SUPPLIER_PROPERTY} from the request properties, this allows insertion of
 * dynamic timestamps into HTTP requests.
 */
@Provider
public class DynamicTimeInjectorFilter implements ClientRequestFilter {

    public static final String INSTANT_SUPPLIER_PROPERTY = DynamicTimeInjectorFilter.class.getName()+".instantSupplier";

    @SuppressWarnings("unchecked")
    @Override
    public void filter(ClientRequestContext requestContext) throws IOException {

        Supplier<Instant> instantSupplier = (Supplier<Instant>) requestContext.getProperty(DynamicTimeInjectorFilter.INSTANT_SUPPLIER_PROPERTY);

        if (instantSupplier == null) {
            return;
        }

        Instant instant = instantSupplier.get();

        try {

            // Query Parameters & Path

            URI oldURI = requestContext.getUri();
            String path = doDynamicTimeReplace(oldURI.getPath(), instant);
            String query = doDynamicTimeReplace(oldURI.getQuery(), instant);
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
                            return doDynamicTimeReplace(str, instant);
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
