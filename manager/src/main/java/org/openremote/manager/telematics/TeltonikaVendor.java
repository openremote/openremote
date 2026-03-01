package org.openremote.manager.telematics;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import com.fasterxml.jackson.databind.JsonNode;
import org.openremote.manager.asset.AssetProcessingService;
import org.openremote.manager.asset.AssetStorageService;
import org.openremote.model.attribute.Attribute;
import org.openremote.model.attribute.MetaItem;
import org.openremote.model.telematics.teltonika.TeltonikaAssetMapper;
import org.openremote.model.telematics.core.DeviceMessage;
import org.openremote.model.telematics.core.TelematicsMessageHandler;
import org.openremote.model.telematics.parameter.TelematicsParameterRegistry;
import org.openremote.model.telematics.protocol.DeviceCommand;
import org.openremote.model.telematics.protocol.DeviceProtocol;
import org.openremote.model.telematics.protocol.MessageContext;
import org.openremote.model.telematics.protocol.ProtocolDecodeException;
import org.openremote.model.telematics.protocol.ProtocolEncodeException;
import org.openremote.model.telematics.session.DeviceSession;
import org.openremote.model.telematics.session.DeviceSessionManager;
import org.openremote.model.telematics.teltonika.*;
import org.openremote.model.util.ValueUtil;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Logger;

import static org.openremote.model.value.MetaItemType.READ_ONLY;

public final class TeltonikaVendor implements TelematicsVendor {

    public static final String VENDOR_ID = "teltonika";
    public static final String PROTOCOL_MQTT_JSON = "teltonika:mqtt:json";
    public static final String PROTOCOL_TCP_AVL = "teltonika:tcp:avl";
    public static final String PROTOCOL_UDP_AVL = "teltonika:udp:avl";
    public static final String CODEC_JSON = "json";

    private static final TeltonikaVendor INSTANCE = new TeltonikaVendor();

    private final TeltonikaRegistry registry = TeltonikaRegistry.getInstance();
    private final TeltonikaAssetMapper assetMapper = new TeltonikaAssetMapper();
    private final TeltonikaCommandMapper commandMapper = new TeltonikaCommandMapper();
    private final TeltonikaMqttJsonProtocol mqttProtocol = new TeltonikaMqttJsonProtocol(registry, commandMapper);
    private final TeltonikaTransportProtocol tcpProtocol = new TeltonikaTransportProtocol(PROTOCOL_TCP_AVL, MessageContext.Transport.TCP, registry, commandMapper);
    private final TeltonikaTransportProtocol udpProtocol = new TeltonikaTransportProtocol(PROTOCOL_UDP_AVL, MessageContext.Transport.UDP, registry, commandMapper);
    private final TeltonikaSessionManager sessionManager = new TeltonikaSessionManager();

    private TeltonikaVendor() {
    }

    public static TeltonikaVendor getInstance() {
        return INSTANCE;
    }

    @Override
    public String getVendorId() {
        return VENDOR_ID;
    }

    @Override
    public String getVendorName() {
        return "Teltonika Telematics";
    }

    @Override
    public Class<DeviceCommand> getCommandClass() {
        return DeviceCommand.class;
    }

    @Override
    public Class<DeviceMessage> getMessageClass() {
        return DeviceMessage.class;
    }

    @Override
    public TelematicsMessageHandler createMessageHandler(Logger logger,
                                                         AssetStorageService assetStorageService,
                                                         AssetProcessingService assetProcessingService) {
        return new TeltonikaMessageHandler(logger, assetStorageService, assetProcessingService, assetMapper);
    }

    @Override
    public DeviceProtocol getProtocol() {
        return mqttProtocol;
    }

    public DeviceProtocol getProtocol(MessageContext.Transport transport) {
        return switch (transport) {
            case MQTT -> mqttProtocol;
            case TCP -> tcpProtocol;
            case UDP -> udpProtocol;
            default -> throw new IllegalArgumentException("Unsupported Teltonika transport: " + transport);
        };
    }

    @Override
    public TelematicsParameterRegistry<TeltonikaParameter<?>> getParameterRegistry() {
        return registry;
    }

