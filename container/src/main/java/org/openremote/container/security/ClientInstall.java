/*
 * Copyright 2016, OpenRemote Inc.
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
package org.openremote.container.security;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.security.PublicKey;

public class ClientInstall {

    protected String realm;

    @JsonProperty("resource")
    protected String clientId;

    @JsonProperty("realm-public-key")
    protected String publicKeyPEM;

    @JsonProperty("auth-server-url")
    protected String authServerUrl;

    @JsonProperty("ssl-required")
    protected String sslRequired;

    @JsonProperty("public-client")
    protected boolean publicClient;

    @JsonIgnore
    protected PublicKey publicKey;

    @JsonIgnore
    protected String authServerUrlForBackendRequests;

    public ClientInstall() {
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

    public String getPublicKeyPEM() {
        return publicKeyPEM;
    }

    public void setPublicKeyPEM(String publicKeyPEM) {
        this.publicKeyPEM = publicKeyPEM;
    }

    public String getAuthServerUrl() {
        return authServerUrl;
    }

    public void setAuthServerUrl(String authServerUrl) {
        this.authServerUrl = authServerUrl;
    }

    public String getSslRequired() {
        return sslRequired;
    }

    public void setSslRequired(String sslRequired) {
        this.sslRequired = sslRequired;
    }

    public boolean isPublicClient() {
        return publicClient;
    }

    public void setPublicClient(boolean publicClient) {
        this.publicClient = publicClient;
    }

    public PublicKey getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(PublicKey publicKey) {
        this.publicKey = publicKey;
    }

    public String getAuthServerUrlForBackendRequests() {
        return authServerUrlForBackendRequests;
    }

    public void setAuthServerUrlForBackendRequests(String authServerUrlForBackendRequests) {
        this.authServerUrlForBackendRequests = authServerUrlForBackendRequests;
    }
}
