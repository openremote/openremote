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
package org.openremote.manager.shared.ngsi.params;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import javax.ws.rs.HttpMethod;
import java.util.Map;

public class HttpCustomParams extends HttpParams {
    @JsonInclude(JsonInclude.Include.NON_NULL)
    protected Map<String, String> headers;
    @JsonProperty("qs")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    protected Map<String, String> parameters;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    protected HttpMethod method;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    protected String payload;

    public HttpCustomParams(@JsonProperty String url, @JsonProperty Map<String, String> headers, @JsonProperty("qs") Map<String, String> parameters, @JsonProperty HttpMethod method, @JsonProperty String payload) {
        super(url);
        this.headers = headers;
        this.parameters = parameters;
        this.method = method;
        this.payload = payload;
    }

    public HttpCustomParams(String url) {
        super(url);
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public void setHeaders(Map<String, String> headers) {
        this.headers = headers;
    }

    public Map<String, String> getParameters() {
        return parameters;
    }

    public void setParameters(Map<String, String> parameters) {
        this.parameters = parameters;
    }

    public HttpMethod getMethod() {
        return method;
    }

    public void setMethod(HttpMethod method) {
        this.method = method;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }
}
