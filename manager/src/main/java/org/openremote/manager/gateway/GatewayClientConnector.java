package org.openremote.manager.gateway;

import io.netty.channel.ChannelHandler;
import org.apache.http.client.utils.URIBuilder;
import org.openremote.agent.protocol.io.AbstractNettyIOClient;
import org.openremote.agent.protocol.websocket.WebsocketIOClient;
import org.openremote.container.Container;
import org.openremote.container.timer.TimerService;
import org.openremote.manager.asset.AssetProcessingService;
import org.openremote.manager.asset.AssetStorageService;
import org.openremote.manager.event.ClientEventService;
import org.openremote.manager.rules.AssetQueryPredicate;
import org.openremote.manager.system.VersionInfo;
import org.openremote.model.Constants;
import org.openremote.model.asset.*;
import org.openremote.model.asset.agent.ConnectionStatus;
import org.openremote.model.attribute.*;
import org.openremote.model.auth.OAuthClientCredentialsGrant;
import org.openremote.model.event.shared.EventFilter;
import org.openremote.model.event.shared.SharedEvent;
import org.openremote.model.gateway.*;
import org.openremote.model.query.AssetQuery;
import org.openremote.model.query.filter.RealmPredicate;
import org.openremote.model.syslog.SyslogCategory;
import org.openremote.model.util.Pair;
import org.openremote.model.util.ValueUtil;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static org.openremote.manager.gateway.GatewayClientService.CLIENT_EVENT_SESSION_PREFIX;
import static org.openremote.manager.gateway.GatewayConnector.ASSET_READ_EVENT_NAME_INITIAL;
import static org.openremote.model.syslog.SyslogCategory.GATEWAY;

/**
 * Handles all communication with the central gateway instance. Some history of gateway API:
 * <ol>
 * <li>ALPHA (pre GATEWAY_API_VERSION) NO LONGER SUPPORTED - Initial implementation; client connects to central manager
 * and then central manager sends various {@link ReadAssetsEvent}s to request all assets from central manager in batches;
 * first event asks for all assets without attributes and then paginates full retrieval of the assets in subsequent calls.
 * </li>
 * <li>BETA (pre GATEWAY_API_VERSION) - Gateway tunneling introduced; same as ALPHA but after asset sync is complete a
 * {@link GatewayCapabilitiesRequestEvent} is sent with no content to determine if the client supports tunneling and a
 * {@link GatewayCapabilitiesResponseEvent} is returned with {@code tunnelingSupported:true|false}. Each tunnel start
 * request contains the public SSH hostname and port to use (which don't actually change from one request to the next)</li>
 * <li>1.0.0 - Refactor to improve connection synchronisation and also to support active tunnel synchronisation. A
 * {@link GatewayInitStartEvent} is sent with a list of active tunnels, gateway API version and tunnel public hostname
 * and port; the client can then synchronise the SSH sessions (for reconnections) and should also send a
 * {@link AssetsEvent} with all the edge gatewway assets (without attributes) and this replaces the initial
 * {@link ReadAssetsEvent} that previous versions would send.</li>
 * </li>
 * </ol>
 */
public class GatewayClientConnector implements AutoCloseable {
    private static final Logger LOG = SyslogCategory.getLogger(GATEWAY, GatewayClientConnector.class.getName());
    protected static final int GATEWAY_SYNC_TIMEOUT_MILLIS = 60000;
    protected final ClientEventService clientEventService;
    protected final TimerService timerService;
    protected final AssetStorageService assetStorageService;
    protected final AssetProcessingService assetProcessingService;
    protected GatewayConnection connection;
    protected WebsocketIOClient<String> client;
    protected GatewayTunnelFactory tunnelFactory;
    protected final List<GatewayTunnelSession> activeTunnelSessions;
    protected Map<String, Map<AttributeRef, Long>> attributeTimestamps;
    protected Consumer<AssetEvent> realmAssetEventConsumer;
    protected Consumer<AttributeEvent> realmAttributeEventConsumer;
    protected String gatewayAPIVersion;
    protected String tunnelHostname;
    protected Integer tunnelPort;
    protected CompletableFuture<Void> initFuture;

