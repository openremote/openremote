package org.openremote.manager.mqtt;

import io.netty.handler.codec.mqtt.MqttQoS;
import org.apache.activemq.artemis.spi.core.protocol.RemotingConnection;
import org.openremote.container.message.MessageBrokerService;
import org.openremote.manager.event.ClientEventService;
import org.openremote.model.asset.Asset;
import org.openremote.model.asset.AssetEvent;
import org.openremote.model.asset.AssetFilter;
import org.openremote.model.attribute.AttributeEvent;
import org.openremote.model.event.Event;
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
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import static org.openremote.manager.mqtt.GatewayMQTTHandler.*;
import static org.openremote.manager.mqtt.MQTTBrokerService.getConnectionIDString;
import static org.openremote.manager.mqtt.MQTTHandler.TOKEN_MULTI_LEVEL_WILDCARD;
import static org.openremote.manager.mqtt.MQTTHandler.TOKEN_SINGLE_LEVEL_WILDCARD;
import static org.openremote.manager.mqtt.MQTTHandler.topicRealm;
import static org.openremote.manager.mqtt.MQTTHandler.topicTokenIndexToString;
import static org.openremote.model.Constants.ASSET_ID_REGEXP;


/**
 * Manages MQTT subscriptions for the MQTT Gateway API, it is responsible for adding and removing subscriptions for both the asset and attribute topics
 * Subscriptions are added to the broker and the subscriber info map, the subscriber info map is used to keep track of the subscriptions for each connection
 *
 */
@SuppressWarnings({"unused", "rawtypes", "unchecked"})
public class GatewayMQTTSubscriptionManager {

    private static final Logger LOG = Logger.getLogger(GatewayMQTTSubscriptionManager.class.getName());
    protected final Map<String, Map<String, Consumer<? extends Event>>> sessionSubscriptionConsumers = new HashMap<>();
    protected GatewayMQTTHandler gatewayMQTTHandler;
    
    public GatewayMQTTSubscriptionManager(GatewayMQTTHandler gatewayMQTTHandler) {
        this.gatewayMQTTHandler = gatewayMQTTHandler;
    }

  
    /**
     * Adds a subscription for the specified topic. Validates the subscription class and asset filter,
     * then registers the subscription with the broker and tracks it for the session.
     * 
     * @param connection the connection associated with the subscription
     * @param topic the topic to subscribe to
     * @param subscriptionClass the class of the subscription (must be AssetEvent or AttributeEvent)
     * @param relativeToAsset the asset to build the filter relative to, can be null
     */
    public void addSubscription(RemotingConnection connection, Topic topic, Class subscriptionClass, Asset<?> relativeToAsset) {
        if (subscriptionClass != AssetEvent.class && subscriptionClass != AttributeEvent.class) {
            LOG.warning("Invalid subscription class " + subscriptionClass);
            return;
        }

        AssetFilter assetFilter = buildAssetFilter(topic, relativeToAsset);
        if (assetFilter == null) {
            LOG.warning("Invalid asset filter for topic " + topic);
            return;
        }

        EventSubscription subscription = new EventSubscription(subscriptionClass, assetFilter, topic.toString());
        Consumer<SharedEvent> consumer = buildEventSubscriptionConsumer(topic);
     

        synchronized (sessionSubscriptionConsumers) {
            // Create subscription consumer and track it for future removal requests
            Map<String, Consumer<? extends Event>> subscriptionConsumers = sessionSubscriptionConsumers.computeIfAbsent(getSessionKey(connection), s -> new HashMap<>());
            subscriptionConsumers.put(topic.getString(), consumer);
            gatewayMQTTHandler.clientEventService.addSubscription(subscription, consumer);
  
        }
    
    }

    /**
     * Removes a subscription for the specified topic. Unregisters the subscription from the broker
     * and removes it from the session's tracking map.
     * 
     * @param connection the connection associated with the subscription
     * @param topic the topic to unsubscribe from
     */
    public void removeSubscription(RemotingConnection connection, Topic topic) {
        String subscriptionId = topic.toString();
        String sessionKey = getSessionKey(connection);

        synchronized (sessionSubscriptionConsumers) {
            sessionSubscriptionConsumers.computeIfPresent(sessionKey, (connectionID, subscriptionConsumers) -> {
                Consumer<? extends Event> consumer = subscriptionConsumers.remove(subscriptionId);
                if (consumer != null) {
                    gatewayMQTTHandler.clientEventService.removeSubscription(consumer);
                }
                if (subscriptionConsumers.isEmpty()) {
                    return null;
                }
                return subscriptionConsumers;
            });
        }
    }

