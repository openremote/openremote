/*
 * Copyright 2017, OpenRemote Inc.
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
package org.openremote.manager.configuration;

import org.openremote.container.timer.TimerService;
import org.openremote.manager.security.ManagerIdentityService;
import org.openremote.manager.web.ManagerWebResource;
import org.openremote.model.configuration.ConfigurationResource;
import org.openremote.model.configuration.ManagerConf;
import org.openremote.model.file.FileInfo;
import org.openremote.model.http.RequestParams;

import java.io.*;

public class ConfigurationResourceImpl extends ManagerWebResource implements ConfigurationResource {

    protected ConfigurationService configurationService;

    public ConfigurationResourceImpl(TimerService timerService, ManagerIdentityService identityService, ConfigurationService configurationService) {
        super(timerService, identityService);
        this.configurationService = configurationService;
    }


    @Override
    public ManagerConf update(RequestParams requestParams, ManagerConf managerConfiguration) throws IOException {
        this.configurationService.saveMangerConfig(managerConfiguration);
        return managerConfiguration;
    }

    @Override
    public String fileUpload(RequestParams requestParams, String path, FileInfo fileInfo) throws IOException {
        this.configurationService.saveImageFile(path, fileInfo);
        return path;
    }
}