    public GatewayClientConnector(GatewayConnection connection,
                                  GatewayTunnelFactory tunnelFactory,
                                  ClientEventService clientEventService,
                                  TimerService timerService,
                                  AssetStorageService assetStorageService,
                                  AssetProcessingService assetProcessingService) {
        this.connection = connection;
        this.tunnelFactory = tunnelFactory;
        this.clientEventService = clientEventService;
        this.timerService = timerService;
        this.assetStorageService = assetStorageService;
        this.assetProcessingService = assetProcessingService;
        activeTunnelSessions = tunnelFactory != null ? new CopyOnWriteArrayList<>() : null;

        if (!connection.isDisabled()) {
            client = createClient();
            if (connection.getAssetSyncRules() != null && !connection.getAssetSyncRules().isEmpty()) {
                attributeTimestamps = new ConcurrentHashMap<>();
            }
        }
    }

    @Override
    public void close() throws Exception {
        if (client != null) {
            LOG.info("Destroying gateway IO client: " + connection);
            try {
                client.disconnect();
                client.removeAllConnectionStatusConsumers();
                client.removeAllMessageConsumers();
                client.setEncoderDecoderProvider(null);
            } catch (Exception e) {
                LOG.log(Level.WARNING, "An exception occurred whilst trying to disconnect the gateway IO client", e);
            } finally {
                client = null;
            }
        }

        if (realmAttributeEventConsumer != null) {
            clientEventService.removeSubscription(realmAttributeEventConsumer);
        }

        if (realmAssetEventConsumer != null) {
            clientEventService.removeSubscription(realmAssetEventConsumer);
        }
    }

    protected WebsocketIOClient<String> createClient() {
        LOG.info("Creating gateway IO client: " + connection);

        if (connection.isDisabled()) {
            LOG.info("Disabled gateway client connection so ignoring: " + connection);
            return null;
        }

        try {
            WebsocketIOClient<String> client = new WebsocketIOClient<>(
                    new URIBuilder()
                            .setScheme(connection.isSecured() ? "wss" : "ws")
                            .setHost(connection.getHost())
                            .setPort(connection.getPort() == null ? -1 : connection.getPort())
                            .setPath("websocket/events")
                            .setParameter(Constants.REALM_PARAM_NAME, connection.getRealm()).build(),
                    null,
                    new OAuthClientCredentialsGrant(
                            new URIBuilder()
                                    .setScheme(connection.isSecured() ? "https" : "http")
                                    .setHost(connection.getHost())
                                    .setPort(connection.getPort() == null ? -1 : connection.getPort())
                                    .setPath("auth/realms/" + connection.getRealm() + "/protocol/openid-connect/token")
                                    .build().toString(),
                            connection.getClientId(),
                            connection.getClientSecret(),
                            null).setBasicAuthHeader(true)
            );

            client.setConnectTimeoutMillis(GATEWAY_SYNC_TIMEOUT_MILLIS);
            client.setEncoderDecoderProvider(() ->
                    new ChannelHandler[] {new AbstractNettyIOClient.MessageToMessageDecoder<>(String.class, client)}
            );
            client.addConnectionStatusConsumer(this::onClientConnectionStatusChanged);
            client.addMessageConsumer(this::onCentralManagerMessage);
            client.setInitFutureSupplier(this::doInit);

            // Subscribe to Asset<?> and attribute events of local realm and pass through to connected manager
            realmAssetEventConsumer = this::sendAssetEvent;
            clientEventService.addSubscription(
                    AssetEvent.class,
                    new AssetFilter<AssetEvent>().setRealm(connection.getLocalRealm()),
                    realmAssetEventConsumer);

            realmAttributeEventConsumer = this::sendAttributeEvent;
            clientEventService.addSubscription(
                    AttributeEvent.class,
                    getOutboundAttributeEventFilter(),
                    realmAttributeEventConsumer);

            client.connect();
            return client;

        } catch (Exception e) {
            LOG.log(Level.WARNING, "Creating gateway IO client failed so marking connection as disabled: " + connection, e);
            connection.setDisabled(true);
        }

        return null;
    }

