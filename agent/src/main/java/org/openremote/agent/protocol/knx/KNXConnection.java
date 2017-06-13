package org.openremote.agent.protocol.knx;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.openremote.agent.protocol.ConnectionStatus;
import org.openremote.agent.protocol.ProtocolExecutorService;

import tuwien.auto.calimero.CloseEvent;
import tuwien.auto.calimero.FrameEvent;
import tuwien.auto.calimero.KNXException;
import tuwien.auto.calimero.link.KNXNetworkLink;
import tuwien.auto.calimero.link.KNXNetworkLinkIP;
import tuwien.auto.calimero.link.NetworkLinkListener;
import tuwien.auto.calimero.link.medium.TPSettings;
import tuwien.auto.calimero.process.ProcessCommunicator;
import tuwien.auto.calimero.process.ProcessCommunicatorImpl;

public class KNXConnection implements NetworkLinkListener {

    protected ConnectionStatus connectionStatus = ConnectionStatus.DISCONNECTED;
    
    protected final static int INITIAL_RECONNECT_DELAY_MILLIS = 1000;
    protected final static int MAX_RECONNECT_DELAY_MILLIS = 60000;
    protected final static int RECONNECT_BACKOFF_MULTIPLIER = 2;
    protected ScheduledFuture<?> reconnectTask;
    protected int reconnectDelayMilliseconds = INITIAL_RECONNECT_DELAY_MILLIS;
    
    protected final List<Consumer<ConnectionStatus>> connectionStatusConsumers = new ArrayList<>();
    
    protected final String gatewayIp;
    protected final ProtocolExecutorService executorService;
    protected final int port = 3671;
    protected KNXNetworkLink knxLink;
    
    private static final Logger LOG = Logger.getLogger(KNXConnection.class.getName());
    
    public KNXConnection(String gatewayIp, ProtocolExecutorService executorService) {
        this.gatewayIp = gatewayIp;
        this.executorService = executorService;
    }

    public synchronized void connect() {
        if (connectionStatus != ConnectionStatus.DISCONNECTED && connectionStatus != ConnectionStatus.WAITING) {
            LOG.finest("Must be disconnected before calling connect");
            return;
        }

        LOG.fine("Connecting");
        onConnectionStatusChanged(ConnectionStatus.CONNECTING);

        
        final InetSocketAddress remote = new InetSocketAddress(gatewayIp, port);
        try  {
            knxLink = KNXNetworkLinkIP.newTunnelingLink(null, remote, false, TPSettings.TP1);
//            ProcessCommunicator pc = new ProcessCommunicatorImpl(knxLink);
            knxLink.addLinkListener(this);

            //TODO  start listening to group notifications using a process listener
            //pc.addProcessListener(this);
            
            if (knxLink.isOpen()) {
                LOG.fine("Successfully connected to: " + gatewayIp + ":" + port);
                reconnectTask = null;
                reconnectDelayMilliseconds = INITIAL_RECONNECT_DELAY_MILLIS;
                onConnectionStatusChanged(ConnectionStatus.CONNECTED);
            } else {
                LOG.log(Level.INFO, "Connection error");
                // Failed to connect so schedule reconnection attempt
                scheduleReconnect();
            }
        }
        catch (final KNXException | InterruptedException | RuntimeException e) {
            LOG.log(Level.INFO, "Connection error", e.getMessage());
            scheduleReconnect();
        }
    }

    public synchronized void disconnect() {
        if (connectionStatus == ConnectionStatus.DISCONNECTING || connectionStatus == ConnectionStatus.DISCONNECTED) {
            LOG.finest("Already disconnecting or disconnected");
            return;
        }

        LOG.finest("Disconnecting");
        onConnectionStatusChanged(ConnectionStatus.DISCONNECTING);
        knxLink.removeLinkListener(this);
        knxLink.close();
        knxLink = null;
        onConnectionStatusChanged(ConnectionStatus.DISCONNECTED);
    }
        
    public synchronized void addConnectionStatusConsumer(Consumer<ConnectionStatus> connectionStatusConsumer) {
        if (!connectionStatusConsumers.contains(connectionStatusConsumer)) {
            connectionStatusConsumers.add(connectionStatusConsumer);
        }
    }

    public synchronized void removeConnectionStatusConsumer(Consumer<ConnectionStatus> connectionStatusConsumer) {
        connectionStatusConsumers.remove(connectionStatusConsumer);
    }

    protected void onConnectionStatusChanged(ConnectionStatus connectionStatus) {
        this.connectionStatus = connectionStatus;

        synchronized (connectionStatusConsumers) {
            connectionStatusConsumers.forEach(
                consumer -> consumer.accept(connectionStatus)
            );
        }
    }
    
    
    @Override
    public void indication(FrameEvent e) {
    }

    @Override
    public void linkClosed(CloseEvent e) {
        onConnectionStatusChanged(ConnectionStatus.CLOSED);
        knxLink.removeLinkListener(this);
        knxLink.close();
        knxLink = null;
        scheduleReconnect();
    }

    @Override
    public void confirmation(FrameEvent e) {
    }
    
    protected synchronized void scheduleReconnect() {
        if (reconnectTask != null) {
            return;
        }

        onConnectionStatusChanged(ConnectionStatus.WAITING);

        if (reconnectDelayMilliseconds < MAX_RECONNECT_DELAY_MILLIS) {
            reconnectDelayMilliseconds *= RECONNECT_BACKOFF_MULTIPLIER;
            reconnectDelayMilliseconds = Math.min(MAX_RECONNECT_DELAY_MILLIS, reconnectDelayMilliseconds);
        }

        LOG.finest("Scheduling reconnection in '" + reconnectDelayMilliseconds + "' milliseconds");

        reconnectTask = executorService.schedule(() -> {
            synchronized (KNXConnection.this) {
                reconnectTask = null;
                // Attempt to reconnect if not disconnecting
                if (connectionStatus != ConnectionStatus.DISCONNECTING && connectionStatus != ConnectionStatus.DISCONNECTED) {
                    connect();
                }
            }
        }, reconnectDelayMilliseconds);
    }
}
