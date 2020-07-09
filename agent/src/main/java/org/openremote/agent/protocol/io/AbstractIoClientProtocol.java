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
package org.openremote.agent.protocol.io;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandler;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.handler.codec.FixedLengthFrameDecoder;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.util.CharsetUtil;
import org.openremote.agent.protocol.AbstractProtocol;
import org.openremote.agent.protocol.Protocol;
import org.openremote.container.Container;
import org.openremote.model.asset.Asset;
import org.openremote.model.asset.AssetAttribute;
import org.openremote.model.asset.agent.ConnectionStatus;
import org.openremote.model.attribute.AttributeEvent;
import org.openremote.model.attribute.AttributeRef;
import org.openremote.model.attribute.MetaItem;
import org.openremote.model.attribute.MetaItemDescriptor;
import org.openremote.model.syslog.SyslogCategory;
import org.openremote.model.value.Value;
import org.openremote.model.value.Values;

import java.nio.charset.Charset;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static org.openremote.model.syslog.SyslogCategory.PROTOCOL;

/**
 * This is an abstract {@link org.openremote.agent.protocol.Protocol} for protocols that require an {@link IoClient}.
 */
public abstract class AbstractIoClientProtocol<T, U extends IoClient<T>> extends AbstractProtocol {

    /**
     * List of protocol {@link MetaItem}s that are used by generic (string based) IO client protocols
     */
    public static final List<MetaItemDescriptor> PROTOCOL_GENERIC_META_ITEM_DESCRIPTORS = Arrays.asList(
        META_PROTOCOL_CHARSET,
        META_PROTOCOL_MAX_LENGTH,
        META_PROTOCOL_DELIMITER,
        META_PROTOCOL_STRIP_DELIMITER,
        META_PROTOCOL_CONVERT_BINARY,
        META_PROTOCOL_CONVERT_HEX
    );

    /**
     * Supplies a set of encoders/decoders that convert from/to {@link String} to/from {@link ByteBuf} based on the generic protocol {@link MetaItem}s
     */
    public static Supplier<ChannelHandler[]> getGenericStringEncodersAndDecoders(AbstractNettyIoClient<String, ?> client, AssetAttribute protocolConfiguration) {

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

        int maxLength = Values.getMetaItemValueOrThrow(
            protocolConfiguration,
            META_PROTOCOL_MAX_LENGTH,
            false,
            false
        ).flatMap(Values::getIntegerCoerced).orElse(Integer.MAX_VALUE);

        List<String> delimiters = Arrays.stream(protocolConfiguration.getMetaItems(META_PROTOCOL_DELIMITER.getUrn()))
            .map(metaItem -> metaItem.getValueAsString().orElse(null))
            .filter(str -> !Objects.isNull(str))
            .collect(Collectors.toList());

        boolean stripDelimiter = Values.getMetaItemValueOrThrow(
            protocolConfiguration,
            META_PROTOCOL_STRIP_DELIMITER,
            false,
            false
        ).flatMap(Values::getBoolean).orElse(false);

        Supplier<ChannelHandler[]> encoderDecoderProvider = () -> {
            List<ChannelHandler> encodersDecoders = new ArrayList<>();

            if (hexMode || binaryMode) {
                encodersDecoders.add(
                    new AbstractNettyIoClient.MessageToByteEncoder<>(
                        String.class,
                        client,
                        (msg, out) -> {
                            byte[] bytes = hexMode ? Protocol.bytesFromHexString(msg) : Protocol.bytesFromBinaryString(msg);
                            out.writeBytes(bytes);
                        }
                    ));

                if (!delimiters.isEmpty()) {
                    ByteBuf[] byteDelimiters = delimiters
                        .stream()
                        .map(delim -> Unpooled.wrappedBuffer(hexMode ? Protocol.bytesFromHexString(delim) : Protocol.bytesFromBinaryString(delim)))
                        .toArray(ByteBuf[]::new);
                    encodersDecoders.add(new DelimiterBasedFrameDecoder(maxLength, stripDelimiter, byteDelimiters));
                } else {
                    encodersDecoders.add(new FixedLengthFrameDecoder(maxLength));
                }

                // Incoming messages will be bytes
                encodersDecoders.add(
                    new AbstractNettyIoClient.ByteToMessageDecoder<>(
                        client,
                        (byteBuf, messages) -> {
                            byte[] bytes = new byte[byteBuf.readableBytes()];
                            byteBuf.readBytes(bytes);
                            String msg = hexMode ? Protocol.bytesToHexString(bytes) : Protocol.bytesToBinaryString(bytes);
                            messages.add(msg);
                        }
                    )
                );
            } else {
                encodersDecoders.add(new StringEncoder(charset));
                if (!delimiters.isEmpty()) {
                    ByteBuf[] byteDelimiters = delimiters
                        .stream()
                        .map(delim -> Unpooled.wrappedBuffer(delim.getBytes(charset)))
                        .toArray(ByteBuf[]::new);
                    encodersDecoders.add(new DelimiterBasedFrameDecoder(maxLength, stripDelimiter, byteDelimiters));
                } else {
                    encodersDecoders.add(new FixedLengthFrameDecoder(maxLength));
                }
                encodersDecoders.add(new StringDecoder(charset));
                encodersDecoders.add(new AbstractNettyIoClient.MessageToMessageDecoder<>(String.class, client));
            }

            return encodersDecoders.toArray(new ChannelHandler[0]);
        };

        return encoderDecoderProvider;
    }

