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

import elemental.json.JsonObject;
import org.apache.camel.builder.RouteBuilder;
import org.openremote.agent3.protocol.Protocol;
import org.openremote.container.Container;
import org.openremote.container.ContainerService;
import org.openremote.container.message.MessageBrokerService;
import org.openremote.container.message.MessageBrokerSetupService;
import org.openremote.container.persistence.PersistenceEvent;
import org.openremote.manager.server.asset.AssetStorageService;
import org.openremote.manager.server.asset.ServerAsset;
import org.openremote.manager.server.datapoint.AssetDatapointService;
import org.openremote.model.AbstractValueTimestampHolder;
import org.openremote.model.AttributeRef;
import org.openremote.model.Function;
import org.openremote.model.asset.*;
import org.openremote.model.asset.agent.AgentAttributes;
import org.openremote.model.asset.agent.ProtocolConfiguration;
import org.openremote.model.asset.thing.ThingAttribute;
import org.openremote.model.util.JsonUtil;

import java.util.*;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static org.openremote.agent3.protocol.Protocol.ACTUATOR_TOPIC;
import static org.openremote.container.persistence.PersistenceEvent.PERSISTENCE_TOPIC;
import static org.openremote.manager.server.asset.AssetPredicates.isPersistenceEventForAssetType;
import static org.openremote.manager.server.asset.AssetPredicates.isPersistenceEventForEntityType;
import static org.openremote.model.asset.AssetType.AGENT;

/**
 * Finds all {@link AssetType#AGENT} and {@link AssetType#THING} assets and starts the protocols for them.
 */
public class AgentService extends RouteBuilder implements ContainerService, Consumer<AssetState> {

    private static final Logger LOG = Logger.getLogger(AgentService.class.getName());
    protected Container container;
    protected AssetStorageService assetStorageService;
    protected AssetDatapointService assetDatapointService;
    protected MessageBrokerService messageBrokerService;
    protected Collection<Protocol> protocols; // TODO: Can protocols be added at runtime
    protected final Map<AttributeRef, ProtocolConfiguration> protocolConfigurations = new HashMap<>();
    protected Function<AttributeRef, ProtocolConfiguration> protocolConfigurationResolver = (AttributeRef protocolRef) -> {
        synchronized (protocolConfigurations) {
            return protocolConfigurations.get(protocolRef);
        }
    };

    @Override
    public void init(Container container) throws Exception {
        this.container = container;
        assetStorageService = container.getService(AssetStorageService.class);
        assetDatapointService = container.getService(AssetDatapointService.class);
        messageBrokerService = container.getService(MessageBrokerService.class);
        container.getService(MessageBrokerSetupService.class).getContext().addRoutes(this);
    }

    @Override
    public void start(Container container) throws Exception {
        protocols = container.getServices(Protocol.class);
        List<ServerAsset> agents = assetStorageService.findAll(new AssetQuery().select(new AssetQuery.Select(true, false)).type(AssetType.AGENT));
        LOG.fine("Deploy all agents in all realms: " + agents.size());

        for (Asset agent : agents) {
            // Extract and store protocol configurations for this agent - there won't be any asset attributes
            // linked yet as agent has just been created
            // Link the protocol configuration
            ProtocolConfiguration.getAll(agent)
                    .forEach(this::linkProtocolConfiguration);
        }
    }

