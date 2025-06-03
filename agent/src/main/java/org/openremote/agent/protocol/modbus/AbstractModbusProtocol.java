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
import org.openremote.model.Container;
import org.openremote.model.asset.agent.ConnectionStatus;
import org.openremote.model.attribute.Attribute;
import org.openremote.model.attribute.AttributeEvent;
import org.openremote.model.attribute.AttributeRef;
import org.openremote.model.syslog.SyslogCategory;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.openremote.model.asset.agent.AgentLink.getOrThrowAgentLinkProperty;
import static org.openremote.model.syslog.SyslogCategory.PROTOCOL;

public abstract class AbstractModbusProtocol<S extends AbstractModbusProtocol<S,T>, T extends ModbusAgent<T, S>>
        extends AbstractProtocol<T, ModbusAgentLink>{

    public static final Logger LOG = SyslogCategory.getLogger(PROTOCOL, AbstractModbusProtocol.class);

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
        pollingMap.put(ref,
                scheduleModbusPollingReadRequest(
                    ref,
                    agentLink.getPollingMillis(),
                    agentLink.getReadMemoryArea(),
                    agentLink.getReadValueType(),
                    agentLink.getReadRegistersAmount(),
                    agentLink.getReadAddress()
                )
        );
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
        int writeAddress = getOrThrowAgentLinkProperty(agentLink.getWriteAddress(), "write address");

        PlcWriteRequest.Builder builder = client.writeRequestBuilder();

        switch (agentLink.getWriteMemoryArea()){
            case COIL -> builder.addTagAddress("coil", "coil:" + writeAddress, processedValue);
            case HOLDING -> builder.addTagAddress("holdingRegisters", "holding-register:" + writeAddress, processedValue);
            default -> throw new IllegalStateException("No other memory area is allowed to write to a Modbus server");
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

    protected ScheduledFuture<?> scheduleModbusPollingReadRequest(AttributeRef ref,
                                                                  long pollingMillis,
                                                                  ModbusAgentLink.ReadMemoryArea readType,
                                                                  ModbusAgentLink.ModbusDataType dataType,
                                                                  Optional<Integer> amountOfRegisters,
                                                                  Optional<Integer> readAddress) {

        PlcReadRequest.Builder builder = client.readRequestBuilder();

        int readAmountOfRegisters = (amountOfRegisters.isEmpty() || amountOfRegisters.get() < 1)
                ? dataType.getRegisterCount() : amountOfRegisters.get();
        String amountOfRegistersString = readAmountOfRegisters <= 1 ? "" : "["+readAmountOfRegisters+"]";

        int address = readAddress.orElseThrow(() -> new RuntimeException("Read Address is empty! Unable to schedule read request."));

        switch (readType) {
            case COIL -> builder.addTagAddress("coils", "coil:" + address + ":" + dataType + amountOfRegistersString);
            case DISCRETE -> builder.addTagAddress("discreteInputs", "discrete-input:" + address + ":" + dataType + amountOfRegistersString);
            case HOLDING -> builder.addTagAddress("holdingRegisters", "holding-register:" + address + ":" + dataType + amountOfRegistersString);
            case INPUT -> builder.addTagAddress("inputRegisters", "input-register:" + address + ":" + dataType + amountOfRegistersString);
            default -> throw new IllegalArgumentException("Unsupported read type: " + readType);
        }
        PlcReadRequest readRequest = builder.build();

        LOG.log(Level.FINE,"Scheduling Modbus Read Value polling request '" + "clientRequest" + "' to execute every " + pollingMillis + "ms for attributeRef: " + ref);
        return scheduledExecutorService.scheduleWithFixedDelay(() -> {
            try {
                PlcReadResponse response = readRequest.execute().get(3, TimeUnit.SECONDS);

                // We currently only request one thing (with the above tag), so we get it from there. If it doesn't exist,
                // we can assume that the request failed.
                String responseTag = response.getTagNames().stream().findFirst()
                        .orElseThrow(() -> new RuntimeException("Could not retrieve the requested value from the response"));

                Object responseValue = response.getObject(responseTag, readAmountOfRegisters-1);
                updateLinkedAttribute(ref, responseValue);
            } catch (Exception e) {
                LOG.log(Level.FINE,"Exception thrown during Modbus Read Value polling request '" + readRequest.toString());
            }
        }, 0, pollingMillis, TimeUnit.MILLISECONDS);
    }

    protected abstract PlcConnection createIoClient(T agent) throws RuntimeException;
}
