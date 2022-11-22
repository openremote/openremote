package org.openremote.manager.webhook;

import org.apache.camel.builder.RouteBuilder;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;
import org.openremote.container.timer.TimerService;
import org.openremote.container.web.WebClient;
import org.openremote.container.web.WebTargetBuilder;
import org.openremote.model.Container;
import org.openremote.model.ContainerService;
import org.openremote.model.auth.OAuthClientCredentialsGrant;
import org.openremote.model.webhook.Webhook;

import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import static org.openremote.container.util.MapAccess.getInteger;

public class WebhookService extends RouteBuilder implements ContainerService {

    private static final Logger LOG = Logger.getLogger(WebhookService.class.getName());
    protected TimerService timerService;
    protected ResteasyClientBuilder clientBuilder;
    protected WebTargetBuilder targetBuilder;

    public static final String WEBHOOK_CONNECT_TIMEOUT = "WEBHOOK_CONNECT_TIMEOUT";
    public static final int WEBHOOK_CONNECT_TIMEOUT_DEFAULT = 2000;
    public static final String WEBHOOK_REQUEST_TIMEOUT = "WEBHOOK_REQUEST_TIMEOUT";
    public static final int WEBHOOK_REQUEST_TIMEOUT_DEFAULT = 10000;

    @Override
    public void init(Container container) throws Exception {
        this.timerService = container.getService(TimerService.class);
        this.clientBuilder = new ResteasyClientBuilder()
                .connectTimeout(
                        getInteger(container.getConfig(), WEBHOOK_CONNECT_TIMEOUT, WEBHOOK_CONNECT_TIMEOUT_DEFAULT),
                        TimeUnit.MILLISECONDS
                )
                .readTimeout(
                        getInteger(container.getConfig(), WEBHOOK_REQUEST_TIMEOUT, WEBHOOK_REQUEST_TIMEOUT_DEFAULT),
                        TimeUnit.MILLISECONDS
                );
    }

    @Override
    public int getPriority() {
        return ContainerService.DEFAULT_PRIORITY;
    }

    @Override
    public void configure() throws Exception {
        LOG.warning("Configuring the WebhookService");
        // empty
    }

    @Override
    public void start(Container container) throws Exception {
        LOG.warning("Starting the WebhookService..");
        // empty
    }

    @Override
    public void stop(Container container) throws Exception {
        LOG.warning("Stopping the WebhookService..");
        // empty
    }

    public void sendHttpRequest(Webhook webhook) {
        LOG.info("Sending HTTP Request for webhook...");
        ResteasyClient client = WebClient.registerDefaults(clientBuilder).build();
        WebTargetBuilder builder = new WebTargetBuilder(client, URI.create(webhook.getUrl()));
        if(webhook.getAuthMethod() != null) {
            switch (webhook.getAuthMethod()) {
                case OAUTH2 -> {
                    Webhook.AuthDetails details = webhook.getAuthDetails();
                    builder.setOAuthAuthentication(new OAuthClientCredentialsGrant(details.url, details.clientId, details.clientSecret, null));
                }
                case HTTP_BASIC -> {
                    builder.setBasicAuthentication(webhook.getAuthDetails().username, webhook.getAuthDetails().password);
                }
            }
        }
        if(webhook.getHeaders() != null && webhook.getHeaders().length > 0) {
            Map<String, List<String>> headers = new HashMap<>();
            for(Webhook.Header header : webhook.getHeaders()) {
                headers.put(header.header, Collections.singletonList(header.value));
            }
            builder.setInjectHeaders(headers);
        }
        Response response = null;
        try {
            ResteasyWebTarget target = builder.build();
            String method = webhook.getMethod().name();
            response = target.request().method(method);
            String entity = response.readEntity(String.class);
            LOG.warning(entity);
        } catch (Exception e) {
            throw e;
        } finally {
            if(response != null) {
                response.close();
            }
            LOG.warning("Done with HTTP request!");
        }
    }
}