    protected CompletableFuture<Void> doInit() {
        // Set init future which will be completed when gateway asset sync is complete
        initFuture = new CompletableFuture<>();
        return initFuture;
    }

    protected void onClientConnectionStatusChanged(ConnectionStatus connectionStatus) {
        LOG.info("Connection status change for gateway IO client '" + connectionStatus + "': " + connection);

        if (connectionStatus == ConnectionStatus.CONNECTED) {
            LOG.info("Gateway client initialisation complete API version is '" + gatewayAPIVersion + "': " + connection);
        }

        clientEventService.publishEvent(new GatewayConnectionStatusEvent(connection.getLocalRealm(), connectionStatus));
    }

    protected void onCentralManagerMessage(String message) {
        SharedEvent event = messageFromString(message, SharedEvent.MESSAGE_PREFIX, SharedEvent.class);

        if (event == null) {
            LOG.finer("Received empty message from central manager: realm=" + connection.getLocalRealm());
            return;
        }

        LOG.finer(() -> "Received message from central manager: realm=" + connection.getLocalRealm() + ", " + event);

        switch (event) {
            // Sent by API version >=1.0.0 on initial connection to central manager
            case GatewayInitStartEvent initStartEvent -> {
                gatewayAPIVersion = initStartEvent.getVersion();
                tunnelHostname = initStartEvent.getTunnelHostname();
                tunnelPort = initStartEvent.getTunnelPort();

                if (!gatewayAPIVersion.equals(VersionInfo.getGatewayApiVersion())) {
                    LOG.warning("Gateway API version mismatch: Central manager API version is '" + gatewayAPIVersion + "' but this manager is '" + VersionInfo.getGatewayApiVersion() + "': " + connection);
                }

                doTunnelSync(initStartEvent.getActiveTunnels());

                // Get all assets in the connections realm
                AssetQuery query = new AssetQuery()
                        .realm((new RealmPredicate(connection.getLocalRealm())))
                        .select(new AssetQuery.Select().excludeAttributes()).recursive(true);
                List<Asset<?>> assets = assetStorageService.findAll(query);
                assets = assets.stream()
                        .map(it -> this.applySyncRules(it, connection.getAssetSyncRules()))
                        .collect(Collectors.toList());
                AssetsEvent responseEvent = new AssetsEvent(assets);
                responseEvent.setMessageID(ASSET_READ_EVENT_NAME_INITIAL);
                sendCentralManagerMessage(responseEvent);
            }
            case GatewayInitDoneEvent initDoneEvent -> {
                initFuture.complete(null);
            }
            // The central manager sends N number of these to synchronise gateway descendant assets on the central manager
            case ReadAssetsEvent readAssets -> {
                if (gatewayAPIVersion == null) {
                    LOG.finer("Pre version 1.0.0 central manager read assets request so closing all active tunnels: " + connection);
                    stopAllGatewayTunnels();
                }

                AssetQuery query = readAssets.getAssetQuery();
                // Force realm to be the one that this client is associated with
                query.realm(new RealmPredicate(connection.getLocalRealm()));
                List<Asset<?>> assets = assetStorageService.findAll(readAssets.getAssetQuery());
                assets = assets.stream()
                        .map(it -> this.applySyncRules(it, connection.getAssetSyncRules()))
                        .collect(Collectors.toList());
                AssetsEvent responseEvent = new AssetsEvent(assets);
                responseEvent.setMessageID(event.getMessageID());
                sendCentralManagerMessage(responseEvent);
            }
            // This event is only sent once asset synchronisation has completed but before init done
            case GatewayCapabilitiesRequestEvent capabilitiesRequestEvent -> {
                if (gatewayAPIVersion == null) {
                    // This is a legacy version of the openremote on the central instance so we won't get a GatewayInitialisedEvent
                    // so we mark this client as connected now
                    LOG.fine("Central manager running an older version so assuming connection is initialised");
                    initFuture.complete(null);
                }

                GatewayCapabilitiesResponseEvent responseEvent = new GatewayCapabilitiesResponseEvent(tunnelFactory != null);
                responseEvent.setMessageID(event.getMessageID());
                sendCentralManagerMessage(responseEvent);
            }
            // Central manager is about to disconnect this client
            case GatewayDisconnectEvent disconnectEvent ->
                LOG.info("Central manager requested disconnect: reason=" + disconnectEvent.getReason());
            // An attribute event has occurred for a gateway descendant asset
            case AttributeEvent attributeEvent ->
                    assetProcessingService.sendAttributeEvent(attributeEvent, getClass().getSimpleName());
            // An asset event has occurred for a gateway descendant asset
            case AssetEvent assetEvent -> {
                if (assetEvent.getCause() == AssetEvent.Cause.CREATE || assetEvent.getCause() == AssetEvent.Cause.UPDATE) {
                    Asset<?> asset = assetEvent.getAsset();
                    asset.setRealm(connection.getLocalRealm());
                    LOG.fine("Request from central manager to create/update an asset: Realm=" + connection.getLocalRealm() + ", Asset ID=" + asset.getId());
                    try {
                        assetStorageService.merge(asset, true);
                    } catch (Exception e) {
                        LOG.log(Level.INFO, "Request from central manager to create/update an asset failed: Realm=" + connection.getLocalRealm() + ", Asset ID=" + asset.getId(), e);
                    }
                }
            }
            case GatewayTunnelStartRequestEvent startRequestEvent -> {
                if (tunnelFactory == null) {
                    LOG.finest("Gateway tunnel creation request received but gateway tunnel factory is not available: realm=" + connection.getLocalRealm());
                    return;
                }
                if (tunnelHostname == null || tunnelPort == 0) {
                    // If we don't have tunnel hostname and port already then this is a legacy manager so try and get from
                    // the start request event - it never changes in legacy manager so we can safely do this once
                    // TODO: Remove this once enough time has passed since this commit was made
                    tunnelHostname = startRequestEvent.getSshHostname();
                    tunnelPort = startRequestEvent.getSshPort();
                }
                startGatewayTunnel(startRequestEvent.getInfo()).whenComplete((v, t) -> {
                    String error = t != null ? t.getMessage() : null;

                    if (t != null) {
                        LOG.warning("Gateway tunnel creation failed: " + t);
                    }
                    GatewayTunnelStartResponseEvent responseEvent = new GatewayTunnelStartResponseEvent(error);
                    responseEvent.setMessageID(startRequestEvent.getMessageID());
                    sendCentralManagerMessage(responseEvent);
                });
            }
            case GatewayTunnelStopRequestEvent stopRequestEvent -> {
                if (tunnelFactory == null) {
                    LOG.finest("Gateway tunnel creation request received but gateway tunnel factory is not available: realm=" + connection.getLocalRealm());
                    return;
                }
                String error = stopGatewayTunnel(stopRequestEvent);
                GatewayTunnelStopResponseEvent responseEvent = new GatewayTunnelStopResponseEvent(error);
                responseEvent.setMessageID(event.getMessageID());
                sendCentralManagerMessage(responseEvent);
            }
            default -> {
                LOG.info("Received unknown event from central manager: " + event);
            }
        }
    }