    /**
     * Removes all subscriptions associated with the specified connection.
     * 
     * @param connection the connection whose subscriptions are to be removed
     */
    public void removeAllSubscriptions(RemotingConnection connection) {
        synchronized (sessionSubscriptionConsumers) {
            sessionSubscriptionConsumers.remove(getSessionKey(connection));
        }
    }



    /**
     * Builds an asset filter based on the topic. The filter is used to determine which asset events
     * match the topic's pattern.
     * 
     * @param topic the topic to build the filter for
     * @param relativeToAsset the asset to build the filter relative to, can be null
     * @return the constructed AssetFilter, or null if the topic is unsupported
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
                // no filter needed
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
        } else {
            String assetId = topicTokens.size() > 4 ? topicTokens.get(ASSET_ID_TOKEN_INDEX) : "";
            String attributeName = topicTokens.size() > 6 ? topicTokens.get(ATTRIBUTE_NAME_TOKEN_INDEX) : "";
            boolean attributeNameIsNotWildcardOrEmpty = !attributeName.equals("+") 
                && !attributeName.equals("#") && !attributeName.isEmpty();
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

  

    /**
     * Builds a consumer that processes and publishes events to the specified topic.
     * 
     * @param topic the topic to publish events to
     * @return a Consumer that handles SharedEvent instances
     */
    protected Consumer<SharedEvent> buildEventSubscriptionConsumer(Topic topic) {
        MqttQoS mqttQoS = MqttQoS.AT_MOST_ONCE;
        Function<SharedEvent, String> topicExpander;
        List<String> topicTokens = topic.getTokens();
        boolean isAttributesValueTopic = ATTRIBUTES_VALUE_TOPIC.equals(topicTokenIndexToString(topic, ATTRIBUTES_TOKEN_INDEX));

        if (isAssetsTopic(topic)) {
            String topicStr = topic.toString();
            String replaceToken = determineReplaceToken(topicStr);
            topicExpander = ev -> replaceToken != null ? topicStr.replace(replaceToken, ((AssetEvent) ev).getId()) : topicStr;
        } else {
            String topicStr = topic.toString();

            if (topicTokens.size() > 4 && topicTokens.get(ASSET_ID_TOKEN_INDEX).equals(TOKEN_SINGLE_LEVEL_WILDCARD)) {
                topicTokens.set(ASSET_ID_TOKEN_INDEX, "$assetId");
            }

            if (topicTokens.size() > 6 && (topicTokens.get(ATTRIBUTE_NAME_TOKEN_INDEX).equals(TOKEN_SINGLE_LEVEL_WILDCARD) 
                || topicTokens.get(ATTRIBUTE_NAME_TOKEN_INDEX).equals(TOKEN_MULTI_LEVEL_WILDCARD))) {
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
                    gatewayMQTTHandler.publishMessage(topicExpander.apply(ev), ev, mqttQoS);
                }
            } else {
                if (ev instanceof AttributeEvent attributeEvent) {
                    if (isAttributesValueTopic) {
                        gatewayMQTTHandler.publishMessage(topicExpander.apply(ev), attributeEvent.getValue().orElse(null), mqttQoS);
                    } else {
                        gatewayMQTTHandler.publishMessage(topicExpander.apply(ev), ev, mqttQoS);
                    }
                }
            }
        };
    }

    /**
     * Determines the token to replace in the topic string. This is typically the last token
     * in the topic string if it is a wildcard.
     * 
     * @param topicStr the topic string to analyze
     * @return the token to replace, or null if no replacement is needed
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

    /**
     * Retrieves the session key for a given connection. This key is used to track subscriptions
     * for the connection.
     * 
     * @param connection the connection to get the session key for
     * @return the session key as a String
     */
    protected static String getSessionKey(RemotingConnection connection) {
        return getConnectionIDString(connection);
    }




}
