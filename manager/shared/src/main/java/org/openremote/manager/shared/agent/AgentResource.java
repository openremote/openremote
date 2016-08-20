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
package org.openremote.manager.shared.agent;

import jsinterop.annotations.JsType;
import org.openremote.manager.shared.http.RequestParams;
import org.openremote.manager.shared.http.SuccessStatusCode;

import javax.ws.rs.*;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

@Path("agent")
@JsType(isNative = true)
public interface AgentResource {

    @GET
    @Produces(APPLICATION_JSON)
    @SuccessStatusCode(200)
        // TODO Implement admin roles on server
        //@RolesAllowed({"read:admin"})
    Agent[] getAll(@BeanParam RequestParams requestParams);

    @GET
    @Path("{agentId}")
    @Produces(APPLICATION_JSON)
    @SuccessStatusCode(200)
    Agent get(@BeanParam RequestParams requestParams, @PathParam("agentId") String agentId);

    @POST
    @Consumes(APPLICATION_JSON)
    @SuccessStatusCode(204)
        // TODO Implement admin roles on server
        //@RolesAllowed({"write:admin"})
    void create(@BeanParam RequestParams requestParams, Agent agent);

    @PUT
    @Path("{agentId}")
    @Consumes(APPLICATION_JSON)
    @SuccessStatusCode(204)
        // TODO Implement admin roles on server
        //@RolesAllowed({"read:admin"})
    void update(@BeanParam RequestParams requestParams, @PathParam("agentId") String agentId, Agent agent);

    @DELETE
    @Path("{agentId}")
    @Produces(APPLICATION_JSON)
    @SuccessStatusCode(204)
        // TODO Implement admin roles on server
        //@RolesAllowed({"read:admin"})
    void delete(@BeanParam RequestParams requestParams, @PathParam("agentId") String agentId);
}
