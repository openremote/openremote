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
package org.openremote.manager.map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.undertow.server.HttpHandler;
import io.undertow.server.handlers.ResponseCodeHandler;
import io.undertow.server.handlers.proxy.ProxyHandler;
import org.openremote.container.web.WebService;
import org.openremote.manager.security.ManagerIdentityService;
import org.openremote.manager.web.ManagerWebService;
import org.openremote.model.Container;
import org.openremote.model.ContainerService;
import org.openremote.model.util.TextUtil;
import org.openremote.model.util.ValueUtil;

import javax.ws.rs.core.UriBuilder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.openremote.container.util.MapAccess.getInteger;
import static org.openremote.container.util.MapAccess.getString;
import static org.openremote.container.web.WebService.pathStartsWithHandler;
import static org.openremote.manager.web.ManagerWebService.API_PATH;

public class MapService implements ContainerService {

    public static final String MAP_SHARED_DATA_BASE_URI = "/shared";
    public static final String OR_MAP_TILES_PATH = "OR_MAP_TILES_PATH";
    public static final String OR_MAP_TILES_PATH_DEFAULT = "manager/src/map/mapdata.mbtiles";
    public static final String OR_MAP_SETTINGS_PATH = "OR_MAP_SETTINGS_PATH";
    public static final String OR_MAP_SETTINGS_PATH_DEFAULT = "manager/src/map/mapsettings.json";
    public static final String OR_MAP_TILESERVER_HOST = "OR_MAP_TILESERVER_HOST";
    public static final String OR_MAP_TILESERVER_HOST_DEFAULT = null;
    public static final String OR_MAP_TILESERVER_PORT = "OR_MAP_TILESERVER_PORT";
    public static final int OR_MAP_TILESERVER_PORT_DEFAULT = 8082;
    public static final String RASTER_MAP_TILE_PATH = "/raster_map/tile";
    public static final String TILESERVER_TILE_PATH = "/styles/standard";
    public static final String OR_MAP_TILESERVER_REQUEST_TIMEOUT = "OR_MAP_TILESERVER_REQUEST_TIMEOUT";
    public static final int OR_MAP_TILESERVER_REQUEST_TIMEOUT_DEFAULT = 10000;
    private static final Logger LOG = Logger.getLogger(MapService.class.getName());
    // Shared SQL connection is fine concurrently in SQLite
    protected Connection connection;
    protected Path mapTilesPath;
    protected Path mapSettingsPath;
    protected Metadata metadata;
    protected ObjectNode mapConfig;
    protected ObjectNode mapSource;
    protected Map<String, ObjectNode> mapSettings = new HashMap<>();
    protected Map<String, ObjectNode> mapSettingsJs = new HashMap<>();

    protected static Metadata getMetadata(Connection connection) {

        PreparedStatement query = null;
        ResultSet result = null;
        Metadata metadata = null;

        try {
            query = connection.prepareStatement("select NAME, VALUE from METADATA");
            result = query.executeQuery();

            Map<String, String> resultMap = new HashMap<>();
            while (result.next()) {
                resultMap.put(result.getString(1), result.getString(2));
            }

            if (resultMap.size() == 0) {
                return new Metadata();
            }

            String attribution = resultMap.get("attribution");
            int maxZoom = Integer.parseInt(resultMap.get("maxzoom"));
            int minZoom = Integer.parseInt(resultMap.get("minzoom"));

            ArrayNode vectorLayer = resultMap.containsKey("json") ? (ArrayNode) ValueUtil.JSON.readTree(resultMap.get("json")).get("vector_layers") : null;
            ArrayNode center = resultMap.containsKey("center") ? (ArrayNode) ValueUtil.JSON.readTree("[" + resultMap.get("center") + "]") : null;
            ArrayNode bounds = resultMap.containsKey("bounds") ? (ArrayNode) ValueUtil.JSON.readTree("[" + resultMap.get("bounds") + "]") : null;

            if (!TextUtil.isNullOrEmpty(attribution) && vectorLayer != null && !vectorLayer.isEmpty() && maxZoom > 0) {
                metadata = new Metadata(attribution, vectorLayer, bounds, center, maxZoom, minZoom);
            }
        } catch (Exception ex) {
            metadata = new Metadata();
            LOG.log(Level.SEVERE, "Failed to get metadata from mbtiles DB", ex);
        } finally {
            closeQuietly(query, result);
        }

        return metadata;
    }

