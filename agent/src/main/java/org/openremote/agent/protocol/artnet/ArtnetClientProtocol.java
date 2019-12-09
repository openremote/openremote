package org.openremote.agent.protocol.artnet;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import io.netty.buffer.ByteBuf;
import io.netty.util.CharsetUtil;
import javassist.bytecode.AttributeInfo;
import jsinterop.base.Any;
import org.openremote.agent.protocol.Protocol;
import org.openremote.agent.protocol.io.IoClient;
import org.openremote.agent.protocol.udp.AbstractUdpClient;
import org.openremote.agent.protocol.udp.AbstractUdpClientProtocol;
import org.openremote.model.asset.AssetAttribute;
import org.openremote.model.attribute.*;
import org.openremote.model.syslog.SyslogCategory;
import org.openremote.model.util.TextUtil;
import org.openremote.model.value.*;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.*;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static org.openremote.model.Constants.PROTOCOL_NAMESPACE;
import static org.openremote.model.attribute.MetaItemDescriptor.Access.ACCESS_PRIVATE;
import static org.openremote.model.attribute.MetaItemDescriptorImpl.metaItemInteger;
import static org.openremote.model.attribute.MetaItemDescriptorImpl.metaItemString;
import static org.openremote.model.syslog.SyslogCategory.PROTOCOL;
import static org.openremote.model.util.TextUtil.REGEXP_PATTERN_INTEGER;
import static org.openremote.model.util.TextUtil.REGEXP_PATTERN_STRING_NON_EMPTY;

public class ArtnetClientProtocol extends AbstractUdpClientProtocol<String> {

    public static final String PROTOCOL_NAME = PROTOCOL_NAMESPACE + ":artnet";
    public static final String PROTOCOL_DISPLAY_NAME = "Artnet Client";
    private static final String PROTOCOL_VERSION = "1.0";

    /**
     * The universe Id of a light
     */
    public static final MetaItemDescriptor META_ARTNET_UNIVERSE_ID = metaItemInteger(
        PROTOCOL_NAME + ":universeId",
        ACCESS_PRIVATE,
        true,
        0,
        512);

    /**
     * The universe asset ID
     */
    public static final MetaItemDescriptor META_ARTNET_UNIVERSE_ASSET_ID = metaItemString(
            PROTOCOL_NAME + ":universeAssetId",
            ACCESS_PRIVATE,
            true,
            REGEXP_PATTERN_STRING_NON_EMPTY,
            MetaItemDescriptor.PatternFailure.STRING_EMPTY);

    private static final Logger LOG = SyslogCategory.getLogger(PROTOCOL, ArtnetClientProtocol.class.getName());

    private final Map<AttributeRef, AttributeInfo> attributeInfoMap = new HashMap<>();
    private static int DEFAULT_RESPONSE_TIMEOUT_MILLIS = 3000;
    private static int DEFAULT_SEND_RETRIES = 1;
    private static int MIN_POLLING_MILLIS = 1000;


    private HashMap<String, HashMap<String, List<ArtnetLight>>> artnetControls =
                    new HashMap<String, HashMap<String, List<ArtnetLight>>>();


