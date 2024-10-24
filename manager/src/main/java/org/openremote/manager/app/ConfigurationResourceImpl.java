/*
 * Copyright 2022, OpenRemote Inc.
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
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package org.openremote.manager.app;

import java.util.logging.Logger;

import org.openremote.container.timer.TimerService;
import org.openremote.container.web.WebService;
import org.openremote.manager.security.ManagerIdentityService;
import org.openremote.manager.web.ManagerWebResource;
import org.openremote.model.file.FileInfo;
import org.openremote.model.http.RequestParams;
import org.openremote.model.manager.ConfigurationResource;

public class ConfigurationResourceImpl extends ManagerWebResource implements ConfigurationResource {

    protected ConfigurationService configurationService;
    private static final Logger LOG = Logger.getLogger(WebService.class.getName());

    public ConfigurationResourceImpl(TimerService timerService, ManagerIdentityService identityService, ConfigurationService configurationService) {
        super(timerService, identityService);
        this.configurationService = configurationService;
    }


    @Override
    public Object update(RequestParams requestParams, Object managerConfiguration) {
        this.configurationService.saveMangerConfig(managerConfiguration);
        return managerConfiguration;
    }

    @Override
    public String fileUpload(RequestParams requestParams, String path, FileInfo fileInfo) {
        this.configurationService.saveImageFile(path, fileInfo);
        return path;
    }
}
