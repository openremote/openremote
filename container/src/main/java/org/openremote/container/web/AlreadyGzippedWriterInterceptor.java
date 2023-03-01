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
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.openremote.container.web;

import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.ext.Provider;
import jakarta.ws.rs.ext.WriterInterceptor;
import jakarta.ws.rs.ext.WriterInterceptorContext;
import java.io.IOException;
import java.util.Arrays;

@Provider
@Priority(Priorities.ENTITY_CODER+1)
public class AlreadyGzippedWriterInterceptor implements WriterInterceptor {

    public static final String[] ALREADY_ZIPPED_MEDIA_TYPES = new String[] {
        "application/vnd.mapbox-vector-tile"
    };

    @Override
    public void aroundWriteTo(WriterInterceptorContext context) throws IOException, WebApplicationException {
        if (context.getMediaType() != null)  {
            if (Arrays.asList(ALREADY_ZIPPED_MEDIA_TYPES).contains(context.getMediaType().toString())) {
                context.getHeaders().putSingle("Content-Encoding", "gzip");
            }
        }
        context.proceed();
    }
}
