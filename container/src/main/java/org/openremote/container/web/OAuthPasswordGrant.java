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
package org.openremote.container.web;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;

@JsonTypeName(OAuthPasswordGrant.PASSWORD_GRANT_TYPE)
public class OAuthPasswordGrant extends OAuthClientCredentialsGrant {

    public static final String VALUE_KEY_USERNAME = "username";
    public static final String VALUE_KEY_PASSWORD = "password";
    public static final String PASSWORD_GRANT_TYPE = "password";

    @JsonCreator
    public OAuthPasswordGrant(@JsonProperty("tokenEndpointUri") String tokenEndpointUri,
                              @JsonProperty(VALUE_KEY_CLIENT_ID) String clientId,
                              @JsonProperty(VALUE_KEY_CLIENT_SECRET) String clientSecret,
                              @JsonProperty(VALUE_KEY_SCOPE) String scope,
                              @JsonProperty(VALUE_KEY_USERNAME) String username,
                              @JsonProperty(VALUE_KEY_PASSWORD) String password) {
        this(tokenEndpointUri, PASSWORD_GRANT_TYPE, clientId, clientSecret, scope, username, password);
    }

    protected OAuthPasswordGrant(String tokenEndpointUri,
                                 String grantType,
                                 String clientId,
                                 String clientSecret,
                                 String scope,
                                 String username,
                                 String password
                                 ) {
        super(tokenEndpointUri, grantType, clientId, clientSecret, scope);
        valueMap.add(VALUE_KEY_USERNAME, username);
        valueMap.add(VALUE_KEY_PASSWORD, password);
    }

    @JsonProperty
    public String getUsername() {
        return valueMap.getFirst(VALUE_KEY_USERNAME);
    }

    @JsonProperty
    public String getPassword() {
        return valueMap.getFirst(VALUE_KEY_PASSWORD);
    }

    public OAuthPasswordGrant setBasicAuthHeader(boolean basicAuthHeader) {
        return (OAuthPasswordGrant)super.setBasicAuthHeader(basicAuthHeader);
    }
}