    protected static ObjectNode loadMapSettingsJson(Path mapSettingsPath) {
        ObjectNode mapSettings = null;

        try {
            mapSettings = (ObjectNode) ValueUtil.JSON.readTree(Files.readAllBytes(mapSettingsPath));
        } catch (Exception ex) {
            LOG.log(Level.SEVERE, "Failed to extract map config from: " + mapSettingsPath.toAbsolutePath(), ex);
        }
        return mapSettings;
    }

    protected static void closeQuietly(PreparedStatement query, ResultSet result) {
        try {
            if (result != null) {
                result.close();
            }
            if (query != null) {
                query.close();
            }
        } catch (Exception ex) {
            LOG.warning("Error closing query/result: " + ex);
        }
    }

    @Override
    public int getPriority() {
        return ContainerService.DEFAULT_PRIORITY;
    }

    @Override
    public void init(Container container) throws Exception {

        mapTilesPath = Paths.get(getString(container.getConfig(), OR_MAP_TILES_PATH, OR_MAP_TILES_PATH_DEFAULT));
        if (!Files.isRegularFile(mapTilesPath)) {
            LOG.warning("Map tiles data file not found '" + mapTilesPath.toAbsolutePath() + "', falling back to built in map");
            mapTilesPath = null;
        }

        mapSettingsPath = Paths.get(getString(container.getConfig(), OR_MAP_SETTINGS_PATH, OR_MAP_SETTINGS_PATH_DEFAULT));
        if (!Files.isRegularFile(mapSettingsPath)) {
            LOG.warning("Map settings file not found '" + mapSettingsPath.toAbsolutePath() + "', falling back to built in map settings");
            mapSettingsPath = null;
        }

//        if (mapTilesPath == null || mapSettingsPath == null) {
//            return;
//        }

        if (mapTilesPath == null) {
            if (Files.isRegularFile(Paths.get("/deployment/map/mapdata.mbtiles"))) {
                mapTilesPath = Paths.get("/deployment/map/mapdata.mbtiles");
            } else if (Files.isRegularFile(Paths.get("/opt/map/mapdata.mbtiles"))) {
                mapTilesPath = Paths.get("/opt/map/mapdata.mbtiles");
            } else if (Files.isRegularFile(Paths.get("manager/src/map/mapdata.mbtiles"))) {
                mapTilesPath = Paths.get("manager/src/map/mapdata.mbtiles");
            }
        }

        if (mapSettingsPath == null) {
            if (Files.isRegularFile(Paths.get("/opt/map/mapsettings.json"))) {
                mapSettingsPath = Paths.get("/opt/map/mapsettings.json");
            } else if (Files.isRegularFile(Paths.get("manager/src/map/mapsettings.json"))) {
                mapSettingsPath = Paths.get("manager/src/map/mapsettings.json");
            }
        }

        container.getService(ManagerWebService.class).addApiSingleton(
                new MapResourceImpl(this, container.getService(ManagerIdentityService.class))
        );

        String tileServerHost = getString(container.getConfig(), OR_MAP_TILESERVER_HOST, OR_MAP_TILESERVER_HOST_DEFAULT);
        int tileServerPort = getInteger(container.getConfig(), OR_MAP_TILESERVER_PORT, OR_MAP_TILESERVER_PORT_DEFAULT);

        if (!TextUtil.isNullOrEmpty(tileServerHost)) {

            WebService webService = container.getService(WebService.class);

            UriBuilder tileServerUri = UriBuilder.fromPath("/")
                    .scheme("http")
                    .host(tileServerHost)
                    .port(tileServerPort);

            @SuppressWarnings("deprecation")
            ProxyHandler proxyHandler = new ProxyHandler(
                    new io.undertow.server.handlers.proxy.SimpleProxyClientProvider(tileServerUri.build()),
                    getInteger(container.getConfig(), OR_MAP_TILESERVER_REQUEST_TIMEOUT, OR_MAP_TILESERVER_REQUEST_TIMEOUT_DEFAULT),
                    ResponseCodeHandler.HANDLE_404
            ).setReuseXForwarded(true);

            HttpHandler proxyWrapper = exchange -> {
                // Change request path to match what the tile server expects
                String path = exchange.getRequestPath().substring(RASTER_MAP_TILE_PATH.length());

                exchange.setRequestURI(TILESERVER_TILE_PATH + path, true);
                exchange.setRequestPath(TILESERVER_TILE_PATH + path);
                exchange.setRelativePath(TILESERVER_TILE_PATH + path);
                proxyHandler.handleRequest(exchange);
            };

            webService.getRequestHandlers().add(0, pathStartsWithHandler("Raster Map Tile Proxy", RASTER_MAP_TILE_PATH, proxyWrapper));
        }
    }

