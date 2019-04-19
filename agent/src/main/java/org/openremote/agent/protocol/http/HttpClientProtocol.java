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
package org.openremote.agent.protocol.http;

import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;
import org.jboss.resteasy.core.Headers;
import org.jboss.resteasy.specimpl.BuiltResponse;
import org.jboss.resteasy.specimpl.ResponseBuilderImpl;
import org.openremote.agent.protocol.AbstractProtocol;
import org.openremote.agent.protocol.Protocol;
import org.openremote.container.Container;
import org.openremote.model.AbstractValueHolder;
import org.openremote.model.ValidationFailure;
import org.openremote.model.ValueHolder;
import org.openremote.model.asset.AssetAttribute;
import org.openremote.model.attribute.MetaItemType;
import org.openremote.model.asset.agent.ConnectionStatus;
import org.openremote.model.asset.agent.ProtocolConfiguration;
import org.openremote.model.attribute.*;
import org.openremote.model.util.Pair;
import org.openremote.model.util.TextUtil;
import org.openremote.model.value.*;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.openremote.container.concurrent.GlobalLock.withLock;
import static org.openremote.model.Constants.PROTOCOL_NAMESPACE;
import static org.openremote.model.util.TextUtil.*;

/**
 * This is a HTTP client protocol for communicating with HTTP servers; it uses the {@link WebTargetBuilder} factory to
 * generate JAX-RS {@link javax.ws.rs.client.WebTarget}s that can be used to make arbitrary calls to endpoints on a HTTP
 * server but it can also be extended and used as a JAX-RS client proxy.
 * <p>
 * <h1>Protocol Configurations</h1>
 * <p>
 * {@link Attribute}s that are configured as {@link ProtocolConfiguration}s for this protocol support the following meta
 * items: <ul> <li>{@link #META_PROTOCOL_BASE_URI} (<b>required</b>)</li> <li>{@link #META_PROTOCOL_USERNAME}</li>
 * <li>{@link #META_PROTOCOL_PASSWORD}</li> <li>{@link #META_PROTOCOL_OAUTH_GRANT}</li> <li>{@link
 * #META_PROTOCOL_PING_PATH}</li> <li>{@link #META_PROTOCOL_PING_METHOD}</li> <li>{@link #META_PROTOCOL_PING_BODY}</li>
 * <li>{@link #META_PROTOCOL_PING_QUERY_PARAMETERS}</li> <li>{@link #META_PROTOCOL_PING_SECONDS}</li> <li>{@link
 * #META_PROTOCOL_FOLLOW_REDIRECTS}</li> <li>{@link #META_FAILURE_CODES}</li> <li>{@link #META_HEADERS}</li> </ul>
 * <h1>Linked Attributes</h1>
 * <p>
 * {@link Attribute}s that are linked to this protocol using an {@link MetaItemType#AGENT_LINK} {@link MetaItem} support
 * the following meta items: <ul> <li>{@link #META_ATTRIBUTE_PATH} (<b>if not supplied then base URI is used</b>)</li>
 * <li>{@link #META_ATTRIBUTE_METHOD}</li> <li>{@link #META_ATTRIBUTE_BODY}</li> <li>{@link
 * #META_ATTRIBUTE_POLLING_SECONDS} (<b>required if attribute value should be set by the response received from this
 * endpoint</b>)</li> <li>{@link #META_QUERY_PARAMETERS}</li> <li>{@link #META_FAILURE_CODES}</li> <li>{@link #META_HEADERS}</li> </ul>
 * <p>
 * <h1>Response filtering</h1>
 * <p>
 * Any {@link Attribute} whose value is to be set by the HTTP server response (i.e. it has an {@link
 * #META_ATTRIBUTE_POLLING_SECONDS} {@link MetaItem}) can use the standard {@link Protocol#META_PROTOCOL_FILTERS} in
 * order to filter the received HTTP response.
 * <p>
 * <h1>Connection Status</h1>
 * <p>
 * The {@link ConnectionStatus} of the {@link ProtocolConfiguration} is determined by the ping {@link
 * org.openremote.model.attribute.MetaItem}s on the protocol configuration. If specified then the {@link
 * #META_PROTOCOL_PING_PATH} will be called every {@link #META_PROTOCOL_PING_SECONDS} this should be a lightweight
 * endpoint (i.e. no response body or small response body) and if the HTTP server requires authentication then this ping
 * endpoint should also be a secured endpoint to validate the credentials.
 * <p>
 * If the {@link #META_PROTOCOL_PING_PATH} is not configured then the {@link ConnectionStatus} will be set based on the
 * responses received from any linked {@link Attribute} requests, the connection status is determined as follows:
 * <p>
 * <ul> <li>No request/response yet sent/received = {@link ConnectionStatus#UNKNOWN}</li> <li>Response status in 100
 * range = {@link ConnectionStatus#ERROR}</li> <li>Response status in 200 range = {@link
 * ConnectionStatus#CONNECTED}</li> <li>Response status in 300 range = {@link ConnectionStatus#ERROR} (unless {@link
 * #META_PROTOCOL_FOLLOW_REDIRECTS} = true)</li> <li>Response status 401/402/403 = {@link
 * ConnectionStatus#ERROR_AUTHENTICATION}</li> <li>Response status in 404+ range = {@link
 * ConnectionStatus#ERROR_CONFIGURATION}</li> <li>Response status in 500 range = {@link ConnectionStatus#ERROR}</li>
 * </ul>
 * <p>
 * <b>NOTE: if an exception is thrown during the request that means no response is returned then this is treated as if
 * a 500 response has been received</b>
 * <h1>Dynamic value injection</h1>
 * This allows the {@link #META_ATTRIBUTE_PATH} and/or {@link #META_ATTRIBUTE_BODY} to contain the linked
 * {@link Attribute} value when sending requests. To dynamically inject the attribute value use
 * {@value #DYNAMIC_VALUE_PLACEHOLDER} as a placeholder and this will be dynamically replaced at request time.
 * <h2>Path example</h2>
 * {@link #META_ATTRIBUTE_PATH} = "volume/set/{$value}" and request received to set attribute value to 100. Actual path
 * used for the request = "volume/set/100"
 * <h2>Query parameter example</h2>
 * {@link #META_QUERY_PARAMETERS} =
 * <blockquote><pre>
 * {@code
 * {
 *     param1: ["val1", "val2"],
 *     param2: 12232,
 *     param3: "{$value}"
 * }
 * }
 * </pre></blockquote>
 * Request received to set attribute value to true. Actual query parameters injected into the request =
 * "param1=val1&param1=val2&param2=12232&param3=true"
 * <h2>Body examples</h2>
 * {@link #META_ATTRIBUTE_BODY} = '<?xml version="1.0" encoding="UTF-8"?>{$value}</xml>' and request received to set attribute value to 100. Actual body
 * used for the request = "{volume: 100}"
 * <p>
 * {@link #META_ATTRIBUTE_BODY} = '{myObject: "{$value}"}' and request received to set attribute value to:
 * <blockquote><pre>
 * {@code
 * {
 *   prop1: true,
 *   prop2: "test",
 *   prop3: {
 *       prop4: 1234.4223
 *   }
 * }
 * }
 * </pre></blockquote>
 * Actual body used for the request = "{myObject: {prop1: true, prop2: "test", prop3: {prop4: 1234.4223}}}"
 */
