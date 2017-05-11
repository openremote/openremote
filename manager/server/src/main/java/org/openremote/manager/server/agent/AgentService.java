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
package org.openremote.manager.server.agent;

import org.apache.camel.builder.RouteBuilder;
import org.openremote.agent.protocol.Protocol;
import org.openremote.agent.protocol.ProtocolAssetService;
import org.openremote.container.Container;
import org.openremote.container.ContainerService;
import org.openremote.container.message.MessageBrokerService;
import org.openremote.container.message.MessageBrokerSetupService;
import org.openremote.container.persistence.PersistenceEvent;
import org.openremote.manager.server.asset.AssetProcessingService;
import org.openremote.manager.server.asset.AssetStorageService;
import org.openremote.manager.server.asset.ServerAsset;
import org.openremote.manager.server.datapoint.AssetDatapointService;
import org.openremote.model.AbstractValueTimestampHolder;
import org.openremote.model.asset.*;
import org.openremote.model.asset.agent.AgentLink;
import org.openremote.model.asset.agent.ProtocolConfiguration;
import org.openremote.model.attribute.AttributeEvent;
import org.openremote.model.attribute.AttributeRef;
import org.openremote.model.util.Pair;
import org.openremote.model.value.ObjectValue;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.openremote.agent.protocol.Protocol.ACTUATOR_TOPIC;
import static org.openremote.agent.protocol.Protocol.DeploymentStatus.*;
import static org.openremote.container.persistence.PersistenceEvent.PERSISTENCE_TOPIC;
import static org.openremote.manager.server.asset.AssetPredicates.isPersistenceEventForAssetType;
import static org.openremote.manager.server.asset.AssetPredicates.isPersistenceEventForEntityType;
import static org.openremote.model.asset.AssetAttribute.attributesFromJson;
import static org.openremote.model.asset.AssetType.AGENT;
import static org.openremote.model.asset.agent.AgentLink.getAgentLink;
import static org.openremote.model.util.TextUtil.isNullOrEmpty;
import static org.openremote.model.util.TextUtil.isValidURN;

/**
 * Finds all {@link AssetType#AGENT} and {@link AssetType#THING} assets and starts the protocols for them.
 */
public class AgentService extends RouteBuilder implements ContainerService, Consumer<AssetState>, ProtocolAssetService {

    private static final Logger LOG = Logger.getLogger(AgentService.class.getName());

    protected Container container;
    protected AssetProcessingService assetProcessingService;
    protected AssetStorageService assetStorageService;
    protected AssetDatapointService assetDatapointService;
    protected MessageBrokerService messageBrokerService;
    protected final Map<AttributeRef, Pair<AssetAttribute, Protocol.DeploymentStatus>> protocolConfigurations = new HashMap<>();
    protected final Map<String, Protocol> protocols = new HashMap<>();

    @Override
    public void init(Container container) throws Exception {
        this.container = container;
        assetProcessingService = container.getService(AssetProcessingService.class);
        assetStorageService = container.getService(AssetStorageService.class);
        assetDatapointService = container.getService(AssetDatapointService.class);
        messageBrokerService = container.getService(MessageBrokerService.class);
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

        List<ServerAsset> agents = assetStorageService.findAll(new AssetQuery()
            .select(new AssetQuery.Select(true, false))
            .type(AssetType.AGENT));
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
        synchronized (protocolConfigurations) {
            new ArrayList<>(protocolConfigurations.values())
                .forEach(protocolConfigAndConsumer -> unlinkProtocolConfiguration(protocolConfigAndConsumer.key));
        }
    }

    @Override
    public void configure() throws Exception {
        from(PERSISTENCE_TOPIC)
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
        if (oldProtocolConfiguration.getObjectValue().equals(protocolConfiguration.getObjectValue())) {
            // Protocol configuration hasn't changed so nothing to do here
            return;
        }

        agent.replaceAttribute(protocolConfiguration);
        LOG.fine("Updating agent protocol configuration: " + protocolRef);
        assetStorageService.merge(agent);
    }

    @Override
    public void sendAttributeEvent(AttributeEvent attributeEvent) {
        assetProcessingService.sendAttributeEvent(attributeEvent, false);
    }

