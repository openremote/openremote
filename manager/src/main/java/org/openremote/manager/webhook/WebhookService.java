/*
 * Copyright 2022, OpenRemote Inc.
 *
 * See the CONTRIBUTORS.txt file in the distribution for a
 * full listing of individual contributors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.openremote.manager.webhook;

import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.camel.builder.RouteBuilder;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.client.jaxrs.internal.ResteasyClientBuilderImpl;
import org.openremote.container.web.WebTargetBuilder;
import org.openremote.model.Container;
import org.openremote.model.ContainerService;
import org.openremote.model.http.HTTPMethod;
import org.openremote.model.webhook.Webhook;

import java.net.URI;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import static org.openremote.container.util.MapAccess.getInteger;

public class WebhookService extends RouteBuilder implements ContainerService {

    private static final Logger LOG = Logger.getLogger(WebhookService.class.getName());
    protected ResteasyClientBuilder clientBuilder;

    public static final String WEBHOOK_CONNECT_TIMEOUT = "WEBHOOK_CONNECT_TIMEOUT";
    public static final int WEBHOOK_CONNECT_TIMEOUT_DEFAULT = 2000;
    public static final String WEBHOOK_REQUEST_TIMEOUT = "WEBHOOK_REQUEST_TIMEOUT";
    public static final int WEBHOOK_REQUEST_TIMEOUT_DEFAULT = 10000;

    @Override
    public void init(Container container) throws Exception {
        this.clientBuilder = new ResteasyClientBuilderImpl()
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


    public boolean sendHttpRequest(Webhook webhook, MediaType mediaType, WebTarget target) {

        try (Response response = this.buildRequest(target, webhook.getHttpMethod(), mediaType, webhook.getPayload())) {
            if (response.getStatusInfo().getFamily() != Response.Status.Family.SUCCESSFUL) {
                LOG.warning("Webhook request responded with error " + response.getStatus() + ": " + response.getStatusInfo().getReasonPhrase());
            } else {
                LOG.info("Webhook request executed successfully with response status " + response.getStatus());
                return true;
            }
        } catch (Exception e) {
            LOG.warning(e.getMessage());
        }
        return false;
    }

    public WebTarget buildWebTarget(Webhook webhook) {

        ResteasyClient client = this.clientBuilder.build();
        WebTargetBuilder builder = new WebTargetBuilder(client, URI.create(webhook.getUrl()));

        // Authentication
        if (webhook.getUsernamePassword() != null) {
            builder.setBasicAuthentication(webhook.getUsernamePassword().getUsername(), webhook.getUsernamePassword().getPassword());
        } else if (webhook.getOAuthGrant() != null) {
            builder.setOAuthAuthentication(webhook.getOAuthGrant());
        }
        if (webhook.getHeaders() != null && !webhook.getHeaders().isEmpty()) {
            builder.setInjectHeaders(webhook.getHeaders());
        }
        return builder.build();
    }

    private Response buildRequest(WebTarget target, HTTPMethod method, MediaType mediaType, String payload) throws ProcessingException {
        return target.request().method(method.name(), (payload != null ? Entity.entity(payload, mediaType) : null));
    }
}
