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

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.MessageToByteEncoder;
import org.openremote.model.syslog.SyslogCategory;
import org.openremote.model.util.TextUtil;

import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.openremote.model.syslog.SyslogCategory.PROTOCOL;

/**
 * This is a {@link MessageProcessor} implementation for sockets.
 * <p>
 * It uses the netty component for managing the socket connection.
 * <p>
 * By default it uses a single {@link ByteToMessageDecoder} to intercept incoming data and to delegate the construction
 * of messages to the abstract {@link #decode} method.
 * <p>
 * For outgoing messages it uses a single
 * {@link MessageToByteEncoder} to delegate the filling of the {@link ByteBuf} to the abstract {@link #encode} method.
 * <p>
 * Consumers wanting to add and/or replace the default encoder/decoder should override {@link #initChannel} and insert
 * the desired {@link ChannelHandler}s into the pipeline.
 * <p>
 * <b>NOTE: Care must be taken when working with Netty {@link ByteBuf} as Netty uses reference counting to manage their
 * lifecycle. Refer to the Netty documentation for more information.</b>
 */
public abstract class AbstractSocketMessageProcessor<T> implements MessageProcessor<T> {

    protected class MessageDecoder extends ByteToMessageDecoder {
        protected List<T> messages = new ArrayList<>(1);

        @Override
        protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
            AbstractSocketMessageProcessor.this.decode(in, messages);
            if (!messages.isEmpty()) {
                // Don't pass them along the channel pipeline just consume them
                messages.forEach(
                    AbstractSocketMessageProcessor.this::onMessageReceived
                );
                messages.clear();
            }
        }

        @Override
        public void channelActive(ChannelHandlerContext ctx) throws Exception {
            super.channelActive(ctx);
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) throws Exception {
            super.channelInactive(ctx);
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
            super.exceptionCaught(ctx, cause);
            LOG.log(Level.SEVERE, "Exception occurred on in-bound message: ", cause);
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
            AbstractSocketMessageProcessor.this.encode(msg, out);
        }
    }

    private static final Logger LOG = SyslogCategory.getLogger(PROTOCOL, AbstractSocketMessageProcessor.class);
    protected final static int INITIAL_RECONNECT_DELAY_MILLIS = 1000;
    protected final static int MAX_RECONNECT_DELAY_MILLIS = 60000;
    protected final static int RECONNECT_BACKOFF_MULTIPLIER = 2;
    protected final List<Consumer<T>> messageConsumers = new ArrayList<>();
    protected final List<Consumer<ConnectionStatus>> connectionStatusConsumers = new ArrayList<>();
    protected String host;
    protected int port;
    protected ConnectionStatus connectionStatus = ConnectionStatus.DISCONNECTED;
    protected ChannelFuture channelFuture;
    protected Channel channel;
    protected Bootstrap bootstrap;
    protected EventLoopGroup workerGroup;
    protected ProtocolExecutorService executorService;
    protected ScheduledFuture reconnectTask;
    protected int reconnectDelayMilliseconds = INITIAL_RECONNECT_DELAY_MILLIS;

    public AbstractSocketMessageProcessor(String host, int port, ProtocolExecutorService executorService) {
        TextUtil.requireNonNullAndNonEmpty(host);
        this.host = host;
        this.port = port;
        this.executorService = executorService;
    }

    @Override
    public synchronized void connect() {
        if (connectionStatus != ConnectionStatus.DISCONNECTED && connectionStatus != ConnectionStatus.WAITING) {
            LOG.finest("Must be disconnected before calling connect");
            return;
        }

        LOG.fine("Connecting");
        onConnectionStatusChanged(ConnectionStatus.CONNECTING);

        if (workerGroup == null) {
            // TODO: In Netty 5 you can pass in an executor service but could pass in thread factory for now
            workerGroup = new NioEventLoopGroup(1);
            bootstrap = new Bootstrap();
            bootstrap.channel(NioSocketChannel.class);
            bootstrap.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 2000);
            bootstrap.option(ChannelOption.SO_KEEPALIVE, true);
            bootstrap.group(workerGroup);

            bootstrap.handler(new ChannelInitializer<SocketChannel>() {
                @Override
                public void initChannel(SocketChannel channel) throws Exception {
                    AbstractSocketMessageProcessor.this.initChannel(channel);
                }
            });
        }

        // Start the client and store the channel
        channelFuture = bootstrap.connect(host, port);
        channel = channelFuture.channel();

        // Add channel callback - this gets called when the channel connects or when channel encounters an error
        channelFuture.addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                synchronized (AbstractSocketMessageProcessor.this) {
                    channelFuture.removeListener(this);

                    if (connectionStatus == ConnectionStatus.DISCONNECTING) {
                        return;
                    }

                    if (future.isSuccess()) {
                        LOG.fine("Successfully connected to: " + host + ":" + port);
                        reconnectTask = null;
                        reconnectDelayMilliseconds = INITIAL_RECONNECT_DELAY_MILLIS;
                        onConnectionStatusChanged(ConnectionStatus.CONNECTED);
                    } else if (future.cause() != null) {
                        LOG.log(Level.INFO, "Connection error", future.cause());
                        // Failed to connect so schedule reconnection attempt
                        scheduleReconnect();
                    }
                }
            }
        });

        // Add closed callback
        channel.closeFuture().addListener(future -> {
            if (connectionStatus != ConnectionStatus.DISCONNECTING) {
                scheduleReconnect();
            }
        });
    }

    @Override
    public synchronized void disconnect() {
        if (connectionStatus == ConnectionStatus.DISCONNECTING || connectionStatus == ConnectionStatus.DISCONNECTED) {
            LOG.finest("Already disconnecting or disconnected");
            return;
        }

        LOG.finest("Disconnecting");
        onConnectionStatusChanged(ConnectionStatus.DISCONNECTING);

        try {
            if (reconnectTask != null) {
                reconnectTask.cancel(false);
            }

            if (channelFuture != null) {
                channelFuture.cancel(true);
                channelFuture.sync();
                channelFuture = null;
            }

            // Close the channel
            if (channel != null) {
                channel.close().sync();
                channel = null;
            }
        } catch (InterruptedException ignored) {

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
            LOG.fine("Cannot send message: Status = " + connectionStatus);
            return;
        }

        try {
            channel.writeAndFlush(message);
            LOG.finer("Message sent");
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Message send failed", e);
        }
    }

    @Override
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
     * Inserts the decoders and encoders into the channel pipeline
     */
    protected void initChannel(SocketChannel channel) {
        channel.pipeline().addLast(new MessageDecoder());
        channel.pipeline().addLast(new MessageEncoder());
    }

    protected synchronized void onMessageReceived(T message) {
        LOG.finer("Message received");
        messageConsumers.forEach(consumer -> consumer.accept(message));
    }

    protected void onConnectionStatusChanged(ConnectionStatus connectionStatus) {
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

        LOG.finest("Scheduling reconnection in '" + reconnectDelayMilliseconds + "' milliseconds");

        reconnectTask = executorService.schedule(() -> {
            synchronized (AbstractSocketMessageProcessor.this) {
                reconnectTask = null;

                // Attempt to reconnect if not disconnecting
                if (connectionStatus != ConnectionStatus.DISCONNECTING && connectionStatus != ConnectionStatus.DISCONNECTED) {
                    connect();
                }
            }
        }, reconnectDelayMilliseconds);
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
}
