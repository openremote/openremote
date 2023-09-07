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

import com.google.api.Http;
import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import io.undertow.servlet.Servlets;
import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.SecurityConstraint;
import io.undertow.servlet.api.SecurityInfo;
import io.undertow.servlet.api.WebResourceCollection;
import org.apache.camel.component.undertow.HttpHandlerRegistrationInfo;
import org.apache.camel.component.undertow.UndertowConsumer;
import org.apache.camel.component.undertow.UndertowHostKey;
import org.apache.camel.component.undertow.UndertowHostOptions;
import org.openremote.container.web.WebService;
import org.openremote.model.Container;

import java.net.URI;

import static org.openremote.container.web.WebService.pathStartsWithHandler;

/**
 * Customised to use existing undertow instance so websocket doesn't have to be on a separate web server instance
 */
public class UndertowHost implements org.apache.camel.component.undertow.UndertowHost {

    protected final Container container;
    protected final UndertowHostKey key;
    protected final UndertowHostOptions options;
    protected final Undertow undertow;
    protected DeploymentInfo deployment;
    protected WebService.RequestHandler websocketHttpHandler;
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

        String path = registrationInfo.getUri().getPath();
        String deploymentName = "Camel WebSocket Deployment";
        deployment = Servlets.deployment()
            .setDeploymentName(deploymentName)
            .setContextPath(path)
            .setClassLoader(getClass().getClassLoader())
            //httpHandler for servlet is ignored, camel handler is used instead of it
            .addInnerHandlerChainWrapper(h -> handler);

        // Require authentication, but authorize specific roles later in Camel
        WebResourceCollection resourceCollection = new WebResourceCollection();
        resourceCollection.addUrlPattern("/*");
        SecurityConstraint constraint = new SecurityConstraint();
        constraint.setEmptyRoleSemantic(SecurityInfo.EmptyRoleSemantic.PERMIT);
        constraint.addWebResourceCollection(resourceCollection);
        deployment.addSecurityConstraints(constraint);

        HttpHandler deploymentHandler = WebService.addServletDeployment(container, deployment, true);

        websocketHttpHandler = pathStartsWithHandler(deploymentName, path, deploymentHandler);

        // Give web socket handler higher priority than any other handlers already added
        container.getService(WebService.class).getRequestHandlers().add(0, websocketHttpHandler);

        // Caller expects a CamelWebSocketHandler instance
        camelHandler = handler;
        return handler;
    }

    @Override
    public void unregisterHandler(UndertowConsumer consumer, HttpHandlerRegistrationInfo registrationInfo) {
        WebService webService = container.getService(WebService.class);

        if (deployment != null) {
            webService.removeServletDeployment(deployment);
            deployment = null;
        }

        webService.getRequestHandlers().remove(websocketHttpHandler);
        websocketHttpHandler = null;
    }
}
