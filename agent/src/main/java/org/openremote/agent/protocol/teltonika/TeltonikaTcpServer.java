package org.openremote.agent.protocol.teltonika;

import io.netty.channel.socket.SocketChannel;
import org.openremote.agent.protocol.tcp.AbstractTCPServer;

import java.net.InetSocketAddress;

public class TeltonikaTcpServer extends AbstractTCPServer<TeltonikaRecord> {

    public TeltonikaTcpServer(InetSocketAddress localAddress) {
        super(localAddress);
    }

    @Override
    protected void addDecoders(SocketChannel channel) {
        addDecoder(channel, new TeltonikaProtocolDecoder(false));
    }

    @Override
    protected void addEncoders(SocketChannel channel) {
        // Decoder writes ByteBuf acknowledgements directly.
    }
}
