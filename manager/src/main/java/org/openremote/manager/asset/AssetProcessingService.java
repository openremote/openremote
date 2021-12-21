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
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.openremote.container.message.MessageBrokerService;
import org.openremote.container.persistence.PersistenceService;
import org.openremote.container.security.AuthContext;
import org.openremote.container.timer.TimerService;
import org.openremote.manager.agent.AgentService;
import org.openremote.manager.datapoint.AssetDatapointService;
import org.openremote.manager.event.ClientEventService;
import org.openremote.manager.event.EventSubscriptionAuthorizer;
import org.openremote.manager.gateway.GatewayService;
import org.openremote.manager.rules.RulesService;
import org.openremote.manager.security.ManagerIdentityService;
import org.openremote.model.Constants;
import org.openremote.model.Container;
import org.openremote.model.ContainerService;
import org.openremote.model.asset.Asset;
import org.openremote.model.asset.AssetResource;
import org.openremote.model.asset.agent.Protocol;
import org.openremote.model.attribute.*;
import org.openremote.model.attribute.AttributeEvent.Source;
import org.openremote.model.security.ClientRole;
import org.openremote.model.util.ValueUtil;
import org.openremote.model.value.MetaItemType;
import org.openremote.model.value.ValueType;

import javax.persistence.EntityManager;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.openremote.container.concurrent.GlobalLock.withLock;
import static org.openremote.model.attribute.AttributeWriteFailure.*;
import static org.openremote.manager.event.ClientEventService.CLIENT_EVENT_TOPIC;
import static org.openremote.model.attribute.AttributeEvent.HEADER_SOURCE;
import static org.openremote.model.attribute.AttributeEvent.Source.*;
import static org.openremote.model.value.MetaItemType.AGENT_LINK;

/**
 * Receives {@link AttributeEvent} from {@link Source} and processes them.
 * <dl>
 * <dt>{@link Source#CLIENT}</dt>
 * <dd><p>Client events published through event bus or sent by web service. These exchanges must contain an {@link AuthContext}
 * header named {@link Constants#AUTH_CONTEXT}.</dd>
 * <dt>{@link Source#INTERNAL}</dt>
 * <dd><p>Events sent to {@link #ASSET_QUEUE} or through {@link #sendAttributeEvent} convenience method by processors.</dd>
 * <dt>{@link Source#SENSOR}</dt>
 * <dd><p>Protocol sensor updates sent to {@link Protocol#SENSOR_QUEUE}.</dd>
 * </dl>
 * NOTE: An attribute value can be changed during Asset<?> CRUD but this does not come through
 * this route but is handled separately, see {@link AssetResource}. Any attribute values
 * assigned during Asset<?> CRUD can be thought of as the attributes initial value.
 * <p>
 * The {@link AttributeEvent}s are first validated depending on their source, and if validation fails
 * at any point then an {@link AssetProcessingException} will be logged as a warning with an
 * {@link AttributeWriteFailure}.
 * <p>
 * Once successfully validated a chain of {@link AssetUpdateProcessor}s is handling the update message:
 * <ul>
 * <li>{@link AgentService}</li>
 * <li>{@link RulesService}</li>
 * <li>{@link AssetStorageService}</li>
 * <li>{@link AssetDatapointService}</li>
 * </ul>
 * <h2>Agent service processing logic</h2>
 * <p>
 * The agent service's role is to communicate asset attribute writes to actuators, through protocols.
 * When the update messages' source is {@link Source#SENSOR}, the agent service ignores the message.
 * The message will also be ignored if the updated attribute is not linked to an agent.
 * <p>
 * If the updated attribute has a valid agent link, an {@link AttributeEvent} is sent on the {@link Protocol#ACTUATOR_TOPIC},
 * for execution on an actual device or service 'things'. The update is then considered complete, and no further processing
 * is necessary. The update will not reach the rules engine or the database.
 * <p>
 * This means that a protocol implementation is responsible for producing a new {@link AttributeEvent} to
 * indicate to the rules and database memory that the attribute value has/has not changed. The protocol should know
 * best when to do this and it will vary from protocol to protocol; some 'things' might respond to an actuator write
 * immediately with a new sensor read, or they might send a later "sensor value changed" message or both or neither
 * (the actuator is "fire and forget"). The protocol must decide what the best course of action is based on the
 * 'things' it communicates with and the transport layer it uses etc.
 * <h2>Rules Service processing logic</h2>
 * <p>
 * Checks if attribute has {@link MetaItemType#RULE_STATE} and/or {@link MetaItemType#RULE_EVENT} {@link MetaItem}s,
 * and if so the message is passed through the rule engines that are in scope for the asset.
 * <p>
 * <h2>Asset Storage Service processing logic</h2>
 * <p>
 * Always tries to persist the attribute value in the database and allows the message to continue if the commit was
 * successful.
 * <h2>Asset Datapoint Service processing logic</h2>
 * <p>
 * Checks if attribute has {@link MetaItemType#STORE_DATA_POINTS} set to false or if the attribute does not have an
 * {@link org.openremote.model.asset.agent.AgentLink} meta, and if so the {@link AttributeEvent}
 * is not stored in a time series DB of historical data, otherwise the value is stored. Then allows the message to
 * continue if the commit was successful.
 */
