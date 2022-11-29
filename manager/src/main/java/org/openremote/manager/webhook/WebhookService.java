package org.openremote.manager.webhook;

import org.apache.camel.builder.RouteBuilder;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;
import org.openremote.container.web.WebClient;
import org.openremote.container.web.WebTargetBuilder;
import org.openremote.model.Container;
import org.openremote.model.ContainerService;
import org.openremote.model.http.HTTPMethod;
import org.openremote.model.webhook.Webhook;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import static org.openremote.container.util.MapAccess.getInteger;

public class WebhookService extends RouteBuilder implements ContainerService {

    private static final Logger LOG = Logger.getLogger(WebhookService.class.getName());
    protected ResteasyClientBuilder clientBuilder;
    protected WebTargetBuilder targetBuilder;

    public static final String WEBHOOK_CONNECT_TIMEOUT = "WEBHOOK_CONNECT_TIMEOUT";
    public static final int WEBHOOK_CONNECT_TIMEOUT_DEFAULT = 2000;
    public static final String WEBHOOK_REQUEST_TIMEOUT = "WEBHOOK_REQUEST_TIMEOUT";
    public static final int WEBHOOK_REQUEST_TIMEOUT_DEFAULT = 10000;

    @Override
    public void init(Container container) throws Exception {
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
        // empty
    }

    @Override
    public void start(Container container) throws Exception {
        // empty
    }

    @Override
    public void stop(Container container) throws Exception {
        // empty
    }

    public void sendHttpRequest(Webhook webhook) {
        ResteasyClient client = WebClient.registerDefaults(clientBuilder).build();
        WebTargetBuilder builder = new WebTargetBuilder(client, URI.create(webhook.getUrl()));

        // Authentication
        if(webhook.getUsernamePassword() != null) {
            builder.setBasicAuthentication(webhook.getUsernamePassword().getUsername(), webhook.getUsernamePassword().getPassword());
        } else if(webhook.getOAuthGrant() != null) {
            builder.setOAuthAuthentication(webhook.getOAuthGrant());
        }
        if (webhook.getHeaders() != null && webhook.getHeaders().size() > 0) {
            builder.setInjectHeaders(webhook.getHeaders());
        }

        Response response = null;
        try {
            ResteasyWebTarget target = builder.build();
            response = this.buildResponse(target, webhook.getHttpMethod(), webhook.getPayload());
            response.readEntity(String.class);
        } catch (Exception e) {
            LOG.warning(e.getMessage());
            throw e;
        } finally {
            if (response != null) {
                response.close();
            }
        }
    }

    private Response buildResponse(ResteasyWebTarget target, HTTPMethod method, String payload) {
        Response response = target.request().method(method.name());
        if(payload != null) {
            return target.request().method(method.name(), Entity.entity(payload, response.getMediaType()));
        } else {
            return response;
        }
    }
}
