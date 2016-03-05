package org.openremote.manager.server.contextbroker;

/**
 * http://telefonicaid.github.io/fiware-orion/api/v2/
 */
public class NgsiClient { /*implements AutoCloseable {

    private static final Logger LOG = Logger.getLogger(NgsiClient.class.getName());

    public static final String BASE_PATH = "/v2";

    final protected List<HttpMessageConverter> messageConverters = ImmutableList.of(
        new StringHttpMessageConverter(),
        new NgsiEntryPointHttpMessageConverter(),
        new NgsiEntityHttpMessageConverter()
    );

    final protected DefaultRestClient defaultClient;
    final protected RxRestClient client;
    final protected String ngsiServerInfo;
    final protected EntryPoint entryPoint;

    public NgsiClient(Vertx vertx, RestClientOptions restClientOptions) {
        defaultClient = new DefaultRestClient(vertx, restClientOptions, messageConverters);
        client = new DefaultRxRestClient(defaultClient);
        ngsiServerInfo = "(" + restClientOptions.getDefaultHost() + ":" + restClientOptions.getDefaultPort() + ")";

        // TODO Not a great way to block startup while we wait for other services (Hystrix?)
        Observable.create(subscriber -> defaultClient.get(BASE_PATH,
            response -> {
                int statusCode = response.statusCode();
                if (statusCode >= 200 && statusCode < 400) {
                    subscriber.onCompleted();
                } else {
                    subscriber.onError(
                        new IllegalStateException(
                            "Invalid response status " + statusCode + " from NGSI server " + ngsiServerInfo
                        )
                    );
                }
            }
            ).exceptionHandler(subscriber::onError).end()
        ).retryWhen(
            new RetryWithDelay("Connecting to NGSI server " + ngsiServerInfo, 10, 3000)
        ).toBlocking().singleOrDefault(null);

        entryPoint = client.get(BASE_PATH, EntryPoint.class,
            request -> {
                LOG.fine("NGSI server " + ngsiServerInfo + " GET entry point");
                request.end();
            })
            .doOnError(throwable -> {
                throw new NgsiException("Can't access entry point of NGSI server " + ngsiServerInfo);
            })
            .map(RestClientResponse::getBody)
            .toBlocking().first();
    }

    @Override
    public void close() {
        defaultClient.close();
    }

    public EntryPoint getEntryPoint() {
        return entryPoint;
    }

    public Observable<Entity[]> listEntities() {
        return client.get(
            getEntryPoint().getEntitiesLocation(),
            Entity[].class,
            request -> {
                LOG.fine(ngsiServerInfo + " GET entities");
                request.end();
            }
        ).flatMap(response -> Observable.just(response.getBody()));
    }

    public Observable<String> postEntity(Entity entity) {
        return client.post(
            getEntryPoint().getEntitiesLocation(),
            request -> {
                LOG.fine(ngsiServerInfo + " POST entity " + entity.getId() + " of type " + entity.getType());
                request.setContentType(MediaType.APPLICATION_JSON);
                request.end(entity);
            }
        ).flatMap(response -> Observable.just(response.headers().get(HttpHeaders.LOCATION)));
    }

    public Observable<Entity> getEntity(String location) {
        return client.get(
            location,
            Entity.class,
            request -> {
                LOG.fine(ngsiServerInfo + " GET entity at location " + location);
                request.end();
            }
        ).flatMap(response -> Observable.just(response.getBody()));
    }

    public Observable<String> putEntity(Entity entity) {
        return client.put(
            getEntryPoint().getEntitiesLocation() + "/" + entity.getId(),
            Entity.class,
            request -> {
                LOG.fine(ngsiServerInfo + " PUT entity " + entity.getId() + " of type " + entity.getType());
                request.headers().add(NgsiEntityHttpMessageConverter.OPERATION_IS_UPDATE, "true");
                request.setContentType(MediaType.APPLICATION_JSON);
                request.end(entity);
            }
        ).flatMap(response -> Observable.just(getEntryPoint().getEntitiesLocation() + "/" + entity.getId()));
    }

    public Observable<Integer> patchEntity(Entity entity) {
        return client.request(
            PATCH,
            getEntryPoint().getEntitiesLocation() + "/" + entity.getId(),
            Entity.class,
            request -> {
                LOG.fine(ngsiServerInfo + " PATCH entity " + entity.getId() + " of type " + entity.getType());
                request.headers().add(NgsiEntityHttpMessageConverter.OPERATION_IS_UPDATE, "true");
                request.setContentType(MediaType.APPLICATION_JSON);
                request.end(entity);
            }
        ).flatMap(response -> Observable.just(response.statusCode()));
    }

    public Observable<Integer> deleteEntity(Entity entity) {
        return deleteEntity(entity.getId());
    }

    public Observable<Integer> deleteEntity(String entityId) {
        return client.delete(
            getEntryPoint().getEntitiesLocation() + "/" + entityId,
            request -> {
                LOG.fine(ngsiServerInfo + " DELETE entity for id " + entityId);
                request.headers().add(NgsiEntityHttpMessageConverter.OPERATION_IS_UPDATE, "true");
                request.setContentType(MediaType.APPLICATION_JSON);
                request.end();
            }
        ).flatMap(response -> Observable.just(response.statusCode()));
    }
    */
}
