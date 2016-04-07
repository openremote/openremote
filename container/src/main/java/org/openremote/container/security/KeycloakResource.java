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
package org.openremote.container.security;

import org.jboss.resteasy.annotations.Form;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.representations.idm.*;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;

import static javax.ws.rs.core.MediaType.*;

public interface KeycloakResource {

    String KEYCLOAK_CONTEXT_PATH = "auth";

    @GET
    @Path("/")
    @Produces(TEXT_HTML)
    Response getWelcomePage();

    @POST
    @Path("realms/{realm}/protocol/openid-connect/token")
    @Consumes(APPLICATION_FORM_URLENCODED)
    @Produces(APPLICATION_JSON)
    AccessTokenResponse getAccessToken(@PathParam("realm") String realm, @Form AuthForm authForm);

    @GET
    @Path("realms/{realm}/clients-registrations/install/{clientId}")
    @Produces(APPLICATION_JSON)
    ClientInstall getClientInstall(@PathParam("realm") String realm, @PathParam("clientId") String clientId);

    @GET
    @Path("admin/realms/{realm}")
    @Produces(APPLICATION_JSON)
    RealmRepresentation getRealm(@PathParam("realm") String realm);

    @PUT
    @Path("admin/realms/{realm}")
    @Consumes(APPLICATION_JSON)
    Response putRealm(@PathParam("realm") String realm, RealmRepresentation realmRepresentation);

    @POST
    @Path("admin/realms/{realm}/clients")
    @Consumes(APPLICATION_JSON)
    Response registerClientApplication(@PathParam("realm") String realm, ClientRepresentation clientRepresentation);

    @GET
    @Path("admin/realms/{realm}/clients")
    @Produces(APPLICATION_JSON)
    ClientRepresentation[] getClientApplications(@PathParam("realm") String realm);

    @GET
    @Path("admin/realms/{realm}/clients/{clientId}")
    @Produces(APPLICATION_JSON)
    ClientRepresentation getClientApplication(@PathParam("realm") String realm, @PathParam("clientId") String clientId);

    @DELETE
    @Path("admin/realms/{realm}/clients/{clientId}")
    Response deleteClientApplication(@PathParam("realm") String realm, @PathParam("clientId") String clientId);

    @POST
    @Path("admin/realms/{realm}/clients/{clientObjectId}/roles")
    @Consumes(APPLICATION_JSON)
    Response createRoleForClientApplication(@PathParam("realm") String realm, @PathParam("clientObjectId") String clientObjectId, RoleRepresentation roleRepresentation);

    @GET
    @Path("admin/realms/{realm}/clients/{clientObjectId}/roles/{role}")
    @Produces(APPLICATION_JSON)
    RoleRepresentation getRoleOfClientApplication(@PathParam("realm") String realm, @PathParam("clientObjectId") String clientObjectId, @PathParam("role") String role);

    @POST
    @Path("admin/realms/{realm}/clients/{clientObjectId}/roles/{role}/composites")
    @Consumes(APPLICATION_JSON)
    Response addCompositesToRoleForClientApplication(@PathParam("realm") String realm, @PathParam("clientObjectId") String clientObjectId, @PathParam("role") String role, RoleRepresentation[] roleRepresentations);

    @GET
    @Path("admin/realms/{realm}/users")
    @Produces(APPLICATION_JSON)
    UserRepresentation[] getUsers(@PathParam("realm") String realm, @QueryParam("username") String username);

    @DELETE
    @Path("admin/realms/{realm}/users/{userId}")
    Response deleteUser(@PathParam("realm") String realm, @PathParam("userId") String userId);

    @POST
    @Path("admin/realms/{realm}/users")
    @Consumes(APPLICATION_JSON)
    Response createUser(@PathParam("realm") String realm, UserRepresentation userRepresentation);

    @PUT
    @Path("admin/realms/{realm}/users/{userId}/reset-password")
    @Consumes(APPLICATION_JSON)
    Response resetPassword(@PathParam("realm") String realm, @PathParam("userId") String userId, CredentialRepresentation credentialRepresentation);

    @POST
    @Path("admin/realms/{realm}/users/{userId}/role-mappings/clients/{clientObjectId}")
    @Consumes(APPLICATION_JSON)
    Response addUserClientRoleMapping(@PathParam("realm") String realm, @PathParam("userId") String userId, @PathParam("clientObjectId") String clientObjectId, RoleRepresentation[] roleRepresentations);
}