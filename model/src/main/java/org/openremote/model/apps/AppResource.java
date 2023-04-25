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

import io.swagger.v3.oas.annotations.tags.Tag;
import org.openremote.model.http.RequestParams;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Response;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;

@Tag(name = "UI Apps")
@Path("apps")
public interface AppResource {

    /**
     * Retrieve a list of available apps
     */
    @GET
    @Produces(APPLICATION_JSON)
    String[] getApps(@BeanParam RequestParams requestParams);

    /**
     * Retrieve info json of all apps.
     */
    @GET
    @Produces(APPLICATION_JSON)
    @Path("info")
    Response getAppInfos(@BeanParam RequestParams requestParams);

    /**
     * Retrieve console app config.
     */
    @GET
    @Produces(APPLICATION_JSON)
    @Path("consoleConfig")
    Response getConsoleConfig(@BeanParam RequestParams requestParams);
}
