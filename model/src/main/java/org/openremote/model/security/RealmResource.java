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
package org.openremote.model.security;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.openremote.model.Constants;
import org.openremote.model.http.RequestParams;

import javax.annotation.security.RolesAllowed;
import javax.validation.Valid;
import javax.ws.rs.*;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

/**
 * Manage realms.
 * <p>
 * All operations can only be called by the superuser.
 * <p>
 * TODO Relax permissions to allow regular users to maintain their own realm
 */
@Tag(name = "Realm")
@Path("realm")
public interface RealmResource {

    @GET
    @Produces(APPLICATION_JSON)
    Realm[] getAll(@BeanParam RequestParams requestParams);

    /**
     * Will return realm and display names for accessible realms by authenticated user
     */
    @GET
    @Path("accessible")
    @Produces(APPLICATION_JSON)
    Realm[] getAccessible(@BeanParam RequestParams requestParams);

    /**
     * Regular users can call this, but only to obtain details about their currently authenticated and active realm.
     */
    @GET
    @Path("{name}")
    @Produces(APPLICATION_JSON)
    Realm get(@BeanParam RequestParams requestParams, @PathParam("name") String realm);

    @PUT
    @Path("{name}")
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @RolesAllowed(Constants.WRITE_ADMIN_ROLE)
    void update(@BeanParam RequestParams requestParams, @PathParam("name") String realmName, @Valid Realm realm);

    @POST
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @RolesAllowed(Constants.WRITE_ADMIN_ROLE)
    void create(@BeanParam RequestParams requestParams, @Valid Realm realm);

    @DELETE
    @Path("{name}")
    @Produces(APPLICATION_JSON)
    @RolesAllowed(Constants.WRITE_ADMIN_ROLE)
    void delete(@BeanParam RequestParams requestParams, @PathParam("name") String realm);
}
