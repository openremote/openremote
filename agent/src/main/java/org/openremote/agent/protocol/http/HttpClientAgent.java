/*
 * Copyright 2020, OpenRemote Inc.
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

import org.openremote.model.asset.agent.Agent;
import org.openremote.model.asset.agent.AgentDescriptor;
import org.openremote.model.asset.agent.AgentLink;
import org.openremote.model.value.AttributeDescriptor;
import org.openremote.model.value.ValueDescriptor;
import org.openremote.model.value.ValueType;

import javax.persistence.Entity;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Entity
public class HttpClientAgent extends Agent<HttpClientAgent, HttpClientProtocol, HttpClientAgent.HttpClientAgentLink> {

    public static class HttpClientAgentLink extends AgentLink<HttpClientAgentLink> {

        protected Map<String, List<String>> headers;
        protected Map<String, List<String>> queryParameters;
        protected Integer pollingMillis;
        protected Boolean pagingMode;
        protected String path;
        protected HttpMethod method;
        protected String contentType;
        protected String pollingAttribute;

        // For Hydrators
        protected HttpClientAgentLink() {}

        public HttpClientAgentLink(String id) {
            super(id);
        }

        public Optional<Map<String, List<String>>> getHeaders() {
            return Optional.ofNullable(headers);
        }

        public HttpClientAgentLink setHeaders(ValueType.MultivaluedStringMap headers) {
            this.headers = headers;
            return this;
        }

        public Optional<Map<String, List<String>>> getQueryParameters() {
            return Optional.ofNullable(queryParameters);
        }

        public HttpClientAgentLink setQueryParameters(ValueType.MultivaluedStringMap queryParameters) {
            this.queryParameters = queryParameters;
            return this;
        }

        public Optional<Integer> getPollingMillis() {
            return Optional.ofNullable(pollingMillis);
        }

        public HttpClientAgentLink setPollingMillis(Integer pollingMillis) {
            this.pollingMillis = pollingMillis;
            return this;
        }

        public Optional<Boolean> getPagingMode() {
            return Optional.ofNullable(pagingMode);
        }

        public HttpClientAgentLink setPagingMode(Boolean pagingMode) {
            this.pagingMode = pagingMode;
            return this;            
        }

        public Optional<String> getPath() {
            return Optional.ofNullable(path);
        }

        public HttpClientAgentLink setPath(String path) {
            this.path = path;
            return this;
        }

        public Optional<HttpMethod> getMethod() {
            return Optional.ofNullable(method);
        }

        public HttpClientAgentLink setMethod(HttpMethod method) {
            this.method = method;
            return this;
        }

        public Optional<String> getContentType() {
            return Optional.ofNullable(contentType);
        }

        public HttpClientAgentLink setContentType(String contentType) {
            this.contentType = contentType;
            return this;
        }

        public Optional<String> getPollingAttribute() {
            return Optional.ofNullable(pollingAttribute);
        }

        public HttpClientAgentLink setPollingAttribute(String pollingAttribute) {
            this.pollingAttribute = pollingAttribute;
            return this;
        }
    }

    public static final ValueDescriptor<HttpMethod> VALUE_HTTP_METHOD = new ValueDescriptor<>("HTTPMethod", HttpMethod.class);

    public static final AttributeDescriptor<String> BASE_URI = new AttributeDescriptor<>("baseURI", ValueType.TEXT);
    public static final AttributeDescriptor<Boolean> FOLLOW_REDIRECTS = new AttributeDescriptor<>("followRedirects", ValueType.BOOLEAN);
    public static final AttributeDescriptor<ValueType.MultivaluedStringMap> REQUEST_HEADERS = new AttributeDescriptor<>("requestHeaders", ValueType.MULTIVALUED_TEXT_MAP);
    public static final AttributeDescriptor<ValueType.MultivaluedStringMap> REQUEST_QUERY_PARAMETERS = new AttributeDescriptor<>("requestQueryParameters", ValueType.MULTIVALUED_TEXT_MAP);
    public static final AttributeDescriptor<Integer> REQUEST_TIMEOUT_MILLIS = new AttributeDescriptor<>("requestTimeoutMillis", ValueType.POSITIVE_INTEGER);

    public static final AgentDescriptor<HttpClientAgent, HttpClientProtocol, HttpClientAgentLink> DESCRIPTOR = new AgentDescriptor<>(
        HttpClientAgent.class, HttpClientProtocol.class, HttpClientAgentLink.class
    );

    /**
     * For use by hydrators (i.e. JPA/Jackson)
     */
    protected HttpClientAgent() {
    }

    public HttpClientAgent(String name) {
        super(name);
    }

    public Optional<String> getBaseURI() {
        return getAttributes().getValue(BASE_URI);
    }

    public HttpClientAgent setBaseURI(String value) {
        getAttributes().getOrCreate(BASE_URI).setValue(value);
        return this;
    }

    public Optional<Boolean> getFollowRedirects() {
        return getAttributes().getValue(FOLLOW_REDIRECTS);
    }

    public HttpClientAgent setFollowRedirects(Boolean value) {
        getAttributes().getOrCreate(FOLLOW_REDIRECTS).setValue(value);
        return this;
    }

    public Optional<ValueType.MultivaluedStringMap> getRequestHeaders() {
        return getAttributes().getValue(REQUEST_HEADERS);
    }

    public HttpClientAgent setRequestHeaders(ValueType.MultivaluedStringMap value) {
        getAttributes().getOrCreate(REQUEST_HEADERS).setValue(value);
        return this;
    }

    public Optional<ValueType.MultivaluedStringMap> getRequestQueryParameters() {
        return getAttributes().getValue(REQUEST_QUERY_PARAMETERS);
    }

    public HttpClientAgent setRequestQueryParameters(ValueType.MultivaluedStringMap value) {
        getAttributes().getOrCreate(REQUEST_QUERY_PARAMETERS).setValue(value);
        return this;
    }

    public Optional<Integer> getRequestTimeoutMillis() {
        return getAttributes().getValue(REQUEST_TIMEOUT_MILLIS);
    }

    public HttpClientAgent setRequestTimeoutMillis(Integer value) {
        getAttributes().getOrCreate(REQUEST_TIMEOUT_MILLIS).setValue(value);
        return this;
    }

    @Override
    public HttpClientProtocol getProtocolInstance() {
        return new HttpClientProtocol(this);
    }
}
