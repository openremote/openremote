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

import org.openremote.model.syslog.SyslogCategory;

import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.client.ClientResponseContext;
import javax.ws.rs.client.ClientResponseFilter;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;

import static org.openremote.model.syslog.SyslogCategory.PROTOCOL;

/**
 * A filter that once a HTTP status code is received that matches any in the {@link #failureResponses} will result
 * in all future requests being blocked (i.e. the request will not reach the server) and instead a
 * {@link Response.Status#METHOD_NOT_ALLOWED} status will be returned.
 */
public class PermanentFailureFilter implements ClientRequestFilter, ClientResponseFilter {

    private static final Logger LOG = SyslogCategory.getLogger(PROTOCOL, PermanentFailureFilter.class);
    protected boolean failed;
    protected List<Integer> failureResponses;

    public PermanentFailureFilter(List<Integer> failureResponses) {
        this.failureResponses = failureResponses;
    }

    @Override
    public void filter(ClientRequestContext requestContext) throws IOException {
        if (failed) {
            requestContext.abortWith(Response.status(Response.Status.METHOD_NOT_ALLOWED).build());
        }
    }

    @Override
    public void filter(ClientRequestContext requestContext, ClientResponseContext responseContext) throws IOException {

        if (failureResponses == null) {
            return;
        }

        if (failureResponses.contains(responseContext.getStatus())) {
            LOG.warning("Server returned a response code that is set as permanent failure so future requests will be blocked: " + responseContext.getStatus());
            failed = true;
        }
    }
}
