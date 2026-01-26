package org.openremote.manager.gateway;

import io.netty.channel.ChannelHandler;
import org.apache.http.client.utils.URIBuilder;
import org.openremote.agent.protocol.io.AbstractNettyIOClient;
import org.openremote.container.timer.TimerService;
import org.openremote.manager.asset.AssetProcessingService;
import org.openremote.manager.asset.AssetStorageService;
import org.openremote.manager.event.ClientEventService;
import org.openremote.manager.rules.AssetQueryPredicate;
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
import static org.openremote.model.syslog.SyslogCategory.GATEWAY;

/**
 * Handles all communication with the central gateway instance.
 */
public class GatewayClientConnector implements AutoCloseable {
    private static final Logger LOG = SyslogCategory.getLogger(GATEWAY, GatewayClientConnector.class.getName());
    protected final ClientEventService clientEventService;
    protected final TimerService timerService;
    protected final AssetStorageService assetStorageService;
    protected final AssetProcessingService assetProcessingService;
    protected GatewayConnection connection;
    protected GatewayIOClient client;
    protected GatewayTunnelFactory tunnelFactory;
    protected final List<GatewayTunnelSession> activeTunnelSessions;
    protected Map<String, Map<AttributeRef, Long>> attributeTimestamps;
    protected Consumer<AssetEvent> realmAssetEventConsumer;
    protected Consumer<AttributeEvent> realmAttributeEventConsumer;
    protected String gatewayAPIVersion;
    protected String tunnelHostname;
    protected int tunnelPort;

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

