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
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;
import org.openremote.container.timer.TimerService;
import org.openremote.container.web.WebService;
import org.openremote.manager.security.ManagerIdentityService;
import org.openremote.manager.web.ManagerWebResource;
import org.openremote.model.manager.ConfigurationResource;
import org.openremote.model.file.FileInfo;
import org.openremote.model.http.RequestParams;

import java.io.*;
import java.net.URI;
import java.nio.file.Files;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.logging.Logger;

public class ConfigurationResourceImpl extends ManagerWebResource implements ConfigurationResource {

    protected ConfigurationService configurationService;
    private static final Logger LOG = Logger.getLogger(WebService.class.getName());

    public ConfigurationResourceImpl(TimerService timerService, ManagerIdentityService identityService, ConfigurationService configurationService) {
        super(timerService, identityService);
        this.configurationService = configurationService;
    }


    @Override
    public Object update(RequestParams requestParams, Object managerConfiguration) {
        this.configurationService.saveManagerConfigFile(managerConfiguration);
        return managerConfiguration;
    }

    @Override
    public String fileUpload(RequestParams requestParams, String path, FileInfo fileInfo) {
        this.configurationService.saveImageFile(path, fileInfo);
        requestParams.getExternalSchemeHostAndPort();
        URI managerConfigPath = requestParams.getExternalBaseUriBuilder()
                .path("master")
                .path("configuration")
                .path("manager")
                .path("image")
                .path(path)
                .build();
        return managerConfigPath.toString();
    }

    @Override
    public ObjectNode getManagerConfig() {
        Optional<File> file = configurationService.getManagerConfig();
        if (file.isEmpty()) {
            throw new NotFoundException("Configuration file not found");
        }

        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode jsonNode = mapper.readTree(file.get());
            if (jsonNode.isObject()) {
                ObjectNode objectNode = (ObjectNode) jsonNode;
                return objectNode;
            } else {
                throw new IOException("The provided JSON is not an object.");
            }
        } catch (IOException e) {
            LOG.severe("Error reading manager config file: " + e.getMessage());
        }
        return null;
    }

    @Override
    public Object getManagerConfigImages(String fileName) {

        try {
            File imageFile = configurationService.getManagerConfigImage(fileName).orElseThrow();
            if (!imageFile.exists()) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }

            String mimeType = Files.probeContentType(imageFile.toPath());
            if (mimeType == null) {
                mimeType = "application/octet-stream";
            }

            Response.ResponseBuilder response = Response.ok(imageFile, mimeType);
            response.header("Access-Control-Allow-Origin", "*");
            response.header("Access-Control-Allow-Methods", "GET, POST, DELETE, PUT");
            response.header("Access-Control-Allow-Headers", "Content-Type, api_key, Authorization");
            return response.build();
        }
        catch (NoSuchElementException e){
            return Response.status(Response.Status.NOT_FOUND).entity("Image not Found").build();
        }
        catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error retrieving image").build();
        }
    }
}