    /**
     * Synchronise the current active tunnel sessions async with what the central instance thinks are open.
     */
    protected void doTunnelSync(GatewayTunnelInfo[] activeTunnels) {
        if (tunnelFactory == null) {
            return;
        }

        Container.EXECUTOR.submit(() -> {

            if (activeTunnels == null || activeTunnels.length == 0) {
                LOG.fine("No gateway tunnel sessions on central instance so stopping any active sessions: " + connection);
                stopAllGatewayTunnels();
                return;
            }

            LOG.fine("Synchronising gateway tunnel sessions with central instance: " + connection);

            // Synchronise the SSH sessions with what the central instance thinks are open
            // Filter out any tunnels expiring in the next 5 seconds
            List<GatewayTunnelInfo> tunnels = Arrays.stream(activeTunnels).filter(activeTunnel -> {
                if (Duration.between(activeTunnel.getAutoCloseTime(), timerService.getNow()).getSeconds() <= 5) {
                    LOG.finer("Ignoring tunnel that is expiring soon: " + activeTunnel);
                    return false;
                }
                return true;
            }).toList();

            // Create any new tunnels currently not established
            tunnels.forEach(activeTunnel -> {
                GatewayTunnelSession activeSession = this.activeTunnelSessions.stream()
                        .filter(activeTunnelSession -> activeTunnelSession.getTunnelInfo().equals(activeTunnel))
                        .findFirst()
                        .orElse(null);
                if (activeSession != null) {
                    LOG.finer("Active tunnel session found for tunnel: " + activeTunnel);
                } else {
                    // No existing session for this active tunnel
                    LOG.fine("Active tunnel session not found for tunnel so starting: " + activeTunnel);
                    startGatewayTunnel(activeTunnel);
                }
            });

            // Remove any tunnels that are no longer active
            this.activeTunnelSessions.removeIf(activeTunnelSession -> {
                boolean obsolete = tunnels.stream().noneMatch(tunnel -> tunnel.equals(activeTunnelSession.getTunnelInfo()));
                if (obsolete) {
                    LOG.fine("Removing obsolete tunnel session: " + activeTunnelSession);
                }
                return obsolete;
            });
        });
    }