@SuppressWarnings("unchecked")
public class AssetProcessingService extends RouteBuilder implements ContainerService {

    public static final int PRIORITY = AssetStorageService.PRIORITY + 1000;
    // TODO: Some of these options should be configurable depending on expected load etc.
    // Message topic for communicating individual asset attribute changes
    public static final String ASSET_QUEUE = "seda://AssetQueue?waitForTaskToComplete=IfReplyExpected&timeout=10000&purgeWhenStopping=true&discardIfNoConsumers=false&size=25000";
    private static final Logger LOG = Logger.getLogger(AssetProcessingService.class.getName());
    final protected List<AssetUpdateProcessor> processors = new ArrayList<>();
    protected TimerService timerService;
    protected ManagerIdentityService identityService;
    protected PersistenceService persistenceService;
    protected RulesService rulesService;
    protected AgentService agentService;
    protected GatewayService gatewayService;
    protected AssetStorageService assetStorageService;
    protected AssetDatapointService assetDatapointService;
    protected AttributeLinkingService assetAttributeLinkingService;
    protected MessageBrokerService messageBrokerService;
    protected ClientEventService clientEventService;
    // Used in testing to detect if initial/startup processing has completed
    protected long lastProcessedEventTimestamp = System.currentTimeMillis();

    protected static Processor handleAssetProcessingException(Logger logger) {
        return exchange -> {
            AttributeEvent event = exchange.getIn().getBody(AttributeEvent.class);
            Exception exception = (Exception) exchange.getProperty(Exchange.EXCEPTION_CAUGHT);

            StringBuilder error = new StringBuilder();

            Source source = exchange.getIn().getHeader(HEADER_SOURCE, "unknown source", Source.class);
            if (source != null) {
                error.append("Error processing from ").append(source);
            }

            String protocolName = exchange.getIn().getHeader(Protocol.SENSOR_QUEUE_SOURCE_PROTOCOL, String.class);
            if (protocolName != null) {
                error.append(" (protocol: ").append(protocolName).append(")");
            }

            // TODO Better exception handling - dead letter queue?
            if (exception instanceof AssetProcessingException) {
                AssetProcessingException processingException = (AssetProcessingException) exception;
                error.append(" - ").append(processingException.getMessage());
                error.append(": ").append(event.toString());
                logger.warning(error.toString());
            } else {
                error.append(": ").append(event.toString());
                logger.log(Level.WARNING, error.toString(), exception);
            }

            // Make the exception available if MEP is InOut
            exchange.getOut().setBody(exception);
        };
    }

    @Override
    public int getPriority() {
        return PRIORITY;
    }

