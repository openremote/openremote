package org.openremote.agent.protocol.teltonika;

import io.netty.channel.socket.DatagramChannel;
import io.netty.channel.socket.SocketChannel;
import org.openremote.agent.protocol.AbstractProtocol;
import org.openremote.agent.protocol.io.IOServer;
import org.openremote.model.Container;
import org.openremote.model.asset.agent.ConnectionStatus;
import org.openremote.model.asset.agent.DefaultAgentLink;
import org.openremote.model.attribute.Attribute;
import org.openremote.model.attribute.AttributeEvent;
import org.openremote.model.telematics.core.DeviceMessage;
import org.openremote.model.telematics.core.TelematicsMessagePublisher;
import org.openremote.model.telematics.protocol.MessageContext;
import org.openremote.model.telematics.teltonika.TeltonikaTrackerAsset;

import java.net.InetSocketAddress;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.logging.Logger;

public class TeltonikaProtocol extends AbstractProtocol<TeltonikaAgent, DefaultAgentLink> {

    public static final String PROTOCOL_DISPLAY_NAME = "Teltonika AVL Server";
    private static final String VENDOR_ID = "teltonika";

    private static final Logger LOG = Logger.getLogger(TeltonikaProtocol.class.getName());

    private TeltonikaTcpServer tcpServer;
    private TeltonikaUdpServer udpServer;
    private IOServer.IoServerMessageConsumer<TeltonikaRecord, SocketChannel, InetSocketAddress> tcpMessageConsumer;
    private IOServer.IoServerMessageConsumer<TeltonikaRecord, DatagramChannel, InetSocketAddress> udpMessageConsumer;
    private Consumer<ConnectionStatus> tcpStatusConsumer;
    private Consumer<ConnectionStatus> udpStatusConsumer;
    private ConnectionStatus tcpStatus = ConnectionStatus.DISCONNECTED;
    private ConnectionStatus udpStatus = ConnectionStatus.DISCONNECTED;

    private TelematicsMessagePublisher telematicsMessagePublisher;

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
        this.telematicsMessagePublisher = container.getService(TelematicsMessagePublisher.class);

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

        telematicsMessagePublisher = null;
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
        telematicsMessagePublisher.submitMessage(
                VENDOR_ID,
                agent.getRealm(),
                toTransport(record),
                record.getCodecName(),
                message
        );
    }

    private DeviceMessage toDeviceMessage(TeltonikaRecord record) {
        Map<String, Attribute<?>> byName = new LinkedHashMap<>();

        record.getAttributes().values().forEach(attr -> byName.put(attr.getName(), attr));

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

    private MessageContext.Transport toTransport(TeltonikaRecord record) {
        if (record.getTransport() == null) {
            throw new IllegalStateException("TeltonikaRecord's Transport is null");
        }
        return switch (record.getTransport().toUpperCase()) {
            case "UDP" -> MessageContext.Transport.UDP;
            case "TCP" -> MessageContext.Transport.TCP;
            case "MQTT" -> MessageContext.Transport.MQTT;
            default -> throw new IllegalArgumentException("Unknown transport " + record.getTransport());
        };
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

    private enum TransportMode {
        TCP,
        UDP,
        BOTH
    }
}