    protected void sendAssetEvent(AssetEvent event) {
        if (connection.getAssetSyncRules() != null) {
            // Apply sync rules to the asset
            event = ValueUtil.clone(event);
            applySyncRules(event.getAsset(), connection.getAssetSyncRules());
        }
        sendCentralManagerMessage(event);
    }

    protected void sendAttributeEvent(AttributeEvent event) {
        if (connection.getAssetSyncRules() != null) {
            // Attribute exclusion from sync rules has already been applied
            // Apply sync rules to the event meta
            event = ValueUtil.clone(event);
            event.setMeta(event.getMeta() != null ? event.getMeta() : new MetaMap());
            applySyncRuleToMeta(
                    event.getName(),
                    event.getMeta(),
                    connection.getAssetSyncRules().getOrDefault(event.getAssetType(),
                            connection.getAssetSyncRules().get("*")));
        }
        sendCentralManagerMessage(event);
    }

    protected void sendCentralManagerMessage(SharedEvent event) {
        if (client != null) {
            if (LOG.isLoggable(Level.FINEST)) {
                LOG.finest("Sending message to central manager: realm=" + connection.getLocalRealm() + ", " + event);
            } else {
                LOG.finer(() -> "Sending message to central manager: realm=" + connection.getLocalRealm() + ", " + event.getClass().getSimpleName());
            }
            client.sendMessage(messageToString(SharedEvent.MESSAGE_PREFIX, event));
        }
    }

    protected CompletableFuture<Void> startGatewayTunnel(GatewayTunnelInfo tunnelInfo) {
        if (tunnelFactory == null) {
            return CompletableFuture.failedFuture(new IllegalStateException("Gateway tunnel factory not available"));
        }

        LOG.info("Start tunnel request received: " + connection + ", " + tunnelInfo);

        GatewayTunnelSession session = tunnelFactory.createSession(tunnelHostname, tunnelPort, tunnelInfo, this::onTunnelSessionClosed);
        activeTunnelSessions.add(session);
        return session.connectFuture.orTimeout(60000, TimeUnit.MILLISECONDS);
    }

    protected String stopGatewayTunnel(GatewayTunnelStopRequestEvent stopRequestEvent) {
        if (activeTunnelSessions == null) {
            return "Gateway tunnel factory not available";
        }

        AtomicReference<GatewayTunnelSession> activeTunnel = new AtomicReference<>();
        activeTunnelSessions.removeIf(activeTunnelSession -> {
            if (activeTunnel.get() != null) {
                // Already matched on previous iteration
                return false;
            }
            boolean matches = Objects.equals(activeTunnelSession.getTunnelInfo(), stopRequestEvent.getInfo());
            if (matches) {
                activeTunnel.set(activeTunnelSession);
            }
            return matches;
        });
        return stopGatewayTunnel(activeTunnel.get());
    }

