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

import org.apache.camel.builder.RouteBuilder;
import org.openremote.agent.protocol.ProtocolAssetService;
import org.openremote.container.message.MessageBrokerService;
import org.openremote.model.PersistenceEvent;
import org.openremote.container.timer.TimerService;
import org.openremote.manager.asset.AssetProcessingException;
import org.openremote.manager.asset.AssetProcessingService;
import org.openremote.manager.asset.AssetStorageService;
import org.openremote.manager.asset.AssetUpdateProcessor;
import org.openremote.manager.event.ClientEventService;
import org.openremote.manager.gateway.GatewayService;
import org.openremote.manager.security.ManagerIdentityService;
import org.openremote.manager.web.ManagerWebService;
import org.openremote.model.Container;
import org.openremote.model.ContainerService;
import org.openremote.model.asset.Asset;
import org.openremote.model.asset.AssetTreeNode;
import org.openremote.model.asset.agent.Agent;
import org.openremote.model.asset.agent.AgentLink;
import org.openremote.model.asset.agent.ConnectionStatus;
import org.openremote.model.asset.agent.Protocol;
import org.openremote.model.attribute.*;
import org.openremote.model.attribute.AttributeEvent.Source;
import org.openremote.model.protocol.ProtocolAssetDiscovery;
import org.openremote.model.protocol.ProtocolAssetImport;
import org.openremote.model.protocol.ProtocolInstanceDiscovery;
import org.openremote.model.query.AssetQuery;
import org.openremote.model.query.filter.AttributePredicate;
import org.openremote.model.query.filter.NameValuePredicate;
import org.openremote.model.query.filter.PathPredicate;
import org.openremote.model.query.filter.StringPredicate;
import org.openremote.model.util.Pair;
import org.openremote.model.util.TextUtil;

import javax.persistence.EntityManager;
import java.util.*;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;
import static org.openremote.container.concurrent.GlobalLock.withLock;
import static org.openremote.container.concurrent.GlobalLock.withLockReturning;
import static org.openremote.container.persistence.PersistenceService.PERSISTENCE_TOPIC;
import static org.openremote.container.persistence.PersistenceService.isPersistenceEventForEntityType;
import static org.openremote.manager.asset.AssetProcessingService.ASSET_QUEUE;
import static org.openremote.manager.gateway.GatewayService.isNotForGateway;
import static org.openremote.model.asset.agent.Protocol.ACTUATOR_TOPIC;
import static org.openremote.model.asset.agent.Protocol.SENSOR_QUEUE;
import static org.openremote.model.attribute.Attribute.getAddedOrModifiedAttributes;
import static org.openremote.model.attribute.AttributeEvent.HEADER_SOURCE;
import static org.openremote.model.attribute.AttributeEvent.Source.*;
import static org.openremote.model.value.MetaItemType.AGENT_LINK;

/**
 * Handles life cycle and communication with {@link Protocol}s.
 * <p>
 * Finds all {@link Agent} assets and manages their {@link Protocol} instances.
 */
public class AgentService extends RouteBuilder implements ContainerService, AssetUpdateProcessor, ProtocolAssetService {

    private static final Logger LOG = Logger.getLogger(AgentService.class.getName());
    public static final int PRIORITY = DEFAULT_PRIORITY + 100; // Start quite late to ensure asset model etc. are initialised
    protected TimerService timerService;
    protected ManagerIdentityService identityService;
    protected AssetProcessingService assetProcessingService;
    protected AssetStorageService assetStorageService;
    protected MessageBrokerService messageBrokerService;
    protected ClientEventService clientEventService;
    protected GatewayService gatewayService;
    protected ScheduledExecutorService executorService;
    protected Map<String, Agent<?, ?, ?>> agentMap;
    protected final Map<String, Future<Void>> agentDiscoveryImportFutureMap = new HashMap<>();
    protected final Map<String, Protocol<?>> protocolInstanceMap = new HashMap<>();
    protected final Map<String, List<Consumer<PersistenceEvent<Asset<?>>>>> childAssetSubscriptions = new HashMap<>();
    protected boolean initDone;
    protected Container container;

