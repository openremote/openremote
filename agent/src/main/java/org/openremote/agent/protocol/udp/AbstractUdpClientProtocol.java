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
package org.openremote.agent.protocol.udp;

import io.netty.util.CharsetUtil;
import org.openremote.agent.protocol.AbstractProtocol;
import org.openremote.agent.protocol.io.IoClient;
import org.openremote.container.Container;
import org.openremote.model.asset.AssetAttribute;
import org.openremote.model.asset.agent.ConnectionStatus;
import org.openremote.model.attribute.AttributeRef;
import org.openremote.model.attribute.MetaItem;
import org.openremote.model.attribute.MetaItemDescriptor;
import org.openremote.model.syslog.SyslogCategory;
import org.openremote.model.util.TextUtil;
import org.openremote.model.value.Value;
import org.openremote.model.value.Values;

import java.nio.charset.Charset;
import java.util.*;
import java.util.concurrent.ScheduledFuture;
import java.util.function.Consumer;
import java.util.logging.Logger;

import static org.openremote.model.Constants.PROTOCOL_NAMESPACE;
import static org.openremote.model.attribute.MetaItemDescriptor.Access.ACCESS_PRIVATE;
import static org.openremote.model.attribute.MetaItemDescriptorImpl.*;
import static org.openremote.model.syslog.SyslogCategory.PROTOCOL;

/**
 * This is an abstract UDP client protocol for communicating with UDP servers; concrete implementations must provide
 * an {@link IoClient<T> for handling over the wire communication}.
 */
public abstract class AbstractUdpClientProtocol<T> extends AbstractProtocol {

    protected class ClientAndQueue {
        IoClient<T> client;
        AttributeRef protocolRef;
        Deque<Runnable> actionQueue = new ArrayDeque<>();
        int retries;
        int timeoutMillis;
        Consumer<T> currentResponseConsumer;
        ScheduledFuture responseMonitor;

        protected ClientAndQueue(IoClient<T> client, AttributeRef protocolRef) {
            this.client = client;
            this.protocolRef = protocolRef;
        }

        protected void connect() {
            LOG.info("Connecting UDP client");
            client.addConnectionStatusConsumer(status -> AbstractUdpClientProtocol.this.onConnectionStatusChanged(protocolRef, status));
            client.addMessageConsumer(this::onMessageReceived);
            client.connect();
        }

        protected void disconnect() {
            LOG.info("Disconnecting UDP client");
            client.removeAllMessageConsumers();
            client.removeAllConnectionStatusConsumers();
            client.disconnect();
        }

        protected synchronized void send(T str, Consumer<T> responseConsumer, AttributeInfo attributeInfo) {
            if (currentResponseConsumer != null) {
                actionQueue.add(() -> processNextCommand(str, responseConsumer, attributeInfo));
                return;
            }

            executorService.schedule(() -> processNextCommand(str, responseConsumer, attributeInfo), 0);
        }

        protected void onMessageReceived(T msg) {
            if (currentResponseConsumer != null) {
                currentResponseConsumer.accept(msg);
            }
        }