    @Override
    public void start(Container container) throws Exception {

        if (mapTilesPath == null || mapSettingsPath == null) {
            return;
        }

        LOG.info("Starting map service with tile data: " + mapTilesPath.toAbsolutePath());
        Class.forName(org.sqlite.JDBC.class.getName());
        connection = DriverManager.getConnection("jdbc:sqlite:" + mapTilesPath.toAbsolutePath());

        metadata = getMetadata(connection);
        if (metadata.isValid()) {
            mapConfig = loadMapSettingsJson(mapSettingsPath);
            if (mapConfig == null) {
                LOG.warning("Map config could not be loaded from '" + mapSettingsPath.toAbsolutePath() + "', map functionality will not work");
                return;
            }
        } else {
            LOG.warning("Map meta data could not be loaded, map functionality will not work");
            return;
        }

        ObjectNode options = Optional.ofNullable((ObjectNode)mapConfig.get("options")).orElse(mapConfig.objectNode());
        ObjectNode defaultOptions = Optional.ofNullable((ObjectNode)options.get("default")).orElse(mapConfig.objectNode());
        options.replace("default", defaultOptions);
        mapConfig.replace("options", options);

        if (!defaultOptions.has("maxZoom")) {
            defaultOptions.put("maxZoom", metadata.maxZoom);
        }
        if (!defaultOptions.has("minZoom")) {
            defaultOptions.put("minZoom", metadata.minZoom);
        }

        if (metadata.getCenter() != null) {
            if (!defaultOptions.has("center")) {
                ArrayNode center = metadata.getCenter().deepCopy();
                center.remove(2);
                defaultOptions.set("center", center);
            }
            if (!defaultOptions.has("zoom")) {
                defaultOptions.put("zoom", metadata.getCenter().get(2).asDouble(13d));
            }
        }

        if (!defaultOptions.has("bounds") && metadata.getBounds() != null) {
            defaultOptions.set("bounds", metadata.getBounds());
        }
    }

    @Override
    public void stop(Container container) throws Exception {
        if (connection != null) {
            connection.close();
        }
    }

    /**
     * Dynamically build Mapbox GL settings based on mapsettings.json
     */
    public ObjectNode getMapSettings(String realm, UriBuilder baseUriBuilder) {

        if (mapSettings.containsKey(realm)) {
            return mapSettings.get(realm);
        }

        if (mapConfig == null) {
            return null;
        }

        final ObjectNode settings = mapSettings.computeIfAbsent(realm, r -> {
            if (metadata.isValid() && !mapConfig.isEmpty()) {
                // Use config as a settings base and convert URLs
                return mapConfig.deepCopy();
            }
            return mapConfig.objectNode();
        });

        if (!metadata.isValid() || mapConfig.isEmpty()) {
            return settings;
        }

        // Set vector_tiles URL to MapResource getSource endpoint
        Optional.ofNullable(settings.get("sources"))
                .map(s -> s.get("vector_tiles"))
                .filter(JsonNode::isObject)
                .ifPresent(vectorTilesNode -> {
                    ObjectNode vectorTilesObj = (ObjectNode)vectorTilesNode;
                    vectorTilesObj.remove("url");

                    vectorTilesObj.put("attribution", metadata.attribution);
                    vectorTilesObj.put("maxzoom", metadata.maxZoom);
                    vectorTilesObj.put("minzoom", metadata.minZoom);
                    vectorTilesObj.replace("vector_layers", metadata.vectorLayers);

                    Optional.ofNullable(mapConfig.get("center")).ifPresent(center ->
                            Optional.ofNullable(mapConfig.has("zoom") && mapConfig.get("zoom").isInt() ? mapConfig.get("zoom") : null).ifPresent(zoom -> {
                                ArrayNode centerArray = center.deepCopy();
                                centerArray.add(zoom);
                                vectorTilesObj.replace("center", centerArray);
                            }));

                    ArrayNode tilesArray = mapConfig.arrayNode();
                    String tileUrl = baseUriBuilder.clone().replacePath(API_PATH).path(realm).path("map/tile").build().toString() + "/{z}/{x}/{y}";
                    tilesArray.insert(0, tileUrl);
                    vectorTilesObj.replace("tiles", tilesArray);

//                    vectorTilesObj.put(
//                            "url",
//                            baseUriBuilder.clone()
//                                    .replacePath(API_PATH)
//                                    .path(realm)
//                                    .path("map/source")
//                                    .build()
//                                    .toString());
                });

        // Set sprite URL to shared folder
        Optional.ofNullable(settings.has("sprite") && settings.get("sprite").isTextual() ? settings.get("sprite").asText() : null).ifPresent(sprite -> {
            String spriteUri =
                    baseUriBuilder.clone()
                            .replacePath(MAP_SHARED_DATA_BASE_URI)
                            .path(sprite)
                            .build().toString();
            settings.put("sprite", spriteUri);
        });

        // Set glyphs URL to shared folder (tileserver-gl glyphs url cannot contain a path segment so add /fonts here
        Optional.ofNullable(settings.has("glyphs") && settings.get("glyphs").isTextual() ? settings.get("glyphs").asText() : null).ifPresent(glyphs -> {
            String glyphsUri =
                    baseUriBuilder.clone()
                            .replacePath(MAP_SHARED_DATA_BASE_URI)
                            .build().toString() + "/fonts/" + glyphs;
            settings.put("glyphs", glyphsUri);
        });

        return settings;
    }

