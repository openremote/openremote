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
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.Serializable;

public class ConsoleProvider implements Serializable {
    protected String version;
    protected boolean requiresPermission;
    protected boolean hasPermission;
    protected boolean success;
    protected boolean enabled;
    protected boolean disabled;
    protected ObjectNode data;

    @JsonCreator
    public ConsoleProvider(@JsonProperty("version") String version,
                           @JsonProperty("requiresPermission") boolean requiresPermission,
                           @JsonProperty("hasPermission") boolean hasPermission,
                           @JsonProperty("success") boolean success,
                           @JsonProperty("enabled") boolean enabled,
                           @JsonProperty("disabled") boolean disabled,
                           @JsonProperty("data") ObjectNode data) {
        this.version = version;
        this.requiresPermission = requiresPermission;
        this.hasPermission = hasPermission;
        this.success = success;
        this.disabled = disabled;
        this.enabled = enabled;
        this.data = data;
    }

    public String getVersion() {
        return version;
    }

    public boolean isRequiresPermission() {
        return requiresPermission;
    }

    public boolean isHasPermission() {
        return hasPermission;
    }

    public boolean isSuccess() {
        return success;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isDisabled() {
        return disabled;
    }

    public ObjectNode getData() {
        return data;
    }
}
