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

import org.openremote.container.Container;
import org.openremote.container.ContainerService;
import org.openremote.container.ContainerHealthStatusProvider;
import org.openremote.model.asset.Asset;
import org.openremote.model.asset.agent.ConnectionStatus;
import org.openremote.model.asset.agent.ProtocolConfiguration;
import org.openremote.model.attribute.AttributeRef;
import org.openremote.model.value.ObjectValue;
import org.openremote.model.value.Value;
import org.openremote.model.value.Values;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

public class AgentHealthStatusProvider implements ContainerHealthStatusProvider {

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
    public Value getHealthStatus() {
        AtomicInteger connectedCount = new AtomicInteger(0);
        AtomicInteger disabledCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);
        AtomicInteger otherCount = new AtomicInteger(0);

        ObjectValue objectValue = Values.createObject();
        objectValue.put("agents", agentService.getAgents().size());
        objectValue.put("protocols", agentService.protocols.size());

        for (Asset agent : agentService.getAgents().values()) {
            AtomicInteger total = new AtomicInteger(0);
            AtomicInteger connected = new AtomicInteger(0);
            AtomicInteger disabled = new AtomicInteger(0);
            AtomicInteger error = new AtomicInteger(0);
            AtomicInteger other = new AtomicInteger(0);

            ObjectValue agentValue = Values.createObject();
            agentValue.put("name", agent.getName());

            // Get protocol configurations for this agent
            agentService.protocolConfigurations.entrySet()
                .stream()
                .filter(protocolConfigInfo -> Objects.equals(agent.getId(), protocolConfigInfo.getKey().getEntityId()))
                .forEach(protocolConfigInfo -> {

                    total.incrementAndGet();

                    ConnectionStatus connectionStatus = protocolConfigInfo.getValue().value;
                    if (connectionStatus != null) {
                        switch (connectionStatus) {

                            case UNKNOWN:
                            case DISCONNECTED:
                            case CONNECTING:
                            case DISCONNECTING:
                            case WAITING:
                                otherCount.incrementAndGet();
                                other.incrementAndGet();
                                break;
                            case CONNECTED:
                                connectedCount.incrementAndGet();
                                connected.incrementAndGet();
                                break;
                            case DISABLED:
                                disabledCount.incrementAndGet();
                                disabled.incrementAndGet();
                                break;
                            case ERROR_AUTHENTICATION:
                            case ERROR_CONFIGURATION:
                            case ERROR:
                                errorCount.incrementAndGet();
                                error.incrementAndGet();
                                break;
                        }
                    } else {
                        otherCount.incrementAndGet();
                        other.incrementAndGet();
                    }

                    ObjectValue protocol = Values.createObject();

                    protocol.put(
                        "protocol",
                        ProtocolConfiguration.getProtocolName(protocolConfigInfo.getValue().key).orElse(null));

                    protocol.put(
                        "status",
                        protocolConfigInfo.getValue().value == null ? null : protocolConfigInfo.getValue().value.name());

                    List<AttributeRef> linkedAttributes = agentService.linkedAttributes.get(protocolConfigInfo.getKey());
                    protocol.put("linkedAttributes", linkedAttributes != null ? linkedAttributes.size() : 0);
                    agentValue.put(protocolConfigInfo.getKey().getAttributeName(), protocol);
                });

            agentValue.put("totalProtocolConfigs", total.get());
            agentValue.put("connectedProtocolConfigs", connected.get());
            agentValue.put("errorProtocolConfigs", error.get());
            agentValue.put("disabledProtocolConfigs", disabled.get());
            agentValue.put("otherProtocolConfigs", other.get());
            objectValue.put(agent.getId(), agentValue);
        }

        objectValue.put("totalProtocolConfigs", agentService.protocolConfigurations.size());
        objectValue.put("connectedProtocolConfigs", connectedCount.get());
        objectValue.put("errorProtocolConfigs", errorCount.get());
        objectValue.put("disabledProtocolConfigs", disabledCount.get());
        objectValue.put("otherProtocolConfigs", otherCount.get());
        objectValue.put("linkedAttributes", agentService.linkedAttributes.values().stream().mapToInt(List::size).sum());

        return objectValue;
    }
}
