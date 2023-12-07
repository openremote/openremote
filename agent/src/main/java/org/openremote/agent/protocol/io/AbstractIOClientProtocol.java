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
import io.netty.handler.codec.Delimiters;
import io.netty.handler.codec.FixedLengthFrameDecoder;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.util.CharsetUtil;
import org.openremote.agent.protocol.AbstractProtocol;
import org.openremote.model.Container;
import org.openremote.model.asset.agent.Agent;
import org.openremote.model.asset.agent.AgentLink;
import org.openremote.model.asset.agent.ConnectionStatus;
import org.openremote.model.asset.agent.Protocol;
import org.openremote.model.attribute.Attribute;
import org.openremote.model.attribute.AttributeEvent;
import org.openremote.model.syslog.SyslogCategory;
import org.openremote.model.util.ValueUtil;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.openremote.model.syslog.SyslogCategory.PROTOCOL;

/**
 * This is an abstract {@link Protocol} for protocols that require an {@link IOClient}.
 */
public abstract class AbstractIOClientProtocol<T extends AbstractIOClientProtocol<T, U, V, W, X>, U extends IOAgent<U, T, X>, V, W extends IOClient<V>, X extends AgentLink<?>> extends AbstractProtocol<U, X> {

    /**
     * Supplies a set of encoders/decoders that convert from/to {@link String} to/from {@link ByteBuf} based on the generic protocol {@link Attribute}s
     */
    public static Supplier<ChannelHandler[]> getGenericStringEncodersAndDecoders(AbstractNettyIOClient<String, ?> client, IOAgent<?, ?, ?> agent) {

        boolean hexMode = agent.getMessageConvertHex().orElse(false);
        boolean binaryMode = agent.getMessageConvertBinary().orElse(false);
        Charset charset = agent.getMessageCharset().map(Charset::forName).orElse(CharsetUtil.UTF_8);
        int maxLength = agent.getMessageMaxLength().orElse(Integer.MAX_VALUE);
        String[] delimiters = agent.getMessageDelimiters().orElse(new String[0]);
        boolean stripDelimiter = agent.getMessageStripDelimiter().orElse(false);

        return () -> {
            List<ChannelHandler> encodersDecoders = new ArrayList<>();

            if (hexMode || binaryMode) {
                encodersDecoders.add(
                    new AbstractNettyIOClient.MessageToByteEncoder<>(
                        String.class,
                        client,
                        (msg, out) -> {
                            byte[] bytes = hexMode ? ValueUtil.bytesFromHexString(msg) : ValueUtil.bytesFromBinaryString(msg);
                            out.writeBytes(bytes);
                        }
                    ));

                if (delimiters.length > 0) {
                    ByteBuf[] byteDelimiters = Arrays.stream(delimiters)
                        .map(delim -> Unpooled.wrappedBuffer(hexMode ? ValueUtil.bytesFromHexString(delim) : ValueUtil.bytesFromBinaryString(delim)))
                        .toArray(ByteBuf[]::new);
                    encodersDecoders.add(new DelimiterBasedFrameDecoder(maxLength, stripDelimiter, byteDelimiters));
                } else {
                    encodersDecoders.add(new FixedLengthFrameDecoder(maxLength));
                }

                // Incoming messages will be bytes
                encodersDecoders.add(
                    new AbstractNettyIOClient.ByteToMessageDecoder<>(
                        client,
                        (byteBuf, messages) -> {
                            byte[] bytes = new byte[byteBuf.readableBytes()];
                            byteBuf.readBytes(bytes);
                            String msg = hexMode ? ValueUtil.bytesToHexString(bytes) : ValueUtil.bytesToBinaryString(bytes);
                            messages.add(msg);
                        }
                    )
                );
            } else {
                encodersDecoders.add(new StringEncoder(charset));
                if (agent.getMessageMaxLength().isPresent()) {
                    encodersDecoders.add(new FixedLengthFrameDecoder(maxLength));
                } else {
                    ByteBuf[] byteDelimiters;
                    if (delimiters.length > 0) {
                        byteDelimiters = Arrays.stream(delimiters)
                            .map(delim -> Unpooled.wrappedBuffer(delim.getBytes(charset)))
                            .toArray(ByteBuf[]::new);
                    } else {
                        byteDelimiters = Delimiters.lineDelimiter();
                    }
                    encodersDecoders.add(new DelimiterBasedFrameDecoder(maxLength, stripDelimiter, byteDelimiters));
                }
                encodersDecoders.add(new StringDecoder(charset));
                encodersDecoders.add(new AbstractNettyIOClient.MessageToMessageDecoder<>(String.class, client));
            }

            return encodersDecoders.toArray(new ChannelHandler[0]);
        };
    }

    public static final Logger LOG = SyslogCategory.getLogger(PROTOCOL, AbstractIOClientProtocol.class);
    protected W client;

    protected AbstractIOClientProtocol(U agent) {
        super(agent);
    }

    @Override
    public String getProtocolInstanceUri() {
        return client != null ? client.getClientUri() : "";
    }

    @Override
    protected void doStop(Container container) throws Exception {
        if (client != null) {
            LOG.fine("Stopping IO client for protocol: " + this);
            client.removeAllMessageConsumers();
            client.removeAllConnectionStatusConsumers();
            LOG.info("Disconnecting IO client");
            client.disconnect();
        }
        client = null;
    }

    @Override
    protected void doStart(Container container) throws Exception {
        try {
            client = createIoClient();
            LOG.fine("Created IO client '" + client.getClientUri() + "' for protocol: " + this);
            client.connect();
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Failed to create IO client for protocol: " + this, e);
            setConnectionStatus(ConnectionStatus.ERROR);
        }
    }

    @Override
    protected void doLinkedAttributeWrite(X agentLink, AttributeEvent event, Object processedValue) {

        if (client == null) {
            return;
        }

        V message = createWriteMessage(agent.getAgentLink(event), event, processedValue);

        if (message == null) {
            LOG.fine("No message produced for attribute event so not sending to IO client '" + client.getClientUri() + "': " + event);
            return;
        }

        AbstractIOClientProtocol.LOG.finest("Sending message to IO client: " + client.getClientUri());
        client.sendMessage(message);
    }

    protected W createIoClient() throws Exception {
        W client = doCreateIoClient();

        if (client == null) {
            throw new IllegalStateException("IO client for protocol should not be null");
        }

        client.addConnectionStatusConsumer(this::onConnectionStatusChanged);
        client.addMessageConsumer(this::onMessageReceived);
        this.client = client;
        return client;
    }

    /**
     * Called when the {@link IOClient} {@link ConnectionStatus} changes
     */
    protected void onConnectionStatusChanged(ConnectionStatus connectionStatus) {
        setConnectionStatus(connectionStatus);
    }

    /**
     * Should return an instance of {@link IOClient} for the linked {@link Agent}.
     */
    protected abstract W doCreateIoClient() throws Exception;

    /**
     * Called when the {@link IOClient} receives a message from the server
     */
    protected abstract void onMessageReceived(V message);

    /**
     * Generate the actual message to send to the {@link IOClient} for this {@link AttributeEvent}
     */
    protected abstract V createWriteMessage(X agentLink, AttributeEvent event, Object processedValue);
}