public class HttpClientProtocol extends AbstractProtocol {

    public static class HttpClientRequest {

        protected String path;
        public String method;
        public MultivaluedMap<String, String> headers;
        public MultivaluedMap<String, String> queryParameters;
        protected List<Integer> failureCodes;
        public String body;
        protected String contentType;
        protected WebTarget client;
        protected WebTarget requestTarget;
        protected boolean dynamicQueryParameters;
        protected boolean dynamicBody;
        protected boolean updateConnectionStatus;
        protected boolean pagingEnabled;

        public HttpClientRequest(WebTarget client,
                                 String path,
                                 String method,
                                 MultivaluedMap<String, String> headers,
                                 MultivaluedMap<String, String> queryParameters,
                                 List<Integer> failureCodes,
                                 boolean updateConnectionStatus,
                                 boolean pagingEnabled,
                                 Value bodyValue,
                                 String contentType) {
            this.client = client;
            this.path = path;
            this.method = method;
            this.headers = headers;
            this.queryParameters = queryParameters;
            this.failureCodes = failureCodes;
            this.updateConnectionStatus = updateConnectionStatus;
            this.pagingEnabled = pagingEnabled;
            this.contentType = contentType;
            dynamicQueryParameters = queryParameters != null
                && queryParameters
                .entrySet()
                .stream()
                .anyMatch(paramNameAndValues ->
                    paramNameAndValues.getValue() != null
                        && paramNameAndValues.getValue()
                        .stream()
                        .anyMatch(val -> val.contains(DYNAMIC_VALUE_PLACEHOLDER)));

            if (bodyValue != null) {
                if (contentType == null) {
                    this.contentType = bodyValue.getType() == ValueType.OBJECT || bodyValue.getType() == ValueType.ARRAY
                        ? MediaType.APPLICATION_JSON
                        : DEFAULT_CONTENT_TYPE;
                }
                body = bodyValue.toString();
            }

            dynamicBody = !TextUtil.isNullOrEmpty(body)
                && body.contains(DYNAMIC_VALUE_PLACEHOLDER);

            boolean dynamicPath = !TextUtil.isNullOrEmpty(path) && path.contains(DYNAMIC_VALUE_PLACEHOLDER);
            if (!dynamicPath) {
                requestTarget = createRequestTarget(path);
            }
        }

        protected WebTarget createRequestTarget(String path) {
            WebTarget requestTarget = client.path(path == null ? "" : path);

            if (headers != null) {
                requestTarget.register(new HeaderInjectorFilter(headers));
            }

            if (queryParameters != null) {
                requestTarget.register(new QueryParameterInjectorFilter(queryParameters, dynamicQueryParameters ? DYNAMIC_VALUE_PLACEHOLDER_REGEXP : null));
            }

            return requestTarget;
        }

        protected Invocation.Builder getRequestBuilder(String dynamicRequestValue) {
            Invocation.Builder requestBuilder;

            if (requestTarget != null) {
                requestBuilder = requestTarget.request();
            } else {
                // This means that the path is dynamic
                String path = this.path.replaceAll(DYNAMIC_VALUE_PLACEHOLDER_REGEXP, dynamicRequestValue);
                requestBuilder = createRequestTarget(path).request();
            }

            if (dynamicQueryParameters) {
                requestBuilder.property(QueryParameterInjectorFilter.DYNAMIC_VALUE, dynamicRequestValue);
            }

            return requestBuilder;
        }

        protected Invocation buildInvocation(Invocation.Builder requestBuilder, String dynamicRequestValue) {
            Invocation invocation;

            if (body == null) {
                invocation = requestBuilder.build(method);
            } else {
                String body = this.body;

                if (dynamicBody) {
                    body = body.replaceAll(DYNAMIC_VALUE_PLACEHOLDER_REGEXP, dynamicRequestValue);
                }
                if (dynamicQueryParameters) {
                    requestBuilder.property(QueryParameterInjectorFilter.DYNAMIC_VALUE, dynamicRequestValue);
                }
                invocation = requestBuilder.build(method, Entity.entity(body, contentType));
            }

            return invocation;
        }

        public Response invoke(String dynamicRequestValue) {
            Invocation.Builder requestBuilder = getRequestBuilder(dynamicRequestValue);
            Invocation invocation = buildInvocation(requestBuilder, dynamicRequestValue);
            return invocation.invoke();
        }

        protected Future<Response> submit(String dynamicRequestValue) {
            Invocation.Builder requestBuilder = getRequestBuilder(dynamicRequestValue);
            Invocation invocation = buildInvocation(requestBuilder, dynamicRequestValue);
            return invocation.submit();
        }

        @Override
        public String toString() {
            return client.getUri() + (path != null ? "/" + path : "");
        }
    }

    /**
     * Used to
     */
    protected static class PagingResponse extends BuiltResponse {

        private PagingResponse(int status, Headers<Object> metadata, Object entity, Annotation[] entityAnnotations) {
            super(status, metadata, entity, entityAnnotations);
        }

        public static ResponseBuilder fromResponse(Response response) {
            ResponseBuilder b = new PagingResponseBuilder().status(response.getStatus());

            for (String headerName : response.getHeaders().keySet()) {
                List<Object> headerValues = response.getHeaders().get(headerName);
                for (Object headerValue : headerValues) {
                    b.header(headerName, headerValue);
                }
            }
            return b;
        }

        @SuppressWarnings("unchecked")
        @Override
        public <T> T readEntity(Class<T> type) {
            return (T) entity;
        }

        @SuppressWarnings("unchecked")
        @Override
        public <T> T readEntity(Class<T> type, Type genericType, Annotation[] anns) {
            return (T) entity;
        }
    }

