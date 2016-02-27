package org.openremote.manager.server.map;

import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import org.openremote.manager.server.identity.IdentityService;
import org.openremote.manager.server.web.HttpRouter;
import org.openremote.manager.server.web.ResponseException;

import java.util.logging.Logger;

import static io.vertx.core.http.HttpHeaders.CONTENT_ENCODING;
import static io.vertx.core.http.HttpHeaders.CONTENT_TYPE;
import static io.vertx.core.http.HttpMethod.GET;
import static org.openremote.manager.server.util.UrlUtil.url;

public class MapRouter extends HttpRouter {

    private static final Logger LOG = Logger.getLogger(MapRouter.class.getName());

    protected final MapService mapService;

    public MapRouter(Vertx vertx, IdentityService identityService, MapService mapService) {
        super(vertx, identityService);
        this.mapService = mapService;

        route(GET, "/").handler(rc -> {
            checkManagerAccess(rc, "read:map");
            HttpServerResponse response = rc.response();
            String tileUrl = url(rc, getRealm(rc), "map", "tile").toString() + "/{z}/{x}/{y}";
            response.putHeader(CONTENT_TYPE, "application/json");
            response.end(mapService.getMapSettings(tileUrl).toJson());
        });

        route(GET, "/tile/:zoom/:column/:row").blockingHandler(rc -> { // Blocking!
            HttpServerRequest request = rc.request();
            HttpServerResponse response = rc.response();
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
                    rc.fail(new ResponseException(404, "Map tile not found"));
                }
            } catch (Exception ex) {
                rc.fail(ex);
            }
        }, false); // Not ordered, execute blocking handler in parallel
    }

}
