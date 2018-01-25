/*
 * Copyright 2016, OpenRemote Inc.
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
package org.openremote.manager.agent;

import com.vividsolutions.jts.geom.Point;
import org.apache.camel.builder.RouteBuilder;
import org.openremote.agent.protocol.Protocol;
import org.openremote.agent.protocol.ProtocolAssetService;
import org.openremote.container.Container;
import org.openremote.container.ContainerService;
import org.openremote.container.message.MessageBrokerService;
import org.openremote.container.message.MessageBrokerSetupService;
import org.openremote.container.persistence.PersistenceEvent;
import org.openremote.container.timer.TimerService;
import org.openremote.container.web.WebService;
import org.openremote.manager.asset.*;
import org.openremote.manager.event.ClientEventService;
import org.openremote.manager.security.ManagerIdentityService;
import org.openremote.model.asset.*;
import org.openremote.model.asset.agent.*;
import org.openremote.model.attribute.AttributeEvent;
import org.openremote.model.attribute.AttributeEvent.Source;
import org.openremote.model.attribute.AttributeRef;
import org.openremote.model.event.shared.TenantFilter;
import org.openremote.model.security.ClientRole;
import org.openremote.model.util.Pair;
import org.openremote.model.value.ObjectValue;

import javax.persistence.EntityManager;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.openremote.agent.protocol.Protocol.ACTUATOR_TOPIC;
import static org.openremote.agent.protocol.Protocol.SENSOR_QUEUE;
import static org.openremote.container.persistence.PersistenceEvent.PERSISTENCE_TOPIC;
import static org.openremote.manager.asset.AssetProcessingService.ASSET_QUEUE;
import static org.openremote.manager.asset.AssetUpdateProcessor.isPersistenceEventForAssetType;
import static org.openremote.manager.asset.AssetUpdateProcessor.isPersistenceEventForEntityType;
import static org.openremote.model.AbstractValueTimestampHolder.VALUE_TIMESTAMP_FIELD_NAME;
import static org.openremote.model.asset.AssetAttribute.attributesFromJson;
import static org.openremote.model.asset.AssetAttribute.getAddedOrModifiedAttributes;
import static org.openremote.model.asset.AssetType.AGENT;
import static org.openremote.model.asset.agent.AgentLink.getAgentLink;
import static org.openremote.model.asset.agent.ConnectionStatus.*;
import static org.openremote.model.attribute.AttributeEvent.HEADER_SOURCE;
import static org.openremote.model.attribute.AttributeEvent.Source.SENSOR;
import static org.openremote.model.util.TextUtil.isNullOrEmpty;
import static org.openremote.model.util.TextUtil.isValidURN;

/**
 * Handles life cycle and communication with {@link Protocol}s.
 * <p>
 * Finds all {@link AssetType#AGENT} assets and manages their {@link ProtocolConfiguration}s.
 */
public class AgentService extends RouteBuilder implements ContainerService, AssetUpdateProcessor, ProtocolAssetService {

    private static final Logger LOG = Logger.getLogger(AgentService.class.getName());

    protected TimerService timerService;
    protected ManagerIdentityService identityService;
    protected AssetProcessingService assetProcessingService;
    protected AssetStorageService assetStorageService;
    protected MessageBrokerService messageBrokerService;
    protected ClientEventService clientEventService;
    protected final Map<AttributeRef, Pair<AssetAttribute, ConnectionStatus>> protocolConfigurations = new HashMap<>();
    protected final Map<String, Protocol> protocols = new HashMap<>();
    protected final List<AttributeRef> linkedAttributes = new ArrayList<>();
    protected LocalAgentConnector localAgentConnector;
    protected Map<String, Asset> agentMap;

    @Override
    public void init(Container container) throws Exception {
        timerService = container.getService(TimerService.class);
        identityService = container.getService(ManagerIdentityService.class);
        assetProcessingService = container.getService(AssetProcessingService.class);
        assetStorageService = container.getService(AssetStorageService.class);
        messageBrokerService = container.getService(MessageBrokerService.class);
        clientEventService = container.getService(ClientEventService.class);
        localAgentConnector = new LocalAgentConnector(this);

        clientEventService.addSubscriptionAuthorizer((auth, subscription) ->
            subscription.isEventType(AgentStatusEvent.class)
                && identityService.getIdentityProvider()
                .canSubscribeWith(auth, subscription.getFilter() instanceof TenantFilter
                    ? ((TenantFilter) subscription.getFilter())
                    : null, ClientRole.READ_ASSETS));

        container.getService(WebService.class).getApiSingletons().add(
            new AgentResourceImpl(
                container.getService(TimerService.class),
                container.getService(ManagerIdentityService.class),
                assetStorageService,
                this)
        );
    }

