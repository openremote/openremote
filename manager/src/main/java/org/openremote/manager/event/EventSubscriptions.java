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
package org.openremote.manager.event;

import org.apache.activemq.artemis.utils.collections.ConcurrentHashSet;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.support.DefaultMessage;
import org.openremote.container.timer.TimerService;
import org.openremote.model.event.TriggeredEventSubscription;
import org.openremote.model.event.shared.CancelEventSubscription;
import org.openremote.model.event.shared.EventSubscription;
import org.openremote.model.event.shared.SharedEvent;
import org.openremote.model.util.TextUtil;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Logger;

import static org.openremote.model.Constants.SESSION_KEY;

/**
 * Manages subscriptions to events for WebSocket sessions.
 */
public class EventSubscriptions {

    private static final Logger LOG = Logger.getLogger(EventSubscriptions.class.getName());

    final protected TimerService timerService;
    final protected ConcurrentMap<String, SessionSubscriptions> sessionSubscriptionIdMap = new ConcurrentHashMap<>();

    class SessionSubscriptions extends ConcurrentHashSet<SessionSubscription<?>> {
        protected void createOrUpdate(EventSubscription<?> eventSubscription) {

            if (TextUtil.isNullOrEmpty(eventSubscription.getSubscriptionId())) {
                cancelByType(eventSubscription.getEventType());
            } else {
                cancelById(eventSubscription.getSubscriptionId());
            }

            add(new SessionSubscription<>(timerService.getCurrentTimeMillis(), eventSubscription));
        }

        protected void cancelByType(String eventType) {
            removeIf(sessionSubscription -> sessionSubscription.subscriptionId == null && sessionSubscription.subscription.getEventType().equals(eventType));
        }

        protected void cancelById(String subscriptionId) {
            removeIf(sessionSubscription -> sessionSubscription.subscription.getSubscriptionId().equals(subscriptionId));
        }
    }

    static class SessionSubscription<T extends SharedEvent> {
        long timestamp;
        final EventSubscription<T> subscription;
        final String subscriptionId;

        public SessionSubscription(long timestamp, EventSubscription<T> subscription) {
            this.timestamp = timestamp;
            this.subscription = subscription;
            this.subscriptionId = subscription.getSubscriptionId();
        }

        public boolean matches(SharedEvent event) {
            return subscription.getEventType().equals(event.getEventType());
        }
    }

    public EventSubscriptions(TimerService timerService) {
        this.timerService = timerService;
    }

    protected void createOrUpdate(String sessionKey, EventSubscription<?> subscription) {
        LOG.finest("Create/update subscription for session '" + sessionKey + "': " + subscription);
        SessionSubscriptions sessionSubscriptions =
            this.sessionSubscriptionIdMap.computeIfAbsent(sessionKey, k -> new SessionSubscriptions());
        sessionSubscriptions.createOrUpdate(subscription);
    }

    protected void cancel(String sessionKey, CancelEventSubscription subscription) {
        if (!this.sessionSubscriptionIdMap.containsKey(sessionKey)) {
            return;
        }
        if (subscription.getEventType() == null && subscription.getSubscriptionId() == null) {
            return;
        }
        LOG.finest("Cancel subscription for session '" + sessionKey + "': " + subscription);
        SessionSubscriptions sessionSubscriptions = this.sessionSubscriptionIdMap.get(sessionKey);

        if (sessionSubscriptions == null) {
            return;
        }

        if (!TextUtil.isNullOrEmpty(subscription.getSubscriptionId())) {
            sessionSubscriptions.cancelById(subscription.getSubscriptionId());
        } else {
            sessionSubscriptions.cancelByType(subscription.getEventType());
        }
        if (sessionSubscriptions.isEmpty()) {
            this.sessionSubscriptionIdMap.remove(sessionKey);
        }
    }

    protected void cancelAll(String sessionKey) {
        if (this.sessionSubscriptionIdMap.containsKey(sessionKey)) {
            LOG.finest("Cancelling all subscriptions for session: " + sessionKey);
            this.sessionSubscriptionIdMap.remove(sessionKey);
        }
    }

    /**
     * Used in {@link ClientEventService#PUBLISH_QUEUE}
     */
    @SuppressWarnings({"unchecked", "unused"})
    public <T extends SharedEvent> List<Message> splitForSubscribers(Exchange exchange) {
        List<Message> messageList = new ArrayList<>();
        T event = (T)exchange.getIn().getBody(SharedEvent.class);

        if (event == null)
            return messageList;

        for (Map.Entry<String, SessionSubscriptions> entry : sessionSubscriptionIdMap.entrySet()) {
            String sessionKey = entry.getKey();
            SessionSubscriptions subscriptions = entry.getValue();

            for (SessionSubscription<?> sessionSubscription : subscriptions) {

                if (!sessionSubscription.matches(event))
                    continue;

                SessionSubscription<T> sessionSub = (SessionSubscription<T>) sessionSubscription;
                T filteredEvent = sessionSub.subscription.getFilter() == null ? event : sessionSub.subscription.getFilter().apply(event);

                if (filteredEvent != null) {
                    LOG.finest("Creating message for subscribed session '" + sessionKey + "': " + filteredEvent);
                    List<T> events = Collections.singletonList(filteredEvent);
                    TriggeredEventSubscription<T> triggeredEventSubscription = new TriggeredEventSubscription<>(events, sessionSub.subscriptionId);

                    if (sessionSub.subscription.isInternal()) {
                        if (triggeredEventSubscription.getEvents() != null) {
                            triggeredEventSubscription.getEvents().forEach(e ->
                                sessionSub.subscription.getInternalConsumer().accept(e));
                        }
                    } else {
                        Message msg = new DefaultMessage(exchange.getContext());
                        msg.setBody(triggeredEventSubscription); // Don't copy the event, use same reference
                        msg.setHeaders(new HashMap<>(exchange.getIn().getHeaders())); // Copy headers
                        msg.setHeader(SESSION_KEY, sessionKey);
                        messageList.add(msg);
                    }
                }
            }
        }
        return messageList;
    }
}
