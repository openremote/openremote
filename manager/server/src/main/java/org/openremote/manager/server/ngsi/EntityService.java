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
package org.openremote.manager.server.ngsi;

import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;
import org.openremote.container.Container;
import org.openremote.container.ContainerService;
import org.openremote.container.observable.RetryWithDelay;
import org.openremote.container.web.WebClient;
import org.openremote.container.web.WebService;
import org.openremote.manager.shared.Consumer;
import org.openremote.manager.shared.ngsi.Attribute;
import org.openremote.manager.shared.ngsi.Entity;
import org.openremote.manager.shared.ngsi.EntryPoint;
import org.openremote.manager.shared.ngsi.params.SubscriptionParams;
import rx.Observable;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.Client;
import javax.ws.rs.core.UriBuilder;
import java.net.URI;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import static org.openremote.container.util.MapAccess.getInteger;
import static org.openremote.container.util.MapAccess.getString;
import static org.openremote.container.web.WebClient.getTarget;

public class EntityService implements ContainerService {

    private static final Logger LOG = Logger.getLogger(EntityService.class.getName());

    public static final String CONTEXTBROKER_HOST = "CONTEXTBROKER_HOST";
    public static final String CONTEXTBROKER_HOST_DEFAULT = "192.168.99.100";
    public static final String CONTEXTBROKER_PORT = "CONTEXTBROKER_PORT";
    public static final int CONTEXTBROKER_PORT_DEFAULT = 8082;
    public static final String CONTEXTBROKER_CONNECT_TIMEOUT = "CONTEXTBROKER_CONNECT_TIMEOUT";
    public static final int CONTEXTBROKER_CONNECT_TIMEOUT_DEFAULT = 2000;
    public static final String CONTEXTBROKER_REQUEST_TIMEOUT = "CONTEXTBROKER_REQUEST_TIMEOUT";
    public static final int CONTEXTBROKER_REQUEST_TIMEOUT_DEFAULT = 10000;
    public static final String CONTEXTBROKER_CLIENT_POOL_SIZE = "CONTEXTBROKER_CLIENT_POOL_SIZE";
    public static final int CONTEXTBROKER_CLIENT_POOL_SIZE_DEFAULT = 20;

    protected UriBuilder contextBrokerHostUri;
    protected Client httpClient;
    protected URI hostUri;
    protected RegistrationProvider registrationProvider;
    protected SubscriptionProvider subscriptionProvider;

    @Override
    public void init(Container container) throws Exception {
        contextBrokerHostUri =
            UriBuilder.fromPath("/")
                .scheme("http")
                .host(getString(container.getConfig(), CONTEXTBROKER_HOST, CONTEXTBROKER_HOST_DEFAULT))
                .port(getInteger(container.getConfig(), CONTEXTBROKER_PORT, CONTEXTBROKER_PORT_DEFAULT));

        LOG.info("Preparing entity service for broker host: " + contextBrokerHostUri.build());

        ResteasyClientBuilder clientBuilder =
            new ResteasyClientBuilder()
                .establishConnectionTimeout(
                    getInteger(container.getConfig(), CONTEXTBROKER_CONNECT_TIMEOUT, CONTEXTBROKER_CONNECT_TIMEOUT_DEFAULT),
                    TimeUnit.MILLISECONDS
                )
                .socketTimeout(
                    getInteger(container.getConfig(), CONTEXTBROKER_REQUEST_TIMEOUT, CONTEXTBROKER_REQUEST_TIMEOUT_DEFAULT),
                    TimeUnit.MILLISECONDS
                )
                .connectionPoolSize(
                    getInteger(container.getConfig(), CONTEXTBROKER_CLIENT_POOL_SIZE, CONTEXTBROKER_CLIENT_POOL_SIZE_DEFAULT)
                );

        this.httpClient = WebClient
            .registerDefaults(container, clientBuilder)
            .register(EntryPointMessageBodyConverter.class)
            .register(EntityMessageBodyConverter.class)
            .register(EntityArrayMessageBodyConverter.class)
            .build();

        hostUri = container.getService(WebService.class).getHostUri();
    }

    @Override
    public void configure(Container container) throws Exception {
        EntityResourceImpl entityResource = new EntityResourceImpl(this);

        container.getService(WebService.class).getApiSingletons().add(new EntryPointMessageBodyConverter());
        container.getService(WebService.class).getApiSingletons().add(new EntityMessageBodyConverter());
        container.getService(WebService.class).getApiSingletons().add(new EntityArrayMessageBodyConverter());
        container.getService(WebService.class).getApiSingletons().add(entityResource);

        subscriptionProvider = entityResource;
        registrationProvider = new ContextBrokerV1ResourceImpl(httpClient, contextBrokerHostUri);

        // Configure the providers
        subscriptionProvider.configure(container);
        registrationProvider.configure(container);
    }

    @Override
    public void start(Container container) throws Exception {
        // TODO Not a great way to block startup while we wait for other services (Hystrix?)
        pingContextBroker();
        LOG.info("Context broker provider available: " + contextBrokerHostUri.build());
    }

    @Override
    public void stop(Container container) throws Exception {
        if (httpClient != null)
            httpClient.close();

        if (registrationProvider != null)
            registrationProvider.stop();
    }

    public Client getHttpClient() {
        return httpClient;
    }

    public RegistrationProvider getRegistrationProvider() { return registrationProvider; }

    public SubscriptionProvider getSubscriptionProvider() { return subscriptionProvider; }

    public ContextBrokerV2Resource getContextBroker() {
        return getContextBroker(getTarget(httpClient, contextBrokerHostUri.build()));
    }

    public ContextBrokerV2Resource getContextBroker(ResteasyWebTarget target) {
        return target.proxy(ContextBrokerV2Resource.class);
    }

    public ContextBrokerV1Resource getContextBrokerV1() {
        return getContextBrokerV1(getTarget(httpClient, contextBrokerHostUri.build()));
    }

    public ContextBrokerV1Resource getContextBrokerV1(ResteasyWebTarget target) {
        return target.proxy(ContextBrokerV1Resource.class);
    }

    public void pingContextBroker() {
        Observable.fromCallable(() -> {
            EntryPoint response = getContextBroker().getEntryPoint();
            if (response != null)
                return true;
            throw new WebApplicationException("Context broker not available");
        }).retryWhen(
            new RetryWithDelay("Connecting to context broker: " + contextBrokerHostUri.build(), 10, 3000)
        ).toBlocking().singleOrDefault(false);
    }

    public boolean registerEntityProvider(String entityType, String entityId, List<Attribute> attributes, EntityProvider provider) {
        return registrationProvider.registerEntityProvider(entityType, entityId, attributes, provider);
    }

    public void unregisterEntityProvider(String entityType, String entityId, EntityProvider provider) {
        registrationProvider.unregisterEntityProvider(entityType, entityId, provider);
    }

    public void unregisterEntityProvider(EntityProvider provider) {
        registrationProvider.unregisterEntityProvider(provider);
    }

    public boolean registerEntityListener(Consumer<Entity[]> listener, SubscriptionParams subscription) {
        return subscriptionProvider.registerSubscriber(listener, subscription);
    }

    public SubscriptionParams getEntityListerSubscriptionParams(Consumer<Entity[]> listener) {
        return subscriptionProvider.getSubscription(listener);
    }

    public void unregisterEntityListener(Consumer<Entity[]> listener) {
        subscriptionProvider.unregisterSubscriber(listener);
    }
}
