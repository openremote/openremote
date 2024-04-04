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
public class GatewayMQTTSubscriptionManager {

    private static final Logger LOG = Logger.getLogger(GatewayMQTTSubscriptionManager.class.getName());
    private final ConcurrentMap<String, GatewaySubscriberInfo> connectionSubscriberInfoMap = new ConcurrentHashMap<>();
    private final MessageBrokerService messageBrokerService;
    private final MQTTBrokerService mqttBrokerService;

    public GatewayMQTTSubscriptionManager(MessageBrokerService messageBrokerService, MQTTBrokerService mqttBrokerService) {
        this.messageBrokerService = messageBrokerService;
        this.mqttBrokerService = mqttBrokerService;
    }

    /**
     * Unsubscribe from a topic, removes the subscription and consumer from the connectionSubscriberInfoMap
     */


    public ConcurrentMap<String, GatewaySubscriberInfo> getConnectionSubscriberInfoMap() {
        return connectionSubscriberInfoMap;
    }

    public void subscribe(RemotingConnection connection, Topic topic, Class subscriptionClass) {

        if (subscriptionClass != AssetEvent.class && subscriptionClass != AttributeEvent.class) {
            LOG.warning("Invalid subscription class " + subscriptionClass);
            return;
        }
        String subscriptionId = topic.toString();
        AssetFilter assetFilter = buildAssetFilter(topic);

        EventSubscription subscription = new EventSubscription(subscriptionClass, assetFilter, subscriptionId);
        Map<String, Object> headers = prepareHeaders(topicRealm(topic), connection);
        subscribeToTopic(subscription, headers);
        Consumer<SharedEvent> subscriptionConsumer = subscriptionClass == AssetEvent.class ?
                getAssetEventSubscriptionConsumer(topic) : null;

        addSubscriberInfo(connection, topic, subscriptionConsumer);
    }

    public void unsubscribe(RemotingConnection connection, Topic topic) {
        unsubscribeFromTopic(connection, topic);
        removeSubscriberInfo(connection, topic);
    }

    // Add the event subscription and consumer to the connectionSubscriberInfoMap
    protected void subscribeToTopic(EventSubscription subscription, Map<String, Object> headers) {
        messageBrokerService.getFluentProducerTemplate()
                .withHeaders(headers)
                .withBody(subscription)
                .to(CLIENT_INBOUND_QUEUE)
                .asyncSend();

    }

    protected AssetFilter<?> buildAssetFilter(Topic topic) {
        boolean isAttributesTopic = isAttributesTopic(topic);
        boolean isAssetsTopic = isAssetsTopic(topic) && !isAttributesTopic;

        String realm = topicRealm(topic);
        List<String> assetIds = new ArrayList<>();
        List<String> parentIds = new ArrayList<>();
        List<String> paths = new ArrayList<>();
        List<String> attributeNames = new ArrayList<>();
        var topicTokens = topic.getTokens();


        // asset events
        if (isAssetsTopic) {
            var assetId = topicTokens.get(ASSET_ID_TOKEN_INDEX);
            var hasAssetId = Pattern.matches(ASSET_ID_REGEXP, assetId);
            var path = "";
            if (topicTokens.size() > ASSET_ID_TOKEN_INDEX + 1) {
                path = topicTokens.get(ASSET_ID_TOKEN_INDEX + 1);
            }

            // realm/clientId/events/assets/#
            // realm/clientId/events/assets/+
            // realm/clientId/events/assets/{assetId}
            // realm/clientId/events/assets/{assetId}/#
            // realm/clientId/events/assets/{assetId}/+
            if (assetId.equals("#")) {
            }
            else if (assetId.equals("+")) {
                parentIds.add(null);
            }
            else if (hasAssetId) {
                assetIds.add(assetId);
            }
            else if (path.equals("#")) {
                paths.add(assetId);
            }
            else if (path.equals("+")) {
                parentIds.add(assetId);
            }
            else {
                return null;
            }
        }
        else if (isAttributesTopic) {
            var assetId = topicTokens.get(ASSET_ID_TOKEN_INDEX);
            var attributeName = topicTokens.get(ATTRIBUTE_NAME_TOKEN_INDEX);
            var hasAssetId = Pattern.matches(ASSET_ID_REGEXP, assetId);
            var hasAttributeName = !attributeName.equals("+") && !attributeName.equals("#");


            // realm/clientId/events/assets/{assetId}/attributes/#
            // realm/clientId/events/assets/{assetId}/attributes/{attributeName}
            // realm/clientId/events/assets/{assetId}/attributes/{attributeName}/#
            // realm/clientId/events/assets/{assetId}/attributes/{attributeName}/+
            if (hasAssetId) {
                var path = topicTokens.get(ATTRIBUTE_NAME_TOKEN_INDEX + 1);

            }

            // realm/clientId/events/assets/+/attributes/{attributeName}/#
            // realm/clientId/events/assets/+/attributes/{attributeName}/+
            // realm/clientId/events/assets/+/attributes/+/#
            // realm/clientId/events/assets/+/attributes/+/+
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


    protected void unsubscribeFromTopic(RemotingConnection connection, Topic topic) {
        String subscriptionId = topic.toString();

        //determine the event class based on the event consumer in the subscription map
        boolean isAssetTopic = connectionSubscriberInfoMap.get(getConnectionIDString(connection))
                .topicSubscriptionMap.get(topic.getString()) instanceof AssetEvent;

        Class<SharedEvent> subscriptionClass = (Class) (isAssetTopic ? AssetEvent.class : AttributeEvent.class);
        CancelEventSubscription cancelEventSubscription = new CancelEventSubscription(subscriptionClass, subscriptionId);
        messageBrokerService.getFluentProducerTemplate()
                .withHeaders(prepareHeaders(topicRealm(topic), connection))
                .withBody(cancelEventSubscription)
                .to(CLIENT_INBOUND_QUEUE)
                .asyncSend();
    }


    protected void addSubscriberInfo(RemotingConnection connection, Topic topic, Consumer<SharedEvent> subscriptionConsumer) {
        synchronized (connectionSubscriberInfoMap) {
            connectionSubscriberInfoMap.compute(getConnectionIDString(connection), (connectionID, subscriberInfo) -> {
                if (subscriberInfo == null) {
                    return new GatewaySubscriberInfo(topic.getString(), subscriptionConsumer);
                } else {
                    subscriberInfo.add(topic.getString(), subscriptionConsumer);
                    return subscriberInfo;
                }
            });
        }
    }


    protected void removeSubscriberInfo(RemotingConnection connection, Topic topic) {
        synchronized (connectionSubscriberInfoMap) {
            connectionSubscriberInfoMap.computeIfPresent(getConnectionIDString(connection), (connectionID, subscriberInfo) -> {
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
    protected Consumer<SharedEvent> getAssetEventSubscriptionConsumer(Topic topic) {
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

        return ev -> {
            if (ev instanceof AssetEvent) {
                LOG.info("Publishing asset event " + ev + " to topic " + topicExpander.apply(ev));
                mqttBrokerService.publishMessage(topicExpander.apply(ev), ev, mqttQoS);
            } else if (ev instanceof AttributeEvent) {
                LOG.info("Publishing attribute event " + ev + " to topic " + topicExpander.apply(ev));
                mqttBrokerService.publishMessage(topicExpander.apply(ev), ev, mqttQoS);
            }
        };
    }


    public static class GatewaySubscriberInfo {
        public Map<String, Consumer<SharedEvent>> topicSubscriptionMap;

        public GatewaySubscriberInfo(String topic, Consumer<SharedEvent> subscriptionConsumer) {
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
