/*
 * Copyright 2017, OpenRemote Inc.
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
package org.openremote.manager.agent;

import org.openremote.model.Container;
import org.openremote.model.ContainerService;
import org.openremote.model.asset.agent.Agent;
import org.openremote.model.asset.agent.ConnectionStatus;
import org.openremote.model.system.HealthStatusProvider;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class AgentHealthStatusProvider implements HealthStatusProvider {

    public static final String NAME = "agents";
    protected AgentService agentService;

    @Override
    public void init(Container container) throws Exception {
        agentService = container.getService(AgentService.class);
    }

    @Override
    public void start(Container container) throws Exception {

    }

    @Override
    public void stop(Container container) throws Exception {

    }

    @Override
    public String getHealthStatusName() {
        return NAME;
    }

    @Override
    public Object getHealthStatus() {
        AtomicInteger connectedCount = new AtomicInteger(0);
        AtomicInteger disabledCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);
        AtomicInteger otherCount = new AtomicInteger(0);

        Map<String, Object> objectValue = new HashMap<>();
        objectValue.put("agents", agentService.getAgents().size());
        objectValue.put("protocols", agentService.protocolInstanceMap.size());

        for (Agent<?, ?, ?> agent : agentService.getAgents().values()) {
            ConnectionStatus status = agent.getAgentStatus().orElse(null);

            if (status != null) {
                switch (status) {
                    case DISCONNECTED, CONNECTING, DISCONNECTING, WAITING -> otherCount.incrementAndGet();
                    case CONNECTED -> connectedCount.incrementAndGet();
                    case DISABLED -> disabledCount.incrementAndGet();
                    case ERROR -> errorCount.incrementAndGet();
                }
            } else {
                otherCount.incrementAndGet();
            }

            Map<String, Object> agentValue = new HashMap<>();
            agentValue.put("name", agent.getName());
            agentValue.put("status", status != null ? status.name() : "null");
            agentValue.put("type", agent.getType());
            objectValue.put(agent.getId(), agentValue);
        }

        objectValue.put("totalAgents", agentService.getAgents().size());
        objectValue.put("connectedAgents", connectedCount.get());
        objectValue.put("errorAgents", errorCount.get());
        objectValue.put("disabledAgents", disabledCount.get());
        objectValue.put("otherAgents", otherCount.get());

        return objectValue;
    }
}