        protected synchronized void processNextCommand(T msg, Consumer<T> responseConsumer, AttributeInfo attributeInfo) {
            currentResponseConsumer = null;

            LOG.fine("Executing send for attribute: " + attributeInfo.attributeRef);

            if (responseConsumer == null) {
                LOG.finer("No response consumer so fire and forget");
                client.sendMessage(msg);

                // Move straight onto next command
                if (!actionQueue.isEmpty()) {
                    executorService.schedule(actionQueue.pop(), 0);
                }
                return;
            }

            LOG.finer("Send expects a response [retries=" + attributeInfo.retries + ", timeout=" + attributeInfo.responseTimeoutMillis + "]");
            this.retries = attributeInfo.retries;
            this.timeoutMillis = attributeInfo.responseTimeoutMillis;
            this.currentResponseConsumer = responseStr -> {
                responseConsumer.accept(responseStr);
                synchronized (ClientAndQueue.this) {
                    if (this.responseMonitor != null) {
                        this.responseMonitor.cancel(true);
                        currentResponseConsumer = null;
                    }

                    if (!actionQueue.isEmpty()) {
                        executorService.schedule(actionQueue.pop(), 0);
                    }
                }
            };

            Runnable failureAction = () -> {
                synchronized (ClientAndQueue.this) {
                    LOG.fine("Send failed: response timeout reached");

                    if (retries > 0) {
                        LOG.fine("Sending message again");
                        retries--;
                        client.sendMessage(msg);
                    } else {
                        LOG.fine("No more retries left so abandoning send");
                        responseMonitor.cancel(false);
                        currentResponseConsumer = null;
                        if (!actionQueue.isEmpty()) {
                            executorService.schedule(actionQueue.pop(), 0);
                        }
                    }
                }
            };

            client.sendMessage(msg);
            responseMonitor = executorService.scheduleWithFixedDelay(failureAction, timeoutMillis, timeoutMillis);
        }
    }

    protected static class AttributeInfo {
        AttributeRef attributeRef;
        Consumer<Value> sendConsumer;
        ScheduledFuture pollingTask;
        int retries;
        int responseTimeoutMillis;

        protected AttributeInfo(AttributeRef attributeRef, int retries, int responseTimeoutMillis) {
            this.attributeRef = attributeRef;
            this.retries = retries;
            this.responseTimeoutMillis = responseTimeoutMillis;
        }
    }

    private static final Logger LOG = SyslogCategory.getLogger(PROTOCOL, AbstractUdpClientProtocol.class.getName());
    public static final String PROTOCOL_NAME = PROTOCOL_NAMESPACE + ":abstractUdpClient";
    protected final Map<AttributeRef, ClientAndQueue> clientMap = new HashMap<>();

    /**
     * The UDP server host name/IP address
     */
    public static final MetaItemDescriptor META_PROTOCOL_HOST =  metaItemString(
            PROTOCOL_NAME + ":host",
            ACCESS_PRIVATE,
            true,
            TextUtil.REGEXP_PATTERN_STRING_NON_EMPTY_NO_WHITESPACE,
            PatternFailure.STRING_EMPTY_OR_CONTAINS_WHITESPACE);

    /**
     * The UDP server port
     */
    public static final MetaItemDescriptor META_PROTOCOL_PORT = metaItemInteger(
            PROTOCOL_NAME + ":port",
            ACCESS_PRIVATE,
            true,
            1,
            65536);

    /**
     * Optionally sets the port that this UDP client will bind to (if not set then a random ephemeral port will be used)
     */
    public static final MetaItemDescriptor META_PROTOCOL_BIND_PORT = metaItemInteger(
            PROTOCOL_NAME + ":bindPort",
            ACCESS_PRIVATE,
            true,
            1,
            65536);

    /**
     * Indicates whether the server replies to each packet not just polling requests (need this to successfully match
     * request and response packets)
     */
    public static final MetaItemDescriptor META_SERVER_ALWAYS_RESPONDS = metaItemFixedBoolean(
        PROTOCOL_NAME + ":serverAlwaysResponds",
        ACCESS_PRIVATE,
        false);

    /**
     * Indicates how long to wait for a response from the server before re-attempting {@link #META_SEND_RETRIES} times
     */
    public static final MetaItemDescriptor META_RESPONSE_TIMEOUT_MILLIS = metaItemInteger(
            PROTOCOL_NAME + ":responseTimeoutMillis",
            ACCESS_PRIVATE,
            true,
            1,
            10000);

    /**
     * How many times to retry sending a failed command
     */
    public static final MetaItemDescriptor META_SEND_RETRIES = metaItemInteger(
            PROTOCOL_NAME + ":sendRetries",
            ACCESS_PRIVATE,
            true,
            1,
            10);