    protected static class PagingResponseBuilder extends ResponseBuilderImpl {
        @Override
        public Response build() {
            if (status == -1 && entity == null) status = 204;
            else if (status == -1) status = 200;
            return new PagingResponse(status, metadata, entity, entityAnnotations);
        }
    }

    public static final String PROTOCOL_NAME = PROTOCOL_NAMESPACE + ":httpClient";

    /*--------------- META ITEMS TO BE USED ON PROTOCOL CONFIGURATIONS ---------------*/
    /**
     * Base URI for all requests to this server
     */
    public static final String META_PROTOCOL_BASE_URI = PROTOCOL_NAME + ":baseUri";
    /**
     * Basic authentication username (string)
     */
    public static final String META_PROTOCOL_USERNAME = PROTOCOL_NAME + ":username";
    /**
     * Basic authentication password (string)
     */
    public static final String META_PROTOCOL_PASSWORD = PROTOCOL_NAME + ":password";
    /**
     * OAuth grant ({@link OAuthGrant} stored as {@link ObjectValue})
     */
    public static final String META_PROTOCOL_OAUTH_GRANT = PROTOCOL_NAME + ":oAuthGrant";
    /**
     * Relative path to endpoint that should be used for connection status ping (string)
     */
    public static final String META_PROTOCOL_PING_PATH = PROTOCOL_NAME + ":pingPath";
    /**
     * HTTP method for connection status ping (integer default: {@value #DEFAULT_HTTP_METHOD})
     */
    public static final String META_PROTOCOL_PING_METHOD = PROTOCOL_NAME + ":pingMethod";
    /**
     * HTTP request body for connection status ping ({@link Value})
     */
    public static final String META_PROTOCOL_PING_BODY = PROTOCOL_NAME + ":pingBody";
    /**
     * Used to indicate the type of data sent in the ping body; (see {@link #META_ATTRIBUTE_CONTENT_TYPE} for details)
     */
    public static final String META_PROTOCOL_PING_CONTENT_TYPE = PROTOCOL_NAME + ":pingContentType";
    /**
     * Headers for connection status ping (see {@link #META_HEADERS} for details)
     */
    public static final String META_PROTOCOL_PING_HEADERS = PROTOCOL_NAME + ":pingHeaders";
    /**
     * Query parameters for connection status ping (see {@link #META_QUERY_PARAMETERS} for details)
     */
    public static final String META_PROTOCOL_PING_QUERY_PARAMETERS = PROTOCOL_NAME + ":pingQueryParameters";
    /**
     * Ping frequency in seconds (integer default: {@value #DEFAULT_PING_SECONDS})
     */
    public static final String META_PROTOCOL_PING_SECONDS = PROTOCOL_NAME + ":pingSeconds";
    /**
     * Flag to indicate whether redirect responses from the HTTP server should be followed (boolean)
     */
    public static final String META_PROTOCOL_FOLLOW_REDIRECTS = PROTOCOL_NAME + ":followRedirects";

    /*--------------- META ITEMS TO BE USED ON LINKED ATTRIBUTES ---------------*/
    /**
     * Relative path to endpoint on the server; supports dynamic value insertion, see class javadoc for details
     * (string)
     */
    public static final String META_ATTRIBUTE_PATH = PROTOCOL_NAME + ":path";
    /**
     * HTTP method for request (string default: {@value #DEFAULT_HTTP_METHOD})
     */
    public static final String META_ATTRIBUTE_METHOD = PROTOCOL_NAME + ":method";
    /**
     * HTTP request body; supports dynamic value insertion, see class javadoc for more details ({@link Value})
     */
    public static final String META_ATTRIBUTE_BODY = PROTOCOL_NAME + ":body";
    /**
     * Used to indicate the type of data sent in the body, a default will be used if not supplied; default is determined
     * as follows:
     * <ul>
     * <li>{@link #META_ATTRIBUTE_BODY} value type = {@link ObjectValue} or {@link ArrayValue}: {@link MediaType#APPLICATION_JSON}</li>
     * <li>{@link #META_ATTRIBUTE_BODY} value type = any other value type: {@value #DEFAULT_CONTENT_TYPE}</li>
     * </ul>
     */
    public static final String META_ATTRIBUTE_CONTENT_TYPE = PROTOCOL_NAME + ":contentType";
    /**
     * Polling frequency in seconds for {@link Attribute}s whose value should come from the HTTP server
     */
    public static final String META_ATTRIBUTE_POLLING_SECONDS = PROTOCOL_NAME + ":pollingSeconds";

    /*--------------- META ITEMS TO BE USED ON PROTOCOL CONFIGURATIONS OR LINKED ATTRIBUTES ---------------*/
    /**
     * HTTP response codes that will automatically disable the {@link ProtocolConfiguration} and in the process
     * unlink all linked {@link Attribute}s; the {@link ConnectionStatus} will change to
     * {@link ConnectionStatus#DISABLED} and this will prevent any further requests being sent to the HTTP
     * server (permanent failure - can be reset by re-enabling the {@link ProtocolConfiguration}; failure codes defined
     * on a linked attribute will be combined with values set on the {@link ProtocolConfiguration} for that request
     * (integer array).
     */
    public static final String META_FAILURE_CODES = PROTOCOL_NAME + ":failureCodes";
    /**
     * HTTP headers to be added to requests, {@link ObjectValue} where the keys represent the header name and the value
     * represents the header value(s) (value can be a single string or an {@link ArrayValue} of strings), a value of
     * null will remove all headers by that name; headers defined on a linked attribute will be combined with headers
     * defined on the {@link ProtocolConfiguration} for that request.
     * <p>
     * NOTE: It is possible to remove a header set by the {@link ProtocolConfiguration} for a specific request by using
     * a value of null for the header value on the linked attribute.
     */
    public static final String META_HEADERS = PROTOCOL_NAME + ":headers";

    /**
     * Boolean indicating if paging should occur according to the Link Header specification: https://developer.github.com/v3/guides/traversing-with-pagination/
     */
    public static final String META_PAGING_ENABLED = PROTOCOL_NAME + ":pagingEnabled";

    /**
     * Query parameters for the request; values specified on a {@link ProtocolConfiguration} will be appended to all
     * requests, values specified on linked attributes will be added to those specified on the {@link
     * ProtocolConfiguration}. {@link ObjectValue} where the keys represent the parameter name and the value represents
     * the parameter value(s); supports dynamic value insertion on linked {@link Attribute}s, see class javadoc for more
     * details (value can be a single string or an {@link ArrayValue} of strings).
     */
    public static final String META_QUERY_PARAMETERS = PROTOCOL_NAME + ":queryParameters";

