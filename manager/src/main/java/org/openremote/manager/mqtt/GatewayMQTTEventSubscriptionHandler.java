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
 * Handles the subscription of GatewayMQTTHandler clients to asset and attribute events, the handler adds the subscription to the broker and
 * the subscriber info map, the subscriber info map is used to keep track of the subscriptions for each connection
 */
@SuppressWarnings({"unused", "rawtypes", "unchecked"})
public class GatewayMQTTEventSubscriptionHandler {

    private static final Logger LOG = Logger.getLogger(GatewayMQTTEventSubscriptionHandler.class.getName());
    private final ConcurrentMap<String, GatewayEventSubscriberInfo> eventSubscriberInfoMap = new ConcurrentHashMap<>();
    private final MessageBrokerService messageBrokerService;
    private final MQTTBrokerService mqttBrokerService;

    public GatewayMQTTEventSubscriptionHandler(MessageBrokerService messageBrokerService, MQTTBrokerService mqttBrokerService) {
        this.messageBrokerService = messageBrokerService;
        this.mqttBrokerService = mqttBrokerService;
    }


    public ConcurrentMap<String, GatewayEventSubscriberInfo> getEventSubscriberInfoMap() {
        return eventSubscriberInfoMap;
    }

    /**
     * Adds a subscription for the specified topic, the subscription will be added to the broker and the subscriber info map
     *
     * @param connection        the connection to add the subscription for
     * @param topic             the topic to subscribe to  (e.g. realm/clientId/events/assets/+/attributes/+/#)
     * @param subscriptionClass the class of the subscription (AssetEvent.class or AttributeEvent.class)
     * @param relativeToAsset   the asset to build the filter relative to, can be null
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
     *
     * @param connection the connection to remove the subscription for
     * @param topic      the topic to remove the subscription for
     */
    public void removeSubscription(RemotingConnection connection, Topic topic) {
        cancelSubscriptionFromBroker(connection, topic);
        removeSubscriberInfo(connection, topic);
    }

    /**
     * Removes all subscriptions for the specified connection
     *
     * @param connection the connection to remove all subscriptions for
     */
    public void removeAllSubscriptions(RemotingConnection connection) {
        synchronized (eventSubscriberInfoMap) {
            eventSubscriberInfoMap.remove(getConnectionIDString(connection));
        }
    }

    protected void sendSubscriptionToBroker(EventSubscription subscription, Map<String, Object> headers) {
        messageBrokerService.getFluentProducerTemplate()
                .withHeaders(headers)
                .withBody(subscription)
                .to(CLIENT_INBOUND_QUEUE)
                .asyncSend();

    }

    protected void cancelSubscriptionFromBroker(RemotingConnection connection, Topic topic) {
        String subscriptionId = topic.toString();
        boolean isAssetTopic = eventSubscriberInfoMap.get(getConnectionIDString(connection))
                .topicSubscriptionMap.get(topic.getString()) instanceof AssetEvent;

        Class<SharedEvent> subscriptionClass = (Class) (isAssetTopic ? AssetEvent.class : AttributeEvent.class);
        CancelEventSubscription cancelEventSubscription = new CancelEventSubscription(subscriptionClass, subscriptionId);
        messageBrokerService.getFluentProducerTemplate()
                .withHeaders(prepareHeaders(topicRealm(topic), connection))
                .withBody(cancelEventSubscription)
                .to(CLIENT_INBOUND_QUEUE)
                .asyncSend();
    }