    protected GatewayIOClient createClient() {
        LOG.info("Creating gateway IO client: " + connection);

        if (connection.isDisabled()) {
            LOG.info("Disabled gateway client connection so ignoring: " + connection);
            return null;
        }

        try {
            GatewayIOClient client = new GatewayIOClient(
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

            client.setEncoderDecoderProvider(() ->
                    new ChannelHandler[] {new AbstractNettyIOClient.MessageToMessageDecoder<>(String.class, client)}
            );

            client.addConnectionStatusConsumer(status -> onClientConnectionStatusChanged(status, false));

            client.addMessageConsumer(this::onCentralManagerMessage);

            realmAssetEventConsumer = this::sendAssetEvent;

            // Subscribe to Asset<?> and attribute events of local realm and pass through to connected manager
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

    protected void onClientConnectionStatusChanged(ConnectionStatus connectionStatus, boolean forcePublish) {
        LOG.finest("Connection status change for gateway IO client '" + connectionStatus + "': " + connection);
        if (connectionStatus == ConnectionStatus.CONNECTED) {
            LOG.finer(() -> "Gateway IO client is connected but now waiting for full initialisation to complete '" + connectionStatus + "': " + connection);
            if (!forcePublish) {
                return;
            }
        }

        clientEventService.publishEvent(new GatewayConnectionStatusEvent(timerService.getCurrentTimeMillis(), connection.getLocalRealm(), connectionStatus));
    }

    protected void onCentralManagerMessage(String message) {
        SharedEvent event = messageFromString(message, SharedEvent.MESSAGE_PREFIX, SharedEvent.class);

        if (event == null) {
            LOG.finer("Received empty message from central manager: realm=" + connection.getLocalRealm());
            return;
        }

        LOG.finer(() -> "Received message from central manager: realm=" + connection.getLocalRealm() + ", " + event);

        switch (event) {
            case GatewayCapabilitiesRequestEvent capabilitiesRequestEvent -> {
                gatewayAPIVersion = capabilitiesRequestEvent.getVersion();
                tunnelHostname = capabilitiesRequestEvent.getTunnelHostname();
                tunnelPort = capabilitiesRequestEvent.getTunnelPort();

                if (gatewayAPIVersion == null) {
                    // This is a legacy version of the openremote on the central instance so we won't get a GatewayInitialisedEvent
                    // so we mark this client as connected now
                    LOG.fine("Central manager running an older version so assuming connection is initialised");
                    onClientInitComplete(null);
                }

                GatewayCapabilitiesResponseEvent responseEvent = new GatewayCapabilitiesResponseEvent(tunnelFactory != null);
                responseEvent.setMessageID(event.getMessageID());
                sendCentralManagerMessage(messageToString(SharedEvent.MESSAGE_PREFIX, responseEvent));
            }
            case GatewayInitialisedEvent gatewayInitializedEvent -> {
                onClientInitComplete(gatewayInitializedEvent.getActiveTunnels());
            }
            case GatewayDisconnectEvent disconnectEvent ->
                LOG.info("Central manager requested disconnect: reason=" + disconnectEvent.getReason());
            case GatewayTunnelStartRequestEvent startRequestEvent -> {
                if (tunnelFactory == null) {
                    LOG.finest("Gateway tunnel creation request received but gateway tunnel factory is not available: realm=" + connection.getLocalRealm());
                    return;
                }
                startGatewayTunnel(startRequestEvent.getInfo()).whenComplete((v, t) -> {
                    String error = t != null ? t.getMessage() : null;

                    if (t != null) {
                        LOG.warning("Gateway tunnel creation failed: " + t.getMessage());
                    }
                    GatewayTunnelStartResponseEvent responseEvent = new GatewayTunnelStartResponseEvent(error);
                    responseEvent.setMessageID(startRequestEvent.getMessageID());
                    sendCentralManagerMessage(messageToString(SharedEvent.MESSAGE_PREFIX, responseEvent));
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
                sendCentralManagerMessage(messageToString(SharedEvent.MESSAGE_PREFIX, responseEvent));
            }
            case AttributeEvent attributeEvent ->
                    assetProcessingService.sendAttributeEvent(attributeEvent, getClass().getSimpleName());
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
            case ReadAssetsEvent readAssets -> {
                AssetQuery query = readAssets.getAssetQuery();
                // Force realm to be the one that this client is associated with
                query.realm(new RealmPredicate(connection.getLocalRealm()));
                List<Asset<?>> assets = assetStorageService.findAll(readAssets.getAssetQuery());
                assets = assets.stream()
                        .map(it -> this.applySyncRules(it, connection.getAssetSyncRules()))
                        .collect(Collectors.toList());
                AssetsEvent responseEvent = new AssetsEvent(assets);
                responseEvent.setMessageID(event.getMessageID());
                sendCentralManagerMessage(messageToString(SharedEvent.MESSAGE_PREFIX, responseEvent));
            }
            default -> {
                LOG.info("Received unknown event from central manager: " + event);
            }
        }
    }

    /**
     * This indicates that the client initialisation is complete (i.e. assets are synchronised)
     */
    protected void onClientInitComplete(GatewayTunnelInfo[] activeTunnels) {
        LOG.info("Gateway client initialisation complete: " + connection);
        clientEventService.publishEvent(new GatewayConnectionStatusEvent(timerService.getCurrentTimeMillis(), connection.getLocalRealm(), ConnectionStatus.CONNECTED));
        onClientConnectionStatusChanged(ConnectionStatus.CONNECTED, true);

        if (tunnelFactory != null && activeTunnels != null) {
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
                    LOG.fine("Active tunnel session not found for tunnel: " + activeTunnel);
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
        } else {
            // Legacy central instance just close all active tunnels
            stopAllGatewayTunnels();
        }
    }

    protected void sendAssetEvent(AssetEvent event) {
        if (connection.getAssetSyncRules() != null) {
            // Apply sync rules to the asset
            event = ValueUtil.clone(event);
            applySyncRules(event.getAsset(), connection.getAssetSyncRules());
        }
        sendCentralManagerMessage(messageToString(SharedEvent.MESSAGE_PREFIX, event));
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
        sendCentralManagerMessage(messageToString(SharedEvent.MESSAGE_PREFIX, event));
    }

    protected void sendCentralManagerMessage(String message) {
        if (client != null) {
            client.sendMessage(message);
        }
    }

    protected CompletableFuture<Void> startGatewayTunnel(GatewayTunnelInfo tunnelInfo) {
        if (tunnelFactory == null) {
            return CompletableFuture.failedFuture(new IllegalStateException("Gateway tunnel factory not available"));
        }

        LOG.info("Start tunnel request received: " + connection + ", " + tunnelInfo);
        GatewayTunnelSession session = tunnelFactory.createSession(tunnelHostname, tunnelPort, tunnelInfo, this::onTunnelSessionClosed);
        activeTunnelSessions.add(session);
        return session.connectFuture.orTimeout(5000, TimeUnit.MILLISECONDS);
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
