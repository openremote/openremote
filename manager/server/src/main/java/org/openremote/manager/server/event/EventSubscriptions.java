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
package org.openremote.manager.server.event;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.impl.DefaultMessage;
import org.openremote.container.web.socket.WebsocketConstants;
import org.openremote.model.event.shared.CancelEventSubscription;
import org.openremote.model.event.shared.EventSubscription;
import org.openremote.model.event.shared.SharedEvent;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * Manages subscriptions to events for WebSocket sessions.
 */
public class EventSubscriptions {

    private static final Logger LOG = Logger.getLogger(EventSubscriptions.class.getName());

    final protected Map<String, SessionSubscriptions> sessionSubscriptions = new HashMap<>();

    final protected ScheduledExecutorService reaperScheduler = Executors.newScheduledThreadPool(
        1,
        r -> {
            Thread t = Executors.defaultThreadFactory().newThread(r);
            t.setDaemon(true);
            return t;
        }
    );

    class SessionSubscriptions extends HashSet<SessionSubscription> {
        public void removeExpired() {
            removeIf(sessionSubscription -> {
                    boolean expired =
                        sessionSubscription.timestamp
                            + (EventSubscription.RENEWAL_PERIOD_SECONDS * 1000)
                            < System.currentTimeMillis();
                    if (expired) {
                        LOG.fine("Removing expired; " + sessionSubscription.subscription);
                    }
                    return expired;
                }
            );
        }

        public void update(EventSubscription eventSubscription) {
            cancel(eventSubscription.getEventType());
            SessionSubscription sessionSubscription = new SessionSubscription(System.currentTimeMillis(), eventSubscription);
            add(sessionSubscription);
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

    public EventSubscriptions() {
        reaperScheduler.scheduleAtFixedRate(() -> {
            synchronized (this.sessionSubscriptions) {
                for (SessionSubscriptions subscriptions : sessionSubscriptions.values()) {
                    subscriptions.removeExpired();
                }
            }
        }, 0, 1, TimeUnit.SECONDS);
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

                if (sessionSubscription.subscription.getFilter() != null) {
                    //noinspection unchecked
                    event = sessionSubscription.subscription.getFilter().apply(event);
                }

                if (event != null) {
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

    /* TODO Move this into AssetStorageService?
        private AssetModifiedEvent[] createAssetModifiedEvents(PersistenceEvent<Asset> persistenceEvent) {
            List<AssetModifiedEvent> events = new ArrayList<>();

            Asset asset = persistenceEvent.getEntity();
            // Assets are a composite structure and we want to be able to fire events that say
            // "a child was removed/inserted for this or that parent". To do this we need to
            // compare old and new entity state, figuring out which parent was modified. There
            // is also the special case of assets without parent: In that case we fire a "children
            // modified" for a special "empty" asset, which clients should consider to be the root
            // item of the whole tree. It has no name, no id, no parent, etc.
            List<String> modifiedParentIds = new ArrayList<>();

            AssetModifiedEvent.Cause cause = null;

            switch (persistenceEvent.getCause()) {
                case INSERT:
                    cause = AssetModifiedEvent.Cause.CREATE;
                    if (asset.getParentId() != null) {
                        modifiedParentIds.add(asset.getParentId());
                    } else {
                        // The "root without id" asset was modified
                        events.add(new AssetModifiedEvent(new Asset(), AssetModifiedEvent.Cause.CHILDREN_MODIFIED));
                    }
                    break;
                case UPDATE:
                    cause = AssetModifiedEvent.Cause.UPDATE;

                    // Find out if the asset has a new parent
                    String previousParentId = persistenceEvent.getPreviousState("parentId");
                    String currentParentId = persistenceEvent.getCurrentState("parentId");
                    if (previousParentId == null && currentParentId == null) {
                        // The "root without id" asset was modified
                        events.add(new AssetModifiedEvent(new Asset(), AssetModifiedEvent.Cause.CHILDREN_MODIFIED));
                    } else {
                        if (previousParentId == null) {
                            // The "root without id" asset was modified
                            events.add(new AssetModifiedEvent(new Asset(), AssetModifiedEvent.Cause.CHILDREN_MODIFIED));
                            modifiedParentIds.add(currentParentId);
                        } else if (currentParentId == null) {
                            // The "root without id" asset was modified
                            events.add(new AssetModifiedEvent(new Asset(), AssetModifiedEvent.Cause.CHILDREN_MODIFIED));
                            modifiedParentIds.add(previousParentId);
                        }
                    }

                    // Only send "children modified" of the parent if the name of the asset changed
                    String previousName = persistenceEvent.getPreviousState("name");
                    String currentName = persistenceEvent.getCurrentState("name");
                    boolean isEqualName = Objects.equals(previousName, currentName);
                    if (!isEqualName) {
                        modifiedParentIds.add(asset.getParentId());
                    }

                    break;
                case DELETE:
                    cause = AssetModifiedEvent.Cause.DELETE;
                    if (asset.getParentId() != null) {
                        modifiedParentIds.add(asset.getParentId());
                    } else {
                        // The "root without id" asset was modified
                        events.add(new AssetModifiedEvent(new Asset(), AssetModifiedEvent.Cause.CHILDREN_MODIFIED));
                    }
                    break;
            }

            events.add(new AssetModifiedEvent(asset, cause));

            for (String modifiedParentId : modifiedParentIds) {
                Asset parent = getAsset(modifiedParentId);
                if (parent != null) {
                    events.add(new AssetModifiedEvent(parent, AssetModifiedEvent.Cause.CHILDREN_MODIFIED));
                }
            }

            return events.toArray(new AssetModifiedEvent[events.size()]);
        }
    */
}