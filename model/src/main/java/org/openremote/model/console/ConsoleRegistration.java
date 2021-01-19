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

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.Map;

/**
 * Encapsulates data about a console instance that is used to generate an {@link org.openremote.model.asset.Asset} of
 * type {@link org.openremote.model.asset.AssetType#CONSOLE}. The console is considered the app used to view the web
 * application (client), for example:
 * <p>
 * Google Chrome on Windows 10 64-bit used to view the manager and customerA applications would generate the following
 * registration:
 * <blockquote><pre>{@code
 * {
 *    "name": "Chrome",
 *    "version": "71.0.3578.98",
 *    "platform": "Windows 10 64-bit",
 *    "model": null,
 *    "providers": {
 *        "push": {
 *            "data": {
 *               "token": "ASasm,sASKasjASasjASJas"
 *            },
 *            "version": "web",
 *            "disabled": false,
 *            "hasPermission": true,
 *            "requiresPermission": true
 *         }
 *    },
 *    "apps": [
 *        "manager"
 *        "customerA"
 *    ]
 * }</pre></blockquote>
 * <p>
 * An Android console app called "Smart City" on a Samsung Galaxy S9 running Android 8.0 would generate the following
 * registration:
 * <blockquote><pre>{@code
 * {
 *    "name": "Smart City",
 *    "version": "1.0.0",
 *    "platform": "Android 8.0",
 *    "model": "Samsung Galaxy S9",
 *    "providers": {
 *        "push": {
 *            "data": {
 *               "token": "ASasm,sASKasjASasjASJas"
 *            },
 *            "version": "fcm",
 *            "disabled": false,
 *            "hasPermission": true,
 *            "requiresPermission": true
 *         },
 *        "geofence": {
 *           "version": "ORConsole",
 *           "disabled": false,
 *           "hasPermission": true,
 *           "requiresPermission": true
 *        }
 *    },
 *    "apps": [
 *        "smartcity"
 *    ]
 * }</pre></blockquote>
 */
public class ConsoleRegistration {
    protected String id;
    @NotBlank
    protected String name;
    @NotBlank
    protected String version;
    @NotBlank
    protected String platform;
    protected String model;
    @NotNull
    protected Map<String, ConsoleProvider> providers;
    protected String[] apps;

    @JsonCreator
    public ConsoleRegistration(@JsonProperty("id") String id,
                               @JsonProperty("name") String name,
                               @JsonProperty("version") String version,
                               @JsonProperty("platform") String platform,
                               @JsonProperty("providers") Map<String, ConsoleProvider> providers,
                               @JsonProperty("model") String model,
                               @JsonProperty("apps") String[] apps) {
        this.id = id;
        this.name = name;
        this.version = version;
        this.platform = platform;
        this.providers = providers;
        this.model = model;
        this.apps = apps;
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

    public String getModel() {
        return model;
    }

    public Map<String, ConsoleProvider> getProviders() {
        return providers;
    }

    public String[] getApps() {
        return apps;
    }
}
