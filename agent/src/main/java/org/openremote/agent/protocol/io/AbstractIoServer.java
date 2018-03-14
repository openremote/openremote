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

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.concurrent.GlobalEventExecutor;
import org.openremote.model.asset.agent.ConnectionStatus;
import org.openremote.model.syslog.SyslogCategory;

import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.openremote.model.syslog.SyslogCategory.PROTOCOL;

/**
 * Abstract implementation of {@link IoServer} that uses the Netty library.
 */
// TODO: In Netty 5 you can pass in an executor service; can only pass in thread factory for now
public abstract class AbstractIoServer<T, U extends Channel> implements IoServer<T, U> {

    private static final Logger LOG = SyslogCategory.getLogger(PROTOCOL, AbstractIoServer.class);
    protected int clientLimit = 0; // 0 means no limit
    protected ServerBootstrap bootstrap;
    protected ChannelFuture channelFuture;
    protected EventLoopGroup workerGroup;
    protected boolean started;
    protected final ChannelGroup allChannels = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
    protected final List<BiConsumer<U, T>> messageConsumers = new ArrayList<>();
    protected final List<BiConsumer<U, ConnectionStatus>> connectionStatusConsumers = new ArrayList<>();

    @Override
    public synchronized void start() {
        if (started) {
            return;
        }

        LOG.fine("Starting IO Server: " + getInstanceIdentifier());
        started = true;

        if (workerGroup == null) {
            workerGroup = getWorkerGroup();
        }

        workerGroup = new NioEventLoopGroup();

        try {
            bootstrap = new ServerBootstrap();
            bootstrap.channel(getServerChannelClass());
            bootstrap.group(workerGroup);
            bootstrap.localAddress(getLocalAddress());
            configureServerChannelOptions();
            configureClientChannelOptions();

            bootstrap.childHandler(new ChannelInitializer<U>() {
                @Override
                protected void initChannel(U channel) {
                    AbstractIoServer.this.initClientChannel(channel);
                }
            });

            // Bind and start to accept incoming connections.
            channelFuture = bootstrap.bind().sync();
            allChannels.add(channelFuture.channel());

        } catch (Exception e) {
            LOG.log(Level.SEVERE, "An error occurred whilst starting the server so shutting down", e);
            stop();
        }
    }

    @Override
    public synchronized void stop() {
        if (!started) {
            return;
        }

        LOG.fine("Stopping IO Server: " + getInstanceIdentifier());
        started = false;

        try {
            if (channelFuture != null) {
                channelFuture.channel().close().sync();
            }

            if (workerGroup != null) {
                workerGroup.shutdownGracefully().sync();
            }
        } catch (InterruptedException ignored) {
        } finally {
            channelFuture = null;
            workerGroup = null;
            bootstrap = null;
        }
    }

    @Override
    public void addMessageConsumer(BiConsumer<U, T> messageConsumer) {
        LOG.finest("Adding message consumer");
        synchronized (messageConsumers) {
            messageConsumers.add(messageConsumer);
        }
    }

    @Override
    public void removeMessageConsumer(BiConsumer<U, T> messageConsumer) {
        LOG.finest("Removing message consumer");
        synchronized (messageConsumers) {
            messageConsumers.remove(messageConsumer);
        }
    }

    @Override
    public void addConnectionStatusConsumer(BiConsumer<U, ConnectionStatus> connectionStatusConsumer) {
        synchronized (connectionStatusConsumers) {
            connectionStatusConsumers.add(connectionStatusConsumer);
        }
    }

    @Override
    public void removeConnectionStatusConsumer(BiConsumer<U, ConnectionStatus> connectionStatusConsumer) {
        synchronized (connectionStatusConsumers) {
            connectionStatusConsumers.remove(connectionStatusConsumer);
        }
    }

    @Override
    public ConnectionStatus getConnectionStatus(U client) {
        return client.isActive() ? ConnectionStatus.CONNECTED : ConnectionStatus.DISCONNECTED;
    }

    @Override
    public void disconnectClient(U client) {
        LOG.finer("Disconnecting client: " + getClientDescriptor(client));
        client.close();
    }

    public boolean isStarted() {
        return started;
    }

