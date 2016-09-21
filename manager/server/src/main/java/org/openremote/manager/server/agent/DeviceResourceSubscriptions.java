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
package org.openremote.manager.server.agent;

import org.openremote.manager.server.event.EventService;
import org.openremote.manager.shared.agent.DeviceResourceValueEvent;
import org.openremote.manager.shared.agent.SubscribeDeviceResourceUpdates;
import org.openremote.manager.shared.agent.UnsubscribeDeviceResourceUpdates;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

// TODO this is quite a hack to get some publish/subscribe system going, it will have to be replaced
public class DeviceResourceSubscriptions {

    private static final Logger LOG = Logger.getLogger(DeviceResourceSubscriptions.class.getName());

    public static final int DEVICE_RESOURCE_SUBSCRIPTION_LIFETIME_SECONDS = 60;

    final protected EventService eventService;
    final protected Map<String, Set<DeviceResourceSubscription>> subscriptions = new ConcurrentHashMap<>();
    final protected ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    public DeviceResourceSubscriptions(EventService eventService) {
        this.eventService = eventService;

        scheduler.scheduleAtFixedRate(() -> {
            synchronized (subscriptions) {
                subscriptions.forEach((sessionKey, subscriptions) -> {
                    Iterator<DeviceResourceSubscription> it = subscriptions.iterator();
                    while (it.hasNext()) {
                        DeviceResourceSubscription subscription = it.next();
                        if (subscription.timestamp + (DEVICE_RESOURCE_SUBSCRIPTION_LIFETIME_SECONDS * 1000) < System.currentTimeMillis()) {
                            LOG.fine("Removing from session '" + sessionKey + "' due to timeout: " + subscription);
                            it.remove();
                        }
                    }
                    if (subscriptions.size() ==0) {
                        this.subscriptions.remove(sessionKey);
                    }
                });

            }
        }, DEVICE_RESOURCE_SUBSCRIPTION_LIFETIME_SECONDS, DEVICE_RESOURCE_SUBSCRIPTION_LIFETIME_SECONDS, TimeUnit.SECONDS);
    }

    public void addSubscription(String sessionKey, SubscribeDeviceResourceUpdates event) {
        DeviceResourceSubscription subscription = new DeviceResourceSubscription(
            event.getAgentId(),
            event.getDeviceKey(),
            System.currentTimeMillis()
        );
        synchronized (subscriptions) {
            Set<DeviceResourceSubscription> subscriptions = this.subscriptions.get(sessionKey);
            if (subscriptions == null) {
                subscriptions = new HashSet<>();
            }
            LOG.fine("Event session '" + sessionKey + "' adding/updating: " + subscription);
            subscriptions.add(subscription);
            this.subscriptions.put(sessionKey, subscriptions);
        }
    }

    public void removeSubscription(String sessionKey, UnsubscribeDeviceResourceUpdates event) {
        synchronized (subscriptions) {
            Set<DeviceResourceSubscription> subscriptions = this.subscriptions.get(sessionKey);
            if (subscriptions == null)
                return;
            Iterator<DeviceResourceSubscription> it = subscriptions.iterator();
            while (it.hasNext()) {
                DeviceResourceSubscription subscription = it.next();
                if (subscription.matches(event)) {
                    LOG.fine("Event session '" + sessionKey + "' removing: " + subscription);
                    it.remove();
                }
            }
        }
    }

    public void dispatch(DeviceResourceValueEvent updatedEvent) {
        synchronized (subscriptions) {
            for (Map.Entry<String, Set<DeviceResourceSubscription>> entry : subscriptions.entrySet()) {
                String sessionKey = entry.getKey();
                for (DeviceResourceSubscription subscription : entry.getValue()) {
                    if (subscription.matches(updatedEvent)) {
                        LOG.fine("For session '" + sessionKey + "' matching subscription of : " + updatedEvent);
                        eventService.sendEvent(sessionKey, updatedEvent);
                    }
                }
            }
        }
    }

}
