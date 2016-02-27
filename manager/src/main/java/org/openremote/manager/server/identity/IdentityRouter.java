package org.openremote.manager.server.identity;


import io.vertx.core.Vertx;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.Json;
import org.openremote.manager.server.web.HttpRouter;
import org.openremote.manager.server.web.ResponseException;

import java.util.logging.Logger;

import static com.hubrick.vertx.rest.MediaType.APPLICATION_JSON_VALUE;
import static io.vertx.core.http.HttpMethod.GET;

public class IdentityRouter extends HttpRouter {

    private static final Logger LOG = Logger.getLogger(IdentityRouter.class.getName());

    public IdentityRouter(Vertx vertx, IdentityService identityService) {
        super(vertx, identityService);

        route(GET, "/install/:clientId").blockingHandler(rc -> {
            HttpServerRequest request = rc.request();
            HttpServerResponse response = rc.response();

            String realm = getRealm(rc);
            String clientId = request.getParam("clientId");

            ClientInstall clientInstall = identityService.getClientInstall(realm, clientId);
            if (clientInstall != null) {
                response.headers().add(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE);
                response.end(Json.encode(clientInstall));
            } else {
                rc.fail(new ResponseException(404, "Client install not found"));
            }
        }, false);
    }

}