    @Override
    public void start(Container container) throws Exception {
        container.getService(MessageBrokerSetupService.class).getContext().addRoutes(this);

        // Load all protocol instances and fail hard and fast when a duplicate is found
        Collection<Protocol> discoveredProtocols = container.getServices(Protocol.class);

        discoveredProtocols
            .forEach(
                discoveredProtocol -> {
                    if (isNullOrEmpty(discoveredProtocol.getProtocolName())
                        || !isValidURN(discoveredProtocol.getProtocolName()))
                        throw new IllegalStateException(
                            "Protocol name is not a valid URN: " + discoveredProtocol.getClass()
                        );
                    if (protocols.containsKey(discoveredProtocol.getProtocolName()))
                        throw new IllegalStateException(
                            "A protocol with the name '" + discoveredProtocol.getProtocolName()
                                + "' has already been loaded: " + discoveredProtocol.getClass()
                        );
                    protocols.put(discoveredProtocol.getProtocolName(), discoveredProtocol);
                }
            );

        Collection<Asset> agents = getAgents().values();
        LOG.fine("Deploy all agents in all realms: " + agents.size());

        /// For all agents, go through their protocol configurations and find
        // assets that are linked to them, to create the binding on startup
        for (Asset agent : agents) {
            agent.getAttributesStream()
                .filter(ProtocolConfiguration::isProtocolConfiguration)
                .forEach(this::linkProtocolConfiguration);
        }
    }

    @Override
    public void stop(Container container) throws Exception {
        /* TODO Accessing the database while shutting down is not a good thing, it will hang if the JDBC pool is shutting down
        synchronized (protocolConfigurations) {
            new ArrayList<>(protocolConfigurations.values())
                .forEach(protocolConfigAndConsumer -> unlinkProtocolConfiguration(protocolConfigAndConsumer.key));
        }
        */
    }

    @Override
    public void configure() throws Exception {
        from(PERSISTENCE_TOPIC)
            .routeId("AgentPersistenceChanges")
            .filter(isPersistenceEventForEntityType(Asset.class))
            .process(exchange -> {
                PersistenceEvent persistenceEvent = exchange.getIn().getBody(PersistenceEvent.class);
                Asset asset = (Asset) persistenceEvent.getEntity();
                if (isPersistenceEventForAssetType(AGENT).matches(exchange)) {
                    processAgentChange(asset, persistenceEvent);
                } else {
                    processAssetChange(asset, persistenceEvent);
                }
            });

        // A protocol wants to write a new sensor value
        from(SENSOR_QUEUE)
            .routeId("FromSensorUpdates")
            .filter(body().isInstanceOf(AttributeEvent.class))
            .setHeader(HEADER_SOURCE, () -> SENSOR)
            .to(ASSET_QUEUE);
    }

    /**
     * This should only be called by protocol implementations to request an update to
     * one of their own protocol configuration attributes.
     */
    @Override
    public void updateProtocolConfiguration(AssetAttribute protocolConfiguration) {
        if (protocolConfiguration == null || !protocolConfiguration.getReference().isPresent()) {
            LOG.warning("Cannot update invalid: " + protocolConfiguration);
            return;
        }

        AttributeRef protocolRef = protocolConfiguration.getReference().get();
        ServerAsset agent = assetStorageService.find(protocolRef.getEntityId(), true);
        if (agent == null || agent.getWellKnownType() != AssetType.AGENT || !agent.hasAttribute(protocolRef.getAttributeName())) {
            LOG.warning("Protocol configuration doesn't belong to a valid agent: " + protocolConfiguration);
            return;
        }

        // Check protocol configuration has changed
        @SuppressWarnings("ConstantConditions")
        AssetAttribute oldProtocolConfiguration = agent.getAttribute(protocolRef.getAttributeName()).get();
        if (oldProtocolConfiguration.equals(protocolConfiguration)) {
            // Protocol configuration hasn't changed so nothing to do here
            return;
        }

        agent.replaceAttribute(protocolConfiguration);
        LOG.fine("Updating agent protocol configuration: " + protocolRef);
        assetStorageService.merge(agent);
    }

