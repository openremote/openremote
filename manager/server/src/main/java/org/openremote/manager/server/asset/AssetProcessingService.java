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
package org.openremote.manager.server.asset;

import org.apache.camel.builder.RouteBuilder;
import org.openremote.agent.protocol.AbstractProtocol;
import org.openremote.agent.protocol.Protocol;
import org.openremote.container.Container;
import org.openremote.container.ContainerService;
import org.openremote.container.message.MessageBrokerService;
import org.openremote.container.message.MessageBrokerSetupService;
import org.openremote.container.timer.TimerService;
import org.openremote.manager.server.agent.AgentService;
import org.openremote.manager.server.datapoint.AssetDatapointService;
import org.openremote.manager.server.event.EventService;
import org.openremote.manager.server.rules.RulesEngine;
import org.openremote.manager.server.rules.RulesService;
import org.openremote.manager.server.security.ManagerIdentityService;
import org.openremote.manager.shared.asset.AssetProcessingException;
import org.openremote.manager.shared.security.ClientRole;
import org.openremote.manager.shared.security.Tenant;
import org.openremote.manager.shared.security.User;
import org.openremote.model.ValidationFailure;
import org.openremote.model.asset.*;
import org.openremote.model.attribute.AttributeEvent;
import org.openremote.model.attribute.AttributeExecuteStatus;
import org.openremote.model.event.shared.SharedEvent;
import org.openremote.model.value.Values;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.openremote.agent.protocol.Protocol.SENSOR_QUEUE;
import static org.openremote.manager.server.event.EventService.INCOMING_EVENT_TOPIC;
import static org.openremote.model.asset.agent.AgentLink.getAgentLink;

/**
 * Receives {@link AttributeEvent}s and processes them.
 * <p>
 * {@link AttributeEvent}s can come from various sources:
 * <ul>
 * <li>Protocol sensor updates ({@link AbstractProtocol#updateLinkedAttribute})</li>
 * <li>Protocol arbitrary updates ({@link AbstractProtocol#sendAttributeEvent})</li>
 * <li>User/Client REST initiated ({@link AssetResourceImpl#writeAttributeValue})</li>
 * <li>User/Client Web Socket initiated ({@link EventService})</li>
 * <li>Rules Engine ({@link org.openremote.model.rules.Assets#dispatch})</li>
 * </ul>
 * The {@link AttributeEvent}s are first validated using basic validation {@link #validateAttributeEvent} then they are
 * validated based on sender:
 * <ul>
 * <li>User/client - {@link #validateUserRequest}</li>
 * </ul>
 * If validation fails at any point then an {@link AssetProcessingException} will be thrown with a
 * {@link AssetProcessingException.Reason} indicating the cause of validation failure.
 * <p>
 * Once successfully validated the event is converted into an {@link AssetState} message which is then passed through
 * the processing chain of <code>Consumer&lt;AssetState&gt;</code> consumers.
 * <p>
 * The regular processing chain is:
 * <ul>
 * <li>{@link RulesService}</li>
 * <li>{@link AgentService}</li>
 * <li>{@link AssetStorageService}</li>
 * <li>{@link AssetDatapointService}</li>
 * </ul>
 * <h2>Rules Service processing logic</h2>
 * <p>
 * Checks if attribute is {@link AssetAttribute#isRuleState} or {@link AssetAttribute#isRuleEvent}, and if
 * it does then the message is passed through the rule engines that are in scope for the asset.
 * <p>
 * For {@link AssetState} messages, the rules service keeps the facts and thus the state of each rules
 * knowledge session in sync with the asset state changes that occur. If an asset attribute value changes,
 * the {@link AssetState} in each rules session will be updated to reflect the change.
 * <p>
 * For {@link AssetEvent} messages, they are inserted in the rules sessions in scope
 * and expired automatically either by a) the rules session if no time-pattern can possibly match the event source
 * timestamp anymore or b) by the rules service if the event lifetime set in {@link RulesService#RULE_EVENT_EXPIRES} is
 * reached or c) by the rules service if the event lifetime set in the attribute {@link AssetMeta#RULE_EVENT_EXPIRES}
 * is reached.
 * <h2>Agent service processing logic</h2>
 * <p>
 * When the {@link AssetState} originates from a protocol sensor update (i.e. generated by a call to
 * {@link org.openremote.agent.protocol.AbstractProtocol#updateLinkedAttribute} then the agent service will ignore the
 * message.
 * <p>
 * When the {@link AssetState} originates from any other source (including a protocol for arbitrary attribute updates)
 * then the agent service will validate that the attribute has a valid {@link AssetMeta#AGENT_LINK}.
 * <p>
 * If it is not linked to an agent then it ignores the message.
 * <p>
 * If it is linked to an agent and the agent link is invalid then the message status is set to
 * {@link AssetState.ProcessingStatus#HANDLED} and the message will not be able to progress through the processing chain.
 * <p>
 * If the message is for a valid linked agent then an {@link AttributeEvent} is sent on the
 * {@link org.openremote.agent.protocol.AbstractProtocol#ACTUATOR_TOPIC} which the protocol will receive in
 * {@link org.openremote.agent.protocol.AbstractProtocol#processLinkedAttributeWrite} for execution on an actual device or
 * service 'things'.
 * <p>
 * This means that a protocol implementation is responsible for producing a new {@link AttributeEvent} to
 * indicate to the system that the attribute value has/has not changed. The protocol should know best when to
 * do this and will vary from protocol to protocol; some 'things' might respond to an actuator command immediately
 * with a new sensor read, or they might send a separate sensor changed message or both or neither (fire and
 * forget). The protocol must decide what the best course of action is based on the 'things' it communicates with
 * and the transport layer it uses etc.
 * <h2>Asset Storage Service processing logic</h2>
 * <p>
 * Always tries to persist the attribute value in the DB and allows the message to continue if the commit was
 * successful.
 * <h2>Asset Datapoint Service processing logic</h2>
 * <p>
 * Checks if attribute has {@link org.openremote.model.asset.AssetMeta#STORE_DATA_POINTS} meta item with a value of true
 * and if it does then the {@link AttributeEvent} is stored in a time series DB. Then allows the message to continue
 * if the commit was successful.
 */
