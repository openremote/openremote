package org.openremote.test;

import com.google.common.collect.ImmutableList;
import com.hubrick.vertx.rest.RestClientOptions;
import com.hubrick.vertx.rest.converter.HttpMessageConverter;
import com.hubrick.vertx.rest.converter.JacksonJsonHttpMessageConverter;
import com.hubrick.vertx.rest.converter.StringHttpMessageConverter;
import com.hubrick.vertx.rest.impl.DefaultRestClient;
import io.vertx.core.json.Json;
import io.vertx.ext.unit.TestContext;

import java.util.List;

public abstract class ManagerClientTest extends IntegrationTest {

    protected String managerHost;
    protected String managerPort;
    protected DefaultRestClient client;

    protected List<HttpMessageConverter> messageConverters = ImmutableList.of(
        new StringHttpMessageConverter(),
        new JacksonJsonHttpMessageConverter<>(Json.mapper)
    );

    @Override
    public void setUp(TestContext context) {
        super.setUp(context);

        managerHost = System.getProperty("docker.manager.host");
        if (managerHost == null)
            throw new IllegalStateException("System property 'docker.manager.host' must be set");
        managerPort = System.getProperty("docker.manager.port");
        if (managerPort == null)
            throw new IllegalStateException("System property 'docker.manager.port' must be set");

        RestClientOptions clientOptions = new RestClientOptions()
            .setConnectTimeout(2000)
            .setGlobalRequestTimeout(2000)
            .setDefaultHost(managerHost)
            .setDefaultPort(Integer.valueOf(managerPort))
            .setKeepAlive(false)
            .setMaxPoolSize(10);

        client = new DefaultRestClient(vertx, clientOptions, messageConverters);
    }

    @Override
    public void tearDown(TestContext testContext) {
        if (client != null) {
            client.close();
        }
        super.tearDown(testContext);
    }

}