    protected static final List<MetaItemDescriptorImpl> PROTOCOL_META_ITEM_DESCRIPTORS = Arrays.asList(
        new MetaItemDescriptorImpl(
            META_PROTOCOL_BASE_URI,
            ValueType.STRING,
            true,
            REGEXP_PATTERN_BASIC_HTTP_URL,
            MetaItemDescriptor.PatternFailure.HTTP_URL.name(),
            1,
            null,
            false),
        new MetaItemDescriptorImpl(
            META_PROTOCOL_USERNAME,
            ValueType.STRING,
            false,
            REGEXP_PATTERN_STRING_NON_EMPTY_NO_WHITESPACE,
            MetaItemDescriptor.PatternFailure.STRING_EMPTY_OR_CONTAINS_WHITESPACE.name(),
            1,
            null,
            false),
        new MetaItemDescriptorImpl(
            META_PROTOCOL_PASSWORD,
            ValueType.STRING,
            false,
            REGEXP_PATTERN_STRING_NON_EMPTY_NO_WHITESPACE,
            MetaItemDescriptor.PatternFailure.STRING_EMPTY_OR_CONTAINS_WHITESPACE.name(),
            1,
            null,
            false),
        new MetaItemDescriptorImpl(
            META_PROTOCOL_OAUTH_GRANT,
            ValueType.OBJECT,
            false,
            null,
            null,
            1,
            null,
            false),
        new MetaItemDescriptorImpl(
            META_PROTOCOL_PING_PATH,
            ValueType.STRING,
            false,
            REGEXP_PATTERN_STRING_NON_EMPTY,
            MetaItemDescriptor.PatternFailure.STRING_EMPTY.name(),
            1,
            null,
            false),
        new MetaItemDescriptorImpl(
            META_PROTOCOL_PING_METHOD,
            ValueType.STRING,
            false,
            REGEXP_PATTERN_STRING_NON_EMPTY,
            MetaItemDescriptor.PatternFailure.STRING_EMPTY.name(),
            1,
            null,
            false),
        new MetaItemDescriptorImpl(
            META_PROTOCOL_PING_BODY,
            null,
            false,
            null,
            null,
            1,
            null,
            false),
        new MetaItemDescriptorImpl(
            META_PROTOCOL_PING_CONTENT_TYPE,
            ValueType.STRING,
            false,
            REGEXP_PATTERN_STRING_NON_EMPTY,
            MetaItemDescriptor.PatternFailure.STRING_EMPTY.name(),
            1,
            null,
            false),
        new MetaItemDescriptorImpl(
            META_PROTOCOL_PING_SECONDS,
            ValueType.NUMBER,
            false,
            REGEXP_PATTERN_INTEGER_POSITIVE_NON_ZERO,
            MetaItemDescriptor.PatternFailure.INTEGER_POSITIVE_NON_ZERO.name(),
            1,
            null,
            false),
        new MetaItemDescriptorImpl(
            META_PROTOCOL_PING_QUERY_PARAMETERS,
            ValueType.OBJECT,
            false,
            null,
            null,
            1,
            null,
            false),
        new MetaItemDescriptorImpl(
            META_PROTOCOL_PING_HEADERS,
            ValueType.OBJECT,
            false,
            null,
            null,
            1,
            null,
            false),
        new MetaItemDescriptorImpl(
            META_PROTOCOL_FOLLOW_REDIRECTS,
            ValueType.BOOLEAN,
            false,
            null,
            null,
            1,
            null,
            false),
        new MetaItemDescriptorImpl(
            META_QUERY_PARAMETERS,
            ValueType.OBJECT,
            false,
            null,
            null,
            1,
            null,
            false),
        new MetaItemDescriptorImpl(
            META_QUERY_PARAMETERS,
            ValueType.OBJECT,
            false,
            null,
            null,
            1,
            null,
            false),
        new MetaItemDescriptorImpl(
            META_FAILURE_CODES,
            ValueType.ARRAY,
            false,
            null,
            null,
            1,
            null,
            false),
        new MetaItemDescriptorImpl(
            META_PAGING_ENABLED,
            ValueType.BOOLEAN,
            false,
            null,
            null,
            1,
            null,
            false)
    );


    public static final List<MetaItemDescriptor> ATTRIBUTE_META_ITEM_DESCRIPTORS = Arrays.asList(
        new MetaItemDescriptorImpl(
            META_ATTRIBUTE_PATH,
            ValueType.STRING,
            false,
            REGEXP_PATTERN_STRING_NON_EMPTY,
            MetaItemDescriptor.PatternFailure.STRING_EMPTY.name(),
            1,
            null,
            false),
        new MetaItemDescriptorImpl(
            META_ATTRIBUTE_METHOD,
            ValueType.STRING,
            false,
            REGEXP_PATTERN_STRING_NON_EMPTY,
            MetaItemDescriptor.PatternFailure.STRING_EMPTY.name(),
            1,
            null,
            false),
        new MetaItemDescriptorImpl(
            META_ATTRIBUTE_BODY,
            null,
            false,
            null,
            null,
            1,
            null,
            false),
        new MetaItemDescriptorImpl(
            META_ATTRIBUTE_CONTENT_TYPE,
            ValueType.STRING,
            false,
            REGEXP_PATTERN_STRING_NON_EMPTY,
            MetaItemDescriptor.PatternFailure.STRING_EMPTY.name(),
            1,
            null,
            false),
        new MetaItemDescriptorImpl(
            META_ATTRIBUTE_POLLING_SECONDS,
            ValueType.NUMBER,
            false,
            REGEXP_PATTERN_INTEGER_POSITIVE_NON_ZERO,
            MetaItemDescriptor.PatternFailure.INTEGER_POSITIVE_NON_ZERO.name(),
            1,
            null,
            false),
        new MetaItemDescriptorImpl(
            META_QUERY_PARAMETERS,
            ValueType.OBJECT,
            false,
            null,
            null,
            1,
            null,
            false),
        new MetaItemDescriptorImpl(
            META_QUERY_PARAMETERS,
            ValueType.OBJECT,
            false,
            null,
            null,
            1,
            null,
            false),
        new MetaItemDescriptorImpl(
            META_FAILURE_CODES,
            ValueType.ARRAY,
            false,
            null,
            null,
            1,
            null,
            false),
        new MetaItemDescriptorImpl(
            META_PAGING_ENABLED,
            ValueType.BOOLEAN,
            false,
            null,
            null,
            1,
            null,
            false)
    );


