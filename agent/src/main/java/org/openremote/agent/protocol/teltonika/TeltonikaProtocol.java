package org.openremote.agent.protocol.teltonika;

import io.netty.channel.socket.DatagramChannel;
import io.netty.channel.socket.SocketChannel;
import org.openremote.agent.protocol.AbstractProtocol;
import org.openremote.agent.protocol.io.IOServer;
import org.openremote.model.Container;
import org.openremote.model.asset.Asset;
import org.openremote.model.asset.agent.ConnectionStatus;
import org.openremote.model.asset.agent.DefaultAgentLink;
import org.openremote.model.attribute.Attribute;
import org.openremote.model.attribute.AttributeEvent;
import org.openremote.model.telematics.core.DeviceMessage;
import org.openremote.model.telematics.teltonika.TeltonikaAssetMapper;
import org.openremote.model.telematics.teltonika.TeltonikaTrackerAsset;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.logging.Logger;

public class TeltonikaProtocol extends AbstractProtocol<TeltonikaAgent, DefaultAgentLink> {

    public static final String PROTOCOL_DISPLAY_NAME = "Teltonika AVL Server";

    private static final Logger LOG = Logger.getLogger(TeltonikaProtocol.class.getName());

    private final TeltonikaAssetMapper assetMapper = new TeltonikaAssetMapper();

    private TeltonikaTcpServer tcpServer;
    private TeltonikaUdpServer udpServer;
    private IOServer.IoServerMessageConsumer<TeltonikaRecord, SocketChannel, InetSocketAddress> tcpMessageConsumer;
    private IOServer.IoServerMessageConsumer<TeltonikaRecord, DatagramChannel, InetSocketAddress> udpMessageConsumer;
    private Consumer<ConnectionStatus> tcpStatusConsumer;
    private Consumer<ConnectionStatus> udpStatusConsumer;
    private ConnectionStatus tcpStatus = ConnectionStatus.DISCONNECTED;
    private ConnectionStatus udpStatus = ConnectionStatus.DISCONNECTED;

    public TeltonikaProtocol(TeltonikaAgent agent) {
        super(agent);
    }

    @Override
    public String getProtocolName() {
        return PROTOCOL_DISPLAY_NAME;
    }

    @Override
    public String getProtocolInstanceUri() {
        String bindHost = agent.getBindHost().orElse("0.0.0.0");
        int bindPort = agent.getBindPort().orElse(0);
        return "teltonika://" + bindHost + ":" + bindPort + "?transport=" + getTransportMode().name();
    }

    @Override
    protected void doStart(Container container) {
        int port = agent.getBindPort().orElseThrow(() ->
                new IllegalArgumentException("Missing or invalid attribute: " + TeltonikaAgent.BIND_PORT.getName()));

        InetSocketAddress bindAddress = agent.getBindHost()
                .map(host -> new InetSocketAddress(host, port))
                .orElseGet(() -> new InetSocketAddress(port));

        TransportMode transportMode = getTransportMode();
        if (transportMode == TransportMode.TCP || transportMode == TransportMode.BOTH) {
            startTcp(bindAddress);
        }
        if (transportMode == TransportMode.UDP || transportMode == TransportMode.BOTH) {
            startUdp(bindAddress);
        }
    }

    @Override
    protected void doStop(Container container) {
        if (tcpServer != null) {
            if (tcpMessageConsumer != null) {
                tcpServer.removeMessageConsumer(tcpMessageConsumer);
            }
            if (tcpStatusConsumer != null) {
                tcpServer.removeConnectionStatusConsumer(tcpStatusConsumer);
            }
            tcpServer.stop();
            tcpServer = null;
            tcpMessageConsumer = null;
            tcpStatusConsumer = null;
            tcpStatus = ConnectionStatus.DISCONNECTED;
        }

        if (udpServer != null) {
            if (udpMessageConsumer != null) {
                udpServer.removeMessageConsumer(udpMessageConsumer);
            }
            if (udpStatusConsumer != null) {
                udpServer.removeConnectionStatusConsumer(udpStatusConsumer);
            }
            udpServer.stop();
            udpServer = null;
            udpMessageConsumer = null;
            udpStatusConsumer = null;
            udpStatus = ConnectionStatus.DISCONNECTED;
        }

        refreshAggregatedStatus();
    }

    @Override
    protected void doLinkAttribute(String assetId, Attribute<?> attribute, DefaultAgentLink agentLink) {
        // No-op: Teltonika assets are managed/provisioned by the protocol.
    }

    @Override
    protected void doUnlinkAttribute(String assetId, Attribute<?> attribute, DefaultAgentLink agentLink) {
        // No-op.
    }

    @Override
    protected void doLinkedAttributeWrite(DefaultAgentLink agentLink, AttributeEvent event, Object processedValue) {
        LOG.fine("Ignoring linked attribute write for Teltonika protocol: " + event.getRef());
    }

    private void startTcp(InetSocketAddress bindAddress) {
        LOG.info("Starting Teltonika TCP server on " + bindAddress);
        tcpServer = new TeltonikaTcpServer(bindAddress);
        tcpStatusConsumer = status -> onServerStatusChanged(TransportMode.TCP, status);
        tcpServer.addConnectionStatusConsumer(tcpStatusConsumer);
        tcpMessageConsumer = (record, channel, sender) -> onRecordReceived(record);
        tcpServer.addMessageConsumer(tcpMessageConsumer);
        tcpServer.start();
    }

