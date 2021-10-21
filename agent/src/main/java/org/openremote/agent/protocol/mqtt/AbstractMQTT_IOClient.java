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
import org.openremote.agent.protocol.io.IOClient;
import org.openremote.container.Container;
import org.openremote.container.util.UniqueIdentifierGenerator;
import org.openremote.model.asset.agent.ConnectionStatus;
import org.openremote.model.auth.UsernamePassword;
import org.openremote.model.syslog.SyslogCategory;

import java.net.URI;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.openremote.model.syslog.SyslogCategory.PROTOCOL;

public abstract class AbstractMQTT_IOClient<S> implements IOClient<MQTTMessage<S>> {

    public static final Logger LOG = SyslogCategory.getLogger(PROTOCOL, AbstractMQTT_IOClient.class);
    protected String clientId;
    protected String host;
    protected int port;
    protected boolean secure;
    protected UsernamePassword usernamePassword;
    protected URI websocketURI;
    protected Mqtt3AsyncClient client;
    protected final Set<Consumer<ConnectionStatus>> connectionStatusConsumers = new HashSet<>();
    protected final Map<String, Set<Consumer<MQTTMessage<S>>>> topicConsumerMap = new HashMap<>();
    protected ScheduledExecutorService executorService;
    protected ConnectionStatus connectionStatus = ConnectionStatus.DISCONNECTED;
    protected boolean disconnected = true; // Need to use this flag to cancel client reconnect task

    protected AbstractMQTT_IOClient(String host, int port, boolean secure, UsernamePassword usernamePassword, URI websocketURI) {
        this(UniqueIdentifierGenerator.generateId(), host, port, secure, usernamePassword, websocketURI);
    }