    @Override
    public void updateAssetLocation(String assetId, Point location) {
        ServerAsset asset = assetStorageService.find(assetId, true);
        if (asset == null) {
            LOG.warning("Asset with requested ID doesn't exist: " + assetId);
            return;
        }

        asset.setLocation(location);
        LOG.fine("Updating asset location '" + assetId + "': " + Arrays.toString(asset.getCoordinates()));
        assetStorageService.merge(asset);
    }

    @Override
    public Asset mergeAsset(Asset asset) {
        Objects.requireNonNull(asset.getId());
        Objects.requireNonNull(asset.getParentId());
        return mergeAsset(asset, null);
    }

    @Override
    public Asset mergeAsset(Asset asset, MergeOptions options) {
        Objects.requireNonNull(asset.getId());
        Objects.requireNonNull(asset.getParentId());

        ServerAsset updatedAsset = ServerAsset.map(asset, new ServerAsset());
        // Use the unique identifier provided by the protocol, it manages its own identifier space
        updatedAsset.setId(asset.getId());

        if (options != null && (options.getIgnoredAttributeNames() != null || options.getIgnoredAttributeKeys() != null)) {
            ServerAsset existingAsset = assetStorageService.find(updatedAsset.getId(), true);
            if (existingAsset != null) {
                // Check if any attributes except the ignored ones were modified
                List<AssetAttribute> existingAttributes = existingAsset.getAttributesList();
                List<AssetAttribute> updatedAttributes = updatedAsset.getAttributesList();

                List<AssetAttribute> addedOrModifiedAttributes = getAddedOrModifiedAttributes(
                    existingAttributes, updatedAttributes, options.getIgnoredAttributeNames(), options.getIgnoredAttributeKeys()
                ).collect(Collectors.toList());

                List<AssetAttribute> removedAttributes = getAddedOrModifiedAttributes(
                    updatedAttributes, existingAttributes, options.getIgnoredAttributeNames(), options.getIgnoredAttributeKeys()
                ).collect(Collectors.toList());

                if (addedOrModifiedAttributes.isEmpty() && removedAttributes.isEmpty()) {
                    LOG.finest("Skipping merge, protocol-provided asset unchanged (excluding ignored attribute names/keys): " + asset);
                    return existingAsset;
                }
            }
        }

        LOG.fine("Merging (and overriding existing older version of) with protocol-provided: " + asset);
        if (options != null && options.getAssignToUserName() != null) {
            return assetStorageService.merge(updatedAsset, true, options.getAssignToUserName());
        } else {
            return assetStorageService.merge(updatedAsset, true);
        }
    }

    @Override
    public boolean deleteAsset(String assetId) {
        LOG.fine("Deleting protocol-provided: " + assetId);
        return assetStorageService.delete(assetId);
    }

    @Override
    public void sendAttributeEvent(AttributeEvent attributeEvent) {
        assetProcessingService.sendAttributeEvent(attributeEvent);
    }

