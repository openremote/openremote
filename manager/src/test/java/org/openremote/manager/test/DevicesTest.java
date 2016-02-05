package org.openremote.manager.test;

import com.yahoo.elide.jsonapi.models.JsonApiDocument;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.openremote.manager.server.ServerVerticle;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.ServerSocket;
import java.util.logging.Logger;

import static org.openremote.manager.server.util.JsonUtil.JSON;

@RunWith(VertxUnitRunner.class)
public class DevicesTest {

    private static final Logger LOG = Logger.getLogger(DevicesTest.class.getName());

    private Vertx vertx;
    private int ephemeralPort;

    @Before
    public void setUp(TestContext context) {
        vertx = Vertx.vertx();

        this.ephemeralPort = findEphemeralPort();

        DeploymentOptions options = new DeploymentOptions().setConfig(
            new JsonObject().put(ServerVerticle.WEB_SERVER_PORT, ephemeralPort)
        );

        vertx.deployVerticle(
            ServerVerticle.class.getName(),
            options,
            context.asyncAssertSuccess()
        );
    }

    @After
    public void tearDown(TestContext context) {
        vertx.close(context.asyncAssertSuccess());
    }

    @Test
    public void retrieveDevices(TestContext context) {
        final Async async = context.async();

        vertx.createHttpClient().getNow(ephemeralPort, "localhost", "/device",
            response -> {
                response.handler(body -> {
                    try {
                        JsonApiDocument jsonApiDocument =
                            JSON.readValue(body.toString(), JsonApiDocument.class);

                        context.assertTrue(
                            jsonApiDocument.getData().get().size() == 2
                        );

                    } catch (IOException ex) {
                        context.fail(ex);
                    }
                    async.complete();
                });
            });
    }

    protected Integer findEphemeralPort() {
        try {
            ServerSocket socket = new ServerSocket(0, 0, Inet4Address.getLocalHost());
            int port = socket.getLocalPort();
            socket.close();
            return port;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
}