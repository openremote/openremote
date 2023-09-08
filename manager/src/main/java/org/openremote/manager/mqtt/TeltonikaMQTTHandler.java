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
import org.openremote.model.syslog.SyslogCategory;
import org.openremote.model.util.ValueUtil;

import java.nio.charset.StandardCharsets;
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
//            getLogger().info("Matches Teltonika Handler");
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
//            container.getService(MessageBrokerService.class).getContext().addRoutes(new UserAssetProvisioningMQTTHandler.ProvisioningPersistenceRouteBuilder(this));
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
    @Override
    public void onPublish(RemotingConnection connection, Topic topic, ByteBuf body) {
        String payloadContent = body.toString(StandardCharsets.UTF_8);
        //  {realmId}/{clientId}/teltonika/{imei}
        String deviceImei = topic.tokens.get(3);

        Object value = ValueUtil.parse(payloadContent).orElse(null);
        String deviceUuid = getAssetId(deviceImei);
        LOG.fine(deviceUuid);

        Asset<?> asset = assetStorageService.find(deviceUuid);
        // Check if asset was found
        if(asset == null){
            UpdateAsset(payloadContent);
        }else{
            CreateNewAsset(payloadContent);
        }



        return;
    }

    private void CreateNewAsset(String payloadContent) {

    }

    private void UpdateAsset(String payloadContent) {

    }

    private String getAssetId(String deviceImei){
        return UniqueIdentifierGenerator.generateId(deviceImei);
    }


}