public class AssetProcessingService extends RouteBuilder implements ContainerService {

    private static final Logger LOG = Logger.getLogger(AssetProcessingService.class.getName());

    // TODO: Some of these options should be configurable depending on expected load etc.
    // Message topic for communicating from client to asset/thing layer
    public static final String ASSET_QUEUE = "seda://AssetQueue?waitForTaskToComplete=NEVER&purgeWhenStopping=true&discardIfNoConsumers=false&size=1000";

    protected TimerService timerService;
    protected ManagerIdentityService managerIdentityService;
    protected RulesService rulesService;
    protected AgentService agentService;
    protected AssetStorageService assetStorageService;
    protected AssetDatapointService assetDatapointService;
    protected MessageBrokerService messageBrokerService;
    protected EventService eventService;
    // Used in testing to detect if initial/startup processing has completed
    protected long lastProcessedEventTimestamp = System.currentTimeMillis();

    final protected List<Consumer<AssetState>> processors = new ArrayList<>();

    @Override
    public void init(Container container) throws Exception {
        timerService = container.getService(TimerService.class);
        managerIdentityService = container.getService(ManagerIdentityService.class);
        rulesService = container.getService(RulesService.class);
        agentService = container.getService(AgentService.class);
        assetStorageService = container.getService(AssetStorageService.class);
        assetDatapointService = container.getService(AssetDatapointService.class);
        messageBrokerService = container.getService(MessageBrokerService.class);
        eventService = container.getService(EventService.class);

        eventService.addSubscriptionAuthorizer((auth, subscription) -> {
            if (!subscription.isEventType(AttributeEvent.class)) {
                return false;
            }

            // Always must have a filter, as you can't subscribe to ALL asset attribute events
            if (subscription.getFilter() != null && subscription.getFilter() instanceof AttributeEvent.EntityIdFilter) {
                AttributeEvent.EntityIdFilter filter = (AttributeEvent.EntityIdFilter) subscription.getFilter();

                // If the asset doesn't exist, subscription must fail
                Asset asset = assetStorageService.find(filter.getEntityId());
                if (asset == null)
                    return false;

                // Superuser can get attribute events for any asset
                if (auth.isSuperUser())
                    return true;

                // Regular user must have role
                if (!auth.isUserInRole(ClientRole.READ_ASSETS.getValue())) {
                    return false;
                }

                if (managerIdentityService.isRestrictedUser(auth.getUserId())) {
                    // Restricted users can only get attribute events for their linked assets
                    if (assetStorageService.isUserAsset(auth.getUserId(), filter.getEntityId()))
                        return true;
                } else {
                    // Regular users can only get attribute events for assets in their realm
                    if (asset.getTenantRealm().equals(auth.getAuthenticatedRealm()))
                        return true;
                }
            }
            return false;
        });

        processors.add(rulesService);
        processors.add(agentService);
        processors.add(assetStorageService);
        processors.add(assetDatapointService);

        container.getService(MessageBrokerSetupService.class).getContext().addRoutes(this);
    }

