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

import org.jboss.resteasy.client.jaxrs.BasicAuthentication;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;
import org.jboss.resteasy.specimpl.ResteasyUriBuilder;
import org.openremote.container.json.JacksonConfig;

import javax.ws.rs.Priorities;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * This is a factory for creating JAX-RS {@link javax.ws.rs.client.WebTarget} instances. The instances share a common
 * {@link javax.ws.rs.client.Client} that uses a connection pool and has the following
 * {@link javax.ws.rs.ext.ContextResolver}s registered (additional filters etc. should be registered on the
 * {@link WebTargetBuilder} instances):
 * <ul>
 * <li>{@link org.openremote.container.json.JacksonConfig}.</li>
 * </ul>
 */
// TODO: This should probably be amalgamated with WebClient somehow to provide a unified JAX-RS Client API
public class WebTargetBuilder {

    public static final int CONNECTION_POOL_SIZE = 200;
    public static final long CONNECTION_CHECKOUT_TIMEOUT_MILLISECONDS = 5000;
    public static final long CONNECTION_TIMEOUT_MILLISECONDS = 10000;
    protected static ResteasyClient client;
    protected static ExecutorService asyncExecutorService;
    protected BasicAuthentication basicAuthentication;
    protected OAuthGrant oAuthGrant;
    protected UriBuilder uri;
    protected List<Integer> failureResponses = new ArrayList<>();
    protected MultivaluedMap<String, String> injectHeaders;
    protected MultivaluedMap<String, String> injectQueryParameters;
    protected boolean followRedirects = false;

    public WebTargetBuilder(String uri) {
        this(ResteasyUriBuilder.fromUri(uri));
    }

    public WebTargetBuilder(String uri, long overrideSocketTimeout) {
        this(ResteasyUriBuilder.fromUri(uri), overrideSocketTimeout);
    }

    public WebTargetBuilder(URI uri) {
        this(ResteasyUriBuilder.fromUri(uri));
    }

    public WebTargetBuilder(UriBuilder uri) {
        if (client == null) {
            initClient(null);
        }
        this.uri = uri;
    }

    public WebTargetBuilder(UriBuilder uri, long overrideSocketTimeout) {
        if (client == null) {
            initClient(overrideSocketTimeout);
        }
        this.uri = uri;
    }

    /**
     * Set the executor service to be used for async operations (if none is supplied then one will be created
     * automatically), this allows fine grained thread management at the application level.
     */
    public static void setExecutorService(ExecutorService executorService) {
        if (client != null) {
            throw new IllegalStateException("Executor service must be set before any call to create");
        }

        asyncExecutorService = executorService;
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


    public WebTargetBuilder setInjectHeaders(MultivaluedMap<String, String> injectHeaders) {
        this.injectHeaders = injectHeaders;
        return this;
    }

    public WebTargetBuilder setInjectQueryParameters(MultivaluedMap<String ,String> injectQueryParameters) {
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
        ResteasyWebTarget target = client.target(uri);

        if (!failureResponses.isEmpty()) {
            // Put a filter with max priority in the filter chain
            target.register(new PermanentFailureFilter(failureResponses), 1);
        }

        if (oAuthGrant != null) {
            WebTarget authTarget = client.target(oAuthGrant.tokenEndpointUri);
            OAuthFilter oAuthFilter = new OAuthFilter(authTarget, oAuthGrant);
            target.register(oAuthFilter, Priorities.AUTHENTICATION);
        } else if (basicAuthentication != null) {
            target.register(basicAuthentication, Priorities.AUTHENTICATION);
        }

        if (injectHeaders != null) {
            target.register(new HeaderInjectorFilter(injectHeaders));
        }

        if (injectQueryParameters != null) {
            target.register(new QueryParameterInjectorFilter(injectQueryParameters, null));
        }

        if (followRedirects) {
            target.register(new FollowRedirectFilter());
        }

        return target;
    }

    protected static void initClient(Long overrideSocketTimeout) {
        if (client != null) {
            return;
        }
        ResteasyClientBuilder clientBuilder = new ResteasyClientBuilder()
            .connectionPoolSize(CONNECTION_POOL_SIZE)
            .connectionCheckoutTimeout(CONNECTION_CHECKOUT_TIMEOUT_MILLISECONDS, TimeUnit.MILLISECONDS)
            .socketTimeout(overrideSocketTimeout == null ? CONNECTION_TIMEOUT_MILLISECONDS : overrideSocketTimeout, TimeUnit.MILLISECONDS)
            .establishConnectionTimeout(CONNECTION_TIMEOUT_MILLISECONDS, TimeUnit.MILLISECONDS)
            .register(new JacksonConfig());

        if (asyncExecutorService != null) {
            clientBuilder.asyncExecutor(asyncExecutorService);
        }

        client = clientBuilder.build();
    }

    public static void close() {
        if (client == null) {
            return;
        }

        client.close();
        client = null;
    }
}
