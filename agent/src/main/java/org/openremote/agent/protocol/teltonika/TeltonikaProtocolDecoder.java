package org.openremote.agent.protocol.teltonika;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import org.openremote.model.attribute.Attribute;
import org.openremote.model.attribute.AttributeMap;
import org.openremote.model.geo.GeoJSONPoint;
import org.openremote.model.telematics.parameter.ParseableValueDescriptor;
import org.openremote.model.telematics.teltonika.TeltonikaAttributeResolver;
import org.openremote.model.telematics.teltonika.TeltonikaParameters;
import org.openremote.model.telematics.teltonika.TeltonikaRegistry;
import org.openremote.model.telematics.teltonika.TeltonikaTrackerAsset;
import org.openremote.model.value.ValueType;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Netty decoder for Teltonika binary protocol (Codecs 8, 8E, 16, GH3000, 12, 13).
 * <p>
 * This decoder handles the binary AVL protocol used by Teltonika GPS trackers,
 * parsing location data and IO elements into OpenRemote attributes using the
 * ParsingValueDescriptor framework.
 * <p>
 * Supported codecs:
 * - CODEC_8 (0x08): Standard AVL protocol
 * - CODEC_8_EXT (0x8E): Extended AVL protocol with 2-byte IO IDs
 * - CODEC_16 (0x10): Protocol with generation type
 * - CODEC_12 (0x0C): Serial/camera data
 * - CODEC_13 (0x0D): Text commands
 * - CODEC_GH3000 (0x07): GH3000 specific protocol
 */
public class TeltonikaProtocolDecoder extends ByteToMessageDecoder {

    private static final Logger LOG = Logger.getLogger(TeltonikaProtocolDecoder.class.getName());

    // Codec identifiers
    public static final int CODEC_GH3000 = 0x07;
    public static final int CODEC_8 = 0x08;
    public static final int CODEC_8_EXT = 0x8E;
    public static final int CODEC_12 = 0x0C;
    public static final int CODEC_13 = 0x0D;
    public static final int CODEC_14 = 0x0E;
    public static final int CODEC_15 = 0x0F;
    public static final int CODEC_16 = 0x10;

    public static final String PROTOCOL_TCP_AVL = "teltonika:tcp:avl";
    public static final String PROTOCOL_UDP_AVL = "teltonika:udp:avl";

    private final TeltonikaRegistry parameterRegistry = TeltonikaRegistry.getInstance();
    private final TeltonikaAttributeResolver attributeResolver = new TeltonikaAttributeResolver(parameterRegistry);

    private final boolean connectionless; // UDP vs TCP

    private String imei;

    /**
     * Creates a new Teltonika protocol decoder.
     *
     * @param connectionless true for UDP (connectionless), false for TCP
     */
    public TeltonikaProtocolDecoder(boolean connectionless) {
        this.connectionless = connectionless;
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf buf, List<Object> out) throws Exception {
        if (connectionless) {
            decodeUdp(ctx, buf, out);
        } else {
            decodeTcp(ctx, buf, out);
        }
    }

    /**
     * Decode TCP-based messages.
     */
    private void decodeTcp(ChannelHandlerContext ctx, ByteBuf buf, List<Object> out) {
        if (buf.readableBytes() < 2) {
            return; // Need more data
        }

        // Check for ping message
        if (buf.readableBytes() == 1 && buf.getByte(buf.readerIndex()) == 0xFF) {
            buf.readByte();
            return; // Ignore ping
        }

        // Check if this is an identification message (IMEI)
        int length = buf.getUnsignedShort(buf.readerIndex());
        if (length > 0 && length < 20) { // Likely IMEI (15 digits)
            if (buf.readableBytes() < 2 + length) {
                return; // Need more data
            }
            parseIdentification(ctx, buf);
            return;
        }

        // Data packet - need at least 8 bytes (preamble + data length)
        if (buf.readableBytes() < 8) {
            return;
        }

        // Validate and skip preamble (4 bytes of zeros)
        if (buf.getUnsignedInt(buf.readerIndex()) != 0) {
            throw new IllegalStateException("Invalid Teltonika TCP preamble: expected 0x00000000");
        }
        buf.skipBytes(4);

        // Parse data
        List<TeltonikaRecord> records = parseData(ctx, buf);
        if (records != null) {
            out.addAll(records);
        }
    }

