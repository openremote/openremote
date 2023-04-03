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

import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.HttpClientBuilder;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;
import org.jboss.resteasy.client.jaxrs.engines.ApacheHttpClient43Engine;
import org.jboss.resteasy.client.jaxrs.internal.BasicAuthentication;
import org.jboss.resteasy.client.jaxrs.internal.ResteasyClientBuilderImpl;
import org.openremote.container.json.JacksonConfig;
import org.openremote.model.auth.OAuthGrant;

import java.net.URI;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

/**
 * This is a factory for creating JAX-RS {@link WebTarget} instances. The instances share a common
 * {@link jakarta.ws.rs.client.Client} that uses a connection pool and has the following
 * {@link jakarta.ws.rs.ext.ContextResolver}s registered (additional filters etc. should be registered on the
 * {@link WebTargetBuilder} instances):
 * <ul>
 * <li>{@link org.openremote.container.json.JacksonConfig}.</li>
 * </ul>
 */
// TODO: This should probably be amalgamated with WebClient somehow to provide a unified JAX-RS Client API and a default
//  client should be made available on the Container
public class WebTargetBuilder {

    public static final int CONNECTION_POOL_SIZE = 10;
    public static final long CONNECTION_CHECKOUT_TIMEOUT_MILLISECONDS = 5000;
    public static final long CONNECTION_TIMEOUT_MILLISECONDS = 10000;
    protected ResteasyClient client;
    protected static ExecutorService executorService;
    protected BasicAuthentication basicAuthentication;
    protected OAuthGrant oAuthGrant;
    protected URI baseUri;
    protected List<Integer> failureResponses = new ArrayList<>();
    protected Map<String, List<String>> injectHeaders;
    protected Map<String, List<String>> injectQueryParameters;
    protected boolean followRedirects = false;

    public WebTargetBuilder(ResteasyClient client, URI baseUri) {
        this.client = client;
        this.baseUri = baseUri;
    }

    /**
     * Add Basic authentication to requests sent by this {@link WebTarget}; this should not be used in conjunction with
     * any other authentication.
     */
    public WebTargetBuilder setBasicAuthentication(String username, String password) {
        this.basicAuthentication = new BasicAuthentication(username, password);
        return this;
    }

    /**
     * Add OAuth authentication to requests sent by this {@link WebTarget}; this should not be used in conjunction with
     * any other authentication (note if basic authentication is also set then this OAuth authentication will take
     * precedence).
     */
    public WebTargetBuilder setOAuthAuthentication(OAuthGrant oAuthGrant) {
        this.oAuthGrant = oAuthGrant;
        return this;
    }

    public WebTargetBuilder setInjectHeaders(Map<String, List<String>> injectHeaders) {
        this.injectHeaders = injectHeaders;
        return this;
    }

    public WebTargetBuilder setInjectQueryParameters(Map<String, List<String>> injectQueryParameters) {
        this.injectQueryParameters = injectQueryParameters;
        return this;
    }

    /**
     * If the specified status code is returned from the server then it will be treated as a permanent failure
     * and the web authTarget will no longer be usable (any future requests will immediately return a
     * {@link Response.Status#METHOD_NOT_ALLOWED} response without hitting the server.
     * <p>
     * <b>NOTE: Any response in 200 range will always be treated as successful.</b>
     */
    public WebTargetBuilder addPermanentFailureResponse(Response.Status...responseStatus) {
        Collections.addAll(
            failureResponses,
            Arrays.stream(responseStatus)
                .map(Response.Status::getStatusCode)
                .toArray(Integer[]::new)
        );
        return this;
    }

    public WebTargetBuilder addPermanentFailureResponse(Integer...responseStatus) {
        Collections.addAll(failureResponses, responseStatus);
        return this;
    }

    public WebTargetBuilder removePermanentFailureResponse(Response.Status...responseStatus) {
        failureResponses.removeAll(
            Arrays.stream(responseStatus)
                .map(Response.Status::getStatusCode)
                .collect(Collectors.toList())
        );
        return this;
    }

