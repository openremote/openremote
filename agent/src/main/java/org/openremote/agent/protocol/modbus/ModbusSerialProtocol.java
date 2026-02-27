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
import org.openremote.agent.protocol.io.AbstractNettyIOClient;
import org.openremote.agent.protocol.modbus.util.ModbusProtocolCallback;
import org.openremote.agent.protocol.modbus.util.ModbusRTUDecoder;
import org.openremote.agent.protocol.modbus.util.ModbusRTUEncoder;
import org.openremote.agent.protocol.modbus.util.ModbusSerialFrame;
import org.openremote.agent.protocol.serial.AbstractSerialProtocol;
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
 * Modbus Serial (RTU) protocol implementation extending AbstractSerialProtocol.
 * Uses ModbusExecutor for shared Modbus logic (batching, polling, data conversion).
 */
public class ModbusSerialProtocol
        extends AbstractSerialProtocol<ModbusSerialProtocol, ModbusSerialAgent, ModbusAgentLink, ModbusSerialFrame, ModbusSerialIOClient>
        implements ModbusProtocolCallback<ModbusSerialFrame> {

    public static final Logger LOG = SyslogCategory.getLogger(SyslogCategory.PROTOCOL, ModbusSerialProtocol.class);

    // Single pending request (RTU is half-duplex, no transaction IDs)
    private volatile CompletableFuture<ModbusSerialFrame> pendingRequest = null;
    private long lastRequestTime = 0;
    private static final long MIN_REQUEST_INTERVAL_MS = 10; // Gap between requests per modbus.org
    private final Object sendLock = new Object();

    // Shared Modbus logic
    private final ModbusExecutor<ModbusSerialFrame> modbusExecutor;

    public ModbusSerialProtocol(ModbusSerialAgent agent) {
        super(agent);
        this.modbusExecutor = new ModbusExecutor<>(this);
    }

    // ========== AbstractSerialProtocol methods ==========

    @Override
    protected ModbusSerialIOClient doCreateIoClient() throws Exception {
        String portName = agent.getSerialPort().orElseThrow(() -> new RuntimeException("Serial port not specified"));
        int baudRate = agent.getBaudRate();
        int dataBits = agent.getDataBits();
        var stopBits = agent.getStopBits();
        var parity = agent.getParity();

        return new ModbusSerialIOClient(portName, baudRate, dataBits, stopBits, parity);
    }

    @Override
    protected Supplier<ChannelHandler[]> getEncoderDecoderProvider() {
        return () -> new ChannelHandler[] {
            new ModbusRTUEncoder(),
            new ModbusRTUDecoder(),
            new AbstractNettyIOClient.MessageToMessageDecoder<>(ModbusSerialFrame.class, client)
        };
    }

    @Override
    protected void onMessageReceived(ModbusSerialFrame frame) {
        // Complete single pending request (RTU has no transaction IDs)
        LOG.finest(() -> "Received frame - UnitID: " + frame.getUnitId() +
                ", FC: 0x" + Integer.toHexString(frame.getFunctionCode() & 0xFF));

        synchronized (modbusExecutor.requestLock) {
            if (pendingRequest != null) {
                pendingRequest.complete(frame);
            } else {
                LOG.warning("Received Modbus Serial response with no pending request");
            }
        }
    }

    @Override
    protected ModbusSerialFrame createWriteMessage(ModbusAgentLink agentLink, AttributeEvent event, Object processedValue) {
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
        synchronized (modbusExecutor.requestLock) {
            if (pendingRequest != null) {
                pendingRequest.cancel(true);
                pendingRequest = null;
            }
        }
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
    public ModbusSerialFrame sendModbusRequest(int unitId, byte[] pdu, long timeoutMs) throws Exception {
        // sendLock ensures only one request-response cycle can be active at a time
        synchronized (sendLock) {
            if (client == null || client.getConnectionStatus() != ConnectionStatus.CONNECTED) {
                throw new IllegalStateException("Client not connected");
            }

            // Wait for minimum interval between requests (Modbus RTU requires gaps)
            long timeSinceLastRequest = System.currentTimeMillis() - lastRequestTime;
            if (timeSinceLastRequest < MIN_REQUEST_INTERVAL_MS) {
                try {
                    Thread.sleep(MIN_REQUEST_INTERVAL_MS - timeSinceLastRequest);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Interrupted while waiting for request interval", e);
                }
            }

            // Create frame and future for response
            ModbusSerialFrame request = new ModbusSerialFrame(unitId, pdu);
            CompletableFuture<ModbusSerialFrame> responseFuture = new CompletableFuture<>();

            // Register pending request (use requestLock for thread-safe access)
            synchronized (modbusExecutor.requestLock) {
                pendingRequest = responseFuture;
            }

            try {
                // Send request
                client.sendMessage(request);
                lastRequestTime = System.currentTimeMillis();
                LOG.finest(() -> "Sent Modbus Serial request - UnitID: " + unitId +
                        ", FC: 0x" + Integer.toHexString(pdu[0] & 0xFF));

                // Wait for response (NOT holding requestLock so handler can complete the future)
                ModbusSerialFrame response = responseFuture.get(timeoutMs, TimeUnit.MILLISECONDS);
                LOG.finest(() -> "Received Modbus Serial response - UnitID: " + response.getUnitId() +
                        ", FC: 0x" + Integer.toHexString(response.getFunctionCode() & 0xFF));
                return response;
            } catch (TimeoutException e) {
                LOG.warning("Modbus Serial request timeout - UnitID: " + unitId);
                throw e;
            } finally {
                // Clear pending request
                synchronized (modbusExecutor.requestLock) {
                    pendingRequest = null;
                }
            }
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
        return "Modbus Serial";
    }

}
