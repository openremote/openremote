package org.openremote.manager.test;

import com.hubrick.vertx.rest.RestClientOptions;
import com.hubrick.vertx.rest.RestClientRequest;
import elemental.json.Json;
import io.vertx.core.http.HttpHeaders;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.openremote.manager.server.ngsi.NgsiClient;
import org.openremote.manager.shared.model.ngsi.Attribute;
import org.openremote.manager.shared.model.ngsi.Entity;

import java.util.Arrays;
import java.util.logging.Logger;

@Ignore // TODO
public class ContextBrokerTest extends IntegrationTest {

    private static final Logger LOG = Logger.getLogger(ContextBrokerTest.class.getName());

    protected NgsiClient ngsiClient;

    @Before
    public void setup() {
        // TODO
        String host = "192.168.99.100";
        int port = 1026;

        RestClientOptions clientOptions = new RestClientOptions()
            .setConnectTimeout(2000)
            .setGlobalRequestTimeout(2000)
            .setDefaultHost(host)
            .setDefaultPort(port)
            .setKeepAlive(true)
            .setMaxPoolSize(10);

        ngsiClient = new NgsiClient(vertx, clientOptions);
    }

    @Test
    public void getEntryPoint(TestContext testContext) {

        final Async async = testContext.async();

        ngsiClient.getEntryPoint(RestClientRequest::end)
            .finallyDo(async::complete)
            .doOnError(testContext::fail)
            .subscribe(
                response -> {
                    testContext.assertNotNull(response.getBody().getEntitiesLocation());
                    testContext.assertNotNull(response.getBody().getTypesLocation());
                    testContext.assertNotNull(response.getBody().getRegistrationsLocation());
                    testContext.assertNotNull(response.getBody().getSubscriptionsLocation());
                }
            );
    }

    @Test
    public void listEntities(TestContext testContext) {

        final Async async = testContext.async();

        ngsiClient.getEntryPoint(RestClientRequest::end)
            .flatMap(response -> ngsiClient.listEntities(response.getBody(), RestClientRequest::end))
            .finallyDo(async::complete)
            .doOnError(testContext::fail)
            .subscribe(
                response -> {
                    Entity[] entities = response.getBody();
                    LOG.info("### GOT: " + Arrays.toString(entities));
                }
            );
    }

    @Test
    public void createEntity(TestContext testContext) {

        final Async async = testContext.async();

        Entity room = new Entity(Json.createObject());
        room.setId("Room123");
        room.setType("Room");
        room.addAttribute(
            new Attribute("temperature", Json.createObject())
                .setType("float")
                .setValue(Json.create(21.3))
        );

        ngsiClient.getEntryPoint(RestClientRequest::end)
            .flatMap(response -> ngsiClient.createEntity(response.getBody(), request -> request.end(room)))
            .finallyDo(async::complete)
            .doOnError(testContext::fail)
            .subscribe(
                response -> {
                    String location = response.headers().get(HttpHeaders.LOCATION);
                    LOG.info("### GOT: " + location);
                }
            );
    }

}