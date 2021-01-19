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
package org.openremote.model.auth;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;

import javax.ws.rs.core.MultivaluedMap;
import java.util.Collections;

@JsonTypeName(OAuthRefreshTokenGrant.REFRESH_TOKEN_GRANT_TYPE)
public class OAuthRefreshTokenGrant extends OAuthClientCredentialsGrant {

    public static final String REFRESH_TOKEN_GRANT_TYPE = "refresh_token";
    @JsonProperty(REFRESH_TOKEN_GRANT_TYPE)
    protected String refreshToken;
    @JsonCreator
    public OAuthRefreshTokenGrant(@JsonProperty("tokenEndpointUri") String tokenEndpointUri,
                                  @JsonProperty(VALUE_KEY_CLIENT_ID) String clientId,
                                  @JsonProperty(VALUE_KEY_CLIENT_SECRET) String clientSecret,
                                  @JsonProperty(VALUE_KEY_SCOPE) String scope,
                                  @JsonProperty(REFRESH_TOKEN_GRANT_TYPE) String refreshToken) {
        this(tokenEndpointUri, REFRESH_TOKEN_GRANT_TYPE, clientId, clientSecret, scope, refreshToken);
    }

    protected OAuthRefreshTokenGrant(String tokenEndpointUri,
                                     String grantType,
                                     String clientId,
                                     String clientSecret,
                                     String scope,
                                     String refreshToken) {
        super(tokenEndpointUri, grantType, clientId, clientSecret, scope);
        this.refreshToken = refreshToken;
    }

    @JsonProperty
    public String getRefreshToken() {
        return refreshToken;
    }

    public OAuthRefreshTokenGrant setBasicAuthHeader(boolean basicAuthHeader) {
        return (OAuthRefreshTokenGrant)super.setBasicAuthHeader(basicAuthHeader);
    }

    @Override
    public MultivaluedMap<String, String> asMultivaluedMap() {
        MultivaluedMap<String, String> valueMap = super.asMultivaluedMap();
        valueMap.put(REFRESH_TOKEN_GRANT_TYPE, Collections.singletonList(refreshToken));
        return valueMap;
    }
}
