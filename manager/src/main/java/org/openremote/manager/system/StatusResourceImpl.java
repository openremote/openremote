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

import com.fasterxml.jackson.databind.node.NullNode;
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
        String version = null;

        ManagerIdentityService identityService = container.getService(ManagerIdentityService.class);
        if (identityService != null && identityService.getIdentityProvider().getFrontendURI() != null) {
            authServerUrl = identityService.getIdentityProvider().getFrontendURI();
        }

        try (InputStream resourceStream = StatusResourceImpl.class.getClassLoader().getResourceAsStream("version.properties")) {
            if (resourceStream != null) {
                versionProps.load(resourceStream);
                version = versionProps.getProperty("version");
            }
        } catch (IOException ignored) {
        }

        if (version == null) {
            LOG.log(Level.WARNING, "Failed to load manager version properties file: version.properties");
            version = "0.0.0";
        }

        serverInfo = Map.of(
            "version", version,
            "authServerUrl", authServerUrl
        );

        LOG.info("Starting OpenRemote version: v"+version);
    }

    @Override
    public Map<String, Object> getHealthStatus() {
        Map<String, Object> objectValue = new HashMap<>();

        healthStatusProviderList.forEach(healthStatusProvider -> {
            Object healthStatus = healthStatusProvider.getHealthStatus();
            Map<String, Object> providerValue = Map.of("data", healthStatus != null ? healthStatus : NullNode.getInstance());
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
