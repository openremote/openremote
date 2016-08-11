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

import org.openremote.manager.shared.http.PATCH;
import org.openremote.manager.shared.http.SuccessStatusCode;
import org.openremote.manager.shared.ngsi.*;
import org.openremote.manager.shared.ngsi.params.EntityListParams;
import org.openremote.manager.shared.ngsi.params.EntityParams;
import org.openremote.manager.shared.ngsi.params.SubscriptionParams;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

/**
 * http://telefonicaid.github.io/fiware-orion/api/v2/stable/
 */
@Path("v2")
public interface ContextBrokerV2Resource {
    /*
    * ***************************************
    * NGSI10
    * ***************************************
     */

    @GET
    @Produces(APPLICATION_JSON)
    EntryPoint getEntryPoint();

    @GET
    @Path("entities")
    @Produces(APPLICATION_JSON)
    Entity[] getEntities(@BeanParam EntityListParams entityListParams);

    @POST
    @Path("entities")
    @Consumes(APPLICATION_JSON)
    Response postEntity(Entity entity);

    @GET
    @Path("entities/{entityId}")
    @Produces(APPLICATION_JSON)
    Entity getEntity(@PathParam("entityId") String entityId, @BeanParam EntityParams entityParams);

    @DELETE
    @Path("entities/{entityId}")
    Response deleteEntity(@PathParam("entityId") String entityId);

    @PUT
    @Path("entities/{entityId}/attrs")
    @Consumes(APPLICATION_JSON)
    Response putEntityAttributes(@PathParam("entityId") String entityId, Entity entity);

    @PATCH
    @Path("entities/{entityId}/attrs")
    @Consumes(APPLICATION_JSON)
    Response patchEntityAttributes(@PathParam("entityId") String entityId, Entity entity);

    @GET
    @Path("subscriptions")
    @Produces(APPLICATION_JSON)
    SubscribeRequestV2[] getSubscriptions();

    @GET
    @Path("subscriptions/{subscriptionId}")
    @Produces(APPLICATION_JSON)
    SubscribeRequestV2 getSubscription(@PathParam("subscriptionId")String subscriptionId);

    @POST
    @Path("subscriptions")
    @Consumes(APPLICATION_JSON)
    Response createSubscription(SubscribeRequestV2 subscription);

    @PATCH
    @Path("subscriptions/{subscriptionId}")
    @Consumes(APPLICATION_JSON)
    @SuccessStatusCode(204)
    Response updateSubscription(@PathParam("subscriptionId")String subscriptionId, SubscribeRequestV2 subscription);

    @DELETE
    @Path("subscriptions/{subscriptionId}")
    @Consumes(APPLICATION_JSON)
    @SuccessStatusCode(204)
    Response deleteSubscription(@PathParam("subscriptionId")String subscriptionId);

    /*
    * ***************************************
    * NGSI9
    * ***************************************
     */

    @POST
    @Path("registrations")
    @Consumes(APPLICATION_JSON)
    Response createRegistration(RegistrationRequestV2 registration);

    @PATCH
    @Path("registrations/{registrationId}")
    @Consumes(APPLICATION_JSON)
    Response updateRegistration(@PathParam("registrationId") String registrationId, Duration duration);

    @DELETE
    @Path("registrations/{registrationId}")
    Response deleteRegistration(@PathParam("registrationId") String registrationId);

    @POST
    @Path("op/register")
    @Consumes(APPLICATION_JSON)
    Response batchRegistration(BatchRegistrationRequestV2 registrations);
}
