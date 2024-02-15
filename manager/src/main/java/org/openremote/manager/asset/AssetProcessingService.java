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

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import jakarta.validation.ConstraintViolation;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.builder.RouteConfigurationBuilder;
import org.openremote.container.message.MessageBrokerService;
import org.openremote.container.persistence.PersistenceService;
import org.openremote.container.timer.TimerService;
import org.openremote.container.util.MapAccess;
import org.openremote.manager.agent.AgentService;
import org.openremote.manager.datapoint.AssetDatapointService;
import org.openremote.manager.event.AttributeEventInterceptor;
import org.openremote.manager.event.ClientEventService;
import org.openremote.manager.event.EventSubscriptionAuthorizer;
import org.openremote.manager.gateway.GatewayService;
import org.openremote.manager.rules.RulesService;
import org.openremote.manager.security.ManagerIdentityService;
import org.openremote.model.Constants;
import org.openremote.model.Container;
import org.openremote.model.ContainerService;
import org.openremote.model.asset.Asset;
import org.openremote.model.attribute.Attribute;
import org.openremote.model.attribute.AttributeEvent;
import org.openremote.model.attribute.MetaItem;
import org.openremote.model.security.ClientRole;
import org.openremote.model.util.ValueUtil;
import org.openremote.model.value.MetaItemType;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.RejectedExecutionException;
import java.util.stream.IntStream;

import static org.openremote.manager.system.HealthService.OR_CAMEL_ROUTE_METRIC_PREFIX;
import static org.openremote.model.attribute.AttributeWriteFailure.*;

