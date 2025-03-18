package org.openremote.agent.protocol.modbus;

import org.apache.plc4x.java.api.PlcConnection;
import org.apache.plc4x.java.api.messages.PlcReadRequest;
import org.apache.plc4x.java.api.messages.PlcReadResponse;
import org.apache.plc4x.java.api.messages.PlcWriteRequest;
import org.apache.plc4x.java.api.messages.PlcWriteResponse;
import org.apache.plc4x.java.api.model.PlcTag;
import org.openremote.agent.protocol.AbstractProtocol;
import org.openremote.agent.protocol.velbus.AbstractVelbusProtocol;
import org.openremote.model.Container;
import org.openremote.model.asset.agent.ConnectionStatus;
import org.openremote.model.attribute.Attribute;
import org.openremote.model.attribute.AttributeEvent;
import org.openremote.model.attribute.AttributeRef;
import org.openremote.model.syslog.SyslogCategory;
import org.openremote.model.util.ValueUtil;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.openremote.model.syslog.SyslogCategory.PROTOCOL;

public abstract class AbstractModbusProtocol<S extends AbstractModbusProtocol<S,T>, T extends ModbusAgent<T, S>> extends AbstractProtocol<T, ModbusAgentLink>{

    public static final Logger LOG = SyslogCategory.getLogger(PROTOCOL, AbstractVelbusProtocol.class);

    public static final int MODBUS_POLLING_RATE_MS = 1000;

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

            if (client.isConnected()) {
                setConnectionStatus(ConnectionStatus.CONNECTED);
            } else {
                setConnectionStatus(ConnectionStatus.DISCONNECTED);
            }
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Failed to create PLC4X connection for protocol instance: " + agent, e);
            setConnectionStatus(ConnectionStatus.ERROR);
            throw e;
        }
    }
    @Override
    protected void doStop(Container container) throws Exception {
        client.close();
    }

    @Override
    protected void doLinkAttribute(String assetId, Attribute<?> attribute, ModbusAgentLink agentLink) throws RuntimeException {
        AttributeRef ref = new AttributeRef(assetId, attribute.getName());
        int unitId = agentLink.getUnitId();
        int readAddress = agentLink.getReadAddress();

        pollingMap.put(ref, schedulePollingRequest(ref, attribute, ((int) agentLink.getRefresh()), unitId, agentLink.getReadType(), agentLink.getReadValueType(), readAddress));
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
        //TODO: Implement writing to the Modbus server

        PlcWriteRequest.Builder builder = client.writeRequestBuilder();

        switch (agentLink.getWriteType()){
            case COIL -> builder.addTagAddress("coil", "coil:" + agentLink.getWriteAddress(), processedValue);
            case HOLDING -> builder.addTagAddress("holdingRegisters", "holding-register:" + agentLink.getWriteAddress(), processedValue);
        }

        try {
            PlcWriteRequest request = builder.build();
            PlcWriteResponse response = request.execute().get();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getProtocolName() {
        return "Modbus TCP Client";
    }

    @Override
    public String getProtocolInstanceUri() {
        return "modbus-tcp://" + agent.getHost() + ":" + agent.getPort();
    }

    protected ScheduledFuture<?> schedulePollingRequest(AttributeRef ref,
                                                        Attribute<?> attribute,
                                                        int pollingMillis,
                                                        int unitId,
                                                        ModbusAgentLink.ReadType readType,
                                                        ModbusAgentLink.ModbusDataType dataType,
                                                        int readAddress) {

        LOG.warning("Scheduling polling request '" + "clientRequest" + "' to execute every " + pollingMillis + "ms for attribute: " + attribute);
        // TODO: integrate unit ID into the connection, meaning the agent
        return scheduledExecutorService.scheduleWithFixedDelay(() -> {
            try {
                PlcReadRequest.Builder builder = client.readRequestBuilder();
                switch (readType) {
                    case COIL -> builder.addTagAddress("coils", "coil:" + readAddress + ":" + dataType);
                    case DISCRETE -> builder.addTagAddress("discreteInputs", "discrete-input:" + readAddress + ":" + dataType);
                    case HOLDING -> builder.addTagAddress("holdingRegisters", "holding-register:" + readAddress + ":" + dataType);
                    case INPUT -> builder.addTagAddress("inputRegisters", "input-register:" + readAddress + ":" + dataType);
                    default -> throw new IllegalArgumentException("Unsupported read type: " + readType);
                }
                PlcReadRequest readRequest = builder.build();


                PlcReadResponse response = readRequest.execute().get();
                Object responseValue = response.getObject(response.getTagNames().stream().findFirst().get());
                Optional<?> coercedResponse = ValueUtil.getValueCoerced(responseValue, dataType.getJavaType());
                LOG.warning(ValueUtil.JSON.writeValueAsString(coercedResponse.get()));
                updateLinkedAttribute(ref, coercedResponse.get());
            } catch (Exception e) {
                LOG.log(Level.WARNING, prefixLogMessage("Exception thrown whilst processing polling response"));
            }
        }, 0, pollingMillis, TimeUnit.MILLISECONDS);
    }

    protected abstract PlcConnection createIoClient(T agent) throws RuntimeException;
}

//try {
//            setConnectionStatus(ConnectionStatus.CONNECTING);
//
//            client = createIoClient(agent);
//
//            client.connect();
//
//            if (client.isConnected()) {
//                setConnectionStatus(ConnectionStatus.CONNECTED);
//            } else {
//                setConnectionStatus(ConnectionStatus.DISCONNECTED);
//            }
//        } catch (Exception e) {
//            LOG.log(Level.SEVERE, "Failed to create IO client for protocol instance: " + agent, e);
//            setConnectionStatus(ConnectionStatus.ERROR);
//            throw e;
//        }

//                ModbusResponsePdu response = ModbusHelper.read(client, unitId, readType, readAddress);
//                Object parsedResponse = ModbusHelper.parseResponse(response);
//                Optional<?> coercedResponse = ValueUtil.getValueCoerced(parsedResponse, attribute.getTypeClass());
//                LOG.fine(ValueUtil.JSON.writeValueAsString(parsedResponse));
//                updateLinkedAttribute(ref, coercedResponse);

