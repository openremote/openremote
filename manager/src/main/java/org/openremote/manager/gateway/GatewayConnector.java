/*
 * Copyright 2020, OpenRemote Inc.
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
package org.openremote.manager.gateway;

import org.openremote.manager.asset.AssetProcessingService;
import org.openremote.manager.asset.AssetStorageService;
import org.openremote.model.asset.*;
import org.openremote.model.asset.agent.ConnectionStatus;
import org.openremote.model.asset.impl.GatewayAsset;
import org.openremote.model.attribute.AttributeEvent;
import org.openremote.model.event.shared.EventRequestResponseWrapper;
import org.openremote.model.event.shared.SharedEvent;
import org.openremote.model.gateway.*;
import org.openremote.model.query.AssetQuery;
import org.openremote.model.syslog.SyslogCategory;
import org.openremote.model.util.Pair;
import org.openremote.model.util.UniqueIdentifierGenerator;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.openremote.model.syslog.SyslogCategory.GATEWAY;

/**
 * Handles all communication between a gateway and this manager instance
 */
public class GatewayConnector {

    private static final Logger LOG = SyslogCategory.getLogger(GATEWAY, GatewayConnector.class.getName());
    public static int MAX_SYNC_RETRIES = 5;
    public static int SYNC_ASSET_BATCH_SIZE = 20;
    public static final String ASSET_READ_EVENT_NAME_INITIAL = "INITIAL";
    public static final String ASSET_READ_EVENT_NAME_BATCH = "BATCH";
    public static final long RESPONSE_TIMEOUT_MILLIS = 10000;
    protected static final Map<String, Pair<Function<String, String>, Function<String, String>>> ASSET_ID_MAPPERS = new HashMap<>();
    protected final String realm;
    protected final String gatewayId;
    protected final AssetStorageService assetStorageService;
    protected final ScheduledExecutorService executorService;
    protected final AssetProcessingService assetProcessingService;
    protected final GatewayService gatewayService;
    protected final Map<String, Asset<?>> pendingAssetMerges = new HashMap<>();
    protected final AtomicReference<EventRequestResponseWrapper<DeleteAssetsRequestEvent>> pendingAssetDelete = new AtomicReference<>();
    protected List<AssetEvent> cachedAssetEvents;
    protected List<AttributeEvent> cachedAttributeEvents;
    protected Consumer<Object> gatewayMessageConsumer;
    protected Runnable requestDisconnect;
    protected final AtomicReference<String> sessionId = new AtomicReference<>();
    protected boolean disabled;
    protected boolean initialSyncInProgress;
    protected ScheduledFuture<?> syncProcessorFuture;
    List<String> syncAssetIds;
    int syncIndex;
    int syncErrors;
    String expectedSyncResponseName;
    protected boolean tunnellingSupported;
    protected final Map<Class<? extends SharedEvent>, BiConsumer<String, SharedEvent>> eventConsumerMap = new HashMap<>();

    protected static List<Integer> ALPHA_NUMERIC_CHARACTERS = new ArrayList<>(62);

    static {
        ALPHA_NUMERIC_CHARACTERS.addAll(
            Stream.concat(
                Stream.concat(
                    IntStream.rangeClosed('a', 'z').boxed(),
                    IntStream.rangeClosed('A', 'Z').boxed()
                ),
                IntStream.rangeClosed('0', '9').boxed()
            ).toList()
        );
    }

    protected GatewayConnector(
        AssetStorageService assetStorageService,
        AssetProcessingService assetProcessingService,
        ScheduledExecutorService executorService,
        GatewayService gatewayService,
        GatewayAsset gateway) {

        this.assetStorageService = assetStorageService;
        this.assetProcessingService = assetProcessingService;
        this.executorService = executorService;
        this.gatewayService = gatewayService;
        this.disabled = gateway.getDisabled().orElse(false);
        this.realm = gateway.getRealm();
        this.gatewayId = gateway.getId();

        // Setup static inbound event handling
        synchronized(eventConsumerMap) {
            eventConsumerMap.put(AssetEvent.class, (msgId, e) -> onAssetEvent((AssetEvent) e));
            eventConsumerMap.put(AttributeEvent.class, (msgId, e) -> onAttributeEvent((AttributeEvent) e));
            eventConsumerMap.put(DeleteAssetsResponseEvent.class, (msgId, e) -> onAssetDeleteResponseEvent(msgId, (DeleteAssetsResponseEvent) e));
        }
        publishAttributeEvent(new AttributeEvent(gatewayId, GatewayAsset.STATUS, ConnectionStatus.DISCONNECTED));
    }

