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

import dev.failsafe.Failsafe;
import dev.failsafe.RetryPolicy;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.EncoderException;
import jakarta.validation.constraints.NotNull;
import org.openremote.agent.protocol.udp.UDPIOClient;
import org.openremote.agent.protocol.websocket.WebsocketIOClient;
import org.openremote.container.Container;
import org.openremote.model.asset.agent.ConnectionStatus;
import org.openremote.model.syslog.SyslogCategory;

import java.net.SocketAddress;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.openremote.model.syslog.SyslogCategory.PROTOCOL;

/**
 * This is a {@link IOClient} implementation for netty.
 * <p>
 * It uses the netty component for managing the connection.
 * <p>
 * Concrete implementations are responsible for providing the {@link ChannelOutboundHandler}s and
 * {@link ChannelInboundHandler}s required to encode/decode the specific type of messages sent/received to/from this
 * client. For {@link IOClient}s that require some specific encoders/decoders irrespective of the message type (e.g.
 * {@link UDPIOClient} and {@link WebsocketIOClient})
 * the {@link #addEncodersDecoders} method can be overridden so the {@link IOClient} can exactly control
 * the order and types of encoders/decoders in the pipeline.
 * <p>
 * Users of the {@link IOClient} can add encoders/decoders for their specific message type using the
 * {@link #setEncoderDecoderProvider}, each {@link IOClient} should make it clear to users what the required output
 * type(s) are for the last encoder and decoder that a user may wish to add, if adding encoders/decoders is not
 * supported then {@link IOClient}s should override this setter and throw an {@link UnsupportedOperationException}.
 * <p>
 * Typically for outgoing messages a single {@link ChannelOutboundHandler} is sufficient and the
 * {@link MessageToByteEncoder} can be used as a base.
 * <p>
 * For inbound messages; the decoders required are very much dependent on the message type and {@link IOClient} type,
 * any number of standard netty {@link ChannelInboundHandler}s can be used but the last handler should build messages
 * of type &lt;T&gt; and pass them to the {@link #onMessageReceived} method of the client; the {@link ByteToMessageDecoder}
 * or {@link MessageToByteEncoder} can be used for this purpose, which one to use will depend on the previous
 * {@link ChannelInboundHandler}s in the pipeline.
 * <p>
 * <b>NOTE: Care must be taken when working with Netty {@link ByteBuf} as Netty uses reference counting to manage their
 * lifecycle. Refer to the Netty documentation for more information.</b>
 */
public abstract class AbstractNettyIOClient<T, U extends SocketAddress> implements NettyIOClient<T> {

    public static long RECONNECT_DELAY_INITIAL_MILLIS = 1000L;
    public static long RECONNECT_DELAY_MAX_MILLIS = 5*60000L;

    /**
     * This is intended to be used at the end of a decoder chain where the previous decoder outputs a {@link ByteBuf};
     * the provided {@link #decoder} should extract the messages of type &lt;T&gt; from the {@link ByteBuf} and add them
     * to the {@link List} and they will then be passed to the {@link IOClient}.
     */
    public static class ByteToMessageDecoder<T> extends io.netty.handler.codec.ByteToMessageDecoder {
        protected List<T> messages = new ArrayList<>(1);
        protected AbstractNettyIOClient<T, ?> client;
        protected BiConsumer<ByteBuf, List<T>> decoder;

        public ByteToMessageDecoder(AbstractNettyIOClient<T, ?> client, @NotNull BiConsumer<ByteBuf, List<T>> decoder) {
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
    }

    /**
     * This is intended to be used at the end of a decoder chain where the previous decoder outputs messages of type &lt;T&gt;.
     */
    public static class MessageToMessageDecoder<T> extends SimpleChannelInboundHandler<T> {
        protected AbstractNettyIOClient<T,?> client;

        public MessageToMessageDecoder(Class<? extends T> typeClazz, AbstractNettyIOClient<T, ?> client) {
            super(typeClazz);
            this.client = client;
        }

        @Override
        public void channelRead0(ChannelHandlerContext ctx, T msg) {
            client.onMessageReceived(msg);
        }
    }

