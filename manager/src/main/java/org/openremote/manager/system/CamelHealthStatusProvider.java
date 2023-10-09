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

import org.apache.camel.Endpoint;
import org.apache.camel.component.seda.SedaEndpoint;
import org.apache.camel.health.HealthCheck;
import org.apache.camel.health.HealthCheckHelper;
import org.apache.camel.support.service.BaseService;
import org.openremote.container.message.MessageBrokerService;
import org.openremote.model.Container;
import org.openremote.model.system.HealthStatusProvider;
import org.openremote.model.util.ValueUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CamelHealthStatusProvider implements HealthStatusProvider {

    public static final String NAME = "camel";
    protected MessageBrokerService brokerService;

    @Override
    public void init(Container container) throws Exception {
        brokerService = container.getService(MessageBrokerService.class);
    }

    @Override
    public void start(Container container) throws Exception {
        // Add mixin for health check results
        ValueUtil.JSON.addMixIn(HealthCheck.Result.class, HealthCheckResultMixin.class);
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
        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> endpointInfos = new ArrayList<>();

        for (Endpoint endpoint : brokerService.getContext().getEndpoints()) {
            Map<String, Object> endpointInfo = new HashMap<>();
            endpointInfo.put("uri", endpoint.getEndpointUri());
            if (endpoint instanceof BaseService baseService) {
                endpointInfo.put("status", baseService.getStatus().name());
            }
            if (endpoint instanceof SedaEndpoint sedaEndpoint) {
                endpointInfo.put("queueSize", sedaEndpoint.getCurrentQueueSize());
                endpointInfo.put("queueMax", sedaEndpoint.getSize());
            }
            endpointInfos.add(endpointInfo);
        }

        result.put("healthChecks", HealthCheckHelper.invoke(brokerService.getContext()));
        result.put("endpoints", endpointInfos);
        return result;
    }
}
