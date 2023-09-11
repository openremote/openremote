package org.openremote.manager.mqtt;

import io.netty.buffer.ByteBuf;
import org.apache.activemq.artemis.spi.core.protocol.RemotingConnection;
import org.keycloak.KeycloakSecurityContext;
import org.openremote.container.util.UniqueIdentifierGenerator;
import org.openremote.manager.asset.AssetStorageService;
import org.openremote.manager.security.ManagerIdentityService;
import org.openremote.manager.security.ManagerKeycloakIdentityProvider;
import org.openremote.model.Container;
import org.openremote.model.asset.Asset;
import org.openremote.model.asset.impl.RoomAsset;
import org.openremote.model.asset.impl.ThingAsset;
import org.openremote.model.attribute.Attribute;
import org.openremote.model.syslog.SyslogCategory;
import org.openremote.model.util.ValueUtil;
import org.openremote.model.value.ValueType;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import static org.openremote.model.syslog.SyslogCategory.API;

public class TeltonikaMQTTHandler extends MQTTHandler {

    private static final String TOKEN_TELTONIKA_DEVICE = "Teltonika";

    private static final Logger LOG = SyslogCategory.getLogger(API, TeltonikaMQTTHandler.class);

    protected AssetStorageService assetStorageService;
    /**
     * Indicates if this handler will handle the specified topic; independent of whether it is a publish or subscribe.
     * Should generally check the third token (index 2) onwards unless {@link #handlesTopic} has been overridden.
     *
     * @param topic
     */
    @Override
    protected boolean topicMatches(Topic topic) {
        if ("teltonika".equalsIgnoreCase(topicTokenIndexToString(topic, 2))){
            getLogger().info("Matches Teltonika Handler");
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

        if (!identityService.isKeycloakEnabled()) {
            LOG.warning("MQTT connections are not supported when not using Keycloak identity provider");
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
        getLogger().fine("canSubscribe");
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
        getLogger().fine("canPublish");
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
     * The listener topics are defined as <code>{realmID}/{userID}/{@value TOKEN_TELTONIKA_DEVICE}/{IMEI}#</code>
     */
    @Override
    public Set<String> getPublishListenerTopics() {
        getLogger().fine("getPublishListenerTopics");
        return Set.of(
                TOKEN_SINGLE_LEVEL_WILDCARD + "/" + TOKEN_SINGLE_LEVEL_WILDCARD + "/" + TOKEN_TELTONIKA_DEVICE + "/" + TOKEN_MULTI_LEVEL_WILDCARD
        );
    }


    /**
     * Called to handle publish if {@link #canPublish} returned true.
     *
     * @param connection
     * @param topic
     * @param body
     */
     //TODO: Make this secure using certificates/authentication
    @Override
    public void onPublish(RemotingConnection connection, Topic topic, ByteBuf body) {
        String payloadContent = body.toString(StandardCharsets.UTF_8);
        //  {realmId}/{clientId}/teltonika/{imei}
        String deviceImei = topic.tokens.get(3);
        String realm = topic.tokens.get(0);

        Object value = ValueUtil.parse(payloadContent).orElse(null);
        String deviceUuid = getAssetId(deviceImei);
        LOG.fine(deviceUuid);

        Asset<?> asset = assetStorageService.find(deviceUuid);
        // Check if asset was found

        // Preexisting asset IMEI: 357073299950291
        if (asset == null) CreateNewAsset(payloadContent, deviceUuid, deviceImei, realm);
        else UpdateAsset(payloadContent, asset);



        return;
    }

    private void CreateNewAsset(String payloadContent, String newDeviceId, String newDeviceImei, String realm) {
        ThingAsset testAsset = new ThingAsset("Teltonika Asset "+newDeviceImei)
                .setRealm(realm)
                .setId(newDeviceId);
        List<Attribute<?>> attributes = getAttributesFromPayload(payloadContent);
        testAsset.addOrReplaceAttributes(
                new Attribute<>("IMEI", ValueType.TEXT, newDeviceImei),
                new Attribute<>("Payload", ValueType.TEXT, payloadContent)
        );
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
     * @return List of {@link Attribute}s to be assigned to the asset
     */
    private List<Attribute<?>> getAttributesFromPayload(String payloadContent) {



        return null;
    }

    private void UpdateAsset(String payloadContent, Asset<?> asset) {

        asset.addOrReplaceAttributes(
                new Attribute<>("Payload", ValueType.TEXT, payloadContent)
        );

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
