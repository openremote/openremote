/*
 * Copyright 2020, OpenRemote Inc.
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
package org.openremote.model.gateway;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Entity
@Table(name = "GATEWAY_CONNECTION")
public class GatewayConnection {

    @Id
    @Column(name = "LOCAL_REALM", nullable = false)
    protected String localRealm;

    @NotNull(message = "{GatewayConnection.host.NotNull}")
    @Size(min = 1, max = 255, message = "{GatewayConnection.host.Size}")
    @Column(name = "HOST", nullable = false)
    protected String host;

    @Min(value = 1, message = "{GatewayConnection.port.Range}")
    @Max(value = 65536, message = "{GatewayConnection.port.Range}")
    @Column(name = "PORT")
    protected Integer port;

    @Column(name = "REALM", nullable = false)
    protected String realm;

    @NotNull(message = "{GatewayConnection.clientId.NotNull}")
    @Size(min = 1, max = 255, message = "{GatewayConnection.clientId.Size}")
    @Column(name = "CLIENT_ID", nullable = false, length = 36)
    protected String clientId;

    @NotNull(message = "{GatewayConnection.clientSecret.NotNull}")
    @Size(min = 36, max = 36, message = "{GatewayConnection.clientSecret.Size}")
    @Column(name = "CLIENT_SECRET", nullable = false, length = 36)
    protected String clientSecret;

    @Column(name = "SECURED")
    protected Boolean secured;

    @Column(name = "DISABLED")
    protected boolean disabled;

    /**
     * For JPA
     */
    GatewayConnection() {
    }

    @JsonCreator
    public GatewayConnection(
        @JsonProperty("localRealm") String localRealm,
        @JsonProperty("host") String host,
        @JsonProperty("port") Integer port,
        @JsonProperty("realm") String realm,
        @JsonProperty("clientId") String clientId,
        @JsonProperty("clientSecret") String clientSecret,
        @JsonProperty("secured") Boolean secured,
        @JsonProperty("disabled") boolean disabled) {
        this.localRealm = localRealm;
        this.host = host;
        this.port = port;
        this.realm = realm;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.secured = secured;
        this.disabled = disabled;
    }

    public GatewayConnection(
        String host,
        Integer port,
        String realm,
        String clientId,
        String clientSecret,
        Boolean secured,
        boolean disabled) {
        this.host = host;
        this.port = port;
        this.realm = realm;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.secured = secured;
        this.disabled = disabled;
    }

    public String getLocalRealm() {
        return localRealm;
    }

    public void setLocalRealm(String localRealm) {
        this.localRealm = localRealm;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public String getRealm() {
        return realm;
    }

    public void setRealm(String realm) {
        this.realm = realm;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

    public boolean isSecured() {
        return secured == null ? true : secured;
    }

    public void setSecured(boolean secured) {
        this.secured = secured;
    }

    public boolean isDisabled() {
        return disabled;
    }

    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
            "localRealm='" + localRealm + '\'' +
            ", host='" + host + '\'' +
            ", port=" + port +
            ", realm='" + realm + '\'' +
            ", clientId='" + clientId + '\'' +
            ", clientSecret='" + clientSecret + '\'' +
            ", secured=" + secured +
            ", disabled=" + disabled +
            '}';
    }
}
