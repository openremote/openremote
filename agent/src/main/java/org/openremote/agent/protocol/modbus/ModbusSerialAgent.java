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
package org.openremote.agent.protocol.modbus;

import org.openremote.model.asset.agent.Agent;
import org.openremote.model.asset.agent.AgentDescriptor;
import org.openremote.model.value.AttributeDescriptor;
import org.openremote.model.value.ValueType;

import jakarta.persistence.Entity;

/**
 * This Modbus serial agent is currently untested, due to difficulties in testing Modbus serial
 * agents (especially the absence of automated Modbus serial testing libraries)
 */
@Entity
public class ModbusSerialAgent extends ModbusAgent<ModbusSerialAgent, ModbusSerialProtocol> {

  public static final AttributeDescriptor<String> SERIAL_PORT =
      Agent.SERIAL_PORT.withOptional(false);
  public static final AttributeDescriptor<Integer> BAUD_RATE =
      Agent.SERIAL_BAUDRATE.withOptional(false);
  public static final AttributeDescriptor<Integer> DATA_BITS =
      new AttributeDescriptor<>("dataBits", ValueType.POSITIVE_INTEGER);
  public static final AttributeDescriptor<Integer> STOP_BITS =
      new AttributeDescriptor<>("stopBits", ValueType.POSITIVE_INTEGER);

  // TODO: Doesn't work, getting a frontend error TypeError: Cannot read properties of undefined
  // (reading 'units') at getValueFormatConstraintOrUnits
  //    public static final AttributeDescriptor<ModbusClientParity> PARITY = new
  // AttributeDescriptor<ModbusClientParity>("parity",
  //            new ValueDescriptor<ModbusClientParity>("modbusClientParity",
  // ModbusClientParity.class)
  //    );

  public enum ModbusClientParity {
    NO_PARITY,
    ODD_PARITY,
    EVEN_PARITY,
    MARK_PARITY,
    SPACE_PARITY,
  }

  public static final AgentDescriptor<ModbusSerialAgent, ModbusSerialProtocol, ModbusAgentLink>
      DESCRIPTOR =
          new AgentDescriptor<>(
              ModbusSerialAgent.class, ModbusSerialProtocol.class, ModbusAgentLink.class);

  /** For use by hydrators (i.e. JPA/Jackson) */
  protected ModbusSerialAgent() {}

  public ModbusSerialAgent(String name) {
    super(name);
  }

  public Integer getBaudRate() {
    return getAttribute(BAUD_RATE).get().getValue().get();
  }

  public Integer getDataBits() {
    return getAttribute(DATA_BITS).get().getValue().get();
  }

  public Integer getStopBits() {
    return getAttribute(STOP_BITS).get().getValue().get();
  }

  //    public ModbusClientParity getParity() {
  //        return getAttribute(PARITY).get().getValue().get();
  //    }

  @Override
  public ModbusSerialProtocol getProtocolInstance() {
    return new ModbusSerialProtocol(this);
  }
}
