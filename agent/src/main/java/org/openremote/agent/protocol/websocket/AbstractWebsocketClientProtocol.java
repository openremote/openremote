/*
 * Copyright 2017, OpenRemote Inc.
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
package org.openremote.agent.protocol.websocket;

import com.fasterxml.jackson.core.type.TypeReference;
import org.apache.http.HttpHeaders;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;
import org.jboss.resteasy.util.BasicAuthHelper;
import org.openremote.agent.protocol.Protocol;
import org.openremote.agent.protocol.http.HttpClientProtocol;
import org.openremote.container.web.OAuthGrant;
import org.openremote.container.web.WebTargetBuilder;
import org.openremote.agent.protocol.io.AbstractIoClientProtocol;
import org.openremote.container.Container;
import org.openremote.model.AbstractValueHolder;
import org.openremote.model.asset.Asset;
import org.openremote.model.asset.AssetAttribute;
import org.openremote.model.asset.agent.ConnectionStatus;
import org.openremote.model.asset.agent.ProtocolConfiguration;
import org.openremote.model.attribute.*;
import org.openremote.model.syslog.SyslogCategory;
import org.openremote.model.util.Pair;
import org.openremote.model.util.TextUtil;
import org.openremote.model.value.StringValue;
import org.openremote.model.value.Values;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.openremote.agent.protocol.http.HttpClientProtocol.*;
import static org.openremote.container.web.WebTargetBuilder.createClient;
import static org.openremote.model.Constants.PROTOCOL_NAMESPACE;
import static org.openremote.model.attribute.MetaItemDescriptor.Access.ACCESS_PRIVATE;
import static org.openremote.model.attribute.MetaItemDescriptorImpl.*;
import static org.openremote.model.syslog.SyslogCategory.PROTOCOL;
import static org.openremote.model.util.TextUtil.REGEXP_PATTERN_STRING_NON_EMPTY;

/**
 * This is a base class for websocket client protocols for communicating with Websocket servers; it uses the
 * {@link WebsocketIoClient} to handle the communication, but also uses the {@link WebTargetBuilder} factory to send
 * {@link WebsocketHttpSubscription}s when the connection is initially established.
 * <p>
 * Implementations of this protocol must provide the required encoders and decoders via {@link #getEncoderDecoderProvider}.
 * <h1>Protocol Configurations</h1>
 * <p>
 * {@link Attribute}s that are configured as {@link ProtocolConfiguration}s for this protocol support the meta
 * items defined in {@link #PROTOCOL_META_ITEM_DESCRIPTORS}.
 * <h1>Linked Attributes</h1>
 * <p>
 * {@link Attribute}s that are linked to this protocol using an {@link MetaItemType#AGENT_LINK} {@link MetaItem} support
 * the meta items defined in {@link #ATTRIBUTE_META_ITEM_DESCRIPTORS}.
 * <h1>Protocol Specifics</h1>
 * When the websocket connection is established it is possible to subscribe to events by specifying the
 * {@link #META_SUBSCRIPTIONS} {@link MetaItem}, a subscription can be either a message sent over the websocket
 * or a HTTP REST API call.
 */
public abstract class AbstractWebsocketClientProtocol<T> extends AbstractIoClientProtocol<T, WebsocketIoClient<T>> {

    public static final String PROTOCOL_NAME = PROTOCOL_NAMESPACE + ":abstractWebsocketClient";
    private static final Logger LOG = SyslogCategory.getLogger(PROTOCOL, AbstractWebsocketClientProtocol.class);

    /*--------------- META ITEMS TO BE USED ON PROTOCOL CONFIGURATIONS ---------------*/

    /**
     * Websocket connect endpoint URI
     */
    public static final MetaItemDescriptor META_PROTOCOL_CONNECT_URI = metaItemString(
        PROTOCOL_NAME + ":uri",
        ACCESS_PRIVATE,
        true,
        REGEXP_PATTERN_STRING_NON_EMPTY,
        PatternFailure.STRING_EMPTY
    );

    /**
     * Headers for websocket connect call (see {@link HttpClientProtocol#META_HEADERS} for details)
     */
    public static final MetaItemDescriptor META_PROTOCOL_CONNECT_HEADERS = metaItemObject(
        PROTOCOL_NAME + ":headers",
        ACCESS_PRIVATE,
        false,
        null);

