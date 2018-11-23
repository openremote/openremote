/*
 * Copyright 2016, OpenRemote Inc.
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
package org.openremote.container.web;

import io.undertow.server.DefaultByteBufferPool;
import io.undertow.server.HttpHandler;
import io.undertow.servlet.api.*;
import io.undertow.servlet.util.ImmediateInstanceHandle;
import io.undertow.websockets.jsr.DefaultContainerConfigurator;
import io.undertow.websockets.jsr.UndertowContainerProvider;
import io.undertow.websockets.jsr.WebSocketDeploymentInfo;
import org.keycloak.KeycloakPrincipal;
import org.openremote.container.message.MessageBrokerSetupService;
import org.openremote.container.security.AuthContext;
import org.openremote.container.security.basic.BasicAuthContext;
import org.openremote.container.security.keycloak.AccessTokenAuthContext;
import org.openremote.container.security.IdentityService;
import org.openremote.container.web.socket.WebsocketAdapter;
import org.openremote.container.web.socket.WebsocketCORSFilter;
import org.openremote.container.web.socket.WebsocketComponent;
import org.openremote.container.web.socket.WebsocketConstants;
import org.xnio.OptionMap;
import org.xnio.Options;
import org.xnio.Xnio;

import javax.servlet.DispatcherType;
import javax.servlet.Filter;
import javax.websocket.HandshakeResponse;
import javax.websocket.server.HandshakeRequest;
import javax.websocket.server.ServerEndpointConfig;
import javax.ws.rs.WebApplicationException;
import java.security.Principal;
import java.util.logging.Logger;

import static javax.ws.rs.core.Response.Status.FORBIDDEN;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static org.openremote.container.web.WebService.pathStartsWithHandler;

public class DefaultWebsocketComponent extends WebsocketComponent {

    private static final Logger LOG = Logger.getLogger(DefaultWebsocketComponent.class.getName());

    static {
        UndertowContainerProvider.disableDefaultContainer();
    }

    final protected IdentityService identityService;
    final protected WebService webService;
    final protected String allowedOrigin;
    protected DeploymentInfo deploymentInfo;
    protected WebService.RequestHandler websocketHttpHandler;

    public DefaultWebsocketComponent(IdentityService identityService, WebService webService, String allowedOrigin) {
        this.identityService = identityService;
        this.webService = webService;
        this.allowedOrigin = allowedOrigin;
    }

    @Override
    protected void deploy() throws Exception {

        WebSocketDeploymentInfo webSocketDeploymentInfo = new WebSocketDeploymentInfo();

        getConsumers().entrySet().forEach(consumerEntry -> {
            String endpointPath = MessageBrokerSetupService.WEBSOCKET_PATH + "/" + consumerEntry.getKey();
            LOG.info("Deploying websocket endpoint: " + endpointPath);
            webSocketDeploymentInfo.addEndpoint(
                ServerEndpointConfig.Builder.create(WebsocketAdapter.class, endpointPath)
                    .configurator(new DefaultContainerConfigurator() {
                        @SuppressWarnings("unchecked")
                        @Override
                        public <T> T getEndpointInstance(Class<T> endpointClass) throws InstantiationException {
                            return (T) new WebsocketAdapter(consumerEntry.getValue());
                        }

                        @SuppressWarnings("unchecked")
                        @Override
                        public void modifyHandshake(ServerEndpointConfig config, HandshakeRequest request, HandshakeResponse response) {

                            Principal principal = request.getUserPrincipal();
                            if (principal == null) {
                                throw new WebApplicationException("Request is not authenticated, can't access user principal", FORBIDDEN);
                            }

                            AuthContext authContext;

                            if (principal instanceof KeycloakPrincipal) {
                                KeycloakPrincipal keycloakPrincipal = (KeycloakPrincipal) principal;
                                authContext = new AccessTokenAuthContext(
                                    keycloakPrincipal.getKeycloakSecurityContext().getRealm(),
                                    keycloakPrincipal.getKeycloakSecurityContext().getToken()
                                );
                            } else if (principal instanceof BasicAuthContext) {
                                authContext = (BasicAuthContext) principal;
                            } else {
                                throw new WebApplicationException("Unsupported user principal type: " + principal, INTERNAL_SERVER_ERROR);
                            }

                            config.getUserProperties().put(WebsocketConstants.HANDSHAKE_AUTH, authContext);

                            super.modifyHandshake(config, request, response);
                        }
                    })
                    .build()
            );
        });

        // We use the I/O thread to handle received websocket frames, as we expect to quickly hand them over to
        // an internal asynchronous message queue for processing, so we don't need a separate worker thread
        // pool for websocket frame processing
        webSocketDeploymentInfo.setDispatchToWorkerThread(false);

        // Make the shit Undertow/Websocket JSR client bootstrap happy - this is the pool that would be used
        // when Undertow acts as a WebSocket client, which we don't do... and I'm not even sure it can do that...
        webSocketDeploymentInfo.setWorker(Xnio.getInstance().createWorker(
            OptionMap.builder()
                .set(Options.WORKER_TASK_MAX_THREADS, 1)
                .set(Options.WORKER_NAME, "WebsocketInternalClient")
                .set(Options.THREAD_DAEMON, true)
                .getMap()
        ));
        boolean directBuffers = Boolean.getBoolean("io.undertow.websockets.direct-buffers");
        webSocketDeploymentInfo.setBuffers(new DefaultByteBufferPool(directBuffers, 1024, 100, 12));

        String deploymentName = "WebSocket Deployment";
        deploymentInfo = new DeploymentInfo()
            .setDeploymentName(deploymentName)
            .setContextPath(MessageBrokerSetupService.WEBSOCKET_PATH)
            .addServletContextAttribute(WebSocketDeploymentInfo.ATTRIBUTE_NAME, webSocketDeploymentInfo)
            .setClassLoader(WebsocketComponent.class.getClassLoader());

        WebResourceCollection resourceCollection = new WebResourceCollection();
        resourceCollection.addUrlPattern("/*");
        // Require authentication, but authorize specific roles later in Camel
        SecurityConstraint constraint = new SecurityConstraint();
        constraint.setEmptyRoleSemantic(SecurityInfo.EmptyRoleSemantic.AUTHENTICATE);
        constraint.addWebResourceCollection(resourceCollection);
        deploymentInfo.addSecurityConstraints(constraint);

        HttpHandler handler = webService.addServletDeployment(identityService, deploymentInfo, true);
        websocketHttpHandler = pathStartsWithHandler(deploymentName, MessageBrokerSetupService.WEBSOCKET_PATH, handler);

        // Give web socket handler higher priority than any other handlers already added
        webService.getRequestHandlers().add(0, websocketHttpHandler);
    }

    @Override
    protected void undeploy() throws Exception {
        if (deploymentInfo != null) {
            webService.removeServletDeployment(deploymentInfo);
            deploymentInfo = null;
        }
        webService.getRequestHandlers().remove(websocketHttpHandler);
        websocketHttpHandler = null;
    }

    /* TODO: Security - fix/use this! */
    protected void addCORSFilter() {
        if (allowedOrigin == null)
            return;
        WebsocketCORSFilter websocketCORSFilter = new WebsocketCORSFilter();
        FilterInfo filterInfo = new FilterInfo(
            "WebSocket CORS Filter",
            WebsocketCORSFilter.class,
            () -> new ImmediateInstanceHandle<Filter>(websocketCORSFilter)
        ).addInitParam(WebsocketCORSFilter.ALLOWED_ORIGIN, allowedOrigin);
        deploymentInfo.addFilter(filterInfo);
        deploymentInfo.addFilterUrlMapping(filterInfo.getName(), "/*", DispatcherType.REQUEST);
    }

}