    protected void stopAllGatewayTunnels() {
        // Close tunnels for this specific connection
        if (tunnelFactory == null) {
            return;
        }

        LOG.info("Stopping all tunnels: " + connection);

        activeTunnelSessions.removeIf(activeTunnelSession -> {
            stopGatewayTunnel(activeTunnelSession);
            return true;
        });
    }

    protected String stopGatewayTunnel(GatewayTunnelSession gatewayTunnelSession) {
        if (gatewayTunnelSession == null) {
            return null;
        }

        LOG.info("Stop tunnel request received: " + connection + ", " + gatewayTunnelSession);
        String error = null;

        try {
            gatewayTunnelSession.disconnect();
        } catch (Exception e) {
            error = e.getMessage();
        }
        return error;
    }

    protected void onTunnelSessionClosed(Throwable sessionCloseError) {
        if (sessionCloseError == null) {
            // Manually requested tunnel session close so nothing to do here
        } else {
            LOG.log(Level.WARNING, "Gateway tunnel session closed with error: " + sessionCloseError.getMessage());

        }
    }

    public GatewayConnection getConnection() {
        return connection;
    }

    public ConnectionStatus getConnectionStatus() {
        if (connection.isDisabled()) {
            return ConnectionStatus.DISABLED;
        }

        return client != null ? client.getConnectionStatus() : null;
    }

    public String getRealm() {
        return connection.getLocalRealm();
    }

    protected EventFilter<AttributeEvent> getOutboundAttributeEventFilter() {

        // Convert filters to predicates for efficiency
        List<Pair<AssetQueryPredicate, GatewayAttributeFilter>> predicatesWithFilters;
        if (connection.getAttributeFilters() != null && !connection.getAttributeFilters().isEmpty()) {
            predicatesWithFilters = connection.getAttributeFilters()
                    .stream()
                    .map(filter -> {
                        AssetQueryPredicate predicate = filter.getMatcher() != null ? new AssetQueryPredicate(timerService, assetStorageService, filter.getMatcher()) : null;
                        return new Pair<>(predicate, filter);
                    })
                    .toList();
        } else {
            predicatesWithFilters = Collections.emptyList();
        }

        return ev -> {
            if (!connection.getLocalRealm().equals(ev.getRealm())) {
                return null;
            }

            // Allow attribute events that came from the central manager to be returned
            if (getClass().getSimpleName().equals(ev.getSource())) {
                return ev;
            }

            boolean allowEvent = predicatesWithFilters.stream()
                    .filter(predicateWithFilter -> {
                        if (predicateWithFilter.key == null) {
                            // Match all
                            return true;
                        }
                        return predicateWithFilter.key.test(ev);
                    })
                    .findFirst()
                    .map(predicatesWithFilter -> {
                        GatewayAttributeFilter filter = predicatesWithFilter.value;
                        if (filter.isAllow()) {
                            return true;
                        }
                        if (filter.getSkipAlways() != null && filter.getSkipAlways()) {
                            return false;
                        }
                        if (filter.getValueChange() != null && filter.getValueChange()) {
                            if (!Objects.equals(ev.getValue(), ev.getOldValue())) {
                                LOG.finest(() -> "Gateway client for '" + connection.getLocalRealm() + "' value change has allowed attribute event: " + ev.getRef());
                                return true;
                            }
                        }
                        if (filter.getDelta() != null) {
                            if (Number.class.isAssignableFrom(ev.getTypeClass())) {
                                double delta = filter.getDelta();
                                double value = ev.getValue(Double.class).orElse(0d);
                                double oldValue = ev.getOldValue(Double.class).orElse(0d);
                                if (Math.abs(value - oldValue) > Math.abs(delta)) {
                                    LOG.finest(() -> "Gateway client for '" + connection.getLocalRealm() + "' delta setting has allowed attribute event: " + ev.getRef());
                                    return true;
                                }
                            }
                        }
                        if (filter.getDurationParsed().isPresent()) {
                            boolean allow = filter.getDurationParsed().map(durationMillis -> {
                                Map<AttributeRef, Long> attributeTimestamps = this.attributeTimestamps.get(connection.getLocalRealm());
                                Long lastSendMillis = attributeTimestamps.get(ev.getRef());
                                if (lastSendMillis == null || timerService.getCurrentTimeMillis() - lastSendMillis > durationMillis) {
                                    LOG.finest(() -> "Gateway client for '" + connection.getLocalRealm() + "' duration setting has allowed attribute event: " + ev.getRef());
                                    attributeTimestamps.put(ev.getRef(), timerService.getCurrentTimeMillis());
                                    return true;
                                }
                                LOG.finest(() -> "Gateway client for '" + connection.getLocalRealm() + "' duration setting has blocked attribute event: " + ev.getRef());
                                return false;
                            }).orElse(true);

                            return allow;
                        }

                        return false;
                    }).orElse(true);

            // Skip any events for attributes excluded by sync rules
            if (allowEvent && connection.getAssetSyncRules() != null) {
                GatewayAssetSyncRule syncRule = connection.getAssetSyncRules().getOrDefault(ev.getAssetType(), connection.getAssetSyncRules().get("*"));
                if (syncRule != null && syncRule.excludeAttributes != null && syncRule.excludeAttributes.contains(ev.getName())) {
                    LOG.finer(() -> "Attribute event excluded due to sync rule: " + ev);
                    allowEvent = false;
                }
            }

            return allowEvent ? ev : null;
        };
    }

