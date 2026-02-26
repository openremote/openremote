package org.openremote.container.security;

import org.keycloak.representations.AccessTokenResponse;

/**
 * Describes the payload returned by an OIDC token endpoint.
 */
public interface OIDCTokenResponse {
    String getToken();
    String getIdToken();
    String getRefreshToken();
    String getTokenType();
    long getRefreshExpiresIn();
    long getExpiresIn();
    String getScope();
    int getNotBeforePolicy();
    String getSessionState();

    static OIDCTokenResponse create(AccessTokenResponse keycloakTokenResponse) {
        return new OIDCTokenResponse() {
            @Override
            public String getToken() {
                return keycloakTokenResponse.getToken();
            }

            @Override
            public String getIdToken() {
                return keycloakTokenResponse.getIdToken();
            }

            @Override
            public String getRefreshToken() {
                return keycloakTokenResponse.getRefreshToken();
            }

            @Override
            public String getTokenType() {
                return keycloakTokenResponse.getTokenType();
            }

            @Override
            public long getRefreshExpiresIn() {
                return keycloakTokenResponse.getRefreshExpiresIn();
            }

            @Override
            public long getExpiresIn() {
                return keycloakTokenResponse.getExpiresIn();
            }

            @Override
            public String getScope() {
                return keycloakTokenResponse.getScope();
            }

            @Override
            public int getNotBeforePolicy() {
                return keycloakTokenResponse.getNotBeforePolicy();
            }

            @Override
            public String getSessionState() {
                return keycloakTokenResponse.getSessionState();
            }
        };
    }
}
