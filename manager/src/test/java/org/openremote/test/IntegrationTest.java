package org.openremote.test;

import io.vertx.core.Vertx;
import io.vertx.core.json.Json;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.openremote.manager.server.util.JsonUtil;

@RunWith(VertxUnitRunner.class)
public abstract class IntegrationTest {

    static {
        // One-time static configuration goes here
        JsonUtil.configure(Json.mapper);
    }

    protected Vertx vertx;

    @Before
    public void setUp(TestContext context) {
        vertx = Vertx.vertx();
    }

    @After
    public void tearDown(TestContext context) {
        vertx.close(context.asyncAssertSuccess());
    }

}
