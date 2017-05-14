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

import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.openremote.agent.protocol.Protocol;
import org.openremote.container.Container;
import org.openremote.container.ContainerService;
import org.openremote.container.message.MessageBrokerService;
import org.openremote.container.message.MessageBrokerSetupService;
import org.openremote.container.security.AuthContext;
import org.openremote.container.timer.TimerService;
import org.openremote.manager.server.agent.AgentService;
import org.openremote.manager.server.datapoint.AssetDatapointService;
import org.openremote.manager.server.event.EventService;
import org.openremote.manager.server.rules.RulesService;
import org.openremote.manager.server.security.ManagerIdentityService;
import org.openremote.manager.shared.asset.AssetProcessingException;
import org.openremote.manager.shared.security.ClientRole;
import org.openremote.model.Constants;
import org.openremote.model.ValidationFailure;
import org.openremote.model.asset.*;
import org.openremote.model.attribute.AttributeEvent;
import org.openremote.model.attribute.AttributeEvent.Source;

import java.util.*;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.openremote.agent.protocol.Protocol.SENSOR_QUEUE;
import static org.openremote.manager.server.asset.AssetRoute.*;
import static org.openremote.manager.server.event.EventService.INCOMING_EVENT_TOPIC;
import static org.openremote.model.attribute.AttributeEvent.HEADER_SOURCE;
import static org.openremote.model.attribute.AttributeEvent.Source.CLIENT;
import static org.openremote.model.attribute.AttributeEvent.Source.INTERNAL;

/**
 * Receives {@link AttributeEvent}s and processes them.
 * <p>
 * {@link AttributeEvent}s can come from various {@link AttributeEvent.Source}s:
 * <ul>
 * <li>Events sent to {@link #ASSET_QUEUE} or through {@link #sendAttributeEvent} convenience methods</li>
 * <li>Client event bus write requests received on {@link EventService#INCOMING_EVENT_TOPIC}</li>
 * <li>Protocol sensor updates sent to {@link Protocol#SENSOR_QUEUE}</li>
 * </ul>
 * <p>
 * The {@link AttributeEvent}s are first validated depending on their source, and if validation fails
 * at any point then an {@link AssetProcessingException} will be logged as a warning with an
 * {@link AssetProcessingException.Reason}.
 * <p>
 * Once successfully validated the event is converted into an {@link AssetState} message which is then passed through
 * the processing chain of consumers.
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
 * so the message is passed through the rule engines that are in scope for the asset.
 * <p>
 * For {@link AssetState} messages, the rules service keeps the facts and thus the state of each rules
 * knowledge session in sync with the asset state changes that occur. If an asset attribute value changes,
 * the {@link AssetState} in each rules session will be updated to reflect the change.
 * <p>
 * For {@link AssetEvent} messages (obtained by converting the {@link AssetState}), they are inserted in the rules
 * sessions in scope and expired automatically either by a) the rules session if no time-pattern can possibly match
 * the event source timestamp anymore or b) by the rules service if the event lifetime set in
 * {@link RulesService#RULE_EVENT_EXPIRES} is reached or c) by the rules service if the event lifetime set in the
 * attribute {@link AssetMeta#RULE_EVENT_EXPIRES} is reached.
 * <h2>Agent service processing logic</h2>
 * <p>
 * The agent service's role is to communicate asset attribute writes to actuators, through protocols.
 * When the update messages' source is {@link AttributeEvent.Source#SENSOR}, the agent service ignores the message.
 * The message will also be ignored if the updated attribute is not linked to a protocol configuration.
 * <p>
 * If the updated attribute has an invalid agent link, the message status is set to {@link AssetState.ProcessingStatus#ERROR}.
 * <p>
 * If the updated attribute has a valid agent link, an {@link AttributeEvent} is sent on the {@link Protocol#ACTUATOR_TOPIC},
 * for execution on an actual device or service 'things'.
 * <p>
 * This means that a protocol implementation is responsible for producing a new {@link AttributeEvent} to
 * indicate to the system that the attribute value has/has not changed. The protocol should know best when to
 * do this and it will vary from protocol to protocol; some 'things' might respond to an actuator write immediately
 * with a new sensor read, or they might send a later "sensor changed" message or both or neither (fire and
 * forget). The protocol must decide what the best course of action is based on the 'things' it communicates with
 * and the transport layer it uses etc.
 * <h2>Asset Storage Service processing logic</h2>
 * <p>
 * Always tries to persist the attribute value in the database and allows the message to continue if the commit was
 * successful.
 * <h2>Asset Datapoint Service processing logic</h2>
 * <p>
 * Checks if attribute is {@link AssetAttribute#isStoreDatapoints()}, and if so the {@link AttributeEvent} is stored
 * is stored in a time series of historical data. Then allows the message to continue if the commit was successful.
 */