    private void startUdp(InetSocketAddress bindAddress) {
        LOG.info("Starting Teltonika UDP server on " + bindAddress);
        udpServer = new TeltonikaUdpServer(bindAddress);
        udpStatusConsumer = status -> onServerStatusChanged(TransportMode.UDP, status);
        udpServer.addConnectionStatusConsumer(udpStatusConsumer);
        udpMessageConsumer = (record, channel, sender) -> onRecordReceived(record);
        udpServer.addMessageConsumer(udpMessageConsumer);
        udpServer.start();
    }

    private void onRecordReceived(TeltonikaRecord record) {
        if (record == null || record.getImei() == null || record.getImei().isBlank()) {
            LOG.warning("Dropping Teltonika record without IMEI");
            return;
        }

        DeviceMessage message = toDeviceMessage(record);
        TeltonikaTrackerAsset asset = getOrCreateAsset(record.getImei());
        applyMessage(asset, message);
    }

    private DeviceMessage toDeviceMessage(TeltonikaRecord record) {
        Map<String, Attribute<?>> byName = new LinkedHashMap<>();

        record.getAttributes().values().forEach(attr -> byName.put(attr.getName(), attr));

        if (record.getLocation() != null) {
            byName.put(TeltonikaTrackerAsset.GPS_LOCATION.getName(),
                    new Attribute<>(TeltonikaTrackerAsset.GPS_LOCATION, record.getLocation(), record.getTimestamp()));
            byName.put(Asset.LOCATION.getName(),
                    new Attribute<>(Asset.LOCATION, record.getLocation(), record.getTimestamp()));
        }

        byName.put(TeltonikaTrackerAsset.TIMESTAMP.getName(),
                new Attribute<>(TeltonikaTrackerAsset.TIMESTAMP, record.getTimestamp(), record.getTimestamp()));

        if (record.getCodecName() != null) {
            byName.put(TeltonikaTrackerAsset.CODEC.getName(),
                    new Attribute<>(TeltonikaTrackerAsset.CODEC, record.getCodecName(), record.getTimestamp()));
        }
        if (record.getTransport() != null) {
            byName.put(TeltonikaTrackerAsset.PROTOCOL.getName(),
                    new Attribute<>(TeltonikaTrackerAsset.PROTOCOL, record.getTransport(), record.getTimestamp()));
        }

        return DeviceMessage.builder()
                .deviceId(record.getImei())
                .protocolName(record.getProtocolId())
                .addAttributes(byName.values())
                .build();
    }

    private TeltonikaTrackerAsset getOrCreateAsset(String imei) {
        String assetId = assetMapper.generateAssetId(imei);
        if (assetService.findAsset(assetId) instanceof TeltonikaTrackerAsset existing) {
            return existing;
        }

        TeltonikaTrackerAsset created = assetMapper.createAsset(imei, agent.getRealm());
        created.setParentId(agent.getId());
        created.setProtocol(recordTransportLabel());
        return assetService.mergeAsset(created);
    }

    private void applyMessage(TeltonikaTrackerAsset asset, DeviceMessage message) {
        List<Attribute<?>> newlyCreated = assetMapper.applyAttributes(asset, message);
        assetService.mergeAsset(asset);

        for (Attribute<?> attribute : message.getAttributes()) {
            boolean isNew = newlyCreated.stream().anyMatch(a -> Objects.equals(a.getName(), attribute.getName()));
            if (isNew || attribute.getValue().isEmpty()) {
                continue;
            }
            sendAttributeEvent(new AttributeEvent(asset.getId(), attribute.getName(), attribute.getValue().get()));
        }
    }

    private TransportMode getTransportMode() {
        String configured = agent.getTransport().orElse("BOTH");
        try {
            return TransportMode.valueOf(configured.toUpperCase());
        } catch (IllegalArgumentException ignored) {
            LOG.warning("Unsupported transport '" + configured + "', defaulting to BOTH");
            return TransportMode.BOTH;
        }
    }

    private void onServerStatusChanged(TransportMode transportMode, ConnectionStatus status) {
        if (transportMode == TransportMode.TCP) {
            tcpStatus = status;
        } else if (transportMode == TransportMode.UDP) {
            udpStatus = status;
        }
        refreshAggregatedStatus();
    }

    private void refreshAggregatedStatus() {
        TransportMode mode = getTransportMode();
        ConnectionStatus next;
        if (mode == TransportMode.BOTH) {
            next = aggregateStatus(tcpStatus, udpStatus);
        } else if (mode == TransportMode.UDP) {
            next = udpStatus;
        } else {
            next = tcpStatus;
        }
        setConnectionStatus(next);
    }

    private ConnectionStatus aggregateStatus(ConnectionStatus first, ConnectionStatus second) {
        if (first == ConnectionStatus.ERROR || second == ConnectionStatus.ERROR) {
            return ConnectionStatus.ERROR;
        }
        if (first == ConnectionStatus.CONNECTED || second == ConnectionStatus.CONNECTED) {
            return ConnectionStatus.CONNECTED;
        }
        if (first == ConnectionStatus.CONNECTING || second == ConnectionStatus.CONNECTING) {
            return ConnectionStatus.CONNECTING;
        }
        if (first == ConnectionStatus.WAITING || second == ConnectionStatus.WAITING) {
            return ConnectionStatus.WAITING;
        }
        if (first == ConnectionStatus.DISCONNECTING || second == ConnectionStatus.DISCONNECTING) {
            return ConnectionStatus.DISCONNECTING;
        }
        return ConnectionStatus.DISCONNECTED;
    }

    private String recordTransportLabel() {
        return switch (getTransportMode()) {
            case TCP -> "TCP";
            case UDP -> "UDP";
            case BOTH -> "TCP+UDP";
        };
    }

    private enum TransportMode {
        TCP,
        UDP,
        BOTH
    }
}
