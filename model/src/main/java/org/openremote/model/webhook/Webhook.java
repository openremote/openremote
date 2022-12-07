/*
 * Copyright 2022, OpenRemote Inc.
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
package org.openremote.model.webhook;

import com.fasterxml.jackson.annotation.JsonCreator;
import org.openremote.model.auth.OAuthGrant;
import org.openremote.model.auth.UsernamePassword;
import org.openremote.model.http.HTTPMethod;

import java.util.List;
import java.util.Map;

public class Webhook {

    protected String name;
    protected String url;
    protected Map<String, List<String>> headers;
    protected HTTPMethod httpMethod;
    protected UsernamePassword usernamePassword;
    protected OAuthGrant oAuthGrant;
    protected String payload;

    @JsonCreator
    public Webhook(String name, String url, Map<String, List<String>> headers, HTTPMethod httpMethod, UsernamePassword usernamePassword, OAuthGrant oAuthGrant, String payload) {
        this.name = name;
        this.url = url;
        this.headers = headers;
        this.httpMethod = httpMethod;
        this.usernamePassword = usernamePassword;
        this.oAuthGrant = oAuthGrant;
        this.payload = payload;
    }

    public String getName() {
        return name;
    }

    public Webhook setName(String name) {
        this.name = name;
        return this;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public Map<String, List<String>> getHeaders() {
        return headers;
    }

    public void setHeaders(Map<String, List<String>> headers) {
        this.headers = headers;
    }

    public HTTPMethod getHttpMethod() {
        return httpMethod;
    }

    public void setHttpMethod(HTTPMethod method) {
        this.httpMethod = method;
    }

    public UsernamePassword getUsernamePassword() {
        return usernamePassword;
    }

    public void setAuthMethod(UsernamePassword usernamePassword) {
        this.usernamePassword = usernamePassword;
    }

    public OAuthGrant getOAuthGrant() {
        return oAuthGrant;
    }

    public void setoAuthGrant(OAuthGrant oAuthGrant) {
        this.oAuthGrant = oAuthGrant;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }
}
