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
package org.openremote.manager.asset;

import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.openremote.agent.protocol.Protocol;
import org.openremote.container.Container;
import org.openremote.container.ContainerService;
import org.openremote.container.message.MessageBrokerService;
import org.openremote.container.message.MessageBrokerSetupService;
import org.openremote.container.security.AuthContext;
import org.openremote.container.timer.TimerService;
import org.openremote.manager.agent.AgentService;
import org.openremote.manager.datapoint.AssetDatapointService;
import org.openremote.manager.event.ClientEventService;
import org.openremote.manager.rules.RulesService;
import org.openremote.manager.security.ManagerIdentityService;
import org.openremote.model.asset.AssetProcessingException;
import org.openremote.model.asset.AssetResource;
import org.openremote.model.Constants;
import org.openremote.model.asset.*;
import org.openremote.model.attribute.AttributeEvent;
import org.openremote.model.attribute.AttributeEvent.Source;
import org.openremote.model.security.ClientRole;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.openremote.manager.asset.AssetRoute.*;
import static org.openremote.manager.event.ClientEventService.CLIENT_EVENT_TOPIC;
import static org.openremote.model.attribute.AttributeEvent.HEADER_SOURCE;
import static org.openremote.model.attribute.AttributeEvent.Source.CLIENT;
import static org.openremote.model.attribute.AttributeEvent.Source.INTERNAL;

