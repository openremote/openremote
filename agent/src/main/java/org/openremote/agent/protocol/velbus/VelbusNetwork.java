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
package org.openremote.agent.protocol.velbus;

import org.openremote.agent.protocol.*;
import org.openremote.agent.protocol.velbus.device.*;
import org.openremote.model.asset.agent.ConnectionStatus;
import org.openremote.model.value.*;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;

import static org.openremote.agent.protocol.velbus.AbstractVelbusProtocol.*;

public class VelbusNetwork {

    protected static int DELAY_BETWEEN_PACKET_WRITES_MILLISECONDS = 100; // Need to throttle bus writes
    protected final Integer timeInjectionIntervalSeconds;
    protected MessageProcessor<VelbusPacket> messageProcessor;
    protected final Queue<VelbusPacket> messageQueue = new ArrayDeque<>();
    protected List<ScheduledFuture> scheduledTasks = new ArrayList<>();
    protected ScheduledFuture timeInjector;
    protected VelbusDevice[] devices = new VelbusDevice[254];
    protected VelbusDevice[] subAddressDevices = new VelbusDevice[254];
    protected ScheduledFuture queueProcessingTask;
    protected ProtocolExecutorService executorService;
    protected final List<Consumer<ConnectionStatus>> connectionStatusConsumers = new ArrayList<>();

    public VelbusNetwork(MessageProcessor<VelbusPacket> messageProcessor, ProtocolExecutorService executorService, Integer timeInjectionIntervalSeconds) {
        this.messageProcessor = messageProcessor;
        this.executorService = executorService;
        this.timeInjectionIntervalSeconds = timeInjectionIntervalSeconds;
        messageProcessor.addConnectionStatusConsumer(this::onConnectionStatusChanged);
        messageProcessor.addMessageConsumer(this::onPacketReceived);
        onConnectionStatusChanged(getConnectionStatus());
        if (timeInjectionIntervalSeconds != null) {
            timeInjector = getExecutorService().scheduleWithFixedDelay(this::doTimeInjection, timeInjectionIntervalSeconds * 1000, timeInjectionIntervalSeconds * 1000);
        }
    }

    protected void addConnectionStatusConsumer(Consumer<ConnectionStatus> connectionStatusConsumer) {
        synchronized (connectionStatusConsumers) {
            connectionStatusConsumers.add(connectionStatusConsumer);
        }
    }

    protected void removeConnectionStatusConsumer(Consumer<ConnectionStatus> connectionStatusConsumer) {
        synchronized (connectionStatusConsumers) {
            connectionStatusConsumers.remove(connectionStatusConsumer);
        }
    }

    public ProtocolExecutorService getExecutorService() {
        return this.executorService;
    }

    public synchronized void sendPackets(VelbusPacket... packets) {
        if (getConnectionStatus() == ConnectionStatus.CONNECTED) {
            messageQueue.addAll(Arrays.asList(packets));

            if (queueProcessingTask == null) {
                startSendingPackets();
            }
        }
    }

    public void connect() {
        if (messageProcessor == null) {
            return;
        }

        messageProcessor.connect();
    }

    public void disconnect() {
        scheduledTasks.forEach(task -> task.cancel(true));
        scheduledTasks.clear();

        if (messageProcessor != null) {
            messageProcessor.disconnect();
        }
    }

    public void close() {
        if (timeInjector != null) {
            timeInjector.cancel(true);
            timeInjector = null;
        }

        synchronized (connectionStatusConsumers) {
            connectionStatusConsumers.clear();
        }

        disconnect();

        if (messageProcessor != null) {
            messageProcessor.removeConnectionStatusConsumer(this::onConnectionStatusChanged);
            messageProcessor.removeMessageConsumer(this::onPacketReceived);
            messageProcessor = null;
        }
    }

    public ConnectionStatus getConnectionStatus() {
        return messageProcessor.getConnectionStatus();
    }

    protected void onConnectionStatusChanged(ConnectionStatus status) {
        synchronized (connectionStatusConsumers) {
            connectionStatusConsumers.forEach(consumer -> consumer.accept(status));
        }

        if (status == ConnectionStatus.CONNECTED) {
            // Don't process stale messages
            messageQueue.clear();

            // Initialise the devices
            for (int i=0; i<devices.length; i++) {
                if (devices[i] != null) {
                    devices[i].initialise();
                }
            }
        } else {
            // Reset the devices
            for (int i=0; i<devices.length; i++) {
                if (devices[i] != null) {
                    devices[i].reset();
                }
            }
            // Clear out sub device registrations
            for (int i=0; i<subAddressDevices.length; i++) {
                subAddressDevices[i] = null;
            }
        }
    }