    /***
     * Builds an asset filter based on the topic, the filter is used to filter asset events based on the topic filter pattern
     * @param topic the topic to build the filter for
     * @param relativeToAsset the asset to build the filter relative to, can be null
     * @return the asset filter or null if the topic is invalid
     */
    protected static AssetFilter<?> buildAssetFilter(Topic topic, Asset<?> relativeToAsset) {
        var topicTokens = topic.getTokens();
        boolean isAttributesTopic = isAttributesTopic(topic);
        boolean isAssetsTopic = isAssetsTopic(topic) && !isAttributesTopic;

        String realm = topicRealm(topic);
        List<String> assetIds = new ArrayList<>();
        List<String> parentIds = new ArrayList<>();
        List<String> paths = new ArrayList<>();
        List<String> attributeNames = new ArrayList<>();

        // enforce relativeTo asset filter, used for gateway connections (they only receive events for their assets)
        if (relativeToAsset != null) {
            paths.add(relativeToAsset.getId()); // filter by path, only descendants of the asset
        }

        if (isAssetsTopic) {
            var assetId = topicTokens.size() > ASSET_ID_TOKEN_INDEX ? topicTokens.get(ASSET_ID_TOKEN_INDEX) : "";
            var path = topicTokens.size() > ASSET_ID_TOKEN_INDEX + 1 ? topicTokens.get(ASSET_ID_TOKEN_INDEX + 1) : "";

            // realm/clientId/events/assets/#
            // all asset events of the realm
            if (assetId.equals("#")) {
            }
            // realm/clientId/events/assets/+
            // all asset events of direct children of the realm
            else if (assetId.equals("+")) {
                parentIds.add(null);
            }
            // topic has a valid assetId
            else if (Pattern.matches(ASSET_ID_REGEXP, assetId)) {
                // realm/clientId/events/assets/{assetId}/#
                // all asset events for descendants of the asset
                if (path.equals("#")) {
                    paths.add(assetId);
                }
                // realm/clientId/events/assets/{assetId}/+
                // all asset events for direct children of the asset
                else if (path.equals("+")) {
                    parentIds.add(assetId);
                }
                // realm/clientId/events/assets/{assetId}
                // all asset events of the specified asset
                else {
                    assetIds.add(assetId);
                }
            } else {
                return null;
            }
        } else if (isAttributesTopic) {
            var assetId = topicTokens.size() > 4 ? topicTokens.get(ASSET_ID_TOKEN_INDEX) : "";
            var attributeName = topicTokens.size() > 6 ? topicTokens.get(ATTRIBUTE_NAME_TOKEN_INDEX) : "";
            boolean attributeNameIsNotWildcardOrEmpty = !attributeName.equals("+") && !attributeName.equals("#") && !attributeName.isEmpty();
            boolean assetIdIsNotWildcardOrEmpty = !assetId.equals("+") && !assetId.equals("#") && !assetId.isEmpty();

            // realm/clientId/events/assets/{assetId}/attributes/{attributeName}/<<path>>
            var path = topicTokens.size() > ATTRIBUTE_NAME_TOKEN_INDEX + 1 ? topicTokens.get(ATTRIBUTE_NAME_TOKEN_INDEX + 1) : "";

            // if the topic has an assetId
            if (assetIdIsNotWildcardOrEmpty && Pattern.matches(ASSET_ID_REGEXP, assetId)) {
                // realm/clientId/events/assets/{assetId}/attributes
                // all attribute events for the specified asset
                if (attributeName.isEmpty()) {
                    assetIds.add(assetId);
                } else if (attributeNameIsNotWildcardOrEmpty) {
                    attributeNames.add(attributeName);

                    // realm/clientId/events/assets/{assetId}/attributes/{attributeName}/#
                    // all attribute events of descendants of the asset with the specified attribute name
                    if (path.equals("#")) {
                        paths.add(assetId);
                    }
                    // realm/clientId/events/assets/{assetId}/attributes/{attributeName}/+
                    // all attribute events of direct children of the asset with the specified attribute name
                    else if (path.equals("+")) {
                        parentIds.add(assetId);
                    }
                    // realm/clientId/events/assets/{assetId}/attributes/{attributeName}
                    // all attribute events of the asset with the specified attribute name
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
                // realm/clientId/events/assets/+/attributes/{attributeName}/#
                // All attribute events of the realm with the specified attributeName
                if (assetId.equals("+") && attributeNameIsNotWildcardOrEmpty && path.equals("#")) {
                    attributeNames.add(attributeName);
                }
                // realm/clientId/events/assets/+/attributes/{attributeName}/+
                // All attribute events for direct children of the realm with the specified attributeName
                else if (assetId.equals("+") && attributeNameIsNotWildcardOrEmpty && path.equals("+")) {
                    parentIds.add(null);
                    attributeNames.add(attributeName);
                }
                // realm/clientId/events/assets/+/attributes/#
                // All attribute events of direct children of the realm
                else if (assetId.equals("+") && attributeName.equals("#")) {
                    // no filter needed
                }

                // realm/clientId/events/assets/+/attributes/+
                // All attribute events of direct children of the realm
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
        synchronized (eventSubscriberInfoMap) {
            eventSubscriberInfoMap.compute(getConnectionIDString(connection), (connectionID, subscriberInfo) -> {
                if (subscriberInfo == null) {
                    return new GatewayEventSubscriberInfo(topic.getString(), subscriptionConsumer);
                } else {
                    subscriberInfo.add(topic.getString(), subscriptionConsumer);
                    return subscriberInfo;
                }
            });
        }
    }


    protected void removeSubscriberInfo(RemotingConnection connection, Topic topic) {
        synchronized (eventSubscriberInfoMap) {
            eventSubscriberInfoMap.computeIfPresent(getConnectionIDString(connection), (connectionID, subscriberInfo) -> {
                if (subscriberInfo.remove(topic.getString()) == 0) {
                    return null;
                }
                return subscriberInfo;
            });
        }
    }

    /**
     * Builds a consumer that publishes the event to the specified topic
     *
     * @param topic the topic to publish the event to
     * @return the consumer that publishes the event to the topic
     */
    protected Consumer<SharedEvent> buildEventSubscriptionConsumer(Topic topic) {

        // Always publish asset/attribute messages with QoS 0
        MqttQoS mqttQoS = MqttQoS.AT_MOST_ONCE;

        // Build topic expander (replace wildcards) so it isn't computed for each event
        Function<SharedEvent, String> topicExpander;

        // get the tokens of the topic
        var topicTokens = topic.getTokens();

        if (isAssetsTopic(topic)) {
            String topicStr = topic.toString();
            // replace the assetId token with the actual assetId (if its multi-level or single-level wildcard)
            String replaceToken = topicStr.endsWith(TOKEN_MULTI_LEVEL_WILDCARD) ? TOKEN_MULTI_LEVEL_WILDCARD : topicStr.endsWith(TOKEN_SINGLE_LEVEL_WILDCARD) ? TOKEN_SINGLE_LEVEL_WILDCARD : null;
            topicExpander = ev -> replaceToken != null ? topicStr.replace(replaceToken, ((AssetEvent) ev).getId()) : topicStr;
        } else {
            String topicStr = topic.toString();


            if (topicTokens.size() > 4) {
                // replace the assetId token with the actual assetId (INDEX: 4)
                if (topicTokens.get(ASSET_ID_TOKEN_INDEX).equals(TOKEN_SINGLE_LEVEL_WILDCARD)) {
                    topicTokens.set(ASSET_ID_TOKEN_INDEX, "$assetId");
                }
            }


            if (topicTokens.size() > 6) {
                // replace the attributeName token with the actual attributeName (INDEX: 6)
                if (topicTokens.get(ATTRIBUTE_NAME_TOKEN_INDEX).equals(TOKEN_SINGLE_LEVEL_WILDCARD) || topicTokens.get(ATTRIBUTE_NAME_TOKEN_INDEX).equals(TOKEN_MULTI_LEVEL_WILDCARD)) {
                    topicTokens.set(ATTRIBUTE_NAME_TOKEN_INDEX, "$attributeName");
                }
            }

            topicExpander = ev -> {
                String expanded = String.join("/", topicTokens);
                if (expanded.contains("$")) {
                    if (ev instanceof AttributeEvent attributeEvent) {
                        expanded = expanded.replace("$assetId", attributeEvent.getId());
                        expanded = expanded.replace("$attributeName", attributeEvent.getName());
                    }
                }
                // handle the last token if it is a wildcard, replace it with the actual assetId/attributeName/id
                String replaceToken = topicStr.endsWith(TOKEN_MULTI_LEVEL_WILDCARD) ? TOKEN_MULTI_LEVEL_WILDCARD : topicStr.endsWith(TOKEN_SINGLE_LEVEL_WILDCARD) ? TOKEN_SINGLE_LEVEL_WILDCARD : null;
                if (replaceToken != null && ev instanceof AttributeEvent) {
                    expanded = expanded.replace(replaceToken, ((AttributeEvent) ev).getId());
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