    private static final Logger LOG = Logger.getLogger(HttpClientProtocol.class.getName());
    public static final String PROTOCOL_DISPLAY_NAME = "Http Client";
    public static final String PROTOCOL_VERSION = "1.0";
    protected static final String HEADER_LINK = "Link";
    public static final String DYNAMIC_VALUE_PLACEHOLDER = "{$value}";
    protected static final String DYNAMIC_VALUE_PLACEHOLDER_REGEXP = "\"?\\{\\$value}\"?";
    private static TimeUnit POLLING_TIME_UNIT = TimeUnit.SECONDS; // Only here to allow override in tests
    public static final int DEFAULT_PING_SECONDS = 60;
    public static final String DEFAULT_HTTP_METHOD = HttpMethod.GET;
    public static final String DEFAULT_CONTENT_TYPE = MediaType.TEXT_PLAIN;
    protected final Map<AttributeRef, Pair<ResteasyWebTarget, List<Integer>>> clientMap = new HashMap<>();
    protected final Map<AttributeRef, HttpClientRequest> requestMap = new HashMap<>();
    protected final Map<AttributeRef, ScheduledFuture> pollingMap = new HashMap<>();

    @Override
    public void init(Container container) throws Exception {
        super.init(container);
        WebTargetBuilder.setExecutorService(executorService);
    }

    @Override
    protected void doStop(Container container) {
        pollingMap.forEach((attributeRef, scheduledFuture) -> scheduledFuture.cancel(true));
        pollingMap.clear();
        requestMap.clear();
        clientMap.clear();
        WebTargetBuilder.close();
    }

    @Override
    protected void doLinkProtocolConfiguration(AssetAttribute protocolConfiguration) {
        final AttributeRef protocolRef = protocolConfiguration.getReferenceOrThrow();

        if (!protocolConfiguration.isEnabled()) {
            updateStatus(protocolRef, ConnectionStatus.DISABLED);
            return;
        }

        String baseUri = protocolConfiguration.getMetaItem(META_PROTOCOL_BASE_URI)
            .flatMap(AbstractValueHolder::getValueAsString).orElseThrow(() ->
                new IllegalArgumentException("Missing or invalid require meta item: " + META_PROTOCOL_BASE_URI));

        /* We're going to fail hard and fast if optional meta items are incorrectly configured */

        Optional<OAuthGrant> oAuthGrant = getOAuthGrant(protocolConfiguration);
        Optional<Pair<StringValue, StringValue>> usernameAndPassword = getUsernameAndPassword(protocolConfiguration);

        boolean followRedirects = Values.getMetaItemValueOrThrow(
            protocolConfiguration,
            META_PROTOCOL_FOLLOW_REDIRECTS,
            BooleanValue.class,
            false,
            true).map(BooleanValue::getBoolean).orElse(false);

        List<Integer> failureCodes = protocolConfiguration.getMetaItem(META_FAILURE_CODES)
            .flatMap(AbstractValueHolder::getValueAsArray)
            .flatMap(arrayValue ->
                Values.getArrayElements(
                    arrayValue,
                    NumberValue.class,
                    true,
                    false,
                    number -> Values.getIntegerCoerced(number).orElse(null)))
            .orElse(null);

        Optional<MultivaluedMap<String, String>> headers = Values.getMetaItemValueOrThrow(
            protocolConfiguration,
            META_HEADERS,
            ObjectValue.class,
            false,
            true)
            .flatMap(objectValue -> getMultivaluedMap(objectValue, true));

        Optional<MultivaluedMap<String, String>> queryParams = Values.getMetaItemValueOrThrow(
            protocolConfiguration,
            META_QUERY_PARAMETERS,
            ObjectValue.class,
            false,
            true)
            .flatMap(objectValue -> getMultivaluedMap(objectValue, false));

        String pingPath = Values.getMetaItemValueOrThrow(
            protocolConfiguration,
            META_PROTOCOL_PING_PATH,
            StringValue.class,
            false,
            true)
            .map(StringValue::getString)
            .orElse(null);


        WebTargetBuilder webTargetBuilder = new WebTargetBuilder(baseUri);
        if (oAuthGrant.isPresent()) {
            LOG.info("Adding OAuth");
            webTargetBuilder.setOAuthAuthentication(oAuthGrant.get());
        } else {
            usernameAndPassword.ifPresent(userPass -> {
                LOG.info("Adding Basic Authentication");
                webTargetBuilder.setBasicAuthentication(userPass.key.getString(),
                    userPass.value.getString());
            });
        }
        headers.ifPresent(webTargetBuilder::setInjectHeaders);
        queryParams.ifPresent(webTargetBuilder::setInjectQueryParameters);
        webTargetBuilder.followRedirects(followRedirects);

        LOG.fine("Creating web target client '" + baseUri + "'");
        ResteasyWebTarget client = webTargetBuilder.build();

        clientMap.put(protocolRef, new Pair<>(client, failureCodes));
        updateStatus(protocolRef, ConnectionStatus.UNKNOWN);

        if (pingPath == null) {
            return;
        }

        String pingMethod = Values.getMetaItemValueOrThrow(
            protocolConfiguration,
            META_PROTOCOL_PING_METHOD,
            StringValue.class,
            false,
            true)
            .map(StringValue::getString)
            .orElse(DEFAULT_HTTP_METHOD);

        Value pingBody = protocolConfiguration
            .getMetaItem(META_PROTOCOL_PING_BODY)
            .flatMap(AbstractValueHolder::getValue)
            .orElse(null);

        MultivaluedMap<String, String> pingHeaders = Values.getMetaItemValueOrThrow(
            protocolConfiguration,
            META_PROTOCOL_PING_HEADERS,
            ObjectValue.class,
            false,
            true)
            .flatMap(objectValue -> getMultivaluedMap(objectValue, true))
            .orElse(null);

        MultivaluedMap<String, String> pingQueryParams = Values.getMetaItemValueOrThrow(
            protocolConfiguration,
            META_PROTOCOL_PING_QUERY_PARAMETERS,
            ObjectValue.class,
            false,
            true)
            .flatMap(objectValue -> getMultivaluedMap(objectValue, false))
            .orElse(null);

        Integer pingPollingSeconds = getPingSeconds(protocolConfiguration);

        String contentType = Values.getMetaItemValueOrThrow(
            protocolConfiguration,
            META_PROTOCOL_PING_CONTENT_TYPE,
            StringValue.class,
            false,
            true)
            .map(StringValue::getString)
            .orElse(null);

        HttpClientRequest pingRequest = buildClientRequest(
            client,
            pingPath,
            pingMethod,
            pingHeaders,
            pingQueryParams,
            null,
            true,
            false,
            pingBody,
            contentType);

        LOG.info("Creating ping polling request '" + pingRequest + "'");

        requestMap.put(protocolRef, pingRequest);
        pollingMap.put(protocolRef, schedulePollingRequest(
            null,
            protocolRef,
            pingRequest,
            pingPollingSeconds));
    }

