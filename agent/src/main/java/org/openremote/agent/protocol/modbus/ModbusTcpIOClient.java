package org.openremote.agent.protocol.modbus;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.MessageToByteEncoder;
import org.openremote.agent.protocol.io.AbstractNettyIOClient;
import org.openremote.model.syslog.SyslogCategory;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import static org.openremote.model.syslog.SyslogCategory.PROTOCOL;

/**
 * Netty-based Modbus TCP client implementation.
 * Handles Modbus TCP framing (MBAP header + PDU).
 */
public class ModbusTcpIOClient extends AbstractNettyIOClient<ModbusTcpIOClient.ModbusTcpFrame, InetSocketAddress> {

    public static final Logger LOG = SyslogCategory.getLogger(PROTOCOL, ModbusTcpIOClient.class);

    protected String host;
    protected int port;
    protected final AtomicInteger transactionIdCounter = new AtomicInteger(0);

    public ModbusTcpIOClient(String host, int port) {
        this.host = host;
        this.port = port;
    }

    @Override
    protected Class<SocketChannel> getChannelClass() {
        return NioSocketChannel.class;
    }

    @Override
    protected InetSocketAddress getRemoteAddress() {
        return new InetSocketAddress(host, port);
    }

    @Override
    protected ChannelHandler[] getEncodersAndDecoders() {
        return new ChannelHandler[] {
            new ModbusTcpEncoder(),
            new ModbusTcpDecoder()
        };
    }

    /**
     * Get next transaction ID (wraps around at 65536)
     */
    public int getNextTransactionId() {
        return transactionIdCounter.updateAndGet(current -> (current + 1) % 65536);
    }

    /**
     * Represents a Modbus TCP frame (MBAP header + PDU)
     */
    public static class ModbusTcpFrame {
        private final int transactionId;
        private final int protocolId;
        private final int length;
        private final int unitId;
        private final byte[] pdu;

        public ModbusTcpFrame(int transactionId, int unitId, byte[] pdu) {
            this.transactionId = transactionId;
            this.protocolId = 0; // Always 0 for Modbus TCP
            this.length = 1 + pdu.length; // Unit ID + PDU
            this.unitId = unitId;
            this.pdu = pdu;
        }

        public ModbusTcpFrame(int transactionId, int protocolId, int length, int unitId, byte[] pdu) {
            this.transactionId = transactionId;
            this.protocolId = protocolId;
            this.length = length;
            this.unitId = unitId;
            this.pdu = pdu;
        }

        public int getTransactionId() {
            return transactionId;
        }

        public int getProtocolId() {
            return protocolId;
        }

        public int getLength() {
            return length;
        }

        public int getUnitId() {
            return unitId;
        }

        public byte[] getPdu() {
            return pdu;
        }

        public byte getFunctionCode() {
            return pdu != null && pdu.length > 0 ? pdu[0] : 0;
        }

        public boolean isException() {
            return pdu != null && pdu.length > 0 && (pdu[0] & 0x80) != 0;
        }
    }

    /**
     * Encodes Modbus TCP frames to bytes (MBAP header + PDU)
     */
    @ChannelHandler.Sharable
    public static class ModbusTcpEncoder extends MessageToByteEncoder<ModbusTcpFrame> {
        @Override
        protected void encode(io.netty.channel.ChannelHandlerContext ctx, ModbusTcpFrame frame, ByteBuf out) {
            // MBAP Header (7 bytes)
            out.writeShort(frame.getTransactionId());  // Transaction ID
            out.writeShort(frame.getProtocolId());     // Protocol ID (0 for Modbus)
            out.writeShort(frame.getLength());          // Length (Unit ID + PDU)
            out.writeByte(frame.getUnitId());          // Unit ID

            // PDU
            if (frame.getPdu() != null) {
                out.writeBytes(frame.getPdu());
            }

            LOG.finest(() -> String.format("Encoded Modbus TCP frame: TxID=%d, UnitID=%d, FC=0x%02X, PDU length=%d",
                frame.getTransactionId(), frame.getUnitId(), frame.getFunctionCode(),
                frame.getPdu() != null ? frame.getPdu().length : 0));
        }
    }

    /**
     * Decodes bytes to Modbus TCP frames (MBAP header + PDU)
     */
    public static class ModbusTcpDecoder extends ByteToMessageDecoder {
        @Override
        protected void decode(io.netty.channel.ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
            // Need at least 7 bytes for MBAP header
            if (in.readableBytes() < 7) {
                return;
            }

            // Mark reader index in case we need to reset
            in.markReaderIndex();

            // Read MBAP header
            int transactionId = in.readUnsignedShort();
            int protocolId = in.readUnsignedShort();
            int length = in.readUnsignedShort();
            int unitId = in.readUnsignedByte();

            // Check if we have enough bytes for the PDU
            int pduLength = length - 1; // Length includes unit ID
            if (in.readableBytes() < pduLength) {
                // Not enough data yet, reset and wait
                in.resetReaderIndex();
                return;
            }

            // Read PDU
            byte[] pdu = new byte[pduLength];
            in.readBytes(pdu);

            ModbusTcpFrame frame = new ModbusTcpFrame(transactionId, protocolId, length, unitId, pdu);

            LOG.finest(() -> String.format("Decoded Modbus TCP frame: TxID=%d, UnitID=%d, FC=0x%02X, PDU length=%d, Exception=%b",
                frame.getTransactionId(), frame.getUnitId(), frame.getFunctionCode(),
                frame.getPdu().length, frame.isException()));

            out.add(frame);
        }
    }
}
