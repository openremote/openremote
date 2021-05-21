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
import java.util.Objects;

@JsonTypeName(OAuthPasswordGrant.PASSWORD_GRANT_TYPE)
public class OAuthPasswordGrant extends OAuthClientCredentialsGrant {

    public static final String VALUE_KEY_USERNAME = "username";
    public static final String VALUE_KEY_PASSWORD = "password";
    public static final String PASSWORD_GRANT_TYPE = "password";
    protected String username;
    protected String password;

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
        this.username = username;
        this.password = password;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public OAuthPasswordGrant setUsername(String username) {
        this.username = username;
        return this;
    }

    public OAuthPasswordGrant setPassword(String password) {
        this.password = password;
        return this;
    }

    public OAuthPasswordGrant setBasicAuthHeader(boolean basicAuthHeader) {
        return (OAuthPasswordGrant)super.setBasicAuthHeader(basicAuthHeader);
    }

    @Override
    public MultivaluedMap<String, String> asMultivaluedMap() {
        MultivaluedMap<String, String> valueMap = super.asMultivaluedMap();
        valueMap.put(VALUE_KEY_USERNAME, Collections.singletonList(username));
        valueMap.put(VALUE_KEY_PASSWORD, Collections.singletonList(password));
        return valueMap;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        OAuthPasswordGrant that = (OAuthPasswordGrant) o;
        return Objects.equals(username, that.username) && Objects.equals(password, that.password);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), username, password);
    }

    @Override
    public String toString() {
        return OAuthPasswordGrant.class.getSimpleName() + "{" +
            "tokenEndpointUri='" + tokenEndpointUri + '\'' +
            ", basicAuthHeader=" + basicAuthHeader +
            ", grantType='" + grantType + '\'' +
            ", clientId='" + clientId + '\'' +
            ", clientSecret='" + clientSecret + '\'' +
            ", scope='" + scope + '\'' +
            ", username='" + username + '\'' +
            ", password='" + password + '\'' +
            '}';
    }
}
