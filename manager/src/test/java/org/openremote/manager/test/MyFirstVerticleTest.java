package org.openremote.manager.test;

import io.vertx.core.Vertx;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.openremote.manager.server.ManagerVerticle;

import java.util.logging.Logger;

@RunWith(VertxUnitRunner.class)
public class MyFirstVerticleTest {

    private static final Logger LOG = Logger.getLogger(MyFirstVerticleTest.class.getName());

    private Vertx vertx;

    @Before
    public void setUp(TestContext context) {
        vertx = Vertx.vertx();

        vertx.deployVerticle(
            ManagerVerticle.class.getName(),
            context.asyncAssertSuccess()
        );
    }

    @After
    public void tearDown(TestContext context) {
        vertx.close(context.asyncAssertSuccess());
    }

    @Test
    public void testMyApplication(TestContext context) {
        final Async async = context.async();

        vertx.createHttpClient().getNow(8080, "localhost", "/hello",
            response -> {
                response.handler(body -> {
                    context.assertTrue(body.toString().contains("Hello"));
                    async.complete();
                });
            });
    }
}