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

import io.netty.buffer.ByteBuf;
import io.netty.util.CharsetUtil;
import org.openremote.agent.protocol.AbstractProtocol;
import org.openremote.agent.protocol.Protocol;
import org.openremote.agent.protocol.io.IoClient;
import org.openremote.container.Container;
import org.openremote.model.asset.AssetAttribute;
import org.openremote.model.asset.agent.ConnectionStatus;
import org.openremote.model.asset.agent.ProtocolConfiguration;
import org.openremote.model.attribute.*;
import org.openremote.model.syslog.SyslogCategory;
import org.openremote.model.util.TextUtil;
import org.openremote.model.value.Value;
import org.openremote.model.value.ValueFilter;
import org.openremote.model.value.Values;

import java.nio.charset.Charset;
import java.util.*;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.logging.Logger;

import static org.openremote.model.Constants.PROTOCOL_NAMESPACE;
import static org.openremote.model.attribute.MetaItemDescriptor.Access.ACCESS_PRIVATE;
import static org.openremote.model.attribute.MetaItemDescriptorImpl.*;
import static org.openremote.model.syslog.SyslogCategory.PROTOCOL;

/**
 * This is a UDP client protocol for communicating with UDP servers; it uses the {@link AbstractUdpClient} to handle the
 * communication, it is important that all data (sent and received) fits in a single datagram packet.
 * <h1>Protocol Configurations</h1>
 * <p>
 * {@link Attribute}s that are configured as {@link ProtocolConfiguration}s for this protocol support the meta
 * items defined in {@link #PROTOCOL_META_ITEM_DESCRIPTORS}.
 * <h1>Linked Attributes</h1>
 * <p>
 * {@link Attribute}s that are linked to this protocol using an {@link MetaItemType#AGENT_LINK} {@link MetaItem} support
 * the meta items defined in {@link #ATTRIBUTE_META_ITEM_DESCRIPTORS}.
 * <h1>Protocol -> Attribute</h1>
 * <p>
 * When a new value comes from the protocol destined for a linked {@link Attribute} the actual value written to the
 * attribute can be filtered in the standard way using {@link ValueFilter}s via the{@link MetaItemType#META_VALUE_FILTERS}
 * {@link MetaItem}.
 * <h1>Attribute -> Protocol</h1>
 * <p>
 * When a linked {@link Attribute} is written to, the actual value written to the protocol can either be the exact value
 * written to the linked {@link Attribute} or the {@link Protocol#META_ATTRIBUTE_WRITE_VALUE} {@link MetaItem} can be
 * used to inject the written value into a bigger payload using the {@link Protocol#DYNAMIC_VALUE_PLACEHOLDER} and then
 * this bigger payload will be written to the protocol.
 * <h1>Executable Attributes</h1>
 * When a linked {@link Attribute} that has an {@link MetaItemType#EXECUTABLE} {@link MetaItem} is executed the
 * {@link Value} stored in the {@link Protocol#META_ATTRIBUTE_WRITE_VALUE} {@link MetaItem} is actually written to the
 * protocol (note dynamic value injection doesn't work in this scenario as there is no dynamic value to inject).
 * <p>
 * <h1>Protocol Specifics</h1>
 * <p>
 * This is a generic protocol that supports:
 * {@link Protocol#META_PROTOCOL_CONVERT_HEX} or {@link Protocol#META_PROTOCOL_CONVERT_BINARY} to facilitate working
 * with UDP servers that handle binary data.
 */
public class UdpClientProtocol extends AbstractProtocol {

    private class ClientAndQueue {
        IoClient<String> client;
        AttributeRef protocolRef;
        Deque<Runnable> actionQueue = new ArrayDeque<>();
        int retries;
        int timeoutMillis;
        Consumer<String> currentResponseConsumer;
        ScheduledFuture responseMonitor;

        protected ClientAndQueue(IoClient<String> client, AttributeRef protocolRef) {
            this.client = client;
            this.protocolRef = protocolRef;
        }

