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
package org.openremote.manager.server.agent;

import org.apache.camel.CamelContext;
import org.apache.camel.Component;
import org.apache.camel.util.CamelContextHelper;
import org.openremote.container.Container;
import org.openremote.container.ContainerService;
import org.openremote.container.message.MessageBrokerService;
import org.openremote.container.web.WebService;
import org.openremote.manager.shared.connector.ConnectorComponent;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;

public class ConnectorService implements ContainerService {

    private static final Logger LOG = Logger.getLogger(ConnectorService.class.getName());

    final protected Map<String, ConnectorComponent> connectors = new LinkedHashMap<>();

    @Override
    public void init(Container container) throws Exception {
        MessageBrokerService messageBrokerService = container.getService(MessageBrokerService.class);
        findConnectors(messageBrokerService.getContext());
    }

    @Override
    public void configure(Container container) throws Exception {
        container.getService(WebService.class).getApiSingletons().add(
            new ConnectorResourceImpl(this)
        );
    }

    @Override
    public void start(Container container) throws Exception {

    }

    @Override
    public void stop(Container container) throws Exception {

    }

    public Map<String, ConnectorComponent> getConnectorComponents() {
        return connectors;
    }

    public ConnectorComponent getConnectorComponentByType(String connectorType) {
        for (ConnectorComponent connector : connectors.values()) {
            if (connector.getType().equals(connectorType))
                return connector;
        }
        return null;
    }

    protected void findConnectors(CamelContext context) throws Exception {
        for (Map.Entry<String, Properties> entry : CamelContextHelper.findComponents(context).entrySet()) {

            String connectorComponentName = entry.getKey();
            Component component = context.getComponent(connectorComponentName);

            if (component instanceof ConnectorComponent) {
                ConnectorComponent connector = (ConnectorComponent)component;

                for (ConnectorComponent existingConnector : connectors.values()) {
                    if (existingConnector.getType().equals(connector.getType())) {
                        throw new IllegalStateException(
                            "Duplicate connector types are not allowed: " + connector.getType()
                        );
                    }
                }

                LOG.info(
                    "Configured connector component '" + connectorComponentName + "' of type: " + connector.getType()
                );

                connectors.put(connectorComponentName, connector);
            }
        }
    }
}
