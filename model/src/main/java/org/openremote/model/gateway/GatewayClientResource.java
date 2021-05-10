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
package org.openremote.model.gateway;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.openremote.model.Constants;
import org.openremote.model.asset.agent.ConnectionStatus;
import org.openremote.model.http.RequestParams;
import javax.annotation.security.RolesAllowed;
import javax.validation.Valid;
import javax.ws.rs.*;

import java.util.List;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

/**
 * Resource for managing the connection to a central manager
 */
@Tag(name = "Gateway")
@Path("gateway")
public interface GatewayClientResource {

    /**
     * Get the {@link GatewayConnection} for the specified realm, user must be a realm admin
     */
    @GET
    @Path("connection/{realm}")
    @Produces(APPLICATION_JSON)
    @RolesAllowed({Constants.READ_ADMIN_ROLE})
    GatewayConnection getConnection(@BeanParam RequestParams requestParams, @PathParam("realm") String realm);

    @GET
    @Path("status/{realm}")
    @Produces(APPLICATION_JSON)
    @RolesAllowed({Constants.READ_ADMIN_ROLE})
    ConnectionStatus getConnectionStatus(@BeanParam RequestParams requestParams, @PathParam("realm") String realm);

    /**
     * Get the {@link GatewayConnection}s for all realms, user must be a super user
     */
    @GET
    @Path("connection")
    @Produces(APPLICATION_JSON)
    @RolesAllowed({Constants.READ_ADMIN_ROLE})
    List<GatewayConnection> getConnections(@BeanParam RequestParams requestParams);

    /**
     * Update a {@link GatewayConnection} for the specified realm
     */
    @PUT
    @Path("connection/{realm}")
    @Consumes(APPLICATION_JSON)
    @RolesAllowed({Constants.WRITE_ADMIN_ROLE})
    void setConnection(@BeanParam RequestParams requestParams, @PathParam("realm") String realm, @Valid GatewayConnection connection);

    @DELETE
    @Path("connection/{realm}")
    @RolesAllowed({Constants.WRITE_ADMIN_ROLE})
    void deleteConnection(@BeanParam RequestParams requestParams, @PathParam("realm") String realm);

    @DELETE
    @Path("connection")
    @RolesAllowed({Constants.WRITE_ADMIN_ROLE})
    void deleteConnections(@BeanParam RequestParams requestParams, @QueryParam("realm") List<String> realms);
}