    /**
     * Decode UDP-based messages.
     */
    private void decodeUdp(ChannelHandlerContext ctx, ByteBuf buf, List<Object> out) {
        if (buf.readableBytes() < 10) {
            return; // Need more data
        }

        int udpLength = buf.readUnsignedShort();
        if (udpLength != buf.readableBytes()) {
            throw new IllegalStateException("Invalid Teltonika UDP length: header=" + udpLength + ", available=" + buf.readableBytes());
        }
        int packetId = buf.readUnsignedShort(); // packet id
        buf.readUnsignedByte();  // packet type
        int locationPacketId = buf.readUnsignedByte();

        // Read IMEI
        int imeiLength = buf.readUnsignedShort();
        String imei = buf.readSlice(imeiLength).toString(StandardCharsets.US_ASCII);

        setImei(imei);


        List<TeltonikaRecord> records = parseData(ctx, buf);
        if (records != null) {
            // Add IMEI to each record
            records.forEach(record -> record.setImei(imei));
            out.addAll(records);
        }

        // Send UDP acknowledgment
        sendUdpAck(ctx, packetId, locationPacketId, records != null ? records.size() : 0);
    }

    /**
     * Parse device identification (IMEI).
     */
    private void parseIdentification(ChannelHandlerContext ctx, ByteBuf buf) {
        int length = buf.readUnsignedShort();
        String imei = buf.readSlice(length).toString(StandardCharsets.US_ASCII);

        LOG.info("Device identified: IMEI=" + imei);

        // Send acknowledgment (1 = accept, 0 = reject)
        ByteBuf response = Unpooled.buffer(1);
        response.writeByte(1); // Accept
        ctx.writeAndFlush(response);

        setImei(imei);
    }

