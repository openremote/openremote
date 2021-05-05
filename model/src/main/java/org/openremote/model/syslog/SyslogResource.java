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
package org.openremote.model.syslog;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.openremote.model.Constants;
import org.openremote.model.http.RequestParams;
import javax.annotation.security.RolesAllowed;
import javax.validation.Valid;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.util.List;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

@Tag(name = "Syslog")
@Path("syslog")
public interface SyslogResource {

    @GET
    @Path("event")
    @Produces(APPLICATION_JSON)
    @RolesAllowed({Constants.READ_RULES_ROLE})
    @SuppressWarnings({"unusable-by-js"})
    Response getEvents(@BeanParam RequestParams requestParams, @QueryParam("level") SyslogLevel level, @QueryParam("per_page") Integer perPage, @QueryParam("page") Integer page, @QueryParam("from") Long from, @QueryParam("to") Long to, @QueryParam("category") List<SyslogCategory> categories, @QueryParam("subCategory") List<String> subCategories);

    @DELETE
    @Path("event")
    @RolesAllowed({Constants.WRITE_ADMIN_ROLE})
    void clearEvents(@BeanParam RequestParams requestParams);

    @GET
    @Path("config")
    @Consumes(APPLICATION_JSON)
    @RolesAllowed({Constants.READ_ADMIN_ROLE})
    SyslogConfig getConfig(@BeanParam RequestParams requestParams);

    @PUT
    @Path("config")
    @Consumes(APPLICATION_JSON)
    @RolesAllowed({Constants.WRITE_ADMIN_ROLE})
    void updateConfig(@BeanParam RequestParams requestParams, @Valid SyslogConfig config);
}
