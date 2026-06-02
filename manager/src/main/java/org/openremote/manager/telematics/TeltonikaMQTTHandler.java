package org.openremote.manager.telematics;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.mqtt.MqttQoS;
import org.apache.activemq.artemis.spi.core.protocol.RemotingConnection;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.keycloak.KeycloakSecurityContext;
import org.openremote.manager.mqtt.MQTTHandler;
import org.openremote.model.Container;
import org.openremote.model.protocol.mqtt.Topic;
import org.openremote.model.telematics.core.DeviceMessage;
import org.openremote.model.telematics.protocol.DeviceCommand;
import org.openremote.model.telematics.protocol.DeviceProtocol;
import org.openremote.model.telematics.protocol.MessageContext;
import org.openremote.model.telematics.protocol.ProtocolDecodeException;
import org.openremote.model.telematics.protocol.ProtocolEncodeException;
import org.openremote.model.util.ValueUtil;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Logger;

public class TeltonikaMQTTHandler extends MQTTHandler {

    private static final Logger LOG = Logger.getLogger(TeltonikaMQTTHandler.class.getName());

    public static final String TELTONIKA_DEVICE_RECEIVE_TOPIC = "data";
    public static final String TELTONIKA_DEVICE_SEND_TOPIC = "commands";
    public static final String TELTONIKA_DEVICE_TOKEN = "teltonika";

    protected TelematicsService telematicsService;
    protected TeltonikaVendor teltonikaVendor;
    protected DeviceProtocol mqttProtocol;

    @Override
    public void start(Container container) throws Exception {
        super.start(container);
        telematicsService = container.getService(TelematicsService.class);
        this.teltonikaVendor = (TeltonikaVendor) telematicsService.getVendor(TELTONIKA_DEVICE_TOKEN).orElseThrow();
        this.mqttProtocol = teltonikaVendor.getProtocol(MessageContext.Transport.MQTT);
    }

    @Override
    protected boolean topicMatches(Topic topic) {
        return TELTONIKA_DEVICE_TOKEN.equalsIgnoreCase(topicTokenIndexToString(topic, 2));
    }

    @Override
    protected Logger getLogger() {
        return LOG;
    }

    @Override
    public boolean checkCanSubscribe(RemotingConnection connection, KeycloakSecurityContext securityContext, Topic topic) {
        if (!canSubscribe(connection, securityContext, topic)) {
            getLogger().warning("Cannot subscribe to this topic, topic=" + topic + ", connection" + connection);
            return false;
        }
        return true;
    }

    @Override
    public boolean canSubscribe(RemotingConnection connection, KeycloakSecurityContext securityContext, Topic topic) {
        if (topic.getTokens().length < 5) {
            getLogger().warning(MessageFormat.format("Topic {0} is not a valid Topic. Please use a valid Topic.", topic));
            return false;
        }
        try {
            Long.parseLong(topic.getTokens()[3]);
        } catch (NumberFormatException e) {
            getLogger().warning(MessageFormat.format("IMEI {0} is not a valid IMEI value. Please use a valid IMEI value.", topic.getTokens()[3]));
            return false;
        }
        return Objects.equals(topic.getTokens()[2], TELTONIKA_DEVICE_TOKEN)
                && (Objects.equals(topic.getTokens()[4], TELTONIKA_DEVICE_RECEIVE_TOPIC)
                || Objects.equals(topic.getTokens()[4], TELTONIKA_DEVICE_SEND_TOPIC));
    }

    @Override
    public boolean checkCanPublish(RemotingConnection connection, KeycloakSecurityContext securityContext, Topic topic) {
        return canPublish(connection, securityContext, topic);
    }

    @Override
    public boolean canPublish(RemotingConnection connection, KeycloakSecurityContext securityContext, Topic topic) {
        getLogger().finer("Teltonika device will publish to Topic " + topic + " to transmit payload");
        return true;
    }

    @Override
    public void onSubscribe(RemotingConnection connection, Topic topic) {
        getLogger().info("CONNECT: Device " + topic.getTokens()[1] + " connected to topic " + topic);
        String imei = getImeiFromTopic(topic);
        telematicsService.markTrackerConnected(
                teltonikaVendor,
                imei,
                topicRealm(topic),
                mqttProtocol.getProtocolId(),
                TeltonikaVendor.CODEC_JSON,
                MessageContext.Transport.MQTT
        );
    }

