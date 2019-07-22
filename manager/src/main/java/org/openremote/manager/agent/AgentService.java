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

import com.fasterxml.jackson.databind.JsonNode;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.ParseContext;
import com.jayway.jsonpath.spi.json.JacksonJsonNodeJsonProvider;
import com.jayway.jsonpath.spi.mapper.JacksonMappingProvider;
import org.apache.camel.builder.RouteBuilder;
import org.openremote.agent.protocol.Protocol;
import org.openremote.agent.protocol.ProtocolAssetService;
import org.openremote.container.Container;
import org.openremote.container.ContainerService;
import org.openremote.container.message.MessageBrokerService;
import org.openremote.container.message.MessageBrokerSetupService;
import org.openremote.container.persistence.PersistenceEvent;
import org.openremote.container.timer.TimerService;
import org.openremote.manager.asset.AssetProcessingException;
import org.openremote.manager.asset.AssetProcessingService;
import org.openremote.manager.asset.AssetStorageService;
import org.openremote.manager.asset.AssetUpdateProcessor;
import org.openremote.manager.event.ClientEventService;
import org.openremote.manager.security.ManagerIdentityService;
import org.openremote.manager.web.ManagerWebService;
import org.openremote.model.asset.Asset;
import org.openremote.model.asset.AssetAttribute;
import org.openremote.model.asset.AssetType;
import org.openremote.model.asset.agent.*;
import org.openremote.model.attribute.AttributeEvent;
import org.openremote.model.attribute.AttributeEvent.Source;
import org.openremote.model.attribute.AttributeRef;
import org.openremote.model.attribute.MetaItemType;
import org.openremote.model.event.shared.TenantFilter;
import org.openremote.model.query.AssetQuery;
import org.openremote.model.query.filter.AttributeRefPredicate;
import org.openremote.model.security.ClientRole;
import org.openremote.model.util.Pair;
import org.openremote.model.util.TextUtil;
import org.openremote.model.value.*;

