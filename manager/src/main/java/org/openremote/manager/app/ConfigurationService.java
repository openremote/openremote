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
import java.util.Map;
import java.util.Optional;
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

        if(Files.isRegularFile(mapSettingsPath)){
            mapConfig = loadMapSettingsJson();
        }
    }

    @Override
    public void start(Container container) throws Exception {
        /* code not overridden yet */
    }

    @Override
    public void stop(Container container) throws Exception {
        /* code not overridden yet */
    }


    public void saveManagerConfigFile(Object managerConfiguration) {
        LOG.log(Level.INFO, "Saving manager_config.json..");
        try (OutputStream out = new FileOutputStream(persistenceService.getStorageDir().resolve("manager_config.json").toFile())) {
            out.write(ValueUtil.JSON.writeValueAsString(managerConfiguration).getBytes());
        } catch (IOException | SecurityException exception) {
            LOG.log(Level.WARNING, "Error when trying to save manager_config.json", exception);
        }

    }


    public void saveImageFile(String path, FileInfo fileInfo) {
        LOG.log(Level.INFO, "Saving image in manager_config.json..");
        File file = new File(pathPublicRoot + path);
        try {
            file.getParentFile().mkdirs();
            if (file.exists()) {
                file.delete();
            }
        } catch (SecurityException se) {
            LOG.log(Level.WARNING, "Could not access folder for editing image in manager_config.json");
            return;
        }

        try (OutputStream out = new FileOutputStream(file)) {
            out.write(CodecUtil.decodeBase64(fileInfo.getContents()));
        } catch (IOException | SecurityException exception) {
            LOG.log(Level.WARNING, "Error when saving image in manager_config.json", exception);
        }

    }

    public Optional<File> getManagerConfig(){
        File file = persistenceService.getStorageDir()
                .resolve("manager")
                .resolve("manager_config.json")
                .toFile();

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

    protected ObjectNode loadMapSettingsJson() {
        try {
            return (ObjectNode) ValueUtil.JSON.readTree(Files.readAllBytes(mapSettingsPath));
        } catch (Exception ex) {
            LOG.log(Level.SEVERE, "Failed to extract map config from: " + mapSettingsPath.toAbsolutePath(), ex);
            return ValueUtil.JSON.createObjectNode();
        }
    }

    public ObjectNode getMapConfig() {
        return mapConfig;
    }

    protected File getMapSettingsFile(){
        return persistenceService.getStorageDir().resolve("mapsettings.json").toFile();
    }

    public void storeMapConfig(Map<String, MapRealmConfig> mapConfiguration) {
        try(OutputStream out = new FileOutputStream(getMapSettingsFile())){
            mapConfig.putPOJO("options", mapConfiguration);
            out.write(ValueUtil.JSON.writeValueAsString(mapConfiguration).getBytes());
        } catch (IOException | NullPointerException exception) {
            LOG.log(Level.WARNING, "Error trying to save mapsettings.json", exception);
        }
    }
}