    /**
     * Concrete implementations must provide an encoder to fill the {@link ByteBuf} ready to be sent `over the wire`.
     */
    public static class MessageToByteEncoder<T> extends io.netty.handler.codec.MessageToByteEncoder<T> {
        protected AbstractNettyIOClient<T, ?> client;
        protected BiConsumer<T, ByteBuf> encoder;

        public MessageToByteEncoder(Class<? extends T> typeClazz, AbstractNettyIOClient<T, ?> client, BiConsumer<T, ByteBuf> encoder) {
            super(typeClazz);
            this.client = client;
            this.encoder = encoder;
        }

        @Override
        protected void encode(ChannelHandlerContext ctx, T msg, ByteBuf out) {
            encoder.accept(msg, out);
        }
    }

    private static final Logger LOG = SyslogCategory.getLogger(PROTOCOL, AbstractNettyIOClient.class);
    protected final List<Consumer<T>> messageConsumers = new CopyOnWriteArrayList<>();
    protected final List<Consumer<ConnectionStatus>> connectionStatusConsumers = new CopyOnWriteArrayList<>();
    protected ConnectionStatus connectionStatus = ConnectionStatus.DISCONNECTED;
    protected Channel channel;
    protected Bootstrap bootstrap;
    protected EventLoopGroup workerGroup;
    protected ExecutorService executorService;
    protected ScheduledExecutorService scheduledExecutorService;
    protected CompletableFuture<Void> connectRetry;
    protected int connectTimeout = 5000;
    protected Supplier<ChannelHandler[]> encoderDecoderProvider;

    protected AbstractNettyIOClient() {
        this.executorService = Container.EXECUTOR;
        this.scheduledExecutorService = Container.SCHEDULED_EXECUTOR;
    }

    @Override
    public void setEncoderDecoderProvider(Supplier<ChannelHandler[]> encoderDecoderProvider) throws UnsupportedOperationException {
        this.encoderDecoderProvider = encoderDecoderProvider;
    }

    public int getConnectTimeoutMillis() {
        return this.connectTimeout;
    }

    public void setConnectTimeoutMillis(int connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    protected abstract Class<? extends Channel> getChannelClass();

    protected abstract EventLoopGroup getWorkerGroup();

    /**
     * Start the actual connection and return a future indicating completion state. Implementors can also
     * add any custom connection logic they require.
     */
    protected abstract Future<Void> startChannel();

    protected void configureChannel() {
        bootstrap.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, getConnectTimeoutMillis());
    }

    @Override
    public void connect() {
        synchronized (this) {
            if (connectionStatus != ConnectionStatus.DISCONNECTED) {
                LOG.finest("Must be disconnected before calling connect: " + getClientUri());
                return;
            }

            LOG.fine("Connecting IO Client: " + getClientUri());
            onConnectionStatusChanged(ConnectionStatus.CONNECTING);
        }

        // TODO: In Netty 5 you can pass in an executor service; can only pass in thread factory for now
        workerGroup = getWorkerGroup();
        bootstrap = new Bootstrap();
        bootstrap.channel(getChannelClass());
        configureChannel();
        bootstrap.group(workerGroup);

        bootstrap.handler(new ChannelInitializer<>() {
            @Override
            public void initChannel(Channel channel) throws Exception {
            AbstractNettyIOClient.this.initChannel(channel);
            }
        });

        scheduleDoConnect(100);
    }

    protected void scheduleDoConnect(long initialDelay) {
        long delay = Math.max(initialDelay, RECONNECT_DELAY_INITIAL_MILLIS);
        long maxDelay = Math.max(delay+1, RECONNECT_DELAY_MAX_MILLIS);

        RetryPolicy<Object> retryPolicy = RetryPolicy.builder()
            .withJitter(Duration.ofMillis(delay))
            .withBackoff(Duration.ofMillis(delay), Duration.ofMillis(maxDelay))
            .handle(Exception.class)
            .onRetryScheduled((execution) ->
                LOG.info("Re-connection scheduled in '" + execution.getDelay() + "' for: " + getClientUri()))
            .onFailedAttempt((execution) -> {
                LOG.info("Connection attempt failed '" + execution.getAttemptCount() + "' for: " + getClientUri() + ", error=" + (execution.getLastException() != null ? execution.getLastException().getMessage() : null));
                doDisconnect();
            })
            .withMaxRetries(Integer.MAX_VALUE)
            .build();

        connectRetry = Failsafe.with(retryPolicy).with(executorService).runAsyncExecution((execution) -> {

            LOG.fine("Connection attempt '" + (execution.getAttemptCount()+1) + "' for: " + getClientUri());
            // Connection future should timeout so we just wait for it but add additional timeout just in case
            Future<Void> connectFuture = doConnect();
            waitForConnectFuture(connectFuture);
            execution.recordResult(null);
        });

        connectRetry.whenComplete((result, ex) -> {
            if (ex != null) {
                // Cleanup resources
                disconnect();
            } else {
                synchronized (this) {
                    if (connectionStatus == ConnectionStatus.CONNECTING) {
                        LOG.fine("Connection attempt success: " + getClientUri());
                        onConnectionStatusChanged(ConnectionStatus.CONNECTED);
                    }
                }
            }
        });
    }

