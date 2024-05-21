package org.openremote.manager.mqtt;

import io.netty.handler.codec.mqtt.MqttQoS;
import org.apache.activemq.artemis.spi.core.protocol.RemotingConnection;
import org.openremote.container.message.MessageBrokerService;
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

@SuppressWarnings({"unused", "rawtypes", "unchecked"})
public class GatewayMQTTEventSubscriptionManager {

    private static final Logger LOG = Logger.getLogger(GatewayMQTTEventSubscriptionManager.class.getName());
    private final ConcurrentMap<String, GatewayEventSubscriberInfo> eventSubscriberInfoMap = new ConcurrentHashMap<>();
    private final MessageBrokerService messageBrokerService;
    private final MQTTBrokerService mqttBrokerService;

    public GatewayMQTTEventSubscriptionManager(MessageBrokerService messageBrokerService, MQTTBrokerService mqttBrokerService) {
        this.messageBrokerService = messageBrokerService;
        this.mqttBrokerService = mqttBrokerService;
    }

    public ConcurrentMap<String, GatewayEventSubscriberInfo> getEventSubscriberInfoMap() {
        return eventSubscriberInfoMap;
    }

    public void addSubscription(RemotingConnection connection, Topic topic, Class subscriptionClass) {

        if (subscriptionClass != AssetEvent.class && subscriptionClass != AttributeEvent.class) {
            LOG.warning("Invalid subscription class " + subscriptionClass);
            return;
        }
        String subscriptionId = topic.toString();
        AssetFilter assetFilter = buildAssetFilter(topic);

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

    public void removeSubscription(RemotingConnection connection, Topic topic) {
        cancelSubscriptionFromBroker(connection, topic);
        removeSubscriberInfo(connection, topic);
    }

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

    protected static AssetFilter<?> buildAssetFilter(Topic topic) {
        var topicTokens = topic.getTokens();
        boolean isAttributesTopic = isAttributesTopic(topic);
        boolean isAssetsTopic = isAssetsTopic(topic) && !isAttributesTopic;

        String realm = topicRealm(topic);
        List<String> assetIds = new ArrayList<>();
        List<String> parentIds = new ArrayList<>();
        List<String> paths = new ArrayList<>();
        List<String> attributeNames = new ArrayList<>();

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

            // if the topic has an assetId and is valid
            if (assetIdIsNotWildcardOrEmpty && Pattern.matches(ASSET_ID_REGEXP, assetId)) {
                // realm/clientId/events/assets/{assetId}/attributes/{attributeName}/<<path>>
                var path = topicTokens.size() > ATTRIBUTE_NAME_TOKEN_INDEX + 1 ? topicTokens.get(ATTRIBUTE_NAME_TOKEN_INDEX + 1) : "";

                // realm/clientId/events/assets/{assetId}/attributes/+
                // all attribute events for the specified asset
                if (attributeName.equals("+")) {
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
            // realm/clientId/events/assets/+/attributes/{attributeName}/#
            // All attribute events of the realm with the specified attributeName

            // realm/clientId/events/assets/+/attributes/{attributeName}/+
            // All attribute events for direct children of the realm with the specified attributeName

            // realm/clientId/events/assets/+/attributes/#
            // All attribute events of the realm

            // realm/clientId/events/assets/+/attributes/+
            // All attribute events of direct children of the realm
            else {
                return null;
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
     * Returns a consumer that publishes asset events to the specified topic
     */
    protected Consumer<SharedEvent> buildEventSubscriptionConsumer(Topic topic) {
        MqttQoS mqttQoS = MqttQoS.AT_MOST_ONCE;
        String topicStr = topic.toString();
        String replaceToken = topicStr.endsWith("+") ? "#" : null;

        Function<SharedEvent, String> topicExpander = ev -> {
            if (replaceToken != null && ev instanceof AssetEvent) {
                return topicStr.replace(replaceToken, ((AssetEvent) ev).getId());
            } else if (replaceToken != null && ev instanceof AttributeEvent) {
                return topicStr.replace(replaceToken, ((AttributeEvent) ev).getName());
            }
            return topicStr;
        };
        LOG.info("Creating event subscription consumer for topic " + topicStr);

        return ev -> {
            LOG.info("Received event " + ev + " for topic " + topicStr);
            if (ev instanceof AssetEvent) {
                LOG.info("Publishing asset event " + ev + " to topic " + topicExpander.apply(ev));
                mqttBrokerService.publishMessage(topicExpander.apply(ev), ev, mqttQoS);
            } else if (ev instanceof AttributeEvent) {
                LOG.info("Publishing attribute event " + ev + " to topic " + topicExpander.apply(ev));
                mqttBrokerService.publishMessage(topicExpander.apply(ev), ev, mqttQoS);
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
