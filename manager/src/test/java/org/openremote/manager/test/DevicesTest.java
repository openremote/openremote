package org.openremote.manager.test;

import com.yahoo.elide.jsonapi.models.JsonApiDocument;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import org.junit.Test;

import java.io.IOException;
import java.util.logging.Logger;

import static org.openremote.manager.server.util.JsonUtil.JSON;

public class DevicesTest extends ServerTest {

    private static final Logger LOG = Logger.getLogger(DevicesTest.class.getName());

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

}