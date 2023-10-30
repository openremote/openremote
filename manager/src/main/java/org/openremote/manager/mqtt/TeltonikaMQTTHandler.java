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
import org.openremote.manager.datapoint.AssetDatapointService;
import org.openremote.manager.security.ManagerIdentityService;
import org.openremote.manager.security.ManagerKeycloakIdentityProvider;
import org.openremote.model.Container;
import org.openremote.model.asset.Asset;
import org.openremote.model.asset.AssetFilter;
import org.openremote.model.asset.AssetStateDuration;
import org.openremote.model.asset.impl.CarAsset;
import org.openremote.model.attribute.*;
import org.openremote.model.datapoint.AssetDatapoint;
import org.openremote.model.query.AssetQuery;
import org.openremote.model.query.filter.AttributePredicate;
import org.openremote.model.query.filter.NumberPredicate;
import org.openremote.model.syslog.SyslogCategory;
import org.openremote.model.teltonika.*;
import org.openremote.model.value.AttributeDescriptor;
import org.openremote.model.value.MetaItemType;
import org.openremote.model.value.ValueType;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Logger;

import static org.openremote.manager.event.ClientEventService.CLIENT_INBOUND_QUEUE;
import static org.openremote.model.syslog.SyslogCategory.API;
import static org.openremote.model.value.MetaItemType.*;

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

    //TODO: Allow the below fields to be defined as environment variables
    private static final String TELTONIKA_DEVICE_RECEIVE_TOPIC = "data";
    private static final String TELTONIKA_DEVICE_SEND_TOPIC = "commands";
    private static final String TELTONIKA_DEVICE_TOKEN = "teltonika";
    private static final String TELTONIKA_DEVICE_SEND_COMMAND_ATTRIBUTE_NAME = "sendToDevice";
    private static final String TELTONIKA_DEVICE_RECEIVE_COMMAND_ATTRIBUTE_NAME = "response";

    private static final Logger LOG = SyslogCategory.getLogger(API, TeltonikaMQTTHandler.class);

    protected AssetStorageService assetStorageService;
    protected AssetDatapointService AssetDatapointService;
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
        return TELTONIKA_DEVICE_TOKEN.equalsIgnoreCase(topicTokenIndexToString(topic, 2));
    }

    @Override
    public void start(Container container) throws Exception {
        super.start(container);
        getLogger().info("Starting Teltonika MQTT Handler");
        ManagerIdentityService identityService = container.getService(ManagerIdentityService.class);
        assetStorageService = container.getService(AssetStorageService.class);
        AssetDatapointService = container.getService(AssetDatapointService.class);
        timerService = container.getService(TimerService.class);
        // Make this nicer.
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
     * @return AssetFilter of CarAssets that have both {@value TELTONIKA_DEVICE_RECEIVE_COMMAND_ATTRIBUTE_NAME} and
     * {@value TELTONIKA_DEVICE_SEND_COMMAND_ATTRIBUTE_NAME} as attributes.
     */
    private AssetFilter<AttributeEvent> buildAssetFilter(){
        List<Asset<?>> assetsWithAttribute = assetStorageService
                .findAll(new AssetQuery().types(CarAsset.class)
                .attributeNames(TELTONIKA_DEVICE_SEND_COMMAND_ATTRIBUTE_NAME, TELTONIKA_DEVICE_RECEIVE_COMMAND_ATTRIBUTE_NAME));
        ArrayList<String> listOfCarAssetIds = new ArrayList<>();
        assetsWithAttribute.forEach(asset -> listOfCarAssetIds.add(asset.getId()));
        AssetFilter<AttributeEvent> event = new AssetFilter<>();
        event.setAssetIds(listOfCarAssetIds.toArray(new String[0]));
        event.setAttributeNames(TELTONIKA_DEVICE_SEND_COMMAND_ATTRIBUTE_NAME);
        return event;
    }


    //Sending a message to master/teltonikaDevice1/teltonika/864636060373301/commands
    //with payload {"CMD":"getstatus"} seems to work. Maybe the quotes are the issue?
    private void handleAttributeMessage(AttributeEvent event) {
        // If this is not an AttributeEvent that updates a sendToDevice field, ignore
        if (!Objects.equals(event.getAttributeName(), TELTONIKA_DEVICE_SEND_COMMAND_ATTRIBUTE_NAME)) return;
        getLogger().info(event.getEventType());
        //Find the asset in question
        Asset<?> asset = assetStorageService.find(new AssetQuery().ids(event.getAssetId()).types(CarAsset.class));

        // Double check, remove later, sanity checks
        if(asset.hasAttribute(TELTONIKA_DEVICE_SEND_COMMAND_ATTRIBUTE_NAME)){
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
                        //TODO: Okay, umm, we got into an infinite loop for this. Pls fix.
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
     * The listener topics are defined as <code>{realmID}/{userID}/{@value TELTONIKA_DEVICE_TOKEN}/{IMEI}</code>
     * //DONE: Be explicit about sending data to {IMEI}/data, and sending commands to {IMEI}/commands.
     */
    @Override
    public Set<String> getPublishListenerTopics() {
        return Set.of(
                TOKEN_SINGLE_LEVEL_WILDCARD + "/" + TOKEN_SINGLE_LEVEL_WILDCARD + "/" +
                        TELTONIKA_DEVICE_TOKEN + "/" + TOKEN_SINGLE_LEVEL_WILDCARD + "/" + TELTONIKA_DEVICE_RECEIVE_TOPIC,
                TOKEN_SINGLE_LEVEL_WILDCARD + "/" + TOKEN_SINGLE_LEVEL_WILDCARD + "/" +
                        TELTONIKA_DEVICE_TOKEN + "/" + TOKEN_SINGLE_LEVEL_WILDCARD + "/" + TELTONIKA_DEVICE_SEND_TOPIC
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
            AttributeMap attributes;
            try{
                attributes = getAttributesFromPayload(payloadContent);
            }catch (JsonProcessingException e) {
                getLogger().severe("Failed to getAttributesFromPayload");
                getLogger().severe(e.toString());
                throw e;
            }
            Attribute<?> payloadAttribute =  new Attribute("payload", ValueType.JSON, payloadContent);
            payloadAttribute.addMeta(new MetaItem<>(MetaItemType.STORE_DATA_POINTS, true));
            attributes.add(payloadAttribute);




            if (asset == null) {
                try{
                    CreateNewAsset(deviceUuid, deviceImei, realm, attributes);
                } catch (Exception e){
                    getLogger().severe("Failed to CreateNewAsset(deviceUuid, deviceImei, realm, attributes);");
                    getLogger().severe(e.toString());
                    throw e;
                }
            }
            else {
            //Check state of Teltonika AVL ID 250 for FMC003, "Trip".
//            Optional<Attribute<?>> sessionAttr = assetChangedTripState(new AttributeRef(asset.getId(), "250"));
                // We want the state where the attribute 250 (Trip) is set to true.
                AttributePredicate pred = new AttributePredicate("250", new NumberPredicate((double) 1, AssetQuery.Operator.EQUALS));

//        Predicate<AssetDatapoint> pred = dp -> Objects.equals(dp.getValue(), Double.parseDouble("1"));
                try{
                    Attribute<?> prevValue = asset.getAttributes().get("250").get();
                    Attribute<?> newValue = attributes.get("250").get();
                    AttributeRef ref = new AttributeRef(asset.getId(), "250");
                    Optional<Attribute<?>> sessionAttr = assetChangedTripState(prevValue, newValue, pred, ref);

                    if (sessionAttr.isPresent()) {
                        Attribute<?> session = sessionAttr.get();
                        session.addOrReplaceMeta(
                                new MetaItem<>(STORE_DATA_POINTS, true),
                                new MetaItem<>(RULE_STATE, true),
                                new MetaItem<>(READ_ONLY, true)
                        );
                        attributes.add(session);

                    }
                }catch (Exception e){
                    getLogger().severe("Could not parse Asset State Duration data");
                    getLogger().severe(e.toString());
                }
                try{
                    UpdateAsset(asset, attributes, topic, connection);
                }catch (Exception e){
                    getLogger().severe("Failed to UpdateAsset(asset, attributes, topic, connection)");
                    getLogger().severe(e.toString());
                    throw e;
                }
            };

        } catch (Exception e){
            getLogger().warning("Could not parse Teltonika device Payload.");
            getLogger().warning(payloadContent);
            getLogger().warning(e.toString());
//            getLogger().warning(e.fi);
        }
        // Check if asset was found
    }

    /**
     * Creates a new asset with the correct "hashed" Asset ID, its IMEI,
     * in the realm the MQTT message of the device submitted,
     * and the parsed list of attributes.
     * //TODO: Add {@value TELTONIKA_DEVICE_SEND_COMMAND_ATTRIBUTE_NAME }, {@value TELTONIKA_DEVICE_RECEIVE_COMMAND_ATTRIBUTE_NAME} Attributes when creating Asset
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
        PayloadJsonObject payload;
        try {
            payload = mapper.readValue(payloadContent, PayloadJsonObject.class);


            AttributeMap attributeMap;
            try{
                attributeMap = payload.state.GetAttributes(params, new AttributeMap(), getLogger());
            }catch (Exception e){
                getLogger().severe("Failed to payload.state.GetAttributes");
                getLogger().severe(e.toString());
                throw e;
            }
            return attributeMap;
        } catch (Exception e) {
            //If the payload wasn't parsed, then it means that it is either a response to a command,
            //or it's genuinely a wrong payload.

            mapper = new ObjectMapper();
            TeltonikaResponsePayload response = mapper.readValue(payloadContent, TeltonikaResponsePayload.class);
            getLogger().info(response.rsp);
            AttributeMap map = new AttributeMap();
            map.addAll(new Attribute<>
                    (
                            new AttributeDescriptor<>(TELTONIKA_DEVICE_RECEIVE_COMMAND_ATTRIBUTE_NAME, ValueType.TEXT),
                            response.rsp
                    )
            );
            return map;
        }

    }


    /**
     * Returns an {@code Optional<Attribute<AssetStateDuration>>}, that {@code ifPresent()}, represents
     * the Duration for which the predicate returned true.
     *
     * @param previousValue The old attribute state (Or the latest datapoint that exists)
     * @param newValue The new attribute value
     * @param pred A Predicate that describes the state change
     * @param ref An AttributeRef that describes which asset and attribute this pertains to.
     * @return An Optional Attribute of type AssetStateDuration that represents the Duration for which the predicate returned true.
     */
    //TODO: Change this to only use an AttributeRef and a Predicate.
    private Optional<Attribute<?>> assetChangedTripState(Attribute<?> previousValue, Attribute<?> newValue, AttributePredicate pred, AttributeRef ref) {
        //We will first check if the predicate fails for the new value, and then check if the predicate is true for the previous value.
        //In that way, we know that the state change happened between the new and previous values.

        //TODO: Understand what happens with negate(), does it change states to the object itself?
        boolean newValueTest =      pred.value.asPredicate(timerService::getCurrentTimeMillis).test(newValue        .getValue().get());
        boolean previousValueTest = pred.value.asPredicate(timerService::getCurrentTimeMillis).test(previousValue   .getValue().get());
        //If the predicate fails, then no changes need to happen.

        // newValue is not 1, previousValue == 1
        if(!(!newValueTest && previousValueTest)) {
            return Optional.empty();
        }

        // Grab all datapoints (To be replaced by AssetDatapointValueQuery)
        List<AssetDatapoint> valueDatapoints = AssetDatapointService.getDatapoints(ref);
        ArrayList<AssetDatapoint> list = new ArrayList<>(valueDatapoints);

        // If there are no historical data found, add some first
        if(list.isEmpty()) return Optional.empty();

        //What we do now is, we will try to figure out the latest datapoint where the predicate fails,
        //before the newValue.
        //This means that, the state change took place between the datapoint we just found and its next one.

        //Find the first datapoint that passes the negated predicate

        AssetDatapoint StateChangeAssetDatapoint = null;

        try {
            for (int i = 0; i < list.size()-1; i++) {
                // Not using Object.equals, but Datapoint.equals

                AssetDatapoint currentDp = list.get(i);
                AssetDatapoint theVeryPreviousDp = list.get(i+1);

    //            So, if the currentDp passes the predicate,
                boolean currentDpTest = pred.value.asPredicate(timerService::getCurrentTimeMillis).test(currentDp.getValue());
    //            and if the very previous one (NEXT one in the array and PREVIOUS in the time dimension).
    //            FAILS the predicate,
                boolean previousDpTest = pred.value.asPredicate(timerService::getCurrentTimeMillis).test(theVeryPreviousDp.getValue());
    //            A state change happened where the state we are looking for was turned on.
    //            We want the currentDp.


                if(currentDpTest && !previousDpTest){
                    StateChangeAssetDatapoint = currentDp;
                    break;
                }
            }

            if (StateChangeAssetDatapoint != null){
                if (!pred.value.asPredicate(timerService::getCurrentTimeMillis).test(StateChangeAssetDatapoint.getValue())){
                    throw new Exception("Found state change datapoint failed predicate");
                }
            }else{
                throw new Exception("Couldn't find asset state change value");
            }

        }catch (Exception e){
            getLogger().warning(e.getMessage());
            return Optional.empty();
        }

        //BUG: The Timestamp of an asset datapoint is NOT attribute.getTimestamp()!
        //This makes it much harder to relate datapoints to relate a duration to the time-distance between two datapoints.
        //To make this easier, we are going to use some time-padding of 1 second from the gap. It's going to be sufficient to cover some edge-cases.

        //The AssetStateDuration will have a startTime of the found state-change variable, and the endDate will be the previousValue's timestamp, that being the end of the state duration
        Attribute<?> tripAttr = new Attribute<>("LastTripStartedAndEndedAt", ValueType.ASSET_STATE_DURATION, new AssetStateDuration(
                new Timestamp(StateChangeAssetDatapoint.getTimestamp()),
                new Timestamp(previousValue.getTimestamp().get())
        ));

        tripAttr.setTimestamp(StateChangeAssetDatapoint.getTimestamp());

        return Optional.of(tripAttr);
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



////        AssetDatapoint datapoint = list.stream().filter(
////                pred.value.asPredicate(timerService::getCurrentTimeMillis)
////        ).toList()
////                .get(0);
//
//        //We now need to get the very next datapoint.
//        AssetDatapoint StateChangeAssetDatapoint = null;
//
//        try {
//            for (int i = 0; i < list.size(); i++) {
//                // Not using Object.equals, but Datapoint.equals
//                if(list.get(i).equals(datapoint)){
//                    //If we found it, just grab the next one
//                    StateChangeAssetDatapoint = list.get(i++);
//                    break;
//                }
//            }
//
//            //Make sure that the Datapoint truly does pass the predicate.
//
//            //If it doesnt, throw an Exception.
//
//
//        }catch (Exception e){
//            getLogger().warning(e.getMessage());
//            return Optional.empty();
//        }
