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
package org.openremote.manager.shared.agent;

import jsinterop.annotations.JsType;
import org.openremote.manager.shared.http.RequestParams;
import org.openremote.manager.shared.http.SuccessStatusCode;
import org.openremote.model.asset.AssetAttribute;
import org.openremote.model.asset.agent.ProtocolDescriptor;
import org.openremote.model.attribute.AttributeValidationResult;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.*;

import java.util.Map;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

/**
 * This resource is for Agent specific tasks; normal asset/attribute CRUD operations should still use
 * {@link org.openremote.manager.shared.asset.AssetResource}.
 *
 *
 *
 */
@Path("agent")
@JsType(isNative = true)
public interface AgentResource {

    /**
     * Retrieve all the protocols that the specified agent supports
     */
    @GET
    @Path("protocol/{agentId}")
    @Produces(APPLICATION_JSON)
    @SuccessStatusCode(200)
    @RolesAllowed({"read:assets"})
    ProtocolDescriptor[] getSupportedProtocols(
        @BeanParam RequestParams requestParams,
        @PathParam("agentId") String agentId
    );

    /**
     * Retrieve all the protocols for all the agents
     */
    @GET
    @Path("protocol")
    @Produces(APPLICATION_JSON)
    @SuccessStatusCode(200)
    @RolesAllowed({"read:assets"})
    Map<String, ProtocolDescriptor[]> getAllSupportedProtocols(
        @BeanParam RequestParams requestParams
    );

    /**
     * Retrieve discovered protocol configurations for the specified protocol on the specified agent
     */
    @GET
    @Path("configuration/{agentId}/{protocolName}")
    @Produces(APPLICATION_JSON)
    @SuccessStatusCode(200)
    @RolesAllowed({"read:assets"})
    AssetAttribute[] getDiscoveredProtocolConfigurations(
        @BeanParam RequestParams requestParams,
        @PathParam("agentId") String agentId,
        @PathParam("protocolName") String protocolName
    );

    /**
     * Ask the appropriate protocol on the specified agent to validate the supplied {@link org.openremote.model.asset.agent.ProtocolConfiguration}
     */
    @POST
    @Path("validate/{agentId}")
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @SuccessStatusCode(200)
    AttributeValidationResult validateProtocolConfiguration(
        @BeanParam RequestParams requestParams,
        @PathParam("agentId") String agentId,
        AssetAttribute protocolConfiguration
    );
}
