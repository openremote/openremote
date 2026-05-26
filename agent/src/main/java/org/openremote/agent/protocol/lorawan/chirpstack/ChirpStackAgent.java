/*
 * Copyright 2025, OpenRemote Inc.
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
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package org.openremote.agent.protocol.lorawan.chirpstack;

import java.util.Optional;

import org.openremote.agent.protocol.lorawan.LoRaWANAgent;
import org.openremote.agent.protocol.mqtt.MQTTAgentLink;
import org.openremote.model.asset.agent.Agent;
import org.openremote.model.asset.agent.AgentDescriptor;
import org.openremote.model.value.AttributeDescriptor;
import org.openremote.model.value.ValueType;

import jakarta.persistence.Entity;

@Entity
public class ChirpStackAgent extends LoRaWANAgent<ChirpStackAgent, ChirpStackProtocol> {

  public static final AttributeDescriptor<String> HOST = Agent.HOST.withOptional(false);
  public static final AttributeDescriptor<Integer> PORT = Agent.PORT.withOptional(false);
  public static final AttributeDescriptor<Boolean> SECURE_GRPC =
      new AttributeDescriptor<>("secureGRPC", ValueType.BOOLEAN).withOptional(false);

  public static final AgentDescriptor<ChirpStackAgent, ChirpStackProtocol, MQTTAgentLink>
      DESCRIPTOR =
          new AgentDescriptor<>(
              ChirpStackAgent.class, ChirpStackProtocol.class, MQTTAgentLink.class);

  /** For use by hydrators (i.e. JPA/Jackson) */
  protected ChirpStackAgent() {}

  public ChirpStackAgent(String name) {
    super(name);
  }

  @Override
  public ChirpStackProtocol getProtocolInstance() {
    return new ChirpStackProtocol(this);
  }

  public Optional<Boolean> getSecureGRPC() {
    return getAttributes().getValue(SECURE_GRPC);
  }

  public ChirpStackAgent setSecureGRPC(Boolean secureGRPC) {
    getAttributes().getOrCreate(SECURE_GRPC).setValue(secureGRPC);
    return this;
  }
}
