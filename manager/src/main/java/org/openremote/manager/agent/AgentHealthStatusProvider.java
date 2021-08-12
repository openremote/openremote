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

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.openremote.model.Container;
import org.openremote.model.ContainerService;
import org.openremote.model.asset.agent.Agent;
import org.openremote.model.asset.agent.ConnectionStatus;
import org.openremote.model.system.HealthStatusProvider;
import org.openremote.model.util.ValueUtil;

import java.util.concurrent.atomic.AtomicInteger;

public class AgentHealthStatusProvider implements HealthStatusProvider, ContainerService {

    public static final String NAME = "agents";
    public static final String VERSION = "1.0";
    protected AgentService agentService;

    @Override
    public int getPriority() {
        return ContainerService.DEFAULT_PRIORITY;
    }

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
    public String getHealthStatusVersion() {
        return VERSION;
    }

    @Override
    public Object getHealthStatus() {
        AtomicInteger connectedCount = new AtomicInteger(0);
        AtomicInteger disabledCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);
        AtomicInteger otherCount = new AtomicInteger(0);

        ObjectNode objectValue = ValueUtil.JSON.createObjectNode();
        objectValue.put("agents", agentService.getAgents().size());
        objectValue.put("protocols", agentService.protocolInstanceMap.size());

        for (Agent<?, ?, ?> agent : agentService.getAgents().values()) {
            ConnectionStatus status = agent.getAgentStatus().orElse(null);

            if (status != null) {
                switch (status) {

                    case DISCONNECTED:
                    case CONNECTING:
                    case DISCONNECTING:
                    case WAITING:
                        otherCount.incrementAndGet();
                        break;
                    case CONNECTED:
                        connectedCount.incrementAndGet();
                        break;
                    case DISABLED:
                        disabledCount.incrementAndGet();
                        break;
                    case ERROR:
                        errorCount.incrementAndGet();
                        break;
                }
            } else {
                otherCount.incrementAndGet();
            }

            ObjectNode agentValue = ValueUtil.JSON.createObjectNode();
            agentValue.put("name", agent.getName());
            agentValue.put("status", status != null ? status.name() : "null");
            agentValue.put("type", agent.getType());
            objectValue.set(agent.getId(), agentValue);
        }

        objectValue.put("totalAgents", agentService.agentMap.size());
        objectValue.put("connectedAgents", connectedCount.get());
        objectValue.put("errorAgents", errorCount.get());
        objectValue.put("disabledAgents", disabledCount.get());
        objectValue.put("otherAgents", otherCount.get());

        return objectValue;
    }
}
