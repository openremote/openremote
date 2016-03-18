package org.openremote.manager.server.assets;


import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;
import org.openremote.container.Container;
import org.openremote.container.ContainerService;
import org.openremote.container.observable.RetryWithDelay;
import org.openremote.container.web.WebClient;
import org.openremote.container.web.WebService;
import org.openremote.manager.shared.ngsi.EntryPoint;
import rx.Observable;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.Client;
import javax.ws.rs.core.UriBuilder;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import static org.openremote.container.web.WebClient.getTarget;

public class AssetsService implements ContainerService {

    private static final Logger LOG = Logger.getLogger(AssetsService.class.getName());

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

    @Override
    public void init(Container container) throws Exception {
        contextBrokerHostUri =
            UriBuilder.fromPath("/")
                .scheme("http")
                .host(container.getConfig(CONTEXTBROKER_HOST, CONTEXTBROKER_HOST_DEFAULT))
                .port(container.getConfigInteger(CONTEXTBROKER_PORT, CONTEXTBROKER_PORT_DEFAULT));

        LOG.info("Preparing assets service for broker host: " + contextBrokerHostUri.build());

        ResteasyClientBuilder clientBuilder =
            new ResteasyClientBuilder()
                .establishConnectionTimeout(
                    container.getConfigInteger(CONTEXTBROKER_CONNECT_TIMEOUT, CONTEXTBROKER_CONNECT_TIMEOUT_DEFAULT),
                    TimeUnit.MILLISECONDS
                )
                .socketTimeout(
                    container.getConfigInteger(CONTEXTBROKER_REQUEST_TIMEOUT, CONTEXTBROKER_REQUEST_TIMEOUT_DEFAULT),
                    TimeUnit.MILLISECONDS
                )
                .connectionPoolSize(
                    container.getConfigInteger(CONTEXTBROKER_CLIENT_POOL_SIZE, CONTEXTBROKER_CLIENT_POOL_SIZE_DEFAULT)
                );

        this.httpClient = WebClient
            .registerDefaults(container, clientBuilder)
            .register(EntryPointMessageBodyConverter.class)
            .register(EntityMessageBodyConverter.class)
            .register(EntityArrayMessageBodyConverter.class)
            .build();
    }

    @Override
    public void configure(Container container) throws Exception {
        container.getService(WebService.class).getApiSingletons().add(new EntryPointMessageBodyConverter());
        container.getService(WebService.class).getApiSingletons().add(new EntityMessageBodyConverter());
        container.getService(WebService.class).getApiSingletons().add(new EntityArrayMessageBodyConverter());
        container.getService(WebService.class).getApiSingletons().add(new AssetsResourceImpl(this));
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
    }

    public Client getHttpClient() {
        return httpClient;
    }

    public ContextBrokerResource getContextBroker() {
        return getContextBroker(getTarget(httpClient, contextBrokerHostUri.build(), null));
    }

    public ContextBrokerResource getContextBroker(ResteasyWebTarget target) {
        return target.proxy(ContextBrokerResource.class);
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

}