    public WebTargetBuilder removePermanentFailureResponse(Integer...responseStatus) {
        failureResponses.removeAll(Arrays.asList(responseStatus));
        return this;
    }

    public WebTargetBuilder followRedirects(boolean followRedirects) {
        this.followRedirects = followRedirects;
        return this;
    }

    public ResteasyWebTarget build() {
        ResteasyWebTarget target = client.target(baseUri);
        target.register(QueryParameterInjectorFilter.class);

        if (!failureResponses.isEmpty()) {
            // Put a filter with max priority in the filter chain
            target.register(new PermanentFailureFilter(failureResponses), 1);
        }

        if (oAuthGrant != null) {
            OAuthFilter oAuthFilter = new OAuthFilter(client, oAuthGrant);
            target.register(oAuthFilter, Priorities.AUTHENTICATION);
        } else if (basicAuthentication != null) {
            target.register(basicAuthentication, Priorities.AUTHENTICATION);
        }

        if (injectHeaders != null) {
            target.register(new HeaderInjectorFilter(injectHeaders));
        }

        if (injectQueryParameters != null) {
            target.property(QueryParameterInjectorFilter.QUERY_PARAMETERS_PROPERTY, mapToMultivaluedMap(injectQueryParameters, new MultivaluedHashMap<>()));
        }

        if (followRedirects) {
            target.register(new FollowRedirectFilter());
        }

        return target;
    }

    public static ResteasyClient createClient(ExecutorService executorService) {
        return createClient(executorService, CONNECTION_POOL_SIZE, CONNECTION_TIMEOUT_MILLISECONDS, null);
    }

    public static ResteasyClient createClient(ExecutorService executorService, int connectionPoolSize, long overrideSocketTimeout, UnaryOperator<ResteasyClientBuilderImpl> builderConfigurator) {

        //Create all of this config code in order to deal with expires cookies in responses
        RequestConfig requestConfig = RequestConfig.custom()
            .setCookieSpec(CookieSpecs.STANDARD)
            .setConnectionRequestTimeout(Long.valueOf(CONNECTION_CHECKOUT_TIMEOUT_MILLISECONDS).intValue())
            .setConnectTimeout(Long.valueOf(CONNECTION_CHECKOUT_TIMEOUT_MILLISECONDS).intValue())
            .setSocketTimeout(Long.valueOf(overrideSocketTimeout).intValue())
            .build();
        HttpClient apacheClient = HttpClientBuilder.create()
            .setDefaultRequestConfig(requestConfig)
            .build();
        ApacheHttpClient43Engine engine = new ApacheHttpClient43Engine(apacheClient);

        ResteasyClientBuilderImpl clientBuilder = new ResteasyClientBuilderImpl()
            .httpEngine(engine)
            .connectionPoolSize(connectionPoolSize)
            .connectionCheckoutTimeout(CONNECTION_CHECKOUT_TIMEOUT_MILLISECONDS, TimeUnit.MILLISECONDS)
            .readTimeout(overrideSocketTimeout, TimeUnit.MILLISECONDS)
            .connectTimeout(CONNECTION_TIMEOUT_MILLISECONDS, TimeUnit.MILLISECONDS)
            .register(new JacksonConfig());

        if (executorService != null) {
            clientBuilder.executorService(executorService);
        }

        if (builderConfigurator != null) {
            clientBuilder = builderConfigurator.apply(clientBuilder);
        }

        return clientBuilder.build();
    }

    public static <K, V, W extends V> MultivaluedMap<K, V> mapToMultivaluedMap(Map<K, List<W>> map, MultivaluedMap<K, V> multivaluedMap) {
        for (Map.Entry<K, List<W>> e : map.entrySet()) {
            multivaluedMap.put(e.getKey(), e.getValue() == null ? null : new ArrayList<>(e.getValue()));
        }

        return multivaluedMap;
    }
}
