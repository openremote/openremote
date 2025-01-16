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
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;
import org.openremote.container.web.WebService;
import org.openremote.manager.app.ConfigurationService;
import org.openremote.manager.security.ManagerIdentityService;
import org.openremote.manager.web.ManagerWebService;
import org.openremote.model.Container;
import org.openremote.model.ContainerService;
import org.openremote.model.manager.MapConfig;
import org.openremote.model.manager.MapSourceConfig;
import org.openremote.model.util.TextUtil;
import org.openremote.model.util.ValueUtil;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.openremote.container.util.MapAccess.getInteger;
import static org.openremote.container.util.MapAccess.getString;
import static org.openremote.container.web.WebService.pathStartsWithHandler;
import static org.openremote.manager.web.ManagerWebService.API_PATH;

public class MapService implements ContainerService {

    public static final String OR_MAP_TILES_CUSTOM_PATH = "manager/src/map/mapdata-custom.mbtiles";
    public static final String MAP_SHARED_DATA_BASE_URI = "/shared";
    public static final String OR_MAP_TILESERVER_HOST = "OR_MAP_TILESERVER_HOST";
    public static final String OR_MAP_TILESERVER_HOST_DEFAULT = null;
    public static final String OR_MAP_TILESERVER_PORT = "OR_MAP_TILESERVER_PORT";
    public static final int OR_MAP_TILESERVER_PORT_DEFAULT = 8082;
    public static final String RASTER_MAP_TILE_PATH = "/raster_map/tile";
    public static final String TILESERVER_TILE_PATH = "/styles/standard";
    public static final String OR_MAP_TILESERVER_REQUEST_TIMEOUT = "OR_MAP_TILESERVER_REQUEST_TIMEOUT";
    public static final int OR_MAP_TILESERVER_REQUEST_TIMEOUT_DEFAULT = 10000;
    private static final Logger LOG = Logger.getLogger(MapService.class.getName());
    private static final String DEFAULT_VECTOR_TILES_URL = "mbtiles://mapdata.mbtiles";
    private static ConfigurationService configurationService;
    // Shared SQL connection is fine concurrently in SQLite
    protected Connection connection;
    protected Metadata metadata;
    protected ObjectNode mapConfig;
    protected ConcurrentMap<String, ObjectNode> mapSettings = new ConcurrentHashMap<>();
    protected ConcurrentMap<String, ObjectNode> mapSettingsJs = new ConcurrentHashMap<>();

    public ObjectNode saveMapConfig(MapConfig mapConfiguration) throws RuntimeException {
        if (mapConfig == null) {
            mapConfig = ValueUtil.JSON.createObjectNode();
        }

        ObjectNode vectorTiles = ValueUtil.JSON.valueToTree(mapConfiguration.sources.get("vector_tiles"));
        if (vectorTiles.hasNonNull("url")) {
            if (!vectorTiles.get("url").textValue().contains("/{z}/{x}/{y}")) {
                throw new WebApplicationException(Response.Status.BAD_REQUEST);
            }
        } else {
            vectorTiles.put("url", DEFAULT_VECTOR_TILES_URL);
            mapConfiguration.sources.put("vector_tiles", ValueUtil.JSON.convertValue(vectorTiles, MapSourceConfig.class));
        }

        mapConfig.putPOJO("options", mapConfiguration.options);
        mapConfig.putPOJO("sources", mapConfiguration.sources);

        configurationService.saveMapConfig(mapConfig);
        mapConfig = configurationService.getMapConfig();
        mapSettings.clear();

        return mapConfig;
    }

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

            if (resultMap.isEmpty()) {
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

        configurationService = container.getService(ConfigurationService.class);

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
        setData();
    }

