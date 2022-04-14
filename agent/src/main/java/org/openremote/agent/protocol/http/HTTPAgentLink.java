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

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import org.openremote.model.asset.agent.AgentLink;
import org.openremote.model.value.ValueType;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public class HTTPAgentLink extends AgentLink<HTTPAgentLink> {

    @JsonPropertyDescription("A JSON object of headers to be added to HTTP request; the key represents the name of the header and for each string value" +
        " supplied a new header will be added with the key name and specified string value")
    protected Map<String, List<String>> headers;
    @JsonPropertyDescription("A JSON object of query parameters to be added to HTTP request URL; the key represents the name of the query parameter and for each string value" +
        " supplied a new query parameter will be added with the key name and specified string value (e.g. 'https://..../?test=1&test=2')")
    protected Map<String, List<String>> queryParameters;
    @JsonPropertyDescription("Indicates that this HTTP request is used to update the linked attribute; this value indicates how frequently the HTTP request is made in order" +
        " to update the linked attribute value")
    protected Integer pollingMillis;
    @JsonPropertyDescription("Indicates that the HTTP server supports pagination using the standard Link header mechanism")
    protected Boolean pagingMode;
    @JsonPropertyDescription("The URL path to append to the agents Base URL when making requests for this linked attribute")
    protected String path;
    @JsonPropertyDescription("The HTTP method to use when making requests for this linked attribute")
    protected HTTPMethod method;
    @JsonPropertyDescription("The content type header value to use when making requests for this linked attribute (shortcut alternative to using headers parameter)")
    protected String contentType;
    @JsonPropertyDescription("Allows the polled response to be written to another attribute with the specified name on the same asset as the linked attribute")
    protected String pollingAttribute;
    @JsonPropertyDescription("Indicates that the HTTP response is binary and should be converted to binary string representation")
    protected boolean messageConvertBinary;
    @JsonPropertyDescription("Indicates that the HTTP response is binary and should be converted to hexidecimal string representation")
    protected boolean messageConvertHex;

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

    public boolean isMessageConvertBinary() {
        return messageConvertBinary;
    }

    public HTTPAgentLink setMessageConvertBinary(boolean messageConvertBinary) {
        this.messageConvertBinary = messageConvertBinary;
        return this;
    }

    public boolean isMessageConvertHex() {
        return messageConvertHex;
    }

    public HTTPAgentLink setMessageConvertHex(boolean messageConvertHex) {
        this.messageConvertHex = messageConvertHex;
        return this;
    }
}
