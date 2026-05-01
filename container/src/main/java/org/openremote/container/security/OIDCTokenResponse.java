/*
 * Copyright 2026, OpenRemote Inc.
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