    protected Void waitForConnectFuture(Future<Void> connectFuture) throws Exception {
        return connectFuture.get(getConnectTimeoutMillis()+1000L, TimeUnit.MILLISECONDS);
    }

    protected Future<Void> doConnect() {
        LOG.info("Establishing connection: " + getClientUri());
        // Start the channel
        return startChannel();
    }

    @Override
    public void disconnect() {
        synchronized (this) {
            if (connectionStatus == ConnectionStatus.DISCONNECTED || connectionStatus == ConnectionStatus.DISCONNECTING) {
                LOG.finest("Already disconnected or disconnecting: " + getClientUri());
                return;
            }

            LOG.fine("Disconnecting IO client: " + getClientUri());
            onConnectionStatusChanged(ConnectionStatus.DISCONNECTING);
        }

        try {
            if (connectRetry != null) {
                connectRetry.cancel(true);
                connectRetry = null;
            }
        } catch (Exception ignored) {}
        doDisconnect();
        try {
            if (workerGroup != null) {
                workerGroup.shutdownGracefully();
                workerGroup = null;
            }
        } catch (Exception ignored) {}
        bootstrap = null;
        onConnectionStatusChanged(ConnectionStatus.DISCONNECTED);
    }

    protected void doReconnect() {
        doDisconnect();
        scheduleDoConnect(5000);
    }

    protected void doDisconnect() {
        LOG.finest("Performing disconnect: " + getClientUri());
        try {
            // Close the channel
            if (channel != null) {
                try {
                    channel.disconnect().await();
                } catch (Exception ignored) {}
                try {
                    channel.close().await();
                } catch (Exception ignored) {}
                channel = null;
            }
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Failed to disconnect gracefully: " + getClientUri(), e);
        }
        LOG.finest("Disconnect done: " + getClientUri());
    }

    @Override
    public void sendMessage(T message) {
        if (channel == null) {
            return;
        }

        try {
            // Don't block here as it can cause deadlock
            channel.writeAndFlush(message);
            LOG.finest("Message sent to server: " + getClientUri());
        } catch (Exception e) {
            LOG.log(Level.INFO, "Message send failed: " + getClientUri(), e);
        }
    }

    @Override
    public ConnectionStatus getConnectionStatus() {
        return connectionStatus;
    }

    @Override
    public void addConnectionStatusConsumer(Consumer<ConnectionStatus> connectionStatusConsumer) {
        if (!connectionStatusConsumers.contains(connectionStatusConsumer)) {
            connectionStatusConsumers.add(connectionStatusConsumer);
        }
    }

    @Override
    public void removeConnectionStatusConsumer(Consumer<ConnectionStatus> connectionStatusConsumer) {
        connectionStatusConsumers.remove(connectionStatusConsumer);
    }

    @Override
    public void removeAllConnectionStatusConsumers() {
        connectionStatusConsumers.clear();
    }

    @Override
    public void addMessageConsumer(Consumer<T> messageConsumer) {
        if (!messageConsumers.contains(messageConsumer)) {
            messageConsumers.add(messageConsumer);
        }
    }

    @Override
    public void removeMessageConsumer(Consumer<T> messageConsumer) {
        messageConsumers.remove(messageConsumer);
    }

    @Override
    public void removeAllMessageConsumers() {
        messageConsumers.clear();
    }