import javax.persistence.EntityManager;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.openremote.agent.protocol.Protocol.ACTUATOR_TOPIC;
import static org.openremote.agent.protocol.Protocol.SENSOR_QUEUE;
import static org.openremote.container.concurrent.GlobalLock.withLock;
import static org.openremote.container.concurrent.GlobalLock.withLockReturning;
import static org.openremote.container.persistence.PersistenceEvent.*;
import static org.openremote.manager.asset.AssetProcessingService.ASSET_QUEUE;
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
    protected final Map<AttributeRef, List<AttributeRef>> linkedAttributes = new HashMap<>();
    protected LocalAgentConnector localAgentConnector;
    protected Map<String, Asset> agentMap;
    protected ParseContext jsonPathParser;

    /**
     * It's important that {@link Protocol}s have a lower priority than this service so they are fully initialized
     * before this service is started.
     */
    @Override
    public int getPriority() {
        return ContainerService.DEFAULT_PRIORITY;
    }

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

        container.getService(ManagerWebService.class).getApiSingletons().add(
            new AgentResourceImpl(
                container.getService(TimerService.class),
                container.getService(ManagerIdentityService.class),
                assetStorageService,
                this)
        );

        jsonPathParser = JsonPath.using(
            new Configuration.ConfigurationBuilder()
                .jsonProvider(new JacksonJsonNodeJsonProvider())
                .mappingProvider(new JacksonMappingProvider())
                .build()
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
            linkProtocolConfigurations(agent.getAttributesStream()
                .filter(ProtocolConfiguration::isProtocolConfiguration)
            );
        }
    }

    @Override
    public void stop(Container container) throws Exception {
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
        Asset agent = assetStorageService.find(protocolRef.getEntityId(), true);
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
    public Asset mergeAsset(Asset asset) {
        Objects.requireNonNull(asset.getId());
        Objects.requireNonNull(asset.getParentId());
        return mergeAsset(asset, null);
    }

    @Override
    public Asset mergeAsset(Asset asset, MergeOptions options) {
        Objects.requireNonNull(asset.getId());
        Objects.requireNonNull(asset.getParentId());

        Asset updatedAsset = Asset.map(asset, new Asset());
        // Use the unique identifier provided by the protocol, it manages its own identifier space
        updatedAsset.setId(asset.getId());

        if (options != null && (options.getIgnoredAttributeNames() != null || options.getIgnoredAttributeKeys() != null)) {
            Asset existingAsset = assetStorageService.find(updatedAsset.getId(), true);
            if (existingAsset != null) {
                // Check if any attributes except the ignored ones were modified
                List<AssetAttribute> existingAttributes = existingAsset.getAttributesList();
                List<AssetAttribute> updatedAttributes = updatedAsset.getAttributesList();

                List<AssetAttribute> addedOrModifiedAttributes = getAddedOrModifiedAttributes(
                    existingAttributes, updatedAttributes, options.getAttributeNamesToEvaluate(), options.getIgnoredAttributeNames(), options.getIgnoredAttributeKeys()
                ).collect(Collectors.toList());

                List<AssetAttribute> removedAttributes = getAddedOrModifiedAttributes(
                    updatedAttributes, existingAttributes, options.getAttributeNamesToEvaluate(), options.getIgnoredAttributeNames(), options.getIgnoredAttributeKeys()
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

    @Override
    public Asset getAgent(AssetAttribute protocolConfiguration) {
        return getAgents().getOrDefault(protocolConfiguration.getReferenceOrThrow().getEntityId(), null);
    }

    /**
     * Looks for new, modified and obsolete protocol configurations and links / unlinks any associated attributes
     */
    protected void processAgentChange(Asset agent, PersistenceEvent persistenceEvent) {
        LOG.finest("Processing agent persistence event: " + persistenceEvent.getCause());

        switch (persistenceEvent.getCause()) {
            case CREATE:
                addReplaceAgent(agent);
                linkProtocolConfigurations(
                    agent.getAttributesStream().filter(ProtocolConfiguration::isProtocolConfiguration)
                );

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
                unlinkProtocolConfigurations(oldProtocolConfigurations
                    .stream()
                    .filter(oldProtocolAttribute -> newProtocolConfigurations
                        .stream()
                        .noneMatch(oldProtocolAttribute::equals)
                    )
                );

                // Link protocols that are in newConfigs but not in oldConfigs
                linkProtocolConfigurations(newProtocolConfigurations
                    .stream()
                    .filter(newProtocolAttribute -> oldProtocolConfigurations
                        .stream()
                        .noneMatch(newProtocolAttribute::equals)
                    )
                );

                break;
            case DELETE:
                removeAgent(agent);
                // Unlink any attributes that have an agent link to this agent
                unlinkProtocolConfigurations(agent.getAttributesStream()
                    .filter(ProtocolConfiguration::isProtocolConfiguration)
                );
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
            case CREATE:

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

    protected void linkProtocolConfigurations(Stream<AssetAttribute> configurations) {
        withLock(getClass().getSimpleName() + "::linkProtocolConfigurations", () -> configurations.forEach(configuration -> {
            AttributeRef protocolAttributeRef = configuration.getReferenceOrThrow();
            Protocol protocol = getProtocol(configuration);

            if (protocol == null) {
                LOG.warning("Cannot find protocol that attribute is linked to: " + protocolAttributeRef);
                return;
            }

            // Store the info
            protocolConfigurations.put(protocolAttributeRef, new Pair<>(configuration, null));

            // Create a consumer callback for protocol status updates
            Consumer<ConnectionStatus> deploymentStatusConsumer = status ->
                publishProtocolConnectionStatus(protocolAttributeRef, status);

            // Set status to WAITING (we don't know what the protocol's status will be after linking configuration)
            publishProtocolConnectionStatus(protocolAttributeRef, WAITING);

            // Link the protocol configuration to the protocol
            try {
                protocol.linkProtocolConfiguration(configuration, deploymentStatusConsumer);
            } catch (Exception e) {
                LOG.log(Level.SEVERE, "Protocol threw an exception during protocol configuration linking", e);
                // Set status to error
                publishProtocolConnectionStatus(protocolAttributeRef, ERROR_CONFIGURATION);
            }

            // Check protocol status and only continue linking attributes if not in error state
            ConnectionStatus connectionStatus = getProtocolConnectionStatus(protocolAttributeRef);
            if (connectionStatus == ERROR_CONFIGURATION || connectionStatus == ERROR) {
                LOG.warning("Protocol connection status is showing error so not linking attributes: " + configuration);
                return;
            }

            // Get all assets that have attributes that use this protocol configuration
            List<Asset> assets = assetStorageService.findAll(
                new AssetQuery()
                    .select(new AssetQuery.Select(AssetQuery.Include.ALL))
                    .attributeMeta(
                        new AttributeRefPredicate(
                            MetaItemType.AGENT_LINK,
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
        }));
    }

    protected void unlinkProtocolConfigurations(Stream<AssetAttribute> configurations) {
        withLock(getClass().getSimpleName() + "::unlinkProtocolConfigurations", () -> configurations.forEach(configuration -> {
            AttributeRef protocolAttributeRef = configuration.getReferenceOrThrow();

            // Get all assets that have attributes that use this protocol configuration
            List<Asset> assets = assetStorageService.findAll(
                new AssetQuery()
                    .select(new AssetQuery.Select(AssetQuery.Include.ALL))
                    .attributeMeta(
                        new AttributeRefPredicate(
                            MetaItemType.AGENT_LINK,
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

            Protocol protocol = getProtocol(configuration);

            // Unlink the protocol configuration from the protocol
            try {
                protocol.unlinkProtocolConfiguration(configuration);
            } catch (Exception e) {
                LOG.log(Level.SEVERE, "Protocol threw an exception during protocol configuration unlinking", e);
            }

            // Set status to disconnected
            publishProtocolConnectionStatus(protocolAttributeRef, DISCONNECTED);
            protocolConfigurations.remove(protocolAttributeRef);
        }));
    }

    protected void publishProtocolConnectionStatus(AttributeRef protocolRef, ConnectionStatus connectionStatus) {
        withLock(getClass().getSimpleName() + "::publishProtocolConnectionStatus", () -> {
            Pair<AssetAttribute, ConnectionStatus> protocolDeploymentInfo = protocolConfigurations.get(protocolRef);
            if (protocolDeploymentInfo != null && protocolDeploymentInfo.value != connectionStatus) {
                LOG.info("Agent protocol status updated to " + connectionStatus + ": " + protocolRef);
                protocolDeploymentInfo.value = connectionStatus;

                // Notify clients
                clientEventService.publishEvent(
                    new AgentStatusEvent(
                        timerService.getCurrentTimeMillis(),
                        agentMap.get(protocolRef.getEntityId()).getRealm(),
                        protocolRef,
                        connectionStatus
                    )
                );
            }
        });
    }

    public ConnectionStatus getProtocolConnectionStatus(AttributeRef protocolRef) {
        return withLockReturning(getClass().getSimpleName() + "::getProtocolConnectionStatus", () ->
            Optional.ofNullable(protocolConfigurations.get(protocolRef))
                .map(pair -> pair.value)
                .orElse(null));
    }

    protected Protocol getProtocol(AssetAttribute protocolConfiguration) {
        return protocols.get(protocolConfiguration.getValueAsString().orElse(null));
    }

    protected void linkAttributes(AssetAttribute protocolConfiguration, Collection<AssetAttribute> attributes) {
        withLock(getClass().getSimpleName() + "::linkAttributes", () -> {
            LOG.fine("Linking all attributes that use protocol attribute: " + protocolConfiguration);
            Protocol protocol = getProtocol(protocolConfiguration);

            if (protocol == null) {
                LOG.severe("Cannot link protocol attributes as protocol is null: " + protocolConfiguration);
                return;
            }

            attributes.removeIf(attr ->
                linkedAttributes.values().stream()
                    .anyMatch(linkedAttributes -> linkedAttributes.contains(attr.getReferenceOrThrow())));

            linkedAttributes.compute(
                protocolConfiguration.getReferenceOrThrow(),
                (protocolRef, linkedAttrs) -> {
                    if (linkedAttrs == null) {
                        linkedAttrs = new ArrayList<>(attributes.size());
                    }
                    linkedAttrs.addAll(attributes.stream().map(AssetAttribute::getReferenceOrThrow).collect(Collectors.toList()));
                    return linkedAttrs;
                });

            try {
                LOG.finest("Linking protocol attributes to: " + protocol.getProtocolName());
                protocol.linkAttributes(attributes, protocolConfiguration);
            } catch (Exception ex) {
                LOG.log(Level.SEVERE, "Ignoring error on linking attributes to protocol: " + protocol.getProtocolName(), ex);
                // Update the status of this protocol configuration to error
                publishProtocolConnectionStatus(protocolConfiguration.getReferenceOrThrow(), ERROR);
            }
        });
    }

    protected void unlinkAttributes(AssetAttribute protocolConfiguration, Collection<AssetAttribute> attributes) {
        withLock(getClass().getSimpleName() + "::unlinkAttributes", () -> {
            LOG.fine("Unlinking all attributes that use protocol attribute: " + protocolConfiguration);
            Protocol protocol = getProtocol(protocolConfiguration);

            if (protocol == null) {
                LOG.severe("Cannot unlink protocol attributes as protocol is null: " + protocolConfiguration);
                return;
            }

            linkedAttributes.computeIfPresent(
                protocolConfiguration.getReferenceOrThrow(),
                (protocolRef, linkedAttrs) -> {
                    linkedAttrs.removeAll(attributes.stream().map(AssetAttribute::getReferenceOrThrow).collect(Collectors.toList()));
                    return linkedAttrs.isEmpty() ? null : linkedAttrs;
                }
            );

            try {
                LOG.finest("Unlinking protocol attributes from: " + protocol.getProtocolName());
                protocol.unlinkAttributes(attributes, protocolConfiguration);
            } catch (Exception ex) {
                LOG.log(Level.SEVERE, "Ignoring error on unlinking attributes from protocol: " + protocol.getProtocolName(), ex);
                // Update the status of this protocol configuration to error
                publishProtocolConnectionStatus(protocolConfiguration.getReferenceOrThrow(), ERROR);
            }
        });
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
                                      Asset asset,
                                      AssetAttribute attribute,
                                      Source source) throws AssetProcessingException {
        if (source == SENSOR) {
            return false;
        }

        Boolean result = withLockReturning(getClass().getSimpleName() + "::processAssetUpdate", () ->
            AgentLink.getAgentLink(attribute)
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
                .orElse(false) // This is a regular attribute so allow the processing to continue
        );
        return result != null ? result : false;
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
        return withLockReturning(getClass().getSimpleName() + "::getProtocolConfiguration", () -> {
            Pair<AssetAttribute, ConnectionStatus> deploymentStatusPair = protocolConfigurations.get(protocolRef);
            return deploymentStatusPair == null ? Optional.empty() : Optional.of(deploymentStatusPair.key);
        });
    }

    public Optional<AgentConnector> getAgentConnector(Asset agent) {
        if (agent == null || agent.getWellKnownType() != AGENT) {
            return Optional.empty();
        }

        return !Agent.hasUrl(agent) ? Optional.of(localAgentConnector) : Optional.empty();
    }

    protected void addReplaceAgent(Asset agent) {
        // Fully load agent asset
        final Asset loadedAgent = assetStorageService.find(agent.getId(), true);
        withLock(getClass().getSimpleName() + "::addReplaceAgent", () -> getAgents().put(loadedAgent.getId(), loadedAgent));
    }

    protected void removeAgent(Asset agent) {
        withLock(getClass().getSimpleName() + "::removeAgent", () -> getAgents().remove(agent.getId()));
    }

    public Map<String, Asset> getAgents() {
        return withLockReturning(getClass().getSimpleName() + "::getAgents", () -> {
            if (agentMap == null) {
                agentMap = assetStorageService.findAll(new AssetQuery()
                    .select(new AssetQuery.Select(AssetQuery.Include.ALL))
                    .type(AssetType.AGENT))
                    .stream()
                    .collect(Collectors.toMap(Asset::getId, agent -> agent));
            }
            return agentMap;
        });
    }

    @SuppressWarnings("unchecked")
    @Override
    public Value applyValueFilters(Value value, ValueFilter... filters) {

        LOG.fine("Applying value filters to value...");

        for (ValueFilter filter : filters) {
            boolean filterOk = filter.getValueType() == Value.class || filter.getValueType() == value.getType().getModelType();

            if (!filterOk) {
                // Try and convert the value
                ValueType filterValueType = ValueType.fromModelType(filter.getValueType());
                if (filterValueType == null) {
                    LOG.fine("Value filter type unknown: " + filter.getValueType().getName());
                    value = null;
                } else {
                    Optional<Value> val = Values.convert(value, filterValueType);
                    if (!val.isPresent()) {
                        LOG.fine("Value filter type '" + filter.getValueType().getName()
                            + "' is not compatible with actual value type '" + value.getType().getModelType().getName()
                            + "': " + filter.getClass().getName());
                    } else {
                        filterOk = true;
                    }
                    value = val.orElse(null);
                }
            }

            if (filterOk) {
                try {
                    Protocol.LOG.finest("Applying value filter: " + filter.getClass().getName());
                    if (filter instanceof RegexValueFilter) {
                        value = applyRegexFilter((StringValue)value, (RegexValueFilter)filter);
                    } else if (filter instanceof SubStringValueFilter) {
                        value = applySubstringFilter((StringValue)value, (SubStringValueFilter)filter);
                    } else if (filter instanceof JsonPathFilter) {
                        value = applyJsonPathFilter(value, (JsonPathFilter)filter);
                    } else {
                        throw new UnsupportedOperationException("Unsupported filter: " + filter);
                    }
                } catch (Exception e) {
                    LOG.log(
                        Level.SEVERE,
                        "Value filter threw an exception during processing: "
                            + filter.getClass().getName(),
                        e);
                    value = null;
                }
            }

            if (value == null) {
                break;
            }
        }

        return value;
    }

    protected Value applySubstringFilter(StringValue value, SubStringValueFilter filter) {
        if (value == null) {
            return null;
        }

        String result = null;

        try {
            if (filter.endIndex != null) {
                result = value.getString().substring(filter.beginIndex, filter.endIndex);
            } else {
                result = value.getString().substring(filter.beginIndex);
            }
        } catch (IndexOutOfBoundsException ignored) {}

        return result == null ? null : Values.create(result);
    }

    protected Value applyRegexFilter(StringValue value, RegexValueFilter filter) {
        if (value == null || filter.pattern == null) {
            return null;
        }

        String filteredStr = null;
        Matcher matcher = filter.pattern.matcher(value.getString());
        int matchIndex = 0;
        boolean matched = matcher.find();

        while(matched && matchIndex<filter.matchIndex) {
            matched = matcher.find();
            matchIndex++;
        }

        if (matched) {
            if (filter.matchGroup <= matcher.groupCount()) {
                filteredStr = matcher.group(filter.matchGroup);
            }
        }

        return filteredStr == null ? null : Values.create(filteredStr);
    }

    protected Value applyJsonPathFilter(Value value, JsonPathFilter filter) {
        if (value == null || TextUtil.isNullOrEmpty(filter.path)) {
            return null;
        }

        JsonNode node = jsonPathParser.parse(value.toJson()).read(filter.path);
        String pathJson = node != null ? node.toString() : null;
        return TextUtil.isNullOrEmpty(pathJson) ? null : Values.parse(pathJson).orElse(null);
    }
}