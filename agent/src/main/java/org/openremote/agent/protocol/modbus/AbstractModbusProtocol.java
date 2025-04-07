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
package org.openremote.agent.protocol.modbus;

import org.apache.plc4x.java.api.PlcConnection;
import org.apache.plc4x.java.api.messages.PlcReadRequest;
import org.apache.plc4x.java.api.messages.PlcReadResponse;
import org.apache.plc4x.java.api.messages.PlcWriteRequest;
import org.apache.plc4x.java.api.messages.PlcWriteResponse;
import org.apache.plc4x.java.api.types.PlcResponseCode;
import org.openremote.agent.protocol.AbstractProtocol;
import org.openremote.agent.protocol.velbus.AbstractVelbusProtocol;
import org.openremote.model.Container;
import org.openremote.model.asset.agent.ConnectionStatus;
import org.openremote.model.attribute.Attribute;
import org.openremote.model.attribute.AttributeEvent;
import org.openremote.model.attribute.AttributeRef;
import org.openremote.model.syslog.SyslogCategory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.openremote.model.asset.agent.AgentLink.getOrThrowAgentLinkProperty;
import static org.openremote.model.syslog.SyslogCategory.PROTOCOL;

public abstract class AbstractModbusProtocol<S extends AbstractModbusProtocol<S,T>, T extends ModbusAgent<T, S>> extends AbstractProtocol<T, ModbusAgentLink>{

    public static final Logger LOG = SyslogCategory.getLogger(PROTOCOL, AbstractVelbusProtocol.class);

    protected final Map<AttributeRef, ScheduledFuture<?>> pollingMap = new HashMap<>();

    PlcConnection client = null;

    public AbstractModbusProtocol(T agent) {
        super(agent);
    }

    @Override
    protected void doStart(Container container) throws Exception {

        try {
            setConnectionStatus(ConnectionStatus.CONNECTING);

            client = createIoClient(agent);

            client.connect();

            setConnectionStatus(client.isConnected() ? ConnectionStatus.CONNECTED : ConnectionStatus.ERROR);
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Failed to create PLC4X connection for protocol instance: " + agent, e);
            setConnectionStatus(ConnectionStatus.ERROR);
            throw e;
        }
    }
    @Override
    protected void doStop(Container container) throws Exception {
        pollingMap.forEach((key, value) -> value.cancel(false));

        client.close();
    }

    @Override
    protected void doLinkAttribute(String assetId, Attribute<?> attribute, ModbusAgentLink agentLink) throws RuntimeException {
        AttributeRef ref = new AttributeRef(assetId, attribute.getName());
        pollingMap.put(ref, schedulePollingRequest(ref, attribute, ((int) agentLink.getRefresh()), agentLink.getReadMemoryArea(), agentLink.getReadValueType(), agentLink.getReadAddress()));
    }

    @Override
    protected void doUnlinkAttribute(String assetId, Attribute<?> attribute, ModbusAgentLink agentLink) {
        AttributeRef attributeRef = new AttributeRef(assetId, attribute.getName());
        ScheduledFuture<?> pollTask = pollingMap.remove(attributeRef);
        if (pollTask != null) {
            pollTask.cancel(false);
        }
    }

    @Override
    protected void doLinkedAttributeWrite(ModbusAgentLink agentLink, AttributeEvent event, Object processedValue) {

        // Look at comment in schedulePollingRequest for an explanation to this
        int offsetWriteAddress = getOrThrowAgentLinkProperty(agentLink.getWriteAddress(), "write address") + 1;

        PlcWriteRequest.Builder builder = client.writeRequestBuilder();

        switch (agentLink.getWriteMemoryArea()){
            case COIL -> builder.addTagAddress("coil", "coil:" + offsetWriteAddress, processedValue);
            case HOLDING -> builder.addTagAddress("holdingRegisters", "holding-register:" + offsetWriteAddress, processedValue);
            default -> throw new IllegalStateException("No other coil is allowed to write to a Modbus server");
        }

        PlcWriteRequest request = builder.build();

        try {
            PlcWriteResponse response = request.execute().get(3, TimeUnit.SECONDS);
            if (response.getResponseCode(response.getTagNames().stream().findFirst().orElseThrow()) != PlcResponseCode.OK){
                throw new IllegalStateException("PLC Write Response code is something other than \"OK\"");
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    protected ScheduledFuture<?> schedulePollingRequest(AttributeRef ref,
                                                        Attribute<?> attribute,
                                                        int pollingMillis,
                                                        ModbusAgentLink.ReadMemoryArea readType,
                                                        ModbusAgentLink.ModbusDataType dataType,
                                                        int readAddress) {

        LOG.log(Level.FINE,"Scheduling polling request '" + "clientRequest" + "' to execute every " + pollingMillis + "ms for attribute: " + attribute);
        return scheduledExecutorService.scheduleWithFixedDelay(() -> {
            try {
                // PLC4X accounts for zero-based addressing by removing 1 from the readAddress that is being read/written, so we compensate for that.
                // So when I try to read address 3, it is really reading address 2, which is the 3rd element of the values.
                // I think this could lead to some confusion, so I'll add that 1 back.

                int offsetReadAddress = readAddress + 1;

                PlcReadRequest.Builder builder = client.readRequestBuilder();
                switch (readType) {
                    case COIL -> builder.addTagAddress("coils", "coil:" + offsetReadAddress + ":" + dataType);
                    case DISCRETE -> builder.addTagAddress("discreteInputs", "discrete-input:" + offsetReadAddress + ":" + dataType);
                    case HOLDING -> builder.addTagAddress("holdingRegisters", "holding-register:" + offsetReadAddress + ":" + dataType);
                    case INPUT -> builder.addTagAddress("inputRegisters", "input-register:" + offsetReadAddress + ":" + dataType);
                    default -> throw new IllegalArgumentException("Unsupported read type: " + readType);
                }
                PlcReadRequest readRequest = builder.build();

                PlcReadResponse response = readRequest.execute().get(3, TimeUnit.SECONDS);

                // We currently only request one thing (with the above tag), so we get it from there. If it doesn't exist,
                // we can assume that the request failed.

                String responseTag = response.getTagNames().stream().findFirst()
                        .orElseThrow(() -> new RuntimeException("Could not retrieve the requested value from the response"));

                Object responseValue = response.getObject(responseTag);
                updateLinkedAttribute(ref, responseValue);
            } catch (Exception e) {
                LOG.log(Level.WARNING, prefixLogMessage("Exception thrown whilst processing polling response"));
            }
        }, 0, pollingMillis, TimeUnit.MILLISECONDS);
    }

    protected abstract PlcConnection createIoClient(T agent) throws RuntimeException;
}
