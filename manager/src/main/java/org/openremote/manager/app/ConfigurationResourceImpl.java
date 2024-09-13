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

import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;
import jakarta.servlet.http.HttpServletResponse;
import net.minidev.json.JSONObject;
import org.apache.activemq.artemis.commons.shaded.json.Json;
import org.openremote.container.timer.TimerService;
import org.openremote.container.web.WebService;
import org.openremote.manager.security.ManagerIdentityService;
import org.openremote.manager.web.ManagerWebResource;
import org.openremote.model.manager.ConfigurationResource;
import org.openremote.model.file.FileInfo;
import org.openremote.model.http.RequestParams;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
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
        return path;
    }

    @Override
    public Object getManagerConfig() {
        Optional<File> file = configurationService.getManagerConfig();
        if (file.isEmpty()) {
            throw new IllegalStateException();
        }
        try (JsonReader reader = new JsonReader(new FileReader(file.get()))) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        } catch (IOException e) {
            LOG.severe("Error reading manager config file: " + e.getMessage());
        }
        return null;
    }
}