    /**
     * Looks for new, modified and obsolete protocol configurations and links / unlinks any associated attributes
     */
    protected void processAgentChange(Asset agent, PersistenceEvent persistenceEvent) {
        LOG.finest("Processing agent persistence event: " + persistenceEvent.getCause());

        switch (persistenceEvent.getCause()) {
            case INSERT:
                addReplaceAgent(agent);
                agent.getAttributesStream()
                    .filter(ProtocolConfiguration::isProtocolConfiguration)
                    .forEach(this::linkProtocolConfiguration);

                break;
            case UPDATE:
                addReplaceAgent(agent);
                // Check if any protocol config attributes have been added/removed or modified
                int attributesIndex = Arrays.asList(persistenceEvent.getPropertyNames()).indexOf("attributes");
                if (attributesIndex < 0) {
                    return;
                }

                // Attributes have possibly changed so need to compare old and new state to determine
                // which protocol configs are affected
                List<AssetAttribute> oldProtocolConfigurations =
                    attributesFromJson(
                        (ObjectValue) persistenceEvent.getPreviousState()[attributesIndex],
                        agent.getId()
                    )
                        .filter(ProtocolConfiguration::isProtocolConfiguration)
                        .collect(Collectors.toList());

                List<AssetAttribute> newProtocolConfigurations =
                    attributesFromJson(
                        (ObjectValue) persistenceEvent.getCurrentState()[attributesIndex],
                        agent.getId()
                    )
                        .filter(ProtocolConfiguration::isProtocolConfiguration)
                        .collect(Collectors.toList());

                // Compare protocol configurations by JSON value
                // Unlink protocols that are in oldConfigs but not in newConfigs
                oldProtocolConfigurations
                    .stream()
                    .filter(oldProtocolAttribute -> newProtocolConfigurations
                        .stream()
                        .noneMatch(oldProtocolAttribute::equals
                        )
                    )
                    .forEach(this::unlinkProtocolConfiguration);

                // Link protocols that are in newConfigs but not in oldConfigs
                newProtocolConfigurations
                    .stream()
                    .filter(newProtocolAttribute -> oldProtocolConfigurations
                        .stream()
                        .noneMatch(newProtocolAttribute::equals
                        )
                    ).forEach(this::linkProtocolConfiguration);

                break;
            case DELETE:
                removeAgent(agent);
                // Unlink any attributes that have an agent link to this agent
                agent.getAttributesStream()
                    .filter(ProtocolConfiguration::isProtocolConfiguration)
                    .forEach(this::unlinkProtocolConfiguration);
                break;
        }
    }

    /**
     * Looks for new, modified and obsolete AGENT_LINK attributes and links / unlinks them
     * with the protocol
     */
    protected void processAssetChange(Asset asset, PersistenceEvent persistenceEvent) {
        LOG.finest("Processing asset persistence event: " + persistenceEvent.getCause());

        switch (persistenceEvent.getCause()) {
            case INSERT:

                // Asset insert persistence events can be fired before the agent insert persistence event
                // so need to check that all protocol configs exist - any that don't we will exclude here
                // and handle in agent insert

                // If an agent insert just occurred then we will end up trying to link the attribute again
                // so we keep track of linked attributes to avoid this

                // Link any AGENT_LINK attributes to their referenced protocol
                Map<AssetAttribute, List<AssetAttribute>> groupedAgentLinksAttributes =
                    getGroupedAgentLinkAttributes(
                        asset.getAttributesStream(),
                        attribute -> true,
                        attribute -> LOG.warning("Linked protocol configuration not found: " + attribute)
                    );
                groupedAgentLinksAttributes.forEach(this::linkAttributes);

                break;
            case UPDATE:
                List<String> propertyNames = Arrays.asList(persistenceEvent.getPropertyNames());

                // Check if attributes of the asset have been modified
                int attributesIndex = propertyNames.indexOf("attributes");
                if (attributesIndex < 0) {
                    return;
                }

                // Attributes have possibly changed so need to compare old and new state to determine any changes to
                // AGENT_LINK attributes
                List<AssetAttribute> oldAgentLinkedAttributes =
                    attributesFromJson(
                        (ObjectValue) persistenceEvent.getPreviousState()[attributesIndex],
                        asset.getId()
                    )
                        .filter(AgentLink::hasAgentLink)
                        .collect(Collectors.toList());

                List<AssetAttribute> newAgentLinkedAttributes =
                    attributesFromJson(
                        (ObjectValue) persistenceEvent.getCurrentState()[attributesIndex],
                        asset.getId()
                    )
                        .filter(AgentLink::hasAgentLink)
                        .collect(Collectors.toList());

                // Unlink thing attributes that are in old but not in new
                getGroupedAgentLinkAttributes(
                    getAddedOrModifiedAttributes(newAgentLinkedAttributes, oldAgentLinkedAttributes, key -> key.equals(VALUE_TIMESTAMP_FIELD_NAME)),
                    attribute -> true
                ).forEach(this::unlinkAttributes);

                // Link thing attributes that are in new but not in old
                getGroupedAgentLinkAttributes(
                    getAddedOrModifiedAttributes(oldAgentLinkedAttributes, newAgentLinkedAttributes, key -> key.equals(VALUE_TIMESTAMP_FIELD_NAME)),
                    attribute -> true,
                    attribute -> LOG.warning("Linked protocol configuration not found: " + attribute)
                ).forEach(this::linkAttributes);

                break;
            case DELETE: {

                // Unlink any AGENT_LINK attributes from the referenced protocol
                Map<AssetAttribute, List<AssetAttribute>> groupedAgentLinkAndProtocolAttributes =
                    getGroupedAgentLinkAttributes(asset.getAttributesStream(), attribute -> true);
                groupedAgentLinkAndProtocolAttributes
                    .forEach(
                        this::unlinkAttributes
                    );
                break;
            }
        }
    }

