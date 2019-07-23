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

import org.apache.http.HttpHeaders;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;
import org.jboss.resteasy.util.BasicAuthHelper;
import org.openremote.agent.protocol.AbstractProtocol;
import org.openremote.agent.protocol.Protocol;
import org.openremote.agent.protocol.http.HttpClientProtocol;
import org.openremote.agent.protocol.http.OAuthGrant;
import org.openremote.agent.protocol.http.WebTargetBuilder;
import org.openremote.container.Container;
import org.openremote.model.AbstractValueHolder;
import org.openremote.model.ValidationFailure;
import org.openremote.model.ValueHolder;
import org.openremote.model.asset.AssetAttribute;
import org.openremote.model.asset.agent.ConnectionStatus;
import org.openremote.model.asset.agent.ProtocolConfiguration;
import org.openremote.model.attribute.*;
import org.openremote.model.query.BaseAssetQuery;
import org.openremote.model.query.filter.StringPredicate;
import org.openremote.model.syslog.SyslogCategory;
import org.openremote.model.util.Pair;
import org.openremote.model.util.TextUtil;
import org.openremote.model.value.*;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.openremote.agent.protocol.http.HttpClientProtocol.*;
import static org.openremote.agent.protocol.http.WebTargetBuilder.createClient;
import static org.openremote.model.Constants.PROTOCOL_NAMESPACE;
import static org.openremote.model.attribute.MetaItemDescriptor.Access.ACCESS_PRIVATE;
import static org.openremote.model.attribute.MetaItemDescriptorImpl.*;
import static org.openremote.model.syslog.SyslogCategory.PROTOCOL;
import static org.openremote.model.util.TextUtil.REGEXP_PATTERN_STRING_NON_EMPTY;

/**
 * <p>
 * This is a Websocket client protocol for communicating with Websocket servers; it uses the {@link WebsocketClient}
 * to handle the communication, but also uses the {@link WebTargetBuilder} factory to send
 * {@link WebsocketHttpSubscription}s when the connection is initially established.
 * <h1>Protocol Configurations</h1>
 * <p>
 * {@link Attribute}s that are configured as {@link ProtocolConfiguration}s for this protocol support the meta
 * items defined in {@link #PROTOCOL_META_ITEM_DESCRIPTORS}.
 * <h1>Linked Attributes</h1>
 * <p>
 * {@link Attribute}s that are linked to this protocol using an {@link MetaItemType#AGENT_LINK} {@link MetaItem} support
 * the meta items defined in {@link #ATTRIBUTE_META_ITEM_DESCRIPTORS}.
 * <h1>Protocol -> Attribute</h1>
 * <p>
 * When a new value comes from the protocol destined for a linked {@link Attribute} the actual value written to the
 * attribute can be filtered in the standard way using {@link ValueFilter}s via the{@link MetaItemType#VALUE_FILTERS}
 * {@link MetaItem}.
 * <h1>Attribute -> Protocol</h1>
 * <p>
 * When a linked {@link Attribute} is written to, the actual value written to the protocol can either be the exact value
 * written to the linked {@link Attribute} or the {@link Protocol#META_ATTRIBUTE_WRITE_VALUE} {@link MetaItem} can be
 * used to inject the written value into a bigger payload using the {@link Protocol#DYNAMIC_VALUE_PLACEHOLDER} and then
 * this bigger payload will be written to the protocol.
 * <h1>Executable Attributes</h1>
 * When a linked {@link Attribute} that has an {@link MetaItemType#EXECUTABLE} {@link MetaItem} is executed the
 * {@link Value} stored in the {@link Protocol#META_ATTRIBUTE_WRITE_VALUE} {@link MetaItem} is actually written to the
 * protocol (note dynamic value injection doesn't work in this scenario as there is no dynamic value to inject).
 * <p>
 * <h1>Protocol Specifics</h1>
 * When the websocket connection is established it is possible to subscribe to events by specifying the
 * {@link #META_SUBSCRIPTIONS} {@link MetaItem}, a subscription can be either a message sent over the websocket
 * or a HTTP REST API call.
 * <p>
 * For a linked {@link Attribute} to display a value from the protocol the {@link #META_ATTRIBUTE_MESSAGE_MATCH_FILTERS}
 * and {@link #META_ATTRIBUTE_MESSAGE_MATCH_PREDICATE} {@link MetaItem}s must be specified these allow incoming messages to
 * be matched to the attribute.
 * <p>
 *
 */
