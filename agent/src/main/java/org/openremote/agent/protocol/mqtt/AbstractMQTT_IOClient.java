/*
 * Copyright 2021, OpenRemote Inc.
 *
 * See the CONTRIBUTORS.txt file in the distribution for a
 * full listing of individual contributors.
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
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.openremote.agent.protocol.mqtt;

import com.hivemq.client.mqtt.MqttClient;
import com.hivemq.client.mqtt.MqttClientSslConfig;
import com.hivemq.client.mqtt.MqttClientSslConfigBuilder;
import com.hivemq.client.mqtt.MqttClientState;
import com.hivemq.client.mqtt.datatypes.MqttQos;
import com.hivemq.client.mqtt.exceptions.ConnectionClosedException;
import com.hivemq.client.mqtt.exceptions.ConnectionFailedException;
import com.hivemq.client.mqtt.lifecycle.MqttDisconnectSource;
import com.hivemq.client.mqtt.mqtt3.Mqtt3AsyncClient;
import com.hivemq.client.mqtt.mqtt3.Mqtt3ClientBuilder;
import com.hivemq.client.mqtt.mqtt3.exceptions.Mqtt3ConnAckException;
import com.hivemq.client.mqtt.mqtt3.exceptions.Mqtt3DisconnectException;
import com.hivemq.client.mqtt.mqtt3.lifecycle.Mqtt3ClientDisconnectedContext;
import com.hivemq.client.mqtt.mqtt3.message.connect.Mqtt3ConnectBuilder;
import com.hivemq.client.mqtt.mqtt3.message.connect.connack.Mqtt3ConnAck;
import com.hivemq.client.mqtt.mqtt3.message.subscribe.Mqtt3Subscribe;
import com.hivemq.client.mqtt.mqtt3.message.subscribe.suback.Mqtt3SubAck;
import com.hivemq.client.mqtt.mqtt3.message.subscribe.suback.Mqtt3SubAckReturnCode;
import org.openremote.agent.protocol.io.IOClient;
import org.openremote.container.Container;
import org.openremote.model.asset.agent.ConnectionStatus;
import org.openremote.model.auth.UsernamePassword;
import org.openremote.model.syslog.SyslogCategory;
import org.openremote.model.util.Pair;
import org.openremote.model.util.UniqueIdentifierGenerator;
import org.openremote.model.util.ValueUtil;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManagerFactory;
import java.net.URI;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.openremote.agent.protocol.io.AbstractNettyIOClient.RECONNECT_DELAY_INITIAL_MILLIS;
import static org.openremote.agent.protocol.io.AbstractNettyIOClient.RECONNECT_DELAY_MAX_MILLIS;
import static org.openremote.model.syslog.SyslogCategory.PROTOCOL;

public abstract class AbstractMQTT_IOClient<S> implements IOClient<MQTTMessage<S>> {

    public static final Logger LOG = SyslogCategory.getLogger(PROTOCOL, AbstractMQTT_IOClient.class);
    protected String clientId;
    protected String host;
    protected int port;
    protected boolean secure;
    protected boolean cleanSession;
    protected UsernamePassword usernamePassword;
    protected URI websocketURI;
    protected Mqtt3AsyncClient client;
    protected final Set<Consumer<ConnectionStatus>> connectionStatusConsumers = new CopyOnWriteArraySet<>();
    protected final ConcurrentMap<String, Pair<MqttQos, Set<Consumer<MQTTMessage<S>>>>> topicConsumerMap = new ConcurrentHashMap<>();
    protected ScheduledExecutorService executorService;
    protected boolean disconnected = true; // Need to use this flag to cancel client reconnect task
    protected final AtomicBoolean connected = new AtomicBoolean(false); // Used for subscriptions
    protected Consumer<String> topicSubscribeFailureConsumer;
    protected ConnectionStatus currentStatus;
    protected MqttQos publishQos = MqttQos.AT_LEAST_ONCE;
    protected MqttQos subscribeQos = MqttQos.AT_LEAST_ONCE;

    protected AbstractMQTT_IOClient(String host, int port, boolean secure, boolean cleanSession, UsernamePassword usernamePassword, URI websocketURI, MQTTLastWill lastWill, KeyManagerFactory keyManagerFactory, TrustManagerFactory trustManagerFactory) {
        this(UniqueIdentifierGenerator.generateId(), host, port, secure, cleanSession, usernamePassword, websocketURI, lastWill, keyManagerFactory, trustManagerFactory);
    }

    protected AbstractMQTT_IOClient(String clientId, String host, int port, boolean secure, boolean cleanSession, UsernamePassword usernamePassword, URI websocketURI, MQTTLastWill lastWill, KeyManagerFactory keyManagerFactory, TrustManagerFactory trustManagerFactory) {
        this.clientId = clientId;
        this.host = host;
        this.port = port;
        this.secure = secure;
        this.cleanSession = cleanSession;
        this.usernamePassword = usernamePassword;
        this.websocketURI = websocketURI;
        this.executorService = Container.SCHEDULED_EXECUTOR;

        Mqtt3ClientBuilder builder = MqttClient.builder()
            .useMqttVersion3()
            .identifier(clientId)
            .addConnectedListener(context -> onConnectionStatusChanged(null))
            .addDisconnectedListener(context -> {
                boolean userClosed = context.getSource() == MqttDisconnectSource.USER;
                if (disconnected) {
                    context.getReconnector().reconnect(false);
                } else {
                    if (this.usernamePassword != null) {
                        ((Mqtt3ClientDisconnectedContext) context).getReconnector().connectWith()
                            .simpleAuth()
                            .username(usernamePassword.getUsername())
                            .password(usernamePassword.getPassword().getBytes())
                            .applySimpleAuth()
                            .applyConnect();
                    }
                }
                if (context.getCause() instanceof Mqtt3DisconnectException) {
                    LOG.info("Client disconnect '" + getClientUri() + "': initiator=" + context.getSource());
                } else if (context.getCause() instanceof Mqtt3ConnAckException) {
                    LOG.info("Connection rejected by the broker '" + getClientUri() + "': reasonCode=" + ((Mqtt3ConnAckException)context.getCause()).getMqttMessage().getReturnCode() + ", initiator=" + context.getSource());
                } else if (context.getCause() instanceof ConnectionClosedException) {
                    LOG.info("Connection closed by " + context.getSource() + " '" + getClientUri() + "': initiator=" + context.getSource());
                } else if (context.getCause() instanceof ConnectionFailedException) {
                    LOG.log(Level.INFO, "Connection failed '" + getClientUri() + "': initiator=" + context.getSource(), context.getCause());
                }
                onConnectionStatusChanged(ConnectionStatus.DISCONNECTED);
            })
            .automaticReconnect()
            .initialDelay(RECONNECT_DELAY_INITIAL_MILLIS, TimeUnit.MILLISECONDS)
            .maxDelay(RECONNECT_DELAY_MAX_MILLIS, TimeUnit.MILLISECONDS)
            .applyAutomaticReconnect();

        if (secure) {
            MqttClientSslConfigBuilder sslBuilder = MqttClientSslConfig.builder();
            if (keyManagerFactory != null) {
                sslBuilder = sslBuilder.keyManagerFactory(keyManagerFactory);
            } if (trustManagerFactory != null) {
                sslBuilder = sslBuilder.trustManagerFactory(trustManagerFactory);
            }
            builder = builder.sslConfig(sslBuilder.build());
        }

        if (websocketURI != null) {
            builder = builder
                .serverHost(websocketURI.getHost())
                .serverPort(websocketURI.getPort())
                .webSocketConfig()
                    .serverPath(websocketURI.getPath())
                    .queryString(websocketURI.getQuery())
                .applyWebSocketConfig();
        } else {
            builder = builder
                .serverHost(host)
                .serverPort(port);
        }

        if (lastWill != null) {
            builder.willPublish()
                .topic(lastWill.getTopic())
                .payload(ValueUtil.getStringCoerced(lastWill.getPayload()).orElse("").getBytes())
                .retain(lastWill.isRetain())
                .applyWillPublish();
        }

        try {
            client = builder.buildAsync();
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Invalid MQTT client config for client '" + getClientUri() + "'", e);
            client = null;
        }
    }

    public AbstractMQTT_IOClient<S> setPublishQos(MqttQos publishQos) {
        this.publishQos = publishQos;
        return this;
    }

    public AbstractMQTT_IOClient<S> setSubscribeQos(MqttQos subscribeQos) {
        this.subscribeQos = subscribeQos;
        return this;
    }

    @Override
    public void sendMessage(MQTTMessage<S> message) {
        if (client == null) {
            LOG.info("Cannot send message as client is invalid: " + getClientUri());
            return;
        }

        if (getConnectionStatus() != ConnectionStatus.CONNECTED) {
            LOG.info("Cannot send message as client is not connected: " + getClientUri());
            return;
        }

        // TODO: Might need to put some timeout logic here as this call can block if client becomes disconnected see (https://github.com/hivemq/hivemq-mqtt-client/issues/554)
        client.publishWith()
            .topic(message.getTopic())
            .payload(messageToBytes(message.getPayload()))
            .qos(Optional.ofNullable(message.getQos()).map(qos -> qos > 2 || qos < 0 ? null : qos).map(MqttQos::fromCode).orElse(MqttQos.AT_LEAST_ONCE))
            .send()
            .whenComplete((publish, throwable) -> {
                if (throwable != null) {
                    // Failure
                    LOG.log(Level.INFO, "Failed to publish to MQTT broker '" + getClientUri() + "'", throwable);
                } else {
                    // Success
                    LOG.finest("Published message to MQTT broker '" + getClientUri() + "'");
                }
            });
    }

    @Override
    public void addMessageConsumer(Consumer<MQTTMessage<S>> messageConsumer) {
        addMessageConsumer("#", null, messageConsumer);
    }

    public boolean addMessageConsumer(String topic, Consumer<MQTTMessage<S>> messageConsumer) {
        return addMessageConsumer(topic, null, messageConsumer);
    }

    public boolean addMessageConsumer(String topic, MqttQos qos, Consumer<MQTTMessage<S>> messageConsumer) {
        if (client == null) {
            return false;
        }

        Pair<MqttQos, Set<Consumer<MQTTMessage<S>>>> consumers = topicConsumerMap.computeIfAbsent(topic, t ->
                new Pair<>(qos != null ? qos : MqttQos.AT_LEAST_ONCE, new HashSet<>()));

        if (consumers.getValue().isEmpty()) {
            // Create the subscription on the client
            if (doClientSubscription(topic)) {
                consumers.getValue().add(messageConsumer);
                return true;
            } else {
                return false;
            }
        } else {
            consumers.getValue().add(messageConsumer);
            return true;
        }
    }

    public void setTopicSubscribeFailureConsumer(Consumer<String> topicSubscribeFailureConsumer) {
        this.topicSubscribeFailureConsumer = topicSubscribeFailureConsumer;
    }

    protected void onSubscribeFailed(String topic) {
        if (this.topicSubscribeFailureConsumer != null) {
            this.topicSubscribeFailureConsumer.accept(topic);
        }
    }

    protected boolean doClientSubscription(String topic) {
        synchronized (connected) {
            if (!connected.get()) {
                // Just return true and let connection logic sort out actual subscription
                return true;
            }
        }

        Consumer<MQTTMessage<S>> messageConsumer = message -> {
            Pair<MqttQos, Set<Consumer<MQTTMessage<S>>>> topicConsumers = topicConsumerMap.get(topic);
            if (topicConsumers != null) {
                topicConsumers.getValue().forEach(consumer -> {
                    try {
                        consumer.accept(message);
                    } catch (Exception e) {
                        LOG.log(Level.WARNING, "Message consumer threw an exception", e);
                    }
                });
            }
        };

        Pair<MqttQos, Set<Consumer<MQTTMessage<S>>>> topicConsumers = topicConsumerMap.get(topic);
        try {
            Mqtt3Subscribe subscribeMessage = Mqtt3Subscribe.builder()
                    .topicFilter(topic)
                    .qos(topicConsumers.getKey())
                    .build();

            Mqtt3SubAck subAck = client.subscribe(subscribeMessage, publish -> {
                try {
                    String topicStr = publish.getTopic().toString();
                    S payload = messageFromBytes(publish.getPayloadAsBytes());
                    MQTTMessage<S> message = new MQTTMessage<>(topicStr, payload);
                    messageConsumer.accept(message);
                } catch (Exception e) {
                    LOG.log(Level.WARNING, "Failed to process published message on client '" + getClientUri() + "'", e);
                }
            }).get();
            if (subAck.getReturnCodes().contains(Mqtt3SubAckReturnCode.FAILURE)) {
                throw new Exception("Server returned failure code for subscription");
            }
            LOG.fine("Subscribed to topic '" + topic + "' on client '" + getClientUri() + "'");
            return true;
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Failed to subscribe to topic '" + topic + "' on client '" + getClientUri() + "': " + e.getMessage());
            executorService.execute(() -> onSubscribeFailed(topic));
        }
        topicConsumerMap.remove(topic);
        return false;
    }

    @Override
    public void removeMessageConsumer(Consumer<MQTTMessage<S>> messageConsumer) {
        removeMessageConsumer("#", messageConsumer);
    }

    public void removeMessageConsumer(String topic, Consumer<MQTTMessage<S>> messageConsumer) {
        topicConsumerMap.computeIfPresent(topic, (t, consumers) -> {
            if (consumers.getValue().remove(messageConsumer) && consumers.getValue().isEmpty()) {
                removeSubscription(topic);
                return null;
            }
            return consumers;
        });
    }

    @Override
    public void removeAllMessageConsumers() {
        Set<String> topics = new HashSet<>(topicConsumerMap.keySet());
        topicConsumerMap.clear();
        topics.forEach(this::removeSubscription);
    }

    protected void removeSubscription(String topic) {
        if (client != null) {
            client.unsubscribeWith()
                .topicFilter(topic)
                .send()
                .whenComplete((subAck, throwable) -> {
                    if (throwable != null) {
                        LOG.log(Level.WARNING, "Failed to unsubscribe to topic '" + topic + "' on client '" + getClientUri() + "'", throwable);
                    } else {
                        LOG.fine("Unsubscribed from topic '" + topic + "' on client '" + getClientUri() + "'");
                    }
                });
        }
    }

    @Override
    public void addConnectionStatusConsumer(Consumer<ConnectionStatus> connectionStatusConsumer) {
        connectionStatusConsumers.add(connectionStatusConsumer);
    }

    @Override
    public void removeConnectionStatusConsumer(Consumer<ConnectionStatus> connectionStatusConsumer) {
        connectionStatusConsumers.remove(connectionStatusConsumer);
    }

    @Override
    public void removeAllConnectionStatusConsumers() {
        connectionStatusConsumers.clear();
    }

    @Override
    public ConnectionStatus getConnectionStatus() {
        if (client == null) {
            return ConnectionStatus.ERROR;
        }
        MqttClientState state = client.getState();
        if (state == MqttClientState.CONNECTED) {
            return ConnectionStatus.CONNECTED;
        }
        if (state == MqttClientState.DISCONNECTED) {
            return ConnectionStatus.DISCONNECTED;
        }

        // We cannot differentiate between connecting and waiting with the callbacks provided so just use waiting state
        return ConnectionStatus.WAITING;
    }

    @Override
    public void connect() {
        synchronized (this) {
            if (client == null) {
                return;
            }
            if (!disconnected) {
                LOG.finest("Must be disconnected before calling connect: " + getClientUri());
                return;
            }

            LOG.fine("Connecting MQTT Client: " + getClientUri());
            this.disconnected = false;
        }

        LOG.info("Establishing connection: " + getClientUri());

        Mqtt3ConnectBuilder.Send<CompletableFuture<Mqtt3ConnAck>> completableFutureSend = client.connectWith()
            .cleanSession(true) // Always clean session as there's some inconsistency in how HiveMQ client handles this on reconnects
            .keepAlive(5);

        if (usernamePassword != null) {
            completableFutureSend = completableFutureSend.simpleAuth()
                .username(usernamePassword.getUsername())
                .password(usernamePassword.getPassword().getBytes())
                .applySimpleAuth();
        }

        completableFutureSend.send().whenComplete((connAck, throwable) -> {
            if (throwable != null) {
                LOG.log(Level.INFO, "Connection failed:" + getClientUri(), throwable.getMessage());
            } else {
                synchronized (connected) {
                    connected.set(true);

                    if (!this.cleanSession && !connAck.isSessionPresent()) {
                        // Need to re-instate the subscriptions as HiveMQ client doesn't do it
                        // Re-add all subscriptions
                        topicConsumerMap.keySet().forEach(this::doClientSubscription);
                    }
                }
            }
        });
    }

    /**
     * Allows an override to be provided as {@link #getConnectionStatus} doesn't always return the correct status when a
     * server initiated disconnect occurs (some sort of timing issue).
     */
    protected void onConnectionStatusChanged(ConnectionStatus statusOverride) {
        ConnectionStatus status = statusOverride != null ? statusOverride : getConnectionStatus();
        if (currentStatus == status) {
            return;
        }
        currentStatus = status;
        LOG.info("Client '" + getClientUri() + "' connection status changed: " + status);

        connectionStatusConsumers.forEach(
                consumer -> {
                    try {
                        consumer.accept(status);
                    } catch (Exception e) {
                        LOG.log(Level.WARNING, "Connection status change handler threw an exception: " + getClientUri(), e);
                    }
                });
    }

    @Override
    public void disconnect() {
        if (client == null) {
            return;
        }

        synchronized (this) {
            if (disconnected) {
                LOG.finest("Already disconnected: " + getClientUri());
                return;
            }

            LOG.finest("Disconnecting IO client: " + getClientUri());
            this.disconnected = true;
        }

        client.disconnect().whenComplete((unused, throwable) -> {
            connected.set(false);

            if (throwable != null) {
                LOG.log(Level.INFO, "Disconnect error '" + getClientUri() + "':" + throwable.getMessage());
            }
            if (this.cleanSession) {
                removeAllMessageConsumers();
            }

            onConnectionStatusChanged(ConnectionStatus.DISCONNECTED);
        });
    }

    @Override
    public String getClientUri() {
        if (websocketURI != null) {
            return "mqtt_" + websocketURI + "?clientId=" + clientId;
        }
        return "mqtt" + (secure ? "s://" : "://" ) + host + ":" + port + "/?clientId=" + clientId;
    }

    public abstract byte[] messageToBytes(S message);

    public abstract S messageFromBytes(byte[] bytes);
}