    @Override
    protected void doUnlinkProtocolConfiguration(AssetAttribute protocolConfiguration) {
        AttributeRef protocolConfigurationRef = protocolConfiguration.getReferenceOrThrow();
        clientMap.remove(protocolConfigurationRef);
        requestMap.remove(protocolConfigurationRef);
        cancelPolling(protocolConfigurationRef);
    }

    @Override
    protected void doLinkAttribute(AssetAttribute attribute, AssetAttribute protocolConfiguration) {
        String method = Values.getMetaItemValueOrThrow(
            attribute,
            META_ATTRIBUTE_METHOD,
            StringValue.class,
            false,
            true)
            .map(StringValue::getString)
            .orElse(DEFAULT_HTTP_METHOD);

        String path = Values.getMetaItemValueOrThrow(
            attribute,
            META_ATTRIBUTE_PATH,
            StringValue.class,
            false,
            true)
            .map(StringValue::getString)
            .orElse(null);

        String contentType = Values.getMetaItemValueOrThrow(
            attribute,
            META_ATTRIBUTE_CONTENT_TYPE,
            StringValue.class,
            false,
            true)
            .map(StringValue::getString)
            .orElse(null);

        Value body = attribute
            .getMetaItem(META_ATTRIBUTE_BODY)
            .flatMap(AbstractValueHolder::getValue)
            .orElse(null);

        List<Integer> failureCodes = attribute.getMetaItem(META_FAILURE_CODES)
            .flatMap(AbstractValueHolder::getValueAsArray)
            .flatMap(arrayValue ->
                Values.getArrayElements(
                    arrayValue,
                    NumberValue.class,
                    true,
                    false,
                    number -> Values.getIntegerCoerced(number).orElse(null))
            ).orElse(null);

        MultivaluedMap<String, String> headers = Values.getMetaItemValueOrThrow(
            attribute,
            META_HEADERS,
            ObjectValue.class,
            false,
            true)
            .flatMap(objectValue -> getMultivaluedMap(objectValue, true))
            .orElse(null);

        MultivaluedMap<String, String> queryParams = Values.getMetaItemValueOrThrow(
            attribute,
            META_QUERY_PARAMETERS,
            ObjectValue.class,
            false,
            true)
            .flatMap(objectValue -> getMultivaluedMap(objectValue, false))
            .orElse(null);

        Optional<Integer> pollingSeconds = Values.getMetaItemValueOrThrow(
            attribute,
            META_ATTRIBUTE_POLLING_SECONDS,
            NumberValue.class,
            false,
            true)
            .map(polling ->
                Values.getIntegerCoerced(polling)
                    .map(seconds -> seconds < 1 ? null : seconds)
                    .orElseThrow(() ->
                        new IllegalArgumentException("Polling seconds meta item must be an integer >= 1")
                    ));

        Boolean pagingEnabled = Values.getMetaItemValueOrThrow(
            attribute,
            META_PAGING_ENABLED,
            BooleanValue.class,
            false,
            true)
            .map(BooleanValue::getBoolean)
            .orElse(false);

        final AttributeRef attributeRef = attribute.getReferenceOrThrow();

        addHttpClientRequest(protocolConfiguration,
            attributeRef,
            path,
            method,
            headers,
            queryParams,
            failureCodes,
            pagingEnabled,
            body,
            contentType,
            pollingSeconds.orElse(null));
    }

    protected void addHttpClientRequest(AssetAttribute protocolConfiguration,
                                        AttributeRef attributeRef,
                                        String path,
                                        String method,
                                        MultivaluedMap<String, String> headers,
                                        MultivaluedMap<String, String> queryParams,
                                        List<Integer> failureCodes,
                                        boolean pagingEnabled,
                                        Value body,
                                        String contentType,
                                        Integer pollingSeconds) {

        AttributeRef protocolConfigurationRef = protocolConfiguration.getReferenceOrThrow();
        Pair<ResteasyWebTarget, List<Integer>> clientAndFailureCodes = clientMap.get(protocolConfigurationRef);
        ResteasyWebTarget client = clientAndFailureCodes != null ? clientAndFailureCodes.key : null;


        if (client == null) {
            LOG.warning("No client found for protocol configuration: " + protocolConfiguration.getReferenceOrThrow());
            return;
        }

        failureCodes = Optional.ofNullable(failureCodes)
            .map(codes -> {
                if (clientAndFailureCodes.value != null) {
                    codes.addAll(clientAndFailureCodes.value);
                }
                return codes;
            }).orElseGet(() -> {
                if (clientAndFailureCodes.value != null) {
                    return clientAndFailureCodes.value;
                }
                return null;
            });

        boolean updateConnectionStatus = !pollingMap.containsKey(protocolConfigurationRef);

        HttpClientRequest clientRequest = buildClientRequest(
            client,
            path,
            method,
            headers,
            queryParams,
            failureCodes,
            updateConnectionStatus,
            pagingEnabled,
            body,
            contentType);

        LOG.fine("Creating HTTP request for attributeRef '" + clientRequest + "': " + attributeRef);

        requestMap.put(attributeRef, clientRequest);

        Optional.ofNullable(pollingSeconds).ifPresent(seconds -> pollingMap.put(attributeRef, schedulePollingRequest(
            attributeRef,
            protocolConfigurationRef,
            clientRequest,
            seconds)));
    }

    @Override
    protected void doUnlinkAttribute(AssetAttribute attribute, AssetAttribute protocolConfiguration) {
        AttributeRef attributeRef = attribute.getReferenceOrThrow();
        requestMap.remove(attributeRef);
        cancelPolling(attributeRef);
    }

    @Override
    protected void processLinkedAttributeWrite(AttributeEvent event, AssetAttribute protocolConfiguration) {
        AttributeRef protocolRef = protocolConfiguration.getReferenceOrThrow();
        HttpClientRequest request = requestMap.get(event.getAttributeRef());
        Pair<ResteasyWebTarget, List<Integer>> clientAndFailureCodes = clientMap.get(protocolRef);

        // If permanent failure code occurred then protocol configuration could be about to be unlinked (disabled)
        // so the check here catches time between marked as disabled and actually being unlinked
        if (request != null && clientAndFailureCodes != null) {

            executeAttributeWriteRequest(request,
                event.getValue().orElse(null),
                response ->
                    onAttributeWriteResponse(
                        request,
                        response,
                        protocolRef
                    ));
        } else {
            LOG.finest("Ignoring attribute write request as either attribute or protocol configuration is not linked: " + event);
        }
    }