    protected void sendMessageToGateway(Object message) {
        try {
            if (gatewayMessageConsumer != null) {
                gatewayMessageConsumer.accept(message);
            }
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Failed to send message to gateway: " + this, e);
        }
    }

    /**
     * Connection for this gateway has started so initiate synchronisation of assets
     */
    protected void connected(String sessionId, Consumer<Object> gatewayMessageConsumer, Runnable requestDisconnect) {

        LOG.fine("Gateway connector connected: " + this);

        synchronized (this.sessionId) {
            if (getSessionId() != null) {
                disconnect();
            }
            this.sessionId.set(sessionId);
        }
        this.gatewayMessageConsumer = gatewayMessageConsumer;
        this.requestDisconnect = requestDisconnect;

        // Reinitialise state
        initialSyncInProgress = true;
        syncProcessorFuture = null;
        cachedAssetEvents = new ArrayList<>();
        cachedAttributeEvents = new ArrayList<>();
        syncAssetIds = null;
        syncIndex = 0;
        syncErrors = 0;

        publishAttributeEvent(new AttributeEvent(gatewayId, GatewayAsset.STATUS, ConnectionStatus.CONNECTED));

        startSync();
    }

    /**
     * Connection to the edge gateway instance has been disconnected so stop any synchronisation
     */
    protected void disconnected(String sessionId) {
        synchronized (this.sessionId) {
            if (!sessionId.equals(this.sessionId.get())) {
                return;
            }
            this.sessionId.set(null);
        }

        LOG.fine("Gateway connector disconnected: " + this);
        if (syncProcessorFuture != null) {
            LOG.finest("Aborting active sync process: " + this);
            syncProcessorFuture.cancel(true);
        }

        initialSyncInProgress = false;
        pendingAssetMerges.clear();
        pendingAssetDelete.set(null);
        publishAttributeEvent(new AttributeEvent(gatewayId, GatewayAsset.STATUS, ConnectionStatus.DISCONNECTED));
    }

    protected void disconnect() {
        synchronized (this.sessionId) {
            if (isConnected()) {
                requestDisconnect.run();
                disconnected(getSessionId());
            }
        }
    }

    protected boolean isConnected() {
        return sessionId.get() != null;
    }

    protected boolean isInitialSyncInProgress() {
        return initialSyncInProgress;
    }

    protected boolean isTunnellingSupported() {
        return tunnellingSupported;
    }

    /**
     * Request for gateway capabilities such as tunneling support
     */
    protected CompletableFuture<GatewayCapabilitiesResponseEvent> getCapabilities() {

        final AtomicReference<GatewayCapabilitiesResponseEvent> responseRef = new AtomicReference<>();

        synchronized (eventConsumerMap) {
            if (eventConsumerMap.containsKey(GatewayCapabilitiesRequestEvent.class)) {
                return CompletableFuture.failedFuture(new IllegalArgumentException("A capabilities request is already pending"));
            }

            eventConsumerMap.put(GatewayCapabilitiesResponseEvent.class, (msgID, e) -> {
                GatewayCapabilitiesResponseEvent response = (GatewayCapabilitiesResponseEvent) e;

                synchronized (responseRef) {
                    responseRef.set(response);
                    responseRef.notify();
                }
            });
        }

        return CompletableFuture.supplyAsync(() -> {
                sendMessageToGateway(new EventRequestResponseWrapper<>(
                    UniqueIdentifierGenerator.generateId(),
                    new GatewayCapabilitiesRequestEvent()
                ));

                // Wait for response indefinitely as timeout handled on CompletableFuture
                try {
                    synchronized (responseRef) {
                        responseRef.wait();
                    }
                } catch (InterruptedException ignored) {
                } finally {
                    synchronized (eventConsumerMap) {
                        eventConsumerMap.remove(GatewayCapabilitiesResponseEvent.class);
                    }
                }
                return responseRef.get();
            }, executorService)
            .orTimeout(RESPONSE_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)
            .whenComplete((result, ex) -> {
                if (ex instanceof TimeoutException) {
                    synchronized (responseRef) {
                        responseRef.notify();
                    }
                }
            });
    }

