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
package org.openremote.manager.system;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.openremote.container.security.IdentityService;
import org.openremote.manager.security.ManagerIdentityService;
import org.openremote.model.Container;
import org.openremote.model.system.HealthStatusProvider;
import org.openremote.model.system.StatusResource;
import org.openremote.model.util.ValueUtil;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

public class StatusResourceImpl implements StatusResource {

    private static final Logger LOG = Logger.getLogger(StatusResourceImpl.class.getName());
    protected List<HealthStatusProvider> healthStatusProviderList;
    protected ObjectNode serverInfo;

    public StatusResourceImpl(Container container, List<HealthStatusProvider> healthStatusProviderList) {
        this.healthStatusProviderList = healthStatusProviderList;
        Properties versionProps = new Properties();
        String authServerUrl = null;

        ManagerIdentityService identityService = container.getService(ManagerIdentityService.class);
        if (identityService != null) {
            authServerUrl = identityService.getIdentityProvider().getFrontendUrl();
        }

        try(InputStream resourceStream = StatusResourceImpl.class.getClassLoader().getResourceAsStream("system.properties")) {
            versionProps.load(resourceStream);
        } catch (IOException e) {
            LOG.log(Level.SEVERE, "Failed to load manager version properties file: system.properties");
            throw new IllegalStateException("Missing manager system.properties file");
        }

        String version = versionProps.getProperty("version");
        serverInfo = ValueUtil.JSON.createObjectNode();
        serverInfo.put("version", version);
        serverInfo.put("authServerUrl", authServerUrl);
    }

    @Override
    public ObjectNode getHealthStatus() {
        ObjectNode objectValue = ValueUtil.JSON.createObjectNode();

        healthStatusProviderList.forEach(healthStatusProvider -> {
                ObjectNode providerValue = ValueUtil.JSON.createObjectNode();
                providerValue.put("version", healthStatusProvider.getHealthStatusVersion());
                providerValue.putPOJO("data", healthStatusProvider.getHealthStatus());
                objectValue.set(healthStatusProvider.getHealthStatusName(), providerValue);
            }
        );

        return objectValue;
    }

    @Override
    public ObjectNode getInfo() {
        return serverInfo;
    }
}