    /**
     * Inserts the decoders and encoders into the channel pipeline and configures standard exception handling and logging
     */
    protected void initChannel(Channel channel) throws Exception {
        this.channel = channel;
        addEncodersDecoders(channel);

        // Add closed callback for logging and reconnect
        channel.closeFuture().addListener(closedFuture -> {
            boolean reconnect = false;

            if (!closedFuture.isSuccess() && closedFuture.cause() != null) {
                LOG.info("Connection closed with exception on '" + getClientUri() + "': " + closedFuture.cause().getMessage());
            }

            synchronized (this) {
                if (connectionStatus == ConnectionStatus.CONNECTED) {
                    onConnectionStatusChanged(ConnectionStatus.CONNECTING);
                    reconnect = true;
                }
            }

            if (reconnect) {
                doReconnect();
            }
        });

        // Add inbound exception handler at end of inbound chain
        channel.pipeline().addLast(new ChannelInboundHandlerAdapter() {
            @Override
            public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
                if (cause instanceof DecoderException decoderException) {
                    onDecodeException(ctx, decoderException);
                } else if (cause instanceof EncoderException encoderException) {
                    onEncodeException(ctx, encoderException);
                } else {
                    // Aggressively force close the channel on other exceptions which will cause a reconnect
                    ctx.close();
                }
            }
        });

        // Add promise listener at start of outbound chain (this is how to handle outbound exceptions)
        channel.pipeline().addLast(new ChannelOutboundHandlerAdapter() {
            @Override
            public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
                // Fire any failure through the channel pipeline the same as inbound exceptions
                promise.addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);
                super.write(ctx, msg, promise);
            }
        });
    }

    protected void addEncodersDecoders(Channel channel) throws Exception {
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
        LOG.finest("Message received notifying consumers: " + getClientUri());
        messageConsumers.forEach(consumer -> {
            try {
                consumer.accept(message);
            } catch (Exception e) {
                LOG.log(Level.WARNING, "Exception occurred in message handler '" + e.getMessage() + "':" + getClientUri());
            }
        });
    }

    protected void onDecodeException(ChannelHandlerContext ctx, DecoderException decoderException) {
        LOG.log(Level.FINE, "Decoder exception occurred on in-bound message '" + decoderException.getMessage() +"': " + getClientUri(), decoderException);
    }

    protected void onEncodeException(ChannelHandlerContext ctx, EncoderException encoderException) {
        LOG.log(Level.FINE, "Encoder exception occurred on out-bound message '" + encoderException.getMessage() +"': " + getClientUri(), encoderException);
    }

    protected void onConnectionStatusChanged(ConnectionStatus connectionStatus) {
        if (this.connectionStatus == connectionStatus) {
            return;
        }
        this.connectionStatus = connectionStatus;
        if (!connectionStatusConsumers.isEmpty()) {
            LOG.finest("Notifying connection status consumers: count=" + connectionStatusConsumers.size());
        }
        connectionStatusConsumers.forEach(
            consumer -> {
                try {
                    consumer.accept(connectionStatus);
                } catch (Exception e) {
                    LOG.log(Level.INFO, "Connection status change handler threw an exception: " + getClientUri(), e);
                }
            });
    }

    @Override
    public String toString() {
        return getClientUri();
    }

    // Not ideal but need to chain futures
    // TODO: Replace once netty uses completable futures
    public static CompletableFuture<Void> toCompletableFuture(Future<Void> future) throws UnsupportedOperationException {
        if (future instanceof CompletableFuture<Void> completableFuture) {
            return completableFuture;
        }
        if (future instanceof ChannelFuture channelFuture) {
            CompletableFuture<Void> completableFuture = new CompletableFuture<>();

            try {
                channelFuture.addListener(cf -> {
                    if (cf.isCancelled()) {
                        completableFuture.cancel(true);
                        return;
                    }
                    if (cf.cause() != null) {
                        completableFuture.completeExceptionally(cf.cause());
                    } else if (!cf.isSuccess()) {
                        completableFuture.completeExceptionally(new RuntimeException("Unknown connection failure occurred"));
                    } else {
                        completableFuture.complete(null);
                    }
                });
            } catch (Exception e) {
                completableFuture.completeExceptionally(e);
            }
            return completableFuture;
        }
        throw new UnsupportedOperationException("Future must be a ChannelFuture or already a CompletableFuture");
    }
}
