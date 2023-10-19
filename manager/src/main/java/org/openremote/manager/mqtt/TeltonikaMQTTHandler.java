package org.openremote.manager.mqtt;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.mqtt.MqttQoS;
import org.apache.activemq.artemis.spi.core.protocol.RemotingConnection;
import org.keycloak.KeycloakSecurityContext;
import org.openremote.container.timer.TimerService;
import org.openremote.container.util.UniqueIdentifierGenerator;
import org.openremote.manager.asset.AssetStorageService;
import org.openremote.manager.security.ManagerIdentityService;
import org.openremote.manager.security.ManagerKeycloakIdentityProvider;
import org.openremote.model.Container;
import org.openremote.model.asset.Asset;
import org.openremote.model.asset.AssetFilter;
import org.openremote.model.asset.impl.CarAsset;
import org.openremote.model.attribute.Attribute;
import org.openremote.model.attribute.AttributeEvent;
import org.openremote.model.attribute.AttributeMap;
import org.openremote.model.attribute.MetaItem;
import org.openremote.model.query.AssetQuery;
import org.openremote.model.syslog.SyslogCategory;
import org.openremote.model.teltonika.TeltonikaMessageResponse;
import org.openremote.model.teltonika.TeltonikaParameter;
import org.openremote.model.teltonika.TeltonikaPayload;
import org.openremote.model.value.AttributeDescriptor;
import org.openremote.model.value.MetaItemType;
import org.openremote.model.value.ValueType;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Logger;

import static org.openremote.manager.event.ClientEventService.CLIENT_INBOUND_QUEUE;
import static org.openremote.model.syslog.SyslogCategory.API;

public class TeltonikaMQTTHandler extends MQTTHandler {
    //TODO: Maybe move this to Models?
    private static class TeltonikaDevice {
        String clientId;
        String commandTopic;

        public TeltonikaDevice(Topic topic) {
            this.clientId = topic.tokens.get(1);
            this.commandTopic = String.format("%s/%s/teltonika/%s/commands",
                    topicRealm(topic),
                    this.clientId,
                    topic.tokens.get(3));
        }
    }