public class AssetProcessingService extends RouteBuilder implements ContainerService {

    private static final Logger LOG = Logger.getLogger(AssetProcessingService.class.getName());

    // TODO: Some of these options should be configurable depending on expected load etc.
    // Message topic for communicating individual asset attribute changes
    String ASSET_QUEUE = "seda://AssetQueue?waitForTaskToComplete=IfReplyExpected&timeout=10000&purgeWhenStopping=true&discardIfNoConsumers=false&size=1000";

    protected TimerService timerService;
    protected ManagerIdentityService identityService;
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
        identityService = container.getService(ManagerIdentityService.class);
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
                if (!auth.hasResourceRole(ClientRole.READ_ASSETS.getValue(), Constants.KEYCLOAK_CLIENT_ID)) {
                    return false;
                }

                if (identityService.isRestrictedUser(auth.getUserId())) {
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

        // A client wants to write attribute state through event bus
        from(INCOMING_EVENT_TOPIC)
            .routeId("FromClientUpdates")
            .filter(body().isInstanceOf(AttributeEvent.class))
            .setHeader(HEADER_SOURCE, () -> CLIENT)
            .to(ASSET_QUEUE);

        // A protocol wants to write a new sensor value
        from(SENSOR_QUEUE)
            .routeId("FromSensorUpdates")
            .filter(body().isInstanceOf(AttributeEvent.class))
            .setHeader(HEADER_SOURCE, () -> Source.SENSOR)
            .to(ASSET_QUEUE);

        // Process attribute events
        from(ASSET_QUEUE)
            .routeId("AssetUpdates")
            .filter(body().isInstanceOf(AttributeEvent.class))
            .doTry()
            .process(extractAttributeEventDetails(assetStorageService))
            .process(validateAttributeEvent())
            .choice()
            .when(header(HEADER_SOURCE).isEqualTo(CLIENT))
            .process(validateAttributeEventFromClient(assetStorageService, identityService))
            .when(header(HEADER_SOURCE).isEqualTo(Source.SENSOR))
            .process(validateAttributeEventFromSensor(agentService))
            .end()
            .process(this::processAttributeEvent)
            .endDoTry()
            .doCatch(AssetProcessingException.class)
            .process(handleAssetProcessingException(LOG));
    }

    /**
     * Send attribute change events into the {@link #ASSET_QUEUE}, from {@link AttributeEvent.Source#CLIENT}.
     * The calling thread will block until processing is complete or queue default timeout is reached. The
     * {@link AuthContext} is used to check if the attribute update is permissible for the client. An
     * {@link AssetProcessingException} will be thrown if processing failed.
     */
    public void processFromClient(AuthContext authContext, AttributeEvent attributeEvent) throws AssetProcessingException {
        Map<String, Object> headers = new HashMap<>();
        headers.put(HEADER_SOURCE, CLIENT);
        headers.put(Constants.AUTH_CONTEXT, authContext);

        Object result = messageBrokerService.getProducerTemplate().requestBodyAndHeaders(
            ASSET_QUEUE, attributeEvent, headers
        );

        if (result instanceof AssetProcessingException) {
            throw (AssetProcessingException) result;
        }
    }

    /**
     * Send attribute change events into the {@link #ASSET_QUEUE}, from {@link AttributeEvent.Source#INTERNAL}.
     */
    public void sendAttributeEvent(AttributeEvent attributeEvent) {
        sendAttributeEvent(attributeEvent, INTERNAL);
    }

    /**
     * Send attribute change events into the {@link #ASSET_QUEUE}.
     */
    public void sendAttributeEvent(AttributeEvent attributeEvent, AttributeEvent.Source source) {
        messageBrokerService.getProducerTemplate().sendBodyAndHeader(
            ASSET_QUEUE,
            attributeEvent,
            HEADER_SOURCE,
            source
        );
    }

