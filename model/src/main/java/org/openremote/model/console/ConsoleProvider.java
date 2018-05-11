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
import org.openremote.model.value.ObjectValue;
import org.openremote.model.value.Value;
import org.openremote.model.value.Values;

import java.util.Map;
import java.util.Optional;

public class ConsoleProvider {
    protected String version;
    protected boolean requiresPermission;
    protected boolean hasPermission;
    protected boolean disabled;
    protected ObjectValue data;

    @JsonCreator
    public ConsoleProvider(@JsonProperty("version") String version,
                           @JsonProperty("requiresPermission") boolean requiresPermission,
                           @JsonProperty("hasPermission") boolean hasPermission,
                           @JsonProperty("disabled") boolean disabled,
                           @JsonProperty("data") ObjectValue data) {
        this.version = version;
        this.requiresPermission = requiresPermission;
        this.hasPermission = hasPermission;
        this.disabled = disabled;
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

    public boolean isDisabled() {
        return disabled;
    }

    public ObjectValue getData() {
        return data;
    }

    public static Optional<ConsoleProvider> fromValue(Value value) {
        return Values.getObject(value).flatMap(obj -> {
            Optional<String> version = obj.getString("version");
            Optional<Boolean> requiresPermission = obj.getBoolean("requiresPermission");
            if (!version.isPresent() || !requiresPermission.isPresent()) {
                return Optional.empty();
            }

            boolean hasPermission = obj.getBoolean("hasPermission").orElse(!requiresPermission.get());
            boolean disabled = obj.getBoolean("disabled").orElse(false);
            ObjectValue data = obj.getObject("data").orElse(null);

            return Optional.of(new ConsoleProvider(version.get(), requiresPermission.get(), hasPermission, disabled, data));
        });
    }

    public Value toValue() {
        ObjectValue obj = Values.createObject();
        obj.put("version", getVersion());
        obj.put("requiresPermission", isRequiresPermission());
        obj.put("hasPermission", isHasPermission());
        if (data != null) {
            obj.put("data", getData());
        }

        return obj;
    }

    public static Value toValue(ConsoleProvider provider) {
        return provider.toValue();
    }

    public static Value toValue(Map<String, ConsoleProvider> providerMap) {
        if (providerMap == null || providerMap.isEmpty()) {
            return null;
        }

        ObjectValue obj = Values.createObject();
        providerMap.forEach((providerName, provider) ->
            obj.put(providerName, provider.toValue())
        );

        return obj;
    }
}
