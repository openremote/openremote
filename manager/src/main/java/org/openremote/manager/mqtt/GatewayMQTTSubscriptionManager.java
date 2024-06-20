package org.openremote.manager.mqtt;

import io.netty.handler.codec.mqtt.MqttQoS;
import org.apache.activemq.artemis.spi.core.protocol.RemotingConnection;
import org.openremote.container.message.MessageBrokerService;
import org.openremote.model.asset.Asset;
import org.openremote.model.asset.AssetEvent;
import org.openremote.model.asset.AssetFilter;
import org.openremote.model.attribute.AttributeEvent;
import org.openremote.model.event.shared.CancelEventSubscription;
import org.openremote.model.event.shared.EventSubscription;
import org.openremote.model.event.shared.SharedEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import static org.openremote.manager.event.ClientEventService.CLIENT_INBOUND_QUEUE;
import static org.openremote.manager.mqtt.GatewayMQTTHandler.*;
import static org.openremote.manager.mqtt.MQTTBrokerService.getConnectionIDString;
import static org.openremote.manager.mqtt.MQTTHandler.topicRealm;
import static org.openremote.model.Constants.ASSET_ID_REGEXP;


/**
 * Manages MQTT subscriptions for the MQTT Gateway API, it is responsible for adding and removing subscriptions for both the asset and attribute topics
 * Subscriptions are added to the broker and the subscriber info map, the subscriber info map is used to keep track of the subscriptions for each connection
 *
 */
@SuppressWarnings({"unused", "rawtypes", "unchecked"})
public class GatewayMQTTSubscriptionManager {

    private static final Logger LOG = Logger.getLogger(GatewayMQTTSubscriptionManager.class.getName());
    private final ConcurrentMap<String, GatewayEventSubscriberInfo> subscriberInfoMap = new ConcurrentHashMap<>();
    private final MessageBrokerService messageBrokerService;
    private final MQTTBrokerService mqttBrokerService;

    public GatewayMQTTSubscriptionManager(MessageBrokerService messageBrokerService, MQTTBrokerService mqttBrokerService) {
        this.messageBrokerService = messageBrokerService;
        this.mqttBrokerService = mqttBrokerService;
    }

    public ConcurrentMap<String, GatewayEventSubscriberInfo> getSubscriberInfoMap() {
        return subscriberInfoMap;
    }

    /**
     * Adds a subscription for the specified topic, the subscription will be added to the broker and the subscriber info map
     * @param relativeToAsset the asset to build the filter relative to, can be null
     */
    public void addSubscription(RemotingConnection connection, Topic topic, Class subscriptionClass, Asset<?> relativeToAsset) {

        if (subscriptionClass != AssetEvent.class && subscriptionClass != AttributeEvent.class) {
            LOG.warning("Invalid subscription class " + subscriptionClass);
            return;
        }
        String subscriptionId = topic.toString();
        AssetFilter assetFilter = buildAssetFilter(topic, relativeToAsset);

        if (assetFilter == null) {
            LOG.warning("Invalid asset filter for topic " + topic);
            return;
        }

        EventSubscription subscription = new EventSubscription(subscriptionClass, assetFilter, subscriptionId);
        Map<String, Object> headers = prepareHeaders(topicRealm(topic), connection);
        sendSubscriptionToBroker(subscription, headers);
        Consumer<SharedEvent> subscriptionConsumer = buildEventSubscriptionConsumer(topic);
        addSubscriberInfo(connection, topic, subscriptionConsumer);
    }

    /**
     * Removes a subscription for the specified topic, the subscription will be removed from the broker and the subscriber info map
     */
    public void removeSubscription(RemotingConnection connection, Topic topic) {
        String subscriptionId = topic.toString();
        boolean isAssetTopic = subscriberInfoMap.get(getConnectionIDString(connection))
                .topicSubscriptionMap.get(topic.getString()) instanceof AssetEvent;

        Class<SharedEvent> subscriptionClass = (Class) (isAssetTopic ? AssetEvent.class : AttributeEvent.class);
        CancelEventSubscription cancelEventSubscription = new CancelEventSubscription(subscriptionClass, subscriptionId);
        messageBrokerService.getFluentProducerTemplate()
                .withHeaders(prepareHeaders(topicRealm(topic), connection))
                .withBody(cancelEventSubscription)
                .to(CLIENT_INBOUND_QUEUE)
                .asyncSend();

        synchronized (subscriberInfoMap) {
            subscriberInfoMap.computeIfPresent(getConnectionIDString(connection), (connectionID, subscriberInfo) -> {
                if (subscriberInfo.remove(topic.getString()) == 0) {
                    return null;
                }
                return subscriberInfo;
            });
        }
    }