    protected void linkProtocolConfiguration(AssetAttribute protocolConfiguration) {
        AttributeRef protocolAttributeRef = protocolConfiguration.getReferenceOrThrow();
        Protocol protocol = getProtocol(protocolConfiguration);

        if (protocol == null) {
            LOG.warning("Cannot find protocol that attribute is linked to: " + protocolAttributeRef);
            return;
        }

        synchronized (protocolConfigurations) {
            // Create a consumer callback for deployment status updates
            Consumer<ConnectionStatus> deploymentStatusConsumer = status ->
                publishProtocolConnectionStatus(protocolAttributeRef, status);

            // Store the info
            protocolConfigurations.put(
                protocolAttributeRef,
                new Pair<>(protocolConfiguration, null)
            );

            // Set status to linking
            publishProtocolConnectionStatus(protocolAttributeRef, CONNECTING);

            // Link the protocol configuration to the protocol
            try {
                protocol.linkProtocolConfiguration(protocolConfiguration, deploymentStatusConsumer);
            } catch (Exception e) {
                LOG.log(Level.SEVERE, "Protocol threw an exception during protocol configuration linking", e);
                // Set status to error
                publishProtocolConnectionStatus(protocolAttributeRef, ERROR_CONFIGURATION);
            }

            // Check protocol status and only continue linking attributes if not in error state
            ConnectionStatus connectionStatus = getProtocolConnectionStatus(protocolAttributeRef);
            if (connectionStatus == ERROR_CONFIGURATION || connectionStatus == ERROR) {
                LOG.warning("Protocol connection status is showing error so not linking attributes: "
                    + protocolConfiguration);
                return;
            }
        }

        // Get all assets that have attributes that use this protocol configuration
        List<ServerAsset> assets = assetStorageService.findAll(
            new AssetQuery()
                .select(new AssetQuery.Select(AssetQuery.Include.ALL))
                .attributeMeta(
                    new AssetQuery.AttributeRefPredicate(
                        AssetMeta.AGENT_LINK,
                        protocolAttributeRef.getEntityId(),
                        protocolAttributeRef.getAttributeName()
                    )
                )
        );

        assets.forEach(
            asset ->
                getGroupedAgentLinkAttributes(
                    asset.getAttributesStream(),
                    assetAttribute -> getAgentLink(assetAttribute)
                        .map(attributeRef -> attributeRef.equals(protocolAttributeRef))
                        .orElse(false),
                    attribute -> LOG.warning("Linked protocol configuration not found: " + attribute)
                ).forEach(this::linkAttributes)
        );
    }

    protected void unlinkProtocolConfiguration(AssetAttribute protocolConfiguration) {
        AttributeRef protocolAttributeRef = protocolConfiguration.getReferenceOrThrow();

        // Get all assets that have attributes that use this protocol configuration
        List<ServerAsset> assets = assetStorageService.findAll(
            new AssetQuery()
                .select(new AssetQuery.Select(AssetQuery.Include.ALL))
                .attributeMeta(
                    new AssetQuery.AttributeRefPredicate(
                        AssetMeta.AGENT_LINK,
                        protocolAttributeRef.getEntityId(),
                        protocolAttributeRef.getAttributeName()
                    )
                )
        );

        assets.forEach(
            asset ->
                getGroupedAgentLinkAttributes(
                    asset.getAttributesStream(),
                    assetAttribute -> getAgentLink(assetAttribute)
                        .map(attributeRef -> attributeRef.equals(protocolAttributeRef))
                        .orElse(false)
                ).forEach(this::unlinkAttributes)
        );

        synchronized (protocolConfigurations) {
            Protocol protocol = getProtocol(protocolConfiguration);

            // Unlink the protocol configuration from the protocol
            try {
                protocol.unlinkProtocolConfiguration(protocolConfiguration);
            } catch (Exception e) {
                LOG.log(Level.SEVERE, "Protocol threw an exception during protocol configuration unlinking", e);
            }

            // Set status to disconnected
            publishProtocolConnectionStatus(protocolAttributeRef, DISCONNECTED);
            protocolConfigurations.remove(protocolAttributeRef);
        }
    }

