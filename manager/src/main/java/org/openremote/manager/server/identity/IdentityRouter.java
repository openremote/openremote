package org.openremote.manager.server.identity;


import io.vertx.core.Vertx;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.Json;
import org.openremote.manager.server.web.HttpRouter;

import java.util.logging.Logger;

import static com.hubrick.vertx.rest.MediaType.APPLICATION_JSON_VALUE;
import static io.vertx.core.http.HttpMethod.GET;

public class IdentityRouter extends HttpRouter {

    private static final Logger LOG = Logger.getLogger(IdentityRouter.class.getName());

    protected final IdentityService identityService;

    public IdentityRouter(Vertx vertx, IdentityService identityService) {
        super(vertx);
        this.identityService = identityService;

        route(GET, "/install/:clientId").blockingHandler(rc -> {
            HttpServerRequest request = rc.request();
            HttpServerResponse response = rc.response();

            String realm = getRealm(rc);
            String clientId = request.getParam("clientId");

            identityService.getKeycloakClient().getClientInstall(realm, clientId).subscribe(
                clientInstall -> {
                    response.headers().add(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE);
                    response.end(Json.encode(clientInstall));
                }
            );
        }, false);
    }

}