    @Override
    public TeltonikaAssetMapper getAssetMapper() {
        return assetMapper;
    }

    @Override
    public TeltonikaSessionManager getSessionManager() {
        return sessionManager;
    }

    @Override
    public TeltonikaCommandMapper getCommandMapper() {
        return commandMapper;
    }

    @Override
    public Set<String> getTransports() {
        return Set.of(MessageContext.Transport.TCP.name(), MessageContext.Transport.UDP.name(), MessageContext.Transport.MQTT.name());
    }

    public static class TeltonikaCommandMapper implements TelematicsVendor.CommandMapper<DeviceCommand, DeviceMessage> {
        @Override
        public boolean supports(DeviceCommand command) {
            return command != null;
        }

        @Override
        public Map<String, Object> toOutboundPayload(DeviceCommand command) {
            return Map.of("CMD", command.getCommand());
        }

        @Override
        public Optional<DeviceCommand> fromInboundResponse(DeviceMessage message) {
            return message.getAttributeValue("teltonika_response", String.class)
                    .or(() -> message.getAttributeValue("response", String.class))
                    .map(DeviceCommand::new);
        }
    }

    public static class TeltonikaMqttJsonProtocol implements DeviceProtocol {

        private final TeltonikaRegistry registry;
        private final TeltonikaCommandMapper commandMapper;
        private final TeltonikaAttributeResolver attributeResolver;

        public TeltonikaMqttJsonProtocol(TeltonikaRegistry registry, TeltonikaCommandMapper commandMapper) {
            this.registry = registry;
            this.commandMapper = commandMapper;
            this.attributeResolver = new TeltonikaAttributeResolver();
        }

        @Override
        public String getProtocolId() {
            return PROTOCOL_MQTT_JSON;
        }

        @Override
        public String getVendorName() {
            return "Teltonika Telematics";
        }

        @Override
        public TelematicsParameterRegistry<?> getParameterRegistry() {
            return registry;
        }

        @Override
        public boolean canHandle(ByteBuf data, MessageContext context) {
            return context.getTransport() == MessageContext.Transport.MQTT;
        }

        @Override
        public List<DeviceMessage> decode(ByteBuf data, MessageContext context) throws ProtocolDecodeException {
            try {
                byte[] bytes = new byte[data.readableBytes()];
                data.getBytes(data.readerIndex(), bytes);

                String deviceId = context.getDeviceId()
                        .orElseThrow(() -> new ProtocolDecodeException(getProtocolId(), "MessageContext.deviceId is required for MQTT decode"));
                JsonNode root = ValueUtil.JSON.readTree(bytes);
                Map<String, Object> payload = extractPayload(root);
                long timestamp = extractTimestamp(payload).orElse(System.currentTimeMillis());

                List<Attribute<?>> attributes = new ArrayList<>();
                for (Map.Entry<String, Object> entry : payload.entrySet()) {
                    String parameterId = entry.getKey();
                    Attribute<?> attr = attributeResolver.resolveJson(parameterId, entry.getValue(), timestamp);
                    attr.addMeta(new MetaItem<>(READ_ONLY, true));
                    attributes.add(attr);
                }

                if (attributes.isEmpty()) {
                    throw new ProtocolDecodeException(getProtocolId(), deviceId, "MQTT JSON payload does not contain decodable values");
                }

                DeviceMessage deviceMessage = DeviceMessage.builder()
                        .deviceId(deviceId)
                        .protocolName(getProtocolId())
                        .addAttributes(attributes)
                        .build();

                context.setCodecName(CODEC_JSON);
                return List.of(deviceMessage);
            } catch (IOException e) {
                throw new ProtocolDecodeException(getProtocolId(), "Invalid Teltonika MQTT JSON payload: " + e.getMessage());
            } catch (RuntimeException e) {
                throw new ProtocolDecodeException(getProtocolId(), "Failed to decode Teltonika MQTT JSON payload: " + e.getMessage(), e);
            }
        }