    @Override
    public String getProtocolName() {
        return PROTOCOL_NAME;
    }

    @Override
    public String getProtocolDisplayName() {
        return PROTOCOL_DISPLAY_NAME;
    }

    @Override
    public String getVersion() {
        return PROTOCOL_VERSION;
    }

    @Override
    protected List<MetaItemDescriptor> getProtocolConfigurationMetaItemDescriptors() {
        return new ArrayList<>(PROTOCOL_META_ITEM_DESCRIPTORS);
    }

    @Override
    protected List<MetaItemDescriptor> getLinkedAttributeMetaItemDescriptors() {
        List<MetaItemDescriptor> descriptors = new ArrayList<>(super.getLinkedAttributeMetaItemDescriptors());
        descriptors.addAll(ATTRIBUTE_META_ITEM_DESCRIPTORS);
        return descriptors;
    }

    @Override
    public AssetAttribute getProtocolConfigurationTemplate() {
        return super.getProtocolConfigurationTemplate()
            .addMeta(
                new MetaItem(META_PROTOCOL_BASE_URI, null)
            );
    }

    @Override
    public AttributeValidationResult validateProtocolConfiguration(AssetAttribute protocolConfiguration) {
        AttributeValidationResult result = super.validateProtocolConfiguration(protocolConfiguration);
        if (result.isValid()) {
            try {
                getOAuthGrant(protocolConfiguration);
                getUsernameAndPassword(protocolConfiguration);
                getPingSeconds(protocolConfiguration);
            } catch (IllegalArgumentException e) {
                result.addAttributeFailure(
                    new ValidationFailure(ValueHolder.ValueFailureReason.VALUE_MISMATCH, PROTOCOL_NAME)
                );
            }
        }
        return result;
    }

    public static Optional<OAuthGrant> getOAuthGrant(AssetAttribute attribute) throws IllegalArgumentException {
        return !attribute.hasMetaItem(META_PROTOCOL_OAUTH_GRANT)
            ? Optional.empty()
            : Optional.of(attribute.getMetaItem(META_PROTOCOL_OAUTH_GRANT)
            .flatMap(AbstractValueHolder::getValueAsObject)
            .map(objValue -> {
                String json = objValue.toJson();
                try {
                    return Container.JSON.readValue(json, OAuthGrant.class);
                } catch (IOException e) {
                    throw new IllegalArgumentException("OAuth Grant meta item is not valid", e);
                }
            })
            .orElseThrow(() -> new IllegalArgumentException("OAuth grant meta item must be an ObjectValue")));
    }

    public static Optional<Pair<StringValue, StringValue>> getUsernameAndPassword(AssetAttribute attribute) throws IllegalArgumentException {
        Optional<StringValue> username = Values.getMetaItemValueOrThrow(
            attribute,
            META_PROTOCOL_USERNAME,
            StringValue.class,
            false,
            true);

        Optional<StringValue> password = Values.getMetaItemValueOrThrow(
            attribute,
            META_PROTOCOL_PASSWORD,
            StringValue.class,
            false,
            true);

        if ((username.isPresent() && !password.isPresent()) || (!username.isPresent() && password.isPresent())) {
            throw new IllegalArgumentException("Both username and password must be set for basic authentication");
        }

        return username.map(stringValue -> new Pair<>(stringValue, password.get()));
    }

    public static Integer getPingSeconds(AssetAttribute attribute) throws IllegalArgumentException {
        return Values.getMetaItemValueOrThrow(
            attribute,
            META_PROTOCOL_PING_SECONDS,
            NumberValue.class,
            false,
            true)
            .map(polling ->
                Values.getIntegerCoerced(polling)
                    .map(seconds -> seconds < 1 ? null : seconds)
                    .orElseThrow(() ->
                        new IllegalArgumentException("Ping polling seconds meta item must be an integer >= 1")
                    ))
            .orElse(DEFAULT_PING_SECONDS);
    }


    public static Optional<MultivaluedMap<String, String>> getMultivaluedMap(ObjectValue objectValue, boolean throwOnError) throws ClassCastException, IllegalArgumentException {
        if (objectValue == null) {
            return Optional.empty();
        }

        MultivaluedMap<String, String> multivaluedMap = new MultivaluedHashMap<>();
        objectValue.stream()
            .forEach((keyAndValue) -> {
                if (keyAndValue.value != null) {
                    switch (keyAndValue.value.getType()) {

                        case OBJECT:
                            if (throwOnError) {
                                throw new IllegalArgumentException("Key '" + keyAndValue.key + "' is of unsupported type: ObjectValue");
                            }
                            break;
                        case ARRAY:
                            Values.getArrayElements(((ArrayValue) keyAndValue.value),
                                StringValue.class,
                                throwOnError,
                                false,
                                StringValue::getString)
                                .ifPresent(strs -> multivaluedMap.addAll(keyAndValue.key, strs));
                            break;
                        case STRING:
                        case NUMBER:
                        case BOOLEAN:
                            multivaluedMap.add(keyAndValue.key, keyAndValue.value.toString());
                            break;
                    }
                } else {
                    multivaluedMap.add(keyAndValue.key, null);
                }
            });

        return multivaluedMap.isEmpty() ? Optional.empty() : Optional.of(multivaluedMap);
    }

    protected HttpClientRequest buildClientRequest(WebTarget client, String path, String method, MultivaluedMap<String, String> headers, MultivaluedMap<String, String> queryParams, List<Integer> failureCodes, boolean updateConnectionStatus, boolean pagingEnabled, Value body, String contentType) {
        return new HttpClientRequest(
            client,
            path,
            method,
            headers,
            queryParams,
            failureCodes,
            updateConnectionStatus,
            pagingEnabled,
            body,
            contentType);
    }

    protected ScheduledFuture schedulePollingRequest(AttributeRef attributeRef,
                                                     AttributeRef protocolConfigurationRef,
                                                     HttpClientRequest clientRequest,
                                                     int pollingSeconds) {

        LOG.fine("Scheduling polling request '" + clientRequest + "' to execute every " + pollingSeconds + " seconds for attribute: " + attributeRef);

        return executorService.scheduleWithFixedDelay(() ->
            executePollingRequest(clientRequest, response ->
                onPollingResponse(
                    clientRequest,
                    response,
                    attributeRef,
                    protocolConfigurationRef)
            ), 0, pollingSeconds, POLLING_TIME_UNIT);
    }

