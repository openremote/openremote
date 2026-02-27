/*
 * Copyright 2023, OpenRemote Inc.
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
package org.openremote.manager.event;

import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import io.undertow.servlet.Servlets;
import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.InstanceFactory;
import io.undertow.servlet.api.ServletContainerInitializerInfo;
import io.undertow.servlet.util.ImmediateInstanceFactory;
import jakarta.servlet.ServletContainerInitializer;
import org.apache.camel.component.undertow.HttpHandlerRegistrationInfo;
import org.apache.camel.component.undertow.UndertowConsumer;
import org.apache.camel.component.undertow.UndertowHostKey;
import org.apache.camel.component.undertow.UndertowHostOptions;
import org.openremote.container.security.WebsocketAuthParamHandler;
import org.openremote.container.web.WebService;
import org.openremote.container.web.WebServiceExceptions;
import org.openremote.model.Container;

import java.net.URI;
import java.util.Collections;

/**
 * Customised to use existing undertow instance so websocket doesn't have to be on a separate web server instance
 */
// TODO: Move realm into request URI if possible and remove Realm query parameter support
public class UndertowHost implements org.apache.camel.component.undertow.UndertowHost {

    public static final String DEPLOYMENT_NAME = "Camel WebSocket Deployment";
    protected final Container container;
    protected final UndertowHostKey key;
    protected final UndertowHostOptions options;
    protected final Undertow undertow;
    protected DeploymentInfo deployment;
    protected HttpHandler camelHandler;

    public UndertowHost(Container container, UndertowHostKey key, UndertowHostOptions options) {
        this.container = container;
        this.undertow = container.getService(WebService.class).getUndertow();
        this.key = key;
        this.options = options;
    }

    @Override
    public void validateEndpointURI(URI httpURI) {
        // all URIs are good
    }

    @Override
    public HttpHandler registerHandler(UndertowConsumer consumer, HttpHandlerRegistrationInfo registrationInfo, HttpHandler handler) {

        if (camelHandler != null) {
            return camelHandler;
        }

       WebService webService = container.getService(WebService.class);

        String path = registrationInfo.getUri().getPath();
        deployment = Servlets.deployment()
            .setDeploymentName(DEPLOYMENT_NAME)
            .setContextPath(path)
            .setClassLoader(getClass().getClassLoader())
            // Add handler to extract authorization and realm query params
            .addInitialHandlerChainWrapper(WebsocketAuthParamHandler::new)
            //httpHandler for servlet is ignored, camel handler is used instead of it
            .addInnerHandlerChainWrapper(h -> handler);

        ServletContainerInitializer containerInitializer = (c, ctx) -> {
            webService.configureServlet(ctx, true, null, null);
        };

        InstanceFactory<ServletContainerInitializer> factory = new ImmediateInstanceFactory<>(containerInitializer);
        deployment.addServletContainerInitializer(
            new ServletContainerInitializerInfo(containerInitializer.getClass(), factory, Collections.emptySet())
        );

        // This will catch anything not handled by Resteasy/Servlets, such as IOExceptions "at the wrong time"
        deployment.setExceptionHandler(new WebServiceExceptions.ServletUndertowExceptionHandler(container.isDevMode()));
        webService.deploy(deployment, false);

        // Caller expects a CamelWebSocketHandler instance
        camelHandler = handler;
        return handler;
    }

    @Override
    public void unregisterHandler(UndertowConsumer consumer, HttpHandlerRegistrationInfo registrationInfo) {
        WebService webService = container.getService(WebService.class);

        if (deployment != null) {
            webService.undeploy(DEPLOYMENT_NAME);
            deployment = null;
        }
    }
}
