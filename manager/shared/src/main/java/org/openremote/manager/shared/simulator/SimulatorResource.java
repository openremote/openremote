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
package org.openremote.manager.shared.simulator;

import jsinterop.annotations.JsType;
import org.openremote.manager.shared.http.RequestParams;
import org.openremote.manager.shared.http.SuccessStatusCode;
import org.openremote.model.attribute.AttributeRef;
import org.openremote.model.simulator.SimulatorState;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.*;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

@Path("simulator")
@JsType(isNative = true)
public interface SimulatorResource {

    @Path("{agentId}/{protocolConfiguration}")
    @GET
    @Produces(APPLICATION_JSON)
    @SuccessStatusCode(200)
    //@RolesAllowed({"read:admin", "write:admin"})
    SimulatorState getSimulatorState(@BeanParam RequestParams requestParams,
                                     @PathParam("agentId") String agentId,
                                     @PathParam("protocolConfiguration") String protocolConfiguration);

    @PUT
    @Produces(APPLICATION_JSON)
    @SuccessStatusCode(200)
    @RolesAllowed({"read:admin", "write:admin"})
    void updateSimulatorState(@BeanParam RequestParams requestParams, AttributeRef protocolConfigurationRef, SimulatorState simulatorState);

}