    /**
     * Parse AVL data records.
     */
    private List<TeltonikaRecord> parseData(ChannelHandlerContext ctx, ByteBuf buf) {
        if (buf.readableBytes() < 2) {
            return null;
        }

        long dataLength = -1;
        int dataStartIndex = -1;

        // Read data length (for TCP)
        if (!connectionless) {
            if (buf.readableBytes() < 4) {
                return null;
            }
            dataLength = buf.readUnsignedInt();
            dataStartIndex = buf.readerIndex();

            if (buf.readableBytes() < dataLength + 4) {
                throw new IllegalStateException("Incomplete Teltonika TCP frame: expected dataLength=" + dataLength +
                        " plus CRC, but only " + buf.readableBytes() + " bytes available");
            }
        }

        int codec = buf.readUnsignedByte();
        int count = buf.readUnsignedByte();

        if (!isSupportedCodec(codec)) {
            throw new IllegalStateException("Unsupported Teltonika codec: " + codec);
        }

        LOG.fine("Parsing codec=" + codec + ", count=" + count);

        List<TeltonikaRecord> records = new ArrayList<>(count);

        for (int i = 0; i < count; i++) {
            LOG.log(Level.FINE, "Parsing record #" + i);
            TeltonikaRecord record = new TeltonikaRecord();
            record.setImei(imei);
            record.setCodecName(codecName(codec));
            record.setProtocolId(connectionless ? PROTOCOL_UDP_AVL : PROTOCOL_TCP_AVL);
            record.setTransport(connectionless ? "UDP" : "TCP");

            if (codec == CODEC_13) {
                decodeCodec13(buf, record);
            } else if (codec == CODEC_12) {
                decodeCodec12(buf, record);
            } else if (codec == CODEC_14 || codec == CODEC_15) {
                decodeCodec14Or15(buf, record, codec);
            } else {
                decodeLocation(buf, codec, record);
            }

            records.add(record);
        }

        // Verify count
        if (buf.readableBytes() >= 1) {
            int count2 = buf.readUnsignedByte();
            if (count != count2) {
                throw new IllegalStateException("Teltonika record count mismatch: " + count + " != " + count2);
            }
        }

        if (!connectionless) {
            int consumed = buf.readerIndex() - dataStartIndex;
            int remainingInData = (int) dataLength - consumed;
            if (remainingInData > 0) {
                if (buf.readableBytes() < remainingInData) {
                    throw new IllegalStateException("Incomplete Teltonika TCP frame payload tail: expected=" + remainingInData +
                            " bytes, available=" + buf.readableBytes());
                }
                buf.skipBytes(remainingInData);
            } else if (remainingInData < 0) {
                throw new IllegalStateException("Teltonika payload over-read by " + (-remainingInData) + " bytes");
            }

            if (buf.readableBytes() < 4) {
                throw new IllegalStateException("Missing Teltonika CRC (4 bytes) after payload");
            }
            long expectedCrc = buf.readUnsignedInt();
            int actualCrc = crc16Ibm(buf, dataStartIndex, (int) dataLength);
            if ((expectedCrc & 0xFFFFL) != (actualCrc & 0xFFFFL)) {
                throw new IllegalStateException("Teltonika CRC mismatch: expected=0x" + Long.toHexString(expectedCrc).toUpperCase() +
                        ", actual=0x" + Integer.toHexString(actualCrc).toUpperCase());
            }
        }

        // Send TCP acknowledgment
        if (!connectionless && codec != CODEC_12 && codec != CODEC_13 && codec != CODEC_14 && codec != CODEC_15) {
            sendTcpAck(ctx, count);
        }

        return records;
    }

    /**
     * Decode location data for CODEC 8, 8E, 16, GH3000.
     */
    private void decodeLocation(ByteBuf buf, int codec, TeltonikaRecord record) {
        AttributeMap attributes = new AttributeMap();
        int globalMask = 0x0F;

        if (codec == CODEC_GH3000) {
            decodeGh3000Location(buf, record, attributes);
            globalMask = record.getGlobalMask();
        } else {
            decodeStandardLocation(buf, codec, record, attributes);
        }

        // Read IO elements by byte size
        readIoElements(buf, codec, attributes, globalMask);

        record.setAttributes(attributes);
        record.getAttributes().forEach((key, value) -> {value.setTimestamp(record.getTimestamp());});
        record.setImei(getImei());
    }

    /**
     * Decode GH3000 location format.
     */
    private void decodeGh3000Location(ByteBuf buf, TeltonikaRecord record, AttributeMap attributes) {
        long time = buf.readUnsignedInt() & 0x3FFFFFFFL;
        time += 1167609600; // Offset from 2007-01-01

        record.setTimestamp(time * 1000);

        int globalMask = buf.readUnsignedByte();
        record.setGlobalMask(globalMask);

        if ((globalMask & 0x01) != 0) { // Has location
            int locationMask = buf.readUnsignedByte();

            if ((locationMask & 0x01) != 0) {
                double latitude = buf.readFloat();
                double longitude = buf.readFloat();
                record.setLocation(new GeoJSONPoint(longitude, latitude));
            }

            if ((locationMask & 0x02) != 0) {
                addAttribute(attributes, TeltonikaParameters.ALTITUDE, buf.readUnsignedShort(), record.getTimestamp());
            }

            if ((locationMask & 0x04) != 0) {
                int course = (int)(buf.readUnsignedByte() * 360.0 / 256);
                addAttribute(attributes, TeltonikaParameters.DIRECTION, course, record.getTimestamp());
            }

            if ((locationMask & 0x08) != 0) {
                int speedSatellite = buf.readUnsignedByte();
                addAttribute(attributes, TeltonikaParameters.SPEED_SATELLITE, speedSatellite, record.getTimestamp());
            }

            if ((locationMask & 0x10) != 0) {
                int satCount = buf.readUnsignedByte();
                addAttribute(attributes, TeltonikaParameters.SATELLITES, satCount, record.getTimestamp());
            }
        }
    }

