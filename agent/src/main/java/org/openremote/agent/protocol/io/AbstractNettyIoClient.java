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

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import org.openremote.agent.protocol.ProtocolExecutorService;
import org.openremote.model.asset.agent.ConnectionStatus;
import org.openremote.model.syslog.SyslogCategory;

import javax.validation.constraints.NotNull;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.openremote.model.syslog.SyslogCategory.PROTOCOL;

/**
 * This is a {@link IoClient} implementation for netty.
 * <p>
 * It uses the netty component for managing the connection.
 * <p>
 * Concrete implementations are responsible for providing the {@link ChannelOutboundHandler}s and
 * {@link ChannelInboundHandler}s required to encode/decode the specific type of messages sent/received to/from this
 * client. For {@link IoClient}s that require some specific encoders/decoders irrespective of the message type (e.g.
 * {@link org.openremote.agent.protocol.udp.UdpIoClient} and {@link org.openremote.agent.protocol.websocket.WebsocketIoClient})
 * the {@link #addEncodersDecoders} method can be overridden so the {@link IoClient} can exactly control
 * the order and types of encoders/decoders in the pipeline.
 * <p>
 * Users of the {@link IoClient} can add encoders/decoders for their specific message type using the
 * {@link #setEncoderDecoderProvider}, each {@link IoClient} should make it clear to users what the required output
 * type(s) are for the last encoder and decoder that a user may wish to add, if adding encoders/decoders is not
 * supported then {@link IoClient}s should override this setter and throw an {@link UnsupportedOperationException}.
 * <p>
 * Typically for outgoing messages a single {@link ChannelOutboundHandler} is sufficient and the
 * {@link MessageToByteEncoder} can be used as a base.
 * <p>
 * For inbound messages; the decoders required are very much dependent on the message type and {@link IoClient} type,
 * any number of standard netty {@link ChannelInboundHandler}s can be used but the last handler should build messages
 * of type &lt;T&gt; and pass them to the {@link #onMessageReceived} method of the client; the {@link ByteToMessageDecoder}
 * or {@link MessageToByteEncoder} can be used for this purpose, which one to use will depend on the previous
 * {@link ChannelInboundHandler}s in the pipeline.
 * <p>
 * <b>NOTE: Care must be taken when working with Netty {@link ByteBuf} as Netty uses reference counting to manage their
 * lifecycle. Refer to the Netty documentation for more information.</b>
 */
public abstract class AbstractNettyIoClient<T, U extends SocketAddress> implements IoClient<T> {

    /**
     * This is intended to be used at the end of a decoder chain where the previous decoder outputs a {@link ByteBuf};
     * the provided {@link #decoder} should extract the messages of type &lt;T&gt; from the {@link ByteBuf} and add them
     * to the {@link List} and they will then be passed to the {@link IoClient}.
     */
    public static class ByteToMessageDecoder<T> extends io.netty.handler.codec.ByteToMessageDecoder {
        protected List<T> messages = new ArrayList<>(1);
        protected AbstractNettyIoClient<T, ?> client;
        protected BiConsumer<ByteBuf, List<T>> decoder;

        public ByteToMessageDecoder(AbstractNettyIoClient<T, ?> client, @NotNull BiConsumer<ByteBuf, List<T>> decoder) {
            this.client = client;
            this.decoder = decoder;
        }

        @Override
        protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
            decoder.accept(in, messages);