    /**
     * Dynamically build Mapbox JS settings based on mapsettings.json
     */
    public ObjectNode getMapSettingsJs(String realm, UriBuilder baseUriBuilder) {

        if (mapSettingsJs.containsKey(realm)) {
            return mapSettingsJs.get(realm);
        }

        final ObjectNode settings = mapSettingsJs.computeIfAbsent(realm, r -> ValueUtil.JSON.createObjectNode());

        if (!metadata.isValid() || mapConfig.isEmpty()) {
            return settings;
        }

        ArrayNode tilesArray = ValueUtil.JSON.createArrayNode();
        String tileUrl = baseUriBuilder.clone().replacePath(RASTER_MAP_TILE_PATH).build().toString() + "/{z}/{x}/{y}.png";
        tilesArray.insert(0, tileUrl);

        settings.replace("options", mapConfig.has("options") && mapConfig.get("options").isObject() ? (ObjectNode)mapConfig.get("options") : null);

        settings.put("attribution", metadata.attribution);
        settings.put("format", "png");
        settings.put("type", "baselayer");
        settings.replace("tiles", tilesArray);

        return settings;
    }

    public byte[] getMapTile(int zoom, int column, int row) {
        // Flip y, oh why
        row = Double.valueOf(Math.pow(2, zoom) - 1 - row).intValue();

        PreparedStatement query = null;
        ResultSet result = null;
        try {
            query = connection.prepareStatement(
                "select TILE_DATA from TILES where ZOOM_LEVEL = ? and TILE_COLUMN = ? and TILE_ROW = ?"
            );

            int index = 0;
            query.setInt(++index, zoom);
            query.setInt(++index, column);
            query.setInt(++index, row);

            result = query.executeQuery();

            if (result.next()) {
                return result.getBytes(1);
            } else {
                return null;
            }
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        } finally {
            closeQuietly(query, result);
        }
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
                "mapTilesPath=" + mapTilesPath +
                ", mapSettingsPath=" + mapSettingsPath +
                '}';
    }

    protected static final class Metadata {
        protected String attribution;
        protected ArrayNode vectorLayers;
        protected int maxZoom;
        protected int minZoom;
        protected ArrayNode bounds;
        protected ArrayNode center;
        protected boolean valid;

        public Metadata(String attribution, ArrayNode vectorLayers, ArrayNode bounds, ArrayNode center, int maxZoom, int minZoom) {
            this.attribution = attribution;
            this.vectorLayers = vectorLayers;
            this.bounds = bounds;
            this.center = center;
            this.maxZoom = maxZoom;
            this.minZoom = minZoom;
            valid = true;
        }

        public Metadata() {
        }

        public String getAttribution() {
            return attribution;
        }

        public ArrayNode getVectorLayers() {
            return vectorLayers;
        }

        public ArrayNode getBounds() {
            return bounds;
        }

        public ArrayNode getCenter() {
            return center;
        }

        public int getMaxZoom() {
            return maxZoom;
        }

        public int getMinZoom() {
            return minZoom;
        }

        public boolean isValid() {
            return valid;
        }
    }
}
