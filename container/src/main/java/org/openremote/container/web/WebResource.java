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

import org.openremote.container.Container;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;

import static javax.ws.rs.core.Response.Status.BAD_REQUEST;

public class WebResource {

    @Context
    protected Application application;

    @Context
    protected UriInfo uriInfo;

    public WebApplication getApplication() {
        return (WebApplication) application;
    }

    public Container getContainer() {
        return getApplication().getContainer();
    }

    public String getRealm() {
        String realm = uriInfo.getQueryParameters().getFirst("realm");
        if (realm == null || realm.length() == 0) {
            throw new WebApplicationException("Missing realm parameter", BAD_REQUEST);
        }
        return realm;
    }
}