    protected void publishProtocolConnectionStatus(AttributeRef protocolRef, ConnectionStatus connectionStatus) {
        synchronized (protocolConfigurations) {
            Pair<AssetAttribute, ConnectionStatus> protocolDeploymentInfo = protocolConfigurations.get(protocolRef);
            if (protocolDeploymentInfo != null && protocolDeploymentInfo.value != connectionStatus) {
                LOG.info("Agent protocol status updated to " + connectionStatus + ": " + protocolRef);
                protocolDeploymentInfo.value = connectionStatus;

                // Notify clients
                clientEventService.publishEvent(
                    new AgentStatusEvent(
                        timerService.getCurrentTimeMillis(),
                        agentMap.get(protocolRef.getEntityId()).getRealmId(),
                        protocolRef,
                        connectionStatus
                    )
                );
            }
        }
    }

    public ConnectionStatus getProtocolConnectionStatus(AttributeRef protocolRef) {
        synchronized (protocolConfigurations) {
            return Optional.ofNullable(protocolConfigurations.get(protocolRef))
                .map(pair -> pair.value)
                .orElse(null);
        }
    }

    protected Protocol getProtocol(AssetAttribute protocolConfiguration) {
        return protocols.get(protocolConfiguration.getValueAsString().orElse(null));
    }

    protected void linkAttributes(AssetAttribute protocolConfiguration, Collection<AssetAttribute> attributes) {
        LOG.fine("Linking all attributes that use protocol attribute: " + protocolConfiguration);
        Protocol protocol = getProtocol(protocolConfiguration);

        if (protocol == null) {
            LOG.severe("Cannot link protocol attributes as protocol is null: " + protocolConfiguration);
            return;
        }

        synchronized (linkedAttributes) {
            attributes.removeIf(attr -> linkedAttributes.contains(attr.getReferenceOrThrow()));
            linkedAttributes.addAll(attributes.stream().map(AssetAttribute::getReferenceOrThrow).collect(Collectors.toList()));
        }

        try {
            LOG.finest("Linking protocol attributes to: " + protocol.getProtocolName());
            protocol.linkAttributes(attributes, protocolConfiguration);
        } catch (Exception ex) {
            LOG.log(Level.SEVERE, "Ignoring error on linking attributes to protocol: " + protocol.getProtocolName(), ex);
            // Update the status of this protocol configuration to error
            publishProtocolConnectionStatus(protocolConfiguration.getReferenceOrThrow(), ERROR);
        }
    }

    protected void unlinkAttributes(AssetAttribute protocolConfiguration, Collection<AssetAttribute> attributes) {
        LOG.fine("Unlinking all attributes that use protocol attribute: " + protocolConfiguration);
        Protocol protocol = getProtocol(protocolConfiguration);

        if (protocol == null) {
            LOG.severe("Cannot unlink protocol attributes as protocol is null: " + protocolConfiguration);
            return;
        }

        synchronized (linkedAttributes) {
            attributes.removeIf(attr -> !linkedAttributes.contains(attr.getReferenceOrThrow()));
            linkedAttributes.removeAll(attributes.stream().map(AssetAttribute::getReferenceOrThrow).collect(Collectors.toList()));
        }

        try {
            LOG.finest("Unlinking protocol attributes from: " + protocol.getProtocolName());
            protocol.unlinkAttributes(attributes, protocolConfiguration);
        } catch (Exception ex) {
            LOG.log(Level.SEVERE, "Ignoring error on unlinking attributes from protocol: " + protocol.getProtocolName(), ex);
            // Update the status of this protocol configuration to error
            publishProtocolConnectionStatus(protocolConfiguration.getReferenceOrThrow(), ERROR);
        }
    }