    /**
     * Decode standard location format (CODEC 8, 8E, 16).
     */
    private void decodeStandardLocation(ByteBuf buf, int codec, TeltonikaRecord record, AttributeMap attributes) {
        // Timestamp (milliseconds since epoch)
        long timestamp = buf.readLong();

        record.setTimestamp(timestamp);

        // Priority
        int priority = buf.readUnsignedByte();
        addAttribute(attributes, TeltonikaParameters.PRIORITY, priority, timestamp);

        // GPS position
        double longitude = buf.readInt() / 10000000.0;
        double latitude = buf.readInt() / 10000000.0;
        record.setLocation(new GeoJSONPoint(longitude, latitude));

        // Altitude
        int altitude = buf.readShort();
        addAttribute(attributes, TeltonikaParameters.ALTITUDE, altitude, timestamp);

        // Direction
        int direction = buf.readUnsignedShort();
        addAttribute(attributes, TeltonikaParameters.DIRECTION, direction, timestamp);

        // Satellites
        int satellites = buf.readUnsignedByte();
        addAttribute(attributes, TeltonikaParameters.SATELLITES, satellites, timestamp);
        record.setValid(satellites > 0);

        // Speed
        int speed = buf.readUnsignedShort();
        addAttribute(attributes, TeltonikaParameters.SPEED, speed, timestamp);

        // Event ID
        int eventId = readExtByte(buf, codec, CODEC_8_EXT, CODEC_16);
        addAttribute(attributes, TeltonikaParameters.EVENT_TRIGGERED, eventId, timestamp);

        // Generation type (CODEC 16 only)
        short generation = -1;
        if (codec == CODEC_16) {
            generation = buf.readUnsignedByte();
        }

        int totalIoCount = readExtByte(buf, codec, CODEC_8_EXT);


        LOG.finer("Decoded location: timestamp=" + timestamp +
            ", priority=" + priority +
            ", lat=" + latitude +
            ", lon=" + longitude +
            ", alt=" + altitude +
            ", speed=" + speed +
            ", dir=" + direction +
            ", sats=" + satellites +
            ", generation=" + generation +
            ", eventId=" + eventId +
            " totalIoCount=" + totalIoCount
        );

        // Total IO count
    }

    /**
     * Read IO elements grouped by byte size.
     */
    private void readIoElements(ByteBuf buf, int codec, AttributeMap attributes, int globalMask) {
//        LOG.finest("Reading IoElements");
        // 1-byte IO elements
        if ((globalMask & 0x02) != 0) {
            int count = readExtByte(buf, codec, CODEC_8_EXT);
            LOG.finer("Read 1byte io elements: count = " + count);
            for (int i = 0; i < count; i++) {
                int id = readExtByte(buf, codec, CODEC_8_EXT, CODEC_16);
                LOG.finer("Read io elements: id = " + id);
                parseIoElement(id, buf.readRetainedSlice(1), attributes);
            }
        }

        // 2-byte IO elements
        if ((globalMask & 0x04) != 0) {
            int count = readExtByte(buf, codec, CODEC_8_EXT);
            for (int i = 0; i < count; i++) {
                int id = readExtByte(buf, codec, CODEC_8_EXT, CODEC_16);
                parseIoElement(id, buf.readRetainedSlice(2), attributes);
            }
        }

        // 4-byte IO elements
        if ((globalMask & 0x08) != 0) {
            int count = readExtByte(buf, codec, CODEC_8_EXT);
            for (int i = 0; i < count; i++) {
                int id = readExtByte(buf, codec, CODEC_8_EXT, CODEC_16);
                parseIoElement(id, buf.readRetainedSlice(4), attributes);
            }
        }

        // 8-byte IO elements (CODEC 8/8E/16 only)
        if (codec == CODEC_8 || codec == CODEC_8_EXT || codec == CODEC_16) {
            int count = readExtByte(buf, codec, CODEC_8_EXT);
            for (int i = 0; i < count; i++) {
                int id = readExtByte(buf, codec, CODEC_8_EXT, CODEC_16);
                parseIoElement(id, buf.readRetainedSlice(8), attributes);
            }
        }

        // Variable-length IO elements (CODEC 8E only)
        if (codec == CODEC_8_EXT) {
            int count = buf.readUnsignedShort();
            for (int i = 0; i < count; i++) {
                int id = buf.readUnsignedShort();
                int length = buf.readUnsignedShort();
                parseIoElement(id, buf.readRetainedSlice(length), attributes);
            }
        }
    }