    private static final String TOKEN_TELTONIKA_DEVICE = "teltonika";
    private static final String DEVICE_SEND_COMMAND_ATTRIBUTE_NAME = "sendToDevice";
    private static final String DEVICE_RECEIVE_COMMAND_ATTRIBUTE_NAME = "response";

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
        return TOKEN_TELTONIKA_DEVICE.equalsIgnoreCase(topicTokenIndexToString(topic, 2));
    }

    @Override
    public void start(Container container) throws Exception {
        super.start(container);
        getLogger().info("Starting Teltonika MQTT Handler");
        ManagerIdentityService identityService = container.getService(ManagerIdentityService.class);
        assetStorageService = container.getService(AssetStorageService.class);
        timerService = container.getService(TimerService.class);
        DeviceParameterPath = Paths.get("deployment/manager/fleet/FMC003.json");
        if (!identityService.isKeycloakEnabled()) {
            getLogger().warning("MQTT connections are not supported when not using Keycloak identity provider");
            isKeycloak = false;
        } else {
            isKeycloak = true;
            identityProvider = (ManagerKeycloakIdentityProvider) identityService.getIdentityProvider();
        }
        //TODO: Figure out a way to update the AssetFilter when a new asset is a candidate to receive a response message
        // Does it even automatically refresh the Asset list?

        clientEventService.addInternalSubscription(
                AttributeEvent.class,
                buildAssetFilter(),
                this::handleAttributeMessage);
    }

    /**
     * Creates a filter for the AttributeEvents that could send a command to a Teltonika Device.
     *
     * @return AssetFilter of CarAssets that have both {@value DEVICE_RECEIVE_COMMAND_ATTRIBUTE_NAME} and
     * {@value DEVICE_SEND_COMMAND_ATTRIBUTE_NAME} as attributes.
     */
    private AssetFilter<AttributeEvent> buildAssetFilter(){
        List<Asset<?>> assetsWithAttribute = assetStorageService
                .findAll(new AssetQuery().types(CarAsset.class)
                .attributeNames(DEVICE_SEND_COMMAND_ATTRIBUTE_NAME));
        ArrayList<String> listOfCarAssetIds = new ArrayList<>();
        assetsWithAttribute.forEach(asset -> listOfCarAssetIds.add(asset.getId()));
        AssetFilter<AttributeEvent> event = new AssetFilter<>();
        event.setAssetIds(listOfCarAssetIds.toArray(new String[0]));
        event.setAttributeNames("sendToDevice");
        return event;
    }


    //Sending a message to master/teltonikaDevice1/teltonika/864636060373301/commands
    //with payload {"CMD":"getstatus"} seems to work. Maybe the quotes are the issue?
    private void handleAttributeMessage(AttributeEvent event) {
        // If this is not an AttributeEvent that updates a sendToDevice field, ignore
        if (!Objects.equals(event.getAttributeName(), DEVICE_SEND_COMMAND_ATTRIBUTE_NAME)) return;
        getLogger().info(event.getEventType());
        //Find the asset in question
        Asset<?> asset = assetStorageService.find(new AssetQuery().ids(event.getAssetId()).types(CarAsset.class));

        // Double check, remove later, sanity checks
        if(asset.hasAttribute(DEVICE_SEND_COMMAND_ATTRIBUTE_NAME)){
            if(Objects.equals(event.getAssetId(), asset.getId())){

                //Get the IMEI of the device
                Attribute<String> imei = asset.getAttribute("IMEI", String.class).get();
                String imeiString = imei.getValue().get();

                // Get the device subscription information, and even if it's subscribed
                TeltonikaDevice deviceInfo = connectionSubscriberInfoMap.get(imeiString);
                //If it's null, the device is not subscribed, leave
                if(deviceInfo == null) {
                    //Maybe it does not have to be subscribed to the topic to receive the message?
                    //When subscribed in MQTT Explorer, it works, but when not, deviceInfo is null

                    getLogger().info(String.format("Device %s is not subscribed to topic, not posting message",
                            imeiString));
                    return;
                // If it is subscribed, check that there is a command there, and send the command
                } else{
                    if(event.getValue().isPresent()){
                        this.sendCommandToTeltonikaDevice((String)event.getValue().get(), deviceInfo);
                    }
                    else{
                        getLogger().warning("Attribute sendToDevice was empty");
                    }
                }

            }
        }


        getLogger().info("fired");
    }

    /**
     * Sends a Command to the {@link TeltonikaDevice} in the correct format.
     *
     * @param command string of the command, without preformatting.
     * List of valid commands can be found in Teltonika's website.
     * @param device A {@link TeltonikaDevice} that is currently subscribed, to which to send the message to.
     */
    private void sendCommandToTeltonikaDevice(String command, TeltonikaDevice device) {
        HashMap<String, String> cmdtest = new HashMap<>();
        cmdtest.put("CMD", command);

        mqttBrokerService.publishMessage(device.commandTopic, cmdtest, MqttQoS.EXACTLY_ONCE);
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
//        getLogger().info("Topic "+topic.toString()+" is not subscribe-able");
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
        getLogger().info("CONNECT: Device "+topic.tokens.get(1)+" connected to topic "+topic+".");
        connectionSubscriberInfoMap.put(topic.tokens.get(3), new TeltonikaDevice(topic));
    }

    @Override
    public void onUnsubscribe(RemotingConnection connection, Topic topic) {
        getLogger().info("DISCONNECT: Device "+topic.tokens.get(1)+" disconnected from topic "+topic+".");
        connectionSubscriberInfoMap.remove(topic.tokens.get(3));
    }

    /**
     * Get the set of topics this handler wants to subscribe to for incoming publish messages; messages that match
     * these topics will be passed to {@link #onPublish}.
     * The listener topics are defined as <code>{realmID}/{userID}/{@value TOKEN_TELTONIKA_DEVICE}/{IMEI}</code>
     * //TODO: Be explicit about sending data to {IMEI}/data, and sending commands to {IMEI}/commands.
     */
    @Override
    public Set<String> getPublishListenerTopics() {
        return Set.of(
                TOKEN_SINGLE_LEVEL_WILDCARD + "/" + TOKEN_SINGLE_LEVEL_WILDCARD + "/" +
                        TOKEN_TELTONIKA_DEVICE + "/" + TOKEN_MULTI_LEVEL_WILDCARD
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
        try {
            AttributeMap attributes = getAttributesFromPayload(payloadContent);
            Attribute<?> payloadAttribute =  new Attribute("payload", ValueType.JSON, payloadContent);
            payloadAttribute.addMeta(new MetaItem<>(MetaItemType.STORE_DATA_POINTS, true));
            attributes.add(payloadAttribute);

            if (asset == null) CreateNewAsset(deviceUuid, deviceImei, realm, attributes);
            else UpdateAsset(asset, attributes,topic, connection);

        } catch (Exception e){
            getLogger().warning("Could not parse Teltonika device Payload.");
            getLogger().warning(payloadContent);
        }
        // Check if asset was found
    }

    /**
     * Creates a new asset with the correct "hashed" Asset ID, its IMEI,
     * in the realm the MQTT message of the device submitted,
     * and the parsed list of attributes.
     *
     * @param newDeviceId The ID of the device's Asset.
     * @param newDeviceImei The IMEI of the device. If passed to
     *                      {@link UniqueIdentifierGenerator#generateId(String)},
     *                      it should always return {@code newDeviceId}.
     * @param realm The realm to create the Asset in.
     * @param attributes The attributes to insert in the Asset.
     */
    private void CreateNewAsset(String newDeviceId, String newDeviceImei, String realm, AttributeMap attributes) {
        getLogger().info("Creating CarAsset with IMEI "+newDeviceImei);

        CarAsset testAsset = new CarAsset("Teltonika Asset "+newDeviceImei)
                .setRealm(realm)
                .setId(newDeviceId);

        //TODO: Maybe move these to getAttributes?
        attributes.add(new Attribute<>("IMEI", ValueType.TEXT, newDeviceImei));
        attributes.add(new Attribute<>("lastContact", ValueType.DATE_AND_TIME,
                new Date(timerService.getCurrentTimeMillis())));

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
    private AttributeMap getAttributesFromPayload(String payloadContent) throws JsonProcessingException {
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
            return payload.state.GetAttributes(params);
        } catch (Exception e) {
            //If the payload wasn't parsed, then it means that it is either a response to a command,
            //or it's genuinely a wrong payload.

            mapper = new ObjectMapper();
            TeltonikaMessageResponse response = mapper.readValue(payloadContent, TeltonikaMessageResponse.class);
            getLogger().info(response.rsp);
            AttributeMap map = new AttributeMap();
            map.add(new Attribute<>
                    (
                            new AttributeDescriptor<>(DEVICE_RECEIVE_COMMAND_ATTRIBUTE_NAME, ValueType.TEXT),
                            response.rsp
                    )
            );
            return map;
        }

    }

    private String getParameterFileString() {
        try {
            return Files.readString(DeviceParameterPath);
        } catch (IOException e) {
            getLogger().warning("Couldn't find FMC003.json, couldn't parse parameters");
            throw new RuntimeException(e);
        }
    }

    /**
     * Updates the {@link Asset} passed, with the {@link AttributeMap} passed.
     * I have a very big feeling that the way this is done is wrong.
     *
     * @param asset The asset to be updated.
     * @param attributes The attributes to be upserted to the Attribute.
     * @param topic The topic to which the MQTT payload was sent.
     * @param connection The connection on which the payload was sent.
     */
    private void UpdateAsset(Asset<?> asset, AttributeMap attributes, Topic topic, RemotingConnection connection) {

        String imei = asset.getAttribute("IMEI").toString();

        getLogger().info("Updating CarAsset with IMEI "+imei);
        //DONE: If the parameters do not exist, then the update fails,
        // because there is no attribute to update. ATTRIBUTE_DOESNT_EXIST
        //OBD details: Prot:6,VIN:WVGZZZ1TZBW068095,TM:15,CNT:19,ST:DATA REQUESTING,P1:0xBE3EA813,P2:0xA005B011,P3:0xFED00400,P4:0x0,MIL:0,DTC:0,ID3,Hdr:7E8,Phy:0
        //Find which attributes need to be updated and which attributes need to be just reminded of updating.

        //I'm not sure why this needs these specific headers.
        Map<String, Object> headers = DefaultMQTTHandler.prepareHeaders(topicRealm(topic), connection);

//        asset.setAttributes(attributes);
        AttributeMap nonExistingAttributes = new AttributeMap();

        attributes.forEach( attribute ->  {
            //Attribute exists, needs to be updated
            if(asset.getAttributes().containsKey(attribute.getName())){
                AttributeEvent attributeEvent = new AttributeEvent(asset.getId(), attribute.getName(), attribute.getValue());
                messageBrokerService.getFluentProducerTemplate()
                        .withHeaders(headers)
                        .withBody(attributeEvent)
                        .to(CLIENT_INBOUND_QUEUE)
                        .asyncSend();
            }
            else{
                nonExistingAttributes.add(attribute);
            }
        });

        asset.addAttributes(nonExistingAttributes.stream().toArray(Attribute[]::new));

        assetStorageService.merge(asset);

    }
}