        protected void connect() {
            LOG.info("Connecting UDP client");
            client.addConnectionStatusConsumer(status -> UdpClientProtocol.this.onConnectionStatusChanged(protocolRef, status));
            client.addMessageConsumer(this::onMessageReceived);
            client.connect();
        }

        protected void disconnect() {
            LOG.info("Disconnecting UDP client");
            client.removeAllMessageConsumers();
            client.removeAllConnectionStatusConsumers();
            client.disconnect();
        }

        protected synchronized void send(String str, Consumer<String> responseConsumer, AttributeInfo attributeInfo) {
            if (currentResponseConsumer != null) {
                actionQueue.add(() -> processNextCommand(str, responseConsumer, attributeInfo));
                return;
            }

            executorService.schedule(() -> processNextCommand(str, responseConsumer, attributeInfo), 0);
        }

        protected void onMessageReceived(String msg) {
            if (currentResponseConsumer != null) {
                currentResponseConsumer.accept(msg);
            }
        }

        protected synchronized void processNextCommand(String str, Consumer<String> responseConsumer, AttributeInfo attributeInfo) {
            currentResponseConsumer = null;

            LOG.fine("Executing send for attribute: " + attributeInfo.attributeRef);

            if (responseConsumer == null) {
                LOG.finer("No response consumer so fire and forget");
                client.sendMessage(str);

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
                        client.sendMessage(str);
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


            client.sendMessage(str);
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


    private static final Logger LOG = SyslogCategory.getLogger(PROTOCOL, UdpClientProtocol.class.getName());
    public static final String PROTOCOL_NAME = PROTOCOL_NAMESPACE + ":udpClient";
    public static final String PROTOCOL_DISPLAY_NAME = "UDP Client";
    public static final String PROTOCOL_VERSION = "1.0";
    private static int DEFAULT_RESPONSE_TIMEOUT_MILLIS = 3000;
    private static int DEFAULT_SEND_RETRIES = 1;
    private static boolean DEFAULT_SERVER_ALWAYS_RESPONDS = false;
    private static int MIN_POLLING_MILLIS = 1000;
    protected final Map<AttributeRef, ClientAndQueue> clientMap = new HashMap<>();
    protected final Map<AttributeRef, AttributeInfo> attributeInfoMap = new HashMap<>();

    /**
     * The UDP server host name/IP address
     */
    public static final MetaItemDescriptor META_PROTOCOL_HOST =  metaItemString(
            PROTOCOL_NAME + ":host",
            ACCESS_PRIVATE,
            true,
            TextUtil.REGEXP_PATTERN_STRING_NON_EMPTY_NO_WHITESPACE,
            MetaItemDescriptor.PatternFailure.STRING_EMPTY_OR_CONTAINS_WHITESPACE);

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
        PROTOCOL_NAME+ ":serverAlwaysResponds",
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

    public static final List<MetaItemDescriptor> ATTRIBUTE_META_ITEM_DESCRIPTORS = Arrays.asList(
        META_ATTRIBUTE_WRITE_VALUE,
        MetaItemType.VALUE_FILTERS,
        META_POLLING_MILLIS,
        META_RESPONSE_TIMEOUT_MILLIS,
        META_SEND_RETRIES,
        META_SERVER_ALWAYS_RESPONDS
    );

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
    public String getProtocolName() {
        return PROTOCOL_NAME;
    }

    @Override
    public String getProtocolDisplayName() {
        return PROTOCOL_DISPLAY_NAME;
    }

    @Override
    public String getVersion() {
        return PROTOCOL_VERSION;
    }

    @Override
    protected List<MetaItemDescriptor> getProtocolConfigurationMetaItemDescriptors() {
        return PROTOCOL_META_ITEM_DESCRIPTORS;
    }

    @Override
    protected List<MetaItemDescriptor> getLinkedAttributeMetaItemDescriptors() {
        return ATTRIBUTE_META_ITEM_DESCRIPTORS;
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

        IoClient client = addClient(host, port, bindPort, charset, binaryMode, hexMode);
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

    @Override
    protected void doLinkAttribute(AssetAttribute attribute, AssetAttribute protocolConfiguration) {

        if (!protocolConfiguration.isEnabled()) {
            LOG.info("Protocol configuration is disabled so ignoring: " + protocolConfiguration.getReferenceOrThrow());
            return;
        }

        ClientAndQueue clientAndQueue = clientMap.get(protocolConfiguration.getReferenceOrThrow());

        if (clientAndQueue == null) {
            return;
        }

        final Integer pollingMillis = Values.getMetaItemValueOrThrow(attribute, META_POLLING_MILLIS, false, true)
                .flatMap(Values::getIntegerCoerced)
                .orElse(null);

        final int responseTimeoutMillis = Values.getMetaItemValueOrThrow(attribute, META_RESPONSE_TIMEOUT_MILLIS, false, true)
                .flatMap(Values::getIntegerCoerced)
                .orElseGet(() ->
                        Values.getMetaItemValueOrThrow(protocolConfiguration, META_RESPONSE_TIMEOUT_MILLIS, false, true)
                                .flatMap(Values::getIntegerCoerced)
                                .orElse(DEFAULT_RESPONSE_TIMEOUT_MILLIS)
                );

        final int sendRetries = Values.getMetaItemValueOrThrow(attribute, META_SEND_RETRIES, false, true)
                .flatMap(Values::getIntegerCoerced)
                .orElseGet(() ->
                        Values.getMetaItemValueOrThrow(protocolConfiguration, META_SEND_RETRIES, false, true)
                                .flatMap(Values::getIntegerCoerced)
                                .orElse(DEFAULT_SEND_RETRIES)
                );

        final boolean serverAlwaysResponds = Values.getMetaItemValueOrThrow(
                attribute,
                META_SERVER_ALWAYS_RESPONDS,
                false,
                false
        ).flatMap(Values::getBoolean).orElseGet(() ->
                Values.getMetaItemValueOrThrow(protocolConfiguration, META_SERVER_ALWAYS_RESPONDS, false, false)
                        .flatMap(Values::getBoolean)
                        .orElse(DEFAULT_SERVER_ALWAYS_RESPONDS)
        );

        Consumer<Value> sendConsumer = null;
        ScheduledFuture pollingTask = null;
        AttributeInfo info = new AttributeInfo(attribute.getReferenceOrThrow(), sendRetries, responseTimeoutMillis);

        if (!attribute.isReadOnly()) {
            sendConsumer = Protocol.createDynamicAttributeWriteConsumer(attribute, str ->
                clientAndQueue.send(
                    str,
                    serverAlwaysResponds ? responseStr -> {
                        // Just drop the response; something in the future could be used to verify send was successful
                    } : null,
                    info));
        }

        if (pollingMillis != null && pollingMillis < MIN_POLLING_MILLIS) {
            LOG.warning("Polling ms must be >= " + MIN_POLLING_MILLIS);
            return;
        }

        final String writeValue = Values.getMetaItemValueOrThrow(attribute, META_ATTRIBUTE_WRITE_VALUE, false, true)
            .map(Object::toString).orElse(null);

        if (pollingMillis != null && TextUtil.isNullOrEmpty(writeValue)) {
            LOG.warning("Polling requires the META_ATTRIBUTE_WRITE_VALUE meta item to be set");
            return;
        }

        if (pollingMillis != null) {
            Consumer<String> responseConsumer = str -> {
                LOG.fine("Polling response received updating attribute: " + attribute.getReferenceOrThrow());
                updateLinkedAttribute(
                    new AttributeState(
                        attribute.getReferenceOrThrow(),
                        str != null ? Values.create(str) : null));
            };

            Runnable pollingRunnable = () -> clientAndQueue.send(writeValue, responseConsumer, info);
            pollingTask = schedulePollingRequest(clientAndQueue, attribute.getReferenceOrThrow(), pollingRunnable, pollingMillis);
        }

        attributeInfoMap.put(attribute.getReferenceOrThrow(), info);
        info.pollingTask = pollingTask;
        info.sendConsumer = sendConsumer;
    }

    @Override
    protected void doUnlinkAttribute(AssetAttribute attribute, AssetAttribute protocolConfiguration) {
        AttributeInfo info = attributeInfoMap.remove(attribute.getReferenceOrThrow());

        if (info != null && info.pollingTask != null) {
            info.pollingTask.cancel(false);
        }
    }

    @Override
    protected void processLinkedAttributeWrite(AttributeEvent event, AssetAttribute protocolConfiguration) {
        AttributeInfo info = attributeInfoMap.get(event.getAttributeRef());

        if (info == null || info.sendConsumer == null) {
            LOG.info("Request to write unlinked attribute or attribute that doesn't support writes so ignoring: " + event);
            return;
        }

        AssetAttribute attribute = getLinkedAttribute(event.getAttributeRef());
        AttributeExecuteStatus status = null;

        if (attribute.isExecutable()) {
            status = event.getValue()
                .flatMap(Values::getString)
                .flatMap(AttributeExecuteStatus::fromString)
                .orElse(null);

            if (status != null && status != AttributeExecuteStatus.REQUEST_START) {
                LOG.fine("Unsupported execution status: " + status);
                return;
            }
        }

        Value value = status != null ? null : event.getValue().orElse(null);
        info.sendConsumer.accept(value);

        if (status != null) {
            updateLinkedAttribute(new AttributeState(event.getAttributeRef(), AttributeExecuteStatus.COMPLETED.asValue()));
        }
    }

    protected void onConnectionStatusChanged(AttributeRef protocolRef, ConnectionStatus connectionStatus) {
        updateStatus(protocolRef, connectionStatus);
    }

    protected ScheduledFuture schedulePollingRequest(ClientAndQueue clientAndQueue,
                                                     AttributeRef attributeRef,
                                                     Runnable pollingTask,
                                                     int pollingMillis) {

        LOG.fine("Scheduling polling request on client '" + clientAndQueue.client + "' to execute every " + pollingMillis + " ms for attribute: " + attributeRef);

        return executorService.scheduleWithFixedDelay(pollingTask, 0, pollingMillis, TimeUnit.MILLISECONDS);
    }

    protected IoClient<String> addClient(String host, int port, Integer bindPort, Charset charset, boolean binaryMode, boolean hexMode) {

        LOG.info("Adding UDP client");

        BiConsumer<ByteBuf, List<String>> decoder;
        BiConsumer<String, ByteBuf> encoder;

        if (hexMode) {
            decoder = (buf, messages) -> {
                byte[] bytes = new byte[buf.readableBytes()];
                buf.readBytes(bytes);
                String msg = Protocol.bytesToHexString(bytes);
                messages.add(msg);
            };
            encoder = (message, buf) -> {
                byte[] bytes = Protocol.bytesFromHexString(message);
                buf.writeBytes(bytes);
            };
        } else if (binaryMode) {
            decoder = (buf, messages) -> {
                byte[] bytes = new byte[buf.readableBytes()];
                buf.readBytes(bytes);
                String msg = Protocol.bytesToBinaryString(bytes);
                messages.add(msg);
            };
            encoder = (message, buf) -> {
                byte[] bytes = Protocol.bytesFromBinaryString(message);
                buf.writeBytes(bytes);
            };
        } else {
            final Charset finalCharset = charset != null ? charset : CharsetUtil.UTF_8;
            decoder = (buf, messages) -> {
                String msg = buf.toString(finalCharset);
                messages.add(msg);
                buf.readerIndex(buf.readerIndex() + buf.readableBytes());
            };
            encoder = (message, buf) -> buf.writeBytes(message.getBytes(finalCharset));
        }

        BiConsumer<ByteBuf, List<String>> finalDecoder = decoder;
        BiConsumer<String, ByteBuf> finalEncoder = encoder;

        return new AbstractUdpClient<String>(host, port, bindPort, executorService) {

            @Override
            protected void decode(ByteBuf buf, List<String> messages) {
                finalDecoder.accept(buf, messages);
            }

            @Override
            protected void encode(String message, ByteBuf buf) {
                finalEncoder.accept(message, buf);
            }
        };
    }
}
