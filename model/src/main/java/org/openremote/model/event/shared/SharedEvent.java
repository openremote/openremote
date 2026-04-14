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
package org.openremote.model.event.shared;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.openremote.model.asset.*;
import org.openremote.model.attribute.AttributeEvent;
import org.openremote.model.datapoint.AssetPredictedDatapointEvent;
import org.openremote.model.event.Event;
import org.openremote.model.gateway.*;
import org.openremote.model.services.ExternalServiceEvent;
import org.openremote.model.rules.RulesEngineStatusEvent;
import org.openremote.model.rules.RulesetChangedEvent;
import org.openremote.model.simulator.RequestSimulatorState;
import org.openremote.model.simulator.SimulatorState;
import org.openremote.model.syslog.SyslogEvent;
import org.openremote.model.alarm.AlarmEvent;

/**
 * An event that can be serialized and shared between client and server.
 */
@JsonSubTypes({
    // Events used on client and server (serializable)
    @JsonSubTypes.Type(value = AlarmEvent.class, name = "alarm"),
    @JsonSubTypes.Type(value = ExternalServiceEvent.class, name = "external-service"),
    @JsonSubTypes.Type(value = SyslogEvent.class, name = "syslog"),
    @JsonSubTypes.Type(value = AttributeEvent.class, name = "attribute"),
    @JsonSubTypes.Type(value = AssetPredictedDatapointEvent.class, name = "asset-predicted-data-points"),
    @JsonSubTypes.Type(value = AssetEvent.class, name = "asset"),
    @JsonSubTypes.Type(value = AssetsEvent.class, name = "assets"),
    @JsonSubTypes.Type(value = ReadAttributeEvent.class, name = "read-asset-attribute"),
    @JsonSubTypes.Type(value = ReadAssetEvent.class, name = "read-asset"),
    @JsonSubTypes.Type(value = ReadAssetsEvent.class, name = "read-assets"),
    @JsonSubTypes.Type(value = ReadAssetTreeEvent.class, name = "read-asset-tree"),
    @JsonSubTypes.Type(value = AssetTreeEvent.class, name = "asset-tree"),
    @JsonSubTypes.Type(value = SimulatorState.class, name = "simulator-state"),
    @JsonSubTypes.Type(value = RequestSimulatorState.class, name = "request-simulator-state"),
    @JsonSubTypes.Type(value = RulesEngineStatusEvent.class, name = "rules-engine-status"),
    @JsonSubTypes.Type(value = RulesetChangedEvent.class, name = "ruleset-changed"),
    @JsonSubTypes.Type(value = GatewayDisconnectEvent.class, name = GatewayDisconnectEvent.TYPE),
    @JsonSubTypes.Type(value = GatewayConnectionStatusEvent.class, name = "gateway-connection-status"),
    @JsonSubTypes.Type(value = GatewayCapabilitiesRequestEvent.class, name = GatewayCapabilitiesRequestEvent.TYPE),
    @JsonSubTypes.Type(value = GatewayCapabilitiesResponseEvent.class, name = GatewayCapabilitiesResponseEvent.TYPE),
    @JsonSubTypes.Type(value = GatewayTunnelStartRequestEvent.class, name = "gateway-tunnel-start-request"),
    @JsonSubTypes.Type(value = GatewayTunnelStartResponseEvent.class, name = "gateway-tunnel-start-response"),
    @JsonSubTypes.Type(value = GatewayTunnelStopRequestEvent.class, name = "gateway-tunnel-stop-request"),
    @JsonSubTypes.Type(value = GatewayTunnelStopResponseEvent.class, name = "gateway-tunnel-stop-response"),
    @JsonSubTypes.Type(value = GatewayInitStartEvent.class, name = GatewayInitStartEvent.TYPE),
    @JsonSubTypes.Type(value = GatewayInitDoneEvent.class, name = GatewayInitDoneEvent.TYPE)
})
@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    property = "eventType"
)
public abstract class SharedEvent extends Event {

    public static final String MESSAGE_PREFIX = "EVENT:";

    public SharedEvent(Long timestamp) {
        super(timestamp);
    }

    public SharedEvent() {
    }
}
