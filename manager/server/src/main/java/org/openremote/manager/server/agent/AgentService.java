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
import org.openremote.container.web.WebService;
import org.openremote.manager.shared.agent.Agent;

import java.util.*;
import java.util.logging.Logger;

import static org.openremote.container.persistence.PersistenceEvent.PERSISTENCE_EVENT_HEADER;
import static org.openremote.container.persistence.PersistenceEvent.PERSISTENCE_EVENT_TOPIC;

public class AgentService implements ContainerService {

    private static final Logger LOG = Logger.getLogger(AgentService.class.getName());

    final protected Map<String, Agent> activeAgents = new LinkedHashMap<>();

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
                    .filter(body().isInstanceOf(Agent.class))
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
        container.getService(WebService.class).getApiSingletons().add(
            new AgentResourceImpl(this)
        );
    }

    @Override
    public void start(Container container) throws Exception {
        reconfigureAgents();
    }

    @Override
    public void stop(Container container) throws Exception {

    }

    public Agent[] getActive() {
        synchronized (activeAgents) {
            return activeAgents.values().toArray(new Agent[activeAgents.size()]);
        }
    }

    public Agent[] getAll(boolean onlyEnabled) {
        return persistenceService.doTransaction(em -> {
            List<Agent> result =
                em.createQuery(
                    onlyEnabled
                        ? "select a from Agent a where a.enabled = true order by a.createdOn desc"
                        : "select a from Agent a order by a.createdOn desc"
                    , Agent.class
                ).getResultList();
            return result.toArray(new Agent[result.size()]);
        });
    }

    public Agent get(String agentId) {
        return persistenceService.doTransaction(em -> {
            return em.find(Agent.class, agentId);
        });
    }

    public void create(Agent agent) {
        persistenceService.doTransaction(em -> {
            em.persist(agent);
        });
    }

    public void update(Agent agent) {
        persistenceService.doTransaction(em -> {
            em.merge(agent);
        });
    }

    public void delete(String agentId) {
        persistenceService.doTransaction(em -> {
            Agent agent = em.find(Agent.class, agentId);
            if (agent != null) {
                em.remove(agent);
            }
        });
    }

    protected void reconfigureAgents() {
        synchronized (activeAgents) {
            // TODO Implement route restart etc.
            LOG.info("### RECONFIGURE AGENTS");
        }
    }

    protected void startAgent(Agent agent) {

    }

    protected void stopAgent(Agent agent) {

    }
}