            if (!messages.isEmpty()) {
                // Don't pass them along the channel pipeline just consume them
                messages.forEach(m -> client.onMessageReceived(m));
                messages.clear();
            }
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
            super.exceptionCaught(ctx, cause);
            client.onDecodeException(ctx, cause);
        }
    }

    /**
     * This is intended to be used at the end of a decoder chain where the previous decoder outputs messages of type &lt;T&gt;.
     */
    public static class MessageToMessageDecoder<T> extends SimpleChannelInboundHandler<T> {
        protected AbstractNettyIoClient<T,?> client;

        public MessageToMessageDecoder(Class<? extends T> typeClazz, AbstractNettyIoClient<T, ?> client) {
            super(typeClazz);
            this.client = client;
        }

        @Override
        public void channelRead0(ChannelHandlerContext ctx, T msg) throws Exception {
            client.onMessageReceived(msg);
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
            super.exceptionCaught(ctx, cause);
            client.onDecodeException(ctx, cause);
        }
    }

    /**
     * Concrete implementations must provide an encoder to fill the {@link ByteBuf} ready to be sent `over the wire`.
     */
    public static class MessageToByteEncoder<T> extends io.netty.handler.codec.MessageToByteEncoder<T> {
        protected AbstractNettyIoClient<T, ?> client;
        protected BiConsumer<T, ByteBuf> encoder;

        public MessageToByteEncoder(Class<? extends T> typeClazz, AbstractNettyIoClient<T, ?> client, BiConsumer<T, ByteBuf> encoder) {
            super(typeClazz);
            this.client = client;
            this.encoder = encoder;
        }

        @Override
        protected void encode(ChannelHandlerContext ctx, T msg, ByteBuf out) {
            encoder.accept(msg, out);
        }

        @SuppressWarnings("deprecation")
        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
            super.exceptionCaught(ctx, cause);
            client.onEncodeException(ctx, cause);
        }
    }

    private static final Logger LOG = SyslogCategory.getLogger(PROTOCOL, AbstractNettyIoClient.class);
    protected final static int INITIAL_RECONNECT_DELAY_MILLIS = 1000;
    protected final static int MAX_RECONNECT_DELAY_MILLIS = 60000;
    protected final static int RECONNECT_BACKOFF_MULTIPLIER = 2;
    protected final List<Consumer<T>> messageConsumers = new ArrayList<>();
    protected final List<Consumer<ConnectionStatus>> connectionStatusConsumers = new ArrayList<>();
    protected ConnectionStatus connectionStatus = ConnectionStatus.DISCONNECTED;
    protected ChannelFuture channelFuture;
    protected Channel channel;
    protected Bootstrap bootstrap;
    protected EventLoopGroup workerGroup;
    protected ProtocolExecutorService executorService;
    protected ScheduledFuture reconnectTask;
    protected int reconnectDelayMilliseconds = INITIAL_RECONNECT_DELAY_MILLIS;
    protected boolean permanentError;
    protected Supplier<ChannelHandler[]> encoderDecoderProvider;

    protected AbstractNettyIoClient(ProtocolExecutorService executorService) {
        this.executorService = executorService;
    }

    @Override
    public void setEncoderDecoderProvider(Supplier<ChannelHandler[]> encodersProvider) throws UnsupportedOperationException {
        this.encoderDecoderProvider = encodersProvider;
    }

    protected abstract Class<? extends Channel> getChannelClass();

    protected abstract EventLoopGroup getWorkerGroup();

    protected abstract ChannelFuture startChannel();

    protected void configureChannel() {
        bootstrap.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 2000);
    }

    @Override
    public synchronized void connect() {
        if (permanentError) {
            LOG.fine("Unable to connect as permanent error has been set");
            return;
        }

        if (connectionStatus != ConnectionStatus.DISCONNECTED && connectionStatus != ConnectionStatus.WAITING) {
            LOG.finest("Must be disconnected before calling connect: " + getClientUri());
            return;
        }

        LOG.fine("Connecting IO Client: " + getClientUri());
        onConnectionStatusChanged(ConnectionStatus.CONNECTING);

        if (workerGroup == null) {
            // TODO: In Netty 5 you can pass in an executor service; can only pass in thread factory for now
            workerGroup = getWorkerGroup();
        }

        bootstrap = new Bootstrap();
        bootstrap.channel(getChannelClass());
        configureChannel();
        bootstrap.group(workerGroup);

        bootstrap.handler(new ChannelInitializer() {
            @Override
            public void initChannel(Channel channel) {
                AbstractNettyIoClient.this.initChannel(channel);
            }
        });

        // Start and store the channel
        channelFuture = startChannel();
        channel = channelFuture.channel();

        // Add channel callback - this gets called when the channel connects or when channel encounters an error
        channelFuture.addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) {
                synchronized (AbstractNettyIoClient.this) {
                    channelFuture.removeListener(this);

                    if (connectionStatus == ConnectionStatus.DISCONNECTING) {
                        return;
                    }

                    if (future.isSuccess()) {
                        LOG.log(Level.INFO, "Connection initialising: " + getClientUri());
                        reconnectTask = null;
                        reconnectDelayMilliseconds = INITIAL_RECONNECT_DELAY_MILLIS;
                    } else if (future.cause() != null) {
                        LOG.log(Level.WARNING, "Connection error: " + getClientUri(), future.cause());
                        // Failed to connect so schedule reconnection attempt
                        scheduleReconnect();
                    }
                }
            }
        });

        // Add closed callback
        channel.closeFuture().addListener(future -> {
            if (connectionStatus != ConnectionStatus.DISCONNECTING && connectionStatus != ConnectionStatus.DISCONNECTED) {
                scheduleReconnect();
            }
        });
    }

    @Override
    public synchronized void disconnect() {
        if ((permanentError && connectionStatus == ConnectionStatus.ERROR) || connectionStatus == ConnectionStatus.DISCONNECTING || connectionStatus == ConnectionStatus.DISCONNECTED) {
            LOG.finest("Already disconnecting or disconnected: " + getClientUri());
            return;
        }

        LOG.finest("Disconnecting IO client: " + getClientUri());
        onConnectionStatusChanged(permanentError ? ConnectionStatus.ERROR : ConnectionStatus.DISCONNECTING);

        try {
            if (reconnectTask != null) {
                reconnectTask.cancel(false);
            }

            if (channelFuture != null) {
                channelFuture.cancel(true);
                channelFuture = null;
            }

            // Close the channel
            if (channel != null) {
                channel.disconnect();
                channel.close();
                channel = null;
            }

        } finally {
            if (workerGroup != null) {
                workerGroup.shutdownGracefully();
                workerGroup = null;
            }
            onConnectionStatusChanged(ConnectionStatus.DISCONNECTED);
        }
    }

    @Override
    public void sendMessage(T message) {
        if (connectionStatus != ConnectionStatus.CONNECTED) {
            LOG.warning("Cannot send message: Status = " + connectionStatus + ": " + getClientUri());
            return;
        }

        try {
            // Don't block here as it can cause deadlock
            channel.writeAndFlush(message);
            LOG.finest("Message sent to server: " + getClientUri());
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Message send failed: " + getClientUri(), e);
        }
    }

    @Override
    public ConnectionStatus getConnectionStatus() {
        return connectionStatus;
    }

    @Override
    public void addConnectionStatusConsumer(Consumer<ConnectionStatus> connectionStatusConsumer) {
        synchronized (connectionStatusConsumers) {
            if (!connectionStatusConsumers.contains(connectionStatusConsumer)) {
                connectionStatusConsumers.add(connectionStatusConsumer);
            }
        }
    }

    @Override
    public void removeConnectionStatusConsumer(Consumer<ConnectionStatus> connectionStatusConsumer) {
        synchronized (connectionStatusConsumers) {
            connectionStatusConsumers.remove(connectionStatusConsumer);
        }
    }

    @Override
    public void removeAllConnectionStatusConsumers() {
        synchronized (connectionStatusConsumers) {
            connectionStatusConsumers.clear();
        }
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

    @Override
    public synchronized void removeAllMessageConsumers() {
        messageConsumers.clear();
    }

    /**
     * Inserts the decoders and encoders into the channel pipeline
     */
    protected void initChannel(Channel channel) {
        channel.pipeline().addLast(new ChannelInboundHandlerAdapter() {
            @Override
            public void channelActive(ChannelHandlerContext ctx) throws Exception {
                synchronized (AbstractNettyIoClient.this) {
                    LOG.fine("Connected: " + getClientUri());
                    onConnectionStatusChanged(ConnectionStatus.CONNECTED);
                }
                super.channelActive(ctx);
            }

            @Override
            public void channelInactive(ChannelHandlerContext ctx) throws Exception {
                synchronized (AbstractNettyIoClient.this) {
                    if (connectionStatus != ConnectionStatus.DISCONNECTING) {
                        // This is a connection failure so ignore as reconnect logic will handle it
                        return;
                    }

                    LOG.fine("Disconnected: " + getClientUri());
                    onConnectionStatusChanged(ConnectionStatus.DISCONNECTED);
                }
                super.channelInactive(ctx);
            }
        });
        addEncodersDecoders(channel);
    }

    protected void addEncodersDecoders(Channel channel) {
        if (encoderDecoderProvider != null) {
            ChannelHandler[] handlers = encoderDecoderProvider.get();
            if (handlers != null) {
                Arrays.stream(handlers).forEach(
                    handler -> channel.pipeline().addLast(handler)
                );
            }
        }
    }

    protected void onMessageReceived(T message) {
        if (connectionStatus != ConnectionStatus.CONNECTED) {
            return;
        }

        LOG.finest("Message received notifying consumers: " + getClientUri());
        messageConsumers.forEach(consumer -> consumer.accept(message));
    }

    protected void onDecodeException(ChannelHandlerContext ctx, Throwable cause) {
        LOG.log(Level.SEVERE, "Exception occurred on in-bound message: " + getClientUri(), cause);
        onConnectionStatusChanged(ConnectionStatus.ERROR);
    }

    protected void onEncodeException(ChannelHandlerContext ctx, Throwable cause) {
        LOG.log(Level.SEVERE, "Exception occurred on out-bound message: " + getClientUri(), cause);
        onConnectionStatusChanged(ConnectionStatus.ERROR);
    }

    protected synchronized void onConnectionStatusChanged(ConnectionStatus connectionStatus) {
        this.connectionStatus = connectionStatus;

        synchronized (connectionStatusConsumers) {
            connectionStatusConsumers.forEach(
                consumer -> consumer.accept(connectionStatus)
            );
        }
    }

    protected synchronized void scheduleReconnect() {
        if (reconnectTask != null) {
            return;
        }

        onConnectionStatusChanged(ConnectionStatus.WAITING);

        if (reconnectDelayMilliseconds < MAX_RECONNECT_DELAY_MILLIS) {
            reconnectDelayMilliseconds *= RECONNECT_BACKOFF_MULTIPLIER;
            reconnectDelayMilliseconds = Math.min(MAX_RECONNECT_DELAY_MILLIS, reconnectDelayMilliseconds);
        }

        LOG.finest("Scheduling reconnection in '" + reconnectDelayMilliseconds + "' milliseconds: " + getClientUri());

        reconnectTask = executorService.schedule(() -> {
            synchronized (AbstractNettyIoClient.this) {
                reconnectTask = null;

                // Attempt to reconnect if not disconnecting
                if (connectionStatus != ConnectionStatus.DISCONNECTING && connectionStatus != ConnectionStatus.DISCONNECTED) {
                    connect();
                }
            }
        }, reconnectDelayMilliseconds);
    }

    protected synchronized void setPermanentError(String message) {
        if (permanentError) {
            return;
        }

        LOG.info("An unrecoverable error has occurred with client '" + getClientUri() + "' is no longer usable: " + message);
        this.permanentError = true;
        disconnect();
    }

    @Override
    public String toString() {
        return getClientUri();
    }
}
