package org.openremote.agent.protocol.artnet;

import io.netty.buffer.ByteBuf;
import io.netty.util.CharsetUtil;
import org.openremote.agent.protocol.Protocol;
import org.openremote.agent.protocol.io.IoClient;
import org.openremote.agent.protocol.udp.AbstractUdpClient;
import org.openremote.agent.protocol.udp.AbstractUdpClientProtocol;
import org.openremote.model.asset.AssetAttribute;
import org.openremote.model.attribute.AttributeEvent;
import org.openremote.model.attribute.MetaItemDescriptor;
import org.openremote.model.syslog.SyslogCategory;

import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.logging.Logger;

import static org.openremote.model.Constants.PROTOCOL_NAMESPACE;
import static org.openremote.model.syslog.SyslogCategory.PROTOCOL;

public class ArtnetClientProtocol extends AbstractUdpClientProtocol<String> {

    private static final Logger LOG = SyslogCategory.getLogger(PROTOCOL, ArtnetClientProtocol.class.getName());
    public static final String PROTOCOL_NAME = PROTOCOL_NAMESPACE + ":artnetClient";
    public static final String PROTOCOL_DISPLAY_NAME = "Artnet Client";
    public static final String PROTOCOL_VERSION = "1.0";

    public static final List<MetaItemDescriptor> ATTRIBUTE_META_ITEM_DESCRIPTORS = Arrays.asList(
            META_ATTRIBUTE_WRITE_VALUE,
            META_POLLING_MILLIS,
            META_RESPONSE_TIMEOUT_MILLIS,
            META_SEND_RETRIES,
            META_SERVER_ALWAYS_RESPONDS
    );

    @Override
    protected IoClient<String> createIoClient(String host, int port, Integer bindPort, Charset charset, boolean binaryMode, boolean hexMode) {

        BiConsumer<ByteBuf, List<String>> decoder;
        BiConsumer<String, ByteBuf> encoder;

        if (hexMode) {
            decoder = (buf, messages) -> {
                byte[] bytes = new byte[buf.readableBytes()];
                buf.readBytes(bytes);
                String msg = Protocol.bytesToHexString(bytes);
                messages.add(msg);
            };
            encoder = (message, buf) -> {
                byte[] bytes = Protocol.bytesFromHexString(message);
                buf.writeBytes(bytes);
            };
        } else if (binaryMode) {
            decoder = (buf, messages) -> {
                byte[] bytes = new byte[buf.readableBytes()];
                buf.readBytes(bytes);
                String msg = Protocol.bytesToBinaryString(bytes);
                messages.add(msg);
            };
            encoder = (message, buf) -> {
                byte[] bytes = Protocol.bytesFromBinaryString(message);
                buf.writeBytes(bytes);
            };
        } else {
            final Charset finalCharset = charset != null ? charset : CharsetUtil.UTF_8;
            decoder = (buf, messages) -> {
                String msg = buf.toString(finalCharset);
                messages.add(msg);
                buf.readerIndex(buf.readerIndex() + buf.readableBytes());
            };
            encoder = (message, buf) -> buf.writeBytes(message.getBytes(finalCharset));
        }

        BiConsumer<ByteBuf, List<String>> finalDecoder = decoder;
        BiConsumer<String, ByteBuf> finalEncoder = encoder;


        return new AbstractUdpClient<String>(host, port, bindPort, executorService) {

            @Override
            protected void decode(ByteBuf buf, List<String> messages) {
                finalDecoder.accept(buf, messages);
            }

            @Override
            protected void encode(String message, ByteBuf buf) {
                message = message.substring(1, message.length() - 1).trim();
                String[] values = message.split(",");

                double dim = Double.parseDouble(values[4]);
                int r;
                int g;
                int b;
                int w;
                if(dim > 0) {
                    r = (int) Math.round((Double.parseDouble(values[0]) / (100 - dim)));
                    g = (int) Math.round((Double.parseDouble(values[1]) / (100 - dim)));
                    b = (int) Math.round((Double.parseDouble(values[2]) / (100 - dim)));
                    w = (int) Math.round((Double.parseDouble(values[3]) / (100 - dim)));
                }else {
                    r = 0;
                    g = 0;
                    b = 0;
                    w = 0;
                }

                byte[] prefix = { 65, 114, 116, 45, 78, 101, 116, 0, 0, 80, 0, 14 };
                buf.writeBytes(prefix);
                buf.writeByte(0);
                buf.writeByte(0);
                buf.writeByte(0);
                buf.writeByte(0);
                buf.writeByte(0);
                buf.writeByte(4);
                for(int i = 0; i <= 18; i++) {
                    buf.writeByte(g);
                    buf.writeByte(r);
                    buf.writeByte(b);
                    buf.writeByte(w);
                }
                finalEncoder.accept(message, buf);
            }
        };
    }

    @Override
    protected void doLinkAttribute(AssetAttribute attribute, AssetAttribute protocolConfiguration) {

    }

    @Override
    protected void doUnlinkAttribute(AssetAttribute attribute, AssetAttribute protocolConfiguration) {

    }

    @Override
    protected void processLinkedAttributeWrite(AttributeEvent event, AssetAttribute protocolConfiguration) {

    }

    @Override
    public String getProtocolName() {
        return PROTOCOL_NAME;
    }

    @Override
    public String getProtocolDisplayName() {
        return PROTOCOL_DISPLAY_NAME;
    }

    @Override
    public String getVersion() {
        return PROTOCOL_VERSION;
    }

    @Override
    protected List<MetaItemDescriptor> getLinkedAttributeMetaItemDescriptors() {
        return ATTRIBUTE_META_ITEM_DESCRIPTORS;
    }
}
