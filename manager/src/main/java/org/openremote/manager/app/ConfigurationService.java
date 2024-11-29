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
import jakarta.ws.rs.NotFoundException;
import org.apache.activemq.artemis.core.remoting.CertificateUtil;
import org.apache.camel.builder.RouteBuilder;
import org.openremote.container.persistence.PersistenceService;
import org.openremote.container.timer.TimerService;
import org.openremote.container.util.CodecUtil;
import org.openremote.manager.security.ManagerIdentityService;
import org.openremote.manager.web.ManagerWebService;
import org.openremote.model.Container;
import org.openremote.model.ContainerService;
import org.openremote.model.file.FileInfo;
import org.openremote.model.rules.flow.Option;
import org.openremote.model.util.ValueUtil;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

import static org.openremote.manager.web.ManagerWebService.OR_CUSTOM_APP_DOCROOT;
import static org.openremote.manager.web.ManagerWebService.OR_CUSTOM_APP_DOCROOT_DEFAULT;
import static org.openremote.container.util.MapAccess.getString;

public class ConfigurationService extends RouteBuilder implements ContainerService {

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

    @Override
    public int getPriority() {
        return ContainerService.DEFAULT_PRIORITY;
    }

    @Override
    public void configure() throws Exception {
        /* code not overridden yet */
    }

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

        Optional<Path> defaultMapSettingsPath = Stream.of("/opt/map/mapsettings.json", "manager/src/map/mapsettings.json")
                .map(Path::of)
                .map(Path::toAbsolutePath)
                .filter(Files::isRegularFile)
                .findFirst();

        Optional<Path> defaultMapTilesPath = Stream.of("/deployment/map/mapdata.mbtiles", "/opt/map/mapdata.mbtiles", "manager/src/map/mapdata.mbtiles")
                .map(Path::of)
                .map(Path::toAbsolutePath)
                .filter(Files::isRegularFile)
                .findFirst();

        Optional<Path> defaultManagerConfigPath = Stream.of(
                pathPublicRoot.resolve("manager").resolve("manager_config.json"),
                persistenceService.getStorageDir().resolve("manager").resolve("manager_config.json"))
                .map(Path::toAbsolutePath)
                .filter(Files::isRegularFile)
                .findFirst();

        if(defaultMapTilesPath.isEmpty() || defaultMapSettingsPath.isEmpty()){
            LOG.warning("Could not find map settings or map tiles");
            return;
        }

        mapTilesPath = Paths.get(getString(container.getConfig(), OR_MAP_TILES_PATH, defaultMapTilesPath.get().toAbsolutePath().toString()));
        mapTilesPath = Files.isRegularFile(mapTilesPath) ? mapTilesPath : defaultMapTilesPath.get();

        mapSettingsPath = Paths.get(getString(container.getConfig(), OR_MAP_SETTINGS_PATH, defaultMapSettingsPath.get().toAbsolutePath().toString()));
        Path persistencePath = persistenceService.getStorageDir().resolve("manager").resolve("mapsettings.json").toAbsolutePath();