    @Override
    public void start(Container container) throws Exception {

    }

    @Override
    public void stop(Container container) throws Exception {

    }

    @Override
    public void configure() throws Exception {

        // Process new sensor values from protocols, must be events for
        // attributes linked to an agent's protocol configurations
        from(SENSOR_QUEUE)
            .filter(body().isInstanceOf(AttributeEvent.class))
            .process(exchange -> {
                AttributeEvent event = exchange.getIn().getBody(AttributeEvent.class);
                Protocol protocol = exchange.getIn().getHeader(
                    SharedEvent.HEADER_SENDER, Protocol.class
                );

                if (protocol == null) {
                    LOG.warning("Sensor update received from a protocol but the protocol is not included in the header");
                    return;
                }

                LOG.fine("Received from protocol '" + protocol.getProtocolName() + "' on sensor queue: " + event);
                ServerAsset asset = assetStorageService.find(event.getEntityId(), true);

                // Get the attribute and check it is actually linked to an agent (although the
                // event comes from a Protocol, we can not assume that the attribute is still linked,
                // consider a protocol that receives a batch of messages because a gateway was offline
                // for a day)
                AssetAttribute attribute = asset.getAttribute(event.getAttributeName()).orElse(null);

                validateAttributeEvent(event, asset, attribute, true);

                AssetAttribute protocolConfiguration =
                    getAgentLink(attribute)
                        .flatMap(agentService::getProtocolConfiguration)
                        .orElse(null);

                if (protocolConfiguration == null) {
                    LOG.warning(
                        "Processing sensor update from protocol '" + protocol.getProtocolName()
                            + "' failed, linked agent protocol configuration not found: " + event
                    );
                    return;
                }

                if (!protocol.getLinkedProtocolConfigurations().contains(protocolConfiguration)) {
                    LOG.warning("Protocol isn't linked to this protocol configuration: " + protocolConfiguration.getReferenceOrThrow());
                    return;
                }

                // Process as non-client source (not southbound)
                processUpdate(asset, attribute, event, true, protocol);

            });

        // All attribute events (except protocol sensor updates) come through here
        from(ASSET_QUEUE)
            .filter(body().isInstanceOf(AttributeEvent.class))
            .process(exchange -> {
                AttributeEvent event = exchange.getIn().getBody(AttributeEvent.class);
                Object sender = exchange.getIn().getHeader(SharedEvent.HEADER_SENDER);
                LOG.fine("Received from '" + sender + "' on asset queue: " + event);

                ServerAsset asset = assetStorageService.find(event.getEntityId(), true);
                AssetAttribute attribute = asset == null ? null : asset.getAttribute(event.getAttributeName()).orElse(null);
                validateAttributeEvent(event, asset, attribute, false);

                if (sender instanceof User) {
                    // This is from southbound so ensure user can perform the update
                    User user = (User)sender;
                    validateUserRequest(event, asset, user);
                } else if (sender instanceof Protocol) {
                    // TODO: implement any protocol specific validation
                } else if (sender instanceof RulesEngine) {
                    // TODO: implement any rules specific validation
                }

                processUpdate(asset, attribute, event, false, sender);
            });

        // Client initiated attribute events - sender should always be a user
        from(INCOMING_EVENT_TOPIC)
            .filter(body().isInstanceOf(AttributeEvent.class))
            .filter(header(SharedEvent.HEADER_SENDER).isInstanceOf(User.class))
            .process(exchange -> {
                AttributeEvent event = exchange.getIn().getBody(AttributeEvent.class);
                User user = exchange.getIn().getHeader(SharedEvent.HEADER_SENDER, User.class);

                LOG.fine("Handling from client: " + event);

                if (event.getEntityId() == null || event.getEntityId().isEmpty() || user == null)
                    return;

                // Put it on the asset queue
                sendAttributeEvent(event, user);
            });
    }

