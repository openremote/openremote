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
import org.openremote.container.persistence.PersistenceService;
import org.openremote.container.web.WebService;
import org.openremote.manager.shared.agent.Agent;

import java.util.List;

public class AgentService implements ContainerService {

    protected PersistenceService persistenceService;

    @Override
    public void init(Container container) throws Exception {
        persistenceService = container.getService(PersistenceService.class);
    }

    @Override
    public void configure(Container container) throws Exception {
        container.getService(WebService.class).getApiSingletons().add(
            new AgentResourceImpl(this)
        );
    }

    @Override
    public void start(Container container) throws Exception {

    }

    @Override
    public void stop(Container container) throws Exception {

    }

    public Agent[] getAll() {
        return persistenceService.doTransaction(em -> {
            List<Agent> result =
                em.createQuery(
                    "select pa from Agent pa order by pa.createdOn desc"
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
}