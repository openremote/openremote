/*
 * Copyright 2017, OpenRemote Inc.
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
package org.openremote.test.protocol.velbus;

import io.netty.channel.ChannelHandler;
import org.openremote.agent.protocol.io.IOClient;
import org.openremote.agent.protocol.io.NettyIOClient;
import org.openremote.agent.protocol.velbus.VelbusPacket;
import org.openremote.model.asset.agent.ConnectionStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class MockVelbusClient implements NettyIOClient<VelbusPacket> {
    protected final List<Consumer<VelbusPacket>> messageConsumers = new ArrayList<>();
    protected final List<Consumer<ConnectionStatus>> statusConsumers = new ArrayList<>();
    protected List<String> sentMessages = new ArrayList<>();
    protected ConnectionStatus connectionStatus = ConnectionStatus.DISCONNECTED;
    protected Map<String, List<String>> mockPackets;

    public void setMockPackets(Map<String, List<String>> mockPackets) {
        this.mockPackets = mockPackets;
    }

    public void setConnectionStatus(ConnectionStatus connectionStatus) {
        this.connectionStatus = connectionStatus;

        statusConsumers.forEach(
            consumer -> consumer.accept(connectionStatus)
        );
    }

    @Override
    public void sendMessage(VelbusPacket message) {
        sentMessages.add(message.toString());

        // If a mock packet mapping exists for this then return the result
        if (mockPackets != null) {
            List<String> mappings = mockPackets.get(message.toString());
            if (mappings != null) {
                synchronized (messageConsumers) {
                    mappings.forEach(returnPacket -> {
                        messageConsumers.forEach(consumer ->
                            consumer.accept(VelbusPacket.fromString(returnPacket))
                        );
                    });
                }
            }
        }
    }

    public void onMessageReceived(VelbusPacket message) {
        synchronized (messageConsumers) {
            messageConsumers.forEach(consumer -> {
                consumer.accept(message);
            });
        }
    }

    @Override
    public void addMessageConsumer(Consumer<VelbusPacket> messageConsumer) {
        synchronized (messageConsumers) {
            messageConsumers.add(messageConsumer);
        }
    }

    @Override
    public void removeMessageConsumer(Consumer<VelbusPacket> messageConsumer) {
        synchronized (messageConsumers) {
            messageConsumers.remove(messageConsumer);
        }
    }

    @Override
    public void removeAllMessageConsumers() {
        synchronized (messageConsumers) {
            messageConsumers.clear();
        }
    }

    @Override
    public void addConnectionStatusConsumer(Consumer<ConnectionStatus> connectionStatusConsumer) {
        statusConsumers.add(connectionStatusConsumer);
    }

    @Override
    public void removeConnectionStatusConsumer(Consumer<ConnectionStatus> connectionStatusConsumer) {
        statusConsumers.remove(connectionStatusConsumer);
    }

    @Override
    public void removeAllConnectionStatusConsumers() {
        statusConsumers.clear();
    }

    @Override
    public ConnectionStatus getConnectionStatus() {
        return connectionStatus;
    }

    @Override
    public void connect() {
        setConnectionStatus(ConnectionStatus.CONNECTED);
    }

    @Override
    public void disconnect() {
        setConnectionStatus(ConnectionStatus.DISCONNECTED);
    }

    @Override
    public String getClientUri() {
        return "velbus://mock";
    }

    @Override
    public void setEncoderDecoderProvider(Supplier<ChannelHandler[]> encoderDecoderProvider) throws UnsupportedOperationException {

    }
}
