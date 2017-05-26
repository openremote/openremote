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
package org.openremote.agent.protocol;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.MessageToByteEncoder;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.util.URISupport;
import org.openremote.container.message.MessageBrokerContext;
import org.openremote.model.syslog.SyslogCategory;
import org.openremote.model.util.TextUtil;

import java.net.SocketAddress;
import java.util.*;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.openremote.model.syslog.SyslogCategory.PROTOCOL;

/**
 * This is a {@link MessageProcessor} implementation for sockets.
 * <p>
 * It uses the camel netty component for managing the socket connection.
 * <p>
 * A limitation of this implementation is that it is not possible to do bi-directional
 * communication over a single socket connection, instead a separate consumer and producer
 * are required which do not share the underlying connection.
 */
public abstract class AbstractSocketCamelMessageProcessor<T> implements MessageProcessor<T> {
    protected class MessageDecoder extends ByteToMessageDecoder {
        protected List<T> messages = new ArrayList<>(1);

        @Override
        protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
            AbstractSocketCamelMessageProcessor.this.decode(in, messages);
            if (!messages.isEmpty()) {
                out.addAll(messages);
                messages.clear();
            }
        }

        @Override
        public void channelActive(ChannelHandlerContext ctx) throws Exception {
            super.channelActive(ctx);
            onConnectionStatusChanged(ConnectionStatus.CONNECTED);
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) throws Exception {
            super.channelInactive(ctx);
            onConnectionStatusChanged(ConnectionStatus.DISCONNECTED);
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
            super.exceptionCaught(ctx, cause);
            LOG.log(Level.SEVERE, "Exception occurred on endpoint: " + consumerEndpointUri.orElse(""), cause);
            onConnectionStatusChanged(ConnectionStatus.ERROR);
        }
    }

    protected class MessageEncoder extends MessageToByteEncoder<T> {
        @Override
        public void connect(ChannelHandlerContext ctx, SocketAddress remoteAddress, SocketAddress localAddress, ChannelPromise promise) throws Exception {
            super.connect(ctx, remoteAddress, localAddress, promise);
        }

        @Override
        protected void encode(ChannelHandlerContext ctx, T msg, ByteBuf out) throws Exception {
            AbstractSocketCamelMessageProcessor.this.encode(msg, out);
        }
    }

    private static final Logger LOG = SyslogCategory.getLogger(PROTOCOL, AbstractSocketCamelMessageProcessor.class);
    protected static final Integer[] instanceCounter = new Integer[1];
    protected static final String registryPrefix = "SocketMessageProcessor-";
    protected final int instanceId;
    protected String host;
    protected int port;
    protected Class<T> clazz;
    protected final List<Consumer<T>> messageConsumers = new ArrayList<>();
    protected final List<Consumer<ConnectionStatus>> connectionStatusConsumers = new ArrayList<>();
    protected ConnectionStatus connectionStatus = ConnectionStatus.DISCONNECTED;
    protected MessageBrokerContext messageBrokerContext;
    protected ProducerTemplate producerTemplate;
    protected Map<String, Object> options;
    protected Optional<String> consumerEndpointUri;
    protected Optional<String> producerEndpointUri;
    protected boolean initialisationDone;

    static {
        instanceCounter[0] = 1;
    }

    public AbstractSocketCamelMessageProcessor(Class<T> clazz, String host, int port, MessageBrokerContext messageBrokerContext, ProducerTemplate producerTemplate, Map<String, Object> options) {
        TextUtil.requireNonNullAndNonEmpty(host);
        Objects.requireNonNull(clazz);
        Objects.requireNonNull(messageBrokerContext);
        Objects.requireNonNull(producerTemplate);
        this.clazz = clazz;
        this.host = host;
        this.port = port;
        this.messageBrokerContext = messageBrokerContext;
        this.producerTemplate = producerTemplate;
        this.options = options != null ? options : new HashMap<>();
        synchronized (instanceCounter) {
            instanceId = instanceCounter[0]++;
        }
    }

    @Override
    public synchronized void connect() {
        if (connectionStatus == ConnectionStatus.CONNECTED) {
            LOG.finest("Already connected");
            return;
        }

        if (!initialisationDone) {
            // Initialise channel pipeline factories
            initialise();
            initialisationDone = true;
        }

        if (!consumerEndpointUri.isPresent()) {
            LOG.fine("No endpoint URI so cannot continue");
            return;
        }

        // Create camel producer and consumer routes
        try {
            messageBrokerContext.addRoutes(new RouteBuilder() {
                @Override
                public void configure() throws Exception {

                    from(consumerEndpointUri.get())
                        .routeId("SocketMessageProcessor" + instanceId)
                        .process(exchange -> {
                            T message = exchange.getIn().getBody(clazz);
                            onMessageReceived(message);
                        });
                }
            });
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Failed to create camel route for this endpoint URI", e);
        }
    }

    @Override
    public synchronized void disconnect() {
        if (connectionStatus == ConnectionStatus.DISCONNECTED) {
            LOG.finest("Already disconnected");
            return;
        }

        try {
            messageBrokerContext.stopRoute("SocketMessageProcessor" + instanceId);
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Failed to stop camel route", e);
        }
    }

    @Override
    public void sendMessage(T message) {
        if (!producerEndpointUri.isPresent()) {
            LOG.fine("Cannot send message as producer endpoint is not present");
            return;
        }

        if (connectionStatus != ConnectionStatus.CONNECTED) {
            LOG.fine("Cannot send message: Status = " + connectionStatus);
            return;
        }

        try {
            producerTemplate.sendBody(producerEndpointUri.get(), message);
            LOG.finer("Message sent");
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Message send failed", e);
        }
    }

    public ConnectionStatus getConnectionStatus() {
        return connectionStatus;
    }

    @Override
    public synchronized void addConnectionStatusConsumer(Consumer<ConnectionStatus> connectionStatusConsumer) {
        if (!connectionStatusConsumers.contains(connectionStatusConsumer)) {
            connectionStatusConsumers.add(connectionStatusConsumer);
        }
    }

    @Override
    public synchronized void removeConnectionStatusConsumer(Consumer<ConnectionStatus> connectionStatusConsumer) {
        connectionStatusConsumers.remove(connectionStatusConsumer);
    }

    @Override
    public synchronized void addMessageConsumer(Consumer<T> messageConsumer) {
        if (!messageConsumers.contains(messageConsumer)) {
            messageConsumers.add(messageConsumer);
        }
    }

    @Override
    public synchronized void removeMessageConsumer(Consumer<T> messageConsumer) {
        messageConsumers.remove(messageConsumer);
    }

    /**
     * Implementations of this message processor need to implement this method in order
     * to decode incoming data.
     * <p>
     * When one or more messages are available in the {@link ByteBuf} then the messages
     * should be constructed and added to the messages list
     */
    protected abstract void decode(ByteBuf buf, List<T> messages) throws Exception;

    protected abstract void encode(T message, ByteBuf buf) throws Exception;

    protected synchronized void initialise() {
/*
        SimpleRegistry registry = messageBrokerContext.getRegistry();
        Map<String, ChannelHandler> encoders = getEncoders();
        Map<String, ChannelHandler> decoders = getDecoders();
        Set<String> encoderNames = null;
        Set<String> decoderNames = null;

        if (encoders != null) {
            encoderNames = encoders.keySet();
            encoders.forEach(
                (name, handler) -> {
                    registry.put(registryPrefix + name, handler);
                }
            );
        }
        if (decoders != null) {
            decoderNames = decoders.keySet();
            decoders.forEach(
                (name, handler) -> {
                    registry.put(registryPrefix + name, handler);
                }
            );
        }

        buildEndpointUris(host, port, encoderNames, decoderNames, options);
*/
    }

