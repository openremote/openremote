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
import org.openremote.model.http.RequestParams;
import org.openremote.model.http.SuccessStatusCode;

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
@Path("tenant")
@JsType(isNative = true)
public interface TenantResource {

    @GET
    @Produces(APPLICATION_JSON)
    @SuccessStatusCode(200)
    @RolesAllowed("read:admin")
    @SuppressWarnings("unusable-by-js")
    Tenant[] getAll(@BeanParam RequestParams requestParams);

    /**
     * Regular users can call this, but only to obtain details about their currently authenticated and active realm.
     */
    @GET
    @Path("{realm}")
    @Produces(APPLICATION_JSON)
    @SuccessStatusCode(200)
    @SuppressWarnings("unusable-by-js")
    Tenant get(@BeanParam RequestParams requestParams, @PathParam("realm") String realm);

    /**
     * Regular users can call this, but only to obtain details about their currently authenticated and active realm.
     */
    @GET
    @Path("for/{realmId}")
    @Produces(APPLICATION_JSON)
    @SuccessStatusCode(200)
    @SuppressWarnings("unusable-by-js")
    Tenant getForRealmId(@BeanParam RequestParams requestParams, @PathParam("realmId") String realmId);

    @PUT
    @Path("{realm}")
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @SuccessStatusCode(204)
    @RolesAllowed("write:admin")
    @SuppressWarnings("unusable-by-js")
    void update(@BeanParam RequestParams requestParams, @PathParam("realm") String realm, @Valid Tenant tenant);

    @POST
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @SuccessStatusCode(204)
    @RolesAllowed("write:admin")
    @SuppressWarnings("unusable-by-js")
    void create(@BeanParam RequestParams requestParams, @Valid Tenant tenant);

    @DELETE
    @Path("{realm}")
    @Produces(APPLICATION_JSON)
    @SuccessStatusCode(204)
    @RolesAllowed("write:admin")
    @SuppressWarnings("unusable-by-js")
    void delete(@BeanParam RequestParams requestParams, @PathParam("realm") String realm);
}