    @Override
    public void onUnsubscribe(RemotingConnection connection, Topic topic) {
        getLogger().info("DISCONNECT: Device " + topic.getTokens()[1] + " disconnected from topic " + topic);
        telematicsService.markTrackerDisconnected(TELTONIKA_DEVICE_TOKEN, getImeiFromTopic(topic));
    }

    @Override
    public Set<String> getPublishListenerTopics() {
        return Set.of(
                TOKEN_SINGLE_LEVEL_WILDCARD + "/" + TOKEN_SINGLE_LEVEL_WILDCARD + "/"
                        + TELTONIKA_DEVICE_TOKEN + "/" + TOKEN_SINGLE_LEVEL_WILDCARD + "/" + TELTONIKA_DEVICE_RECEIVE_TOPIC,
                TOKEN_SINGLE_LEVEL_WILDCARD + "/" + TOKEN_SINGLE_LEVEL_WILDCARD + "/"
                        + TELTONIKA_DEVICE_TOKEN + "/" + TOKEN_SINGLE_LEVEL_WILDCARD + "/" + TELTONIKA_DEVICE_SEND_TOPIC
        );
    }

    public void sendCommand(String realm, String clientId, String imei, DeviceCommand command) {
        String topic = getCommandTopic(realm, clientId, imei);
        MessageContext context = new MessageContext(MessageContext.Transport.MQTT)
                .setRealm(realm)
                .setDeviceId(imei)
                .setCodecName(TeltonikaVendor.CODEC_JSON);

        try {
            ByteBuf encoded = mqttProtocol.encodeCommand(command, context)
                    .orElseThrow(() -> new ProtocolEncodeException(mqttProtocol.getProtocolId(), command.getCommand(), "Protocol returned empty payload"));
            try {
                byte[] bytes = new byte[encoded.readableBytes()];
                encoded.getBytes(encoded.readerIndex(), bytes);
                Object jsonPayload = ValueUtil.JSON.readTree(bytes);
                publishMessage(topic, jsonPayload, MqttQoS.AT_LEAST_ONCE);
            } finally {
                encoded.release();
            }
        } catch (ProtocolEncodeException | IOException e) {
            getLogger().warning("Failed to send Teltonika command to " + imei + ": " + e.getMessage());
        }
    }

    public void sendCommand(String realm, String clientId, String imei, String command) {
        sendCommand(realm, clientId, imei, new DeviceCommand(command));
    }

    @Override
    public void onPublish(RemotingConnection connection, Topic topic, ByteBuf body) {
        try {
            String imei = getImeiFromTopic(topic);
            MessageContext context = new MessageContext(MessageContext.Transport.MQTT)
                    .setRealm(topicRealm(topic))
                    .setDeviceId(imei)
                    .setCodecName(TeltonikaVendor.CODEC_JSON);

            byte[] bytes = new byte[body.readableBytes()];
            body.readBytes(bytes);
            ByteBuf decodeBuffer = Unpooled.wrappedBuffer(bytes);

            try {
                for (DeviceMessage deviceMessage : mqttProtocol.decode(decodeBuffer, context)) {
                    telematicsService.submitMessage(
                            TELTONIKA_DEVICE_TOKEN,
                            topicRealm(topic),
                            MessageContext.Transport.MQTT,
                            TeltonikaVendor.CODEC_JSON,
                            deviceMessage
                    );
                }
            } finally {
                decodeBuffer.release();
            }
        } catch (ProtocolDecodeException e) {
            getLogger().severe("Failed Teltonika MQTT decode on topic " + topic + ": " + e.getMessage());
        } catch (Exception e) {
            getLogger().severe("Error processing Teltonika MQTT message on topic " + topic + ": " + e.getMessage());
            getLogger().warning(ExceptionUtils.getStackTrace(e));
        }
    }

    String getImeiFromTopic(Topic topic) {
        return topic.getTokens()[3];
    }

    String getCommandTopic(String realm, String clientId, String imei) {
        return realm + "/" + clientId + "/" + TELTONIKA_DEVICE_TOKEN + "/" + imei + "/" + TELTONIKA_DEVICE_SEND_TOPIC;
    }

    Map<String, Object> toCommandPayload(DeviceCommand command) {
        return Map.of("ts", System.currentTimeMillis(), "CMD", command.getCommand());
    }
}