    /**
     * Initialise the specified client channel (will be called when a new client connection is made)
     */
    protected void initClientChannel(U channel) {
        LOG.fine("Client initialising: " + getClientDescriptor(channel));

        // Add handler to track when a channel becomes active and to handle exceptions
        channel.pipeline().addLast(new ChannelInboundHandlerAdapter() {
            @Override
            public void channelActive(ChannelHandlerContext ctx) throws Exception {
                onClientConnected(channel);
                super.channelActive(ctx);
            }

            @Override
            public void channelInactive(ChannelHandlerContext ctx) throws Exception {
                onClientDisconnected(channel);
                super.channelInactive(ctx);
            }

            @Override
            public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
                onDecodeException(ctx, cause);
                super.exceptionCaught(ctx, cause);
            }
        });

        channel.pipeline().addLast(new ChannelOutboundHandlerAdapter() {
            @Override
            public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
                onEncodeException(ctx, cause);
                super.exceptionCaught(ctx, cause);
            }
        });

        addDecoders(channel);
        addEncoders(channel);

        // Add handler to route the final messages
        channel.pipeline().addLast(new SimpleChannelInboundHandler<T>() {
            @Override
            protected void channelRead0(ChannelHandlerContext ctx, T msg) {
                onMessageReceived(channel, msg);
            }
        });
    }

    protected void onClientDisconnected(U client) {
        LOG.fine("Client disconnected: " + getClientDescriptor(client));
        // Channel group manages removal of stale channels but whilst we're here
        allChannels.remove(client);
        sendClientConnectionStatus(client, ConnectionStatus.DISCONNECTED);
    }

    protected void onClientConnected(U client) {
        LOG.fine("Client connected: " + getClientDescriptor(client));
        allChannels.add(client);
        sendClientConnectionStatus(client, ConnectionStatus.CONNECTED);
    }

    protected void sendClientConnectionStatus(U channel, ConnectionStatus connectionStatus) {
        synchronized (connectionStatusConsumers) {
            connectionStatusConsumers.forEach(statusConsumer
                                                  -> statusConsumer.accept(channel, connectionStatus));
        }
    }

    protected void onMessageReceived(U channel, T message) {
        synchronized (messageConsumers) {
            messageConsumers.forEach(messageConsumer -> messageConsumer.accept(channel, message));
        }
    }

    protected void onDecodeException(ChannelHandlerContext ctx, Throwable cause) {
        LOG.log(Level.SEVERE, "Exception occurred on in-bound message: ", cause);
        // Forcibly close the client connection
        ctx.channel().close();
    }

    protected void onEncodeException(ChannelHandlerContext ctx, Throwable cause) {
        LOG.log(Level.SEVERE, "Exception occurred on out-bound message: ", cause);
        ctx.channel().close();
    }

    /**
     * Configure options to be applied to the server's own IO channel
     */
    protected void configureServerChannelOptions() {
        bootstrap.option(ChannelOption.SO_BACKLOG, clientLimit);
    }

    /**
     * Configure options to be applied to each client's IO channel
     */
    protected void configureClientChannelOptions() {
        bootstrap.childOption(ChannelOption.SO_KEEPALIVE, true);
    }

    @Override
    public void sendMessage(T message, U client) {
        try {
            client.writeAndFlush(message);
            LOG.finest("Message sent to client: " + getClientDescriptor(client));
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Message send failed", e);
        }
    }

    @Override
    public void sendMessage(T message) {
        try {
            allChannels.writeAndFlush(message);
            LOG.finest("Message sent to all clients");
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Message send failed", e);
        }
    }

    /**
     * Get a string identifier that uniquely identifies the current instance of this server. e.g. tcp://IP:PORT
     */
    protected abstract String getInstanceIdentifier();

    /**
     * Get the class to use for the server channel
     */
    protected abstract Class<? extends ServerChannel> getServerChannelClass();

    /**
     * Get the worker group event loop for the server
     */
    protected abstract EventLoopGroup getWorkerGroup();

    /**
     * Get the socket address to which this server should bind
     */
    protected abstract SocketAddress getLocalAddress();

    /**
     * Should return a descriptor to identify the speficied client for use in log files etc.
     */
    protected abstract String getClientDescriptor(U client);

    protected abstract void addDecoders(U channel);

    protected abstract void addEncoders(U channel);
}