    public void setData() throws ClassNotFoundException, SQLException, NullPointerException {
        Path mapTilesPath = configurationService.getMapTilesPath();
        if (mapTilesPath == null) {
            return;
        }
        mapTilesPath = mapTilesPath.toAbsolutePath();
        if (!mapTilesPath.toFile().exists()) {
            return;
        }
        File parentDir = mapTilesPath.getParent().toFile();
        if (parentDir.isDirectory()) {
            String fileExtension = ".mbtiles";
            File[] matchingFiles = parentDir.listFiles((dir, name) -> !Objects.equals(name, "mapdata.mbtiles") && name.endsWith(fileExtension));

            if (matchingFiles != null && matchingFiles.length != 0) {
                mapTilesPath = matchingFiles[0].toPath().toAbsolutePath();
            }
        }

        Class.forName(org.sqlite.JDBC.class.getName());
        connection = DriverManager.getConnection("jdbc:sqlite:" + mapTilesPath);
        metadata = getMetadata(connection);

        if (metadata.isValid()) {
            mapConfig = configurationService.getMapConfig();
            if (mapConfig == null) {
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
    public ObjectNode getMapSettings(String realm, URI host) {
        String realmUriKey = realm + host.toString();
        if (mapSettings.containsKey(realmUriKey)) {
            return mapSettings.get(realmUriKey);
        }

        if (mapConfig == null) {
            return null;
        }

        final ObjectNode settings = mapSettings.computeIfAbsent(realmUriKey, r -> {
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
                    String url = vectorTilesObj.remove("url").textValue();

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
                    String tileUrl = !url.contentEquals(DEFAULT_VECTOR_TILES_URL)
                        ? url
                        : UriBuilder.fromUri(host)
                            .replacePath(API_PATH)
                            .path(realm)
                            .path("map/tile")
                            .build()
                            .toString() + "/{z}/{x}/{y}";
                    tilesArray.insert(0, tileUrl);
                    vectorTilesObj.replace("tiles", tilesArray);
                });

        // Set sprite URL to shared folder
        Optional.ofNullable(settings.has("sprite") && settings.get("sprite").isTextual() ? settings.get("sprite").asText() : null).ifPresent(sprite -> {
            String spriteUri =
                    UriBuilder.fromUri(host)
                            .replacePath(MAP_SHARED_DATA_BASE_URI)
                            .path(sprite)
                            .build().toString();
            settings.put("sprite", spriteUri);
        });

        // Set glyphs URL to shared folder (tileserver-gl glyphs url cannot contain a path segment so add /fonts here
        Optional.ofNullable(settings.has("glyphs") && settings.get("glyphs").isTextual() ? settings.get("glyphs").asText() : null).ifPresent(glyphs -> {
            String glyphsUri =
                    UriBuilder.fromUri(host)
                            .replacePath(MAP_SHARED_DATA_BASE_URI)
                            .build().toString() + "/fonts/" + glyphs;
            settings.put("glyphs", glyphsUri);
        });

        return settings;
    }

    /**
     * Dynamically build Mapbox JS settings based on mapsettings.json
     */
    public ObjectNode getMapSettingsJs(String realm, URI host) {
        String realmUriKey = realm + host.toString();
        if (mapSettingsJs.containsKey(realmUriKey)) {
            return mapSettingsJs.get(realmUriKey);
        }

        final ObjectNode settings = mapSettingsJs.computeIfAbsent(realmUriKey, r -> ValueUtil.JSON.createObjectNode());

        if (!metadata.isValid() || mapConfig.isEmpty()) {
            return settings;
        }

        ArrayNode tilesArray = ValueUtil.JSON.createArrayNode();
        String tileUrl = UriBuilder.fromUri(host).replacePath(RASTER_MAP_TILE_PATH).build().toString() + "/{z}/{x}/{y}.png";
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

    public boolean saveUploadedFile(InputStream fileInputStream) {
        // TODO: Specify target directory for uploaded file
        Path destinationPath = Paths.get("manager/src/map/", "mapdata-custom.mbtiles");

        try (OutputStream outputStream = Files.newOutputStream(destinationPath)) {
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = fileInputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            LOG.info("File uploaded successfully to: " + destinationPath.toAbsolutePath());
            this.setData();
            return true;
        } catch (IOException e) {
            LOG.log(Level.SEVERE, "Failed to save uploaded file", e);
            return false;
        } catch (SQLException | ClassNotFoundException e) {
            LOG.log(Level.SEVERE, "Failed to load uploaded file", e);
            return false;
        }
    }

    public boolean isCustomUploadedFile() {
        Path destinationPath = Paths.get("manager/src/map/", "mapdata-custom.mbtiles");
        return Files.exists(destinationPath);
    }

    public boolean deleteUploadedFile() {
        Path destinationPath = Paths.get("manager/src/map/", "mapdata-custom.mbtiles");
        boolean deleted = destinationPath.toFile().delete();
        if (deleted) {
            LOG.info("File deleted successfully");
        } else {
            LOG.severe("Failed to delete file");
        }
        try {
            this.setData();
        } catch (SQLException | ClassNotFoundException e) {
            LOG.log(Level.SEVERE, "Failed to load default file", e);
        }
        return deleted;
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