        @Override
        public Optional<ByteBuf> encodeCommand(DeviceCommand command, MessageContext context) throws ProtocolEncodeException {
            try {
                Map<String, Object> payload = commandMapper.toOutboundPayload(command);
                String json = ValueUtil.asJSON(payload)
                        .orElseThrow(() -> new ProtocolEncodeException(getProtocolId(), command.getCommand(), "Cannot serialize command payload"));
                return Optional.of(Unpooled.wrappedBuffer(json.getBytes(StandardCharsets.UTF_8)));
            }catch (Exception e) {
                throw new ProtocolEncodeException(getProtocolId(), command.getCommand(), e.getMessage());
            }
        }

        @Override
        public Optional<ByteBuf> getAcknowledgment(int messageCount, MessageContext context) {
            return Optional.empty();
        }

        private Map<String, Object> extractPayload(JsonNode root) throws ProtocolDecodeException {
            JsonNode reported = root.path("state").path("reported");
            if (reported.isObject()) {
                return ValueUtil.JSON.convertValue(reported, ValueUtil.JSON.getTypeFactory().constructMapType(LinkedHashMap.class, String.class, Object.class));
            }

            JsonNode rsp = root.get("RSP");
            if (rsp == null || rsp.isNull()) {
                throw new ProtocolDecodeException(getProtocolId(), "Unsupported Teltonika MQTT JSON structure: expected state.reported or RSP");
            }

            String response = rsp.isObject() ? rsp.path("RSP").asText(null) : rsp.asText(null);
            if (response == null) {
                throw new ProtocolDecodeException(getProtocolId(), "RSP payload is missing string value");
            }

            return Map.of("response", response);
        }

        private Optional<Long> extractTimestamp(Map<String, Object> payload) {
            Object raw = payload.get(TeltonikaParameters.TIMESTAMP.getId());
            if (raw == null) {
                raw = payload.get(TeltonikaParameters.TIMESTAMP.getName());
            }
            if (raw == null) {
                return Optional.empty();
            }
            return Optional.of(raw instanceof Number n ? n.longValue() : Long.parseLong(raw.toString()));
        }
    }

    public static class TeltonikaTransportProtocol implements DeviceProtocol {

        private final String protocolId;
        private final MessageContext.Transport transport;
        private final TeltonikaRegistry registry;
        private final TeltonikaCommandMapper commandMapper;

        public TeltonikaTransportProtocol(String protocolId,
                                          MessageContext.Transport transport,
                                          TeltonikaRegistry registry,
                                          TeltonikaCommandMapper commandMapper) {
            this.protocolId = protocolId;
            this.transport = transport;
            this.registry = registry;
            this.commandMapper = commandMapper;
        }

        @Override
        public String getProtocolId() {
            return protocolId;
        }

        @Override
        public String getVendorName() {
            return "Teltonika Telematics";
        }

        @Override
        public TelematicsParameterRegistry<?> getParameterRegistry() {
            return registry;
        }

        @Override
        public boolean canHandle(ByteBuf data, MessageContext context) {
            return context.getTransport() == transport;
        }

        @Override
        public List<DeviceMessage> decode(ByteBuf data, MessageContext context) throws ProtocolDecodeException {
            throw new ProtocolDecodeException(protocolId, "Decode is handled by transport-specific Netty decoder for " + transport);
        }

        @Override
        public Optional<ByteBuf> encodeCommand(DeviceCommand command, MessageContext context) throws ProtocolEncodeException {
            try {
                Map<String, Object> payload = commandMapper.toOutboundPayload(command);
                String json = ValueUtil.asJSON(payload)
                        .orElseThrow(() -> new ProtocolEncodeException(protocolId, command.getCommand(), "Cannot serialize command payload"));
                return Optional.of(Unpooled.wrappedBuffer(json.getBytes(StandardCharsets.UTF_8)));
            } catch (ProtocolEncodeException e) {
                throw e;
            } catch (Exception e) {
                throw new ProtocolEncodeException(protocolId, command.getCommand(), e.getMessage());
            }
        }

        @Override
        public Optional<ByteBuf> getAcknowledgment(int messageCount, MessageContext context) {
            return Optional.empty();
        }
    }