/*
    protected Map<String, ChannelHandler> getEncoders() {
        Map<String, ChannelHandler> encoders = new HashMap<>();

        encoders.put("StandardEncoder", new ChannelHandlerFactory() {
            @Override
            public ChannelHandler newChannelHandler() {
                return new MessageEncoder();
            }

            @Override
            public void handlerAdded(ChannelHandlerContext ctx) throws Exception {

            }

            @Override
            public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {

            }

            @Override
            public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {

            }
        });
        return encoders;
    }

    protected Map<String, ChannelHandler> getDecoders() {
        Map<String, ChannelHandler> decoders = new HashMap<>();

        decoders.put("StandardDecoder", new ChannelHandlerFactory() {
            @Override
            public ChannelHandler newChannelHandler() {
                return new MessageDecoder();
            }

            @Override
            public void handlerAdded(ChannelHandlerContext ctx) throws Exception {

            }

            @Override
            public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {

            }

            @Override
            public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {

            }
        });
        return decoders;
    }
*/

    protected synchronized void onMessageReceived(T message) {
        LOG.finer("Message received");
        messageConsumers.forEach(consumer -> {
            consumer.accept(message);
        });
    }

    protected void onConnectionStatusChanged(ConnectionStatus connectionStatus) {
        this.connectionStatus = connectionStatus;

        synchronized (connectionStatusConsumers) {
            connectionStatusConsumers.forEach(
                consumer -> consumer.accept(connectionStatus)
            );
        }
    }

    @SuppressWarnings("unchecked")
    protected void buildEndpointUris(String host, int port, Set<String> encoderNames, Set<String> decoderNames, Map<String, Object> options) {
        try {

            String encodersStr = encoderNames != null ? "&encoders=#" + registryPrefix + String.join(",#" + registryPrefix, encoderNames) : "";
            String decodersStr = decoderNames != null ? "&decoders=#" + registryPrefix + String.join(",#" + registryPrefix, decoderNames) : "";

            // Build endpoint URI with options
            String uri = "netty4:tcp://"
                + host + ":" + port
                + "?disconnectOnNoReply=false"
                + "&clientMode=true"
                + "&sync=false"
                + "&allowDefaultCodec=false"
                + "&keepAlive=true"
                + "&option.child.keepAlive=true"
                + "&disconnect=false";
            uri = URISupport.appendParametersToURI(uri, options);
            producerEndpointUri = Optional.of(uri + encodersStr);
            consumerEndpointUri = Optional.of(uri + decodersStr);
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Unable to construct endpoint URI", e);
            producerEndpointUri = Optional.empty();
            consumerEndpointUri = Optional.empty();
        }
    }
}