    protected void onPacketReceived(VelbusPacket packet) {
        // Forward the packet to the device if it exists
        int address = packet.getAddress();

        if (address > 254 || address < 1) {
            return;
        }

        VelbusDevice matchingDevice = devices[address-1];
        VelbusPacket.InboundCommand command = VelbusPacket.InboundCommand.fromCode(packet.getCommand());
        LOG.finest("Received packet " + command + " : " + packet);

        if (matchingDevice != null) {
            matchingDevice.processReceivedPacket(packet);
        } else {
            // Look for sub address device
            matchingDevice = subAddressDevices[address-1];

            if (matchingDevice != null) {
                matchingDevice.processReceivedPacket(packet);
            }
        }
    }

    public void addPropertyValueConsumer(int deviceAddress, String property, Consumer<DevicePropertyValue> propertyValueConsumer) {
        if (deviceAddress < 1 || deviceAddress > 254) {
            LOG.warning("Invalid device address: " + deviceAddress);
            return;
        }

        VelbusDevice device = getDevice(deviceAddress);

        if (device == null) {
            // Device hasn't been created yet so create it
           device = new VelbusDevice(deviceAddress, this);
            devices[deviceAddress-1] = device;

            if (getConnectionStatus() == ConnectionStatus.CONNECTED) {
                device.initialise();
            }
        }

        device.addPropertyValueConsumer(property, propertyValueConsumer);
    }

    public void removePropertyValueConsumer(int deviceAddress, String property, Consumer<DevicePropertyValue> propertyValueConsumer) {
        if (deviceAddress < 1 || deviceAddress > 254) {
            LOG.warning("Invalid device address: " + deviceAddress);
            return;
        }

        VelbusDevice device = getDevice(deviceAddress);

        if (device != null) {
            device.removePropertyValueConsumer(property, propertyValueConsumer);
        }
    }

    protected void removeAllDevices() {
        for (int i=0; i<devices.length; i++) {
            VelbusDevice device = devices[i];
            if (device != null) {
                device.removeAllPropertyValueConsumers();
            }
            //devices[i] = null;
        }
    }

    public void writeProperty(int deviceAddress, String property, Value value) {
        if (getConnectionStatus() != ConnectionStatus.CONNECTED) {
            return;
        }

        if (deviceAddress < 1 || deviceAddress > 254) {
            LOG.warning("Invalid device address: " + deviceAddress);
            return;
        }

        VelbusDevice device = getDevice(deviceAddress);

        if (device != null) {
            device.writeProperty(property, value);
        }
    }

    /**
     * Used by devices that have sub addresses so they can be associated with another address as well as their base
     * address
     */
    public void registerSubAddress(VelbusDevice velbusDevice, int subAddress) {
        if (subAddress < 1 || subAddress > 254) {
            LOG.warning("Invalid device subaddress '" + subAddress + "' for device: " + velbusDevice.getBaseAddress());
            return;
        }

        subAddressDevices[subAddress-1] = velbusDevice;
    }

    protected VelbusDevice getDevice(int address) {
        return devices[address-1];
    }

    protected synchronized void startSendingPackets() {
        if (queueProcessingTask != null) {
            return;
        }

        queueProcessingTask = getExecutorService().scheduleWithFixedDelay(
            this::doSendPacket,
            0,
            DELAY_BETWEEN_PACKET_WRITES_MILLISECONDS
        );
    }

    protected void doSendPacket() {
        if (getConnectionStatus() == ConnectionStatus.CONNECTED) {
            VelbusPacket packet = messageQueue.poll();
            if (packet != null) {
                VelbusPacket.OutboundCommand command = VelbusPacket.OutboundCommand.fromCode(packet.getCommand());
                LOG.finest("Sending packet " + command + " : " + packet);
                messageProcessor.sendMessage(packet);
            } else {
                queueProcessingTask.cancel(false);
                queueProcessingTask = null;
            }
        }
    }

    public ScheduledFuture scheduleTask(Runnable runnable, int delayMillis) {
        // Remove old completed tasks
        scheduledTasks.removeIf(Future::isDone);

        if (getConnectionStatus() == ConnectionStatus.CONNECTED) {
            ScheduledFuture future = getExecutorService().schedule(runnable, delayMillis);
            scheduledTasks.add(future);
            return future;
        }

        return null;
    }

    protected void doTimeInjection() {
        sendPackets(VelbusDevice.createTimeInjectionPackets());
    }
}