    /**
     * If this is an update from a sensor, or if the changed attribute is not linked to an agent's protocol
     * configuration, it's ignored.
     * <p>
     * Otherwise push the update to the attributes' linked protocol to handle and prevent any further
     * processing of this event by the processing chain. The protocol should raise sensor updates as
     * required (i.e. the protocol is responsible for synchronising state with the database).
     */
    @Override
    public boolean processAssetUpdate(EntityManager entityManager,
                                      ServerAsset asset,
                                      AssetAttribute attribute,
                                      Source source) throws AssetProcessingException {
        if (source == SENSOR) {
            return false;
        }

        return AgentLink.getAgentLink(attribute)
            .map(ref ->
                getProtocolConfiguration(ref)
                    .orElseThrow(() -> new AssetProcessingException(AssetProcessingException.Reason.INVALID_AGENT_LINK))
            )
            .map(protocolConfiguration -> {
                // Its' a send to actuator - push the update to the protocol
                attribute.getStateEvent().ifPresent(attributeEvent -> {
                    LOG.fine("Sending to actuator topic: " + attributeEvent);
                    messageBrokerService.getProducerTemplate().sendBodyAndHeader(
                        ACTUATOR_TOPIC,
                        attributeEvent,
                        Protocol.ACTUATOR_TOPIC_TARGET_PROTOCOL,
                        protocolConfiguration.getValueAsString().orElse("")
                    );
                });
                return true; // Processing complete, skip other processors
            })
            .orElse(false); // This is just a non protocol attribute so allow the processing to continue
    }

    /**
     * Gets all agent link attributes and their linked protocol configuration and groups them by Protocol Configuration
     */
    @SuppressWarnings("ConstantConditions")
    protected Map<AssetAttribute, List<AssetAttribute>> getGroupedAgentLinkAttributes(Stream<AssetAttribute> attributes,
                                                                                      Predicate<AssetAttribute> filter) {

        return getGroupedAgentLinkAttributes(attributes, filter, null);
    }

    @SuppressWarnings("ConstantConditions")
    protected Map<AssetAttribute, List<AssetAttribute>> getGroupedAgentLinkAttributes(Stream<AssetAttribute> attributes,
                                                                                      Predicate<AssetAttribute> filter,
                                                                                      Consumer<AssetAttribute> notFoundConsumer) {
        Map<AssetAttribute, List<AssetAttribute>> result = new HashMap<>();
        attributes
            .filter(AgentLink::hasAgentLink)
            .filter(filter)
            .map(attribute -> new Pair<>(attribute, getAgentLink(attribute)))
            .filter(pair -> pair.value.isPresent())
            .map(pair -> new Pair<>(pair.key, getProtocolConfiguration(pair.value.get())))
            .filter(pair -> {
                if (pair.value.isPresent()) {
                    return true;
                } else if (notFoundConsumer != null) {
                    notFoundConsumer.accept(pair.key);
                }
                return false;
            })
            .forEach(pair -> result.computeIfAbsent(pair.value.get(), newProtocolConfiguration -> new ArrayList<>())
                .add(pair.key)
            );
        return result;
    }

    public String toString() {
        return getClass().getSimpleName() + "{" +
            '}';
    }

    public Optional<AssetAttribute> getProtocolConfiguration(AttributeRef protocolRef) {
        synchronized (protocolConfigurations) {
            Pair<AssetAttribute, ConnectionStatus> deploymentStatusPair = protocolConfigurations.get(protocolRef);
            return deploymentStatusPair == null ? Optional.empty() : Optional.of(deploymentStatusPair.key);
        }
    }

    public Optional<AgentConnector> getAgentConnector(Asset agent) {
        if (agent == null || agent.getWellKnownType() != AGENT) {
            return Optional.empty();
        }

        return !Agent.hasUrl(agent) ? Optional.of(localAgentConnector) : Optional.empty();
    }

    protected synchronized void addReplaceAgent(Asset agent) {
        getAgents().put(agent.getId(), agent);
    }

    protected synchronized void removeAgent(Asset agent) {
        getAgents().remove(agent.getId());
    }

    public synchronized Map<String, Asset> getAgents() {
        if (agentMap == null) {
            agentMap = assetStorageService.findAll(new AssetQuery()
                .select(new AssetQuery.Select(AssetQuery.Include.ALL))
                .type(AssetType.AGENT))
                .stream()
                .collect(Collectors.toMap(Asset::getId, agent -> agent));
        }

        return agentMap;
    }
}