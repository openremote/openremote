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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.openremote.model.asset.*;
import org.openremote.model.attribute.AttributeEvent;
import org.openremote.model.event.Event;
import org.openremote.model.gateway.GatewayConnectionStatusEvent;
import org.openremote.model.gateway.GatewayDisconnectEvent;
import org.openremote.model.rules.RulesEngineStatusEvent;
import org.openremote.model.rules.RulesetChangedEvent;
import org.openremote.model.simulator.RequestSimulatorState;
import org.openremote.model.simulator.SimulatorState;
import org.openremote.model.syslog.SyslogEvent;

/**
 * An event that can be serialized and shared between client and server.
 */
@JsonSubTypes({
    // Events used on client and server (serializable)
    @JsonSubTypes.Type(value = SyslogEvent.class, name = "syslog"),
    @JsonSubTypes.Type(value = AttributeEvent.class, name = "attribute"),
    @JsonSubTypes.Type(value = AssetEvent.class, name = "asset"),
    @JsonSubTypes.Type(value = AssetsEvent.class, name = "assets"),
    @JsonSubTypes.Type(value = ReadAttributeEvent.class, name = "read-asset-attribute"),
    @JsonSubTypes.Type(value = ReadAssetEvent.class, name = "read-asset"),
    @JsonSubTypes.Type(value = ReadAssetsEvent.class, name = "read-assets"),
    @JsonSubTypes.Type(value = SimulatorState.class, name = "simulator-state"),
    @JsonSubTypes.Type(value = RequestSimulatorState.class, name = "request-simulator-state"),
    @JsonSubTypes.Type(value = RulesEngineStatusEvent.class, name = "rules-engine-status"),
    @JsonSubTypes.Type(value = RulesetChangedEvent.class, name = "ruleset-changed"),
    @JsonSubTypes.Type(value = GatewayDisconnectEvent.class, name = "gateway-disconnect"),
    @JsonSubTypes.Type(value = GatewayConnectionStatusEvent.class, name = "gateway-connection-status"),
    @JsonSubTypes.Type(value = DeleteAssetsRequestEvent.class, name = "delete-assets-request"),
    @JsonSubTypes.Type(value = DeleteAssetsResponseEvent.class, name = "delete-assets-response")
})
@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    property = "eventType"
)
public abstract class SharedEvent extends Event {

    public static final String MESSAGE_PREFIX = "EVENT:";

    public SharedEvent(long timestamp) {
        super(timestamp);
    }

    public SharedEvent() {
    }

    public boolean canAccessPublicRead() {
        return false;
    }

    public boolean canAccessRestrictedRead() {
        return false;
    }
}