    public static final Logger LOG = SyslogCategory.getLogger(PROTOCOL, AbstractIoClientProtocol.class);
    protected final Map<AttributeRef, ProtocolIoClient<T, U>> protocolIoClientMap = new HashMap<>();

    public class ProtocolIoClient<T, U extends IoClient<T>> {
        public AttributeRef protocolRef;
        public U client;
        public BiConsumer<AttributeRef, ConnectionStatus> connectionStatusConsumer;
        public BiConsumer<AttributeRef, T> messageConsumer;

        public ProtocolIoClient(AttributeRef protocolRef, U client, BiConsumer<AttributeRef, ConnectionStatus> connectionStatusConsumer, BiConsumer<AttributeRef, T> messageConsumer) {
            this.protocolRef = protocolRef;
            this.client = client;
            this.connectionStatusConsumer = connectionStatusConsumer;
            this.messageConsumer = messageConsumer;
        }

        public void connect() {
            client.addConnectionStatusConsumer(status -> {
                if (connectionStatusConsumer != null) {
                    connectionStatusConsumer.accept(protocolRef, status);
                }
            });

            client.addMessageConsumer(msg -> {
                if (messageConsumer != null) {
                    messageConsumer.accept(protocolRef, msg);
                }
            });

            LOG.info("Connecting IO client");
            client.connect();
        }

        protected void disconnect() {
            client.removeAllMessageConsumers();
            client.removeAllConnectionStatusConsumers();
            LOG.info("Disconnecting IO client");
            client.disconnect();
        }

        protected synchronized void send(T message) {
            LOG.fine("Sending message to IO client: " + client.getClientUri());
            client.sendMessage(message);
        }
    }

    @Override
    protected void doStop(Container container) throws Exception {
        super.doStop(container);
        protocolIoClientMap.forEach((ref, client) -> client.disconnect());
    }

    @Override
    protected void doLinkProtocolConfiguration(Asset agent, AssetAttribute protocolConfiguration) {
        final AttributeRef protocolRef = protocolConfiguration.getReferenceOrThrow();

        ProtocolIoClient<T, U> protocolIoClient = null;

        try {
            protocolIoClient = createProtocolClient(protocolConfiguration);
            LOG.fine("Created IO client '" + protocolIoClient.client.getClientUri() + "' for protocol configuration: " + protocolRef);
            protocolIoClient.connect();
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Failed to create IO client for protocol Configuration: " + protocolConfiguration, e);
            updateStatus(protocolConfiguration.getReferenceOrThrow(), ConnectionStatus.ERROR);
        } finally {
            if (protocolIoClient != null) {
                protocolIoClientMap.put(protocolRef, protocolIoClient);
            }
        }
    }

    @Override
    protected void doUnlinkProtocolConfiguration(Asset agent, AssetAttribute protocolConfiguration) {
        AttributeRef protocolRef = protocolConfiguration.getReferenceOrThrow();
        ProtocolIoClient<T, U> protocolIoClient = protocolIoClientMap.remove(protocolRef);

        if (protocolIoClient != null) {
            LOG.fine("Removed IO client '" + protocolIoClient.client.getClientUri() + "' for protocol configuration: " + protocolRef);
            protocolIoClient.disconnect();
        }
    }

    @Override
    protected void processLinkedAttributeWrite(AttributeEvent event, Value processedValue, AssetAttribute protocolConfiguration) {
        ProtocolIoClient<T, U> protocolIoClient = protocolIoClientMap.get(protocolConfiguration.getReferenceOrThrow());
        AssetAttribute attribute = getLinkedAttribute(event.getAttributeRef());

        if (protocolIoClient == null || attribute == null) {
            return;
        }

        T message = createWriteMessage(protocolConfiguration, attribute, event, processedValue);

        if (message == null) {
            LOG.fine("No message produced for attribute event so not sending to IO client '" + protocolIoClient.client.getClientUri() + "': " + event);
            return;
        }

        protocolIoClient.send(message);
    }

    protected ProtocolIoClient<T, U> createProtocolClient(AssetAttribute protocolConfiguration) throws Exception {
        final AttributeRef protocolRef = protocolConfiguration.getReferenceOrThrow();
        U client = createIoClient(protocolConfiguration);
        Supplier<ChannelHandler[]> encoderDecoderProvider = getEncoderDecoderProvider(client, protocolConfiguration);
        client.setEncoderDecoderProvider(encoderDecoderProvider);
        return new ProtocolIoClient<>(protocolRef, client, this::onConnectionStatusChanged, this::onMessageReceived);
    }

    /**
     * Called when the {@link IoClient} {@link ConnectionStatus} changes
     */
    protected void onConnectionStatusChanged(AttributeRef protocolRef, ConnectionStatus connectionStatus) {
        updateStatus(protocolRef, connectionStatus);
    }

    /**
     * Should return an instance of {@link IoClient} for the supplied protocolConfiguration; the configuration of
     * encoders/decoders is handled by the separate call to {@link #getEncoderDecoderProvider}
     */
    protected abstract U createIoClient(AssetAttribute protocolConfiguration) throws Exception;

    protected abstract Supplier<ChannelHandler[]> getEncoderDecoderProvider(U client, AssetAttribute protocolConfiguration);

    /**
     * Called when the {@link IoClient} receives a message from the server
     */
    protected abstract void onMessageReceived(AttributeRef protocolRef, T message);

    /**
     * Generate the actual message to send to the {@link IoClient} for this {@link AttributeEvent}
     */
    protected abstract T createWriteMessage(AssetAttribute protocolConfiguration, AssetAttribute attribute, AttributeEvent event, Value processedValue);
}