/**
 * Receives {@link AttributeEvent} from various sources (clients and services) and processes them.
 * An {@link AttributeEventInterceptor} can register to intercept the events using {@link #addEventInterceptor} at which
 * point they can decide whether or not to allow the event to continue being processed or not.
 * <p>
 * Any {@link AttributeEvent}s that fail to be processed will generate an {@link AssetProcessingException} which will be
 * logged; there is currently no dead letter queue or retry processing.
 * <p>
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
public class AssetProcessingService extends RouteBuilder implements ContainerService {

    public static final String ATTRIBUTE_EVENT_ROUTE_CONFIG_ID = "attributeEvent";
    public static final int PRIORITY = AssetStorageService.PRIORITY + 1000;
    public static final String ATTRIBUTE_EVENT_ROUTER_QUEUE = "seda://AttributeEventRouter?waitForTaskToComplete=IfReplyExpected&timeout=10000&purgeWhenStopping=false&discardIfNoConsumers=false&size=10000";
    public static final String OR_ATTRIBUTE_EVENT_THREADS = "OR_ATTRIBUTE_EVENT_THREADS";
    public static final int OR_ATTRIBUTE_EVENT_THREADS_DEFAULT = Runtime.getRuntime().availableProcessors();
    protected static final String EVENT_ROUTE_COUNT_HEADER = "EVENT_ROUTE_COUNT_HEADER";
    protected static final String EVENT_PROCESSOR_URI_PREFIX = "seda://AttributeEventProcessor";
    protected static final String EVENT_PROCESSOR_URI_SUFFIX = "?size=3000&timeout=10000";
    public static final String EVENT_PROCESSOR_ROUTE_ID_PREFIX = "AttributeEvent-Processor";
    private static final System.Logger LOG = System.getLogger(AssetProcessingService.class.getName());

    final protected List<AttributeEventInterceptor> eventInterceptors = new ArrayList<>();
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
    protected int eventProcessingThreadCount;
    protected Counter queueFullCounter;

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
        MeterRegistry meterRegistry = container.getMeterRegistry();
        EventSubscriptionAuthorizer assetEventAuthorizer = AssetStorageService.assetInfoAuthorizer(identityService, assetStorageService);

        if (meterRegistry != null) {
            queueFullCounter = meterRegistry.counter(OR_CAMEL_ROUTE_METRIC_PREFIX + "_failed_queue_full", Tags.empty());
        }

        clientEventService.addSubscriptionAuthorizer((requestedRealm, auth, subscription) -> {
            if (!subscription.isEventType(AttributeEvent.class)) {
                return false;
            }
            return assetEventAuthorizer.authorise(requestedRealm, auth, subscription);
        });

        // TODO: Introduce caching here similar to ActiveMQ auth caching
        clientEventService.addEventAuthorizer((requestedRealm, authContext, event) -> {

            if (!(event instanceof AttributeEvent attributeEvent)) {
                return false;
            }

            if (authContext != null && authContext.isSuperUser()) {
                return true;
            }

            // Check realm against user
            if (!identityService.getIdentityProvider().isRealmActiveAndAccessible(authContext,
                requestedRealm)) {
                LOG.log(System.Logger.Level.INFO, "Realm is inactive, inaccessible or nonexistent: " + requestedRealm);
                return false;
            }

            // Users must have write attributes role
            if (authContext != null && !authContext.hasResourceRoleOrIsSuperUser(ClientRole.WRITE_ATTRIBUTES.getValue(),
                Constants.KEYCLOAK_CLIENT_ID)) {
                LOG.log(System.Logger.Level.DEBUG, "User doesn't have required role '" + ClientRole.WRITE_ATTRIBUTES + "': username=" + authContext.getUsername() + ", userRealm=" + authContext.getAuthenticatedRealmName());
                return false;
            }

            // Have to load the asset and attribute to perform additional checks - should permissions be moved out of the
            // asset model (possibly if the performance is determined to be not good enough)
            // TODO: Use a targeted query to retrieve just the info we need
            Asset<?> asset = assetStorageService.find(attributeEvent.getId());
            Attribute<?> attribute = asset != null ? asset.getAttribute(attributeEvent.getName()).orElse(null) : null;

            if (asset == null || !asset.hasAttribute(attributeEvent.getName())) {
                LOG.log(System.Logger.Level.INFO, () -> "Cannot authorize asset event as asset and/or attribute doesn't exist: " + attributeEvent.getRef());
                return false;
            } else if (!Objects.equals(requestedRealm, asset.getRealm())) {
                LOG.log(System.Logger.Level.INFO, () -> "Asset is not in the requested realm: requestedRealm=" + requestedRealm + ", ref=" + attributeEvent.getRef());
                return false;
            }

            if (authContext != null) {
                // Check restricted user
                if (identityService.getIdentityProvider().isRestrictedUser(authContext)) {
                    // Must be asset linked to user
                    if (!assetStorageService.isUserAsset(authContext.getUserId(),
                        attributeEvent.getId())) {
                        LOG.log(System.Logger.Level.DEBUG, () -> "Restricted user is not linked to asset '" + attributeEvent.getId() + "': username=" + authContext.getUsername() + ", userRealm=" + authContext.getAuthenticatedRealmName());
                        return false;
                    }

                    if (attribute == null || !attribute.getMetaValue(MetaItemType.ACCESS_RESTRICTED_WRITE).orElse(false)) {
                        LOG.log(System.Logger.Level.DEBUG, () -> "Asset attribute doesn't support restricted write on '" + attributeEvent.getRef() + "': username=" + authContext.getUsername() + ", userRealm=" + authContext.getAuthenticatedRealmName());
                        return false;
                    }
                }
            } else {
                // Check attribute has public write flag for anonymous write
                if (attribute == null || !attribute.hasMeta(MetaItemType.ACCESS_PUBLIC_WRITE)) {
                    LOG.log(System.Logger.Level.DEBUG, () -> "Asset doesn't support public write on '" + attributeEvent.getRef() + "': username=null");
                    return false;
                }
            }

            return true;
        });

        // Get dynamic route count for event processing (multithreaded event processing but guaranteeing events for the same asset end up in the same route)
        eventProcessingThreadCount = MapAccess.getInteger(container.getConfig(), OR_ATTRIBUTE_EVENT_THREADS, OR_ATTRIBUTE_EVENT_THREADS_DEFAULT);
        if (eventProcessingThreadCount < 1) {
            LOG.log(System.Logger.Level.WARNING, OR_ATTRIBUTE_EVENT_THREADS + " value " + eventProcessingThreadCount + " is less than 1; forcing to 1");
            eventProcessingThreadCount = 1;
        } else if (eventProcessingThreadCount > 20) {
            LOG.log(System.Logger.Level.WARNING, OR_ATTRIBUTE_EVENT_THREADS + " value " + eventProcessingThreadCount + " is greater than max value of 20; forcing to 20");
            eventProcessingThreadCount = 20;
        }

        // Add exception handling for attribute event processing that logs queue full exceptions and counts them
        messageBrokerService.getContext().addRoutesConfigurations(new RouteConfigurationBuilder() {
            @SuppressWarnings("unchecked")
            @Override
            public void configuration() throws Exception {
                routeConfiguration(ATTRIBUTE_EVENT_ROUTE_CONFIG_ID)
                    .onException(IllegalStateException.class, RejectedExecutionException.class, AssetProcessingException.class)
                    .handled(true)
                    .logExhausted(false)
                    .logStackTrace(false)
                    .process(exchange -> {
                        AttributeEvent event = exchange.getIn().getBody(AttributeEvent.class);
                        Exception exception = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Exception.class);

                        if (exception instanceof RejectedExecutionException || (exception instanceof IllegalStateException illegalStateException && "Queue full".equals(illegalStateException.getMessage()))) {
                            exception = new AssetProcessingException(QUEUE_FULL, "Queue for this event is full");
                            if (queueFullCounter != null) {
                                queueFullCounter.increment();
                            }
                        }

                        // Make the exception available if MEP is InOut
                        exchange.getMessage().setBody(exception);

                        if (!LOG.isLoggable(System.Logger.Level.WARNING)) {
                            return;
                        }

                        StringBuilder error = new StringBuilder("Error processing from ");

                        if (event.getSource() != null) {
                            error.append(event.getSource());
                        } else {
                            error.append("N/A");
                        }

                        error.append(": ").append(event.toStringWithValueType());

                        if (exception instanceof AssetProcessingException processingException) {
                            error.append(" ").append(" - ").append(processingException.getMessage());
                            if (processingException.getReason() == ASSET_NOT_FOUND) {
                                LOG.log(System.Logger.Level.DEBUG, error::toString);
                            } else {
                                LOG.log(System.Logger.Level.WARNING, error::toString);
                            }
                        } else {
                            LOG.log(System.Logger.Level.WARNING, error::toString, exception);
                        }
                    });
            }
        });

        messageBrokerService.getContext().addRoutes(this);
    }

    @Override
    public void start(Container container) throws Exception {
    }

    @Override
    public void stop(Container container) throws Exception {
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override
    public void configure() throws Exception {

        // All user authorisation checks MUST have been carried out before events reach this queue

        // Router is responsible for routing events to the same processor for a given asset ID, this allows for
        // multithreaded processing across assets
        from(ATTRIBUTE_EVENT_ROUTER_QUEUE)
            .routeId("AttributeEvent-Router")
            .routeConfigurationId(ATTRIBUTE_EVENT_ROUTE_CONFIG_ID)
            .process(exchange -> {
                AttributeEvent event = exchange.getIn().getBody(AttributeEvent.class);

                if (event.getId() == null || event.getId().isEmpty())
                    return; // Ignore events with no asset ID
                if (event.getName() == null || event.getName().isEmpty())
                    return; // Ignore events with no attribute name

                if (event.getTimestamp() <= 0) {
                    // Set timestamp if not set
                    event.setTimestamp(timerService.getCurrentTimeMillis());
                } else if (event.getTimestamp() > timerService.getCurrentTimeMillis()) {
                    // Use system time if event time is in the future (clock issue)
                    event.setTimestamp(timerService.getCurrentTimeMillis());
                }

                exchange.getIn().setHeader(EVENT_ROUTE_COUNT_HEADER, getEventProcessingRouteNumber(event.getId()));
            })
            .toD(EVENT_PROCESSOR_URI_PREFIX + "${header." + EVENT_ROUTE_COUNT_HEADER + "}");

        // Create the event processor routes
        IntStream.rangeClosed(1, eventProcessingThreadCount).forEach(processorCount -> {
            String camelRouteURI = getEventProcessingRouteURI(processorCount);

            from(camelRouteURI)
                .routeId(EVENT_PROCESSOR_ROUTE_ID_PREFIX + processorCount)
                .routeConfigurationId(ATTRIBUTE_EVENT_ROUTE_CONFIG_ID)
                .process(exchange -> {
                    AttributeEvent event = exchange.getIn().getBody(AttributeEvent.class);
                    LOG.log(System.Logger.Level.TRACE, () -> ">>> Attribute event processing start: processor=" + processorCount + ", event=" + event);
                    long startMillis = System.currentTimeMillis();
                    boolean processed = processAttributeEvent(event);

                    // Need to record time here otherwise an infinite loop generated inside one of the interceptors means the timestamp
                    // is not updated so tests can't then detect the problem.
                    lastProcessedEventTimestamp = startMillis;

                    long processingMillis = System.currentTimeMillis() - startMillis;

                    if (processingMillis > 50) {
                        LOG.log(System.Logger.Level.INFO, () -> "<<< Attribute event processing took a long time " + processingMillis + "ms: processor=" + processorCount + ", event=" + event);
                    } else {
                        LOG.log(System.Logger.Level.DEBUG, () -> "<<< Attribute event processed in " + processingMillis + "ms: processor=" + processorCount + ", event=" + event);
                    }

                    exchange.getIn().setBody(processed);
                });
        });
    }

    public void addEventInterceptor(AttributeEventInterceptor eventInterceptor) {
        eventInterceptors.add(eventInterceptor);
        eventInterceptors.sort(Comparator.comparingInt(AttributeEventInterceptor::getPriority));
    }

    /**
     * Send internal attribute change events into the {@link #ATTRIBUTE_EVENT_ROUTER_QUEUE}.
     */
    public void sendAttributeEvent(AttributeEvent attributeEvent) {
        sendAttributeEvent(attributeEvent, null);
    }

    /**
     * Send internal attribute change events into the {@link #ATTRIBUTE_EVENT_ROUTER_QUEUE}.
     */
    public void sendAttributeEvent(AttributeEvent attributeEvent, Object source) {
        attributeEvent.setSource(source);

        // Set event source time if not already set
        if (attributeEvent.getTimestamp() <= 0) {
            attributeEvent.setTimestamp(timerService.getCurrentTimeMillis());
        }
        messageBrokerService.getFluentProducerTemplate()
                .withBody(attributeEvent)
                .to(ATTRIBUTE_EVENT_ROUTER_QUEUE)
                .asyncSend();
    }

    /**
     * The {@link AttributeEvent} is passed to each registered {@link AttributeEventInterceptor} and if no interceptor
     * handles the event then the {@link Attribute} value is updated in the DB with the new event value and timestamp.
     */
    protected boolean processAttributeEvent(AttributeEvent event) throws AssetProcessingException {

        // TODO: Get asset lock so it cannot be modified during event processing
        persistenceService.doTransaction(em -> {

            // TODO: Retrieve optimised DTO rather than whole asset
            Asset<?> asset = assetStorageService.find(em, event.getId(), true);

            if (asset == null) {
                throw new AssetProcessingException(ASSET_NOT_FOUND, "Asset may have been deleted before event could be processed or it never existed");
            }

            Attribute<Object> attribute = asset.getAttribute(event.getName()).orElseThrow(() ->
                new AssetProcessingException(ATTRIBUTE_NOT_FOUND, "Attribute may have been deleted before event could be processed or it never existed"));

            // Type coercion
            Object value = event.getValue().map(eventValue -> {
                Class<?> attributeValueType = attribute.getTypeClass();
                return ValueUtil.getValueCoerced(eventValue, attributeValueType).orElseThrow(() -> {
                    String msg = "Event processing failed unable to coerce value into the correct value type: realm=" + event.getRealm() + ", attribute=" + event.getRef() + ", event value type=" + eventValue.getClass() + ", attribute value type=" + attributeValueType;
                    return new AssetProcessingException(INVALID_VALUE, msg);
                });
            }).orElse(null);
            event.setValue(value);

            AttributeEvent enrichedEvent = new AttributeEvent(asset, attribute, event.getSource(), event.getValue().orElse(null), event.getTimestamp(), attribute.getValue().orElse(null), attribute.getTimestamp().orElse(0L));

            // Do standard JSR-380 validation on the event
            Set<ConstraintViolation<AttributeEvent>> validationFailures = ValueUtil.validate(enrichedEvent);

            if (!validationFailures.isEmpty()) {
                String msg = "Event processing failed value failed constraint validation: realm=" + enrichedEvent.getRealm() + ", attribute=" + enrichedEvent.getRef() + ", event value type=" + enrichedEvent.getValue().map(v -> v.getClass().getName()).orElse("null") + ", attribute value type=" + enrichedEvent.getTypeClass();
                throw new AssetProcessingException(INVALID_VALUE, msg);
            }

            // TODO: Remove AttributeExecuteStatus
//            // For executable attributes, non-sensor sources can set a writable attribute execute status
//            if (attribute.getType() == ValueType.EXECUTION_STATUS && source != SENSOR) {
//                Optional<AttributeExecuteStatus> status = event.getValue()
//                    .flatMap(ValueUtil::getString)
//                    .flatMap(AttributeExecuteStatus::fromString);
//
//                if (status.isPresent() && !status.get().isWrite()) {
//                    throw new AssetProcessingException(INVALID_ATTRIBUTE_EXECUTE_STATUS);
//                }
//            }

            String interceptorName = null;
            boolean intercepted = false;

            for (AttributeEventInterceptor interceptor : eventInterceptors) {
                try {
                    intercepted = interceptor.intercept(em, enrichedEvent);
                } catch (AssetProcessingException ex) {
                    throw new AssetProcessingException(ex.getReason(), "Interceptor '" + interceptor + "' error=" + ex.getMessage());
                } catch (Throwable t) {
                    throw new AssetProcessingException(
                        INTERCEPTOR_FAILURE,
                        "Interceptor '" + interceptor + "' uncaught exception error=" + t.getMessage(),
                        t
                    );
                }
                if (intercepted) {
                    interceptorName = interceptor.getName();
                    break;
                }
            }

            if (intercepted) {
                LOG.log(System.Logger.Level.TRACE, "Event intercepted: interceptor=" + interceptorName + ", ref=" + enrichedEvent.getRef() + ", source=" + enrichedEvent.getSource());
            } else {
                if (enrichedEvent.isOutdated()) {
                    LOG.log(System.Logger.Level.INFO, () -> "Event is older than current attribute value so marking as outdated: ref=" + enrichedEvent.getRef() + ", event=" + Instant.ofEpochMilli(enrichedEvent.getTimestamp()) + ", previous=" + Instant.ofEpochMilli(enrichedEvent.getOldValueTimestamp()));
                    // Generate an event for this so internal subscribers can act on it if needed
                    clientEventService.publishEvent(new OutdatedAttributeEvent(enrichedEvent));
                } else if (!assetStorageService.updateAttributeValue(em, enrichedEvent)) {
                    throw new AssetProcessingException(
                        STATE_STORAGE_FAILED, "database update failed, no rows updated"
                    );
                }
            }
        });

        return true;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
            '}';
    }

    protected int getEventProcessingRouteNumber(String assetId) {
        int charCode = Character.codePointAt(assetId, 0);
        return (charCode % eventProcessingThreadCount) + 1;
    }

    protected String getEventProcessingRouteURI(int routeNumber) {
        return EVENT_PROCESSOR_URI_PREFIX + routeNumber + EVENT_PROCESSOR_URI_SUFFIX;
    }
}
