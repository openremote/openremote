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

import io.netty.channel.ChannelHandler;
import io.netty.handler.timeout.ReadTimeoutHandler;
import org.openremote.agent.protocol.io.AbstractNettyIOClient;
import org.openremote.agent.protocol.modbus.util.ModbusProtocolCallback;
import org.openremote.agent.protocol.modbus.util.ModbusTcpDecoder;
import org.openremote.agent.protocol.modbus.util.ModbusTcpFrame;
import org.openremote.agent.protocol.tcp.AbstractTCPClientProtocol;
import org.openremote.model.Container;
import org.openremote.model.asset.agent.ConnectionStatus;
import org.openremote.model.attribute.Attribute;
import org.openremote.model.attribute.AttributeEvent;
import org.openremote.model.attribute.AttributeRef;
import org.openremote.model.syslog.SyslogCategory;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.function.Supplier;
import java.util.logging.Logger;

/**
 * Modbus TCP protocol implementation extending AbstractTCPClientProtocol.
 * Uses ModbusExecutor for shared Modbus logic (batching, polling, data conversion).
 */
public class ModbusTcpProtocol
        extends AbstractTCPClientProtocol<ModbusTcpProtocol, ModbusTcpAgent, ModbusAgentLink, ModbusTcpFrame, ModbusTcpIOClient>
        implements ModbusProtocolCallback<ModbusTcpFrame> {

    public static final Logger LOG = SyslogCategory.getLogger(SyslogCategory.PROTOCOL, ModbusTcpProtocol.class);

    // Request/response correlation via transaction ID
    private final Map<Integer, CompletableFuture<ModbusTcpFrame>> pendingRequests = new ConcurrentHashMap<>();

    // Shared Modbus logic
    private final ModbusExecutor<ModbusTcpFrame> modbusExecutor;

    public ModbusTcpProtocol(ModbusTcpAgent agent) {
        super(agent);
        this.modbusExecutor = new ModbusExecutor<>(this);
    }

    // ========== AbstractTCPClientProtocol methods ==========

    @Override
    protected ModbusTcpIOClient doCreateIoClient() throws Exception {
        String host = agent.getHost().orElseThrow(() -> new IllegalStateException("Host not configured"));
        int port = agent.getPort().orElseThrow(() -> new IllegalStateException("Port not configured"));
        return new ModbusTcpIOClient(host, port);
    }

    @Override
    protected Supplier<ChannelHandler[]> getEncoderDecoderProvider() {
        return () -> new ChannelHandler[] {
            new ReadTimeoutHandler(30, TimeUnit.SECONDS),
            new ModbusTcpIOClient.ModbusTcpEncoder(),
            new ModbusTcpDecoder(),
            new AbstractNettyIOClient.MessageToMessageDecoder<>(ModbusTcpFrame.class, client)
        };
    }

    @Override
    protected void onMessageReceived(ModbusTcpFrame frame) {
        // Request/response correlation via transaction ID
        LOG.finest(() -> "Received frame - TxID: " + frame.getTransactionId() + ", UnitID: " + frame.getUnitId() +
                ", FC: 0x" + Integer.toHexString(frame.getFunctionCode() & 0xFF));

        int txId = frame.getTransactionId();
        CompletableFuture<ModbusTcpFrame> future = pendingRequests.get(txId);
        if (future != null) {
            future.complete(frame);
        } else {
            LOG.warning("Received Modbus TCP response for unknown or timed out transaction ID: " + txId);
        }
    }

    @Override
    protected ModbusTcpFrame createWriteMessage(ModbusAgentLink agentLink, AttributeEvent event, Object processedValue) {
        // Not used - we override doLinkedAttributeWrite entirely
        return null;
    }

    // ========== Lifecycle overrides ==========

    @Override
    protected void doStart(Container container) throws Exception {
        super.doStart(container);  // Creates and connects IOClient
        modbusExecutor.onStart();    // Initialize device config
    }

    @Override
    protected void doStop(Container container) throws Exception {
        modbusExecutor.onStop();     // Cancel scheduled tasks
        pendingRequests.clear();
        super.doStop(container);   // Disconnect IOClient
    }

    // ========== Attribute linking - delegate to helper ==========

    @Override
    protected void doLinkAttribute(String assetId, Attribute<?> attribute, ModbusAgentLink agentLink) {
        modbusExecutor.linkAttribute(assetId, attribute, agentLink);
    }

    @Override
    protected void doUnlinkAttribute(String assetId, Attribute<?> attribute, ModbusAgentLink agentLink) {
        modbusExecutor.unlinkAttribute(assetId, attribute, agentLink);
    }

    // ========== Write handling - delegate to helper ==========

    @Override
    protected void doLinkedAttributeWrite(ModbusAgentLink agentLink, AttributeEvent event, Object processedValue) {
        modbusExecutor.handleAttributeWrite(agentLink, event, processedValue);
    }

    // ========== ModbusProtocolCallback implementation ==========

    @Override
    public ModbusTcpFrame sendModbusRequest(int unitId, byte[] pdu, long timeoutMs) throws Exception {
        int transactionId;
        CompletableFuture<ModbusTcpFrame> responseFuture;

        synchronized (modbusExecutor.requestLock) {
            if (client == null || client.getConnectionStatus() != ConnectionStatus.CONNECTED) {
                throw new IllegalStateException("Client not connected");
            }

            transactionId = client.getNextTransactionId();
            ModbusTcpFrame request = new ModbusTcpFrame(transactionId, unitId, pdu);
            responseFuture = new CompletableFuture<>();
            pendingRequests.put(transactionId, responseFuture);

            client.sendMessage(request);
            LOG.finest(() -> "Sent Modbus TCP request - TxID: " + transactionId + ", UnitID: " + unitId +
                    ", FC: 0x" + Integer.toHexString(pdu[0] & 0xFF));
        }

        try {
            ModbusTcpFrame response = responseFuture.get(timeoutMs, TimeUnit.MILLISECONDS);
            LOG.finest(() -> "Received Modbus TCP response - TxID: " + response.getTransactionId() +
                    ", UnitID: " + response.getUnitId() + ", FC: 0x" + Integer.toHexString(response.getFunctionCode() & 0xFF));
            return response;
        } catch (TimeoutException e) {
            LOG.warning("Modbus TCP request timeout - TxID: " + transactionId + ", UnitID: " + unitId);
            throw e;
        } finally {
            pendingRequests.remove(transactionId);
        }
    }

    @Override
    public ConnectionStatus getConnectionStatus() {
        return client != null ? client.getConnectionStatus() : ConnectionStatus.DISCONNECTED;
    }

    @Override
    public Optional<ModbusAgent.DeviceConfigMap> getDeviceConfig() {
        return agent.getDeviceConfig();
    }

    @Override
    public ModbusAgent<?, ?> getModbusAgent() {
        return agent;
    }

    @Override
    public ScheduledExecutorService getScheduledExecutorService() {
        return scheduledExecutorService;
    }

    @Override
    public Map<AttributeRef, Attribute<?>> getLinkedAttributes() {
        return linkedAttributes;
    }

    @Override
    public void publishAttributeEvent(AttributeEvent event) {
        sendAttributeEvent(event);
    }

    @Override
    public String getProtocolName() {
        return "Modbus TCP";
    }

    @Override
    public String getProtocolInstanceUri() {
        return client != null ? client.getClientUri() : "";
    }
}
