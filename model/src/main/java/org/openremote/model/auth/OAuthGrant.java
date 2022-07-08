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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.openremote.model.util.TextUtil;

import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import java.io.Serializable;
import java.util.*;

import static org.openremote.model.util.TextUtil.requireNonNullAndNonEmpty;

@JsonTypeInfo(use=JsonTypeInfo.Id.NAME, include=JsonTypeInfo.As.EXISTING_PROPERTY, property = OAuthGrant.VALUE_KEY_GRANT_TYPE)
@JsonSubTypes({
    @JsonSubTypes.Type(name=OAuthPasswordGrant.PASSWORD_GRANT_TYPE, value=OAuthPasswordGrant.class),
    @JsonSubTypes.Type(name=OAuthClientCredentialsGrant.CLIENT_CREDENTIALS_GRANT_TYPE, value=OAuthClientCredentialsGrant.class),
    @JsonSubTypes.Type(name=OAuthRefreshTokenGrant.REFRESH_TOKEN_GRANT_TYPE, value=OAuthRefreshTokenGrant.class),
})
public abstract class OAuthGrant implements Serializable {

    public static final String VALUE_KEY_GRANT_TYPE = "grant_type";
    public static final String VALUE_KEY_CLIENT_ID = "client_id";
    public static final String VALUE_KEY_CLIENT_SECRET = "client_secret";
    public static final String VALUE_KEY_SCOPE = "scope";
    protected String tokenEndpointUri;
    protected boolean basicAuthHeader;
    protected Map<String, List<String>> additionalValueMap = null;
    @JsonProperty(VALUE_KEY_GRANT_TYPE)
    protected String grantType;
    @JsonProperty(VALUE_KEY_CLIENT_ID)
    String clientId;
    @JsonProperty(VALUE_KEY_CLIENT_SECRET)
    String clientSecret;
    @JsonProperty(VALUE_KEY_SCOPE)
    String scope;

    protected OAuthGrant(String tokenEndpointUri, String grantType, String clientId, String clientSecret, String scope) {
        requireNonNullAndNonEmpty(tokenEndpointUri);
        requireNonNullAndNonEmpty(grantType);
        requireNonNullAndNonEmpty(clientId);
        this.grantType = grantType;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.scope = scope;
        this.tokenEndpointUri = tokenEndpointUri;
    }

    public MultivaluedMap<String, String> asMultivaluedMap() {
        MultivaluedMap<String, String> valueMap = new MultivaluedHashMap<>();
        valueMap.put(VALUE_KEY_GRANT_TYPE, Collections.singletonList(grantType));
        valueMap.put(VALUE_KEY_CLIENT_ID, Collections.singletonList(clientId));
        if(!TextUtil.isNullOrEmpty(clientSecret)) {
            valueMap.put(VALUE_KEY_CLIENT_SECRET, Collections.singletonList(clientSecret));
        }
        if(!TextUtil.isNullOrEmpty(scope)) {
            valueMap.put(VALUE_KEY_SCOPE, Collections.singletonList(scope));
        }
        if(additionalValueMap != null)
            valueMap.putAll(additionalValueMap);
        return valueMap;
    }

    public void addFormParameter(String key, String value) {
        if(additionalValueMap == null)
            additionalValueMap = new HashMap<>();
        additionalValueMap.put(key, Collections.singletonList(value));
    }

    public String getTokenEndpointUri() {
        return tokenEndpointUri;
    }

    public OAuthGrant setTokenEndpointUri(String tokenEndpointUri) {
        this.tokenEndpointUri = tokenEndpointUri;
        return this;
    }

    public String getGrantType() {
        return grantType;
    }

    public String getClientId() {
        return clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public String getScope() {
        return scope;
    }

    public boolean isBasicAuthHeader() {
        return basicAuthHeader;
    }

    public OAuthGrant setBasicAuthHeader(boolean basicAuthHeader) {
        this.basicAuthHeader = basicAuthHeader;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OAuthGrant grant = (OAuthGrant) o;
        return basicAuthHeader == grant.basicAuthHeader && Objects.equals(tokenEndpointUri, grant.tokenEndpointUri) && Objects.equals(grantType, grant.grantType) && Objects.equals(clientId, grant.clientId) && Objects.equals(clientSecret, grant.clientSecret) && Objects.equals(scope, grant.scope);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tokenEndpointUri, basicAuthHeader, grantType, clientId, clientSecret, scope);
    }

    @Override
    public String toString() {
        return OAuthGrant.class.getSimpleName() + "{" +
            "tokenEndpointUri='" + tokenEndpointUri + '\'' +
            ", basicAuthHeader=" + basicAuthHeader +
            ", grantType='" + grantType + '\'' +
            ", clientId='" + clientId + '\'' +
            ", clientSecret='" + clientSecret + '\'' +
            ", scope='" + scope + '\'' +
            '}';
    }
}
