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
import org.apache.camel.builder.RouteBuilder;
import org.openremote.container.persistence.PersistenceService;
import org.openremote.container.timer.TimerService;
import org.openremote.container.util.CodecUtil;
import org.openremote.container.web.WebService;
import org.openremote.manager.security.ManagerIdentityService;
import org.openremote.manager.web.ManagerWebService;
import org.openremote.model.Container;
import org.openremote.model.ContainerService;
import org.openremote.model.file.FileInfo;
import org.openremote.model.manager.MapRealmConfig;
import org.openremote.model.util.ValueUtil;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.openremote.manager.web.ManagerWebService.OR_CUSTOM_APP_DOCROOT;
import static org.openremote.manager.web.ManagerWebService.OR_CUSTOM_APP_DOCROOT_DEFAULT;
import static org.openremote.container.util.MapAccess.getString;

public class ConfigurationService extends RouteBuilder implements ContainerService {

    public static final String OR_MAP_SETTINGS_PATH = "OR_MAP_SETTINGS_PATH";
    public static final String OR_MAP_SETTINGS_PATH_DEFAULT = "manager/src/map/mapsettings.json";

    protected ManagerIdentityService identityService;
    protected PersistenceService persistenceService;
    protected Path pathPublicRoot;

    private static final Logger LOG = Logger.getLogger(WebService.class.getName());
    protected Path mapSettingsPath;

    protected ObjectNode mapConfig;
    protected ObjectNode managerConfig;

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

        mapSettingsPath = Paths.get(getString(container.getConfig(), OR_MAP_SETTINGS_PATH, OR_MAP_SETTINGS_PATH_DEFAULT));
        if (!Files.isRegularFile(mapSettingsPath)) {
            LOG.warning("Map settings file not found '" + mapSettingsPath.toAbsolutePath() + "', falling back to built in map settings");
            mapSettingsPath = persistenceService.getStorageDir().resolve("mapsettings.json");
            if(!Files.isRegularFile(mapSettingsPath)){
                mapSettingsPath = Paths.get(OR_MAP_SETTINGS_PATH_DEFAULT).resolve("mapsettings.json");
                if(!Files.isRegularFile(mapSettingsPath)){
                    LOG.severe("Map settings file not found, map functionality will not work");
                }
            }
        }

        // Will throw if failed, stopping startup fast and hard
        try {
            loadMapSettingsJson();
        }catch (Exception e){
            LOG.severe("Could not load MapSettings.json file. Map functionality will be limited. Error: "+ e.getMessage());
        }

        loadManagerConfigJson();
    }

    @Override
    public void start(Container container) throws Exception {

        // Check if the configuration contains references to files that are located in the deployment, and if they are,
        // move them to the storageDir.
        ObjectNode changedConfig = this.checkAndFixImageReferences(this.managerConfig);

        if(changedConfig != null){
            saveManagerConfigFile(changedConfig);
        }
    }

    @Override
    public void stop(Container container) throws Exception {
        /* code not overridden yet */
    }

    protected void loadMapSettingsJson() throws Exception {
        try {
            this.mapConfig = (ObjectNode) ValueUtil.JSON.readTree(Files.readAllBytes(mapSettingsPath));
        } catch (Exception ex) {
            LOG.log(Level.SEVERE, "Failed to extract map config from: " + mapSettingsPath.toAbsolutePath(), ex);
            throw ex;
        }
    }

    public Optional<File> getManagerConfigFile(){
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
        return this.managerConfig;
    }

    public Path getManagerConfigPath(){
        return persistenceService.getStorageDir()
                .resolve("manager")
                .resolve("manager_config.json");
    }


    protected void loadManagerConfigJson() {
        try {
            this.managerConfig = (ObjectNode) ValueUtil.JSON.readTree(getManagerConfigFile().orElseThrow());
        } catch (Exception ex) {
            LOG.log(Level.INFO, "Could not load manager_config.json file. Appearance will be default. " +
                    "Failed to extract manager config from: " + mapSettingsPath.toAbsolutePath(), ex);
        }
    }



    public ObjectNode getMapConfig(){
        return this.mapConfig;
    }

    protected File getMapConfigFile(){
        return persistenceService.getStorageDir().resolve("manager").resolve("mapsettings.json").toFile();
    }

    public Path getManagerConfigImagePath(){
        return this.persistenceService.resolvePath("manager").resolve("images");
    }

    public void saveMapConfigFile(Map<String, MapRealmConfig> mapConfiguration) {
        try(OutputStream out = new FileOutputStream(getMapConfigFile())){
            mapConfig.putPOJO("options", mapConfiguration);
            out.write(ValueUtil.JSON.writeValueAsString(mapConfiguration).getBytes());
            this.loadMapSettingsJson();
        } catch (Exception exception) {
            LOG.log(Level.WARNING, "Error trying to save mapsettings.json", exception);
        }
    }


    public void saveManagerConfigFile(ObjectNode managerConfiguration) throws Exception {
        LOG.log(Level.INFO, "Saving manager_config.json...");

        // When saving the manager_config, automatically save it to the storageDir, as any other case would mean
        // that it's stored in OR_CUSTOM_APP_DOCROOT
        try (OutputStream out = new FileOutputStream(getManagerConfigPath().toFile())) {
            // Check references to images
            ObjectNode changedConfig = this.checkAndFixImageReferences(this.managerConfig);

            if(changedConfig != null){
                managerConfiguration = changedConfig.deepCopy();
            }
            // Write to file
            out.write(ValueUtil.JSON.writeValueAsString(managerConfiguration).getBytes());

            // Reload manager_config.json from file
            this.loadManagerConfigJson();
        } catch (IOException | SecurityException exception) {
            LOG.log(Level.WARNING, "Error when trying to save manager_config.json", exception);
        }

    }

    public void saveConfigImageFile(String path, FileInfo fileInfo) throws Exception {
        LOG.log(Level.INFO, "Saving image in manager_config.json..");
        File file = getManagerConfigImagePath().
                resolve(path.charAt(0) == '/' ? path.substring(1) : path)
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
                Boolean isValid = file.getCanonicalPath().contains(getManagerConfigImagePath().toFile().getCanonicalPath() + File.separator);
                if (!isValid) return Optional.empty();
                } catch (IOException e) {
                    return Optional.empty();
                }
        }

        return file.isFile() ? Optional.of(file) : Optional.empty();
    }

    public ObjectNode checkAndFixImageReferences(ObjectNode managerConfig) throws Exception{

        Boolean configChanged = false;

        List<String> imageTypes = List.of("logo", "logoMobile", "favicon");

        for (Iterator<Map.Entry<String, JsonNode>> it = managerConfig.get("realms").fields(); it.hasNext(); ) {
            Map.Entry<String, JsonNode> kvp = it.next();
            String realmName = kvp.getKey();
            JsonNode realm = kvp.getValue();
            for (String type : imageTypes) {
                String image = realm.get(type) != null ? realm.get(type).asText() : "";
                if (!image.isBlank()) {
                    // Remove initial `/`
                    image = image.charAt(0) == '/' ? image.substring(1) : image;
                    Path imagePath = Path.of(image);
                    Path path = pathPublicRoot.resolve(imagePath).toAbsolutePath();

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
                        }

                        // Change the reference in the config to the typical API-like reference:

                        ((ObjectNode) managerConfig.get("realms").get(realmName)).put(type, strippedImage);
                        configChanged = true;
                    }
                }
            }
        }
        return configChanged ? managerConfig : null;
    }
}