    @Override
    public void init(Container container) throws Exception {
        timerService = container.getService(TimerService.class);
        identityService = container.getService(ManagerIdentityService.class);
        persistenceService = container.getService(PersistenceService.class);
        rulesService = container.getService(RulesService.class);
        agentService = container.getService(AgentService.class);
        gatewayService = container.getService(GatewayService.class);
        assetStorageService = container.getService(AssetStorageService.class);
        assetDatapointService = container.getService(AssetDatapointService.class);
        assetAttributeLinkingService = container.getService(AttributeLinkingService.class);
        messageBrokerService = container.getService(MessageBrokerService.class);
        clientEventService = container.getService(ClientEventService.class);
        EventSubscriptionAuthorizer assetEventAuthorizer = AssetStorageService.assetInfoAuthorizer(identityService, assetStorageService);

        clientEventService.addSubscriptionAuthorizer((auth, subscription) -> {
            if (!subscription.isEventType(AttributeEvent.class)) {
                return false;
            }
            return assetEventAuthorizer.apply(auth, subscription);
        });

        processors.add(gatewayService);
        processors.add(agentService);
        processors.add(rulesService);
        processors.add(assetDatapointService);
        processors.add(assetAttributeLinkingService);

        container.getService(MessageBrokerService.class).getContext().addRoutes(this);
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
        /* TODO This message consumer should be transactionally consistent with the database, this is currently not the case

         Our "if I have not processed this message before" duplicate detection:

          - discard events with source time greater than server processing time (future events)
          - discard events with source time less than last applied/stored event source time
          - allow the rest (also events with same source time, order of application undefined)

         Possible improvements moving towards at-least-once:

         - Make AssetUpdateProcessor transactional with a two-phase commit API
         - Replace at-most-once ClientEventService with at-least-once capable, embeddable message broker/protocol
         - See pseudocode here: http://activemq.apache.org/should-i-use-xa.html
         - Do we want JMS/AMQP/WSS or SOME_API/MQTT/WSS? ActiveMQ or Moquette?
        */
        from(ASSET_QUEUE)
            .routeId("AssetQueueProcessor")
            .filter(body().isInstanceOf(AttributeEvent.class))
            .doTry()
            // Lock the global context, we can only process attribute events when the
            // context isn't locked. Agent- and RulesService lock the context while protocols
            // or rulesets are modified.
            .process(exchange -> withLock(getClass().getSimpleName() + "::processFromAssetQueue", () -> {

                AttributeEvent event = exchange.getIn().getBody(AttributeEvent.class);
                LOG.finest("Processing: " + event);
                if (event.getAssetId() == null || event.getAssetId().isEmpty())
                    return;
                if (event.getAttributeName() == null || event.getAttributeName().isEmpty())
                    return;
                Source source = exchange.getIn().getHeader(HEADER_SOURCE, () -> null, Source.class);
                if (source == null) {
                    throw new AssetProcessingException(MISSING_SOURCE);
                }

                // Process the asset update in a database transaction, this ensures that processors
                // will see consistent database state and we only commit if no processor failed. This
                // still won't make this procedure consistent with the message queue from which we consume!
                persistenceService.doTransaction(em -> {
                    Asset<?> asset = assetStorageService.find(em, event.getAssetId(), true);

                    if (asset == null) {
                        if (source == SENSOR) {
                            // Fail silently as a protocol may have queued updates before the asset was deleted
                            return;
                        }

                        throw new AssetProcessingException(ASSET_NOT_FOUND);
                    }

                    Attribute<?> oldAttribute = asset.getAttribute(event.getAttributeName()).orElse(null);
                    if (oldAttribute == null) {
                        if (source == SENSOR) {
                            // Fail silently as a protocol may have queued updates before the attribute was modified/deleted
                            return;
                        }

                        throw new AssetProcessingException(ATTRIBUTE_NOT_FOUND);
                    }

                    switch (source) {
                        case CLIENT:

                            AuthContext authContext = exchange.getIn().getHeader(Constants.AUTH_CONTEXT, AuthContext.class);
                            if (authContext == null) {
                                // Check attribute has public write flag
                                if (!oldAttribute.hasMeta(MetaItemType.ACCESS_PUBLIC_WRITE)) {
                                    throw new AssetProcessingException(INSUFFICIENT_ACCESS);
                                }
                            } else {
                                // Check realm, must be accessible
                                if (!identityService.getIdentityProvider().isTenantActiveAndAccessible(authContext,
                                    asset.getRealm())) {
                                    throw new AssetProcessingException(INSUFFICIENT_ACCESS);
                                }

                                // Regular user must have write attributes role
                                if (!authContext.hasResourceRoleOrIsSuperUser(ClientRole.WRITE_ATTRIBUTES.getValue(),
                                    authContext.getClientId())) {
                                    throw new AssetProcessingException(INSUFFICIENT_ACCESS);
                                }

                                // Check restricted user
                                if (identityService.getIdentityProvider().isRestrictedUser(authContext)) {
                                    // Must be asset linked to user
                                    if (!assetStorageService.isUserAsset(authContext.getUserId(),
                                        event.getAssetId())) {
                                        throw new AssetProcessingException(INSUFFICIENT_ACCESS);
                                    }
                                    // Must be writable by restricted client
                                    if (!oldAttribute.getMetaValue(MetaItemType.ACCESS_RESTRICTED_WRITE).orElse(false)) {
                                        throw new AssetProcessingException(INSUFFICIENT_ACCESS);
                                    }
                                }
                            }
                            break;

                        case SENSOR:
                            Optional<Protocol<?>> protocol = oldAttribute.getMetaValue(AGENT_LINK)
                                .map(agentLink -> agentService.getProtocolInstance(agentLink.getId()));

                            // Sensor event must be for an attribute linked to an agent
                            if (!protocol.isPresent()) {
                                throw new AssetProcessingException(INVALID_AGENT_LINK);
                            }
                            break;
                    }

                    // For executable attributes, non-sensor sources can set a writable attribute execute status
                    if (oldAttribute.getType() == ValueType.EXECUTION_STATUS && source != SENSOR) {
                        Optional<AttributeExecuteStatus> status = event.getValue()
                            .flatMap(ValueUtil::getString)
                            .flatMap(AttributeExecuteStatus::fromString);

                        if (status.isPresent() && !status.get().isWrite()) {
                            throw new AssetProcessingException(INVALID_ATTRIBUTE_EXECUTE_STATUS);
                        }
                    }

                    // Type coercion
                    Object value = event.getValue().map(eventValue -> {
                        Class<?> attributeValueType = oldAttribute.getType().getType();
                        return ValueUtil.getValueCoerced(eventValue, attributeValueType).orElseThrow(() -> {
                            LOG.info("Failed to coerce attribute event value into the correct value type: event value type=" + eventValue.getClass() + ", attribute value type=" + attributeValueType);
                            return new AssetProcessingException(INVALID_VALUE_FOR_WELL_KNOWN_ATTRIBUTE);
                        });

                    }).orElse(null);

                    // TODO: Use schema validation
                    // Check if attribute is well known and the value is valid
//                    AssetModelUtil.getAssetDescriptor(asset.getType()).map(assetDescriptor -> assetDescriptor.get)
//                    AssetModelUtil.getAttributeDescriptor(oldAttribute.name).ifPresent(wellKnownAttribute -> {
//                        // Check if the value is valid
//                        wellKnownAttribute.getValueDescriptor()
//                            .getValidator().flatMap(v -> v.apply(event.getValue().orElse(null)))
//                            .ifPresent(validationFailure -> {
//                                throw new AssetProcessingException(
//                                    INVALID_VALUE_FOR_WELL_KNOWN_ATTRIBUTE
//                                );
//                            });
//                    });

                    // Either use the timestamp of the event or set event time to processing time
                    long processingTime = timerService.getCurrentTimeMillis();
                    long eventTime = event.getTimestamp() > 0 ? event.getTimestamp() : processingTime;

                    // Ensure timestamp of event is not in the future as that would essentially block access to
                    // the attribute until after that time (maybe that is desirable behaviour)
                    if (eventTime - processingTime > 0) {
                        // TODO: Decide how to handle update events in the future - ignore or change timestamp
                        throw new AssetProcessingException(
                            EVENT_IN_FUTURE,
                            "current time: " + new Date(processingTime) + "/" + processingTime
                                + ", event time: " + new Date(eventTime) + "/" + eventTime
                        );
                    }

                    // Check the last update timestamp of the attribute, ignoring any event that is older than last update
                    // TODO This means we drop out-of-sequence events but accept events with the same source timestamp
                    // TODO Several attribute events can occur in the same millisecond, then order of application is undefined
                    oldAttribute.getTimestamp().filter(t -> t >= 0 && eventTime < t).ifPresent(
                        lastStateTime -> {
                            throw new AssetProcessingException(
                                EVENT_OUTDATED,
                                "last asset state time: " + new Date(lastStateTime) + "/" + lastStateTime
                                    + ", event time: " + new Date(eventTime) + "/" + eventTime);
                        }
                    );

                    // Create a copy of the attribute and set the new value and timestamp
                    @SuppressWarnings("rawtypes")
                    Attribute updatedAttribute = ValueUtil.clone(oldAttribute);
                    updatedAttribute.setValue(value, eventTime);

                    // Push through all processors
                    boolean consumedCompletely = processAssetUpdate(em, asset, updatedAttribute, source);

                    // Publish a new event for clients if no processor consumed the update completely
                    if (!consumedCompletely) {
                        publishClientEvent(asset, updatedAttribute);
                    }
                });
            }))
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

    public void sendAttributeEvent(AttributeEvent attributeEvent, Source source) {
        // Set event source time if not already set
        if (attributeEvent.getTimestamp() <= 0) {
            attributeEvent.setTimestamp(timerService.getCurrentTimeMillis());
        }
        messageBrokerService.getProducerTemplate().sendBodyAndHeader(ASSET_QUEUE, attributeEvent, HEADER_SOURCE, source);
    }

    /**
     * This deals with single {@link Attribute} updates and pushes them through the chain where each processor is given
     * the opportunity to completely consume the update or allow its progress to the next processor, see {@link
     * AssetUpdateProcessor#processAssetUpdate}. If no processor completely consumed the update, the attribute will be
     * stored in the database.
     */
    protected boolean processAssetUpdate(EntityManager em,
                                         Asset<?> asset,
                                         Attribute<?> attribute,
                                         Source source) throws AssetProcessingException {

        String attributeStr = "Asset ID=" + asset.getId() + ", Asset name=" + asset.getName() + ", " + attribute;

        LOG.fine(">>> Processing start: " + attributeStr);

        // Need to record time here otherwise an infinite loop generated inside one of the processors means the timestamp
        // is not updated so tests can't then detect the problem.
        lastProcessedEventTimestamp = System.currentTimeMillis();

        boolean complete = false;
        for (AssetUpdateProcessor processor : processors) {
            LOG.finest("==> Processor " + processor + " accepts: " + attributeStr);
            try {
                complete = processor.processAssetUpdate(em, asset, attribute, source);
            } catch (AssetProcessingException ex) {
                throw ex;
            } catch (Throwable t) {
                throw new AssetProcessingException(
                    PROCESSOR_FAILURE,
                    "processor '" + processor + "' threw an exception",
                    t
                );
            }
            if (complete) {
                LOG.fine("<== Processor " + processor + " completely consumed: " + attributeStr);
                break;
            } else {
                LOG.finest("<== Processor " + processor + " done with: " + attributeStr);
            }
        }

        if (!complete) {
            LOG.fine("No processor consumed the update completely, storing: " + attributeStr);
            storeAttributeValue(em, asset, attribute);
        }

        LOG.fine("<<< Processing complete: " + attributeStr);
        return complete;
    }

    protected void storeAttributeValue(EntityManager em, Asset<?> asset, Attribute<?> attribute) throws AssetProcessingException {

        if (!assetStorageService.updateAttributeValue(em, asset, attribute)) {
            throw new AssetProcessingException(
                STATE_STORAGE_FAILED, "database update failed, no rows updated"
            );
        }
    }

    protected void publishClientEvent(Asset<?> asset, Attribute<?> attribute) {
        // TODO Catch "queue full" exception (e.g. when producing thousands of INFO messages in rules)?
        clientEventService.publishEvent(
            attribute.getMetaValue(MetaItemType.ACCESS_RESTRICTED_READ).orElse(false),
            new AttributeEvent(
                asset.getId(),
                attribute.getName(),
                attribute.getValue().orElse(null),
                timerService.getCurrentTimeMillis()
            ).setParentId(asset.getParentId()).setRealm(asset.getRealm()).setPath(asset.getPath())
        );
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
            '}';
    }
}
