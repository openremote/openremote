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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.openremote.container.Container;
import org.openremote.model.util.TextUtil;
import org.openremote.model.value.ObjectValue;
import org.openremote.model.value.Values;

import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;

import static org.openremote.model.util.TextUtil.requireNonNullAndNonEmpty;

@JsonTypeInfo(use=JsonTypeInfo.Id.NAME, include=JsonTypeInfo.As.EXISTING_PROPERTY, property = OAuthGrant.VALUE_KEY_GRANT_TYPE)
@JsonSubTypes({
    @JsonSubTypes.Type(name=OAuthPasswordGrant.PASSWORD_GRANT_TYPE, value=OAuthPasswordGrant.class),
    @JsonSubTypes.Type(name=OAuthClientCredentialsGrant.CLIENT_CREDENTIALS_GRANT_TYPE, value=OAuthClientCredentialsGrant.class),
    @JsonSubTypes.Type(name=OAuthRefreshTokenGrant.VALUE_KEY_REFRESH_TOKEN, value=OAuthRefreshTokenGrant.class),
})
public abstract class OAuthGrant {

    public static final String VALUE_KEY_GRANT_TYPE = "grant_type";
    public static final String VALUE_KEY_CLIENT_ID = "client_id";
    public static final String VALUE_KEY_CLIENT_SECRET = "client_secret";
    public static final String VALUE_KEY_SCOPE = "scope";
    @JsonIgnore
    protected MultivaluedMap<String, String> valueMap = new MultivaluedHashMap<>(6);
    protected String tokenEndpointUri;
    @JsonProperty
    protected boolean basicAuthHeader;

    protected OAuthGrant(String tokenEndpointUri, String grantType, String clientId, String clientSecret, String scope) {
        requireNonNullAndNonEmpty(tokenEndpointUri);
        requireNonNullAndNonEmpty(grantType);
        requireNonNullAndNonEmpty(clientId);
        this.tokenEndpointUri = tokenEndpointUri;
        valueMap.add(VALUE_KEY_GRANT_TYPE, grantType);
        valueMap.add(VALUE_KEY_CLIENT_ID, clientId);
        if(!TextUtil.isNullOrEmpty(clientSecret)) {
            valueMap.add(VALUE_KEY_CLIENT_SECRET, clientSecret);
        }
        if(!TextUtil.isNullOrEmpty(scope)) {
            valueMap.add(VALUE_KEY_SCOPE, scope);
        }
    }

    public MultivaluedMap<String, String> getValueMap() {
        return valueMap;
    }

    @JsonProperty
    public String getTokenEndpointUri() {
        return tokenEndpointUri;
    }

    @JsonProperty(VALUE_KEY_GRANT_TYPE)
    public String getGrantType() {
        return valueMap.getFirst(VALUE_KEY_GRANT_TYPE);
    }

    @JsonProperty(VALUE_KEY_CLIENT_ID)
    public String getClientId() {
        return valueMap.getFirst(VALUE_KEY_CLIENT_ID);
    }

    @JsonProperty(VALUE_KEY_CLIENT_SECRET)
    public String getClientSecret() {
        return valueMap.getFirst(VALUE_KEY_CLIENT_SECRET);
    }

    @JsonProperty(VALUE_KEY_SCOPE)
    public String getScope() {
        return valueMap.getFirst(VALUE_KEY_SCOPE);
    }

    public boolean isBasicAuthHeader() {
        return basicAuthHeader;
    }

    public OAuthGrant setBasicAuthHeader(boolean basicAuthHeader) {
        this.basicAuthHeader = basicAuthHeader;
        return this;
    }

    public ObjectValue toObjectValue() {
        try {
            return Values.<ObjectValue>parse(Container.JSON.writeValueAsString(this)).orElse(null);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }
}