    /**
     * Send attribute change events into the {@link #ASSET_QUEUE}.
     *
     * The sender indicates the source of the attribute event this should be one of:
     * <ul>
     * <li>{@link org.openremote.manager.shared.security.User} - From user/client</li>
     * <li>{@link Protocol} - From a protocol</li>
     * <li>{@link org.openremote.manager.server.rules.RulesEngine} - From rules</li>
     * </ul>
     */
    public void sendAttributeEvent(AttributeEvent attributeEvent, Object sender) {
        messageBrokerService.getProducerTemplate().sendBodyAndHeader(
            ASSET_QUEUE,
            attributeEvent,
            SharedEvent.HEADER_SENDER,
            sender
        );
    }

    // TODO: Remove this once AssetResourceImpl can get exceptions from camel route
    public void sendAttributeEventWithoutCamel(AttributeEvent event, Object sender) {
        LOG.fine("Received from '" + sender + "' on asset queue: " + event);

        ServerAsset asset = assetStorageService.find(event.getEntityId(), true);
        AssetAttribute attribute = asset == null ? null : asset.getAttribute(event.getAttributeName()).orElse(null);
        validateAttributeEvent(event, asset, attribute, false);

        if (sender instanceof User) {
            // This is from southbound so ensure user can perform the update
            User user = (User)sender;
            validateUserRequest(event, asset, user);
        } else if (sender instanceof Protocol) {
            // TODO: implement any protocol specific validation
        } else if (sender instanceof RulesEngine) {
            // TODO: implement any rules specific validation
        }

        processUpdate(asset, attribute, event, false, sender);
    }

    // TODO: Update tests to include sender and remove this
    protected void sendAttributeEvent(AttributeEvent attributeEvent) {
        sendAttributeEvent(attributeEvent, null);
    }

    /**
     * Performs basic validation of all {@link AttributeEvent}s:
     * <ol>
     * <li>Check event refers to a valid asset</li>
     * <li>Check event refers to a valid attribute</li>
     * <li>Check event doesn't refer to an {@link AssetType#AGENT} Asset</li>
     * <li>Check that events for executable attributes are of type {@link AttributeExecuteStatus} and are
     * {@link AttributeExecuteStatus#isWrite} (unless this is a sensor update)</li>
     * </ol>
     */
    protected void validateAttributeEvent(AttributeEvent event, Asset asset, AssetAttribute attribute, boolean isSensorUpdate) throws AssetProcessingException {

        if (asset == null) {
            LOG.warning("Asset doesn't exist: " + event.getAttributeRef());
            throw new AssetProcessingException(AssetProcessingException.Reason.ASSET_NOT_FOUND);
        }

        if (attribute == null) {
            LOG.warning("Attribute doesn't exist: " + event.getAttributeRef());
            throw new AssetProcessingException(AssetProcessingException.Reason.ATTRIBUTE_NOT_FOUND);
        }

        // Prevent editing of individual agent attributes
        if (asset.getWellKnownType() == AssetType.AGENT) {
            LOG.warning("Agent attributes can not be updated individually, update the whole asset instead: " + asset);
            throw new AssetProcessingException(AssetProcessingException.Reason.INVALID_ACTION);
        }

        // Only sensor updates can send AttributeExecuteStatus marked as read values
        if (!isSensorUpdate && attribute.isExecutable()) {
            Optional<AttributeExecuteStatus> status = event.getValue()
                .flatMap(Values::getString)
                .flatMap(AttributeExecuteStatus::fromString);

            if (!status.isPresent()) {
                LOG.warning("Attribute event doesn't contain a valid AttributeExecuteStatus value: " + event);
                throw new AssetProcessingException(AssetProcessingException.Reason.INVALID_ACTION);
            }

            if (!status.get().isWrite()) {
                LOG.warning("Only AttributeExecuteStatus write value can be written to an attribute: " + event);
                throw new AssetProcessingException(AssetProcessingException.Reason.INVALID_ACTION);
            }
        }
    }

