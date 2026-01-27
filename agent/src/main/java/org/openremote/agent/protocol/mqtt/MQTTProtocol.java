/*
 * Copyright 2021, OpenRemote Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package org.openremote.agent.protocol.mqtt;

import static org.openremote.model.syslog.SyslogCategory.PROTOCOL;

import java.net.URI;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.logging.Logger;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManagerFactory;

import com.hivemq.client.mqtt.datatypes.MqttQos;

import org.apache.http.client.utils.URIBuilder;
import org.openremote.model.Container;
import org.openremote.model.attribute.Attribute;
import org.openremote.model.attribute.AttributeEvent;
import org.openremote.model.attribute.AttributeRef;
import org.openremote.model.protocol.ProtocolUtil;
import org.openremote.model.protocol.mqtt.Topic;
import org.openremote.model.security.KeyStoreService;
import org.openremote.model.syslog.SyslogCategory;
import org.openremote.model.util.UniqueIdentifierGenerator;
import org.openremote.model.util.ValueUtil;

import jakarta.validation.constraints.NotNull;

public class MQTTProtocol
    extends AbstractMQTTClientProtocol<
        MQTTProtocol, MQTTAgent, String, MQTT_IOClient, MQTTAgentLink> {

  private static final Logger LOG = SyslogCategory.getLogger(PROTOCOL, MQTTProtocol.class);
  public static final String PROTOCOL_DISPLAY_NAME = "MQTT Client";
  protected final Map<AttributeRef, Consumer<MQTTMessage<String>>> protocolMessageConsumers =
      new HashMap<>();
  protected final Map<String, Map<String, Map<AttributeRef, Consumer<MQTTMessage<String>>>>>
      wildcardTopicConsumerMap = new HashMap<>();
  protected final Set<Topic> wildcardTopics = new HashSet<>();
  protected KeyStoreService keyStoreService;

  protected MQTTProtocol(MQTTAgent agent) {
    super(agent);
  }

  @Override
  protected void doLinkAttribute(String assetId, Attribute<?> attribute, MQTTAgentLink agentLink)
      throws RuntimeException {
    agentLink
        .getSubscriptionTopic()
        .ifPresent(
            topicStr -> {
              AttributeRef attributeRef = new AttributeRef(assetId, attribute.getName());
              Topic topic = Topic.parse(topicStr);
              Topic matchedWildcardTopic = findMatchingWildcardTopic(topic);

              Consumer<String> genericConsumer =
                  ProtocolUtil.createGenericAttributeMessageConsumer(
                      assetId,
                      attribute,
                      agentLink,
                      timerService::getCurrentTimeMillis,
                      this::updateLinkedAttribute);
              Consumer<MQTTMessage<String>> messageConsumer =
                  msg -> {
                    if (genericConsumer != null) {
                      genericConsumer.accept(msg.payload);
                    } else {
                      updateLinkedAttribute(
                          new AttributeRef(assetId, attribute.getName()), msg.payload);
                    }
                  };

              if (matchedWildcardTopic != null) {
                addWildcardMessageConsumer(
                    matchedWildcardTopic.toString(),
                    topicStr,
                    attributeRef,
                    Optional.of(agentLink.getQos().orElse(agent.getSubscribeQoS().orElse(0)))
                        .map(qos -> qos > 2 || qos < 0 ? null : qos)
                        .map(MqttQos::fromCode)
                        .orElse(MqttQos.AT_MOST_ONCE),
                    messageConsumer);
              } else {
                client.addMessageConsumer(
                    topicStr,
                    Optional.of(agentLink.getQos().orElse(agent.getSubscribeQoS().orElse(0)))
                        .map(qos -> qos > 2 || qos < 0 ? null : qos)
                        .map(MqttQos::fromCode)
                        .orElse(MqttQos.AT_MOST_ONCE),
                    messageConsumer);
                protocolMessageConsumers.put(
                    new AttributeRef(assetId, attribute.getName()), messageConsumer);
              }
            });
  }

  @Override
  protected void doStart(Container container) throws Exception {
    keyStoreService = container.getService(KeyStoreService.class);
    if (keyStoreService == null) throw new Exception("Couldn't load KeyStoreService");

    agent
        .getWildcardSubscriptionTopics()
        .ifPresent(
            wildcardTopicStrs -> {
              try {
                List<Topic> wildcardTopics =
                    Arrays.stream(wildcardTopicStrs)
                        .map(Topic::parse)
                        .peek(
                            t -> {
                              if (!t.hasWildcard()) {
                                throw new IllegalArgumentException(
                                    "Wildcard topic must contain at least one wildcard: " + t);
                              }
                            })
                        .toList();
                this.wildcardTopics.addAll(wildcardTopics);
              } catch (Exception e) {
                LOG.warning("Failed to parse wildcard topic list: " + e.getMessage());
                throw e;
              }
            });

    super.doStart(container);
  }

  @Override
  protected void doUnlinkAttribute(
      String assetId, Attribute<?> attribute, MQTTAgentLink agentLink) {
    agentLink
        .getSubscriptionTopic()
        .ifPresent(
            topicStr -> {
              AttributeRef attributeRef = new AttributeRef(assetId, attribute.getName());
              Consumer<MQTTMessage<String>> messageConsumer =
                  protocolMessageConsumers.remove(attributeRef);
              if (messageConsumer != null) {
                client.removeMessageConsumer(topicStr, messageConsumer);
              } else {
                Topic topic = Topic.parse(topicStr);
                Optional.ofNullable(findMatchingWildcardTopic(topic))
                    .ifPresent(
                        wildcardTopic ->
                            removeWildcardMessageConsumer(
                                wildcardTopic.toString(), topicStr, attributeRef));
              }
            });
  }

  @Override
  protected MQTT_IOClient doCreateIoClient() throws Exception {
    String host = agent.getHost().orElse(null);
    int port =
        agent
            .getPort()
            .orElseGet(
                () -> {
                  if (agent.isSecureMode().orElse(false)) {
                    return agent.isWebsocketMode().orElse(false) ? 443 : 8883;
                  } else {
                    return agent.isWebsocketMode().orElse(false) ? 80 : 1883;
                  }
                });

    URI websocketURI = null;

    if (agent.isWebsocketMode().orElse(false)) {
      URIBuilder builder = new URIBuilder().setHost(host).setPort(port);
      agent.getWebsocketPath().ifPresent(builder::setPath);
      agent
          .getWebsocketQuery()
          .map(query -> query.startsWith("?") ? query.substring(1) : query)
          .ifPresent(builder::setCustomQuery);
      websocketURI = builder.build();
    }

    MQTTLastWill lastWill = null;

    if (agent.getLastWillTopic().isPresent()) {
      String topic = agent.getLastWillTopic().get();
      String payload = agent.getLastWillPayload().orElse(null);
      boolean retain = agent.isLastWillRetain().orElse(false);
      lastWill = new MQTTLastWill(topic, payload, retain);
    }

    // It's fine if they're null, they're not going to be used when creating the client
    TrustManagerFactory trustManagerFactory = null;
    KeyManagerFactory keyManagerFactory = null;
    if (agent.isSecureMode().orElse(false) && agent.getCertificateAlias().isPresent()) {
      trustManagerFactory = keyStoreService.getTrustManagerFactory();
      keyManagerFactory =
          keyStoreService.getKeyManagerFactory(
              agent.getRealm() + "." + agent.getCertificateAlias().orElseThrow());
    }

    MQTT_IOClient client =
        new MQTT_IOClient(
            agent.getClientId().orElseGet(UniqueIdentifierGenerator::generateId),
            host,
            port,
            agent.isSecureMode().orElse(false),
            !agent.isResumeSession().orElse(false),
            agent.getUsernamePassword().orElse(null),
            websocketURI,
            lastWill,
            keyManagerFactory,
            trustManagerFactory);

    agent.getSubscribeQoS().ifPresent(qos -> client.setSubscribeQos(MqttQos.fromCode(qos)));
    agent.getPublishQoS().ifPresent(qos -> client.setPublishQos(MqttQos.fromCode(qos)));

    return client;
  }

  @Override
  protected void addMessageConsumer(MQTT_IOClient client) {
    // Don't want the default message consumer, topic specific consumers will do the message routing
    // for us
  }

  @Override
  protected void onMessageReceived(MQTTMessage<String> message) {
    // This isn't used instead messages are targeted by topic
  }

  @Override
  protected MQTTMessage<String> createWriteMessage(
      MQTTAgentLink agentLink, AttributeEvent event, Object processedValue) {
    Optional<String> topic = agentLink.getPublishTopic();

    if (topic.isEmpty()) {
      LOG.fine(
          prefixLogMessage("Publish topic is not set in agent link so cannot publish message"));
      return null;
    }

    String valueStr = ValueUtil.convert(processedValue, String.class);
    return new MQTTMessage<>(topic.get(), valueStr, agentLink.getQos().orElse(null));
  }

  @Override
  public String getProtocolName() {
    return "MQTT Client";
  }

  protected void addWildcardMessageConsumer(
      String wildcardTopic,
      String topic,
      AttributeRef attributeRef,
      MqttQos qos,
      Consumer<MQTTMessage<String>> messageConsumer) {
    LOG.fine(
        "Adding wildcard message consumer uri="
            + client.getClientUri()
            + ", wildcardTopic="
            + wildcardTopic
            + ", topic="
            + topic
            + ", "
            + attributeRef);

    synchronized (wildcardTopicConsumerMap) {
      wildcardTopicConsumerMap.compute(
          wildcardTopic,
          (wt, subConsumers) -> {
            if (subConsumers == null) {
              subConsumers = new ConcurrentHashMap<>();
              Map<String, Map<AttributeRef, Consumer<MQTTMessage<String>>>> finalSubConsumers =
                  subConsumers;
              client.addMessageConsumer(
                  wildcardTopic,
                  qos,
                  msg ->
                      Optional.ofNullable(finalSubConsumers.get(msg.getTopic()))
                          .ifPresent(
                              consumers ->
                                  consumers.values().forEach(consumer -> consumer.accept(msg))));
            }
            subConsumers
                .computeIfAbsent(topic, t -> new ConcurrentHashMap<>())
                .put(attributeRef, messageConsumer);
            return subConsumers;
          });
    }
  }

  protected void removeWildcardMessageConsumer(
      String wildcardTopic, String topic, AttributeRef attributeRef) {
    LOG.fine(
        "Removing wildcard message consumer uri="
            + client.getClientUri()
            + ", wildcardTopic="
            + wildcardTopic
            + ", topic="
            + topic
            + ", "
            + attributeRef);

    synchronized (wildcardTopicConsumerMap) {
      wildcardTopicConsumerMap.computeIfPresent(
          wildcardTopic,
          (wt, wildcardTopicConsumers) -> {
            wildcardTopicConsumers.computeIfPresent(
                topic,
                (t, attributeRefConsumerMap) -> {
                  attributeRefConsumerMap.remove(attributeRef);
                  return attributeRefConsumerMap.isEmpty() ? null : attributeRefConsumerMap;
                });
            if (wildcardTopicConsumers.isEmpty()) {
              // No more consumers for the wildcard topic
              client.removeMessageConsumers(wt);
              return null;
            }
            return wildcardTopicConsumers;
          });
    }
  }

  protected Topic findMatchingWildcardTopic(@NotNull Topic topic) {
    return wildcardTopics.stream()
        .filter(wildcardTopic -> wildcardTopic.matches(topic))
        .findFirst()
        .orElse(null);
  }
}
