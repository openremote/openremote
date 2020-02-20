/*
 * Copyright 2016, OpenRemote Inc.
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
package org.openremote.model.http;

import javaemul.internal.annotations.GwtIncompatible;
import jsinterop.annotations.JsIgnore;
import jsinterop.annotations.JsProperty;
import jsinterop.annotations.JsType;
import org.openremote.model.interop.Function;
import org.openremote.model.util.TextUtil;

import javax.ws.rs.HeaderParam;
import javax.ws.rs.core.*;
import java.net.URI;

@JsType
public class RequestParams<IN, OUT> {

    @HeaderParam(HttpHeaders.AUTHORIZATION)
    @JsProperty(name = HttpHeaders.AUTHORIZATION)
    public String authorization;

    @HeaderParam("X-Forwarded-Proto")
    public String forwardedProtoHeader;

    @HeaderParam("X-Forwarded-Host")
    public String forwardedHostHeader;

    @HeaderParam("X-Forwarded-Port")
    public Integer forwardedPortHeader;

    @JsIgnore
    @Context
    @GwtIncompatible
    public UriInfo uriInfo;

    @JsProperty(name = "$entity")
    public String entity;

    @JsProperty(name = "$entityWriter")
    public Function<IN, String> entityWriter;

    @JsProperty(name = "$contentType")
    public String contentType;

    @JsProperty(name = "$accepts")
    public String accepts;

    @JsProperty(name = "$apiURL")
    public String apiURL;

    @JsProperty(name = "$username")
    public String username;

    @JsProperty(name = "$password")
    public String password;

    @JsProperty(name = "$callback")
    public RequestCallback callback;

    @JsProperty(name = "$async")
    public boolean async = true; // Default to async request

    @JsIgnore
    public RequestParams() {
        this(null);
    }

    /**
     * Defaults to "<code>Accept: application/json</code>" header, call {@link #setAccepts(String)} to override.
     */
    public RequestParams(RequestCallback requestCallback) {
        this.callback = requestCallback;
        this.accepts = MediaType.APPLICATION_JSON;
    }

    public RequestParams<IN, OUT> withBearerAuth(String authorization) {
        this.authorization = "Bearer " + authorization;
        return this;
    }

    public String getBearerAuth() {
        if (authorization == null || !authorization.startsWith("Bearer ") || authorization.split(" ").length != 2)
            return null;
        return authorization.split(" ")[1];
    }

    /**
     * Handles reverse proxying and returns the request base URI
     */
    @JsIgnore
    public UriBuilder getRequestBaseUri() {
        URI uri = this.uriInfo.getRequestUri();
        String scheme = TextUtil.isNullOrEmpty(this.forwardedProtoHeader) ? uri.getScheme() : this.forwardedProtoHeader;
        int port = this.forwardedPortHeader == null ? uri.getPort() : this.forwardedPortHeader;
        String host = TextUtil.isNullOrEmpty(this.forwardedHostHeader) ? uri.getHost() : this.forwardedHostHeader;
        return this.uriInfo.getBaseUriBuilder().scheme(scheme).host(host).port(port);
    }

    public RequestParams<IN, OUT> setEntity(String entity) {
        this.entity = entity;
        return this;
    }

    public RequestParams<IN, OUT> setEntityWriter(Function<IN, String> entityWriter) {
        this.entityWriter = entityWriter;
        return this;
    }

    public RequestParams<IN, OUT> setContentType(String contentType) {
        this.contentType = contentType;
        return this;
    }

    public RequestParams<IN, OUT> setAccepts(String accepts) {
        this.accepts = accepts;
        return this;
    }

    public RequestParams<IN, OUT> setApiURL(String apiURL) {
        this.apiURL = apiURL;
        return this;
    }

    public RequestParams<IN, OUT> setUsername(String username) {
        this.username = username;
        return this;
    }

    public RequestParams<IN, OUT> setPassword(String password) {
        this.password = password;
        return this;
    }

}