    /**
     * Parse a single IO element using the parameter registry.
     */
    @SuppressWarnings("unchecked")
    private void parseIoElement(int id, ByteBuf value, AttributeMap attributes) {
        LOG.finer("Parsing IoElement AVL ID:" + id);
        try {
            attributes.add(attributeResolver.resolveBinaryIo(id, value, 0));
        } finally {
            value.release();
        }
    }

    /**
     * Helper to add parsed attribute to map.
     */
    private <T> void addAttribute(AttributeMap attributes, ParseableValueDescriptor<T> descriptor, T value, Long timestamp) {
        attributes.add(new Attribute<>(parameterRegistry.findMatchingAttributeDescriptor(TeltonikaTrackerAsset.class, descriptor).orElseThrow(), value, timestamp));
    }

    /**
     * Decode CODEC 12 (Serial/Camera data).
     */
    private void decodeCodec12(ByteBuf buf, TeltonikaRecord record) {
        // Set timestamp to current time as serial data has no timestamp
        record.setTimestamp(System.currentTimeMillis());

        int type = buf.readUnsignedByte();
        int length = buf.readInt();

        AttributeMap attributes = new AttributeMap();

        if (type == 0x0D) { // Camera data
            // Store as hex for now
            String data = ByteBufUtil.hexDump(buf.readSlice(length));
            Attribute<String> attr = new Attribute<>("cameraData", ValueType.TEXT, data);
            attributes.add(attr);
        } else {
            // Check if printable text
            String data = buf.readSlice(length).toString(StandardCharsets.UTF_8);
            Attribute<String> attr = new Attribute<>("serialData", ValueType.TEXT, data);
            attributes.add(attr);
        }

        record.setAttributes(attributes);
    }

    /**
     * Decode CODEC 13 (Text commands).
     */
    private void decodeCodec13(ByteBuf buf, TeltonikaRecord record) {
        buf.readUnsignedByte(); // type
        int length = buf.readInt() - 4;

        long timestamp = buf.readUnsignedInt() * 1000L;
        record.setTimestamp(timestamp);

        String data = buf.readSlice(length).toString(StandardCharsets.UTF_8).trim();

        AttributeMap attributes = new AttributeMap();
        Attribute<String> attr = new Attribute<>("textCommand", ValueType.TEXT, data);
        attributes.add(attr);
        record.setAttributes(attributes);
    }

