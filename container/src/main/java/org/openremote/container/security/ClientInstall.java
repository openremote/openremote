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
