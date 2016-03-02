package org.openremote.manager.server.map;

import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import org.keycloak.representations.AccessToken;
import org.openremote.manager.server.Constants;
import org.openremote.manager.server.identity.ClientInstall;
import org.openremote.manager.server.identity.IdentityService;
import org.openremote.manager.server.web.HttpRouter;

import java.util.logging.Level;
import java.util.logging.Logger;

import static io.vertx.core.http.HttpHeaders.CONTENT_ENCODING;
import static io.vertx.core.http.HttpHeaders.CONTENT_TYPE;
import static io.vertx.core.http.HttpMethod.GET;
import static org.openremote.manager.server.util.UrlUtil.url;

public class MapRouter extends HttpRouter {

    private static final Logger LOG = Logger.getLogger(MapRouter.class.getName());

    protected final MapService mapService;

    public MapRouter(Vertx vertx, MapService mapService) {
        super(vertx);
        this.mapService = mapService;

        route(GET, "/").handler(context -> {
            HttpServerResponse response = context.response();
            String tileUrl = url(context, getRealm(context), "map", "tile").toString() + "/{z}/{x}/{y}";
            response.putHeader(CONTENT_TYPE, "application/json");
            response.end(mapService.getMapSettings(tileUrl).toJson());
        });

        route(GET, "/tile/:zoom/:column/:row").blockingHandler(context -> { // Blocking!
            HttpServerRequest request = context.request();
            HttpServerResponse response = context.response();
            try {
                int zoom = Integer.valueOf(request.getParam("zoom"));
                int column = Integer.valueOf(request.getParam("column"));
                int row = Integer.valueOf(request.getParam("row"));

                // Flip y, oh why
                row = new Double(Math.pow(2, zoom) - 1 - row).intValue();

                byte[] tile = mapService.getMapTile(zoom, column, row);
                if (tile != null) {
                    response.putHeader(CONTENT_TYPE, "application/vnd.mapbox-vector-tile");
                    response.putHeader(CONTENT_ENCODING, "gzip");
                    response.end(Buffer.buffer(tile));
                } else {
                    LOG.fine("MapWidget tile not found: " + request.absoluteURI());
                    response.setStatusCode(404);
                }
            } catch (Exception ex) {
                LOG.log(Level.SEVERE, "Error getting map tile: " + request.absoluteURI(), ex);
                response.setStatusCode(500);
            } finally {
                context.next();
            }
        }, false); // Not ordered, execute blocking handler in parallel
    }

}
