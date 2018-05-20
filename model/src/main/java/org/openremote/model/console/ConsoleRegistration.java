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
package org.openremote.model.console;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

public class ConsoleRegistration {
    protected String id;
    protected String name;
    protected String version;
    protected String platform;
    protected Map<String, ConsoleProvider> providers;

    @JsonCreator
    public ConsoleRegistration(@JsonProperty("id") String id,
                               @JsonProperty("name") String name,
                               @JsonProperty("version") String version,
                               @JsonProperty("platform") String platform,
                               @JsonProperty("providers") Map<String, ConsoleProvider> providers) {
        this.id = id;
        this.name = name;
        this.version = version;
        this.platform = platform;
        this.providers = providers;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public String getVersion() {
        return version;
    }

    public String getPlatform() {
        return platform;
    }

    public Map<String, ConsoleProvider> getProviders() {
        return providers;
    }
}