    protected AbstractMQTT_IOClient(String clientId, String host, int port, boolean secure, UsernamePassword usernamePassword, URI websocketURI) {
        this.clientId = clientId;
        this.host = host;
        this.port = port;
        this.secure = secure;
        this.usernamePassword = usernamePassword;
        this.websocketURI = websocketURI;
        this.executorService = Container.EXECUTOR_SERVICE;

        Mqtt3ClientBuilder builder = MqttClient.builder()
            .useMqttVersion3()
            .identifier(clientId)
            .addConnectedListener(context -> {
                LOG.info("Client is connected from the broker '" + getClientUri() + "'");
                onConnectionStatusChanged(ConnectionStatus.CONNECTED);
            })
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
                onConnectionStatusChanged(userClosed ? ConnectionStatus.DISCONNECTED : ConnectionStatus.WAITING);
            })
            .automaticReconnect()
            .initialDelay(RECONNECT_DELAY_INITIAL_MILLIS, TimeUnit.MILLISECONDS)
            .maxDelay(RECONNECT_DELAY_MAX_MILLIS, TimeUnit.MILLISECONDS)
            .applyAutomaticReconnect();

        if (secure) {
            builder = builder.sslWithDefaultConfig();
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

        try {
            client = builder.buildAsync();
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Invalid MQTT client config for client '" + getClientUri() + "'", e);
            client = null;
            connectionStatus = ConnectionStatus.ERROR;
        }
    }

    @Override
    public void sendMessage(MQTTMessage<S> message) {
        if (client == null) {
            LOG.info("Cannot send message as client is invalid  '" + getClientUri() + "'");
            return;
        }

        client.publishWith()
            .topic(message.topic)
            .payload(messageToBytes(message.payload))
            .send()
            .whenComplete((publish, throwable) -> {
                if (throwable != null) {
                    // Failure
                    LOG.log(Level.INFO, "Failed to publish to MQTT broker '" + getClientUri() + "'", throwable);
                } else {
                    // Success
                    LOG.finer("Published message to MQTT broker '" + getClientUri() + "'");
                }
            });
    }

    @Override
    public void addMessageConsumer(Consumer<MQTTMessage<S>> messageConsumer) {
        addMessageConsumer("#", messageConsumer);
    }

    public void addMessageConsumer(String topic, Consumer<MQTTMessage<S>> messageConsumer) {
        if (client == null) {
            return;
        }

        boolean initialise;
        Set<Consumer<MQTTMessage<S>>> consumers;

        synchronized (topicConsumerMap) {
            consumers = topicConsumerMap.computeIfAbsent(topic, (t) -> new HashSet<>());
            synchronized (consumers) {
                initialise = consumers.isEmpty();
                consumers.add(messageConsumer);
            }
        }

        if (initialise) {
            addSubscription(
                topic,
                message -> {
                    if (!topicConsumerMap.containsKey(topic)) {
                        return;
                    }
                    synchronized (consumers) {
                        consumers.forEach(consumer -> {
                            try {
                                consumer.accept(message);
                            } catch (Exception e) {
                                LOG.log(Level.WARNING, "Message consumer threw an exception", e);
                            }
                        });
                    }
                },
                () -> {
                    synchronized (consumers) {
                        consumers.remove(messageConsumer);
                        if (consumers.isEmpty()) {
                            synchronized (topicConsumerMap) {
                                topicConsumerMap.remove(topic);
                            }
                        }
                    }
                });
        }
    }

    @Override
    public void removeMessageConsumer(Consumer<MQTTMessage<S>> messageConsumer) {
        removeMessageConsumer("#", messageConsumer);
    }

    public void removeMessageConsumer(String topic, Consumer<MQTTMessage<S>> messageConsumer) {
        synchronized (topicConsumerMap) {
            topicConsumerMap.computeIfPresent(topic, (t, consumers) -> {
                //noinspection SynchronizationOnLocalVariableOrMethodParameter
                synchronized (consumers) {
                    if (consumers.remove(messageConsumer) && consumers.isEmpty()) {
                        removeSubscription(topic);
                        return null;
                    }
                    return consumers;
                }
            });
        }
    }

    @Override
    public void removeAllMessageConsumers() {
        synchronized (topicConsumerMap) {
            Set<String> topics = new HashSet<>(topicConsumerMap.keySet());
            topicConsumerMap.clear();
            topics.forEach(this::removeSubscription);
        }
    }

    protected void addSubscription(String topic, Consumer<MQTTMessage<S>> onPublish, Runnable onFailure) {
        client.subscribeWith()
            .topicFilter(topic)
            .callback(publish -> {
                try {
                    String topicStr = publish.getTopic().toString();
                    S payload = messageFromBytes(publish.getPayloadAsBytes());
                    MQTTMessage<S> message = new MQTTMessage<>(topicStr, payload);
                    onPublish.accept(message);
                } catch (Exception e) {
                    LOG.log(Level.WARNING, "Failed to process published message on client '" + getClientUri() + "'", e);
                }
            })
            .send()
            .whenComplete((subAck, throwable) -> {
                if (throwable != null) {
                    LOG.log(Level.WARNING, "Failed to subscribe to topic '" + topic + "' on client '" + getClientUri() + "'", throwable);
                    onFailure.run();
                } else {
                    LOG.fine("Subscribed to topic '" + topic + "' on client '" + getClientUri() + "'");
                }
            });
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
        return connectionStatus;
    }

    @Override
    public void connect() {
        synchronized (this) {
            if (getConnectionStatus() != ConnectionStatus.DISCONNECTED) {
                LOG.finer("Must be disconnected and not in error before calling connect: " + getClientUri());
                return;
            }

            LOG.fine("Connecting MQTT Client: " + getClientUri());
            onConnectionStatusChanged(ConnectionStatus.CONNECTING);
        }

        this.disconnected = false;

        LOG.info("Establishing connection: " + getClientUri());

        Mqtt3ConnectBuilder.Send<CompletableFuture<Mqtt3ConnAck>> completableFutureSend = client.connectWith().keepAlive(5);

        if (usernamePassword != null) {
            completableFutureSend = completableFutureSend.simpleAuth()
                .username(usernamePassword.getUsername())
                .password(usernamePassword.getPassword().getBytes())
                .applySimpleAuth();
        }

        completableFutureSend.send();
    }

    protected void onConnectionStatusChanged(ConnectionStatus connectionStatus) {
        this.connectionStatus = connectionStatus;

        executorService.submit(() -> {
            synchronized (connectionStatusConsumers) {
                connectionStatusConsumers.forEach(
                    consumer -> {
                        try {
                            consumer.accept(connectionStatus);
                        } catch (Exception e) {
                            LOG.log(Level.WARNING, "Connection status change handler threw an exception: " + getClientUri(), e);
                        }
                    });
            }
        });
    }

    @Override
    public void disconnect() {
        if (client == null) {
            return;
        }

        synchronized (this) {
            if (connectionStatus == ConnectionStatus.DISCONNECTED) {
                LOG.finest("Already disconnected: " + getClientUri());
                return;
            }

            LOG.finest("Disconnecting IO client: " + getClientUri());
            onConnectionStatusChanged(ConnectionStatus.DISCONNECTED);
        }

        this.disconnected = true;

        client.disconnect().whenComplete((unused, throwable) -> {

            if (throwable != null) {
                LOG.info("Failed to disconnect");
            }
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