public class WebsocketClientProtocol extends AbstractProtocol {

    public static final String PROTOCOL_NAME = PROTOCOL_NAMESPACE + ":websocketClient";
    public static final String PROTOCOL_DISPLAY_NAME = "Websocket Client";
    public static final String PROTOCOL_VERSION = "1.0";
    private static final Logger LOG = SyslogCategory.getLogger(PROTOCOL, WebsocketClientProtocol.class.getName());

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
     * Basic authentication username (string)
     */
    public static final MetaItemDescriptor META_PROTOCOL_USERNAME = metaItemString(
        PROTOCOL_NAME + ":username",
        ACCESS_PRIVATE,
        false,
        REGEXP_PATTERN_STRING_NON_EMPTY,
        PatternFailure.STRING_EMPTY);

    /**
     * Basic authentication password (string)
     */
    public static final MetaItemDescriptor META_PROTOCOL_PASSWORD = metaItemString(
        PROTOCOL_NAME + ":password",
        ACCESS_PRIVATE,
        false,
        REGEXP_PATTERN_STRING_NON_EMPTY,
        PatternFailure.STRING_EMPTY);

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
     * used on a linked {@link AssetAttribute}); the subscriptions are executed in the order specified in the array
     */
    public static final MetaItemDescriptor META_SUBSCRIPTIONS = metaItemArray(
        PROTOCOL_NAME + ":subscriptions",
        ACCESS_PRIVATE,
        false,
        null);

    /*--------------- META ITEMS TO BE USED ON LINKED ATTRIBUTES ---------------*/

    /**
     * {@link ValueFilter}s to apply to incoming messages with the final result being compared to the
     * {@link #META_ATTRIBUTE_MESSAGE_MATCH_PREDICATE}, if they are equal (e.g. {@link String#equals}) then the message
     * is intended for this linked attribute and the message can be once again filtered using the
     * {@link MetaItemType#VALUE_FILTERS} {@link MetaItem}. The {@link MetaItem} value should be an {@link ArrayValue}
     * of {@link ObjectValue}s where each {@link ObjectValue} represents a serialised {@link ValueFilter}. The message
     * will pass through the filters in array order
     */
    public static final MetaItemDescriptor META_ATTRIBUTE_MESSAGE_MATCH_FILTERS = metaItemArray(
        PROTOCOL_NAME + ":messageMatchFilters",
        ACCESS_PRIVATE,
        false,
        null);

    /**
     * The predicate to use on filtered message to determine if the message is intended for this
     * linked attribute
     */
    public static final MetaItemDescriptor META_ATTRIBUTE_MESSAGE_MATCH_PREDICATE = metaItemObject(
        PROTOCOL_NAME + ":messageMatchPredicate",
        ACCESS_PRIVATE,
        false,
        new StringPredicate(BaseAssetQuery.Match.EXACT, false, "").toModelValue());

    public static final List<MetaItemDescriptor> ATTRIBUTE_META_ITEM_DESCRIPTORS = Arrays.asList(
        META_ATTRIBUTE_MESSAGE_MATCH_FILTERS,
        META_ATTRIBUTE_MESSAGE_MATCH_PREDICATE,
        META_ATTRIBUTE_WRITE_VALUE);

    public static final List<MetaItemDescriptor> PROTOCOL_META_ITEM_DESCRIPTORS = Arrays.asList(
        META_PROTOCOL_CONNECT_URI,
        META_PROTOCOL_CONNECT_HEADERS,
        META_PROTOCOL_USERNAME,
        META_PROTOCOL_PASSWORD,
        META_PROTOCOL_OAUTH_GRANT,
        META_SUBSCRIPTIONS);

    protected ResteasyClient client;
    protected Map<AttributeRef, WebsocketClient> websocketClients = new HashMap<>();
    protected Map<AttributeRef, List<Pair<AttributeRef, Consumer<String>>>> protocolMessageConsumers = new HashMap<>();
    protected final Map<AttributeRef, List<Runnable>> protocolConnectedTasks = new HashMap<>();
    protected Map<AttributeRef, MultivaluedMap<String, String>> clientHeaders = new HashMap<>();
    protected Map<AttributeRef, Consumer<Value>> attributeWriteConsumers = new HashMap<>();

    @Override
    public void init(Container container) throws Exception {
        super.init(container);
        client = createClient(executorService);
    }

    @Override
    protected void doStop(Container container) {
        websocketClients.forEach((ref, client) -> client.disconnect());
    }

    @Override
    protected void doLinkProtocolConfiguration(AssetAttribute protocolConfiguration) {
        final AttributeRef protocolRef = protocolConfiguration.getReferenceOrThrow();

        if (!protocolConfiguration.isEnabled()) {
            updateStatus(protocolRef, ConnectionStatus.DISABLED);
            return;
        }

        String uriStr = protocolConfiguration.getMetaItem(META_PROTOCOL_CONNECT_URI)
                .flatMap(AbstractValueHolder::getValueAsString).orElseThrow(() ->
                        new IllegalArgumentException("Missing or invalid require meta item: " + META_PROTOCOL_CONNECT_URI));

        URI uri;

        try {
            uri = new URI(uriStr);
        } catch (URISyntaxException e) {
            LOG.log(Level.WARNING, "META_PROTOCOL_CONNECT_URI value is not a valid URI");
            updateStatus(protocolRef, ConnectionStatus.ERROR_CONFIGURATION);
            return;
        }

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

        Optional<WebsocketSubscription[]> subscriptions = Values.getMetaItemValueOrThrow(
            protocolConfiguration,
            META_SUBSCRIPTIONS,
            false,
            true)
            .flatMap(Values::getArray)
            .map(arrValue -> {
                try {
                    return Container.JSON.readValue(arrValue.toJson(), WebsocketSubscription[].class);
                } catch (IOException e) {
                    LOG.log(Level.WARNING, "Failed to deserialize WebsocketSubscription[]", e);
                    return null;
                }
            });

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

        try {
            WebsocketClient websocketClient = new WebsocketClient(uri, headers, oAuthGrant.orElse(null), executorService);
            websocketClient.addConnectionStatusConsumer(connectionStatus -> this.onClientConnectionStatusChanged(protocolRef, connectionStatus));
            websocketClient.addMessageConsumer(msg -> this.onClientMessage(protocolRef, msg));
            websocketClients.put(protocolRef, websocketClient);

            MultivaluedMap<String, String> finalHeaders = headers;
            clientHeaders.put(protocolRef, headers);
            subscriptions.ifPresent(websocketSubscriptions ->
                protocolConnectedTasks.put(protocolRef, new ArrayList<>(Collections.singleton(() -> doSubscriptions(websocketClient, finalHeaders, websocketSubscriptions))))
            );

            websocketClient.connect();
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Failed to establish websocket connection, protocol is unusable", e);
            updateStatus(protocolRef, ConnectionStatus.ERROR);
        }
    }

    @Override
    protected void doUnlinkProtocolConfiguration(AssetAttribute protocolConfiguration) {
        AttributeRef protocolConfigurationRef = protocolConfiguration.getReferenceOrThrow();
        WebsocketClient client = websocketClients.remove(protocolConfigurationRef);
        if (client != null) {
            client.removeAllMessageConsumers();
            client.removeAllConnectionStatusConsumers();
            client.disconnect();
        }
        clientHeaders.remove(protocolConfigurationRef);
        protocolMessageConsumers.remove(protocolConfigurationRef);
        protocolConnectedTasks.remove(protocolConfigurationRef);
    }

    @Override
    protected void doLinkAttribute(AssetAttribute attribute, AssetAttribute protocolConfiguration) {

        AttributeRef protocolRef = protocolConfiguration.getReferenceOrThrow();
        WebsocketClient client = websocketClients.get(protocolRef);

        if (client == null) {
            LOG.warning("Linked attribute protocol configuration is not valid or disabled so cannot link attribute");
            return;
        }

        ValueFilter[] filters = Values.getMetaItemValueOrThrow(
            attribute,
            META_ATTRIBUTE_MESSAGE_MATCH_FILTERS,
            false,
            true)
            .map(Value::toJson)
            .map(json -> {
                try {
                    return Container.JSON.readValue(json, ValueFilter[].class);
                } catch (IOException e) {
                    LOG.log(Level.WARNING, "Failed to deserialize ValueFilter[]", e);
                    return null;
                }
            }).orElse(null);

        StringPredicate matchPredicate = Values.getMetaItemValueOrThrow(
            attribute,
            META_ATTRIBUTE_MESSAGE_MATCH_PREDICATE,
            false,
            true)
            .map(Value::toJson)
            .map(s -> {
                try {
                    return Container.JSON.readValue(s, StringPredicate.class);
                } catch (IOException e) {
                    LOG.log(Level.WARNING, "Failed to deserialise StringPredicate", e);
                    return null;
                }
            })
            .orElse(null);

        Optional<WebsocketSubscription[]> subscriptions = Values.getMetaItemValueOrThrow(
            attribute,
            META_SUBSCRIPTIONS,
            false,
            true)
            .flatMap(Values::getArray)
            .map(arrValue -> {
                try {
                    return Container.JSON.readValue(arrValue.toJson(), WebsocketSubscription[].class);
                } catch (IOException e) {
                    LOG.log(Level.WARNING, "Failed to deserialize WebsocketSubscription[]", e);
                    return null;
                }
            });

        boolean hasFilters = filters != null || matchPredicate != null;

        if (!attribute.isReadOnly()) {
            Consumer<Value> writeConsumer = Protocol.createDynamicAttributeWriteConsumer(attribute, str -> {
                LOG.fine("Sending message to websocket client: " + client.getSocketAddressString());
                client.sendMessage(str);
            });
            attributeWriteConsumers.put(attribute.getReferenceOrThrow(), writeConsumer);
        } else if (!hasFilters) {
            LOG.warning("Readonly attribute doesn't have any message filters and will therefore display nothing: " + attribute.getReferenceOrThrow());
        }

        if (hasFilters && (filters == null || matchPredicate == null)) {
            LOG.warning("Invalid or missing '" + META_ATTRIBUTE_MESSAGE_MATCH_FILTERS.getUrn() + "' and/or '" + META_ATTRIBUTE_MESSAGE_MATCH_PREDICATE.getUrn() + "': " + attribute.getReferenceOrThrow());
            return;
        }

        if (hasFilters) {
            protocolMessageConsumers.compute(protocolRef, (ref, consumers) -> {
                if (consumers == null) {
                    consumers = new ArrayList<>();
                }
                consumers.add(new Pair<>(
                    attribute.getReferenceOrThrow(),
                    createAttributeWebsocketMessageConsumer(attribute.getReferenceOrThrow(), filters, matchPredicate)));
                return consumers;
            });
        }

        subscriptions.ifPresent(websocketSubscriptions -> {
            MultivaluedMap<String, String> headers = clientHeaders.get(protocolRef);
            Runnable task = () -> doSubscriptions(client, headers, websocketSubscriptions);
            if (client.getConnectionStatus() == ConnectionStatus.CONNECTED) {
                executorService.schedule(task, 1000);
            } else {
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
        });
    }

    @Override
    protected void doUnlinkAttribute(AssetAttribute attribute, AssetAttribute protocolConfiguration) {
        AttributeRef attributeRef = attribute.getReferenceOrThrow();
        attributeWriteConsumers.remove(attributeRef);
        protocolMessageConsumers.compute(protocolConfiguration.getReferenceOrThrow(), (ref, consumers) -> {
            if (consumers != null) {
                consumers.removeIf((attrRefConsumer) -> attrRefConsumer.key.equals(attributeRef));
            }
            return consumers;
        });
    }

    @Override
    protected void processLinkedAttributeWrite(AttributeEvent event, AssetAttribute protocolConfiguration) {

        Consumer<Value> writeConsumer = attributeWriteConsumers.get(event.getAttributeRef());

        if (writeConsumer == null) {
            LOG.fine("Attempt to write to an attribute that doesn't support writes: " + event.getAttributeRef());
            return;
        }

        AssetAttribute attribute = getLinkedAttribute(event.getAttributeRef());
        AttributeExecuteStatus status = null;

        if (attribute.isExecutable()) {
            status = event.getValue()
                .flatMap(Values::getString)
                .flatMap(AttributeExecuteStatus::fromString)
                .orElse(null);

            if (status != null && status != AttributeExecuteStatus.REQUEST_START) {
                LOG.fine("Unsupported execution status: " + status);
                return;
            }
        }

        Value value = status != null ? null : event.getValue().orElse(null);
        LOG.fine("Attribute write being sent to the websocket client: " + event.getAttributeRef());
        writeConsumer.accept(value);

        if (status != null) {
            updateLinkedAttribute(new AttributeState(event.getAttributeRef(), AttributeExecuteStatus.COMPLETED.asValue()));
        }
    }

    @Override
    public String getProtocolName() {
        return PROTOCOL_NAME;
    }

    @Override
    public String getProtocolDisplayName() {
        return PROTOCOL_DISPLAY_NAME;
    }

    @Override
    public String getVersion() {
        return PROTOCOL_VERSION;
    }

    @Override
    protected List<MetaItemDescriptor> getProtocolConfigurationMetaItemDescriptors() {
        return new ArrayList<>(PROTOCOL_META_ITEM_DESCRIPTORS);
    }

    @Override
    protected List<MetaItemDescriptor> getLinkedAttributeMetaItemDescriptors() {
        List<MetaItemDescriptor> descriptors = new ArrayList<>(super.getLinkedAttributeMetaItemDescriptors());
        descriptors.addAll(ATTRIBUTE_META_ITEM_DESCRIPTORS);
        return descriptors;
    }

    @Override
    public AssetAttribute getProtocolConfigurationTemplate() {
        return super.getProtocolConfigurationTemplate()
                .addMeta(
                        new MetaItem(META_PROTOCOL_CONNECT_URI, null)
                );
    }

    @Override
    public AttributeValidationResult validateProtocolConfiguration(AssetAttribute protocolConfiguration) {
        AttributeValidationResult result = super.validateProtocolConfiguration(protocolConfiguration);
        if (result.isValid()) {
            try {
                Protocol.getOAuthGrant(protocolConfiguration);
                getUsernameAndPassword(protocolConfiguration);
            } catch (IllegalArgumentException e) {
                result.addAttributeFailure(
                        new ValidationFailure(ValueHolder.ValueFailureReason.VALUE_MISMATCH, PROTOCOL_NAME)
                );
            }
        }
        return result;
    }

    private void onClientMessage(AttributeRef protocolRef, String msg) {
        List<Pair<AttributeRef, Consumer<String>>> consumers = protocolMessageConsumers.get(protocolRef);
        if (consumers != null) {
            consumers.forEach(c -> {
                if (c.value != null) {
                    c.value.accept(msg);
                }
            });
        }
    }

    protected void onClientConnectionStatusChanged(AttributeRef protocolRef, ConnectionStatus connectionStatus) {
        updateStatus(protocolRef, connectionStatus);

        if (connectionStatus == ConnectionStatus.CONNECTED) {
            // Look for any subscriptions that need to be processed
            List<Runnable> connectedTasks;

            synchronized (protocolConnectedTasks) {
                connectedTasks = protocolConnectedTasks.remove(protocolRef);
            }
            if (connectedTasks != null) {
                // Execute after a delay to ensure connection is properly initialised
                executorService.schedule(() -> connectedTasks.forEach(Runnable::run), 500);
            }
        }
    }

    protected void doSubscriptions(WebsocketClient websocketClient, MultivaluedMap<String, String> headers, WebsocketSubscription[] subscriptions) {
        LOG.info("Executing subscriptions for websocket: " + websocketClient.getSocketAddressString());

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
            subscription -> processSubscription(websocketClient, finalHeaders, subscription)
        );
    }

    protected void processSubscription(WebsocketClient websocketClient, MultivaluedMap<String, String> headers, WebsocketSubscription subscription) {
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
            if (response.getStatusInfo().getFamily() != Response.Status.Family.SUCCESSFUL) {
                LOG.warning("WebsocketHttpSubscription returned an un-successful response code: " + response.getStatus());
            }
        } else {
            websocketClient.sendMessage(subscription.body);
        }
    }

    protected Consumer<String> createAttributeWebsocketMessageConsumer(AttributeRef attributeRef, ValueFilter[] filters, StringPredicate matchPredicate) {

        return message -> {
            if (!TextUtil.isNullOrEmpty(message)) {
                StringValue stringValue = Values.create(message);
                Value val = assetService.applyValueFilters(stringValue, filters);
                if (val != null) {
                    if (StringPredicate.asPredicate(matchPredicate).test(val.toString())) {
                        LOG.fine("Websocket message matches attribute so writing message to the attribute: " + attributeRef);
                        updateLinkedAttribute(new AttributeState(attributeRef, stringValue));
                    }
                }
            }
        };
    }
}
