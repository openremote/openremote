package org.openremote.manager.mqtt;

import io.netty.buffer.ByteBuf;
import org.apache.activemq.artemis.core.config.Configuration;
import org.apache.activemq.artemis.spi.core.protocol.RemotingConnection;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.keycloak.KeycloakSecurityContext;
import org.openremote.manager.asset.AssetProcessingService;
import org.openremote.manager.asset.AssetStorageService;
import org.openremote.manager.setup.SetupService;
import org.openremote.model.Container;
import org.openremote.model.asset.Asset;
import org.openremote.model.attribute.Attribute;
import org.openremote.model.attribute.AttributeEvent;
import org.openremote.model.attribute.AttributeMap;
import org.openremote.model.attribute.AttributeState;
import org.openremote.model.geo.GeoJSONPoint;
import org.openremote.model.protocol.mqtt.Topic;
import org.openremote.model.telematics.Message;
import org.openremote.model.telematics.Payload;
import org.openremote.model.telematics.TrackerAsset;
import org.openremote.model.telematics.teltonika.TeltonikaMessage;
import org.openremote.model.telematics.teltonika.TeltonikaMqttMessage;
import org.openremote.model.util.UniqueIdentifierGenerator;
import org.openremote.model.util.ValueUtil;
import org.openremote.model.value.AttributeDescriptor;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.*;
import java.util.logging.Logger;

public class TeltonikaMQTTHandler extends MQTTHandler {
    private static final String TELTONIKA_DEVICE_RECEIVE_TOPIC = "data";
    private static final String TELTONIKA_DEVICE_SEND_TOPIC = "commands";
    private static final String TELTONIKA_DEVICE_TOKEN = "teltonika";

    protected AssetStorageService assetStorageService;
    protected AssetProcessingService assetProcessingService;


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
        long imeiValue;
        try{
            imeiValue = Long.parseLong(topic.getTokens()[3]);
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
    public void onPublish(RemotingConnection connection, Topic topic, ByteBuf body) {
        try {
            Message message = null;

            Asset<? extends TrackerAsset> tracker = getCreateAssetFromTopic(topic);

            byte[] bytes = new byte[body.readableBytes()];
            body.readBytes(bytes);
            try {
                message = new TeltonikaMqttMessage(ValueUtil.JSON.readTree(bytes), null);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            Payload payload = message.getPayloadByIndex(0);
            Long timestamp = payload.getTimestamp();

            AttributeMap map = new AttributeMap();
            map.addAll(message.getPayloadByIndex(0).asMap().entrySet().stream().map(kvp ->
                    new Attribute(kvp.getKey().getDescriptor(), kvp.getKey().getValue().apply(kvp.getValue()), timestamp)).toArray(Attribute[]::new));

            sendAttributeEvents(tracker, map.stream().toArray(Attribute[]::new));

        }catch (Exception e){
            getLogger().severe("Error processing Teltonika MQTT message on topic "+topic+": "+e.getMessage());
            getLogger().warning(ExceptionUtils.getStackTrace(e));
        }
    }

    private void sendAttributeEvents(Asset<? extends TrackerAsset> asset, Attribute<?>[] attrs) {
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

        oldAttributes.stream().map(attr -> new AttributeEvent(asset.getId(), attr.getName(), attr.getValue().get()))
                .forEach(assetProcessingService::sendAttributeEvent);

    }

    public Asset<? extends TrackerAsset> getCreateAssetFromTopic(Topic topic){
        String imei = getImeiFromTopic(topic);
        Asset<? extends TrackerAsset> asset = assetStorageService.find(UniqueIdentifierGenerator.generateId(imei), TrackerAsset.class);
        if(asset == null){
            getLogger().warning("No asset found for IMEI: "+imei);
            asset = new TrackerAsset()
                    .setRealm(topicRealm(topic))
                    .setName("Teltonika Device " + imei)
                    .setId(UniqueIdentifierGenerator.generateId(getImeiFromTopic(topic)))
                    .setImei(imei)
                    .setManufacturer("Teltonika")
                    .setModel("")
                    .setCodec("Codec JSON")
                    .setProtocol("MQTT");

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
}
