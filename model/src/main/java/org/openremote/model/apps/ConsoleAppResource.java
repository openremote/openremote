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
package org.openremote.model.apps;

import jsinterop.annotations.JsType;
import org.openremote.model.Constants;
import org.openremote.model.http.RequestParams;
import org.openremote.model.http.SuccessStatusCode;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.*;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

@Path("app")
@JsType(isNative = true)
public interface ConsoleAppResource {

    /**
     * Retrieve installed console applications. Only the superuser can perform this operation,
     * a 403 status is returned if a regular user tries to access the console applications.
     */
    @GET
    @Produces(APPLICATION_JSON)
    @SuccessStatusCode(200)
    @RolesAllowed({Constants.READ_APPS_ROLE})
    @SuppressWarnings("unusable-by-js")
    String[] getInstalledApps(@BeanParam RequestParams requestParams);

    @GET
    @Path("config")
    @Produces(APPLICATION_JSON)
    @SuccessStatusCode(200)
    ConsoleAppConfig getAppConfig(@BeanParam RequestParams requestParams);
}
