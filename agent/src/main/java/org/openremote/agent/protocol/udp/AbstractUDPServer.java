/*
 * Copyright 2019, OpenRemote Inc.
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

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramChannel;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.handler.codec.DatagramPacketEncoder;
import io.netty.handler.codec.MessageToMessageDecoder;
import io.netty.handler.codec.MessageToMessageEncoder;
import org.openremote.agent.protocol.io.AbstractNettyIOServer;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.logging.Level;

/**
 * UDP in Netty is crap; everything goes through the single server channel as it is connectionless but there is no way
 * of tracking packet origin across decoders. From reading each channel is single threaded so making an assumption that
 * we can store packet origin for processing in {@link #onMessageReceived}.
 * For outbound messages we wrap each decoder in a {@link DatagramPacketEncoder} which unfortunately only supports
 * {@link MessageToMessageEncoder}s.
 */
public abstract class AbstractUDPServer<T> extends AbstractNettyIOServer<T, DatagramChannel, Bootstrap, InetSocketAddress> {

    protected InetSocketAddress localAddress;
    protected InetSocketAddress lastMessageSender;

    public AbstractUDPServer(InetSocketAddress localAddress) {
        this.localAddress = localAddress;
    }

    @Override
    protected String getSocketAddressString() {
        return localAddress == null ? null : "udp://" + localAddress;
    }

    @Override
    protected String getClientDescriptor(DatagramChannel client) {
        return null;
    }

    @Override
    protected Bootstrap createAndConfigureBootstrap() {
        Bootstrap b = new Bootstrap();
        b.group(new NioEventLoopGroup())
                .channel(NioDatagramChannel.class)
                .localAddress(localAddress)
                .option(ChannelOption.SO_BROADCAST, true);
        return b;
    }

    @Override
    protected void initChannel(DatagramChannel channel) {
        super.initChannel(channel);

        // Convert datagram packet to default address envelope
        channel.pipeline().addLast(new MessageToMessageDecoder<DatagramPacket>() {
            @Override
            protected void decode(ChannelHandlerContext ctx, DatagramPacket msg, List<Object> out) throws Exception {
                lastMessageSender = msg.sender();
                out.add(msg.content().retain());
            }
        });

        addDecoders(channel);

        // Add handler to route the final messages
        channel.pipeline().addLast(new SimpleChannelInboundHandler<T>() {
            @Override
            protected void channelRead0(ChannelHandlerContext ctx, T msg) {
                handleMessageReceived(channel, msg);
            }
        });

        // just ensure there is a datagram packet at the end of the outward pipeline
        channel.pipeline().addLast(new MessageToMessageEncoder<AddressedEnvelope<ByteBuf, InetSocketAddress>>() {

            @Override
            protected void encode(ChannelHandlerContext ctx, AddressedEnvelope<ByteBuf, InetSocketAddress> msg, List<Object> out) throws Exception {
                if (msg instanceof DatagramPacket) {
                    out.add(msg.retain());
                } else {
                    out.add(new DatagramPacket(msg.content(), msg.recipient(), msg.sender()));
                }
            }
        });

        addEncoders(channel);
    }

    @Override
    public void sendMessage(T message) {
        sendMessage(message, new InetSocketAddress("255.255.255.255", localAddress.getPort()));
    }

    @Override
    public void sendMessage(T message, DatagramChannel client) {
        throw new IllegalStateException("Sending to a channel not supported for UDP");
    }

    @Override
    public void sendMessage(T message, InetSocketAddress destination) {
        try {
            channelFuture.channel().writeAndFlush(new DefaultAddressedEnvelope<>(message, destination, localAddress));
            LOG.finest("Message sent to client: " + destination.toString());
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Message send failed", e);
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override
    protected void addEncoder(DatagramChannel channel, ChannelOutboundHandler encoder) {
        if (encoder instanceof DatagramPacketEncoder) {
            super.addEncoder(channel, encoder);
            return;
        }

        if (!(encoder instanceof MessageToMessageEncoder)) {
            throw new IllegalArgumentException("Only MessageToMessage encoders can be used with UDP");
        }

        MessageToMessageEncoder mEncoder = (MessageToMessageEncoder)encoder;
        super.addEncoder(channel, new DatagramPacketEncoder(mEncoder));
    }

    @Override
    protected void handleMessageReceived(DatagramChannel channel, T message) {
        InetSocketAddress sender = lastMessageSender;
        lastMessageSender = null;
        onMessageReceived(message, channel, sender);
    }
}