    /**
     * Array of {@link WebsocketSubscription}s that should be executed either once the websocket connection is
     * established (when used on a linked {@link ProtocolConfiguration}) or when a linked attribute is linked (when
     * used on a linked {@link Attribute}); the subscriptions are executed in the order specified in the array
     */
    public static final MetaItemDescriptor META_SUBSCRIPTIONS = metaItemArray(
        PROTOCOL_NAME + ":subscriptions",
        ACCESS_PRIVATE,
        false,
        null);

    /*--------------- META ITEMS TO BE USED ON LINKED ATTRIBUTES ---------------*/

    public static final List<MetaItemDescriptor> ATTRIBUTE_META_ITEM_DESCRIPTORS = Arrays.asList(
        META_SUBSCRIPTIONS);

    public static final List<MetaItemDescriptor> PROTOCOL_META_ITEM_DESCRIPTORS = Arrays.asList(
        META_PROTOCOL_CONNECT_URI,
        META_PROTOCOL_CONNECT_HEADERS,
        META_PROTOCOL_USERNAME,
        META_PROTOCOL_PASSWORD,
        META_PROTOCOL_OAUTH_GRANT,
        META_SUBSCRIPTIONS);

    public static final int CONNECTED_SEND_DELAY_MILLIS = 2000;
    protected ResteasyClient client;
    protected final Map<AttributeRef, List<Runnable>> protocolConnectedTasks = new HashMap<>();
    protected final Map<AttributeRef, Map<AttributeRef, Runnable>> attributeConnectedTasks = new HashMap<>();
    protected Map<AttributeRef, MultivaluedMap<String, String>> clientHeaders = new HashMap<>();

    @Override
    public void init(Container container) throws Exception {
        super.init(container);
        client = createClient(executorService);
    }

    @Override
    protected void doUnlinkProtocolConfiguration(Asset agent, AssetAttribute protocolConfiguration) {
        super.doUnlinkProtocolConfiguration(agent, protocolConfiguration);
        AttributeRef protocolRef = protocolConfiguration.getReferenceOrThrow();
        clientHeaders.remove(protocolRef);
        synchronized (protocolConnectedTasks) {
            protocolConnectedTasks.remove(protocolRef);
        }
        synchronized (attributeConnectedTasks) {
            attributeConnectedTasks.remove(protocolRef);
        }
    }

    @Override
    protected WebsocketIoClient<T> createIoClient(AssetAttribute protocolConfiguration) throws Exception {
        final AttributeRef protocolRef = protocolConfiguration.getReferenceOrThrow();

        String uriStr = protocolConfiguration.getMetaItem(META_PROTOCOL_CONNECT_URI)
            .flatMap(AbstractValueHolder::getValueAsString).orElseThrow(() ->
                new IllegalArgumentException("Missing or invalid required meta item: " + META_PROTOCOL_CONNECT_URI));

        URI uri = new URI(uriStr);

        /* We're going to fail hard and fast if optional meta items are incorrectly configured */

        Optional<OAuthGrant> oAuthGrant = Protocol.getOAuthGrant(protocolConfiguration);
        Optional<Pair<StringValue, StringValue>> usernameAndPassword = getUsernameAndPassword(protocolConfiguration);

        MultivaluedMap<String, String> headers = Values.getMetaItemValueOrThrow(
            protocolConfiguration,
            META_PROTOCOL_CONNECT_HEADERS,
            false,
            true)
            .flatMap(Values::getObject)
            .flatMap(objectValue -> getMultivaluedMap(objectValue, true))
            .orElse(null);

        Optional<WebsocketSubscription<T>[]> subscriptions = getSubscriptions(protocolConfiguration);

        if (!oAuthGrant.isPresent() && usernameAndPassword.isPresent()) {
            String authValue = BasicAuthHelper.createHeader(usernameAndPassword.get().key.getString(), usernameAndPassword.get().value.getString());
            if (headers == null) {
                headers = new MultivaluedHashMap<>();
                headers.add(HttpHeaders.AUTHORIZATION, authValue);
            } else {
                headers.remove(HttpHeaders.AUTHORIZATION);
                headers.replace(HttpHeaders.AUTHORIZATION, Collections.singletonList(authValue));
            }
        }

        WebsocketIoClient<T> websocketClient = new WebsocketIoClient<>(uri, headers, oAuthGrant.orElse(null), executorService);

        MultivaluedMap<String, String> finalHeaders = headers;
        clientHeaders.put(protocolRef, headers);
        subscriptions.ifPresent(websocketSubscriptions ->
            addProtocolConnectedTask(protocolRef, () -> doSubscriptions(protocolRef, websocketClient, finalHeaders, websocketSubscriptions))
        );

        return websocketClient;
    }

