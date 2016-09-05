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

import org.apache.camel.builder.RouteBuilder;
import org.openremote.container.Container;
import org.openremote.container.ContainerService;
import org.openremote.container.message.MessageBrokerService;
import org.openremote.container.persistence.PersistenceService;
import org.openremote.manager.shared.asset.Asset;
import org.openremote.manager.shared.asset.AssetType;

import java.util.logging.Logger;

import static org.openremote.container.persistence.PersistenceEvent.PERSISTENCE_EVENT_HEADER;
import static org.openremote.container.persistence.PersistenceEvent.PERSISTENCE_EVENT_TOPIC;

public class AgentService implements ContainerService {

    private static final Logger LOG = Logger.getLogger(AgentService.class.getName());

    protected MessageBrokerService messageBrokerService;
    protected PersistenceService persistenceService;
    protected ConnectorService connectorService;

    @Override
    public void init(Container container) throws Exception {
        messageBrokerService = container.getService(MessageBrokerService.class);
        persistenceService = container.getService(PersistenceService.class);
        connectorService = container.getService(ConnectorService.class);

        messageBrokerService.getContext().addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from(PERSISTENCE_EVENT_TOPIC)
                    .filter(body().isInstanceOf(Asset.class))
                    .filter(exchange -> {
                        Asset asset = exchange.getIn().getBody(Asset.class);
                        return AssetType.AGENT.equals(asset.getWellKnownType());
                    })
                    .process(exchange -> {
                        LOG.info("### AGENT PERSISTENCE EVENT: " + exchange.getIn().getHeader(PERSISTENCE_EVENT_HEADER));
                        LOG.info("### AGENT: " + exchange.getIn().getBody());
                        // TODO: Implement fine-grained life cycle changes
                        reconfigureAgents();
                    });
            }
        });
    }

    @Override
    public void configure(Container container) throws Exception {
    }

    @Override
    public void start(Container container) throws Exception {
        // TODO On startup, load all enabled agents and reconfigureAgents();
    }

    @Override
    public void stop(Container container) throws Exception {

    }

    protected void reconfigureAgents() {
        LOG.info("############################### TODO: RECONFIGURE AGENTS");
    }

}