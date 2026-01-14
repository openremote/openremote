/*
 * Copyright 2025, OpenRemote Inc.
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
package org.openremote.agent.protocol.modbus.util;

import org.openremote.agent.protocol.modbus.ModbusAgent;
import org.openremote.model.asset.agent.ConnectionStatus;
import org.openremote.model.attribute.Attribute;
import org.openremote.model.attribute.AttributeEvent;
import org.openremote.model.attribute.AttributeRef;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Callback interface for Modbus protocol operations.
 * Implemented by the concrete protocol classes (ModbusTcpProtocol, ModbusSerialProtocol)
 * to provide transport-specific functionality to the shared ModbusProtocolHelper.
 *
 * @param <F> the frame type (ModbusTcpFrame or ModbusSerialFrame)
 */
public interface ModbusProtocolCallback<F extends ModbusFrame> {

    /**
     * Send a Modbus request and wait for response.
     * This is transport-specific (TCP uses transaction IDs, Serial uses timing).
     *
     * @param unitId the slave/unit ID
     * @param pdu the Protocol Data Unit to send
     * @param timeoutMs timeout in milliseconds
     * @return the response frame
     * @throws Exception if the request fails or times out
     */
    F sendModbusRequest(int unitId, byte[] pdu, long timeoutMs) throws Exception;

    /**
     * Get the current connection status.
     * @return the connection status
     */
    ConnectionStatus getConnectionStatus();

    /**
     * Update a linked attribute with a new value.
     * @param ref the attribute reference
     * @param value the new value
     */
    void updateLinkedAttribute(AttributeRef ref, Object value);

    /**
     * Get the device configuration map.
     * @return optional device config map
     */
    Optional<ModbusAgent.DeviceConfigMap> getDeviceConfig();

    /**
     * Get the Modbus agent instance.
     * Named differently from Protocol.getAgent() to avoid conflict.
     * @return the Modbus agent
     */
    ModbusAgent<?, ?> getModbusAgent();

    /**
     * Get the scheduled executor service for scheduling tasks.
     * @return the executor service
     */
    ScheduledExecutorService getScheduledExecutorService();

    /**
     * Get the map of linked attributes.
     * @return map of attribute refs to attributes
     */
    Map<AttributeRef, Attribute<?>> getLinkedAttributes();

    /**
     * Publish an attribute event to update an attribute value.
     * @param event the event to publish
     */
    void publishAttributeEvent(AttributeEvent event);

    /**
     * Get the protocol name for logging purposes.
     * @return protocol name (e.g., "TCP" or "Serial")
     */
    String getProtocolName();
}