    @Override
    public void stop(Container container) throws Exception {
        List<ProtocolConfiguration> protocolConfigs;

        synchronized (protocolConfigurations) {
            protocolConfigs = Arrays.asList(protocolConfigurations.values().toArray(new ProtocolConfiguration[0]));
        }

        protocolConfigs.forEach(this::unlinkProtocolConfiguration);
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
     * Looks for new, modified and obsolete protocol configurations and links / unlinks any associated
     * attributes
     */
    protected void processAgentChange(Asset agent, PersistenceEvent persistenceEvent) {
        LOG.finest("Processing agent persistence event: " + persistenceEvent.getCause());

        switch (persistenceEvent.getCause()) {
            case INSERT:

                // Link all protocol configurations for this agent
                ProtocolConfiguration.getAll(agent)
                        .forEach(protocolConfiguration -> {
                            AttributeRef protocolRef = new AttributeRef(agent.getId(), protocolConfiguration.getName());
                            LOG.fine("Agent was persisted (" + persistenceEvent.getCause() + "), linking protocol: " + protocolRef);
                            linkProtocolConfiguration(protocolConfiguration);
                        });

                break;
            case UPDATE:

                // Check if any protocol config attributes have been added/removed or modified
                int attributesIndex = Arrays.asList(persistenceEvent.getPropertyNames()).indexOf("attributes");
                if (attributesIndex < 0) {
                    return;
                }

                // Attributes have possibly changed so need to compare old and new state to determine
                // which protocol configs are affected
                AgentAttributes oldAttributes = new AgentAttributes(agent.getId(), (JsonObject) persistenceEvent.getPreviousState()[attributesIndex]);
                AgentAttributes newAttributes = new AgentAttributes(agent.getId(), (JsonObject) persistenceEvent.getCurrentState()[attributesIndex]);
                List<ProtocolConfiguration> oldProtocolConfigs = ProtocolConfiguration.getAll(oldAttributes.get());
                List<ProtocolConfiguration> newProtocolConfigs = ProtocolConfiguration.getAll(newAttributes.get());

                // Compare protocol configurations by JSON value
                // Unlink protocols that are in oldConfigs but not in newConfigs
                oldProtocolConfigs
                        .stream()
                        .filter(oldProtocolConfig -> newProtocolConfigs
                                .stream()
                                .noneMatch(newProtocolConfig -> JsonUtil.equals(
                                        oldProtocolConfig.getJsonObject(),
                                        newProtocolConfig.getJsonObject())
                                )
                        )
                        .forEach(obsoleteProtocolConfig -> {
                            AttributeRef protocolRef = new AttributeRef(agent.getId(), obsoleteProtocolConfig.getName());
                            LOG.fine("Agent was persisted (" + persistenceEvent.getCause() + "), unlinking protocol: " + protocolRef);
                            unlinkProtocolConfiguration(obsoleteProtocolConfig);
                        });

                // Link protocols that are in newConfigs but not in oldConfigs
                newProtocolConfigs
                        .stream()
                        .filter(newConfig -> oldProtocolConfigs
                                .stream()
                                .noneMatch(oldConfig ->
                                        JsonUtil.equals(
                                                oldConfig.getJsonObject(),
                                                newConfig.getJsonObject()
                                        )
                                )
                        )
                        .forEach(newProtocolConfig -> {
                            AttributeRef protocolRef = new AttributeRef(agent.getId(), newProtocolConfig.getName());
                            LOG.fine("Agent was persisted (" + persistenceEvent.getCause() + "), linking protocol: " + protocolRef);
                            linkProtocolConfiguration(newProtocolConfig);
                        });
                break;
            case DELETE:

                // Unlink any attributes that have an agent link to this agent
                List<ProtocolConfiguration> protocolConfigurations = ProtocolConfiguration.getAll(agent);
                protocolConfigurations
                        .forEach(protocolConfig -> {
                            AttributeRef protocolRef = new AttributeRef(agent.getId(), protocolConfig.getName());
                            LOG.fine("Agent was persisted (" + persistenceEvent.getCause() + "), unlinking protocol: " + protocolRef);
                            unlinkProtocolConfiguration(protocolConfig);
                        });
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
            case INSERT: {
                // Asset insert persistence events can be fired before the agent insert persistence event
                // so need to check that all protocol configs exist - any that don't we will exclude here
                // and handle in agent insert

                // Link any AGENT_LINK attributes to the referenced protocol
                Map<AttributeRef, List<ThingAttribute>> groupedThingAttributes = ThingAttribute.getAllGroupedByProtocolRef(asset, protocolConfigurationResolver);
                groupedThingAttributes.forEach((protocolRef, thingAttributes) -> linkAttributes(getProtocol(protocolConfigurationResolver.apply(protocolRef)), thingAttributes));
                break;
            }
            case UPDATE:

                // Check if attributes of the asset have been modified
                int attributesIndex = Arrays.asList(persistenceEvent.getPropertyNames()).indexOf("attributes");
                if (attributesIndex < 0) {
                    return;
                }

                // Attributes have possibly changed so need to compare old and new state to determine any changes to
                // AGENT_LINK attributes
                AssetAttributes oldAttributes = new AssetAttributes(asset.getId(), (JsonObject) persistenceEvent.getPreviousState()[attributesIndex]);
                AssetAttributes newAttributes = new AssetAttributes(asset.getId(), (JsonObject) persistenceEvent.getCurrentState()[attributesIndex]);
                List<ThingAttribute> oldThingAttributes = ThingAttribute.getAll(oldAttributes.get(), protocolConfigurationResolver);
                List<ThingAttribute> newThingAttributes = ThingAttribute.getAll(newAttributes.get(), protocolConfigurationResolver);

                // Unlink thing attributes that are in old but not in new
                oldThingAttributes
                        .stream()
                        .filter(oldThingAttribute -> newThingAttributes
                                .stream()
                                .noneMatch(newThingAttribute -> JsonUtil.equals( // Ignore the timestamp in comparison
                                        oldThingAttribute.getJsonObject(),
                                        newThingAttribute.getJsonObject(),
                                        Collections.singletonList(AbstractValueTimestampHolder.VALUE_TIMESTAMP_FIELD_NAME))
                                )
                        )
                        .collect(Collectors.groupingBy(thingAttribute -> thingAttribute.getProtocolRef()))
                        .forEach((protocolRef, thingAttributes) ->
                                unlinkAttributes(getProtocol(protocolConfigurationResolver.apply(protocolRef)), thingAttributes)
                        );

                // Link thing attributes that are in new but not in old
                newThingAttributes
                        .stream()
                        .filter(newThingAttribute -> oldThingAttributes
                                .stream()
                                .noneMatch(oldThingAttribute -> JsonUtil.equals( // Ignore the timestamp in comparison
                                        oldThingAttribute.getJsonObject(),
                                        newThingAttribute.getJsonObject(),
                                        Collections.singletonList(AbstractValueTimestampHolder.VALUE_TIMESTAMP_FIELD_NAME))
                                )
                        )
                        .collect(Collectors.groupingBy(thingAttribute -> thingAttribute.getProtocolRef()))
                        .forEach((protocolRef, thingAttributes) ->
                                linkAttributes(getProtocol(protocolConfigurationResolver.apply(protocolRef)), thingAttributes)
                        );
                break;
            case DELETE: {

                // Unlink any AGENT_LINK attributes from the referenced protocol
                Map<AttributeRef, List<ThingAttribute>> groupedThingAttributes = ThingAttribute.getAllGroupedByProtocolRef(asset, protocolConfigurationResolver);
                groupedThingAttributes.forEach((protocolRef, thingAttributes) -> unlinkAttributes(getProtocol(protocolConfigurationResolver.apply(protocolRef)), thingAttributes));
                break;
            }
        }
    }

    protected void linkProtocolConfiguration(ProtocolConfiguration protocolConfiguration) {
        AttributeRef protocolAttributeRef = protocolConfiguration.getReference();

        synchronized (protocolConfigurations) {
            // Store this configuration for easy access later
            protocolConfigurations.put(protocolAttributeRef, protocolConfiguration);
        }

        LOG.finest("Linking all attributes that use protocol configuration: " + protocolAttributeRef);

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

        assets.forEach(asset -> {
            Map<AttributeRef, List<ThingAttribute>> groupedThingAttributes = ThingAttribute.getAllGroupedByProtocolRef(asset, protocolConfigurationResolver);
            linkAttributes(getProtocol(protocolConfiguration), groupedThingAttributes.get(protocolAttributeRef));
        });
    }

    protected void unlinkProtocolConfiguration(ProtocolConfiguration protocolConfiguration) {
        AttributeRef protocolRef = protocolConfiguration.getReference();

        // Get all assets that have attributes that use this protocol configuration
        List<ServerAsset> assets = assetStorageService.findAll(
                new AssetQuery()
                        .select(new AssetQuery.Select(true, false))
                        .attributeMeta(
                        new AssetQuery.AttributeRefPredicate(
                                AssetMeta.AGENT_LINK,
                                protocolRef.getEntityId(),
                                protocolRef.getAttributeName()
                        )
                )
        );

        assets.forEach(asset -> {
            Map<AttributeRef, List<ThingAttribute>> groupedThingAttributes = ThingAttribute.getAllGroupedByProtocolRef(asset, protocolConfigurationResolver);
            unlinkAttributes(getProtocol(protocolConfiguration), groupedThingAttributes.get(protocolRef));
        });

        synchronized (protocolConfigurations) {
            protocolConfigurations.remove(protocolRef);
        }
    }

    protected Protocol getProtocol(ProtocolConfiguration protocolConfiguration) {
        return protocols
                .stream()
                .filter(protocol -> {
                    if (protocol.getProtocolName() == null || protocol.getProtocolName().length() == 0)
                        throw new IllegalStateException("Protocol can't have empty name: " + protocol.getClass());
                    return protocol.getProtocolName().equals(protocolConfiguration.getProtocolName());
                })
                .findFirst()
                .orElse(null);
    }

    protected void linkAttributes(Protocol protocol, List<ThingAttribute> attributes) {
        if (protocol == null) {
            LOG.severe("Cannot link protocol attributes as protocol is null");
            return;
        }

        try {
            LOG.finest("Linking protocol attributes to: " + protocol.getProtocolName());
            protocol.linkAttributes(attributes);
        } catch (Exception ex) {
            // TODO: Better error handling?
            LOG.log(Level.WARNING, "Ignoring error on linking attributes to protocol: " + protocol.getProtocolName(), ex);
        }
    }

    protected void unlinkAttributes(Protocol protocol, List<ThingAttribute> attributes) {
        if (protocol == null) {
            LOG.severe("Cannot link protocol attributes as protocol is null");
            return;
        }

        try {
            LOG.finest("Unlinking protocol attributes form: " + protocol.getProtocolName());
            protocol.unlinkAttributes(attributes);
        } catch (Exception ex) {
            // TODO: Better error handling?
            LOG.log(Level.WARNING, "Ignoring error on unlinking attributes from protocol: " + protocol.getProtocolName(), ex);
        }
    }

    /**
     * If this update is not for an asset of type THING or it has been initiated by
     * a protocol then we ignore it.
     * <p>
     * Otherwise we push the update to the protocol to handle and prevent any further
     * processing of this event by the processing chain, the protocol should raise
     * sensor updates as required (i.e. the protocol is responsible for synchronising state)
     *
     */
    @Override
    public void accept(AssetState assetState) {
        // If update was initiated by a protocol ignore it
        if (assetState.isNorthbound()) {
            LOG.fine("Ignoring as it came from a protocol:" + assetState);
            return;
        }


        AttributeRef protocolRef = ThingAttribute.getProtocolRef(assetState.getAttribute());

        if (protocolRef != null) {
            // Check attribute is linked to an actual agent
            ProtocolConfiguration protocolConfiguration = protocolConfigurationResolver.apply(protocolRef);

            if (protocolConfiguration == null) {
                LOG.warning("Cannot process as agent link is invalid:" + assetState);
                assetState.setError(new RuntimeException("Attribute has an invalid agent link:" + assetState.getAttribute()));
                assetState.setProcessingStatus(AssetState.ProcessingStatus.ERROR);
                return;
            }
        } else {
            // This is just a non protocol attribute so allow the processing to continue
            LOG.fine("Ignoring as it is not for an attribute linked to an agent:" + assetState);
            return;
        }

        // Its' a send to actuator - push the update to the protocol
        LOG.fine("Processing: " + assetState);
        messageBrokerService.getProducerTemplate().sendBody(
            ACTUATOR_TOPIC,
            assetState.getAttribute().getStateEvent()
        );
        assetState.setProcessingStatus(AssetState.ProcessingStatus.HANDLED);
    }

    public Function<AttributeRef, ProtocolConfiguration> getProtocolConfigurationResolver() {
        return protocolConfigurationResolver;
    }

/*
    public static List<ThingAttribute> getThingAttributes(Asset asset) {
        return new AssetAttributes(asset.getAttributes()).get()
                .stream()
                .filter(AssetAttribute::isAgentLinked)
                .map(ThingAttribute::new)
                .collect(Collectors.toList());
    }
*/

    public String toString() {
        return getClass().getSimpleName() + "{" +
            '}';
    }
}