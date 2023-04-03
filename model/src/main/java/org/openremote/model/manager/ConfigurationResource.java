/*
 * Copyright 2022, OpenRemote Inc.
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
package org.openremote.model.manager;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.openremote.model.Constants;
import org.openremote.model.file.FileInfo;
import org.openremote.model.http.RequestParams;

import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.*;
import java.io.IOException;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;

@Tag(name = "Configuration")
@Path("configuration")
public interface ConfigurationResource {

    @PUT
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @Path("manager")
    @RolesAllowed({Constants.WRITE_ADMIN_ROLE})
    Object update(@BeanParam RequestParams requestParams, Object managerConfiguration);

    @POST
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @Path("manager/file")
    @RolesAllowed({Constants.WRITE_ADMIN_ROLE})
    String fileUpload(
            @BeanParam RequestParams requestParams,
            @QueryParam("path")
            String path,
            FileInfo fileInfo
    );


}