/**
 * Receives {@link AttributeEvent}s from {@link AttributeEvent.Source}s and processes them.
 * <dl>
 * <dt>{@link AttributeEvent.Source#CLIENT}</dt>
 * <dd><p>Client events published through event bus or sent by web service. These exchanges must contain an {@link AuthContext}
 * header named {@link Constants#AUTH_CONTEXT}.</dd>
 * <dt>{@link AttributeEvent.Source#INTERNAL}</dt>
 * <dd><p>Events sent to {@link #ASSET_QUEUE} or through {@link #sendAttributeEvent} convenience method.</dd>
 * <dt>{@link AttributeEvent.Source#SENSOR}</dt>
 * <dd><p>Protocol sensor updates sent to {@link Protocol#SENSOR_QUEUE}.</dd>
 * </dl>
 * NOTE: An attribute value can be changed during Asset CRUD but this does not come through
 * this route but is handled separately, see {@link AssetResource}. Any attribute values
 * assigned during Asset CRUD can be thought of as the attributes initial value.
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
 * <li>{@link AgentService}</li>
 * <li>{@link RulesService}</li>
 * <li>{@link AssetStorageService}</li>
 * <li>{@link AssetDatapointService}</li>
 * </ul>
 * <h2>Agent service processing logic</h2>
 * <p>
 * The agent service's role is to communicate asset attribute writes to actuators, through protocols.
 * When the update messages' source is {@link AttributeEvent.Source#SENSOR}, the agent service ignores the message.
 * The message will also be ignored if the updated attribute is not linked to a protocol configuration.
 * <p>
 * If the updated attribute has an invalid agent link, the message status is set to {@link AssetState.ProcessingStatus#ERROR}.
 * <p>
 * If the updated attribute has a valid agent link, an {@link AttributeEvent} is sent on the {@link Protocol#ACTUATOR_TOPIC},
 * for execution on an actual device or service 'things'. The update is then marked as
 * {@link AssetState.ProcessingStatus#COMPLETED} and no further processing is necessary. The update will not reach the
 * rules engine or the database processors.
 * <p>
 * This means that a protocol implementation is responsible for producing a new {@link AttributeEvent} to
 * indicate to the rules and database memory that the attribute value has/has not changed. The protocol should know
 * best when to do this and it will vary from protocol to protocol; some 'things' might respond to an actuator write
 * immediately with a new sensor read, or they might send a later "sensor changed" message or both or neither (fire and
 * forget). The protocol must decide what the best course of action is based on the 'things' it communicates with
 * and the transport layer it uses etc.
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
    public static final String ASSET_QUEUE = "seda://AssetQueue?waitForTaskToComplete=IfReplyExpected&timeout=10000&purgeWhenStopping=true&discardIfNoConsumers=false&size=25000";

    protected TimerService timerService;
    protected ManagerIdentityService identityService;
    protected RulesService rulesService;
    protected AgentService agentService;
    protected AssetStorageService assetStorageService;
    protected AssetDatapointService assetDatapointService;
    protected AssetAttributeLinkingService assetAttributeLinkingService;
    protected MessageBrokerService messageBrokerService;
    protected ClientEventService clientEventService;
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
        assetAttributeLinkingService = container.getService(AssetAttributeLinkingService.class);
        messageBrokerService = container.getService(MessageBrokerService.class);
        clientEventService = container.getService(ClientEventService.class);

        clientEventService.addSubscriptionAuthorizer((auth, subscription) -> {
            if (!subscription.isEventType(AttributeEvent.class)) {
                return false;
            }

            // Always must have a filter, as you can't subscribe to ALL asset attribute events
            if (subscription.getFilter() != null && subscription.getFilter() instanceof AttributeEvent.EntityIdFilter) {
                AttributeEvent.EntityIdFilter filter = (AttributeEvent.EntityIdFilter) subscription.getFilter();

                // Superuser can get attribute events for any asset
                if (auth.isSuperUser())
                    return true;

                // Regular user must have role
                if (!auth.hasResourceRole(ClientRole.READ_ASSETS.getValue(), Constants.KEYCLOAK_CLIENT_ID)) {
                    return false;
                }

                boolean isRestrictedUser = identityService.getIdentityProvider().isRestrictedUser(auth.getUserId());

                // Client can subscribe to several assets
                for (String assetId : filter.getEntityId()) {
                    Asset asset = assetStorageService.find(assetId);
                    // If the asset doesn't exist, subscription must fail
                    if (asset == null)
                        return false;
                    if (isRestrictedUser) {
                        // Restricted users can only get attribute events for their linked assets
                        if (!assetStorageService.isUserAsset(auth.getUserId(), assetId))
                            return false;
                    } else {
                        // Regular users can only get attribute events for assets in their realm
                        if (!asset.getTenantRealm().equals(auth.getAuthenticatedRealm()))
                            return false;
                    }
                }
                return true;
            }
            return false;
        });

        processors.add(agentService);
        processors.add(rulesService);
        processors.add(assetStorageService);
        processors.add(assetDatapointService);
        processors.add(assetAttributeLinkingService);

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
        from(CLIENT_EVENT_TOPIC)
            .routeId("FromClientUpdates")
            .filter(body().isInstanceOf(AttributeEvent.class))
            .setHeader(HEADER_SOURCE, () -> CLIENT)
            .to(ASSET_QUEUE);

        // Process attribute events
        /* TODO This message consumer should be transactionally consistent with the database, this is currenlty not the case

         Our "if I have not processed this message before" duplicate detection:

          - discard events with source time greater than server processing time (future events)
          - discard events with source time less than last applied/stored event source time
          - allow the rest (also events with same source time, order of application undefined)

         Possible improvements moving towards at-least-once:

          - Hibernate ThreadLocal Session & Transaction for the whole Camel Exchange/route
            0. Receive attribute event, security/404 checks
            1. Open DB local transaction
            2. Read last asset state
            3. Write non-duplicate attribute event
            4. Commit DB local transaction
            5. Commit reception on message broker

          - See pseudocode here: http://activemq.apache.org/should-i-use-xa.html

          - Replace at-most-once ClientEventService with at-least-once capable, embeddable message broker/protocol

          - Do we want JMS/AMQP/WSS or SOME_API/MQTT/WSS? ActiveMQ or Moquette?
        */
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
            .process(buildAssetState(timerService))
            .process(this::processAssetState)
            .endDoTry()
            .doCatch(AssetProcessingException.class)
            .process(handleAssetProcessingException(LOG));
    }

    /**
     * Send internal attribute change events into the {@link #ASSET_QUEUE}.
     */
    public void sendAttributeEvent(AttributeEvent attributeEvent) {
        sendAttributeEvent(attributeEvent, INTERNAL);
    }
    public void sendAttributeEvent(AttributeEvent attributeEvent, AttributeEvent.Source source) {
        // Set event source time if not already set
        if (attributeEvent.getTimestamp() <= 0) {
            attributeEvent.setTimestamp(timerService.getCurrentTimeMillis());
        }
        messageBrokerService.getProducerTemplate().sendBodyAndHeader(ASSET_QUEUE, attributeEvent, HEADER_SOURCE, source);
    }

    /**
     * This deals with single {@link AssetState} and pushes them through the chain where each
     * consumer is given the opportunity to consume the event or allow its progress to the next
     * consumer, see {@link AssetState.ProcessingStatus}.
     */
    protected void processAssetState(Exchange exchange) {
        AssetState assetState = exchange.getIn().getHeader(HEADER_ASSET_STATE, AssetState.class);
        LOG.fine(">>> Processing start: " + assetState);
        // Need to record time here otherwise an infinite loop generated inside one of the processors means the timestamp
        // is not updated so tests can't then detect the problem.
        lastProcessedEventTimestamp = System.currentTimeMillis();
        int processorCount = 0;
        processorLoop:
        for (Consumer<AssetState> processor : processors) {
            processorCount++;

            try {
                LOG.finest("==> Processor " + processor + " accepts: " + assetState);
                processor.accept(assetState);
            } catch (Throwable t) {
                LOG.log(Level.SEVERE, "!!! Processor " + processor + " threw an exception whilst consuming: " + assetState, t);
                assetState.setProcessingStatus(AssetState.ProcessingStatus.ERROR);
                assetState.setError(t);
            }

            switch (assetState.getProcessingStatus()) {
                case COMPLETED:
                    LOG.finest("<== Processor " + processor + " finally handled: " + assetState);
                    break processorLoop;
                case ERROR:
                    LOG.log(Level.SEVERE, "<== Processor " + processor + " error: " + assetState, assetState.getError());
                    break processorLoop;
                default:
                    LOG.finest("<== Processor " + processor + " done with: " + assetState);
            }
        }
        if (assetState.getProcessingStatus() != AssetState.ProcessingStatus.ERROR) {
            if (assetState.getProcessingStatus() != AssetState.ProcessingStatus.COMPLETED)
                assetState.setProcessingStatus(AssetState.ProcessingStatus.COMPLETED);

            // Only notify clients of events that reach the end of the chain
            if (processorCount == processors.size()) {
                clientEventService.publishEvent(new AttributeEvent(
                    assetState.getId(),
                    assetState.getAttributeName(),
                    assetState.getValue(),
                    timerService.getCurrentTimeMillis()
                ));
            }
        }
        LOG.fine("<<< Processing complete: " + assetState);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
            '}';
    }
}
