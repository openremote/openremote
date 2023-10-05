package org.openremote.manager.mqtt;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.buffer.ByteBuf;
import org.apache.activemq.artemis.spi.core.protocol.RemotingConnection;
import org.keycloak.KeycloakSecurityContext;
import org.openremote.container.timer.TimerService;
import org.openremote.container.util.UniqueIdentifierGenerator;
import org.openremote.manager.asset.AssetStorageService;
import org.openremote.manager.security.ManagerIdentityService;
import org.openremote.manager.security.ManagerKeycloakIdentityProvider;
import org.openremote.model.Container;
import org.openremote.model.asset.Asset;
import org.openremote.model.asset.impl.CarAsset;
import org.openremote.model.attribute.Attribute;
import org.openremote.model.syslog.SyslogCategory;
import org.openremote.model.teltonika.TeltonikaParameter;
import org.openremote.model.teltonika.TeltonikaPayload;
import org.openremote.model.util.ValueUtil;
import org.openremote.model.value.ValueType;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.logging.Logger;

import static org.openremote.model.syslog.SyslogCategory.API;

public class TeltonikaMQTTHandler extends MQTTHandler {

    private static final String TOKEN_TELTONIKA_DEVICE = "teltonika";

    private static final Logger LOG = SyslogCategory.getLogger(API, TeltonikaMQTTHandler.class);

    protected AssetStorageService assetStorageService;
    protected TimerService timerService;
    /**
     * Indicates if this handler will handle the specified topic; independent of whether it is a publish or subscribe.
     * Should generally check the third token (index 2) onwards unless {@link #handlesTopic} has been overridden.
     *
     * @param topic
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

        if (!identityService.isKeycloakEnabled()) {
            getLogger().warning("MQTT connections are not supported when not using Keycloak identity provider");
            isKeycloak = false;
        } else {
            isKeycloak = true;
            identityProvider = (ManagerKeycloakIdentityProvider) identityService.getIdentityProvider();
        }
    }

    @Override
    protected Logger getLogger() {
        return LOG;
    }

    /**
     * Called to authorise a subscription if {@link #handlesTopic} returned true; should return true if the subscription
     * is allowed otherwise return false.
     *
     * @param connection
     * @param securityContext
     * @param topic
     */
    @Override
    public boolean canSubscribe(RemotingConnection connection, KeycloakSecurityContext securityContext, Topic topic) {
        getLogger().info("Topic "+topic.toString()+" is not subscribe-able");
        return false;
    }

    /**
     * Overrides MQTTHandler.checkCanPublish for this specific Handler,
     * until secure Authentication and Auto-provisioning
     * of Teltonika Devices is created.
     *
     * To be removed after implementation is complete.
     */
    @Override
    public boolean checkCanPublish(RemotingConnection connection, KeycloakSecurityContext securityContext, Topic topic) {
        return true;
    }

    /**
     * Called to authorise a publish if {@link #handlesTopic} returned true; should return true if the publish is
     * allowed otherwise return false.
     *
     * @param connection
     * @param securityContext
     * @param topic
     */
    @Override
    public boolean canPublish(RemotingConnection connection, KeycloakSecurityContext securityContext, Topic topic) {
        getLogger().info("Teltonika device will publish to Topic "+topic.toString()+" to transmit payload");
        return true;
    }

    /**
     * Called to handle subscribe if {@link #canSubscribe} returned true.
     *
     * @param connection
     * @param topic
     */
    @Override
    public void onSubscribe(RemotingConnection connection, Topic topic) {
    }

    /**
     * Called to handle unsubscribe if {@link #handlesTopic} returned true.
     *
     * @param connection
     * @param topic
     */
    @Override
    public void onUnsubscribe(RemotingConnection connection, Topic topic) {
    }

    /**
     * Get the set of topics this handler wants to subscribe to for incoming publish messages; messages that match
     * these topics will be passed to {@link #onPublish}.
     *
     * The listener topics are defined as <code>{realmID}/{userID}/{@value TOKEN_TELTONIKA_DEVICE}/{IMEI}</code>
     */
    @Override
    public Set<String> getPublishListenerTopics() {
        getLogger().fine("getPublishListenerTopics");
        return Set.of(
                TOKEN_SINGLE_LEVEL_WILDCARD + "/" + TOKEN_SINGLE_LEVEL_WILDCARD + "/" + TOKEN_TELTONIKA_DEVICE + "/" + TOKEN_SINGLE_LEVEL_WILDCARD
        );
    }


