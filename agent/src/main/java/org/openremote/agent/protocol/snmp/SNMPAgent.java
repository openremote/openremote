/*
 * Copyright 2026, OpenRemote Inc.
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
package org.openremote.agent.protocol.snmp;

import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonValue;

import org.openremote.model.asset.agent.Agent;
import org.openremote.model.asset.agent.AgentDescriptor;
import org.openremote.model.value.AttributeDescriptor;
import org.openremote.model.value.ValueDescriptor;

import jakarta.persistence.Entity;

@Entity
public class SNMPAgent extends Agent<SNMPAgent, SNMPProtocol, SNMPAgentLink> {

  public enum SNMPVersion {
    V1(0),
    V2c(1),
    V3(3);

    private final int version;

    SNMPVersion(int version) {
      this.version = version;
    }

    public int getVersion() {
      return version;
    }

    @JsonValue
    public String getValue() {
      return toString();
    }
  }

  public static final ValueDescriptor<SNMPVersion> VALUE_SNMP_VERSION =
      new ValueDescriptor<>("SNMPVersion", SNMPVersion.class);

  public static final AttributeDescriptor<SNMPVersion> SNMP_VERSION =
      new AttributeDescriptor<>("SNMPVersionValue", VALUE_SNMP_VERSION);
  public static final AttributeDescriptor<String> SNMP_BIND_HOST =
      Agent.BIND_HOST.withOptional(false);
  public static final AttributeDescriptor<Integer> SNMP_BIND_PORT =
      Agent.BIND_PORT.withOptional(false);

  public static final AgentDescriptor<SNMPAgent, SNMPProtocol, SNMPAgentLink> DESCRIPTOR =
      new AgentDescriptor<>(SNMPAgent.class, SNMPProtocol.class, SNMPAgentLink.class);

  /** For use by hydrators (i.e. JPA/Jackson) */
  protected SNMPAgent() {}

  public SNMPAgent(String name) {
    super(name);
  }

  @Override
  public SNMPProtocol getProtocolInstance() {
    return new SNMPProtocol(this);
  }

  public Optional<SNMPVersion> getSNMPVersion() {
    return getAttributes().getValue(SNMP_VERSION);
  }

  public SNMPAgent setSNMPVersion(SNMPVersion version) {
    getAttributes().getOrCreate(SNMP_VERSION).setValue(version);
    return this;
  }
}
