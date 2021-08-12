/*
 * Copyright 2021, OpenRemote Inc.
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
package org.openremote.agent.protocol.http;

import org.openremote.model.asset.agent.AgentLink;
import org.openremote.model.value.ValueType;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public class HTTPAgentLink extends AgentLink<HTTPAgentLink> {

    protected Map<String, List<String>> headers;
    protected Map<String, List<String>> queryParameters;
    protected Integer pollingMillis;
    protected Boolean pagingMode;
    protected String path;
    protected HTTPMethod method;
    protected String contentType;
    protected String pollingAttribute;

    // For Hydrators
    protected HTTPAgentLink() {
    }

    public HTTPAgentLink(String id) {
        super(id);
    }

    public Optional<Map<String, List<String>>> getHeaders() {
        return Optional.ofNullable(headers);
    }

    public HTTPAgentLink setHeaders(ValueType.MultivaluedStringMap headers) {
        this.headers = headers;
        return this;
    }

    public Optional<Map<String, List<String>>> getQueryParameters() {
        return Optional.ofNullable(queryParameters);
    }

    public HTTPAgentLink setQueryParameters(ValueType.MultivaluedStringMap queryParameters) {
        this.queryParameters = queryParameters;
        return this;
    }

    public Optional<Integer> getPollingMillis() {
        return Optional.ofNullable(pollingMillis);
    }

    public HTTPAgentLink setPollingMillis(Integer pollingMillis) {
        this.pollingMillis = pollingMillis;
        return this;
    }

    public Optional<Boolean> getPagingMode() {
        return Optional.ofNullable(pagingMode);
    }

    public HTTPAgentLink setPagingMode(Boolean pagingMode) {
        this.pagingMode = pagingMode;
        return this;
    }

    public Optional<String> getPath() {
        return Optional.ofNullable(path);
    }

    public HTTPAgentLink setPath(String path) {
        this.path = path;
        return this;
    }

    public Optional<HTTPMethod> getMethod() {
        return Optional.ofNullable(method);
    }

    public HTTPAgentLink setMethod(HTTPMethod method) {
        this.method = method;
        return this;
    }

    public Optional<String> getContentType() {
        return Optional.ofNullable(contentType);
    }

    public HTTPAgentLink setContentType(String contentType) {
        this.contentType = contentType;
        return this;
    }

    public Optional<String> getPollingAttribute() {
        return Optional.ofNullable(pollingAttribute);
    }

    public HTTPAgentLink setPollingAttribute(String pollingAttribute) {
        this.pollingAttribute = pollingAttribute;
        return this;
    }
}