    @Override
    public int getPriority() {
        return PRIORITY;
    }

    @Override
    public void init(Container container) throws Exception {
        this.container = container;
        timerService = container.getService(TimerService.class);
        identityService = container.getService(ManagerIdentityService.class);
        assetProcessingService = container.getService(AssetProcessingService.class);
        assetStorageService = container.getService(AssetStorageService.class);
        messageBrokerService = container.getService(MessageBrokerService.class);
        clientEventService = container.getService(ClientEventService.class);
        gatewayService = container.getService(GatewayService.class);
        executorService = container.getExecutorService();

        if (initDone) {
            return;
        }

        container.getService(ManagerWebService.class).addApiSingleton(
            new AgentResourceImpl(
                container.getService(TimerService.class),
                container.getService(ManagerIdentityService.class),
                assetStorageService,
                this,
                container.getExecutorService())
        );

        initDone = true;
    }

    @Override
    public void start(Container container) throws Exception {
        container.getService(MessageBrokerService.class).getContext().addRoutes(this);

        // Load all enabled agents and instantiate a protocol instance for each
        LOG.fine("Loading agents...");
        Collection<Agent<?, ?, ?>> agents = getAgents().values();
        LOG.fine("Found agent count = " + agents.size());

        agents.stream().forEach(this::doAgentInit);
    }

    @Override
    public void stop(Container container) throws Exception {
        List<Agent<?,?,?>> agents = new ArrayList<>(agentMap.values());
        agents.forEach(agent -> this.stopAgent(agent.getId()));
        agentMap.clear();
        protocolInstanceMap.clear();
    }

    @SuppressWarnings("unchecked")
    @Override
    public void configure() throws Exception {
        from(PERSISTENCE_TOPIC)
            .routeId("AgentPersistenceChanges")
            .filter(isPersistenceEventForEntityType(Asset.class))
            .filter(isNotForGateway(gatewayService))
            .process(exchange -> {
                PersistenceEvent<Asset<?>> persistenceEvent = (PersistenceEvent<Asset<?>>)exchange.getIn().getBody(PersistenceEvent.class);

                if (isPersistenceEventForEntityType(Agent.class).matches(exchange)) {
                    PersistenceEvent<Agent<?, ?, ?>> agentEvent = (PersistenceEvent<Agent<?,?,?>>)(PersistenceEvent<?>)persistenceEvent;
                    processAgentChange(agentEvent);
                } else {
                    processAssetChange(persistenceEvent);
                }
            });

        // A protocol wants to write a new sensor value
        from(SENSOR_QUEUE)
            .routeId("FromSensorUpdates")
            .filter(body().isInstanceOf(AttributeEvent.class))
            .setHeader(HEADER_SOURCE, () -> SENSOR)
            .to(ASSET_QUEUE);
    }

    @Override
    public <T extends Asset<?>> T mergeAsset(T asset) {
        Objects.requireNonNull(asset.getId());
        Objects.requireNonNull(asset.getParentId());

        // Do basic check that parent is at least an agent...doesn't confirm its' the correct agent so
        // that's up to the protocol to guarantee
        // TODO: need to revisit this once agent generated assets logic finalised
//        if (!getAgents().containsKey(asset.getParentId())) {
//            String msg = "Cannot merge protocol-provided asset as the parent ID is not a valid agent ID: " + asset;
//            LOG.warning(msg);
//            throw new IllegalArgumentException(msg);
//        }

        // TODO: Define access permissions for merged asset (user asset links inherit from parent agent?)
        LOG.fine("Merging asset with protocol-provided: " + asset);
        return assetStorageService.merge(asset, true);
    }

