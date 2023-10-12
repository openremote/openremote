package org.openremote.manager.mqtt;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.mqtt.MqttQoS;
import net.fortuna.ical4j.model.property.Tel;
import org.apache.activemq.artemis.spi.core.protocol.RemotingConnection;
import org.apache.tools.ant.taskdefs.optional.windows.Attrib;
import org.keycloak.KeycloakSecurityContext;
import org.openremote.container.timer.TimerService;
import org.openremote.container.util.UniqueIdentifierGenerator;
import org.openremote.manager.asset.AssetStorageService;
import org.openremote.manager.security.ManagerIdentityService;
import org.openremote.manager.security.ManagerKeycloakIdentityProvider;
import org.openremote.model.Container;
import org.openremote.model.asset.Asset;
import org.openremote.model.asset.AssetEvent;
import org.openremote.model.asset.impl.CarAsset;
import org.openremote.model.attribute.Attribute;
import org.openremote.model.attribute.AttributeEvent;
import org.openremote.model.attribute.AttributeMap;
import org.openremote.model.attribute.AttributeRef;
import org.openremote.model.event.shared.EventFilter;
import org.openremote.model.event.shared.SharedEvent;
import org.openremote.model.query.AssetQuery;
import org.openremote.model.syslog.SyslogCategory;
import org.openremote.model.teltonika.TeltonikaParameter;
import org.openremote.model.teltonika.TeltonikaPayload;
import org.openremote.model.value.AttributeDescriptor;
import org.openremote.model.value.ValueType;
import org.w3c.dom.Attr;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Consumer;
import java.util.logging.Logger;

import static org.openremote.manager.event.ClientEventService.CLIENT_INBOUND_QUEUE;
import static org.openremote.model.syslog.SyslogCategory.API;

public class TeltonikaMQTTHandler extends MQTTHandler {
    private class TeltonikaDevice {
        String clientId;
        String commandTopic;

        public TeltonikaDevice(Topic topic) {
            this.clientId = topic.tokens.get(1);
            this.commandTopic = String.format("%s/%s/teltonika/%s/commands", topicRealm(topic), this.clientId, topic.tokens.get(3));
        }
    }

    private static final String TOKEN_TELTONIKA_DEVICE = "teltonika";

    private static final Logger LOG = SyslogCategory.getLogger(API, TeltonikaMQTTHandler.class);

    protected AssetStorageService assetStorageService;
    protected TimerService timerService;
    protected Path DeviceParameterPath;

    protected final ConcurrentMap<String, TeltonikaDevice> connectionSubscriberInfoMap = new ConcurrentHashMap<>();




    /**
     * Indicates if this handler will handle the specified topic; independent of whether it is a publish or subscribe.
     * Should generally check the third token (index 2) onwards unless {@link #handlesTopic} has been overridden.
     *
     */
    @Override
    protected boolean topicMatches(Topic topic) {
        if ("teltonika".equalsIgnoreCase(topicTokenIndexToString(topic, 2))){
            getLogger().info("Topic Matches Teltonika Handler");
            return true;
        }
        return false;
    }

    @Override
    public void start(Container container) throws Exception {
        super.start(container);
        getLogger().info("Starting Teltonika MQTT Handler");
        ManagerIdentityService identityService = container.getService(ManagerIdentityService.class);
        assetStorageService = container.getService(AssetStorageService.class);
        timerService = container.getService(TimerService.class);
        DeviceParameterPath = Paths.get("/deployment/manager/fleet/FMC003.json");
        if (!identityService.isKeycloakEnabled()) {
            getLogger().warning("MQTT connections are not supported when not using Keycloak identity provider");
            isKeycloak = false;
        } else {
            isKeycloak = true;
            identityProvider = (ManagerKeycloakIdentityProvider) identityService.getIdentityProvider();
        }

        clientEventService.addInternalSubscription(
                AttributeEvent.class,
                null,
                this::handleAttributeMessage);
    }

