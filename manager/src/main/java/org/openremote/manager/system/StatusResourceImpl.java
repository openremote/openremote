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

import org.openremote.manager.security.ManagerIdentityService;
import org.openremote.model.Container;
import org.openremote.model.system.HealthStatusProvider;
import org.openremote.model.system.StatusResource;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

public class StatusResourceImpl implements StatusResource {

    private static final Logger LOG = Logger.getLogger(StatusResourceImpl.class.getName());
    protected List<HealthStatusProvider> healthStatusProviderList;
    protected Map<String, Object> serverInfo;

    public StatusResourceImpl(Container container, List<HealthStatusProvider> healthStatusProviderList) {
        this.healthStatusProviderList = healthStatusProviderList;
        Properties versionProps = new Properties();
        String authServerUrl = "";

        ManagerIdentityService identityService = container.getService(ManagerIdentityService.class);
        if (identityService != null) {
            authServerUrl = identityService.getIdentityProvider().getFrontendUrl();
        }

        try(InputStream resourceStream = StatusResourceImpl.class.getClassLoader().getResourceAsStream("version.properties")) {
            versionProps.load(resourceStream);
        } catch (IOException e) {
            LOG.log(Level.SEVERE, "Failed to load manager version properties file: version.properties");
            throw new IllegalStateException("Missing manager version.properties file");
        }

        String version = versionProps.getProperty("version");
        serverInfo = Map.of(
            "version", version,
            "authServerUrl", authServerUrl
        );
    }

    @Override
    public Map<String, Object> getHealthStatus() {
        Map<String, Object> objectValue = new HashMap<>();

        healthStatusProviderList.forEach(healthStatusProvider -> {
            Map<String, Object> providerValue = Map.of("data", healthStatusProvider.getHealthStatus());
                objectValue.put(healthStatusProvider.getHealthStatusName(), providerValue);
            }
        );

        return objectValue;
    }

    @Override
    public Map<String, Object> getInfo() {
        return serverInfo;
    }
}
