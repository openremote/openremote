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
package org.openremote.app.client.event;

import elemental2.dom.DomGlobal;
import org.openremote.app.client.OpenRemoteApp;
import org.openremote.model.event.TriggeredEventSubscription;
import org.openremote.model.event.bus.EventBus;
import org.openremote.model.event.shared.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class EventServiceImpl implements EventService {

    final protected OpenRemoteApp app;
    final protected EventBus eventBus;
    final protected SharedEventMapper sharedEventMapper;
    final protected SharedEventArrayMapper sharedEventArrayMapper;
    final protected EventSubscriptionMapper eventSubscriptionMapper;
    final protected CancelEventSubscriptionMapper cancelEventSubscriptionMapper;
    final protected UnauthorizedEventSubscriptionMapper unauthorizedEventSubscriptionMapper;
    final protected TriggeredEventSubscriptionMapper triggeredEventSubscriptionMapper;

    final protected Map<String, Double> activeSubscriptions = new HashMap<>();

    public EventServiceImpl(OpenRemoteApp app,
                            EventBus eventBus,
                            SharedEventMapper sharedEventMapper,
                            SharedEventArrayMapper sharedEventArrayMapper,
                            EventSubscriptionMapper eventSubscriptionMapper,
                            CancelEventSubscriptionMapper cancelEventSubscriptionMapper,
                            UnauthorizedEventSubscriptionMapper unauthorizedEventSubscriptionMapper,
                            TriggeredEventSubscriptionMapper triggeredEventSubscriptionMapper) {
        this.app = app;
        this.eventBus = eventBus;
        this.sharedEventMapper = sharedEventMapper;
        this.sharedEventArrayMapper = sharedEventArrayMapper;
        this.eventSubscriptionMapper = eventSubscriptionMapper;
        this.cancelEventSubscriptionMapper = cancelEventSubscriptionMapper;
        this.unauthorizedEventSubscriptionMapper = unauthorizedEventSubscriptionMapper;
        this.triggeredEventSubscriptionMapper = triggeredEventSubscriptionMapper;

        this.app.addServiceMessageConsumer(this::onServiceMessageReceived);

        this.app.addServiceConnectionCloseListener(this::stop);
    }

    @Override
    public <E extends SharedEvent> void subscribe(Class<E> eventClass) {
        subscribe(eventClass, null);
    }

    @Override
    public <E extends SharedEvent> void subscribe(Class<E> eventClass, EventFilter<E> filter) {
        EventSubscription<E> subscription = new EventSubscription<>(eventClass, filter);
        final String key = subscription.getEventType();
        final String data = EventSubscription.SUBSCRIBE_MESSAGE_PREFIX + eventSubscriptionMapper.write(subscription);

        if (activeSubscriptions.containsKey(key)) {
            DomGlobal.clearInterval(activeSubscriptions.get(key));
        }

        sendServiceMessage(data);

        double interval = DomGlobal.setInterval(
            e -> sendServiceMessage(data), EventSubscription.RENEWAL_PERIOD_SECONDS / 2 * 1000
        );

        activeSubscriptions.put(key, interval);
    }

    @Override
    public <E extends SharedEvent> void unsubscribe(Class<E> eventClass) {
        CancelEventSubscription<E> cancellation = new CancelEventSubscription<>(eventClass);
        final String key = cancellation.getEventType();
        if (activeSubscriptions.containsKey(key)) {
            DomGlobal.clearInterval(activeSubscriptions.get(key));
            sendServiceMessage(CancelEventSubscription.MESSAGE_PREFIX + cancelEventSubscriptionMapper.write(cancellation));
            activeSubscriptions.remove(key);
        }
    }

    @Override
    public void dispatch(SharedEvent sharedEvent) {
        sendServiceMessage(SharedEvent.MESSAGE_PREFIX + sharedEventMapper.write(sharedEvent));
    }

    @Override
    public void stop() {
        for (Map.Entry<String, Double> entry : activeSubscriptions.entrySet()) {
            DomGlobal.clearInterval(entry.getValue());
            CancelEventSubscription cancellation = new CancelEventSubscription(entry.getKey(), null);
            sendServiceMessage(CancelEventSubscription.MESSAGE_PREFIX + cancelEventSubscriptionMapper.write(cancellation));
        }
        activeSubscriptions.clear();
    }

    protected void sendServiceMessage(String data) {
        this.app.sendServiceMessage(data);
    }

    protected void onServiceMessageReceived(String data) {
        if (data == null || data.length() == 0)
            return;
        if (data.startsWith(UnauthorizedEventSubscription.MESSAGE_PREFIX)) {
            data = data.substring(UnauthorizedEventSubscription.MESSAGE_PREFIX.length());
            UnauthorizedEventSubscription<?> failure = unauthorizedEventSubscriptionMapper.read(data);
            eventBus.dispatch(new SubscriptionFailureEvent(failure.getSubscription().getEventType()));
        } else if (data.startsWith(TriggeredEventSubscription.MESSAGE_PREFIX)) {
            data = data.substring(TriggeredEventSubscription.MESSAGE_PREFIX.length());

            if (data.startsWith("{")) {
                TriggeredEventSubscription<?> triggered = triggeredEventSubscriptionMapper.read(data);
                if (triggered.getEvents() == null) {
                    SharedEvent event = sharedEventMapper.read(data);
                    eventBus.dispatch(event);
                } else {
                    triggered.getEvents().forEach(eventBus::dispatch);
                }
            }
        } else if (data.startsWith(SharedEvent.MESSAGE_PREFIX)) {
            data = data.substring(SharedEvent.MESSAGE_PREFIX.length());
            if (data.startsWith("[")) {
                // Handle array of events
                SharedEvent[] events = sharedEventArrayMapper.read(data);
                for (SharedEvent event : events) {
                    eventBus.dispatch(event);
                }
            } else {
                String[] dataArr = data.split(":(.+)");
                if (dataArr.length == 2) {
                    String subscriptionId = dataArr[0];
                    if (dataArr[1].startsWith("[")) {
                        // Handle array of events
                        SharedEvent[] events = sharedEventArrayMapper.read(data);
                        for (SharedEvent event : events) {
                            eventBus.dispatch(event);
                        }
                    } else {
                        SharedEvent event = sharedEventMapper.read(data);
                        eventBus.dispatch(event);
                    }
                }
            }
        }
    }
}
