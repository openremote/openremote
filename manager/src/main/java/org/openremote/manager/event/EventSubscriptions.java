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

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.impl.DefaultMessage;
import org.openremote.container.timer.TimerService;
import org.openremote.container.web.socket.WebsocketConstants;
import org.openremote.manager.concurrent.ManagerExecutorService;
import org.openremote.model.event.shared.CancelEventSubscription;
import org.openremote.model.event.shared.EventSubscription;
import org.openremote.model.event.shared.SharedEvent;

import java.util.*;
import java.util.logging.Logger;

/**
 * Manages subscriptions to events for WebSocket sessions.
 */
public class EventSubscriptions {

    private static final Logger LOG = Logger.getLogger(EventSubscriptions.class.getName());

    final protected TimerService timerService;
    final protected Map<String, SessionSubscriptions> sessionSubscriptions = new HashMap<>();

    class SessionSubscriptions extends HashSet<SessionSubscription> {
        public void removeExpired() {
            removeIf(sessionSubscription -> {
                    boolean expired =
                        sessionSubscription.timestamp
                            + (EventSubscription.RENEWAL_PERIOD_SECONDS * 1000)
                            < timerService.getCurrentTimeMillis();
                    if (expired) {
                        LOG.fine("Removing expired; " + sessionSubscription.subscription);
                    }
                    return expired;
                }
            );
        }

        public void update(EventSubscription eventSubscription) {
            cancel(eventSubscription.getEventType());
            add(new SessionSubscription(timerService.getCurrentTimeMillis(), eventSubscription));
        }

        public void cancel(String eventType) {
            removeIf(sessionSubscription -> sessionSubscription.subscription.getEventType().equals(eventType));
        }
    }

    class SessionSubscription {
        final long timestamp;
        final EventSubscription subscription;

        public SessionSubscription(long timestamp, EventSubscription subscription) {
            this.timestamp = timestamp;
            this.subscription = subscription;
        }

        public boolean matches(SharedEvent event) {
            return subscription.getEventType().equals(event.getEventType());
        }
    }

    public EventSubscriptions(TimerService timerService, ManagerExecutorService executorService) {
        LOG.info("Starting background task checking for expired event subscriptions from clients");
        this.timerService = timerService;
        executorService.scheduleAtFixedRate(() -> {
            synchronized (this.sessionSubscriptions) {
                for (SessionSubscriptions subscriptions : sessionSubscriptions.values()) {
                    subscriptions.removeExpired();
                }
            }
        }, 5000, 1000);
    }

    public void update(String sessionKey, EventSubscription subscription) {
        synchronized (this.sessionSubscriptions) {
            // TODO Check if the user can actually subscribe to the events it wants, how do we do that?
            LOG.fine("For session '" + sessionKey + "', updating: " + subscription);
            SessionSubscriptions sessionSubscriptions =
                this.sessionSubscriptions.computeIfAbsent(sessionKey, k -> new SessionSubscriptions());
            sessionSubscriptions.update(subscription);
        }
    }

    public void cancel(String sessionKey, CancelEventSubscription subscription) {
        synchronized (this.sessionSubscriptions) {
            if (!this.sessionSubscriptions.containsKey(sessionKey))
                return;
            LOG.fine("For session '" + sessionKey + "', cancelling: " + subscription);
            SessionSubscriptions sessionSubscriptions = this.sessionSubscriptions.get(sessionKey);
            sessionSubscriptions.cancel(subscription.getEventType());
        }
    }

    public void cancelAll(String sessionKey) {
        synchronized (this.sessionSubscriptions) {
            if (this.sessionSubscriptions.containsKey(sessionKey)) {
                LOG.fine("Cancelling all subscriptions for session: " + sessionKey);
                this.sessionSubscriptions.remove(sessionKey);
            }
        }
    }

    @SuppressWarnings("unchecked")
    public <T extends SharedEvent> List<Message> splitForSubscribers(Exchange exchange) {
        List<Message> messageList = new ArrayList<>();
        SharedEvent event = exchange.getIn().getBody(SharedEvent.class);
        if (event == null)
            return messageList;

        Set<Map.Entry<String, SessionSubscriptions>> sessionSubscriptionsSet;
        synchronized (this.sessionSubscriptions) {
            sessionSubscriptionsSet = new HashSet<>(sessionSubscriptions.entrySet());
        }

        for (Map.Entry<String, SessionSubscriptions> entry : sessionSubscriptionsSet) {
            String sessionKey = entry.getKey();
            SessionSubscriptions subscriptions = entry.getValue();

            for (SessionSubscription sessionSubscription : subscriptions) {
                if (!sessionSubscription.matches(event))
                    continue;

                if (sessionSubscription.subscription.getFilter() == null
                    || sessionSubscription.subscription.getFilter().apply(event)) {
                    LOG.fine("Creating message for subscribed session '" + sessionKey + "': " + event);
                    Message msg = new DefaultMessage();
                    msg.setBody(event); // Don't copy the event, use same reference
                    msg.setHeaders(new HashMap<>(exchange.getIn().getHeaders())); // Copy headers
                    msg.setHeader(WebsocketConstants.SESSION_KEY, sessionKey);
                    messageList.add(msg);
                }
            }
        }
        return messageList;
    }
}