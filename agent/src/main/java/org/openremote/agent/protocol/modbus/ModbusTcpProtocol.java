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
import org.openremote.agent.protocol.io.NettyIOClient;
import org.openremote.agent.protocol.modbus.util.ModbusTcpDecoder;
import org.openremote.agent.protocol.modbus.util.ModbusTcpEncoder;
import org.openremote.agent.protocol.modbus.util.ModbusTcpFrame;
import org.openremote.agent.protocol.tcp.TCPIOClient;
import org.openremote.model.asset.agent.ConnectionStatus;
import org.openremote.model.syslog.SyslogCategory;

import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.logging.Logger;

/**
 * Modbus TCP protocol implementation.
 * Handles TCP-specific transport: transaction ID correlation and full-duplex communication.
 */
public class ModbusTcpProtocol extends AbstractModbusProtocol<ModbusTcpProtocol, ModbusTcpAgent, ModbusTcpFrame> {

    public static final Logger LOG = SyslogCategory.getLogger(SyslogCategory.PROTOCOL, ModbusTcpProtocol.class);

    // Request/response correlation via transaction ID
    private final Map<Integer, CompletableFuture<ModbusTcpFrame>> pendingRequests = new ConcurrentHashMap<>();
    private final AtomicInteger transactionIdCounter = new AtomicInteger(0);

    public ModbusTcpProtocol(ModbusTcpAgent agent) {
        super(agent);
    }

    // ========== Transport-specific implementations ==========

    @Override
    protected NettyIOClient<ModbusTcpFrame> doCreateIoClient() throws Exception {
        String host = agent.getHost().orElseThrow(() -> new IllegalStateException("Host not configured"));
        int port = agent.getPort().orElseThrow(() -> new IllegalStateException("Port not configured"));
        return new TCPIOClient<>(host, port);
    }

    @Override
    protected Supplier<ChannelHandler[]> getEncoderDecoderProvider() {
        return () -> new ChannelHandler[] {
            new ReadTimeoutHandler(30, TimeUnit.SECONDS),
            new ModbusTcpEncoder(),
            new ModbusTcpDecoder(),
            new AbstractNettyIOClient.MessageToMessageDecoder<>(ModbusTcpFrame.class, (AbstractNettyIOClient<ModbusTcpFrame, ?>) client)
        };
    }

    @Override
    protected void onMessageReceived(ModbusTcpFrame frame) {
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
    protected void doTransportStop() {
        pendingRequests.clear();
    }

    @Override
    public ModbusTcpFrame sendModbusRequest(int unitId, byte[] pdu, long timeoutMs) throws Exception {
        int transactionId;
        CompletableFuture<ModbusTcpFrame> responseFuture;

        synchronized (requestLock) {
            if (client == null || client.getConnectionStatus() != ConnectionStatus.CONNECTED) {
                throw new IllegalStateException("Client not connected");
            }

            transactionId = transactionIdCounter.updateAndGet(current -> (current + 1) % 65536);
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
    public String getProtocolName() {
        return "Modbus TCP";
    }
}