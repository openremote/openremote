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

@JsonTypeName(OAuthClientCredentialsGrant.CLIENT_CREDENTIALS_GRANT_TYPE)
public class OAuthClientCredentialsGrant extends OAuthGrant {
    public static final String CLIENT_CREDENTIALS_GRANT_TYPE = "client_credentials";

    @JsonCreator
    public OAuthClientCredentialsGrant(@JsonProperty("tokenEndpointUri") String tokenEndpointUri,
                                       @JsonProperty(VALUE_KEY_CLIENT_ID) String clientId,
                                       @JsonProperty(VALUE_KEY_CLIENT_SECRET) String clientSecret,
                                       @JsonProperty(VALUE_KEY_SCOPE) String scope) {
        this(tokenEndpointUri, CLIENT_CREDENTIALS_GRANT_TYPE, clientId, clientSecret, scope);
    }

    protected OAuthClientCredentialsGrant(String tokenEndpointUri,
                                          String grantType,
                                          String clientId,
                                          String clientSecret,
                                          String scope) {
        super(tokenEndpointUri, grantType, clientId, clientSecret, scope);
    }

    public OAuthClientCredentialsGrant setBasicAuthHeader(boolean basicAuthHeader) {
        return (OAuthClientCredentialsGrant)super.setBasicAuthHeader(basicAuthHeader);
    }
}