    private void handleAttributeMessage(AttributeEvent event) {
        getLogger().info(event.toString());
        List<Asset<?>> assetsWithAttribute = assetStorageService.findAll(new AssetQuery().types(CarAsset.class).attributeName("sendToDevice"));

        for (Asset<?> asset : assetsWithAttribute) {
            if(asset.hasAttribute("sendToDevice")){
                if(Objects.equals(event.getAssetId(), asset.getId())){
//                    new AttributeRef(asset.getId(), "sendToDevice")
                    Attribute<String> imei = asset.getAttribute("IMEI", String.class).get();
                    String imeiString = imei.getValue().get();
                    TeltonikaDevice deviceInfo = connectionSubscriberInfoMap.get(imeiString);
                    if(deviceInfo == null) {
                        getLogger().info(String.format("Device %s is not subscribed to topic, not posting message", imeiString));
                        return;
                    } else{
                        this.sendCommandToTeltonikaDevice((String)event.getValue().get(), deviceInfo);
                    }

                }
            }
        }


        getLogger().info("fired");
    }

    private void sendCommandToTeltonikaDevice(String command, TeltonikaDevice device) {
        String commandSource = String.format("{'CMD': '%s'}", command);
        mqttBrokerService.publishMessage(device.commandTopic, commandSource, MqttQoS.EXACTLY_ONCE);
    }

    @Override
    protected Logger getLogger() {
        return LOG;
    }

    //Users should be able to subscribe for now
    @Override
    public boolean checkCanSubscribe(RemotingConnection connection, KeycloakSecurityContext securityContext, Topic topic) {
        return true;
    }

    @Override
    public boolean canSubscribe(RemotingConnection connection, KeycloakSecurityContext securityContext, Topic topic) {
        getLogger().info("Topic "+topic.toString()+" is not subscribe-able");
        return true;
    }

    /**
     * Overrides MQTTHandler.checkCanPublish for this specific Handler,
     * until secure Authentication and Auto-provisioning
     * of Teltonika Devices is created.
     * To be removed after implementation is complete.
     */
    @Override
    public boolean checkCanPublish(RemotingConnection connection, KeycloakSecurityContext securityContext, Topic topic) {
        return true;
    }

    @Override
    public boolean canPublish(RemotingConnection connection, KeycloakSecurityContext securityContext, Topic topic) {
        getLogger().info("Teltonika device will publish to Topic "+topic.toString()+" to transmit payload");
        return true;
    }

    public void onSubscribe(RemotingConnection connection, Topic topic) {
        getLogger().info("device "+topic.tokens.get(1)+" connected to topic "+topic.toString()+".");
        connectionSubscriberInfoMap.put(topic.tokens.get(3), new TeltonikaDevice(topic));
    }

    @Override
    public void onUnsubscribe(RemotingConnection connection, Topic topic) {
        getLogger().info("device "+topic.tokens.get(1)+" disconnected from topic "+topic.toString()+".");
        connectionSubscriberInfoMap.remove(topic.tokens.get(3));
    }

    /**
     * Get the set of topics this handler wants to subscribe to for incoming publish messages; messages that match
     * these topics will be passed to {@link #onPublish}.
     * The listener topics are defined as <code>{realmID}/{userID}/{@value TOKEN_TELTONIKA_DEVICE}/{IMEI}</code>
     */

    @Override
    public Set<String> getPublishListenerTopics() {
        getLogger().fine("getPublishListenerTopics");
        return Set.of(
                TOKEN_SINGLE_LEVEL_WILDCARD + "/" + TOKEN_SINGLE_LEVEL_WILDCARD + "/" + TOKEN_TELTONIKA_DEVICE + "/" + TOKEN_MULTI_LEVEL_WILDCARD
        );
    }


