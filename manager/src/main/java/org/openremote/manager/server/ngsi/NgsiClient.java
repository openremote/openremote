package org.openremote.manager.server.ngsi;

import com.google.common.collect.ImmutableList;
import com.hubrick.vertx.rest.MediaType;
import com.hubrick.vertx.rest.RestClientOptions;
import com.hubrick.vertx.rest.RestClientRequest;
import com.hubrick.vertx.rest.RestClientResponse;
import com.hubrick.vertx.rest.converter.HttpMessageConverter;
import com.hubrick.vertx.rest.converter.StringHttpMessageConverter;
import com.hubrick.vertx.rest.impl.DefaultRestClient;
import com.hubrick.vertx.rest.rx.RxRestClient;
import com.hubrick.vertx.rest.rx.impl.DefaultRxRestClient;
import io.vertx.core.Vertx;
import org.openremote.manager.shared.model.ngsi.Entity;
import org.openremote.manager.shared.model.ngsi.EntryPoint;
import rx.Observable;
import rx.functions.Action1;

import java.util.List;

/**
 * http://telefonicaid.github.io/fiware-orion/api/v2/
 */
public class NgsiClient {

    public static final String BASE_PATH = "/v2";

    final protected List<HttpMessageConverter> messageConverters = ImmutableList.of(
        new StringHttpMessageConverter(),
        new NgsiEntryPointHttpMessageConverter(),
        new NgsiEntityHttpMessageConverter()
    );

    final protected RxRestClient client;

    public NgsiClient(Vertx vertx, RestClientOptions restClientOptions) {
        client = new DefaultRxRestClient(
            new DefaultRestClient(vertx, restClientOptions, messageConverters)
        );
    }

    public Observable<RestClientResponse<EntryPoint>> getEntryPoint(Action1<RestClientRequest> requestBuilder) {
        return client.get(BASE_PATH, EntryPoint.class, requestBuilder);
    }

    public Observable<RestClientResponse<Entity[]>> listEntities(EntryPoint entryPoint,
                                                                 Action1<RestClientRequest> requestBuilder) {
        return client.get(entryPoint.getEntitiesLocation(), Entity[].class, requestBuilder);
    }

    public Observable<RestClientResponse<Void>> createEntity(EntryPoint entryPoint,
                                                             Action1<RestClientRequest> requestBuilder) {
        return client.post(entryPoint.getEntitiesLocation(), restClientRequest -> {
            restClientRequest.setContentType(MediaType.APPLICATION_JSON);
            requestBuilder.call(restClientRequest);
        });
    }
}