    private void decodeCodec14Or15(ByteBuf buf, TeltonikaRecord record, int codec) {
        int type = buf.readUnsignedByte();
        int length = buf.readInt();

        byte[] payload = new byte[length];
        buf.readBytes(payload);

        long timestamp = System.currentTimeMillis();
        if (codec == CODEC_15 && length >= 12) {
            long ts = ((payload[0] & 0xFFL) << 24)
                    | ((payload[1] & 0xFFL) << 16)
                    | ((payload[2] & 0xFFL) << 8)
                    | (payload[3] & 0xFFL);
            timestamp = ts * 1000L;
        }
        record.setTimestamp(timestamp);

        String text = codec == CODEC_15 && length > 12
                ? new String(payload, 12, length - 12, StandardCharsets.UTF_8).trim()
                : extractPrintableAscii(payload);

        AttributeMap attributes = new AttributeMap();
        attributes.add(new Attribute<>("textCommand", ValueType.TEXT, text));
        attributes.add(new Attribute<>("codecMessageType", ValueType.INTEGER, type));
        record.setAttributes(attributes);
    }

    /**
     * Read extended byte (1 or 2 bytes depending on codec).
     */
    private int readExtByte(ByteBuf buf, int codec, int... extCodecs) {
//        LOG.finer("Read extension byte " + codec);
        for (int extCodec : extCodecs) {
            if (codec == extCodec) {
                return buf.readUnsignedShort();
            }
        }
        return buf.readUnsignedByte();
    }

    /**
     * Send TCP acknowledgment.
     */
    private void sendTcpAck(ChannelHandlerContext ctx, int count) {
        ByteBuf response = Unpooled.buffer(4);
        response.writeInt(count);
        ctx.writeAndFlush(response);
    }

    /**
     * Send UDP acknowledgment.
     */
    private void sendUdpAck(ChannelHandlerContext ctx, int packetId, int locationPacketId, int count) {
        ByteBuf response = Unpooled.buffer(7);
        response.writeShort(5);      // length
        response.writeShort(packetId);      // packet id
        response.writeByte(0x01);    // type
        response.writeByte(locationPacketId);
        response.writeByte(count);
        ctx.writeAndFlush(response);
    }

    private void setImei(String imei) {
        this.imei = imei;
    }

    private String getImei() {
        return this.imei;
    }

    private String codecName(int codec) {
        return switch (codec) {
            case CODEC_GH3000 -> "codec_gh3000";
            case CODEC_8 -> "codec_8";
            case CODEC_8_EXT -> "codec_8e";
            case CODEC_12 -> "codec_12";
            case CODEC_13 -> "codec_13";
            case CODEC_14 -> "codec_14";
            case CODEC_15 -> "codec_15";
            case CODEC_16 -> "codec_16";
            default -> "codec_unknown_" + codec;
        };
    }

    private boolean isSupportedCodec(int codec) {
        return codec == CODEC_GH3000 || codec == CODEC_8 || codec == CODEC_8_EXT ||
                codec == CODEC_12 || codec == CODEC_13 || codec == CODEC_14 ||
                codec == CODEC_15 || codec == CODEC_16;
    }

    private int crc16Ibm(ByteBuf buf, int offset, int length) {
        int crc = 0;
        for (int i = 0; i < length; i++) {
            crc ^= buf.getUnsignedByte(offset + i);
            for (int j = 0; j < 8; j++) {
                if ((crc & 0x0001) == 1) {
                    crc = (crc >> 1) ^ 0xA001;
                } else {
                    crc >>= 1;
                }
            }
        }
        return crc & 0xFFFF;
    }

    private String extractPrintableAscii(byte[] payload) {
        int bestStart = -1;
        int bestLen = 0;
        int currentStart = -1;
        int currentLen = 0;
        for (int i = 0; i < payload.length; i++) {
            int b = payload[i] & 0xFF;
            if (b >= 32 && b <= 126) {
                if (currentStart < 0) {
                    currentStart = i;
                    currentLen = 1;
                } else {
                    currentLen++;
                }
                if (currentLen > bestLen) {
                    bestLen = currentLen;
                    bestStart = currentStart;
                }
            } else {
                currentStart = -1;
                currentLen = 0;
            }
        }
        if (bestStart >= 0) {
            return new String(payload, bestStart, bestLen, StandardCharsets.UTF_8).trim();
        }
        return ByteBufUtil.hexDump(payload).toUpperCase();
    }
}