    protected void executePollingRequest(HttpClientRequest clientRequest, Consumer<Response> responseConsumer) {
        Response originalResponse = null, lastResponse = null;
        List<String> entities = new ArrayList<>();

        try {
            originalResponse = clientRequest.invoke(null);
            if (clientRequest.pagingEnabled) {
                lastResponse = originalResponse;
                entities.add(lastResponse.readEntity(String.class));
                while ((lastResponse = executePagingRequest(clientRequest, lastResponse)) != null) {
                    entities.add(lastResponse.readEntity(String.class));
                }
                originalResponse = PagingResponse.fromResponse(originalResponse).entity(entities).build();
            }
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Exception thrown whilst doing polling request", e);
        }

        responseConsumer.accept(originalResponse);
    }

    protected Response executePagingRequest(HttpClientRequest clientRequest, Response response) {
        Optional<String> linkHeader = Optional.ofNullable(response.getHeaderString(HEADER_LINK));
        if (linkHeader.isPresent()) {
            Optional<String> nextUrl = getLinkHeaderValue(linkHeader.get(), "next");
            if (nextUrl.isPresent()) {
                return clientRequest.client.register(new PaginationFilter(nextUrl.get())).request().build(clientRequest.method).invoke();
            }
        }
        return null;
    }

    protected void executeAttributeWriteRequest(HttpClientRequest clientRequest,
                                                Value attributeValue,
                                                Consumer<Response> responseConsumer) {
        String valueStr = attributeValue == null ? null : attributeValue.toString();
        Response response = null;

        try {
            response = clientRequest.invoke(valueStr);
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Exception thrown whilst doing attribute write request", e);
        }

        responseConsumer.accept(response);
    }

    protected void onPollingResponse(HttpClientRequest request,
                                     Response response,
                                     AttributeRef attributeRef,
                                     AttributeRef protocolConfigurationRef) {

        int responseCode = response != null ? response.getStatus() : 500;

        if (request.updateConnectionStatus) {
            updateConnectionStatus(request, protocolConfigurationRef, responseCode);
        }

        Value value = null;

        if (response != null && response.hasEntity() && response.getStatusInfo().getFamily() == Response.Status.Family.SUCCESSFUL) {
            try {
                String responseBody = response.readEntity(String.class);
                value = responseBody != null ? Values.create(responseBody) : null;
            } catch (Exception e) {
                LOG.log(Level.SEVERE, "Error occurred whilst trying to read response body", e);
                if (request.updateConnectionStatus) {
                    updateConnectionStatus(request, protocolConfigurationRef, 500);
                }
            }
        } else if (isPermanentFailure(responseCode, request.failureCodes)) {
            doPermanentFailure(protocolConfigurationRef);
            cancelPolling(attributeRef != null ? attributeRef : protocolConfigurationRef);
            return;
        }

        if (attributeRef != null) {
            updateLinkedAttribute(new AttributeState(attributeRef, value));
        }
    }

    protected void onAttributeWriteResponse(HttpClientRequest request,
                                            Response response,
                                            AttributeRef protocolConfigurationRef) {

        int responseCode = response != null ? response.getStatus() : 500;

        if (request.updateConnectionStatus) {
            updateConnectionStatus(request, protocolConfigurationRef, responseCode);
        }

        if (isPermanentFailure(responseCode, request.failureCodes)) {
            doPermanentFailure(protocolConfigurationRef);
        }
    }

    protected void cancelPolling(AttributeRef attributeRef) {
        withLock(getProtocolName() + "::cancelPolling", () -> {
            ScheduledFuture pingPoll = pollingMap.remove(attributeRef);
            if (pingPoll != null) {
                pingPoll.cancel(false);
            }
        });
    }

    protected void updateConnectionStatus(HttpClientRequest request, AttributeRef protocolConfigurationRef, int responseCode) {
        Response.Status status = Response.Status.fromStatusCode(responseCode);
        ConnectionStatus connectionStatus = ConnectionStatus.CONNECTED;
        if (status == null) {
            connectionStatus = ConnectionStatus.UNKNOWN;
        } else {
            switch (status.getFamily()) {
                case INFORMATIONAL:
                    connectionStatus = ConnectionStatus.ERROR;
                    break;
                case SUCCESSFUL:
                    connectionStatus = ConnectionStatus.CONNECTED;
                    break;
                case REDIRECTION:
                    connectionStatus = ConnectionStatus.ERROR;
                    break;
                case CLIENT_ERROR:
                    if (responseCode == 401 || responseCode == 402 || responseCode == 403) {
                        connectionStatus = ConnectionStatus.ERROR_AUTHENTICATION;
                    } else {
                        connectionStatus = ConnectionStatus.ERROR_CONFIGURATION;
                    }
                    break;
                case SERVER_ERROR:
                    connectionStatus = ConnectionStatus.ERROR;
                    break;
                case OTHER:
                    break;
            }
        }

        LOG.fine("Updating connection status based on polling response code: URI=" + request +
            ", ResponseCode=" + responseCode + ", Status=" + connectionStatus);
        updateStatus(protocolConfigurationRef, connectionStatus);
    }

    protected boolean isPermanentFailure(int responseCode, List<Integer> failureCodes) {
        return failureCodes != null && failureCodes.contains(responseCode);
    }

    protected void doPermanentFailure(AttributeRef protocolConfigurationRef) {
        LOG.info("Permanent failure triggered for protocol configuration: " + protocolConfigurationRef);
        LinkedProtocolInfo protocolInfo = linkedProtocolConfigurations.get(protocolConfigurationRef);

        if (protocolInfo == null) {
            LOG.warning("Attempt to mark unlinked protocol configuration as failed");
            return;
        }

        LOG.fine("Updating protocol configuration enabled status to false");
        updateLinkedProtocolConfiguration(
            protocolInfo.getProtocolConfiguration(),
            protocolConfig -> protocolConfig.setDisabled(true)
        );
    }

    protected Optional<String> getLinkHeaderValue(String linkHeader, String rel) {
        Optional<String> nextUrl = Optional.empty();
        String[] parts = linkHeader.split(",");
        Optional<String> relPart = Arrays.stream(parts).filter(p -> p.endsWith("rel=\"" + rel + "\"")).findFirst();
        if (relPart.isPresent()) {
            parts = relPart.get().split(";");
            nextUrl = Optional.of(parts[0].replace("<", "").replace(">", "").trim());
        }
        return nextUrl;
    }
}
