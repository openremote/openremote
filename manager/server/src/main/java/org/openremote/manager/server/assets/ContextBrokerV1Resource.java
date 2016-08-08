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
package org.openremote.manager.server.assets;

import org.openremote.manager.shared.ngsi.*;

import javax.ws.rs.*;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

/**
 * http://fiware-orion.readthedocs.io/en/1.2.1/user/walkthrough_apiv1/index.html#ngsi9-standard-operations
 */
@Path("v1")
public interface ContextBrokerV1Resource {
    /*
    * ***************************************
    * NGSI10
    * ***************************************
     */

    @POST
    @Path("queryContext")
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    ContextResponseWrapper queryContext(EntityAttributeQuery query);

    /*
    * ***************************************
    * NGSI9
    * ***************************************
     */

    @POST
    @Path("registry/registerContext")
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    ContextRegistrationV1Response registerContext(ContextRegistrationV1 registration);
}