    @Override
    protected void onConnectionStatusChanged(AttributeRef protocolRef, ConnectionStatus connectionStatus) {
        super.onConnectionStatusChanged(protocolRef, connectionStatus);
        if (connectionStatus == ConnectionStatus.CONNECTED) {
            onConnected(protocolRef);
        }
    }

    @Override
    protected void doLinkAttribute(AssetAttribute attribute, AssetAttribute protocolConfiguration) {
        AttributeRef protocolRef = protocolConfiguration.getReferenceOrThrow();
        ProtocolIoClient<T, WebsocketIoClient<T>> protocolClient = protocolIoClientMap.get(protocolRef);
        WebsocketIoClient<T> client = protocolClient.client;
        Optional<WebsocketSubscription<T>[]> subscriptions = getSubscriptions(attribute);

        subscriptions.ifPresent(websocketSubscriptions -> {
            MultivaluedMap<String, String> headers = clientHeaders.get(protocolRef);
            Runnable task = () -> doSubscriptions(protocolRef, client, headers, websocketSubscriptions);
            addAttributeConnectedTask(protocolRef, attribute.getReferenceOrThrow(), task);
            if (client.getConnectionStatus() == ConnectionStatus.CONNECTED) {
                executorService.schedule(task, 1000);
            }
        });
    }

    @Override
    protected void doUnlinkAttribute(AssetAttribute attribute, AssetAttribute protocolConfiguration) {
        AttributeRef protocolRef = protocolConfiguration.getReferenceOrThrow();
        AttributeRef attributeRef = attribute.getReferenceOrThrow();

        synchronized (attributeConnectedTasks) {
            attributeConnectedTasks.compute(protocolRef, (ref, tasks) -> {
                if (tasks != null) {
                    tasks.remove(attributeRef);
                }
                return tasks;
            });
        }
    }

    protected void onConnected(AttributeRef protocolRef) {
        // Look for any subscriptions that need to be processed
        List<Runnable> protocolTasks;
        Map<AttributeRef, Runnable> attributeTasks;

        synchronized (protocolConnectedTasks) {
            protocolTasks = protocolConnectedTasks.get(protocolRef);
        }
        if (protocolTasks != null) {
            // Execute after a delay to ensure connection is properly initialised
            executorService.schedule(() -> protocolTasks.forEach(Runnable::run), CONNECTED_SEND_DELAY_MILLIS);
        }

        synchronized (attributeConnectedTasks) {
            attributeTasks = attributeConnectedTasks.get(protocolRef);
        }
        if (attributeTasks != null) {
            // Execute after a delay to ensure connection is properly initialised
            executorService.schedule(() -> attributeTasks.forEach((ref, task) -> task.run()), CONNECTED_SEND_DELAY_MILLIS);
        }
    }

    protected void addProtocolConnectedTask(AttributeRef protocolRef, Runnable task) {
        synchronized (protocolConnectedTasks) {
            protocolConnectedTasks.compute(protocolRef, (ref, tasks) -> {
                if (tasks == null) {
                    tasks = new ArrayList<>();
                }
                tasks.add(task);
                return tasks;
            });
        }
    }

    protected void addAttributeConnectedTask(AttributeRef protocolRef, AttributeRef attributeRef, Runnable task) {
        synchronized (attributeConnectedTasks) {
            attributeConnectedTasks.compute(protocolRef, (ref, tasks) -> {
                if (tasks == null) {
                    tasks = new HashMap<>();
                }
                tasks.put(attributeRef, task);
                return tasks;
            });
        }
    }

