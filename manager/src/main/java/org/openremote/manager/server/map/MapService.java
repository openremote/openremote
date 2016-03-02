package org.openremote.manager.server.map;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import static org.openremote.manager.server.Constants.*;

public class MapService {

    private static final Logger LOG = Logger.getLogger(MapService.class.getName());

    public static final String MAP_TILES_PATH = "MAP_TILES_PATH";
    public static final String MAP_TILES_PATH_DEFAULT = "sample/mapdata.mbtiles";
    public static final String MAP_SETTINGS_PATH = "MAP_SETTINGS_PATH";
    public static final String MAP_SETTINGS_PATH_DEFAULT = "sample/mapsettings.json";

    // Shared SQL connection is fine concurrently in SQLite
    protected Connection connection;

    protected Vertx vertx;
    protected boolean devMode;
    protected Path mapSettingsPath;
    protected JsonObject mapSettings;

    public void start(Vertx vertx, JsonObject config) {
        this.vertx = vertx;
        this.devMode = config.getBoolean(DEV_MODE, DEV_MODE_DEFAULT);

        Path mapTilesPath = Paths.get(config.getString(MAP_TILES_PATH, MAP_TILES_PATH_DEFAULT));
        if (!Files.isRegularFile(mapTilesPath)) {
            throw new IllegalStateException(
                "MapWidget tiles data file not found: " + mapTilesPath.toAbsolutePath()
            );
        }

        mapSettingsPath = Paths.get(config.getString(MAP_SETTINGS_PATH, MAP_SETTINGS_PATH_DEFAULT));
        if (!Files.isRegularFile(mapSettingsPath)) {
            throw new IllegalStateException(
                "MapWidget settings file not found: " + mapSettingsPath.toAbsolutePath()
            );
        }

        LOG.info("Starting map service with tile data: " + mapTilesPath.toAbsolutePath());
        try {
            Class.forName(org.sqlite.JDBC.class.getName());
            connection = DriverManager.getConnection("jdbc:sqlite:" + mapTilesPath.toAbsolutePath());
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }

        readMapSettings();
    }

    public void stop() {
        LOG.info("Stopping map service...");
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException ex) {
                LOG.warning("Error closing connection: " + ex);
            }
        }
    }

    public JsonObject getMapSettings(String tileUrl) {

        // Refresh map settings for every request in dev mode, cache it in production
        if (devMode) {
            readMapSettings();
        }

        JsonObject settingsCopy = mapSettings.copy();
        JsonArray tilesArray = new JsonArray();
        tilesArray.add(tileUrl);
        settingsCopy.getJsonObject("style").getJsonObject("sources").getJsonObject("vector_tiles").put("tiles", tilesArray);
        return settingsCopy;
    }

    public byte[] getMapTile(int zoom, int column, int row) throws Exception {
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
        } finally {
            closeQuietly(query, result);
        }
    }

    protected void readMapSettings() {

        // Mix settings from file with database metadata, and some hardcoded magic
        try {
            String mapSettingsJson = vertx.fileSystem().readFileBlocking(mapSettingsPath.toAbsolutePath().toString()).toString();
            mapSettings = new JsonObject(mapSettingsJson);
        } catch (Exception ex) {
            throw new RuntimeException("Error parsing map settings: " + mapSettingsPath.toAbsolutePath(), ex);
        }

        JsonObject style = mapSettings.getJsonObject("style");

        style.put("version", 8);

        style.put("glyphs", STATIC_PATH + "/fonts/{fontstack}/{range}.pbf");

        JsonObject sources = new JsonObject();
        style.put("sources", sources);

        JsonObject vectorTiles = new JsonObject();
        sources.put("vector_tiles", vectorTiles);

        vectorTiles.put("type", "vector");

        PreparedStatement query = null;
        ResultSet result = null;
        try {
            query = connection.prepareStatement("select NAME, VALUE from METADATA");
            result = query.executeQuery();

            Map<String, String> resultMap = new HashMap<>();
            while (result.next()) {
                resultMap.put(result.getString(1), result.getString(2));
            }

            if (resultMap.size() == 0) {
                throw new RuntimeException("Missing JSON metadata in map database");
            }

            JsonObject metadataJson = new JsonObject(resultMap.get("json"));
            vectorTiles.put("vector_layers", metadataJson.getJsonArray("vector_layers"));

            vectorTiles.put("maxzoom", Integer.valueOf(resultMap.get("maxzoom")));
            vectorTiles.put("minzoom", Integer.valueOf(resultMap.get("minzoom")));
            vectorTiles.put("attribution", resultMap.get("attribution"));

        } catch (Exception ex) {
            throw new RuntimeException(ex);
        } finally {
            closeQuietly(query, result);
        }
    }

    protected void closeQuietly(PreparedStatement query, ResultSet result) {
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

}