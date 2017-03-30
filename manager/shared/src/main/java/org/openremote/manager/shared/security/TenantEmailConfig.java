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
package org.openremote.manager.shared.security;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.HashMap;
import java.util.Map;

@JsonIgnoreProperties({"password", "auth", "port", "host", "from", "user", "ssl", "starttls"})
public class TenantEmailConfig {
    @JsonProperty
    protected Map<String,String> settings = new HashMap<>();

    public Map<String,String> asMap() {
        return settings;
    }

    public void setPassword(String password) {
        settings.put("password", password);
    }

    public String getPassword() {
        return settings.get("password");
    }

    public Boolean isAuth() {
        return settings.containsKey("auth") ? Boolean.parseBoolean(settings.get("auth")) : null;
    }

    public void setAuth(boolean auth) {
        settings.put("auth", Boolean.toString(auth));
    }

    public Integer getPort() {
        return settings.containsKey("port") ? Integer.parseInt(settings.get("port")) : null;
    }

    public void setPort(int port) {
        settings.put("port", Integer.toString(port));
    }

    public String getHost() {
        return settings.get("host");
    }

    public void setHost(String host) {
        settings.put("host", host);
    }

    public String getFrom() {
        return settings.get("from");
    }

    public void setFrom(String from) {
        settings.put("from", from);
    }

    public String getUser() {
        return settings.get("user");
    }

    public void setUser(String user) {
        settings.put("user", user);
    }

    public Boolean isSsl() {
        return settings.containsKey("ssl") ? Boolean.parseBoolean(settings.get("ssl")) : null;
    }

    public void setSsl(boolean ssl) {
        settings.put("ssl", Boolean.toString(ssl));
    }

    public Boolean isStarttls() {
        return settings.containsKey("starttls") ? Boolean.parseBoolean(settings.get("starttls")) : null;
    }

    public void setStarttls(boolean starttls) {
        settings.put("starttls", Boolean.toString(starttls));
    }
}
