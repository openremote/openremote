package org.openremote.container.web;

/**
 * Details for a client web request, used when forwarding calls through reverse proxy.
 */
public class ClientRequestInfo {

    final protected String remoteAddress;
    final protected String accessToken;

    public ClientRequestInfo(String remoteAddress, String accessToken) {
        this.remoteAddress = remoteAddress;
        this.accessToken = accessToken;
    }

    public String getRemoteAddress() {
        return remoteAddress;
    }

    public String getAccessToken() {
        return accessToken;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
            "remoteAddress='" + remoteAddress + '\'' +
            ", authToken='" + accessToken + '\'' +
            '}';
    }
}