    public static final List<MetaItemDescriptor> PROTOCOL_META_ITEM_DESCRIPTORS = Arrays.asList(
        META_PROTOCOL_HOST,
        META_PROTOCOL_PORT,
        META_PROTOCOL_BIND_PORT,
        META_PROTOCOL_CHARSET,
        META_PROTOCOL_CONVERT_BINARY,
        META_PROTOCOL_CONVERT_HEX,
        META_RESPONSE_TIMEOUT_MILLIS,
        META_SEND_RETRIES,
        META_SERVER_ALWAYS_RESPONDS
    );

    @Override
    protected void doStop(Container container) {
        clientMap.forEach((ref, clientAndQueue) -> clientAndQueue.disconnect());
    }

    @Override
    protected List<MetaItemDescriptor> getProtocolConfigurationMetaItemDescriptors() {
        return PROTOCOL_META_ITEM_DESCRIPTORS;
    }

    @Override
    public AssetAttribute getProtocolConfigurationTemplate() {
        return super.getProtocolConfigurationTemplate()
            .addMeta(
                new MetaItem(META_PROTOCOL_HOST, null),
                new MetaItem(META_PROTOCOL_PORT, null)
            );
    }

    @Override
    protected void doLinkProtocolConfiguration(AssetAttribute protocolConfiguration) {

        if (!protocolConfiguration.isEnabled()) {
            LOG.info("Protocol configuration is disabled so ignoring: " + protocolConfiguration.getReferenceOrThrow());
            return;
        }

        final AttributeRef protocolRef = protocolConfiguration.getReferenceOrThrow();

        String host = Values.getMetaItemValueOrThrow(
            protocolConfiguration,
            META_PROTOCOL_HOST,
            true,
            true
        ).flatMap(Values::getString).orElse(null);

        int port = Values.getMetaItemValueOrThrow(
                protocolConfiguration,
                META_PROTOCOL_PORT,
                true,
                true
        ).flatMap(Values::getIntegerCoerced).orElse(0);

        Integer bindPort = Values.getMetaItemValueOrThrow(
                protocolConfiguration,
                META_PROTOCOL_BIND_PORT,
                false,
                false
        ).flatMap(Values::getIntegerCoerced).orElse(null);

        boolean hexMode = Values.getMetaItemValueOrThrow(
                protocolConfiguration,
                META_PROTOCOL_CONVERT_HEX,
                false,
                false
        ).flatMap(Values::getBoolean).orElse(false);

        boolean binaryMode = Values.getMetaItemValueOrThrow(
                protocolConfiguration,
                META_PROTOCOL_CONVERT_BINARY,
                false,
                false
        ).flatMap(Values::getBoolean).orElse(false);

        Charset charset = Values.getMetaItemValueOrThrow(
                protocolConfiguration,
                META_PROTOCOL_CHARSET,
                false,
                true
        ).flatMap(Values::getString).map(Charset::forName).orElse(CharsetUtil.UTF_8);

        IoClient<T> client = createIoClient(host, port, bindPort, charset, binaryMode, hexMode);
        ClientAndQueue clientAndQueue = new ClientAndQueue(client, protocolRef);
        clientMap.put(protocolRef, clientAndQueue);
        clientAndQueue.connect();
    }

    @Override
    protected void doUnlinkProtocolConfiguration(AssetAttribute protocolConfiguration) {
        final AttributeRef protocolRef = protocolConfiguration.getReferenceOrThrow();

        ClientAndQueue clientAndQueue = clientMap.remove(protocolRef);
        if (clientAndQueue != null) {
            clientAndQueue.disconnect();
            updateStatus(protocolRef, ConnectionStatus.DISCONNECTED);
        }
    }

    protected void onConnectionStatusChanged(AttributeRef protocolRef, ConnectionStatus connectionStatus) {
        updateStatus(protocolRef, connectionStatus);
    }


    protected abstract IoClient<T> createIoClient(String host, int port, Integer bindPort, Charset charset, boolean binaryMode, boolean hexMode);
}