    /**
     * Removes all subscriptions for the specified connection
     */
    public void removeAllSubscriptions(RemotingConnection connection) {
        synchronized (subscriberInfoMap) {
            subscriberInfoMap.remove(getConnectionIDString(connection));
        }
    }

    /**
     * Sends the EventSubscription to the internal broker
     */
    protected void sendSubscriptionToBroker(EventSubscription subscription, Map<String, Object> headers) {
        messageBrokerService.getFluentProducerTemplate()
                .withHeaders(headers)
                .withBody(subscription)
                .to(CLIENT_INBOUND_QUEUE)
                .asyncSend();

    }


    /***
     * Builds an asset filter based on the topic, the filter is used to filter asset events based on the topic filter pattern
     * @param relativeToAsset the asset to build the filter relative to, can be null
     */
    protected static AssetFilter<?> buildAssetFilter(Topic topic, Asset<?> relativeToAsset) {
        List<String> topicTokens = topic.getTokens();
        boolean isAttributesTopic = isAttributesTopic(topic);
        boolean isAssetsTopic = isAssetsTopic(topic) && !isAttributesTopic;

        String realm = topicRealm(topic);
        List<String> assetIds = new ArrayList<>();
        List<String> parentIds = new ArrayList<>();
        List<String> paths = new ArrayList<>();
        List<String> attributeNames = new ArrayList<>();

        if (isAssetsTopic) {
            String assetId = topicTokens.size() > ASSET_ID_TOKEN_INDEX ? topicTokens.get(ASSET_ID_TOKEN_INDEX) : "";
            String path = topicTokens.size() > ASSET_ID_TOKEN_INDEX + 1 ? topicTokens.get(ASSET_ID_TOKEN_INDEX + 1) : "";

            if (assetId.equals("#")) {
            }
            else if (assetId.equals("+")) {
                parentIds.add(null);
            }
            // topic has a valid assetId
            else if (Pattern.matches(ASSET_ID_REGEXP, assetId)) {
                if (path.equals("#")) {
                    paths.add(assetId);
                }
                else if (path.equals("+")) {
                    parentIds.add(assetId);
                }
                else {
                    assetIds.add(assetId);
                }
            } else {
                return null;
            }
        } else if (isAttributesTopic) {
            String assetId = topicTokens.size() > 4 ? topicTokens.get(ASSET_ID_TOKEN_INDEX) : "";
            String attributeName = topicTokens.size() > 6 ? topicTokens.get(ATTRIBUTE_NAME_TOKEN_INDEX) : "";
            boolean attributeNameIsNotWildcardOrEmpty = !attributeName.equals("+") && !attributeName.equals("#") && !attributeName.isEmpty();
            boolean assetIdIsNotWildcardOrEmpty = !assetId.equals("+") && !assetId.equals("#") && !assetId.isEmpty();

            String path = topicTokens.size() > ATTRIBUTE_NAME_TOKEN_INDEX + 1 ? topicTokens.get(ATTRIBUTE_NAME_TOKEN_INDEX + 1) : "";

            // if the topic has an assetId
            if (assetIdIsNotWildcardOrEmpty && Pattern.matches(ASSET_ID_REGEXP, assetId)) {
                if (attributeName.isEmpty()) {
                    assetIds.add(assetId);
                } else if (attributeNameIsNotWildcardOrEmpty) {
                    attributeNames.add(attributeName);

                    if (path.equals("#")) {
                        paths.add(assetId);
                    }

                    else if (path.equals("+")) {
                        parentIds.add(assetId);
                    }

                    else {
                        assetIds.add(assetId);
                    }
                }
                // unsupported topic-filter
                else {
                    return null;
                }
            }
            // if the topic has a wildcard assetId
            else {
                if (assetId.equals("+") && attributeNameIsNotWildcardOrEmpty && path.equals("#")) {
                    attributeNames.add(attributeName);
                }
                else if (assetId.equals("+") && attributeNameIsNotWildcardOrEmpty && path.equals("+")) {
                    parentIds.add(null);
                    attributeNames.add(attributeName);
                }
                else if (assetId.equals("+") && attributeName.equals("#")) {
                    // no filter needed
                }
                else if (assetId.equals("+") && attributeName.equals("+")) {
                    parentIds.add(null);
                }
                // unsupported topic-filter
                else {
                    return null;
                }
            }

        }
        AssetFilter<?> assetFilter = new AssetFilter<>().setRealm(realm);

        // enforce relativeTo asset filter, used for gateway connections (they only receive events for their assets)
        if (relativeToAsset != null) {
            LOG.info("Building asset filter relative to asset: " + relativeToAsset.getId());
            paths.add(relativeToAsset.getId()); // filter by path, only descendants of the asset
        }

        if (!assetIds.isEmpty()) {
            assetFilter.setAssetIds(assetIds.toArray(new String[0]));
        }
        if (!parentIds.isEmpty()) {
            assetFilter.setParentIds(parentIds.toArray(new String[0]));
        }
        if (!paths.isEmpty()) {
            assetFilter.setPath(paths.toArray(new String[0]));
        }
        if (!attributeNames.isEmpty()) {
            assetFilter.setAttributeNames(attributeNames.toArray(new String[0]));
        }
        return assetFilter;
    }