    public static class TeltonikaSessionManager implements DeviceSessionManager {

        private final Map<String, DeviceSession> sessions = new ConcurrentHashMap<>();
        private final List<SessionListener> listeners = new CopyOnWriteArrayList<>();

        @Override
        public DeviceSession getOrCreate(String deviceId, DeviceProtocol protocol, String realm) {
            return sessions.computeIfAbsent(deviceId, id -> {
                DeviceSession created = new TeltonikaSession(id, protocol, realm);
                listeners.forEach(l -> l.onSessionCreated(created));
                return created;
            });
        }

        @Override
        public Optional<DeviceSession> get(String deviceId) {
            return Optional.ofNullable(sessions.get(deviceId));
        }

        @Override
        public Optional<DeviceSession> getByAssetId(String assetId) {
            return sessions.values().stream().filter(s -> s.getAssetId().isPresent() && s.getAssetId().get().equals(assetId)).findFirst();
        }

        @Override
        public Optional<DeviceSession> remove(String deviceId) {
            DeviceSession removed = sessions.remove(deviceId);
            if (removed != null) {
                listeners.forEach(l -> l.onSessionRemoved(removed));
            }
            return Optional.ofNullable(removed);
        }

        @Override
        public java.util.stream.Stream<DeviceSession> all() {
            return sessions.values().stream();
        }

        @Override
        public java.util.stream.Stream<DeviceSession> byRealm(String realm) {
            return sessions.values().stream().filter(s -> s.getRealm().equals(realm));
        }

        @Override
        public java.util.stream.Stream<DeviceSession> byProtocol(String protocolId) {
            return sessions.values().stream().filter(s -> protocolId.equals(s.getProtocol().getProtocolId()));
        }

        @Override
        public int getActiveSessionCount() {
            return sessions.size();
        }

        @Override
        public int cleanupTimedOut(long timeoutSeconds) {
            List<String> toRemove = new ArrayList<>();
            sessions.forEach((id, session) -> {
                if (session.isTimedOut(timeoutSeconds)) {
                    toRemove.add(id);
                }
            });
            toRemove.forEach(this::remove);
            return toRemove.size();
        }

        @Override
        public void addListener(SessionListener listener) {
            listeners.add(listener);
        }

        @Override
        public void removeListener(SessionListener listener) {
            listeners.remove(listener);
        }
    }

    private static class TeltonikaSession implements DeviceSession {
        private final String deviceId;
        private final DeviceProtocol protocol;
        private final String realm;
        private volatile State state = State.CONNECTING;
        private volatile Instant createdAt = Instant.now();
        private volatile Instant lastSeen = createdAt;
        private volatile String assetId;
        private volatile long messageCount = 0;

        private TeltonikaSession(String deviceId, DeviceProtocol protocol, String realm) {
            this.deviceId = deviceId;
            this.protocol = protocol;
            this.realm = realm;
        }

        @Override
        public String getDeviceId() {
            return deviceId;
        }

        @Override
        public Optional<String> getAssetId() {
            return Optional.ofNullable(assetId);
        }

        @Override
        public void setAssetId(String assetId) {
            this.assetId = assetId;
        }

        @Override
        public DeviceProtocol getProtocol() {
            return protocol;
        }

        @Override
        public State getState() {
            return state;
        }

        @Override
        public Instant getLastSeen() {
            return lastSeen;
        }

        @Override
        public Instant getCreatedAt() {
            return createdAt;
        }

        @Override
        public String getRealm() {
            return realm;
        }

        @Override
        public void onMessage(DeviceMessage message) {
            lastSeen = Instant.now();
            messageCount++;
            state = State.CONNECTED;
        }

        @Override
        public void onConnect() {
            state = State.CONNECTED;
            lastSeen = Instant.now();
        }

        @Override
        public void onDisconnect() {
            state = State.DISCONNECTED;
        }

        @Override
        public boolean queueCommand(DeviceCommand command) {
            return true;
        }

        @Override
        public long getMessageCount() {
            return messageCount;
        }
    }
}