    protected CompletableFuture<Void> startTunnel(GatewayTunnelInfo tunnelInfo) {

        if (!isConnected() || isInitialSyncInProgress()) {
            String msg = "Gateway is not connected or initial sync in progress so cannot start tunnel: " + this;
            LOG.info(msg);
            throw new IllegalStateException(msg);
        }

        final AtomicReference<GatewayTunnelStartResponseEvent> responseRef = new AtomicReference<>();

        synchronized (eventConsumerMap) {
            if (eventConsumerMap.containsKey(GatewayTunnelStartResponseEvent.class)) {
                return CompletableFuture.failedFuture(new IllegalArgumentException("A start tunnel request is already pending"));
            }

            eventConsumerMap.put(GatewayTunnelStartResponseEvent.class, (msgID, e) -> {
                GatewayTunnelStartResponseEvent response = (GatewayTunnelStartResponseEvent) e;

                synchronized (responseRef) {
                    responseRef.set(response);
                    responseRef.notify();
                }
            });
        }

        return CompletableFuture.runAsync(() -> {
            sendMessageToGateway(new EventRequestResponseWrapper<>(
                tunnelInfo.getId(),
                new GatewayTunnelStartRequestEvent(gatewayService.getTunnelSSHHostname(), gatewayService.getTunnelSSHPort(), tunnelInfo)
            ));

            // Wait for response indefinitely as timeout handled on CompletableFuture
            try {
                synchronized (responseRef) {
                    responseRef.wait();
                }

                GatewayTunnelStartResponseEvent responseEvent = responseRef.get();

                if (responseEvent != null && responseEvent.getError() != null) {
                    throw new RuntimeException("Failed to start tunnel: error=" + responseEvent.getError() + ", " + tunnelInfo);
                }
            } catch (InterruptedException ignored) {
            } finally {
                synchronized (eventConsumerMap) {
                    eventConsumerMap.remove(GatewayTunnelStartResponseEvent.class);
                }
            }
        }, executorService)
                .orTimeout(RESPONSE_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)
                .whenComplete((result, ex) -> {
                    if (ex instanceof TimeoutException) {
                        synchronized (responseRef) {
                            responseRef.notify();
                        }
                    }
                });
    }

    protected CompletableFuture<Void> stopTunnel(GatewayTunnelInfo tunnelInfo) {

        final AtomicReference<GatewayTunnelStopResponseEvent> responseRef = new AtomicReference<>();

        synchronized (eventConsumerMap) {
            if (eventConsumerMap.containsKey(GatewayTunnelStopResponseEvent.class)) {
                return CompletableFuture.failedFuture(new IllegalArgumentException("A stop tunnel request is already pending"));
            }

            eventConsumerMap.put(GatewayTunnelStopResponseEvent.class, (msgID, e) -> {
                GatewayTunnelStopResponseEvent response = (GatewayTunnelStopResponseEvent) e;

                synchronized (responseRef) {
                    responseRef.set(response);
                    responseRef.notify();
                }
            });
        }

        return CompletableFuture.runAsync(() -> {
                sendMessageToGateway(new EventRequestResponseWrapper<>(
                    tunnelInfo.getId(),
                    new GatewayTunnelStopRequestEvent(tunnelInfo)
                ));

                // Wait for response indefinitely as timeout handled on CompletableFuture
                try {
                    synchronized (responseRef) {
                        responseRef.wait();
                    }

                    GatewayTunnelStopResponseEvent responseEvent = responseRef.get();

                    if (responseEvent != null && responseEvent.getError() != null) {
                        throw new RuntimeException("Failed to stop tunnel: error=" + responseEvent.getError() + ", " + tunnelInfo);
                    }
                } catch (InterruptedException ignored) {
                } finally {
                    synchronized (eventConsumerMap) {
                        eventConsumerMap.remove(GatewayTunnelStopResponseEvent.class);
                    }
                }
            }, executorService)
            .orTimeout(RESPONSE_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)
            .whenComplete((result, ex) -> {
                if (ex instanceof TimeoutException) {
                    synchronized (responseRef) {
                        responseRef.notify();
                    }
                }
            });
    }

