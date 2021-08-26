/*
 * Copyright 2019, OpenRemote Inc.
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
package org.openremote.agent.protocol.websocket;

import com.fasterxml.jackson.annotation.JsonTypeName;
import org.openremote.model.value.ValueType;

import java.util.List;
import java.util.Map;

@JsonTypeName(WebsocketHTTPSubscription.TYPE)
public class WebsocketHTTPSubscription extends WebsocketSubscription {

    public enum Method {
        GET,
        PUT,
        POST
    }

    public static final String TYPE = "http";

    public Method method;
    public String contentType;
    public ValueType.MultivaluedStringMap headers;
    public String uri;

    public WebsocketHTTPSubscription() {
    }

    public WebsocketHTTPSubscription method(Method method) {
        this.method = method;
        return this;
    }

    public WebsocketHTTPSubscription contentType(String contentType) {
        this.contentType = contentType;
        return this;
    }

    public WebsocketHTTPSubscription headers(ValueType.MultivaluedStringMap headers) {
        this.headers = headers;
        return this;
    }

    public WebsocketHTTPSubscription uri(String uri) {
        this.uri = uri;
        return this;
    }

    public WebsocketHTTPSubscription body(Object body) {
        super.body(body);
        return this;
    }

    @Override
    public String toString() {
        return WebsocketHTTPSubscription.class.getSimpleName() + "{" +
            "method=" + method +
            ", uri='" + uri + '\'' +
            ", contentType='" + contentType + '\'' +
            ", headers=" + headers +
            ", body='" + body + '\'' +
            '}';
    }
}