        if(!Files.isRegularFile(mapSettingsPath)) {
            if(!Files.isRegularFile(persistencePath)){
                persistencePath.getParent().toFile().mkdirs();
                LOG.warning("Copying defaults to and using " + persistencePath + " for mapsettings.json");
                Files.copy(defaultMapSettingsPath.get(), persistencePath);
                mapSettingsPath = persistencePath;
            }
        }
        if(Files.isRegularFile(persistencePath)) {
            mapSettingsPath = persistencePath;
        }
        LOG.info("Configuration Service Used files:");
        LOG.info("\t- manager_config.json: " + getManagerConfigFile());
        LOG.info("\t- mapsettings.json: " + mapSettingsPath.toAbsolutePath());
        LOG.info("\t- mapdata.mbtiles: " + mapTilesPath.toAbsolutePath());
    }

    @Override
    public void start(Container container) throws Exception {
    }

    @Override
    public void stop(Container container) throws Exception {
        /* code not overridden yet */
    }

    protected Optional<File> getManagerConfigFile(){
        File file = getManagerConfigPath().toFile();

        if(file.exists() && !file.isDirectory()){
            return Optional.of(file);
        }else{
            file = pathPublicRoot
                    .resolve("manager_config.json")
                    .toFile();
            if (file.exists() && !file.isDirectory()){
                return Optional.of(file);
            }
        }
        return Optional.empty();
    }

    public ObjectNode getManagerConfig(){
        try {
            return (ObjectNode) ValueUtil.JSON.readTree(getManagerConfigFile().orElseThrow());
        } catch (Exception e) {
            LOG.severe("Could not read manager_config.json from "+ getManagerConfigFile());
            return null;
        }
    }

    protected Path getManagerConfigPath(){
        return persistenceService.getStorageDir()
                .resolve("manager")
                .resolve("manager_config.json");
    }

    @Override
    public String toString() {
        return "ConfigurationService{" +
                "mapTilesPath=" + mapTilesPath +
                ", mapSettingsPath=" + mapSettingsPath +
                ", managerConfigPath=" + getManagerConfigPath() +
                '}';
    }

    public ObjectNode getMapConfig(){
        try {
            return (ObjectNode) ValueUtil.JSON.readTree(mapSettingsPath.toFile());
        } catch (IOException e) {
            LOG.severe("failed to getMapConfig");
        }

        return null;
    }

    protected File getMapConfigFile(){
        return persistenceService.getStorageDir().resolve("manager").resolve("mapsettings.json").toFile();
    }

    public Path getManagerConfigImagePath(){
        return this.persistenceService.resolvePath("manager").resolve("images");
    }

    public void saveMapConfigFile(ObjectNode mapConfiguration) {
        try(OutputStream out = new FileOutputStream(getMapConfigFile())){
            out.write(ValueUtil.JSON.writeValueAsString(mapConfiguration).getBytes());
            this.mapSettingsPath = getMapConfigFile().toPath();
        } catch (Exception exception) {
            LOG.log(Level.WARNING, "Error trying to save mapsettings.json", exception);
        }
    }

    public Path getMapTilesPath(){
        return mapTilesPath.toAbsolutePath();
    }


    public void saveManagerConfigFile(ObjectNode managerConfiguration) throws Exception {
        LOG.log(Level.INFO, "Saving manager_config.json to "+getManagerConfigPath().toFile()+"...");

        // When saving the manager_config, automatically save it to the storageDir, as any other case would mean
        // that it's stored in OR_CUSTOM_APP_DOCROOT
        try (OutputStream out = new FileOutputStream(getManagerConfigPath().toFile())) {
            // Check references to images
            ObjectNode changedConfig = this.checkAndFixImageReferences(managerConfiguration);

            if(changedConfig != null){
                managerConfiguration = changedConfig.deepCopy();
            }
            // Write to file
            out.write(ValueUtil.JSON.writeValueAsString(managerConfiguration).getBytes());
        } catch (IOException | SecurityException exception) {
            LOG.log(Level.WARNING, "Error when trying to save manager_config.json", exception);
        }

    }

    public void saveConfigImageFile(String path, FileInfo fileInfo) throws Exception {
        LOG.log(Level.INFO, "Saving image in manager_config.json..");
        path = path.contains("/images/") ? path.replace("/images/", "") : path;

        Path resolvedPath = Path.of(path.charAt(0) == '/' ? path.substring(1) : path);
        resolvedPath = getManagerConfigImagePath().resolve(resolvedPath);
        boolean isValid = resolvedPath.toFile().getCanonicalPath().contains(getManagerConfigImagePath().toFile().getCanonicalPath() + File.separator);
        if(!isValid) throw new Exception("Reference to location outside the permitted directory");

        File file = getManagerConfigImagePath().
                resolve(path)
                .toAbsolutePath().toFile();
        try {
            file.getParentFile().mkdirs();
            if (file.exists()) {
                file.delete();
            }
        } catch (SecurityException se) {
            LOG.log(Level.WARNING, "Could not access folder for editing image in manager_config.json");
            throw se;
        }

        try (OutputStream out = new FileOutputStream(file.getAbsoluteFile())) {
            out.write(CodecUtil.decodeBase64(fileInfo.getContents()));
        } catch (IOException | SecurityException exception) {
            LOG.log(Level.WARNING, "Error when saving image in manager_config.json", exception);
            throw exception;
        }

    }

    public Optional<File> getManagerConfigImage(String filename) {
        File file = getManagerConfigImagePath().resolve(filename).toFile();

        if(!file.isFile()){
            // fallback to OR_CUSTOM_APP_DOCROOT
            file = pathPublicRoot.resolve("images").resolve(filename).toFile();
        }else{
            try {
                //Check if the file retrieved is somewhere within the storageDir/manager directory.
                //If it is, return an Optional.empty file to denote Not Found
                boolean isValid = file.getCanonicalPath().contains(getManagerConfigImagePath().toFile().getCanonicalPath() + File.separator);
                if (!isValid) return Optional.empty();
                } catch (IOException e) {
                    return Optional.empty();
                }
        }

        return file.isFile() ? Optional.of(file) : Optional.empty();
    }

    protected ObjectNode checkAndFixImageReferences(ObjectNode managerConfig) throws Exception{

        boolean configChanged = false;

        List<String> imageTypes = List.of("logo", "logoMobile", "favicon");

        for (Iterator<Map.Entry<String, JsonNode>> it = managerConfig.get("realms").fields(); it.hasNext(); ) {
            Map.Entry<String, JsonNode> kvp = it.next();
            String realmName = kvp.getKey();
            JsonNode realm = kvp.getValue();
            for (String type : imageTypes) {
                String image = realm.get(type) != null ? realm.get(type).asText() : "";
                if (!image.isBlank()) {
                    // Remove initial `/`
                    image = image.contains("/api/master/configuration/manager/image/")
                            ? image.replace("/api/master/configuration/manager/image/", "")
                            : image;
                    image = image.charAt(0) == '/' ? image.substring(1) : image;
                    Path imagePath = Path.of(image);
                    Path path = pathPublicRoot.resolve("images").resolve(imagePath).toAbsolutePath();

                    if (path.toFile().isFile()) {

                        // Remove the `/images/` part to avoid having an images/images folder in storageDir
                        String strippedImage = image.replace("images/", "");

                        File persistenceImageFile = getManagerConfigImagePath().resolve(Path.of(strippedImage)).toAbsolutePath().toFile();

                        File deploymentImageFile = path.toAbsolutePath().toFile();

                        // If this file is in the deployment folder AND the storageDir doesn't contain that,
                        // copy the file to the persistenceImageFile.
                        if (deploymentImageFile.isFile() && !persistenceImageFile.isFile()) {
                            persistenceImageFile.getParentFile().mkdirs();
                            Files.copy(deploymentImageFile.toPath(), persistenceImageFile.toPath());
                            // Change the reference in the config to the typical API-like reference:
                            ((ObjectNode) managerConfig.get("realms").get(realmName)).put(type, "/api/master/configuration/manager/image/"+strippedImage);
                            configChanged = true;
                        }
                        // If there is no file in either locations, throw since we can't resolve the reference to the image
                        // Will be caught at the resource implementation
                        else if (!deploymentImageFile.isFile() && !persistenceImageFile.isFile()) {
                            throw new NotFoundException("File not found in both persistence directory and storage directory");
                        }
                    }
                }
            }
        }
        return configChanged ? managerConfig : null;
    }
}
