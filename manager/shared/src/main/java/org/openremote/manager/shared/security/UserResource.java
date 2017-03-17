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
package org.openremote.manager.shared.security;

import jsinterop.annotations.JsType;
import org.openremote.manager.shared.http.RequestParams;
import org.openremote.manager.shared.http.SuccessStatusCode;

import javax.validation.Valid;
import javax.ws.rs.*;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

@Path("user")
@JsType(isNative = true)
public interface UserResource {

    @GET
    @Produces(APPLICATION_JSON)
    @Path("{realm}")
    @SuccessStatusCode(200)
    User[] getAll(@BeanParam RequestParams requestParams, @PathParam("realm") String realm);

    @GET
    @Path("{realm}/{userId}")
    @Produces(APPLICATION_JSON)
    @SuccessStatusCode(200)
    User get(@BeanParam RequestParams requestParams, @PathParam("realm") String realm, @PathParam("userId") String userId);

    @PUT
    @Path("{realm}/{userId}")
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @SuccessStatusCode(204)
    void update(@BeanParam RequestParams requestParams, @PathParam("realm") String realm, @PathParam("userId") String userId, @Valid User user);

    @POST
    @Path("{realm}")
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @SuccessStatusCode(204)
    void create(@BeanParam RequestParams requestParams, @PathParam("realm") String realm, @Valid User user);

    @DELETE
    @Path("{realm}/{userId}")
    @Produces(APPLICATION_JSON)
    @SuccessStatusCode(204)
    void delete(@BeanParam RequestParams requestParams, @PathParam("realm") String realm, @PathParam("userId") String userId);

    @PUT
    @Path("{realm}/{userId}/reset-password")
    @SuccessStatusCode(204)
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    void resetPassword(@BeanParam RequestParams requestParams, @PathParam("realm") String realm, @PathParam("userId") String userId, Credential credential);

    @GET
    @Path("{realm}/{userId}/role")
    @Produces(APPLICATION_JSON)
    @SuccessStatusCode(200)
    Role[] getRoles(@BeanParam RequestParams requestParams, @PathParam("realm") String realm, @PathParam("userId") String userId);

    @PUT
    @Path("{realm}/{userId}/role")
    @Consumes(APPLICATION_JSON)
    @SuccessStatusCode(204)
    void updateRoles(@BeanParam RequestParams requestParams, @PathParam("realm") String realm, @PathParam("userId") String userId, Role[] roles);
}