    @Override
    public boolean deleteAssets(String...assetIds) {
        LOG.fine("Deleting protocol-provided: " + Arrays.toString(assetIds));
        return assetStorageService.delete(Arrays.asList(assetIds), false);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends Asset<?>> T findAsset(String assetId) {
        LOG.fine("Getting protocol-provided: " + assetId);
        return (T)assetStorageService.find(assetId);
    }

    @Override
    public <T extends Asset<?>> T findAsset(String assetId, Class<T> assetType) {
        LOG.fine("Getting protocol-provided: " + assetId);
        return assetStorageService.find(assetId, assetType);
    }

    @Override
    public List<Asset<?>> findAssets(String assetId, AssetQuery assetQuery) {
        if (TextUtil.isNullOrEmpty(assetId)) {
            return Collections.emptyList();
        }

        if (assetQuery == null) {
            assetQuery = new AssetQuery();
        }

        // Ensure agent ID is injected into each path predicate
        if (assetQuery.paths != null) {
            for (PathPredicate pathPredicate : assetQuery.paths) {
                int len = pathPredicate.path.length;
                pathPredicate.path = Arrays.copyOf(pathPredicate.path, len+1);
                pathPredicate.path[len] = assetId;
            }
        } else {
            assetQuery.paths(new PathPredicate(assetId));
        }

        return assetStorageService.findAll(assetQuery);
    }

    @Override
    public void sendAttributeEvent(AttributeEvent attributeEvent) {
        assetProcessingService.sendAttributeEvent(attributeEvent);
    }

    protected void processAgentChange(PersistenceEvent<Agent<?, ?, ?>> persistenceEvent) {

        LOG.finest("Processing agent persistence event: " + persistenceEvent.getCause());
        Agent<?, ?, ?> agent = persistenceEvent.getEntity();

        switch (persistenceEvent.getCause()) {
            case CREATE:
                agent = addReplaceAgent(agent);
                doAgentInit(agent);
                break;
            case UPDATE:
                onAgentUpdated(agent);
                break;
            case DELETE:
                if (!removeAgent(agent.getId())) {
                    return;
                }
                // Unlink any attributes that have an agent link to this agent
                stopAgent(agent.getId());
                break;
        }
    }

    protected Agent<?,?,?> onAgentUpdated(Agent<?,?,?> agent) {

        // Old agent can be null if an update happens straight after the creation
        if (!removeAgent(agent.getId())) {
            return agent;
        }

        stopAgent(agent.getId());
        agent = addReplaceAgent(agent);

        if (agent == null) {
            return null;
        }

        doAgentInit(agent);
        return agent;
    }

    /**
     * Looks for new, modified and obsolete AGENT_LINK attributes and links / unlinks them
     * with the protocol
     */
    protected void processAssetChange(PersistenceEvent<Asset<?>> persistenceEvent) {

        LOG.finest("Processing asset persistence event: " + persistenceEvent.getCause());
        Asset<?> asset = persistenceEvent.getEntity();

        switch (persistenceEvent.getCause()) {
            case CREATE:

                // Link any AGENT_LINK attributes to their referenced agent asset
                getGroupedAgentLinkAttributes(
                    asset.getAttributes().stream(),
                    attribute -> true
                ).forEach((agent, attributes) -> this.linkAttributes(agent, asset.getId(), attributes));

                break;
            case UPDATE:
                int attributesIndex = Arrays.asList(persistenceEvent.getPropertyNames()).indexOf("attributes");
                if (attributesIndex < 0) {
                    return;
                }

                List<Attribute<?>> oldLinkedAttributes = ((AttributeMap)persistenceEvent.getPreviousState("attributes"))
                    .stream()
                    .filter(attr -> attr.hasMeta(AGENT_LINK))
                    .collect(toList());

                List<Attribute<?>> newLinkedAttributes = ((AttributeMap) persistenceEvent.getCurrentState("attributes"))
                    .stream()
                    .filter(attr -> attr.hasMeta(AGENT_LINK))
                    .collect(Collectors.toList());

                // Unlink obsolete or modified linked attributes
                List<Attribute<?>> obsoleteOrModified = getAddedOrModifiedAttributes(newLinkedAttributes, oldLinkedAttributes).collect(toList());

                getGroupedAgentLinkAttributes(
                    obsoleteOrModified.stream(),
                    attribute -> true
                ).forEach((agent, attributes) -> unlinkAttributes(agent.getId(), asset.getId(), attributes));

                // Link new or modified attributes
                getGroupedAgentLinkAttributes(
                    newLinkedAttributes.stream().filter(attr ->
                        !oldLinkedAttributes.contains(attr) || obsoleteOrModified.contains(attr)),
                    attribute -> true)
                    .forEach((agent, attributes) -> linkAttributes(agent, asset.getId(), attributes));

                break;
            case DELETE: {

                // Unlink any AGENT_LINK attributes from the referenced protocol
                getGroupedAgentLinkAttributes(asset.getAttributes().stream(), attribute -> true)
                    .forEach((agent, attributes) -> unlinkAttributes(agent.getId(), asset.getId(), attributes));
                break;
            }
        }

        notifyAgentAncestor(asset, persistenceEvent);
    }

    protected void notifyAgentAncestor(Asset<?> asset, PersistenceEvent<Asset<?>> persistenceEvent) {
        String parentId = asset.getParentId();

        if ((asset instanceof Agent) || parentId == null) {
            return;
        }

        String ancestorAgentId = null;

        if (agentMap.containsKey(parentId)) {
            ancestorAgentId = parentId;
        } else {
            // If path is not loaded then get the parents path as the asset might have been deleted
            if (asset.getPath() == null) {
                Asset<?> parentAsset = assetStorageService.find(parentId);
                if (parentAsset != null && parentAsset.getPath() != null) {
                    ancestorAgentId = Arrays.stream(parentAsset.getPath())
                        .filter(assetId -> getAgents().containsKey(assetId))
                        .findFirst()
                        .orElse(null);
                }
            }
        }

        if (ancestorAgentId != null) {
            notifyChildAssetChange(ancestorAgentId, persistenceEvent);
        }
    }

    protected void doAgentInit(Agent<?,?,?> agent) {
        boolean isDisabled = agent.isDisabled().orElse(false);
        if (isDisabled) {
            LOG.fine("Agent is disabled so not starting: " + agent);
            sendAttributeEvent(new AttributeEvent(agent.getId(), Agent.STATUS.getName(), ConnectionStatus.DISABLED));
        } else {
            this.startAgent(agent);
        }
    }

    protected void startAgent(Agent<?,?,?> agent) {
        withLock(getClass().getSimpleName() + "::startAgent", () -> {
            Protocol<?> protocol = null;

            try {
                protocol = agent.getProtocolInstance();
                protocolInstanceMap.put(agent.getId(), protocol);

                LOG.fine("Starting protocol instance: " + protocol);
                protocol.start(container);
                LOG.fine("Started protocol instance:" + protocol);

                LOG.finer("Linking attributes to protocol instance: " + protocol);

                // Get all assets that have attributes with agent link meta for this agent
                List<Asset<?>> assets = assetStorageService.findAll(
                    new AssetQuery()
                        .attributes(
                            new AttributePredicate().meta(
                                new NameValuePredicate(AGENT_LINK, new StringPredicate(agent.getId()), false, new NameValuePredicate.Path("id"))
                            )
                        )
                );

                LOG.finer("Found '" + assets.size() + "' asset(s) with attributes linked to this protocol instance: " + protocol);

                assets.forEach(
                    asset ->
                        getGroupedAgentLinkAttributes(
                            asset.getAttributes().stream(),
                            assetAttribute -> assetAttribute.getMetaValue(AGENT_LINK)
                                .map(agentLink -> agentLink.getId().equals(agent.getId()))
                                .orElse(false)
                        ).forEach((agnt, attributes) -> linkAttributes(agnt, asset.getId(), attributes))
                );


            } catch (Exception e) {
                if (protocol != null) {
                    try {
                        protocol.stop(container);
                    } catch (Exception ignored) {
                    }
                }
                protocolInstanceMap.remove(agent.getId());
                LOG.log(Level.SEVERE, "Failed to start protocol instance for agent: " + agent, e);
                sendAttributeEvent(new AttributeEvent(agent.getId(), Agent.STATUS.getName(), ConnectionStatus.ERROR));
            }
        });
    }

    protected void stopAgent(String agentId) {
        withLock(getClass().getSimpleName() + "::stopAgent", () -> {
            Protocol<?> protocol = protocolInstanceMap.get(agentId);

            if (protocol == null) {
                return;
            }

            Map<String, List<Attribute<?>>> groupedAttributes = protocol.getLinkedAttributes().entrySet().stream().collect(
                Collectors.groupingBy(entry -> entry.getKey().getId(), mapping(Map.Entry::getValue, toList()))
            );

            groupedAttributes.forEach((assetId, linkedAttributes) -> unlinkAttributes(agentId, assetId, linkedAttributes));

            // Stop the protocol instance
            try {
                protocol.stop(container);
            } catch (Exception e) {
                LOG.log(Level.SEVERE, "Protocol instance threw an exception whilst being stopped", e);
            }

            // Remove child asset subscriptions for this agent
            childAssetSubscriptions.remove(agentId);
            protocolInstanceMap.remove(agentId);
        });
    }

    protected void linkAttributes(Agent<?,?,?> agent, String assetId, Collection<Attribute<?>> attributes) {
        withLock(getClass().getSimpleName() + "::linkAttributes", () -> {
            Protocol<?> protocol = getProtocolInstance(agent.getId());

            if (protocol == null) {
                return;
            }

            LOG.info("Linking asset '" + assetId + "' attributes linked to protocol: assetId=" + assetId + ", attributes=" + attributes.size() +  ", protocol=" + protocol);

            attributes.forEach(attribute -> {
                AttributeRef attributeRef = new AttributeRef(assetId, attribute.getName());
                try {
                    if (!protocol.getLinkedAttributes().containsKey(attributeRef)) {
                        LOG.finer("Linking attribute '" + attributeRef + "' to protocol: " + protocol);
                        protocol.linkAttribute(assetId, attribute);
                    }
                } catch (Exception ex) {
                    LOG.log(Level.SEVERE, "Failed to link attribute '" + attributeRef + "' to protocol: " + protocol, ex);
                }
            });
        });
    }

    protected void unlinkAttributes(String agentId, String assetId, List<Attribute<?>> attributes) {
        withLock(getClass().getSimpleName() + "::unlinkAttributes", () -> {
            Protocol<?> protocol = getProtocolInstance(agentId);

            if (protocol == null) {
                return;
            }

            LOG.info("Unlinking asset '" + assetId + "' attributes linked to protocol: assetId=" + assetId + ", attributes=" + attributes.size() +  ", protocol=" + protocol);

            attributes.forEach(attribute -> {
                try {
                    AttributeRef attributeRef = new AttributeRef(assetId, attribute.getName());
                    if (protocol.getLinkedAttributes().containsKey(attributeRef)) {
                        LOG.finer("Unlinking attribute '" + attributeRef + "' to protocol: " + protocol);
                        protocol.unlinkAttribute(assetId, attribute);
                    }
                } catch (Exception ex) {
                    LOG.log(Level.SEVERE, "Ignoring error on unlinking attribute '" + attribute + "' from protocol: " + protocol, ex);
                }
            });
        });
    }

    /**
     * If this is an update from a sensor, or if the changed attribute is not linked to an agent, it's ignored.
     * <p>
     * Otherwise push the update to the attributes' linked protocol to handle and prevent any further
     * processing of this event by the processing chain. The protocol should raise sensor updates as
     * required (i.e. the protocol is responsible for synchronising state with the database).
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    public boolean processAssetUpdate(EntityManager entityManager,
                                      Asset<?> asset,
                                      Attribute<?> attribute,
                                      Source source) throws AssetProcessingException {

        if (source == SENSOR || source == GATEWAY) {
            return false;
        }

        AttributeEvent attributeEvent = new AttributeEvent(new AttributeState(asset.getId(), attribute), attribute.getTimestamp().orElseGet(timerService::getCurrentTimeMillis));

        if (asset instanceof Agent) {

            // This is how we update the agent when an attribute event occurs on an agent's attribute by a client, this
            // is not ideal as value is not committed to the DB and could in theory be consumed by another processor
            // but we need to know the source which isn't available from the client event service.
            // TODO: Expose event source to client event subscriptions
            Agent<?, ?, ?> agent = getAgent(attributeEvent.getAssetId());

            if (agent != null) {

                // Update in memory agent
                agent.addOrReplaceAttributes(attribute);

                if (source == CLIENT && agent.isConfigurationAttribute(attribute.getName())) {
                    LOG.finer("Agent attribute event occurred from a client for an agent config attribute so updating: agent=" + agent.getId() + ", event=" + attributeEvent);
                    onAgentUpdated(agent);
                }
            }

            // Don't consume the event as we want the agent attribute to be updated in the DB
            return false;
        }

        Boolean result = withLockReturning(getClass().getSimpleName() + "::processAssetUpdate", () ->
            attribute.getMetaValue(AGENT_LINK)
                .map(agentLink -> {
                    LOG.finer("Attribute write for agent linked attribute: agent=" + agentLink.getId() + ", asset=" + asset.getId() + ", attribute=" + attribute.getName());

                    messageBrokerService.getProducerTemplate().sendBodyAndHeader(
                        ACTUATOR_TOPIC,
                        attributeEvent,
                        Protocol.ACTUATOR_TOPIC_TARGET_PROTOCOL,
                        getProtocolInstance(agentLink.getId())
                    );
                    return true; // Processing complete, skip other processors
                }).orElse(false) // This is a regular attribute so allow the processing to continue
        );
        return result != null ? result : false;
    }

    /**
     * Gets all agent link attributes and their linked agent and groups them by agent
     */
    protected Map<Agent<?,?,?>, List<Attribute<?>>> getGroupedAgentLinkAttributes(Stream<Attribute<?>> attributes,
                                                                                      Predicate<Attribute<?>> filter) {
        return attributes
            .filter(attribute ->
                // Exclude attributes without agent link or with agent link to not recognised agents (could be gateway agents)
                attribute.getMetaValue(AGENT_LINK)
                    .map(agentLink -> {
                        if (!getAgents().containsKey(agentLink.getId())) {
                            LOG.finest("Agent linked attribute, agent not found or this is a gateway asset: " + attribute);
                            return false;
                        }
                        return true;
                    })
                    .orElse(false))
            .filter(filter)
            .map(attribute -> new Pair<Agent<?,?,?>, Attribute<?>>(attribute.getMetaValue(AGENT_LINK).map(AgentLink::getId).map(agentId -> getAgents().get(agentId)).orElse(null), attribute))
            .filter(agentAttribute -> agentAttribute.key != null)
            .collect(Collectors.groupingBy(
                agentAttribute -> agentAttribute.key,
                    Collectors.collectingAndThen(Collectors.toList(), agentAttribute -> agentAttribute.stream().map(item->item.value).collect(toList())) //TODO had to change to this because compiler has issues with inferring types, need to check for a better solution
            ));
    }

    public String toString() {
        return getClass().getSimpleName() + "{" + "}";
    }

    protected Agent<?, ?, ?> addReplaceAgent(Agent<?, ?, ?> agent) {

        // Fully load agent asset if path and parent info not loaded
        if (agent.getPath() == null || (agent.getPath().length > 1 && agent.getParentId() == null)) {
            LOG.info("Agent is not fully loaded so retrieving the agent from the DB: " + agent.getId());
            final Agent<?, ?, ?> loadedAgent = assetStorageService.find(agent.getId(), true, Agent.class);
            if (loadedAgent == null) {
                LOG.info("Agent not found in the DB, maybe it has been removed: " + agent.getId());
                return null;
            }
            agent = loadedAgent;
        }

        Agent<?, ?, ?> finalAgent = agent;
        withLock(getClass().getSimpleName() + "::addReplaceAgent", () -> getAgents().put(finalAgent.getId(), finalAgent));
        return agent;
    }

    @SuppressWarnings("ConstantConditions")
    protected boolean removeAgent(String agentId) {
        return withLockReturning(getClass().getSimpleName() + "::removeAgent", () -> getAgents().remove(agentId) != null);
    }

    public Agent<?, ?, ?> getAgent(String agentId) {
        return getAgents().get(agentId);
    }

    protected Map<String, Agent<?, ?, ?>> getAgents() {

        if (agentMap != null) {
            return agentMap;
        }

        return withLockReturning(getClass().getSimpleName() + "::getAgents", () -> {
            if (agentMap == null) {
                agentMap = assetStorageService.findAll(
                        new AssetQuery().types(Agent.class)
                    )
                    .stream()
                    .filter(asset -> gatewayService.getLocallyRegisteredGatewayId(asset.getId(), null) == null)
                    .collect(Collectors.toMap(Asset::getId, agent -> (Agent<?, ?, ?>)agent));
            }
            return agentMap;
        });
    }

    public Protocol<?> getProtocolInstance(Agent<?, ?, ?> agent) {
        return getProtocolInstance(agent.getId());
    }

    public Protocol<?> getProtocolInstance(String agentId) {
        return protocolInstanceMap.get(agentId);
    }

    @Override
    public void subscribeChildAssetChange(String agentId, Consumer<PersistenceEvent<Asset<?>>> assetChangeConsumer) {
        if (!getAgents().containsKey(agentId)) {
            LOG.info("Attempt to subscribe to child asset changes with an invalid agent ID: " +agentId);
            return;
        }

        withLock(getClass().getSimpleName() + "::subscribeChildAssetChange", () -> {
            List<Consumer<PersistenceEvent<Asset<?>>>> consumerList = childAssetSubscriptions
                .computeIfAbsent(agentId, (id) -> new ArrayList<>());
            if (!consumerList.contains(assetChangeConsumer)) {
                consumerList.add(assetChangeConsumer);
            }
        });
    }

    @Override
    public void unsubscribeChildAssetChange(String agentId, Consumer<PersistenceEvent<Asset<?>>> assetChangeConsumer) {
        withLock(getClass().getSimpleName() + "::unsubscribeChildAssetChange", () ->
            childAssetSubscriptions.computeIfPresent(agentId, (id, consumerList) -> {
                consumerList.remove(assetChangeConsumer);
                return consumerList.isEmpty() ? null : consumerList;
            }));
    }

    protected void notifyChildAssetChange(String agentId, PersistenceEvent<Asset<?>> assetPersistenceEvent) {
        withLock(getClass().getSimpleName() + "::notifyChildAssetChange", () ->
            childAssetSubscriptions.computeIfPresent(agentId, (id, consumerList) -> {
                LOG.finer("Notifying child asset change consumers of change to agent child asset: Agent ID=" + id + ", Asset<?> ID=" + assetPersistenceEvent.getEntity().getId());
                try {
                    consumerList.forEach(consumer -> consumer.accept(assetPersistenceEvent));
                } catch (Exception e) {
                    LOG.log(Level.WARNING, "Child asset change consumer threw an exception: Agent ID=" + id + ", Asset<?> ID=" + assetPersistenceEvent.getEntity().getId(), e);
                }
                return consumerList;
            }));
    }

    public boolean isProtocolAssetDiscoveryOrImportRunning(String agentId) {
        return agentDiscoveryImportFutureMap.containsKey(agentId);
    }

    public Future<Void> doProtocolInstanceDiscovery(String parentId, Class<? extends ProtocolInstanceDiscovery> instanceDiscoveryProviderClass, Consumer<Agent<?,?,?>[]> onDiscovered) {

        LOG.fine("Initiating protocol instance discovery: Provider = " + instanceDiscoveryProviderClass);

        Runnable task = () -> {
            if (parentId != null && gatewayService.getLocallyRegisteredGatewayId(parentId, null) != null) {
                // TODO: Implement gateway instance discovery using client event bus
                return;
            }

            try {
                ProtocolInstanceDiscovery instanceDiscovery = instanceDiscoveryProviderClass.getDeclaredConstructor().newInstance();
                Future<Void> discoveryFuture = instanceDiscovery.startInstanceDiscovery(onDiscovered);
                discoveryFuture.get();
            } catch (InterruptedException e) {
                LOG.info("Protocol instance discovery was cancelled");
            } catch (Exception e) {
                LOG.log(Level.WARNING, "Failed to do protocol instance discovery: Provider = " + instanceDiscoveryProviderClass, e);
            } finally {
                LOG.fine("Finished protocol instance discovery: Provider = " + instanceDiscoveryProviderClass);
            }
        };

        return executorService.submit(task, null);
    }

    public Future<Void> doProtocolAssetDiscovery(Agent<?, ?, ?> agent, Consumer<AssetTreeNode[]> onDiscovered) throws RuntimeException {

        Protocol<?> protocol = getProtocolInstance(agent.getId());

        if (protocol == null) {
            throw new UnsupportedOperationException("Agent is either invalid, disabled or mis-configured: " + agent);
        }

        if (!(protocol instanceof ProtocolAssetDiscovery)) {
            throw new UnsupportedOperationException("Agent protocol doesn't support asset discovery");
        }

        LOG.fine("Initiating protocol asset discovery: Agent = " + agent);

        synchronized (agentDiscoveryImportFutureMap) {
            okToContinueWithImportOrDiscovery(agent.getId());

            Runnable task = () -> {
                try {
                    if (gatewayService.getLocallyRegisteredGatewayId(agent.getId(), null) != null) {
                        // TODO: Implement gateway instance discovery using client event bus
                        return;
                    }

                    ProtocolAssetDiscovery assetDiscovery = (ProtocolAssetDiscovery) protocol;
                    Future<Void> discoveryFuture = assetDiscovery.startAssetDiscovery(onDiscovered);
                    discoveryFuture.get();
                } catch (InterruptedException e) {
                    LOG.info("Protocol asset discovery was cancelled");
                } catch (Exception e) {
                    LOG.log(Level.WARNING, "Failed to do protocol asset discovery: Agent = " + agent, e);
                } finally {
                    LOG.fine("Finished protocol asset discovery: Agent = " + agent);
                    agentDiscoveryImportFutureMap.remove(agent.getId());
                }
            };

            Future<Void> future = executorService.submit(task, null);
            agentDiscoveryImportFutureMap.put(agent.getId(), future);
            return future;
        }
    }

    public Future<Void> doProtocolAssetImport(Agent<?, ?, ?> agent, byte[] fileData, Consumer<AssetTreeNode[]> onDiscovered) throws RuntimeException {

        Protocol<?> protocol = getProtocolInstance(agent.getId());

        if (protocol == null) {
            throw new UnsupportedOperationException("Agent is either invalid, disabled or mis-configured: " + agent);
        }

        if (!(protocol instanceof ProtocolAssetImport)) {
            throw new UnsupportedOperationException("Agent protocol doesn't support asset import");
        }

        LOG.fine("Initiating protocol asset import: Agent = " + agent);
        synchronized (agentDiscoveryImportFutureMap) {
            okToContinueWithImportOrDiscovery(agent.getId());

            Runnable task = () -> {
                try {
                    if (gatewayService.getLocallyRegisteredGatewayId(agent.getId(), null) != null) {
                        // TODO: Implement gateway instance discovery using client event bus
                        return;
                    }

                    ProtocolAssetImport assetImport = (ProtocolAssetImport) protocol;
                    Future<Void> discoveryFuture = assetImport.startAssetImport(fileData, onDiscovered);
                    discoveryFuture.get();
                } catch (InterruptedException e) {
                    LOG.info("Protocol asset import was cancelled");
                } catch (Exception e) {
                    LOG.log(Level.WARNING, "Failed to do protocol asset import: Agent = " + agent, e);
                } finally {
                    LOG.fine("Finished protocol asset import: Agent = " + agent);
                    agentDiscoveryImportFutureMap.remove(agent.getId());
                }
            };

            Future<Void> future = executorService.submit(task, null);
            agentDiscoveryImportFutureMap.put(agent.getId(), future);
            return future;
        }
    }

    protected void okToContinueWithImportOrDiscovery(String agentId) {
        if (agentDiscoveryImportFutureMap.containsKey(agentId)) {
            String msg = "Protocol asset discovery or import already running for requested agent: " + agentId;
            LOG.info(msg);
            throw new IllegalStateException(msg);
        }
    }
}
