package org.openremote.manager.test;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;

@RunWith(VertxUnitRunner.class)
public abstract class IntegrationTest {

    protected Vertx vertx;
    protected HttpClient httpClient;

    @Before
    public void setUp(TestContext context) {
        vertx = Vertx.vertx();
        httpClient = vertx.createHttpClient();
    }

    @After
    public void tearDown(TestContext context) {
        httpClient.close();
        vertx.close(context.asyncAssertSuccess());
    }

}
