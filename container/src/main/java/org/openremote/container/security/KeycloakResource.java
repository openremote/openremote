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

import org.keycloak.representations.AccessTokenResponse;

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
    AccessTokenResponse getAccessToken(@PathParam("realm") String realm, @BeanParam AuthForm authForm);

    @GET
    @Path("realms/{realm}/clients-registrations/install/{clientId}")
    @Produces(APPLICATION_JSON)
    ClientInstall getClientInstall(@PathParam("realm") String realm, @PathParam("clientId") String clientId);

}