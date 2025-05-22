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
package org.openremote.manager.ai;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;

import java.util.Map;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;

@Tag(name = "AI", description = "Operations on system status")
@Path("ai")
public interface AiResource {

    @POST
    @Path("registerAI")
    @Produces(APPLICATION_JSON)
    @Operation(operationId = "registerAIService", summary = "Register the AI Service")
    boolean registerAIService(Map<String, Object> map);

    @GET
    @Path("websocket")
    @Produces(APPLICATION_JSON)
    @Operation(operationId = "getWebsocketInfo", summary = "Retrieve the system information")
    Map<String, Object> getWebsocketInfo();
}