    /**
     *
     * If the assetSyncRules affect this AssetType:
     * 1) Filter out the attributes that have been defined in GatewayAssetSyncRule.<assetType>.excludeAttributes
     * 2) Find the metaItem exclusions, with the specific attibuteName having priority over the wildcard. If none are found, Use Optional.Empty
     * 3) Put the attribute and the List of excluded metaItems in a Tuple (MutablePair)
     * 4) Filter out any excluded metaItems by using Attribute.setMeta(Attribute.getMeta)
     * 5)
     *
     * @param asset Asset to filter
     * @param assetSyncRules Asset Sync Rules as found in GatewayConnection
     * @return The asset as given, with attributes and metaItems stripped out as instructed by the assetSyncRules
     */
    protected Asset<?> applySyncRules(Asset<?> asset, Map<String, GatewayAssetSyncRule> assetSyncRules) {

        if (asset == null || assetSyncRules == null) {
            return asset;
        }

        GatewayAssetSyncRule syncRule = assetSyncRules.getOrDefault(asset.getType(), assetSyncRules.get("*"));

        if (syncRule == null) {
            return asset;
        }

        List<Attribute<?>> attributes = asset.getAttributes().stream()
                .filter(it -> syncRule.excludeAttributes == null || !syncRule.excludeAttributes.contains(it.getName()))
                .peek(attribute -> applySyncRuleToMeta(attribute.getName(), attribute.getMeta(), syncRule)).toList();

        asset.setAttributes(attributes);
        return asset;
    }

    protected void applySyncRuleToMeta(String attributeName, MetaMap meta, GatewayAssetSyncRule syncRule) {
        if (syncRule == null) {
            return;
        }
        if (syncRule.excludeAttributeMeta != null && !meta.isEmpty()) {
            List<String> excludeMetaRules = syncRule.excludeAttributeMeta.getOrDefault(attributeName, syncRule.excludeAttributeMeta.get("*"));
            if (excludeMetaRules != null && !excludeMetaRules.isEmpty()) {
                meta.keySet().removeIf(excludeMetaRules::contains);
            }
        }
        if (syncRule.addAttributeMeta != null) {
            Map<String, MetaItem<?>> addMetaRules = syncRule.addAttributeMeta.getOrDefault(attributeName, syncRule.addAttributeMeta.get("*"));
            if (addMetaRules != null && !addMetaRules.isEmpty()) {
                meta.addAll(addMetaRules);
            }
        }
    }

    protected String getClientSessionKey() {
        return CLIENT_EVENT_SESSION_PREFIX + getRealm();
    }

    protected static <T> T messageFromString(String message, String prefix, Class<T> clazz) {
        message = message.substring(prefix.length());
        return ValueUtil.parse(message, clazz).orElse(null);
    }

    protected static String messageToString(String prefix, Object message) {
        String str = ValueUtil.asJSON(message).orElse("null");
        return prefix + str;
    }
}