    protected void doSubscriptions(AttributeRef protocolRef, WebsocketIoClient<T> websocketClient, MultivaluedMap<String, String> headers, WebsocketSubscription<T>[] subscriptions) {
        LOG.info("Executing subscriptions for websocket: " + websocketClient.getClientUri());

        // Inject OAuth header
        if (!TextUtil.isNullOrEmpty(websocketClient.authHeaderValue)) {
            if (headers == null) {
                headers = new MultivaluedHashMap<>();
            }
            headers.remove(HttpHeaders.AUTHORIZATION);
            headers.add(HttpHeaders.AUTHORIZATION, websocketClient.authHeaderValue);
        }

        MultivaluedMap<String, String> finalHeaders = headers;
        Arrays.stream(subscriptions).forEach(
            subscription -> doSubscription(websocketClient, finalHeaders, subscription)
        );
    }

    protected void doSubscription(WebsocketIoClient<T> websocketClient, MultivaluedMap<String, String> headers, WebsocketSubscription<T> subscription) {
        if (subscription instanceof WebsocketHttpSubscription) {
            WebsocketHttpSubscription httpSubscription = (WebsocketHttpSubscription)subscription;

            if (TextUtil.isNullOrEmpty(httpSubscription.uri)) {
                LOG.warning("Websocket subscription missing or empty URI so skipping: " + subscription);
                return;
            }

            URI uri;

            try {
                uri = new URI(httpSubscription.uri);
            } catch (URISyntaxException e) {
                LOG.warning("Websocket subscription invalid URI so skipping: " + subscription);
                return;
            }

            if (httpSubscription.method == null) {
                httpSubscription.method = WebsocketHttpSubscription.Method.valueOf(DEFAULT_HTTP_METHOD);
            }

            if (TextUtil.isNullOrEmpty(httpSubscription.contentType)) {
                httpSubscription.contentType = DEFAULT_CONTENT_TYPE;
            }

            if (httpSubscription.headers != null) {
                headers = headers != null ? new MultivaluedHashMap<String, String>(headers) : new MultivaluedHashMap<>();
                @SuppressWarnings("OptionalGetWithoutIsPresent")
                MultivaluedMap<String, String> subscriptionHeaders = getMultivaluedMap(httpSubscription.headers, false).get();
                MultivaluedMap<String, String> finalHeaders = headers;
                subscriptionHeaders.forEach((header, values) -> {
                    if (values == null || values.isEmpty()) {
                        finalHeaders.remove(header);
                    } else {
                        finalHeaders.addAll(header, values);
                    }
                });
            }

            WebTargetBuilder webTargetBuilder = new WebTargetBuilder(client, uri);

            if (headers != null) {
                webTargetBuilder.setInjectHeaders(headers);
            }

            LOG.fine("Creating web target client for subscription '" + uri + "'");
            ResteasyWebTarget target = webTargetBuilder.build();

            Invocation invocation;

            if (httpSubscription.body == null) {
                invocation = target.request().build(httpSubscription.method.toString());
            } else {
                invocation = target.request().build(httpSubscription.method.toString(), Entity.entity(httpSubscription.body, httpSubscription.contentType));
            }
            Response response = invocation.invoke();
            response.close();
            if (response.getStatusInfo().getFamily() != Response.Status.Family.SUCCESSFUL) {
                LOG.warning("WebsocketHttpSubscription returned an un-successful response code: " + response.getStatus());
            }
        } else {
            websocketClient.sendMessage(subscription.body);
        }
    }

    protected static <T> Optional<WebsocketSubscription<T>[]> getSubscriptions(Attribute attribute) {
        return Values.getMetaItemValueOrThrow(
            attribute,
            META_SUBSCRIPTIONS,
            false,
            true)
            .flatMap(Values::getArray)
            .map(arrValue -> {
                try {
                    return Container.JSON.readValue(arrValue.toJson(), new TypeReference<WebsocketSubscription<T>[]>() {});
                } catch (IOException e) {
                    LOG.log(Level.WARNING, "Failed to deserialize WebsocketSubscription[]", e);
                    return null;
                }
            });
    }
}
