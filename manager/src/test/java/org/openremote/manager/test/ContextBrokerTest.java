package org.openremote.manager.test;

import com.hubrick.vertx.rest.RestClientOptions;
import elemental.json.Json;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import org.junit.Ignore;
import org.junit.Test;
import org.openremote.manager.server.ngsi.NgsiClient;
import org.openremote.manager.shared.model.ngsi.Attribute;
import org.openremote.manager.shared.model.ngsi.Entity;
import rx.Observable;

import java.util.List;
import java.util.logging.Logger;

import static org.junit.Assert.assertNotNull;

@Ignore // TODO
public class ContextBrokerTest extends ServerTest {

    private static final Logger LOG = Logger.getLogger(ContextBrokerTest.class.getName());

    protected NgsiClient ngsiClient;

    @Override
    public void setUp(TestContext testContext) {
        super.setUp(testContext);

        // TODO
        String host = "192.168.99.100";
        int port = 1026;

        RestClientOptions clientOptions = new RestClientOptions()
                .setConnectTimeout(2000)
                .setGlobalRequestTimeout(2000)
                .setDefaultHost(host)
                .setDefaultPort(port)
                .setKeepAlive(false)
                .setMaxPoolSize(10);

        ngsiClient = new NgsiClient(vertx, clientOptions);
        deleteAllEntities(testContext);
    }

    @Override
    public void tearDown(TestContext testContext) {
        if (ngsiClient != null) {
            ngsiClient.close();
        }
        super.tearDown(testContext);
    }

    @Test
    public void checkEntryPoint(TestContext testContext) {
        assertNotNull(ngsiClient.getEntryPoint().getEntitiesLocation());
        assertNotNull(ngsiClient.getEntryPoint().getTypesLocation());
        assertNotNull(ngsiClient.getEntryPoint().getRegistrationsLocation());
        assertNotNull(ngsiClient.getEntryPoint().getSubscriptionsLocation());
        testContext.async().complete();
    }

    @Test
    public void createUpdateDeleteEntity(TestContext testContext) {
        final Async async = testContext.async();

        Entity room = new Entity(Json.createObject());
        room.setId("Room123");
        room.setType("Room");
        room.addAttribute(
                new Attribute("temperature", Json.createObject())
                        .setType("float")
                        .setValue(Json.create(21.3))
        );

        ngsiClient.postEntity(room)
                .flatMap(location -> ngsiClient.getEntity(location))
                .map(entity -> {
                    Attribute temperature = entity.getAttribute("temperature");
                    testContext.assertEquals(21.3, temperature.getValue().asNumber());
                    temperature.setValue(Json.create(22.5));
                    return entity;
                })
                .flatMap(entity -> ngsiClient.putEntity(entity))
                .flatMap(updateLocation -> ngsiClient.getEntity(updateLocation))
                .finallyDo(async::complete)
                .doOnError(testContext::fail)
                .subscribe(resultEntity -> {
                    Attribute temperature = resultEntity.getAttribute("temperature");
                    testContext.assertEquals(22.5, temperature.getValue().asNumber());
                });
    }

    protected void deleteAllEntities(TestContext testContext) {
        List<Integer> statusCodes = ngsiClient.listEntities()
                .flatMap(Observable::from)
                .flatMap(entity -> ngsiClient.deleteEntity(entity))
                .doOnError(testContext::fail)
                .toList().toBlocking().single();
        for (Integer statusCode : statusCodes) {
            testContext.assertEquals(statusCode, 204);
        }
    }
}