    @Override
    public void onPublish(RemotingConnection connection, Topic topic, ByteBuf body) {
        String payloadContent = body.toString(StandardCharsets.UTF_8);
        getLogger().info(payloadContent);
        String deviceImei = topic.tokens.get(3);
        String realm = topic.tokens.get(0);

        String deviceUuid = UniqueIdentifierGenerator.generateId(deviceImei);
        getLogger().info("IMEI Linked to Asset ID:"+ deviceUuid);

        Asset<?> asset = assetStorageService.find(deviceUuid);
        AttributeMap attributes = getAttributesFromPayload(payloadContent);
        // Check if asset was found
        if (asset == null) CreateNewAsset(payloadContent, deviceUuid, deviceImei, realm, attributes);
        else UpdateAsset(payloadContent, asset, attributes,topic, connection);
    }

    private void CreateNewAsset(String payloadContent, String newDeviceId, String newDeviceImei, String realm, AttributeMap attributes) {
        getLogger().info("Creating CarAsset with IMEI "+newDeviceImei+" and payload:");
        getLogger().info(payloadContent);

        CarAsset testAsset = new CarAsset("Teltonika Asset "+newDeviceImei)
                .setRealm(realm)
                .setId(newDeviceId);

        attributes.add(new Attribute<>("IMEI", ValueType.TEXT, newDeviceImei));
        attributes.add(new Attribute<>("lastContact", ValueType.DATE_AND_TIME, new Date(timerService.getCurrentTimeMillis())));

        testAsset.addOrReplaceAttributes(attributes.stream().toArray(Attribute[]::new));
        Asset<?> mergedAsset = assetStorageService.merge(testAsset);

        if(mergedAsset != null){
            getLogger().info("Created Asset through Teltonika: " + testAsset);
//            getLogger().info()
        }else{
            getLogger().info("Failed to create Asset: " + testAsset);
        }
    }

    /**
     * Returns list of attributes depending on the Teltonika JSON Payload.
     * Uses the logic and results from parsing the Teltonika Parameter IDs.
     *
     * @param payloadContent Payload coming from Teltonika device
     * @return Map of {@link Attribute}s to be assigned to the {@link Asset}.
     */
    private AttributeMap getAttributesFromPayload(String payloadContent) {
        HashMap<Integer, TeltonikaParameter> params = new HashMap<>();
        ObjectMapper mapper = new ObjectMapper();
        try {
            // Parse file with Parameter details
            TeltonikaParameter[] paramArray = mapper.readValue(getParameterFileString(), TeltonikaParameter[].class);

            getLogger().info("Parsed "+paramArray.length+" Teltonika Parameters");

            // Add each element to the HashMap, with the key being the unique parameter ID and the parameter
            // being the value
            for (TeltonikaParameter param : paramArray) {
                params.put(param.getPropertyId(), param);
            }
        } catch (Exception e) {
            getLogger().info(e.toString());
        }
        //Parameters parsed, time to understand the payload
        TeltonikaPayload payload;
        try {
            payload = mapper.readValue(payloadContent, TeltonikaPayload.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        return payload.state.GetAttributes(params);
    }

    private String getParameterFileString() {
        try {
            return Files.readString(DeviceParameterPath);
        } catch (IOException e) {
            getLogger().warning("Couldn't find FMC003.json, couldn't parse parameters");
            throw new RuntimeException(e);
        }
    }

    private void UpdateAsset(String payloadContent, Asset<?> asset, AttributeMap attributes, Topic topic, RemotingConnection connection) {

        String imei = asset.getAttribute("IMEI").toString();

        getLogger().info("Updating CarAsset with IMEI "+imei+" and payload:");
        getLogger().info(payloadContent);

        attributes.forEach( attribute ->  {
            Map<String, Object> headers = DefaultMQTTHandler.prepareHeaders(topicRealm(topic), connection);
            AttributeEvent attributeEvent = new AttributeEvent(asset.getId(), attribute.getName(), attribute.getValue());
            messageBrokerService.getFluentProducerTemplate()
                    .withHeaders(headers)
                    .withBody(attributeEvent)
                    .to(CLIENT_INBOUND_QUEUE)
                    .asyncSend();
        });
    }


}