    protected void addSubscriberInfo(RemotingConnection connection, Topic topic, Consumer<SharedEvent> subscriptionConsumer) {
        synchronized (subscriberInfoMap) {
            subscriberInfoMap.compute(getConnectionIDString(connection), (connectionID, subscriberInfo) -> {
                if (subscriberInfo == null) {
                    return new GatewayEventSubscriberInfo(topic.getString(), subscriptionConsumer);
                } else {
                    subscriberInfo.add(topic.getString(), subscriptionConsumer);
                    return subscriberInfo;
                }
            });
        }
    }

    /**
     * Builds a consumer that publishes the event to the specified topic
     */
    protected Consumer<SharedEvent> buildEventSubscriptionConsumer(Topic topic) {
        // Always publish asset/attribute messages with QoS 0
        MqttQoS mqttQoS = MqttQoS.AT_MOST_ONCE;
        Function<SharedEvent, String> topicExpander;
        List<String> topicTokens = topic.getTokens();

        if (isAssetsTopic(topic)) {
            String topicStr = topic.toString();
            String replaceToken = determineReplaceToken(topicStr);
            topicExpander = ev -> replaceToken != null ? topicStr.replace(replaceToken, ((AssetEvent) ev).getId()) : topicStr;
        } else {
            String topicStr = topic.toString();

            if (topicTokens.size() > 4 && topicTokens.get(ASSET_ID_TOKEN_INDEX).equals(TOKEN_SINGLE_LEVEL_WILDCARD)) {
                topicTokens.set(ASSET_ID_TOKEN_INDEX, "$assetId");
            }

            if (topicTokens.size() > 6 && (topicTokens.get(ATTRIBUTE_NAME_TOKEN_INDEX).equals(TOKEN_SINGLE_LEVEL_WILDCARD) || topicTokens.get(ATTRIBUTE_NAME_TOKEN_INDEX).equals(TOKEN_MULTI_LEVEL_WILDCARD))) {
                topicTokens.set(ATTRIBUTE_NAME_TOKEN_INDEX, "$attributeName");
            }

            topicExpander = ev -> {
                String expanded = String.join("/", topicTokens);
                if (expanded.contains("$") && ev instanceof AttributeEvent attributeEvent) {
                    expanded = expanded.replace("$assetId", attributeEvent.getId());
                    expanded = expanded.replace("$attributeName", attributeEvent.getName());
                }

                // handle the last token if it is a wildcard, replace it with the actual assetId/attributeName/id
                String replaceToken = determineReplaceToken(expanded);
                if (replaceToken != null && ev instanceof AttributeEvent attributeEvent) {
                    expanded = expanded.replace(replaceToken, attributeEvent.getId());
                }
                return expanded;
            };
        }

        return ev -> {
            LOG.info("Publishing event to topic: " + topicExpander.apply(ev));
            if (isAssetsTopic(topic)) {
                if (ev instanceof AssetEvent) {
                    mqttBrokerService.publishMessage(topicExpander.apply(ev), ev, mqttQoS);
                }
            } else {
                if (ev instanceof AttributeEvent attributeEvent) {
                    mqttBrokerService.publishMessage(topicExpander.apply(ev), ev, mqttQoS);
                }
            }
        };
    }

    /*
      * Determines the token to replace in the topic string, the token is the last token in the topic string
     */
    protected String determineReplaceToken(String topicStr) {
        if (topicStr.endsWith(TOKEN_MULTI_LEVEL_WILDCARD)) {
            return TOKEN_MULTI_LEVEL_WILDCARD;
        } else if (topicStr.endsWith(TOKEN_SINGLE_LEVEL_WILDCARD)) {
            return TOKEN_SINGLE_LEVEL_WILDCARD;
        } else {
            return null;
        }
    }

    public static class GatewayEventSubscriberInfo {
        public Map<String, Consumer<SharedEvent>> topicSubscriptionMap;

        public GatewayEventSubscriberInfo(String topic, Consumer<SharedEvent> subscriptionConsumer) {
            this.topicSubscriptionMap = new HashMap<>();
            this.topicSubscriptionMap.put(topic, subscriptionConsumer);
        }

        protected void add(String topic, Consumer<SharedEvent> subscriptionConsumer) {
            topicSubscriptionMap.put(topic, subscriptionConsumer);
        }

        protected int remove(String topic) {
            topicSubscriptionMap.remove(topic);
            return topicSubscriptionMap.size();
        }
    }


}