    /**
     * Called to handle publish if {@link #canPublish} returned true.
     *
     * @param connection
     * @param topic
     * @param body
     */
    @Override
    public void onPublish(RemotingConnection connection, Topic topic, ByteBuf body) {


        String payloadContent = body.toString(StandardCharsets.UTF_8);

        Attribute<?>[] attributes = getAttributesFromPayload(payloadContent);

        String deviceImei = topic.tokens.get(3);
        String realm = topic.tokens.get(0);

        Object value = ValueUtil.parse(payloadContent).orElse(null);
        String deviceUuid = getAssetId(deviceImei);
        getLogger().info("IMEI Linked to Asset ID:"+ deviceUuid);

        Asset<?> asset = assetStorageService.find(deviceUuid);
        // Check if asset was found
        // Preexisting asset IMEI: 357073299950291
        if (asset == null) CreateNewAsset(payloadContent, deviceUuid, deviceImei, realm, attributes);
        else UpdateAsset(payloadContent, asset, attributes);



        return;
    }

    private void CreateNewAsset(String payloadContent, String newDeviceId, String newDeviceImei, String realm, Attribute<?>[] attributes) {
        getLogger().info("Creating CarAsset with IMEI "+newDeviceImei+" and payload:");
        getLogger().info(payloadContent);

        CarAsset testAsset = new CarAsset("Teltonika Asset "+newDeviceImei)
                .setRealm(realm)
                .setId(newDeviceId);

        List<Attribute<?>> attributesList = new ArrayList<>(Arrays.stream(attributes).toList());

        attributesList.add(new Attribute<>("IMEI", ValueType.TEXT, newDeviceImei));
        attributesList.add(new Attribute<>("lastContact", ValueType.DATE_AND_TIME, new Date(timerService.getCurrentTimeMillis())));

        testAsset.addOrReplaceAttributes(attributesList.toArray(new Attribute[0]));
//                .setAttributes(Attribute<>)
//        testAsset = assetResource.create(null, testAsset)357073299950291
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
     * @return Array of {@link Attribute}s to be assigned to the {@link Asset}.
     */
    private Attribute<?>[] getAttributesFromPayload(String payloadContent) {
        ObjectMapper mapper = new ObjectMapper();
        Map<Integer, TeltonikaParameter> params = new HashMap<>();
        try {
            // Parse file with Parameter details
            String text = Files.readString(Paths.get("model/src/main/java/org/openremote/model/teltonika/FMC003.json"));
            TeltonikaParameter[] paramArray = mapper.readValue(text, TeltonikaParameter[].class);

            // Add each element to the HashMap, with the key being the unique parameter ID and the parameter
            // being the value
            for (TeltonikaParameter param : paramArray) {
                params.put(param.getPropertyId(), param);
            }
        } catch (Exception e) {
            getLogger().info(e.toString());
        }

//        getLogger().info(String.valueOf(params.length));
        //Parameters parsed, time to understand the payload
        TeltonikaPayload payload;
        try {
            payload = mapper.readValue(payloadContent, TeltonikaPayload.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        String x = payload.toString();

        Attribute<?>[] attrs = payload.state.GetAttributes(params);

        return attrs;

//        throw new NotImplementedException();
    }

    private void UpdateAsset(String payloadContent, Asset<?> asset, Attribute<?>[] attributes) {

        String imei = asset.getAttribute("IMEI").toString();

        getLogger().info("Creating CarAsset with IMEI "+imei+" and payload:");
        getLogger().info(payloadContent);

        asset.addOrReplaceAttributes(attributes);

        Asset<?> mergedAsset = assetStorageService.merge(asset);

        if(mergedAsset != null){
            getLogger().info("Updated Asset through Teltonika: " + asset);
//            getLogger().info()
        }else{
            getLogger().info("Failed to update Asset: " + asset);
        }

    }

    private String getAssetId(String deviceImei){
        return UniqueIdentifierGenerator.generateId(deviceImei);
    }



}
