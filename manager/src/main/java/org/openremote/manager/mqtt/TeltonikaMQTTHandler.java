package org.openremote.manager.mqtt;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.fortuna.ical4j.model.parameter.Display;
import org.apache.activemq.artemis.spi.core.protocol.RemotingConnection;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.keycloak.KeycloakSecurityContext;
import org.openremote.manager.asset.AssetProcessingService;
import org.openremote.manager.asset.AssetStorageService;
import org.openremote.model.Container;
import org.openremote.model.asset.Asset;
import org.openremote.model.attribute.Attribute;
import org.openremote.model.attribute.AttributeEvent;
import org.openremote.model.attribute.AttributeMap;
import org.openremote.model.protocol.mqtt.Topic;
import org.openremote.model.telematics.*;
import org.openremote.model.telematics.teltonika.*;
import org.openremote.model.util.UniqueIdentifierGenerator;
import org.openremote.model.util.ValueUtil;
import org.openremote.model.value.AttributeDescriptor;
import org.openremote.model.value.ValueDescriptor;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.*;
import java.util.function.Function;
import java.util.logging.Logger;

public class TeltonikaMQTTHandler extends MQTTHandler {
    private static final String TELTONIKA_DEVICE_RECEIVE_TOPIC = "data";
    private static final String TELTONIKA_DEVICE_SEND_TOPIC = "commands";
    private static final String TELTONIKA_DEVICE_TOKEN = "teltonika";

    protected AssetStorageService assetStorageService;
    protected AssetProcessingService assetProcessingService;
    protected final TeltonikaParameterRegistry parameterRegistry = TeltonikaParameterRegistry.getInstance();


    @Override
    public void start(Container container) throws Exception {
        super.start(container);
        assetStorageService = container.getService(AssetStorageService.class);
        assetProcessingService = container.getService(AssetProcessingService.class);
    }

    @Override
    protected boolean topicMatches(Topic topic) {
        return TELTONIKA_DEVICE_TOKEN.equalsIgnoreCase(topicTokenIndexToString(topic, 2));
    }

    @Override
    protected Logger getLogger() {
        return Logger.getLogger(TeltonikaMQTTHandler.class.getName());
    }

    @Override
    public boolean checkCanSubscribe(RemotingConnection connection, KeycloakSecurityContext securityContext, Topic topic) {
        // Skip standard checks
        if (!canSubscribe(connection, securityContext, topic)) {
            getLogger().warning("Cannot subscribe to this topic, topic=" + topic + ", connection" + connection);
            return false;
        }
        return true;
    }

    /**
     * Checks if the Subscribing client should be allowed to subscribe to the topic that is handled by this Handler.
     * For Teltonika device endpoints, we need the fourth token (Index 3) to be a valid IMEI number.
     * We do that by checking using IMEIValidator. If IMEI checking is false, then skip the check.
     */
    // To be removed when auto-provisioning works
    @Override
    public boolean canSubscribe(RemotingConnection connection, KeycloakSecurityContext securityContext, Topic topic) {
        if(topic.getTokens().length < 5){
            getLogger().warning(MessageFormat.format("Topic {0} is not a valid Topic. Please use a valid Topic.", topic.toString()));
            return false;
        }
        try{
            Long.parseLong(topic.getTokens()[3]);
        }catch (NumberFormatException e){
            getLogger().warning(MessageFormat.format("IMEI {0} is not a valid IMEI value. Please use a valid IMEI value.", topic.getTokens()[3]));
            return false;
        }
        return Objects.equals(topic.getTokens()[2], TELTONIKA_DEVICE_TOKEN) &&
                (
                        Objects.equals(topic.getTokens()[4], TELTONIKA_DEVICE_RECEIVE_TOPIC) ||
                                Objects.equals(topic.getTokens()[4], TELTONIKA_DEVICE_SEND_TOPIC)
                );
    }
    @Override
    public boolean checkCanPublish(RemotingConnection connection, KeycloakSecurityContext securityContext, Topic topic) {
        return canPublish(connection,securityContext, topic);
    }