    /**
     * This deals with single attribute value changes and pushes them through the attribute event
     * processing chain where each consumer is given the opportunity to consume the event or allow
     * its progress to the next consumer, see {@link AssetState.ProcessingStatus}.
     * <p>
     * NOTE: An attribute value can be changed during Asset CRUD but this does not come through
     * this route but is handled separately see {@link AssetResourceImpl}. Any attribute values
     * assigned during Asset CRUD can be thought of as the attributes initial value and is subject
     * to change by the following actors (depending on attribute meta etc.) All actors use this
     * entry point to initiate an attribute value change: Sensor updates from protocols, attribute
     * write requests from protocols, attribute write requests from clients, and attribute write
     * dispatching as rules RHS action.
     * <p>
     * TODO: This can be migrated to Camel route as well
     */
    protected void processAttributeEvent(Exchange exchange) {

        AttributeEvent event = exchange.getIn().getBody(AttributeEvent.class);
        AttributeEvent.Source source = exchange.getIn().getHeader(HEADER_SOURCE, AttributeEvent.Source.class);
        ServerAsset asset = exchange.getIn().getHeader(HEADER_ASSET, ServerAsset.class);
        AssetAttribute attribute = exchange.getIn().getHeader(HEADER_ATTRIBUTE, AssetAttribute.class);

        long eventTime = event.getTimestamp();
        long processingTime = timerService.getCurrentTimeMillis();

        try {
            LOG.fine(">>> Processing start, source " + source +
                " (event time: " + new Date(eventTime) + "/" + eventTime +
                ", processing time: " + new Date(processingTime) + "/" + processingTime
                + ") : " + event);

            // Ensure timestamp of event is not in the future as that would essentially block access to
            // the attribute until after that time (maybe that is desirable behaviour)
            // Allow a leniency of 1s
            if (eventTime - processingTime > 1000) {
                // TODO: Decide how to handle update events in the future - ignore or change timestamp
                LOG.warning("Ignoring future " + event
                    + ", current time: " + new Date(processingTime) + "/" + processingTime
                    + ", event time: " + new Date(eventTime) + "/" + eventTime + " in: " + asset);
                return;
            }

            // Hold on to existing attribute state so we can use it during processing
            Optional<AttributeEvent> lastStateEvent = attribute.getStateEvent();

            // Check the last update timestamp of the attribute, ignoring any event that is older than last update
            // TODO: This means we drop out-of-sequence events, we might need better at-least-once handling
            if (lastStateEvent.isPresent() && lastStateEvent.get().getTimestamp() >= 0 && eventTime <= lastStateEvent.get().getTimestamp()) {
                LOG.warning("Ignoring outdated " + event
                    + ", last asset state time: " + lastStateEvent.map(e -> new Date(e.getTimestamp()).toString()).orElse("-1") + "/" + lastStateEvent.map(AttributeEvent::getTimestamp).orElse(-1L)
                    + ", event time: " + new Date(eventTime) + "/" + eventTime + " in: " + asset);

                return;
            }

            // Set new value and event timestamp on attribute
            attribute.setValue(event.getValue().orElse(null), eventTime);

            // Validate constraints of attribute
            List<ValidationFailure> validationFailures = attribute.getValidationFailures();
            if (!validationFailures.isEmpty()) {
                LOG.warning("Validation failure(s) " + validationFailures + ", can't process update of: " + attribute);
                return;
            }

            AssetState assetState = new AssetState(
                asset,
                attribute,
                lastStateEvent.flatMap(AttributeEvent::getValue).orElse(null),
                lastStateEvent.map(AttributeEvent::getTimestamp).orElse(-1L),
                source
            );

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
                        LOG.log(Level.SEVERE, "<== Processor " + processor + " error: " + assetState, assetState.getError());
                        break processorLoop;
                    default:
                        LOG.fine("<== Processor " + processor + " done with: " + assetState);
                }
            }
            if (assetState.getProcessingStatus() != AssetState.ProcessingStatus.ERROR) {
                assetState.setProcessingStatus(AssetState.ProcessingStatus.COMPLETED);
                eventService.publishEvent(
                    new AttributeEvent(assetState.getId(), assetState.getAttributeName(), assetState.getValue())
                );
            }

        } finally {
            lastProcessedEventTimestamp = System.currentTimeMillis();
            LOG.fine("<<< Processing complete: " + event);
        }
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
            '}';
    }
}