    protected String getRealm() {
        return realm;
    }

    protected boolean isDisabled() {
        return disabled;
    }

    protected void setDisabled(boolean disabled) {
        this.disabled = disabled;
        this.disconnect();
    }

    protected String getSessionId() {
        return sessionId.get();
    }

    protected void publishAttributeEvent(AttributeEvent event) {
        assetProcessingService.sendAttributeEvent(event, GatewayService.class.getName());
    }

    synchronized protected void onGatewayEvent(String messageId, SharedEvent e) {

        if (initialSyncInProgress) {
            if (e instanceof AssetsEvent) {
                onSyncAssetsResponse(messageId, (AssetsEvent) e);
            } else if (e instanceof AttributeEvent) {
                cachedAttributeEvents.add((AttributeEvent) e);
            } else if (e instanceof AssetEvent) {
                cachedAssetEvents.add((AssetEvent) e);
            }
        } else {
            synchronized (eventConsumerMap) {
                BiConsumer<String, SharedEvent> consumer = eventConsumerMap.get(e.getClass());
                if (consumer != null) {
                    consumer.accept(messageId, e);
                }
            }
        }
    }

    /**
     * Get list of gateway assets (get basic details and then batch load them to minimise load)
     */
    synchronized protected void startSync() {

        if (syncAborted()) {
            return;
        }

        expectedSyncResponseName = ASSET_READ_EVENT_NAME_INITIAL;
        sendMessageToGateway(new EventRequestResponseWrapper<>(
            ASSET_READ_EVENT_NAME_INITIAL,
            new ReadAssetsEvent(new AssetQuery().select(new AssetQuery.Select().excludeAttributes()).recursive(true))));
        syncProcessorFuture = executorService.schedule(this::onSyncAssetsTimeout, RESPONSE_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
    }

    /**
     * Called if a response isn't received from the gateway within {@link #RESPONSE_TIMEOUT_MILLIS}
     */
    synchronized protected void onSyncAssetsTimeout() {
        if (!isConnected()) {
            return;
        }

        LOG.info("Gateway sync timeout occurred: " + this);
        syncErrors++;

        if (syncAborted()) {
            return;
        }

        if (syncAssetIds == null) {
            // Haven't received initial list of assets so retry
            startSync();
        } else {
            requestAssets();
        }
    }

    protected boolean syncAborted() {
        if (syncErrors == MAX_SYNC_RETRIES) {
            LOG.warning("Gateway sync max retries reached so disconnecting the gateway: " + this);
            requestDisconnect.run();
            return true;
        }

        return false;
    }

    /**
     * Request assets in batches of {@link #SYNC_ASSET_BATCH_SIZE} to avoid overloading the gateway
     */
    protected void requestAssets() {

        if (syncAborted()) {
            return;
        }

        String[] requestAssetIds = syncAssetIds.stream().skip(syncIndex).limit(SYNC_ASSET_BATCH_SIZE).toArray(String[]::new);
        expectedSyncResponseName = ASSET_READ_EVENT_NAME_BATCH + syncIndex;

        LOG.fine("Synchronising gateway assets " + syncIndex+1 + "-" + syncIndex + requestAssetIds.length + " of " + syncAssetIds.size() + ": " + this);
        sendMessageToGateway(
            new EventRequestResponseWrapper<>(
                expectedSyncResponseName,
                new ReadAssetsEvent(
                    new AssetQuery()
                        .ids(requestAssetIds)
                )
            )
        );
        syncProcessorFuture = executorService.schedule(this::requestAssets, RESPONSE_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
    }

    synchronized protected void onSyncAssetsResponse(String messageId, AssetsEvent e) {
        if (!isConnected()) {
            return;
        }

        if (!expectedSyncResponseName.equalsIgnoreCase(messageId)) {
            LOG.info("Unexpected response from gateway so ignoring (expected=" + expectedSyncResponseName + ", actual =" + messageId + "): " + this);
            return;
        }

        syncProcessorFuture.cancel(true);
        syncProcessorFuture = null;
        boolean isInitialResponse = ASSET_READ_EVENT_NAME_INITIAL.equalsIgnoreCase(messageId);

        if (isInitialResponse) {

            // Put assets in hierarchical order
            Map<String, String> gatewayAssetIdParentIdMap = e.getAssets() == null ? Collections.emptyMap() : e.getAssets().stream()
                .collect(HashMap::new, (m, v) -> m.put(v.getId(), v.getParentId()), HashMap::putAll);

            ToIntFunction<Asset<?>> assetLevelExtractor = asset -> {
                int level = 0;
                String parentId = asset.getParentId();
                while (parentId != null) {
                    level++;
                    parentId = gatewayAssetIdParentIdMap.get(parentId);
                }
                return level;
            };

            syncAssetIds =  e.getAssets() == null ? Collections.emptyList() : e.getAssets()
                .stream()
                .sorted(Comparator.comparingInt(assetLevelExtractor))
                .map(Asset::getId)
                .collect(Collectors.toList());

            if (syncAssetIds.isEmpty()) {
                deleteObsoleteLocalAssets();
                onInitialSyncComplete();
                return;
            }

            requestAssets();

        } else {

            List<String> requestedAssetIds = syncAssetIds.stream().skip(syncIndex).limit(SYNC_ASSET_BATCH_SIZE).collect(Collectors.toList());
            List<Asset<?>> returnedAssets = e.getAssets() == null ? Collections.emptyList() : e.getAssets();

            // Remove any assets that have been deleted since requested
            cachedAssetEvents.removeIf(
                assetEvent -> {
                    boolean remove = requestedAssetIds.stream().anyMatch(id -> id.equals(assetEvent.getId()) && assetEvent.getCause() == AssetEvent.Cause.DELETE);
                    if (remove) {
                        syncAssetIds.remove(assetEvent.getId());
                        requestedAssetIds.remove(assetEvent.getId());
                    }
                    return remove;
                });

            if (returnedAssets.size() != requestedAssetIds.size() || !returnedAssets.stream().allMatch(asset -> requestedAssetIds.contains(asset.getId()))) {
                LOG.warning("Retrieved gateway asset batch count or ID mismatch, attempting to re-send the request: " + this);
                syncErrors++;
                requestAssets();
                return;
            }

            // Returned asset order may not match request order so re-order
            returnedAssets = returnedAssets.stream()
                .sorted(Comparator.comparingInt(a -> syncAssetIds.indexOf(a.getId())))
                .collect(Collectors.toList());

            // Merge returned assets ensuring the latest version of each is merged
            returnedAssets.stream()
                .map(returnedAsset -> {
                    final AtomicReference<Asset<?>> latestAssetVersion = new AtomicReference<>(returnedAsset);
                    cachedAssetEvents.removeIf(
                        assetEvent -> {
                            boolean remove = assetEvent.getId().equals(returnedAsset.getId()) && (assetEvent.getCause() == AssetEvent.Cause.UPDATE || assetEvent.getCause() == AssetEvent.Cause.READ);
                            if (remove && assetEvent.getAsset().getVersion() > latestAssetVersion.get().getVersion()) {
                                latestAssetVersion.set(assetEvent.getAsset());
                            }
                            return remove;
                        });
                    return latestAssetVersion.get();
                }).forEach(this::saveAssetLocally);


            // Request next batch or move on
            syncIndex += requestedAssetIds.size();
            if (syncIndex >= syncAssetIds.size()) {
                LOG.info("All requested gateway assets retrieved: " + this);

                Set<String> refreshAssets = new HashSet<>();

                cachedAssetEvents.forEach(
                    assetEvent -> {
                        if (assetEvent.getCause() == AssetEvent.Cause.DELETE) {
                            syncAssetIds.remove(assetEvent.getId());
                        } else if (assetEvent.getCause() == AssetEvent.Cause.CREATE) {
                            syncAssetIds.add(assetEvent.getId());
                            try {
                                saveAssetLocally(assetEvent.getAsset());
                            } catch (Exception ex) {
                                LOG.log(Level.SEVERE, "Failed to add new gateway asset (Asset=" + assetEvent.getAsset() + "): " + this, ex);
                            }
                        } else {
                            refreshAssets.add(assetEvent.getId());
                        }
                    }
                );

                deleteObsoleteLocalAssets();
                onInitialSyncComplete();

                // Refresh attributes that have changed
                cachedAttributeEvents.forEach(attributeEvent -> {
                    String assetId = attributeEvent.getId();
                    if (!refreshAssets.contains(assetId)) {
                        LOG.info("1 or more gateway asset attribute values have changed so requesting the asset again (Asset<?> ID=" + assetId + ": " + this);
                        refreshAssets.add(assetId);
                    }
                });

                // Refresh assets that have changed
                refreshAssets.forEach(id -> sendMessageToGateway(new ReadAssetEvent(id)));
            } else {
                requestAssets();
            }
        }
    }

    protected void deleteObsoleteLocalAssets() {

        // Find obsolete local assets
        List<Asset<?>> localAssets = assetStorageService.findAll(
            new AssetQuery()
                .select(new AssetQuery.Select().excludeAttributes())
                .recursive(true)
                .parents(gatewayId)
        );

        // Delete obsolete assets
        List<String> obsoleteLocalAssetIds = localAssets.stream()
            .map(Asset::getId)
            .filter(id -> !syncAssetIds.contains(mapAssetId(gatewayId, id, true)))
            .toList();

        if (!obsoleteLocalAssetIds.isEmpty()) {
            boolean deleted = deleteAssetsLocally(obsoleteLocalAssetIds);
            if (!deleted) {
                LOG.warning("Failed to delete obsolete local gateway assets; assets are not correctly synced: " + this);
            }
        }
    }

    protected void onInitialSyncComplete() {
        initialSyncInProgress = false;
        cachedAssetEvents.clear();
        cachedAttributeEvents.clear();

        getCapabilities().whenComplete ((response, error) -> {
            if (error != null) {
                LOG.warning("An error occurred whilst getting the gateway capabilities, assuming no support: " + this);
            }
            tunnellingSupported = response != null && response.isTunnelingSupported();
            publishAttributeEvent(new AttributeEvent(gatewayId, GatewayAsset.TUNNELING_SUPPORTED, tunnellingSupported));
        });
    }

    @SuppressWarnings("unchecked")
    protected <T extends Asset<?>> T mergeGatewayAsset(T asset, boolean isUpdate) {

        if (!isConnected() || isInitialSyncInProgress()) {
            String msg = "Gateway is not connected or initial sync in progress so cannot merge asset: Asset<?> ID=" + asset.getId() + ": " + this;
            LOG.info(msg);
            throw new IllegalStateException(msg);
        }

        final String id = asset.getId();
        final String parentId = asset.getParentId();

        synchronized (pendingAssetMerges) {

            if (id != null && pendingAssetMerges.containsKey(id)) {
                String msg = "Gateway asset merge already pending for this asset: Asset<?> ID=" + id + ": " + this;
                LOG.info(msg);
                throw new IllegalStateException(msg);
            }

            if (id == null) {
                // Generate an ID to allow tracking the asset when it is returned from the gateway
                asset.setId(UniqueIdentifierGenerator.generateId());
            } else {
                // Put original gateway asset ID back
                asset.setId(mapAssetId(gatewayId, id, true));
            }

            if (gatewayId.equals(parentId)) {
                asset.setParentId(null);
            } else if (parentId != null) {
                // Put original parent asset ID back
                asset.setParentId(mapAssetId(gatewayId, parentId, true));
            }

            sendMessageToGateway(new AssetEvent(isUpdate ? AssetEvent.Cause.UPDATE : AssetEvent.Cause.CREATE, asset, null));
            pendingAssetMerges.put(asset.getId(), null);
        }

        synchronized (asset.getId()) {
            try {
                asset.getId().wait(RESPONSE_TIMEOUT_MILLIS);

                T mergedAsset;
                synchronized (pendingAssetMerges) {
                    mergedAsset = (T)pendingAssetMerges.remove(asset.getId());
                }

                if (mergedAsset == null) {
                    throw new IllegalStateException("Gateway asset merge failed:  Asset ID=" + asset.getId() + ", Asset ID Mapped=" + id + ": " + this);
                }

                return mergedAsset;

            } catch (InterruptedException e) {
                String msg = "Gateway asset merge interrupted: Asset ID=" + asset.getId() + ", Asset ID Mapped=" + id + ": " + this;
                LOG.info(msg);
                throw new IllegalStateException(msg);
            } finally {
                synchronized (pendingAssetMerges) {
                    pendingAssetMerges.remove(asset.getId());
                }
                asset.setId(id);
                asset.setParentId(parentId);
            }
        }
    }

    protected boolean deleteGatewayAssets(List<String> assetIds) {

        if (!isConnected() || isInitialSyncInProgress()) {
            String msg = "Gateway is not connected or initial sync in progress so cannot delete asset(s):  Asset<?> IDs=" + Arrays.toString(assetIds.toArray()) + ": " + this;
            LOG.info(msg);
            throw new IllegalStateException(msg);
        }

        synchronized (pendingAssetDelete) {

            if (pendingAssetDelete.get() != null) {
                String msg = "Gateway asset delete already pending: Asset<?> IDs Mapped=" + Arrays.toString(pendingAssetDelete.get().getEvent().getAssetIds().toArray()) + ": " + this;
                LOG.info(msg);
                throw new IllegalStateException(msg);
            }

            List<String> originalIds = assetIds.stream().map(id -> mapAssetId(gatewayId, id, true)).toList();

            pendingAssetDelete.set(new EventRequestResponseWrapper<>(
                UniqueIdentifierGenerator.generateId(),
                new DeleteAssetsRequestEvent(new ArrayList<>(originalIds))
            ));

            try {
                sendMessageToGateway(pendingAssetDelete.get());
                pendingAssetDelete.wait(RESPONSE_TIMEOUT_MILLIS);
                if (pendingAssetDelete.get() != null) {
                    throw new IllegalStateException("Gateway asset delete failed: Asset<?> IDs=" + originalIds + ", Asset<?> IDs Mapped=" + Arrays.toString(assetIds.toArray()) + ": " + this);
                }
                return true;
            } catch (InterruptedException e) {
                String msg = "Gateway asset delete interrupted: Asset<?> IDs=" + originalIds + ", Asset<?> IDs Mapped=" + Arrays.toString(assetIds.toArray()) + ": " + this;
                LOG.info(msg);
                throw new IllegalStateException(msg);
            } finally {
                pendingAssetDelete.set(null);
            }
        }
    }

    protected void onAssetDeleteResponseEvent(String messageId, DeleteAssetsResponseEvent e) {

        synchronized (pendingAssetDelete) {
            if (pendingAssetDelete.get() == null) {
                return;
            }

            if (!pendingAssetDelete.get().getMessageId().equals(messageId)) {
                LOG.info("Gateway asset delete response name does not match request so ignoring: " + this);
                return;
            }

            if (e.isDeleted()) {
                pendingAssetDelete.set(null);
            }

            pendingAssetDelete.notify();
        }
    }

    @SuppressWarnings("SynchronizationOnLocalVariableOrMethodParameter")
    synchronized protected void onAssetEvent(AssetEvent e) {

        switch (e.getCause()) {
            case CREATE, READ, UPDATE -> {
                String assetId = e.getId();
                Asset<?> mergedAsset = saveAssetLocally(e.getAsset());
                synchronized (pendingAssetMerges) {
                    if (pendingAssetMerges.containsKey(assetId)) {
                        @SuppressWarnings("OptionalGetWithoutIsPresent")
                        Map.Entry<String, Asset<?>> pendingAssetMergeEntry = pendingAssetMerges.entrySet().stream().filter(entry -> entry.getKey().equals(assetId)).findFirst().get();
                        String id = pendingAssetMergeEntry.getKey();
                        pendingAssetMergeEntry.setValue(mergedAsset);

                        synchronized (id) {
                            // Notify the waiting merge thread
                            id.notify();
                        }
                    }
                }
            }
            case DELETE -> {
                try {
                    deleteAssetsLocally(Collections.singletonList(mapAssetId(gatewayId, e.getId(), false)));
                } catch (Exception ex) {
                    LOG.log(Level.SEVERE, "Removing obsolete asset failed: " + e.getId() + ": " + this, ex);
                }
            }
        }
    }

    protected void onAttributeEvent(AttributeEvent e) {
        // Just push the event through the processing chain
        publishAttributeEvent(new AttributeEvent(mapAssetId(gatewayId, e.getId(), false), e.getName(), e.getValue().orElse(null), e.getTimestamp()));
    }

    protected <T extends Asset<?>> T saveAssetLocally(T asset) {
        String assetId = asset.getId();
        asset.setId(mapAssetId(gatewayId, assetId, false));
        asset.setParentId(asset.getParentId() != null ? mapAssetId(gatewayId, asset.getParentId(), false) : gatewayId);
        asset.setRealm(realm);
        LOG.fine("Creating/updating gateway asset: Asset ID=" + assetId + ", Asset ID Mapped=" + asset.getId() + ": " + this);
        return assetStorageService.merge(asset, true, true, null);
    }

    protected boolean deleteAssetsLocally(List<String> assetIds) {
        LOG.fine("Removing gateway asset: Asset IDs=" + Arrays.toString(assetIds.toArray()) + ": " + this);
        return assetStorageService.delete(assetIds, true);
    }

    @Override
    public String toString() {
        return GatewayConnector.class.getSimpleName() + "{" +
            "gatewayId='" + gatewayId + '\'' +
            '}';
    }

    /**
     * An easily reversible mathematical way of ensuring gateway asset IDs are unique by incrementing the first two
     * characters by adding the first two characters of the gateway ID for inbound IDs and the reverse for outbound.
     */
    public static String mapAssetId(String gatewayId, String assetId, boolean outbound) {
        Pair<Function<String, String>, Function<String, String>> gatewayIdMappers = ASSET_ID_MAPPERS.computeIfAbsent(gatewayId, gwId -> {
            int g1 = gatewayId.charAt(0) % ALPHA_NUMERIC_CHARACTERS.size();
            int g2 = gatewayId.charAt(1) % ALPHA_NUMERIC_CHARACTERS.size();

            BiFunction<Integer, String, String> mapper = (sign, id) -> {
                int a1 = (ALPHA_NUMERIC_CHARACTERS.indexOf((int)id.charAt(0)) + (sign * g1) + ALPHA_NUMERIC_CHARACTERS.size()) % ALPHA_NUMERIC_CHARACTERS.size();
                int a2 = (ALPHA_NUMERIC_CHARACTERS.indexOf((int)id.charAt(1)) + (sign * g2) + ALPHA_NUMERIC_CHARACTERS.size()) % ALPHA_NUMERIC_CHARACTERS.size();
                return String.valueOf((char)ALPHA_NUMERIC_CHARACTERS.get(a1).intValue()) + ((char)ALPHA_NUMERIC_CHARACTERS.get(a2).intValue()) + id.substring(2);
            };

            return new Pair<>(
                id -> mapper.apply(1, id), // Inbound
                id -> mapper.apply(-1, id) // Outbound
            );
        });

        return outbound ? gatewayIdMappers.value.apply(assetId) : gatewayIdMappers.key.apply(assetId);
    }
}
