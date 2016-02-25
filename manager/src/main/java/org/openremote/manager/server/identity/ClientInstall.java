package org.openremote.manager.server.identity;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ClientInstall {

    protected String realm;

    @JsonProperty("resource")
    protected String clientId;

    @JsonProperty("realm-public-key")
    protected String realmPublicKey;

    @JsonProperty("auth-server-url")
    protected String authServerUrl;

    @JsonProperty("ssl-required")
    protected String sslRequired;

    @JsonProperty("public-client")
    protected boolean publicClient;

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

    public String getRealmPublicKey() {
        return realmPublicKey;
    }

    public void setRealmPublicKey(String realmPublicKey) {
        this.realmPublicKey = realmPublicKey;
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
}