    public static final List<MetaItemDescriptor> ATTRIBUTE_META_ITEM_DESCRIPTORS = Arrays.asList(
            META_ATTRIBUTE_WRITE_VALUE,
            META_POLLING_MILLIS,
            META_RESPONSE_TIMEOUT_MILLIS,
            META_SEND_RETRIES,
            META_ARTNET_UNIVERSE_ID,
            META_ARTNET_UNIVERSE_ASSET_ID
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
                Value msg = null;
                try{
                    msg = Values.parse(message).get();
                    ArtNetPacket packet = ArtNetPacket.fromValue(msg).get();
                    ByteBuf output = packet.toBuffer(buf);
                    finalEncoder.accept(message, output);
                }catch(IllegalArgumentException ex) {
                    ex.printStackTrace();
                }
            }
        };
    }

    @Override
    protected void doLinkProtocolConfiguration(AssetAttribute protocolConfiguration) {
        super.doLinkProtocolConfiguration(protocolConfiguration);
        //TODO DO NULL-CHECK
        artnetControls.put(protocolConfiguration.getAssetId().get(), new HashMap<>());//Initialize Parent ArtnetNetwork with empty HashMap to put Universes in
    }

    @Override
    protected void doUnlinkProtocolConfiguration(AssetAttribute protocolConfiguration) {
        super.doUnlinkProtocolConfiguration(protocolConfiguration);
        artnetControls.remove(protocolConfiguration.getAssetId().get());
    }

    @Override
    protected void doLinkAttribute(AssetAttribute attribute, AssetAttribute protocolConfiguration)
    {
        String networkId = protocolConfiguration.getReference().get().getEntityId();//Get ID of Network
        //Get Asset ID of the Universe based upon one of their attributes. You can't use the attribute to get the parent Asset.
        String universeId = Values.getMetaItemValueOrThrow(attribute, META_ARTNET_UNIVERSE_ASSET_ID, false ,true)
                .flatMap(Values::getString)
                .orElse(null);

        HashMap<String, List<ArtnetLight>> universe = artnetControls.get(networkId);

        if(universe != null)
        {
            LOG.info("Universe is already linked to another Network");
            return;
        }

        artnetControls.get(networkId).put(universeId, new ArrayList<ArtnetLight>());

        //List<ArtnetLight> lights = universe.get(attribute.getAssetId()/*Get ID of the Universe*/);

        if (!protocolConfiguration.isEnabled()) {
            LOG.info("Protocol configuration is disabled so ignoring: " + protocolConfiguration.getReferenceOrThrow());
            return;
        }

        ClientAndQueue clientAndQueue = clientMap.get(protocolConfiguration.getReferenceOrThrow());

        if (clientAndQueue == null) {
            return;
        }

        final Integer pollingMillis = Values.getMetaItemValueOrThrow(attribute, META_POLLING_MILLIS, false, true)
                .flatMap(Values::getIntegerCoerced)
                .orElse(null);

        final int responseTimeoutMillis = Values.getMetaItemValueOrThrow(attribute, META_RESPONSE_TIMEOUT_MILLIS, false, true)
                .flatMap(Values::getIntegerCoerced)
                .orElseGet(() ->
                        Values.getMetaItemValueOrThrow(protocolConfiguration, META_RESPONSE_TIMEOUT_MILLIS, false, true)
                                .flatMap(Values::getIntegerCoerced)
                                .orElse(DEFAULT_RESPONSE_TIMEOUT_MILLIS)
                );

        final int sendRetries = Values.getMetaItemValueOrThrow(attribute, META_SEND_RETRIES, false, true)
                .flatMap(Values::getIntegerCoerced)
                .orElseGet(() ->
                        Values.getMetaItemValueOrThrow(protocolConfiguration, META_SEND_RETRIES, false, true)
                                .flatMap(Values::getIntegerCoerced)
                                .orElse(DEFAULT_SEND_RETRIES)
                );

        Consumer<Value> sendConsumer = null;
        ScheduledFuture pollingTask = null;
        AttributeInfo info = new AttributeInfo(attribute.getReferenceOrThrow(), sendRetries, responseTimeoutMillis);

        if (!attribute.isReadOnly()) {
            sendConsumer = Protocol.createDynamicAttributeWriteConsumer(attribute, str ->
                    clientAndQueue.send(
                            str,
                            responseStr -> {
                                // TODO: Add discovery
                                // Just drop the response; something in the future could be used to verify send was successful
                            },
                            info));
        }

        if (pollingMillis != null && pollingMillis < MIN_POLLING_MILLIS) {
            LOG.warning("Polling ms must be >= " + MIN_POLLING_MILLIS);
            return;
        }

        final String writeValue = Values.getMetaItemValueOrThrow(attribute, META_ATTRIBUTE_WRITE_VALUE, false, true)
                .map(Object::toString).orElse(null);

        if (pollingMillis != null && TextUtil.isNullOrEmpty(writeValue)) {
            LOG.warning("Polling requires the META_ATTRIBUTE_WRITE_VALUE meta item to be set");
            return;
        }

        if (pollingMillis != null) {
            Consumer<String> responseConsumer = str -> {
                LOG.fine("Polling response received updating attribute: " + attribute.getReferenceOrThrow());
                updateLinkedAttribute(
                        new AttributeState(
                                attribute.getReferenceOrThrow(),
                                str != null ? Values.create(str) : null));
            };

            Runnable pollingRunnable = () -> clientAndQueue.send(writeValue, responseConsumer, info);
            pollingTask = schedulePollingRequest(clientAndQueue, attribute.getReferenceOrThrow(), pollingRunnable, pollingMillis);
        }

        attributeInfoMap.put(attribute.getReferenceOrThrow(), info);
        info.pollingTask = pollingTask;
        info.sendConsumer = sendConsumer;
    }

    protected ScheduledFuture schedulePollingRequest(ClientAndQueue clientAndQueue,
                                                     AttributeRef attributeRef,
                                                     Runnable pollingTask,
                                                     int pollingMillis) {

        LOG.fine("Scheduling polling request on client '" + clientAndQueue.client + "' to execute every " + pollingMillis + " ms for attribute: " + attributeRef);

        return executorService.scheduleWithFixedDelay(pollingTask, 0, pollingMillis, TimeUnit.MILLISECONDS);
    }

    @Override
    protected void doUnlinkAttribute(AssetAttribute attribute, AssetAttribute protocolConfiguration) {

    }

    @Override
    protected void processLinkedAttributeWrite(AttributeEvent event, AssetAttribute protocolConfiguration) {

        protocolConfiguration.getType();

        AttributeInfo info = attributeInfoMap.get(event.getAttributeRef());
        if (info == null || info.sendConsumer == null) {
            LOG.info("Request to write unlinked attribute or attribute that doesn't support writes so ignoring: " + event);
            return;
        }

        Value value = event.getValue().orElse(null);
        info.sendConsumer.accept(value);

        updateLinkedAttribute(event.getAttributeState());
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

    public class ArtnetLight {

        private String assetId;
        private int r, g, b, w;
        private double dim;

        public ArtnetLight(String assetId, int r, int g, int b, double dim) {
            this.r = r;
            this.g = g;
            this.b = b;
            this.dim = dim;
        }
    }

}
