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

import org.openremote.container.Container;
import org.openremote.container.ContainerService;
import org.openremote.container.util.IdentifierUtil;
import org.openremote.container.web.WebService;
import org.openremote.manager.server.assets.AssetsService;
import org.openremote.manager.shared.agent.Agent;
import org.openremote.manager.shared.ngsi.Entity;
import org.openremote.manager.shared.ngsi.params.EntityListParams;
import org.openremote.manager.shared.ngsi.params.EntityParams;

import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class AgentService implements ContainerService {

    private static final Logger LOG = Logger.getLogger(AgentService.class.getName());

    protected ConnectorService connectorService;
    protected AssetsService assetsService;

    @Override
    public void init(Container container) throws Exception {
        connectorService = container.getService(ConnectorService.class);
        assetsService = container.getService(AssetsService.class);
    }

    @Override
    public void configure(Container container) throws Exception {
        container.getService(WebService.class).getApiSingletons().add(
            new AgentResourceImpl(connectorService, this)
        );
    }

    @Override
    public void start(Container container) throws Exception {

    }

    @Override
    public void stop(Container container) throws Exception {

    }

    public Response createAgent(String name, String connectorType) {
        Agent agent = new Agent();

        agent.setId(IdentifierUtil.generateGlobalUniqueId());
        agent.setEnabled(true);
        agent.setName(name);
        agent.setConnectorType(connectorType);

        return assetsService.getContextBroker().postEntity(agent);
    }

    public Agent[] getAgents() {
        Entity[] entities = assetsService.getContextBroker().getEntities(
            new EntityListParams().type(Agent.TYPE)
        );
       List<Agent> agents = new ArrayList<>();
        for (Entity entity : entities) {
            Agent agent = new Agent(entity.getJsonObject());
            agents.add(agent);
        }
        return agents.toArray(new Agent[agents.size()]);
    }

    public Agent getAgent(String id) {
        return new Agent(
            assetsService
                .getContextBroker()
                .getEntity(id, new EntityParams())
                .getJsonObject()
        );

    }
}