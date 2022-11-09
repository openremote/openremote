/*
 * Copyright 2022, OpenRemote Inc.
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
package org.openremote.manager.system;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.apache.camel.Endpoint;
import org.apache.camel.component.direct.DirectEndpoint;
import org.apache.camel.component.seda.SedaEndpoint;
import org.apache.camel.health.HealthCheck;
import org.apache.camel.health.HealthCheckHelper;
import org.openremote.container.message.MessageBrokerService;
import org.openremote.model.Container;
import org.openremote.model.ContainerService;
import org.openremote.model.system.HealthStatusProvider;
import org.openremote.model.util.ValueUtil;
import org.openremote.protocol.zwave.model.commandclasses.channel.value.Value;

import java.util.Collection;

import static org.openremote.manager.event.ClientEventService.CLIENT_EVENT_QUEUE;

public class CamelHealthStatusProvider implements HealthStatusProvider, ContainerService {

    public static final String NAME = "camel";
    protected MessageBrokerService brokerService;

    @Override
    public void init(Container container) throws Exception {
        brokerService = container.getService(MessageBrokerService.class);
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
        Collection<HealthCheck.Result> results = HealthCheckHelper.invoke(brokerService.getContext());
        return ValueUtil.asJSON(results).orElse(null);
    }
}
