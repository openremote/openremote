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

import io.undertow.server.HttpHandler;
import io.undertow.server.handlers.ResponseCodeHandler;
import io.undertow.server.handlers.cache.CacheHandler;
import io.undertow.server.handlers.cache.DirectBufferCache;
import io.undertow.server.handlers.proxy.ProxyHandler;
import org.openremote.container.Container;
import org.openremote.container.ContainerService;
import org.openremote.container.web.WebService;
import org.openremote.manager.security.ManagerIdentityService;
import org.openremote.manager.web.ManagerWebService;
import org.openremote.model.util.TextUtil;
import org.openremote.model.value.ArrayValue;
import org.openremote.model.value.ObjectValue;
import org.openremote.model.value.Values;

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
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.openremote.container.util.MapAccess.getInteger;
import static org.openremote.container.util.MapAccess.getString;
import static org.openremote.container.web.WebService.pathStartsWithHandler;
import static org.openremote.manager.web.ManagerWebService.API_PATH;

public class MapService implements ContainerService {

    public static final String MAP_TILES_PATH = "MAP_TILES_PATH";
    public static final String MAP_TILES_PATH_DEFAULT = "deployment/map/mapdata.mbtiles";
    public static final String MAP_SETTINGS_PATH = "MAP_SETTINGS_PATH";
    public static final String MAP_SETTINGS_PATH_DEFAULT = "deployment/map/mapsettings.json";
    public static final String MAP_TILESERVER_HOST = "MAP_TILESERVER_HOST";
    public static final String MAP_TILESERVER_HOST_DEFAULT = null;
    public static final String MAP_TILESERVER_PORT = "MAP_TILESERVER_PORT";
    public static final int MAP_TILESERVER_PORT_DEFAULT = 8082;
    public static final String RASTER_MAP_TILE_PATH = "/raster_map/tile";
    public static final String TILESERVER_TILE_PATH = "/styles/standard";
    public static final String MAP_TILESERVER_REQUEST_TIMEOUT = "MAP_TILESERVER_REQUEST_TIMEOUT";
    public static final int MAP_TILESERVER_REQUEST_TIMEOUT_DEFAULT = 10000;
    private static final Logger LOG = Logger.getLogger(MapService.class.getName());
    // Shared SQL connection is fine concurrently in SQLite
    protected Connection connection;
    protected Path mapTilesPath;
    protected Path mapSettingsPath;
    protected Metadata metadata;
    protected ObjectValue mapConfig;
    protected ObjectValue mapSource;
    protected ObjectValue mapSettings;
    protected ObjectValue mapSettingsJs;

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
            ArrayValue vectorLayer = Values.<ObjectValue>parse(resultMap.get("json")).flatMap(json -> json.getArray("vector_layers")).orElse(null);
            int maxZoom = Integer.valueOf(resultMap.get("maxzoom"));
            int minZoom = Integer.valueOf(resultMap.get("minzoom"));
            if (!TextUtil.isNullOrEmpty(attribution) && vectorLayer != null && !vectorLayer.isEmpty() && maxZoom > 0) {
                metadata = new Metadata(attribution, vectorLayer, maxZoom, minZoom);
            }
        } catch (Exception ex) {
            metadata = new Metadata();
            LOG.log(Level.SEVERE, "Failed to get metadata from mbtiles DB", ex);
        } finally {
            closeQuietly(query, result);
        }

        return metadata;
    }

    protected static ObjectValue getMapConfig(Path mapSettingsPath) {
        ObjectValue mapSettings = Values.createObject();

        try {
            String mapSettingsJson = new String(Files.readAllBytes(mapSettingsPath), "utf-8");
            mapSettings = Values.<ObjectValue>parse(mapSettingsJson).orElseGet(Values::createObject);
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
    public void init(Container container) throws Exception {

        mapTilesPath = Paths.get(getString(container.getConfig(), MAP_TILES_PATH, MAP_TILES_PATH_DEFAULT));
        if (!Files.isRegularFile(mapTilesPath)) {
            throw new IllegalStateException(
                    "Map tiles data file not found (wrong working directory?): " + mapTilesPath.toAbsolutePath()
            );
        }

        mapSettingsPath = Paths.get(getString(container.getConfig(), MAP_SETTINGS_PATH, MAP_SETTINGS_PATH_DEFAULT));
        if (!Files.isRegularFile(mapSettingsPath)) {
            throw new IllegalStateException(
                    "Map settings file not found: " + mapSettingsPath.toAbsolutePath()
            );
        }

        container.getService(ManagerWebService.class).getApiSingletons().add(
                new MapResourceImpl(this, container.getService(ManagerIdentityService.class))
        );

        String tileServerHost = getString(container.getConfig(), MAP_TILESERVER_HOST, MAP_TILESERVER_HOST_DEFAULT);
        int tileServerPort = getInteger(container.getConfig(), MAP_TILESERVER_PORT, MAP_TILESERVER_PORT_DEFAULT);

        if (!TextUtil.isNullOrEmpty(tileServerHost)) {

            WebService webService = container.getService(WebService.class);

            UriBuilder tileServerUri = UriBuilder.fromPath("/")
                    .scheme("http")
                    .host(tileServerHost)
                    .port(tileServerPort);

            ProxyHandler proxyHandler = new ProxyHandler(
                    new io.undertow.server.handlers.proxy.SimpleProxyClientProvider(tileServerUri.build()),
                    getInteger(container.getConfig(), MAP_TILESERVER_REQUEST_TIMEOUT, MAP_TILESERVER_REQUEST_TIMEOUT_DEFAULT),
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

            CacheHandler cacheHandler = new CacheHandler(new DirectBufferCache())

            webService.getRequestHandlers().add(0, pathStartsWithHandler("Raster Map Tile Proxy", RASTER_MAP_TILE_PATH, proxyWrapper));
        }
    }

    @Override
    public void start(Container container) throws Exception {
        LOG.info("Starting map service with tile data: " + mapTilesPath.toAbsolutePath());
        Class.forName(org.sqlite.JDBC.class.getName());
        connection = DriverManager.getConnection("jdbc:sqlite:" + mapTilesPath.toAbsolutePath());

        metadata = getMetadata(connection);
        if (metadata.isValid()) {
            mapConfig = getMapConfig(mapSettingsPath);
            if (!mapConfig.hasKeys()) {
                LOG.warning("Map config could not be loaded from '" + mapSettingsPath.toAbsolutePath() + "', map functionality will not work");
                return;
            }
        } else {
            LOG.warning("Map meta data could not be loaded, map functionality will not work");
            return;
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
    public ObjectValue getMapSettings(String realm, UriBuilder baseUriBuilder) {

        if (mapSettings != null) {
            return mapSettings;
        }

        mapSettings = Values.createObject();

        if (!metadata.isValid() || !mapConfig.hasKeys()) {
            return mapSettings;
        }

        // Use config as a settings base and convert URLs
        mapSettings = mapConfig.deepCopy();

        // Set vector_tiles URL to MapResource getSource endpoint
        mapSettings.getObject("sources")
                .flatMap(s -> s.getObject("vector_tiles"))
                .ifPresent(vectorTilesObj ->
                        vectorTilesObj.put(
                                "url",
                                baseUriBuilder.clone()
                                        .replacePath(API_PATH)
                                        .path(realm)
                                        .path("map/source")
                                        .build()
                                        .toString()));

        // Set sprite URL to shared folder
        mapSettings.getString("sprite").ifPresent(sprite -> {
            String spriteUri =
                    baseUriBuilder.clone()
                            .replacePath(ManagerWebService.SHARED_PATH)
                            .path(sprite)
                            .build().toString();
            mapSettings.put("sprite", spriteUri);
        });

        // Set glyphs URL to shared folder (tileserver-gl glyphs url cannot contain a path segment so add /fonts here
        mapSettings.getString("glyphs").ifPresent(glyphs -> {
            String glyphsUri =
                    baseUriBuilder.clone()
                            .replacePath(ManagerWebService.SHARED_PATH)
                            .build().toString() + "/fonts/" + glyphs;
            mapSettings.put("glyphs", glyphsUri);
        });

        if (!mapSettings.hasKey("maxZoom")) {
            mapSettings.put("maxZoom", metadata.maxZoom);
        }
        if (!mapSettings.hasKey("minZoom")) {
            mapSettings.put("minZoom", metadata.minZoom);
        }

        return mapSettings;
    }

    /**
     * Dynamically build Mapbox GL sources using metadata from mbtiles DB
     */
    public ObjectValue getMapSource(String realm, UriBuilder baseUriBuilder) {

        if (mapSource != null) {
            return mapSource;
        }

        mapSource = Values.createObject();

        if (!metadata.isValid() || !mapConfig.hasKeys()) {
            return mapSource;
        }

        mapSource.put("attribution", metadata.attribution);
        mapSource.put("maxzoom", metadata.maxZoom);
        mapSource.put("minzoom", metadata.minZoom);
        mapSource.put("vector_layers", metadata.vectorLayers);

        mapConfig.getArray("center").ifPresent(center ->
                mapConfig.getNumber("zoom").ifPresent(zoom -> {
                    ArrayValue centerArray = center.deepCopy();
                    centerArray.add(Values.create(zoom));
                    mapSource.put("center", centerArray);
                }));

        ArrayValue tilesArray = Values.createArray();
        String tileUrl = baseUriBuilder.clone().replacePath(API_PATH).path(realm).path("map/tile").build().toString() + "/{z}/{x}/{y}";
        tilesArray.set(0, tileUrl);
        mapSource.put("tiles", tilesArray);

        return mapSource;
    }

    /**
     * Dynamically build Mapbox JS settings based on mapsettings.json
     */
    public ObjectValue getMapSettingsJs(String realm, UriBuilder baseUriBuilder) {

        if (mapSettingsJs != null) {
            return mapSettingsJs;
        }

        mapSettingsJs = Values.createObject();

        if (!metadata.isValid() || !mapConfig.hasKeys()) {
            return mapSettingsJs;
        }

        mapSettingsJs.put("attribution", metadata.attribution);
        mapSettingsJs.put("maxzoom", mapConfig.getNumber("maxZoom").map(Double::intValue).orElse(metadata.maxZoom));
        mapSettingsJs.put("minzoom", mapConfig.getNumber("minZoom").map(Double::intValue).orElse(metadata.minZoom));
        if (mapConfig.hasKey("zoom")) {
            mapSettingsJs.put("zoom", mapConfig.getNumber("zoom").orElse(0d));
        }
        mapSettingsJs.put("format", "png");
        mapSettingsJs.put("type", "baselayer");

        mapConfig.getArray("center").ifPresent(center ->
            mapConfig.getNumber("zoom").ifPresent(zoom -> {
                    ArrayValue centerArray = center.deepCopy();
                    centerArray.add(Values.create(zoom));
                    mapSettingsJs.put("center", centerArray);
                }));

        mapConfig.getArray("maxBounds").ifPresent(maxBounds -> mapSettingsJs.put("bounds", maxBounds.deepCopy()));

        ArrayValue tilesArray = Values.createArray();
        String tileUrl = baseUriBuilder.clone().replacePath(RASTER_MAP_TILE_PATH).build().toString() + "/{z}/{x}/{y}.png";
        tilesArray.set(0, tileUrl);
        mapSettingsJs.put("tiles", tilesArray);

        return mapSettingsJs;
    }

    public byte[] getMapTile(int zoom, int column, int row) {
        // Flip y, oh why
        row = new Double(Math.pow(2, zoom) - 1 - row).intValue();

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
        protected ArrayValue vectorLayers;
        protected int maxZoom;
        protected int minZoom;
        protected boolean valid;

        public Metadata(String attribution, ArrayValue vectorLayers, int maxZoom, int minZoom) {
            this.attribution = attribution;
            this.vectorLayers = vectorLayers;
            this.maxZoom = maxZoom;
            this.minZoom = minZoom;
            valid = true;
        }

        public Metadata() {
        }

        public String getAttribution() {
            return attribution;
        }

        public ArrayValue getVectorLayers() {
            return vectorLayers;
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