    @Override
    public boolean canPublish(RemotingConnection connection, KeycloakSecurityContext securityContext, org.openremote.model.protocol.mqtt.Topic topic) {
        getLogger().finer("Teltonika device will publish to Topic "+topic.toString()+" to transmit payload");
        return true;
    }

    @Override
    public void onSubscribe(RemotingConnection connection, Topic topic) {
        getLogger().info("CONNECT: Device "+topic.getTokens()[1]+" connected to topic "+topic);
    }

    @Override
    public void onUnsubscribe(RemotingConnection connection, Topic topic) {
        getLogger().info("DISCONNECT: Device "+topic.getTokens()[1]+" disconnected from topic "+topic);
    }

    @Override
    public Set<String> getPublishListenerTopics() {
        return Set.of(
                TOKEN_SINGLE_LEVEL_WILDCARD + "/" + TOKEN_SINGLE_LEVEL_WILDCARD + "/" +
                        TELTONIKA_DEVICE_TOKEN + "/" + TOKEN_SINGLE_LEVEL_WILDCARD + "/" + TELTONIKA_DEVICE_RECEIVE_TOPIC
        );
    }
    @Override
    @SuppressWarnings("unchecked")
    public void onPublish(RemotingConnection connection, Topic topic, ByteBuf body) {
        try {
            TeltonikaTrackerAsset tracker = getCreateAssetFromTopic(topic);

            byte[] bytes = new byte[body.readableBytes()];
            body.readBytes(bytes);
            TeltonikaMqttMessage message;
            try {
                message = new TeltonikaMqttMessage(ValueUtil.JSON.readTree(bytes), null);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            Payload payload = message.getPayloadByIndex(0);
            Long timestamp = payload.getTimestamp();

            getLogger().fine(String.format("Processing payload with timestamp: %d (%s)", timestamp, new java.util.Date(timestamp)));

            AttributeMap map = new AttributeMap();
            map.addAll(message.getPayloadByIndex(0).entrySet().stream().map(kvp -> {

                TeltonikaValueDescriptor<?> parameter = (kvp.getKey() instanceof TeltonikaValueDescriptor<?> tvd)
                        ? tvd
                        : null;



                Object value = kvp.getValue();

                parameter = parameter != null ? parameter : new TeltonikaValueDescriptor<>(kvp.getKey().toString(), String.class, ignored -> value.toString());

                // Try to find matching AttributeDescriptor in TeltonikaTrackerAsset
                AttributeDescriptor<?> attributeDescriptor = parameterRegistry.findMatchingAttributeDescriptor(TeltonikaTrackerAsset.class, parameter).orElse(null);
                if(attributeDescriptor == null){
                    getLogger().warning("No Attribute Descriptor found for key "+parameter.getAvlId());
                    attributeDescriptor = new AttributeDescriptor<>(Integer.toString(parameter.getAvlId()), (ValueDescriptor<String>) parameter);
                }

                // Convert JSON value to binary format to test the binary parsing implementation
                Object parsedValue;
                try {
                    ByteBuf binaryBuffer = convertJsonValueToBinary(value, parameter);
                    // Convert ByteBuf to ByteBuffer for ParsingValueDescriptor
                    // IMPORTANT: Ensure we start reading from position 0
                    java.nio.ByteBuffer nioBuffer = binaryBuffer.nioBuffer();
                    nioBuffer.position(0); // Explicitly set position to 0
                    nioBuffer.order(java.nio.ByteOrder.BIG_ENDIAN); // Ensure big-endian byte order

                    // Debug logging at FINE level
                    if (getLogger().isLoggable(java.util.logging.Level.FINE)) {
                        byte[] debugBytes = new byte[nioBuffer.remaining()];
                        nioBuffer.mark();
                        nioBuffer.get(debugBytes);
                        nioBuffer.reset();
                        getLogger().fine(String.format("Converting Key=%s, InputValue=%s, ExpectedLength=%d, ActualBytes=%s",
                            parameter.getAvlId(), value, parameter.getLength(),
                            java.util.HexFormat.of().formatHex(debugBytes)));
                    }

                    parsedValue = parameter.parse(binaryBuffer);

                    getLogger().fine(String.format("Parsed Key=%s: InputValue=%s → ParsedValue=%s (type=%s)",
                        parameter.getAvlId(), value, parsedValue, parsedValue != null ? parsedValue.getClass().getSimpleName() : "null"));

                    binaryBuffer.release(); // Clean up
                } catch (Exception e) {
                    getLogger().severe("Failed to parse value for key " + parameter.getAvlId() + ": " + e.getMessage());
                    getLogger().severe("Stack trace: " + ExceptionUtils.getStackTrace(e));
                    // Fallback to direct value usage
                    parsedValue = value;
                    if (value != null && parameter.getType() != null) {
                        Class<?> expectedType = parameter.getType();
                        if (expectedType == Integer.class && value instanceof Number) {
                            parsedValue = ((Number) value).intValue();
                        } else if (expectedType == Long.class && value instanceof Number) {
                            parsedValue = ((Number) value).longValue();
                        } else if (expectedType == Double.class && value instanceof Number) {
                            parsedValue = ((Number) value).doubleValue();
                        } else if (expectedType == Boolean.class && value instanceof Number) {
                            parsedValue = ((Number) value).intValue() == 1;
                        } else if (expectedType == String.class && !(value instanceof String)) {
                            parsedValue = value.toString();
                        }
                    }
                }

                // Create attribute using the name-based constructor to avoid type inference issues
                return new Attribute<>((AttributeDescriptor<Object>) attributeDescriptor, parsedValue, timestamp);
            }).toArray(Attribute[]::new));

            sendAttributeEvents(tracker, map.stream().toArray(Attribute[]::new));

        }catch (Exception e){
            getLogger().severe("Error processing Teltonika MQTT message on topic "+topic+": "+e.getMessage());
            getLogger().warning(ExceptionUtils.getStackTrace(e));
        }
    }

    private void sendAttributeEvents(TeltonikaTrackerAsset asset, Attribute<?>[] attrs) {
        List<Attribute<?>> newAttributes = new ArrayList<>();
        List<Attribute<?>> oldAttributes = new ArrayList<>();
        Arrays.stream(attrs).forEach(attr -> {
            if(asset.getAttribute(attr.getName()).isEmpty()){
                newAttributes.add(attr);
            } else {
                oldAttributes.add(attr);
            }
        });

        assetStorageService.merge(asset.addAttributes(newAttributes.toArray(Attribute[]::new)));

        oldAttributes.stream()
                .filter(attr -> attr.getValue().isPresent())
                .map(attr -> new AttributeEvent(asset.getId(), attr.getName(), attr.getValue().get()))
                .forEach(assetProcessingService::sendAttributeEvent);

    }

    public TeltonikaTrackerAsset getCreateAssetFromTopic(Topic topic){
        String imei = getImeiFromTopic(topic);
        TeltonikaTrackerAsset asset = assetStorageService.find(UniqueIdentifierGenerator.generateId(imei), TeltonikaTrackerAsset.class);
        if(asset == null){
            getLogger().warning("No asset found for IMEI: "+imei);
            asset = new TeltonikaTrackerAsset();
            asset.setRealm(topicRealm(topic));
            asset.setName("Teltonika Device " + imei);
            asset.setId(UniqueIdentifierGenerator.generateId(getImeiFromTopic(topic)));
            asset.setImei(imei);
            asset.setManufacturer("Teltonika");
            asset.setModel("");
            asset.setCodec("Codec JSON");
            asset.setProtocol("MQTT");

            asset.addOrReplaceAttributes(
                    new Attribute<>(Asset.LOCATION, null),
                    new Attribute<>(Asset.NOTES, null)
            );

            return assetStorageService.merge(asset);
        }
        return asset;
    }

    String getImeiFromTopic(Topic topic){
        return topic.getTokens()[3];
    }

    /**
     * Converts a JSON value to binary ByteBuf format based on the expected length from ParsingValueDescriptor.
     * This allows testing the binary parsing implementation with JSON MQTT data.
     * <p>
     * IMPORTANT: The JSON values from Teltonika MQTT are RAW values (before multiplier is applied by the parser).
     * The parser will apply the multiplier when reading from the binary buffer.
     */
    private ByteBuf convertJsonValueToBinary(Object value, ParsingValueDescriptor<?> valueDescriptor) {
        if (value == null) {
            return Unpooled.buffer(0);
        }

        int length = valueDescriptor.getLength();
        if (length <= 0) {
            // Variable length or unknown - convert to string bytes
            byte[] bytes = value.toString().getBytes(StandardCharsets.UTF_8);
            return Unpooled.wrappedBuffer(bytes);
        }

        Class<?> expectedType = valueDescriptor.getType();
        ByteBuf buffer = Unpooled.buffer(length);

        try {
            switch (length) {
                case 1 -> {
                    // 1-byte value
                    if (expectedType == Boolean.class) {
                        boolean boolVal = (value instanceof Boolean) ? (Boolean) value
                                : (value instanceof Number && ((Number) value).intValue() == 1)
                                || "true".equalsIgnoreCase(value.toString()) || "1".equals(value.toString());
                        buffer.writeByte(boolVal ? 1 : 0);
                    } else if (value instanceof Number) {
                        // Write as signed or unsigned byte
                        buffer.writeByte(((Number) value).intValue());
                    } else {
                        buffer.writeByte(Integer.parseInt(value.toString()));
                    }
                }
                case 2 -> {
                    // 2-byte value - write the raw value (multiplier will be applied by parser)
                    if (value instanceof Number) {
                        buffer.writeShort(((Number) value).shortValue());
                    } else {
                        buffer.writeShort(Integer.parseInt(value.toString()));
                    }
                }
                case 4 -> {
                    // 4-byte value - write the raw value (multiplier will be applied by parser)
                    if (value instanceof Number) {
                        buffer.writeInt(((Number) value).intValue());
                    } else {
                        buffer.writeInt(Integer.parseInt(value.toString()));
                    }
                }
                case 8 -> {
                    // 8-byte value
                    if (value instanceof Number) {
                        buffer.writeLong(((Number) value).longValue());
                    } else if (expectedType == String.class && value instanceof String) {
                        // For hex string IDs, the JSON value is already the raw number
                        try {
                            long longVal = Long.parseLong(value.toString());
                            buffer.writeLong(longVal);
                        } catch (NumberFormatException e) {
                            // Try as hex if decimal fails
                            try {
                                long longVal = Long.parseLong(value.toString(), 16);
                                buffer.writeLong(longVal);
                            } catch (NumberFormatException e2) {
                                getLogger().warning("Could not parse 8-byte value: " + value);
                                buffer.writeLong(0);
                            }
                        }
                    } else {
                        buffer.writeLong(Long.parseLong(value.toString()));
                    }
                }
                default -> {
                    // Fixed length string
                    byte[] bytes = value.toString().getBytes(StandardCharsets.UTF_8);
                    int bytesToWrite = Math.min(bytes.length, length);
                    buffer.writeBytes(bytes, 0, bytesToWrite);
                    // Pad with zeros if needed
                    for (int i = bytesToWrite; i < length; i++) {
                        buffer.writeByte(0);
                    }
                }
            }
        } catch (Exception e) {
            getLogger().warning("Failed to convert value to binary: " + e.getMessage());
            buffer.release();
            // Fallback to string bytes
            byte[] bytes = value.toString().getBytes(StandardCharsets.UTF_8);
            return Unpooled.wrappedBuffer(bytes);
        }

        return buffer;
    }
}
