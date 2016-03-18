/*
 * Copyright 2015, OpenRemote Inc.
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

import io.undertow.server.HttpHandler;
import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.FilterInfo;
import io.undertow.servlet.api.InstanceHandle;
import io.undertow.websockets.jsr.DefaultContainerConfigurator;
import io.undertow.websockets.jsr.WebSocketDeploymentInfo;
import org.openremote.container.message.MessageBrokerService;
import org.openremote.container.web.socket.WebsocketAdapter;
import org.openremote.container.web.socket.WebsocketCORSFilter;
import org.openremote.container.web.socket.WebsocketComponent;
import org.openremote.container.web.socket.WebsocketConsumer;

import javax.servlet.DispatcherType;
import javax.servlet.Filter;
import javax.websocket.server.ServerEndpointConfig;
import java.util.Map;
import java.util.logging.Logger;

public class DefaultWebsocketComponent extends WebsocketComponent {

    private static final Logger LOG = Logger.getLogger(DefaultWebsocketComponent.class.getName());

    final protected WebService webService;
    final protected String allowedOrigin;
    protected DeploymentInfo deploymentInfo;

    public DefaultWebsocketComponent(WebService webService, String allowedOrigin) {
        this.webService = webService;
        this.allowedOrigin = allowedOrigin;
    }

    @Override
    protected void deploy() throws Exception {

        WebSocketDeploymentInfo webSocketDeploymentInfo = new WebSocketDeploymentInfo();

        getConsumers().entrySet().stream().forEach(consumerEntry -> {
            String endpointPath = MessageBrokerService.WEBSOCKET_PATH + "/" + consumerEntry.getKey();
            LOG.info("Deploying websocket endpoint: " + endpointPath);
            webSocketDeploymentInfo.addEndpoint(
                ServerEndpointConfig.Builder.create(WebsocketAdapter.class, endpointPath)
                    .configurator(new DefaultContainerConfigurator() {
                        @SuppressWarnings("unchecked")
                        @Override
                        public <T> T getEndpointInstance(Class<T> endpointClass) throws InstantiationException {
                            return (T) new WebsocketAdapter(consumerEntry.getValue());
                        }
                    })
                    .build()
            );
        });

        deploymentInfo = new DeploymentInfo()
            .setDeploymentName("WebSocket Deployment")
            .setContextPath(MessageBrokerService.WEBSOCKET_PATH)
            .addServletContextAttribute(WebSocketDeploymentInfo.ATTRIBUTE_NAME, webSocketDeploymentInfo)
            .setClassLoader(WebsocketComponent.class.getClassLoader());

        HttpHandler handler = webService.addServletDeployment(deploymentInfo);

        webService.getPrefixRoutes().put(
            MessageBrokerService.WEBSOCKET_PATH, handler
        );
    }

    @Override
    protected void undeploy() throws Exception {
        if (deploymentInfo != null) {
            webService.removeServletDeployment(deploymentInfo);
            deploymentInfo = null;
        }
        webService.getPrefixRoutes().remove(MessageBrokerService.WEBSOCKET_PATH);
    }

    protected void addCORSFilter() {
        if (allowedOrigin == null)
            return;
        WebsocketCORSFilter websocketCORSFilter = new WebsocketCORSFilter();
        FilterInfo filterInfo = new FilterInfo("WebSocket CORS Filter", WebsocketCORSFilter.class, () -> new InstanceHandle<Filter>() {
            @Override
            public Filter getInstance() {
                return websocketCORSFilter;
            }

            @Override
            public void release() {
            }
        }).addInitParam(WebsocketCORSFilter.ALLOWED_ORIGIN, allowedOrigin);
        deploymentInfo.addFilter(filterInfo);
        deploymentInfo.addFilterUrlMapping(filterInfo.getName(), "/*", DispatcherType.REQUEST);
    }

}
