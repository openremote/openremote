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
import org.openremote.model.asset.AssetAttribute;
import org.openremote.model.asset.agent.ConnectionStatus;
import org.openremote.model.asset.agent.ProtocolConfiguration;
import org.openremote.model.attribute.*;
import org.openremote.model.syslog.SyslogCategory;
import org.openremote.model.util.TextUtil;
import org.openremote.model.value.Value;
import org.openremote.model.value.Values;

import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
 * <p>
 * This is a generic protocol that supports
 * {@link org.openremote.agent.protocol.Protocol#META_PROTOCOL_CONVERT_HEX} or
 * {@link org.openremote.agent.protocol.Protocol#META_PROTOCOL_CONVERT_BINARY} to facilitate working with UDP servers
 * that handle binary data.
 * <p>
 * <h1>Protocol Configurations</h1>
 * An instance is created by defining a {@link ProtocolConfiguration} with the following {@link MetaItem}s:
 * <ul>
 * <li>{@link #META_PROTOCOL_HOST} <b>(required)</b></li>
 * <li>{@link #META_PROTOCOL_PORT} <b>(required)</b></li>
 * <li>{@link #META_PROTOCOL_BIND_PORT}</li>
 * <li>{@link #META_PROTOCOL_CHARSET} (defaults to UTF8 if not specified)</li>
 * </ul>
 * <p>
 * <h1>Linked Attributes</h1>
 * {@link Attribute}s that are linked to this protocol using an {@link MetaItemType#AGENT_LINK} {@link MetaItem} support
 * the following meta items:
 * <ul>
 * <li>{@link #META_ATTRIBUTE_WRITE_VALUE} (if specified also supports dynamic value injection as described in {@link Protocol})</li>
 * <li>{@link #META_VALUE_FILTERS}</li>
 * <li>{@link #META_POLLING_MILLIS}</li>
 * </ul>
 *
 */
public class UdpClientProtocol extends AbstractProtocol {

    private static class AttributeInfo {
        Consumer<Value> sendConsumer;
        ScheduledFuture pollingTask;
        int retries;
        boolean expectResponse;

        public AttributeInfo(Consumer<Value> sendConsumer, ScheduledFuture pollingTask, int retries, boolean expectResponse) {
            this.sendConsumer = sendConsumer;
            this.pollingTask = pollingTask;
            this.retries = retries;
            this.expectResponse = expectResponse;
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
    protected final Map<AttributeRef, IoClient<String>> clientMap = new HashMap<>();
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
    MetaItemDescriptor META_SERVER_ALWAYS_RESPONDS = metaItemFixedBoolean(PROTOCOL_NAMESPACE + ":serverAlwaysResponds", ACCESS_PRIVATE, false);

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

        Consumer<ConnectionStatus> connectionStatusConsumer = connectionStatus ->
                onConnectionStatusChanged(protocolRef, connectionStatus);

        Consumer<String> messageConsumer = message -> onClientMessageReceived(protocolRef, message);

        IoClient client = addClient(host, port, bindPort, charset, binaryMode, hexMode, messageConsumer, connectionStatusConsumer);
        clientMap.put(protocolRef, client);
        connectClient(protocolRef, client);
    }

    @Override
    protected void doUnlinkProtocolConfiguration(AssetAttribute protocolConfiguration) {
        final AttributeRef protocolRef = protocolConfiguration.getReferenceOrThrow();

        IoClient<String> client = clientMap.remove(protocolRef);
        removeClient(client);
        disconnectClient(protocolRef, client);
        updateStatus(protocolRef, ConnectionStatus.DISCONNECTED);
    }

    @Override
    protected void doLinkAttribute(AssetAttribute attribute, AssetAttribute protocolConfiguration) {

        if (!protocolConfiguration.isEnabled()) {
            LOG.info("Protocol configuration is disabled so ignoring: " + protocolConfiguration.getReferenceOrThrow());
            return;
        }

        IoClient<String> client = clientMap.get(protocolConfiguration.getReferenceOrThrow());

        if (client == null) {
            return;
        }

        final String writeValue = Values.getMetaItemValueOrThrow(attribute, META_ATTRIBUTE_WRITE_VALUE, true, true)
                .map(Object::toString).orElse(null);

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

        if (!attribute.isReadOnly()) {
            sendConsumer = createWriteConsumer(client, attribute.getReferenceOrThrow(), writeValue, null);
        }

        if (pollingMillis != null && pollingMillis < MIN_POLLING_MILLIS) {
            LOG.warning("Polling ms must be >= " + MIN_POLLING_MILLIS);
            return;
        }

        if (pollingMillis != null && TextUtil.isNullOrEmpty(writeValue)) {
            LOG.warning("Polling requires the META_ATTRIBUTE_WRITE_VALUE meta item to be set");
            return;
        }

        if (pollingMillis != null) {
            Consumer<String> responseConsumer = str ->
                    updateLinkedAttribute(
                            new AttributeState(
                                    attribute.getReferenceOrThrow(),
                                    str != null ? Values.create(str) : null));

            Consumer<Value> pollingWriter = createWriteConsumer(client, attribute.getReferenceOrThrow(), writeValue, responseConsumer);
            Runnable pollingRunnable = () -> pollingWriter.accept(null);
            pollingTask = schedulePollingRequest(client, attribute.getReferenceOrThrow(), pollingRunnable, pollingMillis);
        }

        attributeInfoMap.put(attribute.getReferenceOrThrow(), new AttributeInfo(sendConsumer, pollingTask, sendRetries, serverAlwaysResponds));
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

        info.sendConsumer.accept(event.getValue().orElse(null));
    }

    protected Consumer<Value> createWriteConsumer(IoClient<String> client, AttributeRef attributeRef, String writeValue, Consumer<String> responseConsumer) {
        return value -> {

            String str = value != null ? value.toString() : "";

            if (!TextUtil.isNullOrEmpty(writeValue) && writeValue.contains(DYNAMIC_VALUE_PLACEHOLDER)) {
                str = writeValue.replaceAll(DYNAMIC_VALUE_PLACEHOLDER_REGEXP, str);
            }

            onClientWriteRequest(client, attributeRef, str, responseConsumer);
        };
    }

    protected void onConnectionStatusChanged(AttributeRef protocolRef, ConnectionStatus connectionStatus) {
        updateStatus(protocolRef, connectionStatus);
    }

    protected void onClientWriteRequest(IoClient<String> client, AttributeRef attributeRef, String value, Consumer<String> responseConsumer) {

    }

    protected void onClientMessageReceived(AttributeRef protocolRef, String message) {

    }

    protected ScheduledFuture schedulePollingRequest(IoClient<String> client,
                                                     AttributeRef attributeRef,
                                                     Runnable pollingTask,
                                                     int pollingMillis) {

        LOG.fine("Scheduling polling request on client '" + client + "' to execute every " + pollingMillis + " seconds for attribute: " + attributeRef);

        return executorService.scheduleWithFixedDelay(pollingTask, 0, pollingMillis, TimeUnit.MILLISECONDS);
    }

    protected IoClient<String> addClient(String host, int port, Integer bindPort, Charset charset, boolean binaryMode, boolean hexMode, Consumer<String> messageConsumer, Consumer<ConnectionStatus> statusConsumer) {

        LOG.info("Adding UDP client");

        BiConsumer<ByteBuf, List<String>> decoder;
        BiConsumer<String, ByteBuf> encoder;

        if (hexMode) {
            decoder = (buf, messages) -> {
                byte[] bytes = new byte[buf.readableBytes()];
                buf.readBytes(bytes);
                String msg = Protocol.bytesToHexString(bytes);
                buf.release();
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
                buf.release();
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
                buf.release();
                messages.add(msg);
            };
            encoder = (message, buf) -> buf.writeBytes(message.getBytes(finalCharset));
        }

        BiConsumer<ByteBuf, List<String>> finalDecoder = decoder;
        BiConsumer<String, ByteBuf> finalEncoder = encoder;

        final IoClient<String> client = new AbstractUdpClient<String>(host, port, bindPort, executorService) {

            @Override
            protected void decode(ByteBuf buf, List<String> messages) {
                finalDecoder.accept(buf, messages);
            }

            @Override
            protected void encode(String message, ByteBuf buf) {
                finalEncoder.accept(message, buf);
            }
        };

        client.addMessageConsumer(messageConsumer);
        client.addConnectionStatusConsumer(statusConsumer);
        return client;
    }

    protected void removeClient(IoClient client) {
        if (client == null) {
            return;
        }

        LOG.info("Removing UDP client");
        client.removeAllMessageConsumers();
        client.removeAllConnectionStatusConsumers();
    }

    protected void connectClient(AttributeRef protocolRef, IoClient client) {
        if (client == null) {
            return;
        }
        LOG.info("Connecting UDP client");
        client.connect();
    }

    protected void disconnectClient(AttributeRef protocolRef, IoClient client) {
        if (client == null) {
            return;
        }
        LOG.info("Disconnecting UDP client");
        client.disconnect();
    }
}