    /**
     * Looks for new, modified and obsolete protocol configurations and links / unlinks any associated
     * attributes
     */
    protected void processAgentChange(Asset agent, PersistenceEvent persistenceEvent) {
        LOG.finest("Processing agent persistence event: " + persistenceEvent.getCause());

        switch (persistenceEvent.getCause()) {
            case INSERT:

                agent.getAttributesStream()
                    .filter(ProtocolConfiguration::isProtocolConfiguration)
                    .forEach(this::linkProtocolConfiguration);

                break;
            case UPDATE:

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
                        .noneMatch(newProtocolAttribute ->
                            oldProtocolAttribute.getObjectValue().equals(newProtocolAttribute.getObjectValue())
                        )
                    )
                    .forEach(this::unlinkProtocolConfiguration);

                // Link protocols that are in newConfigs but not in oldConfigs
                newProtocolConfigurations
                    .stream()
                    .filter(newProtocolAttribute -> oldProtocolConfigurations
                        .stream()
                        .noneMatch(oldProtocolAttribute ->
                            oldProtocolAttribute.getObjectValue().equals(newProtocolAttribute.getObjectValue())
                        )
                    ).forEach(this::linkProtocolConfiguration);

                break;
            case DELETE:

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

                // Link any AGENT_LINK attributes to the referenced protocol
                Map<AssetAttribute, List<AssetAttribute>> groupedAgentLinksAttributes =
                    getGroupedAgentLinkAttributes(
                        asset.getAttributesStream(),
                        attribute -> true,
                        attribute -> LOG.warning("Linked protocol configuration not found: " + attribute)
                    );
                groupedAgentLinksAttributes.forEach(this::linkAttributes);

                break;
            case UPDATE:

                // Check if attributes of the asset have been modified
                int attributesIndex = Arrays.asList(persistenceEvent.getPropertyNames()).indexOf("attributes");
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
                Stream<AssetAttribute> attributesToUnlink = oldAgentLinkedAttributes
                    .stream()
                    .filter(oldAgentLinkedAttribute -> newAgentLinkedAttributes
                        .stream()
                        .noneMatch(newAgentLinkedAttribute ->
                            oldAgentLinkedAttribute.getObjectValue().equalsIgnoreKeys(
                                newAgentLinkedAttribute.getObjectValue(),
                                AbstractValueTimestampHolder.VALUE_TIMESTAMP_FIELD_NAME
                            )
                        )
                    );

                getGroupedAgentLinkAttributes(attributesToUnlink, attribute -> true)
                    .forEach(this::unlinkAttributes);

                // Link thing attributes that are in new but not in old
                Stream<AssetAttribute> attributesToLink = newAgentLinkedAttributes
                    .stream()
                    .filter(newThingAttribute -> oldAgentLinkedAttributes
                        .stream()
                        .noneMatch(oldThingAttribute ->
                            oldThingAttribute.getObjectValue().equalsIgnoreKeys(
                                newThingAttribute.getObjectValue(),
                                AbstractValueTimestampHolder.VALUE_TIMESTAMP_FIELD_NAME
                            )
                        )
                    );

                getGroupedAgentLinkAttributes(
                    attributesToLink,
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
        LOG.info("Linking all attributes that use protocol attribute: " + protocolConfiguration);
        AttributeRef protocolAttributeRef = protocolConfiguration.getReferenceOrThrow();
        Protocol protocol = getProtocol(protocolConfiguration);

        if (protocol == null) {
            LOG.warning("Cannot find protocol that attribute is linked to: " + protocolAttributeRef);
            return;
        }

        synchronized (protocolConfigurations) {
            // Create a consumer callback for deployment status updates
            Consumer<Protocol.DeploymentStatus> deploymentStatusConsumer = status ->
                publishProtocolDeploymentStatus(protocolAttributeRef, status);

            // Store the info
            protocolConfigurations.put(
                protocolAttributeRef,
                new Pair<>(protocolConfiguration, LINKING)
            );

            // Set status to linking
            publishProtocolDeploymentStatus(protocolAttributeRef, LINKING);

            // Link the protocol configuration to the protocol
            try {
                protocol.linkProtocolConfiguration(protocolConfiguration, deploymentStatusConsumer);
            } catch (Exception e) {
                LOG.log(Level.SEVERE, "Protocol threw an exception during protocol configuration linking", e);
                // Set status to error
                publishProtocolDeploymentStatus(protocolAttributeRef, ERROR);
                // Cannot continue to link attributes;
                return;
            }
        }

        // Get all assets that have attributes that use this protocol configuration
        List<ServerAsset> assets = assetStorageService.findAll(
            new AssetQuery()
                .select(new AssetQuery.Select(true, false))
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
        LOG.info("Unlinking all attributes that use protocol attribute: " + protocolConfiguration);
        AttributeRef protocolAttributeRef = protocolConfiguration.getReferenceOrThrow();

        // Get all assets that have attributes that use this protocol configuration
        List<ServerAsset> assets = assetStorageService.findAll(
            new AssetQuery()
                .select(new AssetQuery.Select(true, false))
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

            // Set status to error
            publishProtocolDeploymentStatus(protocolAttributeRef, UNLINKED);
            protocolConfigurations.remove(protocolAttributeRef);
        }
    }

    // TODO: Implement mechanism for publishing/subscribing to protocolconfiguration deployment status
    protected void publishProtocolDeploymentStatus(AttributeRef protocolRef, Protocol.DeploymentStatus status) {
        LOG.fine("Protocol status updated to '" + status + "': " + protocolRef);
        synchronized (protocolConfigurations) {
            Pair<AssetAttribute, Protocol.DeploymentStatus> protocolDeploymentInfo = protocolConfigurations.get(protocolRef);
            if (protocolDeploymentInfo != null) {
                protocolDeploymentInfo.value = status;
            }
        }
    }

    public Protocol.DeploymentStatus getProtocolDeploymentStatus(AttributeRef protocolRef) {
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
        Protocol protocol = getProtocol(protocolConfiguration);

        if (protocol == null) {
            LOG.severe("Cannot link protocol attributes as protocol is null: " + protocolConfiguration);
            return;
        }

        try {
            LOG.finest("Linking protocol attributes to: " + protocol.getProtocolName());
            protocol.linkAttributes(attributes, protocolConfiguration);
        } catch (Exception ex) {
            LOG.log(Level.SEVERE, "Ignoring error on linking attributes to protocol: " + protocol.getProtocolName(), ex);
            // Update the status of this protocol configuration to error
            publishProtocolDeploymentStatus(protocolConfiguration.getReferenceOrThrow(), ERROR);
        }
    }

    protected void unlinkAttributes(AssetAttribute protocolConfiguration, Collection<AssetAttribute> attributes) {
        Protocol protocol = getProtocol(protocolConfiguration);

        if (protocol == null) {
            LOG.severe("Cannot unlink protocol attributes as protocol is null: " + protocolConfiguration);
            return;
        }

        try {
            LOG.finest("Unlinking protocol attributes from: " + protocol.getProtocolName());
            protocol.unlinkAttributes(attributes, protocolConfiguration);
        } catch (Exception ex) {
            LOG.log(Level.SEVERE, "Ignoring error on unlinking attributes from protocol: " + protocol.getProtocolName(), ex);
            // Update the status of this protocol configuration to error
            publishProtocolDeploymentStatus(protocolConfiguration.getReferenceOrThrow(), ERROR);
        }
    }

    /**
     * If this is not a southbound update from a client, or if the changed attribute is not linked to
     * an agent's protocol configuration, we ignore it.
     * <p>
     * Otherwise we push the update to the attributes' linked protocol to handle and prevent any further
     * processing of this event by the processing chain. The protocol should raise sensor updates as
     * required (i.e. the protocol is responsible for synchronising state with the database).
     */
    @Override
    public void accept(AssetState assetState) {
        if (assetState.isSouthbound()) {
            LOG.fine("Handling south-bound update: " + assetState);
        } else {
            LOG.fine("Ignoring non-client update: " + assetState);
            return;
        }

        AgentLink.getAgentLink(assetState.getAttribute())
            .map(ref ->
                getProtocolConfiguration(ref)
                    .orElseGet(() -> {
                        LOG.warning("Cannot process as agent link is invalid:" + assetState);
                        assetState.setError(new RuntimeException("Attribute has an invalid agent link: " + assetState.getAttribute()));
                        assetState.setProcessingStatus(AssetState.ProcessingStatus.ERROR);
                        return null;
                    })
            )
            .map(protocolConfiguration -> {
                // Its' a send to actuator - push the update to the protocol
                LOG.fine("Processing: " + assetState);

                Optional<AttributeEvent> event = assetState
                    .getAttribute()
                    .getStateEvent();

                if (event.isPresent()) {
                    messageBrokerService.getProducerTemplate().sendBodyAndHeader(
                        ACTUATOR_TOPIC,
                        event.get(),
                        Protocol.ACTUATOR_TOPIC_TARGET_PROTOCOL,
                        protocolConfiguration.getValueAsString().orElse("")
                    );
                } else {
                    LOG.warning("Attribute doesn't have a valid attribute event: " + assetState);
                }

                assetState.setProcessingStatus(AssetState.ProcessingStatus.HANDLED);
                return protocolConfiguration;
            })
            .orElseGet(() -> {
                // This is just a non protocol attribute so allow the processing to continue
                LOG.fine("Ignoring as it is not for an attribute linked to an agent:" + assetState);
                return null;
            });
    }

    /**
     * Gets all agent link attributes and their linked protocol configuration and groups them by Protocol Configuration
     */
    @SuppressWarnings("ConstantConditions")
    protected Map<AssetAttribute, List<AssetAttribute>> getGroupedAgentLinkAttributes(Stream<AssetAttribute> attributes,
                                                                                      Predicate<AssetAttribute> filter) {

        return getGroupedAgentLinkAttributes(attributes, filter, null);
    }

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
            Pair<AssetAttribute, Protocol.DeploymentStatus> deploymentStatusPair = protocolConfigurations.get(protocolRef);
            return deploymentStatusPair == null ? Optional.empty() : Optional.of(deploymentStatusPair.key);
        }
    }
}