    /**
     * Performs validation of {@link AttributeEvent}s sent by users/clients:
     * <p>
     * All users (including super users):
     * <ol>
     * <li>Check asset's realm is valid and active</li>
     * <li>Check that the event is not for a read only attribute (not even super users are allowed to write to readonly attributes)</li>
     * </ol>
     * Additional checks for non super users:
     * <ol>
     * <li>User must have {@link ClientRole#WRITE_ASSETS} role</li>
     * <li>User must be in the same realm as the asset</li>
     * <li>Restricted users can only write to assets linked to them</li>
     * <li>Restricted users can only write to attributes marked as protected</li>
     * </ol>
     *
     */
    protected void validateUserRequest(AttributeEvent event, Asset asset, User user) throws AssetProcessingException {

        // Attribute presence has already been verified
        //noinspection ConstantConditions
        AssetAttribute attribute = asset.getAttribute(event.getAttributeName()).get();

        // Check realm is active and accessible
        Tenant tenant = managerIdentityService.getTenant(asset.getRealmId());
        if (tenant == null || !tenant.isActive()) {
            throw new AssetProcessingException(AssetProcessingException.Reason.INVALID_ACTION);
        }

        // Prevent editing of read only attributes
        if (attribute.isReadOnly()) {
            LOG.warning("Ignoring southbound " + event + ", attribute is read-only in: " + asset);
            throw new AssetProcessingException(AssetProcessingException.Reason.INVALID_ACTION);
        }

        // All other checks only apply to non super user
        if (user.isSuperUser()) {
            return;
        }

        // Regular user must have write assets role
        if (!user.hasRole(ClientRole.WRITE_ASSETS.getValue())) {
            throw new AssetProcessingException(AssetProcessingException.Reason.INSUFFICIENT_ACCESS);
        }

        // Regular users can only write attribute events for assets in their realm
        if (!asset.getTenantRealm().equals(user.getRealm())) {
            throw new AssetProcessingException(AssetProcessingException.Reason.INSUFFICIENT_ACCESS);
        }

        // Restricted users can only write attribute events for their linked assets
        if (user.isRestricted() && !assetStorageService.isUserAsset(user.getId(), asset.getId())) {
            throw new AssetProcessingException(AssetProcessingException.Reason.INSUFFICIENT_ACCESS);
        }

        // Restricted users can only write attribute events for protected attributes
        if (user.isRestricted() && !attribute.isProtected()) {
            throw new AssetProcessingException(AssetProcessingException.Reason.INSUFFICIENT_ACCESS);
        }
    }

