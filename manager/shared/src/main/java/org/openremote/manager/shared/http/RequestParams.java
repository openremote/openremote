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
package org.openremote.manager.shared.http;

import jsinterop.annotations.JsIgnore;
import jsinterop.annotations.JsProperty;
import jsinterop.annotations.JsType;

import javax.ws.rs.HeaderParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;

@JsType
public class RequestParams<OUT> {

    @HeaderParam(HttpHeaders.AUTHORIZATION)
    @JsProperty(name = HttpHeaders.AUTHORIZATION)
    public String authorization;

    @JsProperty(name = "$entity")
    public String entity;

    @JsProperty(name = "$entityWriter")
    public EntityWriter entityWriter;

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
    public Request.InternalCallback callback;

    @JsProperty(name = "$async")
    public boolean async = true; // Default to async request

    @JsIgnore
    public RequestParams() {
    }

    /**
     * Defaults to "<code>Accept: application/json</code>" header, call {@link #setAccepts(String)} to override.
     */
    @JsIgnore
    public RequestParams(RequestCallback<OUT> requestCallback) {
        this.callback = new Request.InternalCallbackImpl(requestCallback);
        this.accepts = MediaType.APPLICATION_JSON;
    }

    public RequestParams<OUT> withBearerAuth(String authorization) {
        this.authorization = "Bearer " + authorization;
        return this;
    }

    public String getBearerAuth() {
        if (authorization == null || !authorization.startsWith("Bearer ") || authorization.split(" ").length != 2)
            return null;
        return authorization.split(" ")[1];
    }

    public RequestParams<OUT> setEntity(String entity) {
        this.entity = entity;
        return this;
    }

    public RequestParams<OUT> setEntityWriter(EntityWriter entityWriter) {
        this.entityWriter = entityWriter;
        return this;
    }

    public RequestParams<OUT> setContentType(String contentType) {
        this.contentType = contentType;
        return this;
    }

    public RequestParams<OUT> setAccepts(String accepts) {
        this.accepts = accepts;
        return this;
    }

    public RequestParams<OUT> setApiURL(String apiURL) {
        this.apiURL = apiURL;
        return this;
    }

    public RequestParams<OUT> setUsername(String username) {
        this.username = username;
        return this;
    }

    public RequestParams<OUT> setPassword(String password) {
        this.password = password;
        return this;
    }

    public RequestParams<OUT> setAsync(boolean async) {
        this.async = async;
        return this;
    }
}
