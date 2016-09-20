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
package org.openremote.manager.server.asset;

import org.openremote.container.persistence.PersistenceEvent;
import org.openremote.manager.server.event.EventService;
import org.openremote.manager.shared.asset.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

// TODO this is quite a hack to get some publish/subscribe system going, it will have to be replaced
// TODO More fine grained selection assets to subscribe to - currently all
public abstract class AssetListenerSubscriptions {

    private static final Logger LOG = Logger.getLogger(AssetListenerSubscriptions.class.getName());

    final protected int ASSET_LISTENERS_MAX_LIFETIME_SECONDS = 60;

    final protected EventService eventService;
    final protected Map<String, Long> subscriptions = new ConcurrentHashMap<>();
    final protected ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    public AssetListenerSubscriptions(EventService eventService) {
        this.eventService = eventService;

        // Max life time of listeners is 60 seconds
        scheduler.scheduleAtFixedRate(() -> {
            subscriptions.forEach((sessionKey, timeStamp) -> {
                if (timeStamp + (ASSET_LISTENERS_MAX_LIFETIME_SECONDS * 1000) < System.currentTimeMillis()) {
                    LOG.fine("Removing asset listener session due to timeout: " + sessionKey);
                    subscriptions.remove(sessionKey);
                }
            });
        }, ASSET_LISTENERS_MAX_LIFETIME_SECONDS, ASSET_LISTENERS_MAX_LIFETIME_SECONDS, TimeUnit.SECONDS);

    }

    public void addSubscription(String sessionKey, SubscribeAssetModified event) {
        Long timestamp = System.currentTimeMillis();
        LOG.fine("Subscribing/updating subscription to asset change events for session: " + sessionKey);
        subscriptions.put(sessionKey, timestamp);
    }

    public void removeSubscription(String sessionKey, UnsubscribeAssetModified event) {
        LOG.fine("Unsubscribing from asset change events for session: " + sessionKey);
        subscriptions.remove(sessionKey);
    }

    public void dispatch(PersistenceEvent persistenceEvent) {
        //noinspection unchecked
        for (AssetModifiedEvent assetModifiedEvent : createAssetModifiedEvents(persistenceEvent)) {
            LOG.fine("Publishing asset modified event to asset listeners: " + subscriptions.size());
            for (String sessionKey : subscriptions.keySet()) {
                eventService.sendEvent(sessionKey, assetModifiedEvent);
            }
        }
    }

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
                    events.add(new AssetModifiedEvent(new AssetInfo(), AssetModifiedEvent.Cause.CHILDREN_MODIFIED));
                }
                break;
            case UPDATE:
                cause = AssetModifiedEvent.Cause.UPDATE;

                // Find out if the asset has a new parent
                String previousParentId = persistenceEvent.getPreviousState("parentId");
                String currentParentId = persistenceEvent.getCurrentState("parentId");
                if (previousParentId == null && currentParentId == null) {
                    // The "root without id" asset was modified
                    events.add(new AssetModifiedEvent(new AssetInfo(), AssetModifiedEvent.Cause.CHILDREN_MODIFIED));
                } else {
                    if (previousParentId == null) {
                        // The "root without id" asset was modified
                        events.add(new AssetModifiedEvent(new AssetInfo(), AssetModifiedEvent.Cause.CHILDREN_MODIFIED));
                        modifiedParentIds.add(currentParentId);
                    } else if (currentParentId == null) {
                        // The "root without id" asset was modified
                        events.add(new AssetModifiedEvent(new AssetInfo(), AssetModifiedEvent.Cause.CHILDREN_MODIFIED));
                        modifiedParentIds.add(previousParentId);
                    }
                }

                // Only send "children modified" of the parent if the name of the asset changed
                String previousName = persistenceEvent.getPreviousState("name");
                String currentName = persistenceEvent.getCurrentState("name");
                boolean isEqualName = Objects.equals(previousName, currentName);
                if (!isEqualName){
                    modifiedParentIds.add(asset.getParentId());
                }

                break;
            case DELETE:
                cause = AssetModifiedEvent.Cause.DELETE;
                if (asset.getParentId() != null) {
                    modifiedParentIds.add(asset.getParentId());
                } else {
                    // The "root without id" asset was modified
                    events.add(new AssetModifiedEvent(new AssetInfo(), AssetModifiedEvent.Cause.CHILDREN_MODIFIED));
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

    abstract protected Asset getAsset(String assetId);
}
