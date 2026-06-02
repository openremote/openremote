package org.openremote.agent.protocol.teltonika;

import io.netty.channel.socket.DatagramChannel;
import org.openremote.agent.protocol.udp.AbstractUDPServer;

import java.net.InetSocketAddress;

public class TeltonikaUdpServer extends AbstractUDPServer<TeltonikaRecord> {

    public TeltonikaUdpServer(InetSocketAddress localAddress) {
        super(localAddress);
    }

    @Override
    protected void addDecoders(DatagramChannel channel) {
        addDecoder(channel, new TeltonikaProtocolDecoder(true));
    }

    @Override
    protected void addEncoders(DatagramChannel channel) {
        // Decoder writes ByteBuf acknowledgements directly.
    }
}
