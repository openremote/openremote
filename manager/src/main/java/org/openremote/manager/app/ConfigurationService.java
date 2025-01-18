/*
 * Copyright 2022, OpenRemote Inc.
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
package org.openremote.manager.app;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.openremote.container.persistence.PersistenceService;
import org.openremote.container.timer.TimerService;
import org.openremote.container.util.CodecUtil;
import org.openremote.manager.security.ManagerIdentityService;
import org.openremote.manager.web.ManagerWebService;
import org.openremote.model.Container;
import org.openremote.model.ContainerService;
import org.openremote.model.file.FileInfo;
import org.openremote.model.manager.ManagerAppConfig;
import org.openremote.model.manager.ManagerAppRealmConfig;
import org.openremote.model.util.TextUtil;
import org.openremote.model.util.ValueUtil;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

import static org.openremote.container.util.MapAccess.getString;
import static org.openremote.manager.web.ManagerWebService.OR_CUSTOM_APP_DOCROOT;
import static org.openremote.manager.web.ManagerWebService.OR_CUSTOM_APP_DOCROOT_DEFAULT;

public class ConfigurationService implements ContainerService {

    public static final String OR_MAP_SETTINGS_PATH = "OR_MAP_SETTINGS_PATH";
    public static final String OR_MAP_SETTINGS_PATH_DEFAULT = "manager/src/map/mapsettings.json";

    public static final String OR_MAP_TILES_PATH = "OR_MAP_TILES_PATH";
    public static final String OR_MAP_TILES_PATH_DEFAULT = "manager/src/map/mapdata.mbtiles";

    protected ManagerIdentityService identityService;
    protected PersistenceService persistenceService;
    protected Path pathPublicRoot;

    private static final Logger LOG = Logger.getLogger(ConfigurationService.class.getName());

    protected Path mapTilesPath;
    protected Path mapSettingsPath;
    protected Path managerConfigPath;
    protected AtomicReference<ManagerAppConfig> managerAppConfig;

    @Override
    public void init(Container container) throws Exception {
        identityService = container.getService(ManagerIdentityService.class);
        persistenceService = container.getService(PersistenceService.class);
        pathPublicRoot = Paths.get(getString(container.getConfig(), OR_CUSTOM_APP_DOCROOT, OR_CUSTOM_APP_DOCROOT_DEFAULT));
        container.getService(ManagerWebService.class).addApiSingleton(
                new ConfigurationResourceImpl(
                        container.getService(TimerService.class),
                        identityService, this)
        );

        // Retrieve default configuration files; Try to find all possible default locations of each file, and then
        // return the first one. Since the stream maintains ordering, we use the first available one, since they're placed
        // below in order of most importance. Throw an Exception if no default file could be found.

        mapSettingsPath = Stream.of(getPersistedMapConfigPath().toString(), getString(container.getConfig(), OR_MAP_SETTINGS_PATH, OR_MAP_SETTINGS_PATH_DEFAULT), "/opt/map/mapsettings.json", OR_MAP_SETTINGS_PATH_DEFAULT)
                .map(Path::of)
                .map(Path::toAbsolutePath)
                .filter(Files::isRegularFile)
                .findFirst().orElse(null);

        mapTilesPath = Stream.of(getString(container.getConfig(), OR_MAP_TILES_PATH, OR_MAP_TILES_PATH_DEFAULT), "/deployment/map/mapdata.mbtiles", "/opt/map/mapdata.mbtiles", OR_MAP_TILES_PATH_DEFAULT)
                .map(Path::of)
                .map(Path::toAbsolutePath)
                .filter(Files::isRegularFile)
                .findFirst().orElse(null);

        managerConfigPath = Stream.of(getPersistedManagerConfigPath(), pathPublicRoot.resolve("manager_config.json"))
                .map(Path::toAbsolutePath)
                .filter(Files::isRegularFile)
                .findFirst().orElse(null);

        if (mapSettingsPath == null) {
            LOG.warning("Could not find map settings");
            return;
        }

        LOG.info("Configuration Service Used files:");
        LOG.info("\t- manager_config.json: " + managerConfigPath);
        LOG.info("\t- mapsettings.json: " + mapSettingsPath);
        LOG.info("\t- mapdata.mbtiles: " + mapTilesPath);
    }

    @Override
    public void start(Container container) throws Exception {
    }

    @Override
    public void stop(Container container) throws Exception {
        /* code not overridden yet */
    }

    @Override
    public String toString() {
        return "ConfigurationService{" +
                "mapTilesPath=" + mapTilesPath +
                ", mapSettingsPath=" + mapSettingsPath +
                ", managerConfigPath=" + managerConfigPath +
                '}';
    }

    public ObjectNode getMapConfig() {
        if (mapSettingsPath == null) {
            return null;
        }
        try {
            return (ObjectNode) ValueUtil.JSON.readTree(mapSettingsPath.toFile());
        } catch (IOException e) {
            LOG.severe("Could not read map_settings.json from " + mapSettingsPath);
        }

        return null;
    }

    public void saveMapConfig(ObjectNode mapConfiguration) throws RuntimeException {
        LOG.log(Level.INFO, "Saving map_settings.json to: " + getPersistedMapConfigPath());
        try {
            Path p = getPersistedMapConfigPath();
            File file = p.toAbsolutePath().toFile();
            if (!file.getParentFile().exists()) file.getParentFile().mkdirs();

            Files.writeString(p, ValueUtil.JSON.writeValueAsString(mapConfiguration), StandardCharsets.UTF_8);
            // Ensure mapSettingsPath is now pointing to persistence path
            mapSettingsPath = p;
        } catch (Exception exception) {
            String msg = "Error saving map_settings.json: msg=" + exception.getMessage();
            LOG.log(Level.WARNING, msg);
            throw new IllegalStateException(msg);
        }
    }

    public Path getMapTilesPath() {
        return mapTilesPath;
    }

    public ManagerAppConfig getManagerConfig() {
        if (managerAppConfig != null) {
            return managerAppConfig.get();
        }
        if (managerConfigPath == null) {
            return null;
        }
        try {
            String managerConfigStr = Files.readString(managerConfigPath, StandardCharsets.UTF_8);
            return ValueUtil.parse(managerConfigStr, ManagerAppConfig.class).orElse(null);
        } catch (Exception e) {
            LOG.severe("Could not read manager_config.json from " + managerConfigPath);
            return null;
        }
    }

    public void saveManagerConfig(ManagerAppConfig managerAppConfig) throws Exception {
        LOG.log(Level.INFO, "Saving manager_config.json to: " + getPersistedManagerConfigPath());

        try {
            // Check references to images
            managerAppConfig = checkAndFixImageReferences(managerAppConfig);
            Path p = getPersistedManagerConfigPath();
            File file = p.toAbsolutePath().toFile();
            if (!file.getParentFile().exists()) file.getParentFile().mkdirs();

            Files.writeString(p, ValueUtil.JSON.writeValueAsString(managerAppConfig), StandardCharsets.UTF_8);
            // Ensure managerConfigPath is now pointing to persistence path
            managerConfigPath = p;
            if (this.managerAppConfig == null) {
                this.managerAppConfig = new AtomicReference<>(managerAppConfig);
            } else {
                this.managerAppConfig.set(managerAppConfig);
            }
        } catch (Exception exception) {
            String msg = "Error saving manager_config.json: msg=" + exception.getMessage();
            LOG.log(Level.WARNING, msg);
            throw new Exception(msg);
        }
    }

    public void saveManagerConfigImage(String path, FileInfo fileInfo) throws Exception {
        LOG.log(Level.INFO, "Saving image in manager_config.json: " + fileInfo);
        path = path.replace("/images/", "");
        path = path.charAt(0) == '/' ? path.substring(1) : path;
        Path resolvedPath = Path.of(path);
        resolvedPath = getPersistedManagerConfigImagePath().resolve(resolvedPath);
        Path filePath = getPersistedManagerConfigImagePath().resolve(path);
        File file = filePath.toAbsolutePath().toFile();

        try {
            boolean isValid = resolvedPath.toFile().getCanonicalPath().contains(getPersistedManagerConfigImagePath().toFile().getCanonicalPath() + File.separator);

            if (!isValid) {
                String msg = "Failed to save manager config image path outside permitted directory: " + resolvedPath;
                LOG.warning(msg);
                throw new Exception("Failed to save manager config image path outside permitted directory: " + resolvedPath);
            }

            if (!file.getParentFile().exists()) file.getParentFile().mkdirs();
            Files.write(filePath, CodecUtil.decodeBase64(fileInfo.getContents()));
        } catch (Exception exception) {
            String msg = "Error saving image in manager_config.json '" + filePath + "': msg=" + exception.getMessage();
            LOG.log(Level.WARNING, msg);
            throw new Exception(msg);
        }
    }

    public Optional<File> getManagerConfigImage(String filename) {
        File file = getPersistedManagerConfigImagePath().resolve(filename).toFile();
        String checkPath;

        try {
            if (file.isFile()) {
                checkPath = getPersistedManagerConfigImagePath().toFile().getCanonicalPath();
            } else {
                // fallback to OR_CUSTOM_APP_DOCROOT
                file = pathPublicRoot.resolve("images").resolve(filename).toFile();
                checkPath = pathPublicRoot.toFile().getCanonicalPath();
            }

            //Check if the file retrieved is somewhere within the allowed directory.
            boolean isValid = file.getCanonicalPath().contains(checkPath + File.separator);
            if (!isValid) return Optional.empty();
        } catch (IOException e) {
            return Optional.empty();
        }

        return file.isFile() ? Optional.of(file) : Optional.empty();
    }

    protected Path getPersistedManagerConfigPath() {
        return persistenceService.getStorageDir().resolve("manager").resolve("manager_config.json");
    }

    protected Path getPersistedMapConfigPath() {
        return persistenceService.getStorageDir().resolve("manager").resolve("mapsettings.json");
    }

    protected Path getPersistedManagerConfigImagePath() {
        return this.persistenceService.resolvePath("manager").resolve("images");
    }

    protected ManagerAppConfig checkAndFixImageReferences(ManagerAppConfig managerAppConfig) {

        if (!getPersistedManagerConfigImagePath().toFile().exists()) getPersistedManagerConfigImagePath().toFile().mkdirs();
        if (managerAppConfig.getRealms() != null && !managerAppConfig.getRealms().isEmpty()) {
            managerAppConfig.getRealms().values().forEach(managerAppRealmConfig -> {
                if (!TextUtil.isNullOrEmpty(managerAppRealmConfig.getLogo())) {
                    managerAppRealmConfig.setLogo(fixImageRef(managerAppRealmConfig.getLogo()));
                }
                if (!TextUtil.isNullOrEmpty(managerAppRealmConfig.getLogoMobile())) {
                    managerAppRealmConfig.setLogo(fixImageRef(managerAppRealmConfig.getLogoMobile()));
                }
                if (!TextUtil.isNullOrEmpty(managerAppRealmConfig.getFavicon())) {
                    managerAppRealmConfig.setLogo(fixImageRef(managerAppRealmConfig.getFavicon()));
                }
            });
        }
        return managerAppConfig;
    }

    protected String fixImageRef(String image) {
        if (!image.isBlank()) {
            image = image.replace("/api/master/configuration/manager/image/", "");
            image = image.replace("images/", "");
            image = image.charAt(0) == '/' ? image.substring(1) : image;
            Path imagePath = Path.of(image);
            Path persistedImagePath = getPersistedManagerConfigImagePath().resolve(imagePath).toAbsolutePath();
            Path path = pathPublicRoot.resolve("images").resolve(imagePath).toAbsolutePath();

            if (!Files.isRegularFile(persistedImagePath)) {
                if (Files.isRegularFile(path)) {
                    try {
                        // Image doesn't exist in persisted path but does exist in doc root so copy it
                        Files.copy(path, persistedImagePath);
                        // Change the reference in the config to the typical API-like reference:
                        image = "/api/master/configuration/manager/image/" + imagePath;
                    } catch (Exception e) {
                        LOG.warning("Error occurred whilst copying manager config image to persisted path: " + imagePath);
                    }
                } else {
                    LOG.warning("manager_config.json image reference doesn't exist: " + imagePath);
                }
            }
        }
        return image;
    }
}
