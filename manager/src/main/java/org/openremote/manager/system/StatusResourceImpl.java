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

import org.openremote.model.system.HealthStatusProvider;
import org.openremote.model.system.StatusResource;
import org.openremote.model.value.ObjectValue;
import org.openremote.model.value.Values;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

public class StatusResourceImpl implements StatusResource {

    private static final Logger LOG = Logger.getLogger(StatusResourceImpl.class.getName());
    protected List<HealthStatusProvider> healthStatusProviderList;
    protected Properties versionProps = new Properties();

    public StatusResourceImpl(List<HealthStatusProvider> healthStatusProviderList) {
        this.healthStatusProviderList = healthStatusProviderList;

        try(InputStream resourceStream = StatusResourceImpl.class.getClassLoader().getResourceAsStream("system.properties")) {
            versionProps.load(resourceStream);
        } catch (IOException e) {
            LOG.log(Level.SEVERE, "Failed to load manager version properties file: system.properties");
            throw new IllegalStateException("Missing manager system.properties file");
        }
    }

    @Override
    public ObjectValue getHealthStatus() {
        ObjectValue objectValue = Values.createObject();

        healthStatusProviderList.forEach(healthStatusProvider -> {
                ObjectValue providerValue = Values.createObject();
                providerValue.put("version", healthStatusProvider.getHealthStatusVersion());
                providerValue.put("data", healthStatusProvider.getHealthStatus());
                objectValue.put(healthStatusProvider.getHealthStatusName(), providerValue);
            }
        );

        return objectValue;
    }

    @Override
    public ObjectValue getInfo() {
        String version = versionProps.getProperty("version");
        ObjectValue objectValue = Values.createObject();
        objectValue.put("version", version);
        return objectValue;
    }
}
