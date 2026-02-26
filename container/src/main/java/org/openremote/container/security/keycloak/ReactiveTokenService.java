package org.openremote.container.security.keycloak;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import org.keycloak.representations.AccessTokenResponse;

import java.util.concurrent.CompletionStage;

/**
 * This is a reactive async version of the keycloak {@link org.keycloak.admin.client.token.TokenService}
 */
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
public interface ReactiveTokenService {

    @POST
    @Path("/realms/{realm}/protocol/openid-connect/token")
    CompletionStage<AccessTokenResponse> grantToken(@PathParam("realm") String realm, MultivaluedMap<String, String> map);

    @POST
    @Path("/realms/{realm}/protocol/openid-connect/token")
    CompletionStage<AccessTokenResponse> refreshToken(@PathParam("realm") String realm, MultivaluedMap<String, String> map);

    @POST
    @Path("/realms/{realm}/protocol/openid-connect/logout")
    CompletionStage<Void> logout(@PathParam("realm") String realm, MultivaluedMap<String, String> map);
}