    /*
     * This deals with single attribute value changes and pushes them through the attribute event
     * processing chain where each consumer is given the opportunity to consume the event or allow
     * it progress to the next consumer {@link AssetState.ProcessingStatus}.
     * <p>
     * NOTE: An attribute value can be changed during Asset CRUD but this does not come through
     * this route but is handled separately see {@link AssetResourceImpl}. Any attribute values
     * assigned during Asset CRUD can be thought of as the attributes initial value and is subject
     * to change by the following actors (depending on attribute meta etc.) All actors use this
     * entry point to initiate an attribute value change: Sensor updates from protocols, attribute
     * write requests from protocols, attribute write requests from clients, and attribute write
     * dispatching as rules RHS action.
     */
    protected void processUpdate(ServerAsset asset,
                                 AssetAttribute attribute,
                                 AttributeEvent attributeEvent,
                                 boolean isSensorUpdate,
                                 Object sender) {
        // Ensure timestamp of event is not in the future as that would essentially block access to
        // the attribute until after that time (maybe that is desirable behaviour)
        // Allow a leniency of 1s
        long currentMillis = timerService.getCurrentTimeMillis();
        if (attributeEvent.getTimestamp() - currentMillis > 1000) {
            // TODO: Decide how to handle update events in the future - ignore or change timestamp
            LOG.warning("Ignoring future " + attributeEvent
                + ", current time: " + new Date(currentMillis) + "/" + currentMillis
                + ", event time: " + new Date(attributeEvent.getTimestamp()) + "/" + attributeEvent.getTimestamp() + " in: " + asset);
            return;
        }

        // Hold on to existing attribute state so we can use it during processing
        Optional<AttributeEvent> lastStateEvent = attribute.getStateEvent();

        // Check the last update timestamp of the attribute, ignoring any event that is older than last update
        // TODO: This means we drop out-of-sequence events, we might need better at-least-once handling
        if (lastStateEvent.isPresent() && lastStateEvent.get().getTimestamp() >= 0 && attributeEvent.getTimestamp() <= lastStateEvent.get().getTimestamp()) {
            LOG.warning("Ignoring outdated " + attributeEvent
                + ", last asset state time: " + lastStateEvent.map(event -> new Date(event.getTimestamp()).toString()).orElse("-1") + "/" + lastStateEvent.map(AttributeEvent::getTimestamp).orElse(-1L)
                + ", event time: " + new Date(attributeEvent.getTimestamp()) + "/" + attributeEvent.getTimestamp() + " in: " + asset);

            return;
        }

        // Set new value and event timestamp on attribute
        attribute.setValue(attributeEvent.getValue().orElse(null), attributeEvent.getTimestamp());

        // Validate constraints of attribute
        List<ValidationFailure> validationFailures = attribute.getValidationFailures();
        if (!validationFailures.isEmpty()) {
            LOG.warning("Validation failure(s) " + validationFailures + ", can't process update of: " + attribute);
            return;
        }


        // Push as an asset state through the processing chain
        AssetState assetState = new AssetState(
                asset,
                attribute,
                lastStateEvent.flatMap(AttributeEvent::getValue).orElse(null),
                lastStateEvent.map(AttributeEvent::getTimestamp).orElse(-1L),
                isSensorUpdate,
                sender);

        try {
            LOG.fine(">>> Processing start " +
                "(event time: " + new Date(assetState.getValueTimestamp()) + "/" + assetState.getValueTimestamp() +
                ", processing time: " + new Date(currentMillis) + "/" + currentMillis
                + ") : " + assetState);
            processorLoop:
            for (Consumer<AssetState> processor : processors) {
                try {
                    LOG.fine("==> Processor " + processor + " accepts: " + assetState);
                    processor.accept(assetState);
                } catch (Throwable t) {
                    LOG.log(Level.SEVERE, "Asset update consumer '" + processor + "' threw an exception whilst consuming: " + assetState, t);
                    assetState.setProcessingStatus(AssetState.ProcessingStatus.ERROR);
                    assetState.setError(t);
                }

                switch (assetState.getProcessingStatus()) {
                    case HANDLED:
                        LOG.fine("<== Processor " + processor + " finally handled: " + assetState);
                        break processorLoop;
                    case ERROR:
                        LOG.log(Level.SEVERE, "Processor " + processor + " error: " + assetState, assetState.getError());
                        break processorLoop;
                }
            }
            if (assetState.getProcessingStatus() != AssetState.ProcessingStatus.ERROR) {
                assetState.setProcessingStatus(AssetState.ProcessingStatus.COMPLETED);
                publishEvent(assetState);
            }

        } finally {
            lastProcessedEventTimestamp = System.currentTimeMillis();
            LOG.fine("<<< Processing complete: " + assetState);
        }
    }

    protected void publishEvent(AssetState assetState) {
        eventService.publishEvent(
            new AttributeEvent(assetState.getId(), assetState.getAttributeName(), assetState.getValue())
        );
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
            '}';
    }
}
