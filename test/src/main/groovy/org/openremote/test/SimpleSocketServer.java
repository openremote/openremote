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
package org.openremote.test;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.util.concurrent.GlobalEventExecutor;

import java.util.logging.Logger;

/**
 * Class for creating a simple socket server that echos back any received messages. Adapted from Netty example:
 * <p>
 * <a href="https://netty.io/wiki/user-guide-for-4.x.html#wiki-h2-4">https://netty.io/wiki/user-guide-for-4.x.html#wiki-h2-4</a>
 * <p>
 * The server can be configured as {@link #broadcastMode} which means that received messages are echoed to all connected
 * clients.
 * <p>
 * There is also the {@link #sendMessage(byte[])} method for broadcasting an arbitrary array of data to connected
 * clients.
 */
public class SimpleSocketServer {
    private static final Logger LOG = Logger.getLogger(SimpleSocketServer.class.getName());
    private int port;
    private ChannelFuture channelFuture;
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private boolean broadcastMode;
    ChannelGroup allChannels = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);

    public SimpleSocketServer(int port, boolean broadcastMode) {
        this.port = port;
        this.broadcastMode = broadcastMode;
    }

    public void start() throws InterruptedException {
        bossGroup = new NioEventLoopGroup(); // (1)
        workerGroup = new NioEventLoopGroup();
        try {
            LOG.info("Starting socket server on port: " + port);
            ServerBootstrap b = new ServerBootstrap(); // (2)
            b.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class) // (3)
                .childHandler(new ChannelInitializer<SocketChannel>() { // (4)
                    @Override
                    public void initChannel(SocketChannel ch) throws Exception {
                        ch.pipeline().addLast(new ChannelInboundHandlerAdapter() {
                            @Override
                            public void channelInactive(ChannelHandlerContext ctx) throws Exception {
                                allChannels.remove(ctx.channel());
                                super.channelInactive(ctx);
                            }

                            @Override
                            public void channelActive(ChannelHandlerContext ctx) throws Exception {
                                allChannels.add(ctx.channel());
                                super.channelActive(ctx);
                            }

                            @Override
                            public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                                if (broadcastMode) {
                                    allChannels.writeAndFlush(msg);
                                } else {
                                    ctx.write(msg);
                                    ctx.flush();
                                }
                            }
                        });
                    }
                })
                .option(ChannelOption.SO_BACKLOG, 128)
                .childOption(ChannelOption.SO_KEEPALIVE, true);

            // Bind and start to accept incoming connections.
            channelFuture = b.bind(port).sync();

        } catch (InterruptedException e) {
            stop();
        }
    }

    public void stop() throws InterruptedException {
        LOG.fine("Stopping socket server on port: " + port);

        if (channelFuture != null) {
            channelFuture.channel().close().sync();
        }

        if (workerGroup != null) {
            workerGroup.shutdownGracefully().sync();
        }
        if (bossGroup != null) {
            bossGroup.shutdownGracefully().sync();
        }
    }

    public void sendMessage(byte[] message) {
        LOG.fine("Sending message to all channels");
        ByteBuf out = Unpooled.copiedBuffer(message);
        allChannels.writeAndFlush(out);
    }
}
